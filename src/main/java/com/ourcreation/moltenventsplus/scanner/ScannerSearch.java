package com.ourcreation.moltenventsplus.scanner;

import com.mojang.logging.LogUtils;
import com.ourcreation.moltenventsplus.MoltenVentsPlus;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ChunkResult;
import net.minecraft.server.level.ChunkLevel;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.slf4j.Logger;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/** Incremental vanilla structure-start lookup. All level access stays on the logical server thread. */
@EventBusSubscriber(modid = MoltenVentsPlus.MODID)
public final class ScannerSearch {
    public static final int MAX_RESULTS = 10;
    public static final int PING_TICKS = 400;
    public static final int COOLDOWN_TICKS = 20;
    public static final int MAX_SEARCH_TICKS = 6000;
    private static final int MAX_ACTIVE_SEARCHES = 16;
    private static final int MAX_CANDIDATES = 4096;
    private static final int MAX_KNOWN_VENT_CHUNKS = 8192;
    private static final int MAX_CONFIRMATIONS = 2;
    private static final int MAX_FAILED_CONFIRMATIONS = 32;
    private static final int MAX_SITES = 4096;
    private static final int CANDIDATES_PER_TICK = 4;
    private static final long WORK_BUDGET_NANOS = 2_000_000L;
    // Companion cores may extend eleven blocks outside the selected structure chunk.
    private static final int CORE_MARGIN = 11;
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final TicketType<UUID> CONFIRM_TICKET = TicketType.create("vent_surveyor_confirm", UUID::compareTo, 40);
    private static final TicketType<UUID> METADATA_TICKET = TicketType.create("vent_surveyor_metadata", UUID::compareTo, 40);
    // A region ticket at the STRUCTURE_STARTS level must not promote all candidates to FULL.
    private static final int METADATA_TICKET_DISTANCE = 33 - ChunkLevel.byStatus(ChunkStatus.STRUCTURE_STARTS);
    private static final Map<MinecraftServer, State> STATES = new IdentityHashMap<>();
    private static final Map<MinecraftServer, ConcurrentHashMap<ChunkPos, PendingChunk>> PENDING_STARTS = new ConcurrentHashMap<>();

    private record PlacedStructure(Holder<Structure> structure, RandomSpreadStructurePlacement placement) {}
    private record Candidate(ChunkPos chunk, long lowerDistanceSquared, List<PlacedStructure> structures) {}
    private record Site(BlockPos position, long distance) {}
    private record PendingStart(ScannerTarget target, net.minecraft.world.level.levelgen.structure.StructureStart start) {}
    private record PendingChunk(ServerLevel level, List<PendingStart> starts) {}

    private static final class State {
        int tick;
        final Map<UUID, Job> jobs = new HashMap<>();
        final ArrayDeque<UUID> queue = new ArrayDeque<>();
        final Map<UUID, Integer> nextScan = new HashMap<>();
        final Map<UUID, Integer> lastPacket = new HashMap<>();
        final LinkedHashMap<ChunkPos, Map<ScannerTarget, List<BlockPos>>> knownVents = new LinkedHashMap<>();
        final Set<CompletableFuture<ChunkResult<ChunkAccess>>> confirmations = Collections.newSetFromMap(new IdentityHashMap<>());
        int knownRevision;
    }

    private static final class Job {
        final ServerLevel level;
        final ScannerTarget target;
        final int requestId;
        final int started;
        final BlockPos origin;
        final List<Candidate> candidates;
        final StructurePieceSerializationContext serialization;
        int cursor;
        int lastProgress;
        int nextSignal;
        final PriorityQueue<Site> sites = new PriorityQueue<>(Comparator.comparingLong(Site::distance)
                .thenComparingInt(site -> site.position.getX()).thenComparingInt(site -> site.position.getZ())
                .thenComparingInt(site -> site.position.getY()));
        final Set<BlockPos> seen = new HashSet<>();
        final Set<BlockPos> confirmed = new HashSet<>();
        BlockPos reportPosition;
        String reportDescription;
        int knownRevision = -1;
        final Set<BlockPos> invalid = new HashSet<>();
        final UUID confirmationTicket = UUID.randomUUID();
        CompletableFuture<ChunkResult<ChunkAccess>> confirmation;
        ChunkPos confirmationChunk;
        CompletableFuture<ChunkResult<ChunkAccess>> metadata;
        Candidate metadataCandidate;
        BlockPos confirmingPosition;
        int failedConfirmations;
        volatile boolean detached;

        Job(ServerPlayer player, ScannerTarget target, int requestId, int started, List<Candidate> candidates) {
            this.level = player.serverLevel();
            this.target = target;
            this.requestId = requestId;
            this.started = started;
            this.lastProgress = started;
            this.nextSignal = started;
            this.origin = player.blockPosition().immutable();
            this.candidates = candidates;
            this.serialization = StructurePieceSerializationContext.fromLevel(level);
        }

        boolean finished(int now) {
            return now - started >= target.warmupTicks() && cursor >= candidates.size()
                    && sites.isEmpty() && confirmation == null && metadata == null && !hasPendingStarts(this);
        }

        void add(BlockPos position) {
            var pos = position.immutable();
            long distance = distanceSquared(origin, pos);
            if (distance > (long) target.radius() * target.radius()
                    || !level.getWorldBorder().isWithinBounds(pos) || seen.contains(pos)) return;
            if (seen.size() >= MAX_SITES) throw new IllegalStateException("Vent survey site limit exceeded");
            seen.add(pos);
            sites.add(new Site(pos, distance));
        }

        void step(State state) {
            if (metadata != null) {
                if (!metadata.isDone()) return;
                var candidate = metadataCandidate;
                try {
                    var result = metadata.getNow(null);
                    var chunk = result == null ? null : result.orElse(null);
                    if (chunk != null) readStarts(candidate, chunk);
                } finally { releaseMetadata(this); }
                return;
            }
            if (cursor >= candidates.size() || confirmed.size() + sites.size() >= target.maxResults()) return;
            state.confirmations.removeIf(CompletableFuture::isDone);
            if (state.confirmations.size() >= MAX_CONFIRMATIONS) return;
            var candidate = candidates.get(cursor++);
            var cache = level.getChunkSource();
            var loaded = cache.getChunkNow(candidate.chunk.x, candidate.chunk.z);
            if (loaded != null) { readStarts(candidate, loaded); return; }
            metadataCandidate = candidate;
            cache.addRegionTicket(METADATA_TICKET, candidate.chunk, METADATA_TICKET_DISTANCE, confirmationTicket);
            // On the server thread getChunkFuture can block. Dispatch its documented
            // off-thread branch, which schedules native generation back on the server.
            metadata = CompletableFuture.supplyAsync(() -> detached
                    ? CompletableFuture.<ChunkResult<ChunkAccess>>completedFuture(null)
                    : cache.getChunkFuture(candidate.chunk.x, candidate.chunk.z, ChunkStatus.STRUCTURE_STARTS, true),
                    Util.backgroundExecutor()).thenCompose(Function.identity());
            state.confirmations.add(metadata);
        }

        private void readStarts(Candidate candidate, ChunkAccess chunk) {
            var manager = level.structureManager();
            for (var placed : candidate.structures) {
                var start = manager.getStartForStructure(SectionPos.bottomOf(chunk), placed.structure.value(), chunk);
                if (start == null || !start.isValid()) continue;
                for (var piece : start.getPieces()) {
                    var tag = piece.createTag(serialization);
                    if (tag.contains("Origin", Tag.TAG_LONG)) add(BlockPos.of(tag.getLong("Origin")));
                }
            }
        }
    }

    public static boolean holding(ServerPlayer player) {
        return player.getMainHandItem().getItem() instanceof VentSurveyorItem
                || player.getOffhandItem().getItem() instanceof VentSurveyorItem;
    }

    public static boolean supports(ServerPlayer player, ScannerTarget target) {
        return player.getMainHandItem().getItem() instanceof VentSurveyorItem main && main.supports(target)
                || player.getOffhandItem().getItem() instanceof VentSurveyorItem off && off.supports(target);
    }

    public static void request(ServerPlayer player, ScannerNetwork.Request request) {
        var state = STATES.computeIfAbsent(player.server, ignored -> new State());
        var uuid = player.getUUID();
        Integer last = state.lastPacket.put(uuid, state.tick);
        if (last != null && state.tick - last < 10) {
            reject(player, request, "cooldown", 10 - (state.tick - last));
            return;
        }
        var target = ScannerTarget.byId(request.targetId());
        if (request.requestId() < 0 || target.isEmpty()) {
            reject(player, request, "invalid_target", 0);
            return;
        }
        if (!holding(player) || !player.isAlive() || player.isSpectator()) {
            reject(player, request, "not_held", 0);
            return;
        }
        if (!supports(player, target.get())) {
            reject(player, request, "tier_locked", 0);
            return;
        }
        if (!player.level().dimension().equals(Level.OVERWORLD)) {
            reject(player, request, "wrong_dimension", 0);
            return;
        }
        int retry = state.nextScan.getOrDefault(uuid, state.tick) - state.tick;
        if (retry > 0) {
            reject(player, request, "cooldown", retry);
            return;
        }
        if (state.jobs.containsKey(uuid) || state.jobs.size() >= MAX_ACTIVE_SEARCHES) {
            reject(player, request, "busy", 20);
            return;
        }
        if (!player.server.getWorldData().worldGenOptions().generateStructures()) {
            reject(player, request, "unavailable", 0);
            return;
        }
        state.nextScan.put(uuid, state.tick + COOLDOWN_TICKS);
        try {
            drainLoadedStarts(player.server, state, System.nanoTime() + WORK_BUDGET_NANOS);
            var candidates = candidates(player.serverLevel(), player.blockPosition(), target.get());
            if (candidates == null) {
                reject(player, request, "unavailable", 0);
                return;
            }
            var job = new Job(player, target.get(), request.requestId(), state.tick, candidates);
            refreshKnown(job, state);
            if (job.finished(state.tick)) {
                reply(player, job, ScannerNetwork.Status.NOT_FOUND, BlockPos.ZERO, "", 0);
                return;
            }
            state.jobs.put(uuid, job);
            state.queue.addLast(uuid);
            reply(player, job, ScannerNetwork.Status.SEARCHING, BlockPos.ZERO, "", 0);
        } catch (RuntimeException error) {
            LOGGER.warn("Unable to start vent survey for {}", target.get().id(), error);
            reject(player, request, "unavailable", 0);
        }
    }

    /** Only the requesting player and the exact scan token can stop a live job. */
    public static void cancel(ServerPlayer player, ScannerNetwork.Cancel cancel) {
        var state = STATES.get(player.server);
        if (state == null || cancel.requestId() < 0) return;
        var uuid = player.getUUID();
        var job = state.jobs.get(uuid);
        if (job == null || job.requestId != cancel.requestId()) return;
        discard(state, uuid, job);
        state.nextScan.remove(uuid);
        state.lastPacket.remove(uuid);
        reply(player, job, ScannerNetwork.Status.REJECTED, BlockPos.ZERO, "cancelled", 0);
    }

    /** Build from the world's actual placement data, including biome/datapack filtering. */
    private static List<Candidate> candidates(ServerLevel level, BlockPos origin, ScannerTarget target) {
        int range = target.radius();
        var registry = level.registryAccess().registryOrThrow(Registries.STRUCTURE);
        var structureState = level.getChunkSource().getGeneratorState();
        var grouped = new LinkedHashMap<ChunkPos, List<PlacedStructure>>();
        int minChunkX = Math.floorDiv(origin.getX() - range - CORE_MARGIN, 16);
        int maxChunkX = Math.floorDiv(origin.getX() + range + CORE_MARGIN, 16);
        int minChunkZ = Math.floorDiv(origin.getZ() - range - CORE_MARGIN, 16);
        int maxChunkZ = Math.floorDiv(origin.getZ() + range + CORE_MARGIN, 16);
        for (var key : target.structures()) {
            var optional = registry.getHolder(key);
            if (optional.isEmpty()) continue;
            var holder = optional.get();
            for (var rawPlacement : structureState.getPlacementsForStructure(holder)) {
                if (!(rawPlacement instanceof RandomSpreadStructurePlacement placement)) continue;
                int spacing = placement.spacing();
                int minX = Math.floorDiv(minChunkX, spacing), maxX = Math.floorDiv(maxChunkX, spacing);
                int minZ = Math.floorDiv(minChunkZ, spacing), maxZ = Math.floorDiv(maxChunkZ, spacing);
                if ((long) (maxX - minX + 1) * (maxZ - minZ + 1) > MAX_CANDIDATES) return null;
                for (int x = minX; x <= maxX; x++) for (int z = minZ; z <= maxZ; z++) {
                    var cp = placement.getPotentialStructureChunk(level.getSeed(), x * spacing, z * spacing);
                    long lower = lowerDistanceSquared(origin, cp);
                    if (lower > (long) range * range) continue;
                    if (cp.getMaxBlockX() + CORE_MARGIN < level.getWorldBorder().getMinX()
                            || cp.getMinBlockX() - CORE_MARGIN > level.getWorldBorder().getMaxX()
                            || cp.getMaxBlockZ() + CORE_MARGIN < level.getWorldBorder().getMinZ()
                            || cp.getMinBlockZ() - CORE_MARGIN > level.getWorldBorder().getMaxZ()) continue;
                    var list = grouped.computeIfAbsent(cp, ignored -> new ArrayList<>());
                    if (list.stream().noneMatch(existing -> existing.structure.equals(holder)))
                        list.add(new PlacedStructure(holder, placement));
                    if (grouped.size() > MAX_CANDIDATES) return null;
                }
            }
        }
        return grouped.entrySet().stream()
                .map(entry -> new Candidate(entry.getKey(), lowerDistanceSquared(origin, entry.getKey()), List.copyOf(entry.getValue())))
                .sorted(Comparator.comparingLong(Candidate::lowerDistanceSquared)
                        .thenComparingInt(value -> value.chunk.x).thenComparingInt(value -> value.chunk.z))
                .toList();
    }

    private static long lowerDistanceSquared(BlockPos origin, ChunkPos chunk) {
        long dx = Math.max(0, Math.max(chunk.getMinBlockX() - CORE_MARGIN - origin.getX(),
                origin.getX() - chunk.getMaxBlockX() - CORE_MARGIN));
        long dz = Math.max(0, Math.max(chunk.getMinBlockZ() - CORE_MARGIN - origin.getZ(),
                origin.getZ() - chunk.getMaxBlockZ() - CORE_MARGIN));
        return dx * dx + dz * dz;
    }

    private static long distanceSquared(BlockPos origin, BlockPos target) {
        long dx = (long) target.getX() - origin.getX(), dz = (long) target.getZ() - origin.getZ();
        return dx * dx + dz * dz;
    }

    @SubscribeEvent
    public static void tick(ServerTickEvent.Post event) {
        var state = STATES.get(event.getServer());
        if (state == null) {
            var pending = PENDING_STARTS.get(event.getServer());
            if (pending == null || pending.isEmpty()) return;
            state = new State();
            STATES.put(event.getServer(), state);
        }
        state.tick++;
        state.confirmations.removeIf(CompletableFuture::isDone);
        long deadline = System.nanoTime() + WORK_BUDGET_NANOS;
        drainLoadedStarts(event.getServer(), state, deadline);
        // Cancellation and expiry are cheap and must still run when a busy server has
        // no spare world-generation time for additional candidate checks this tick.
        for (var entry : new ArrayList<>(state.jobs.entrySet())) {
            var uuid = entry.getKey();
            var job = entry.getValue();
            var player = event.getServer().getPlayerList().getPlayer(uuid);
            String reason = player == null || !player.isAlive() || player.isSpectator()
                    || player.serverLevel() != job.level ? "cancelled"
                    : state.tick - job.started >= MAX_SEARCH_TICKS ? "timed_out" : null;
            if (reason == null) {
                if (job.metadataCandidate != null && state.tick % 20 == 0)
                    job.level.getChunkSource().addRegionTicket(METADATA_TICKET, job.metadataCandidate.chunk,
                            METADATA_TICKET_DISTANCE, job.confirmationTicket);
                if (job.confirmationChunk != null && state.tick % 20 == 0)
                    job.level.getChunkSource().addRegionTicket(CONFIRM_TICKET, job.confirmationChunk, 0, job.confirmationTicket);
                if (state.tick - job.lastProgress >= 40) {
                    job.lastProgress = state.tick;
                    reply(player, job, ScannerNetwork.Status.SEARCHING, BlockPos.ZERO, "", 0);
                }
                continue;
            }
            discard(state, uuid, job);
            if (player != null) reply(player, job, ScannerNetwork.Status.REJECTED, BlockPos.ZERO, reason, 0);
        }
        int work = 0;
        while (!state.queue.isEmpty() && work++ < CANDIDATES_PER_TICK && System.nanoTime() < deadline && event.hasTime()) {
            var uuid = state.queue.removeFirst();
            var job = state.jobs.get(uuid);
            if (job == null) continue;
            var player = event.getServer().getPlayerList().getPlayer(uuid);
            if (player == null || !player.isAlive() || player.isSpectator()
                    || player.serverLevel() != job.level) {
                discard(state, uuid, job);
                if (player != null) reply(player, job, ScannerNetwork.Status.REJECTED, BlockPos.ZERO, "cancelled", 0);
                continue;
            }
            if (state.tick - job.started >= MAX_SEARCH_TICKS) {
                discard(state, uuid, job);
                reply(player, job, ScannerNetwork.Status.REJECTED, BlockPos.ZERO, "timed_out", 0);
                continue;
            }
            try {
                if (state.tick - job.started < job.target.warmupTicks()) { state.queue.addLast(uuid); continue; }
                refreshKnown(job, state);
                if (job.confirmation != null || !job.sites.isEmpty()) {
                    var result = confirm(job, state);
                    if (result == Confirmation.VALID) {
                        reply(player, job, ScannerNetwork.Status.FOUND, job.reportPosition, job.reportDescription, 0);
                        job.nextSignal = state.tick + job.target.signalIntervalTicks();
                        if (job.confirmed.size() >= job.target.maxResults()) {
                            discard(state, uuid, job);
                            reply(player, job, ScannerNetwork.Status.COMPLETE, BlockPos.ZERO, "", 0);
                            continue;
                        }
                    } else if (result == Confirmation.CAPPED) {
                        discard(state, uuid, job);
                        reply(player, job, ScannerNetwork.Status.REJECTED, BlockPos.ZERO, "timed_out", 0);
                        continue;
                    }
                }
                job.step(state);
                if (job.finished(state.tick)) {
                    discard(state, uuid, job);
                    reply(player, job, job.confirmed.isEmpty() ? ScannerNetwork.Status.NOT_FOUND : ScannerNetwork.Status.COMPLETE,
                            BlockPos.ZERO, "", 0);
                } else state.queue.addLast(uuid);
            } catch (RuntimeException error) {
                discard(state, uuid, job);
                LOGGER.warn("Vent survey failed for {}", job.target.id(), error);
                reply(player, job, ScannerNetwork.Status.REJECTED, BlockPos.ZERO, "unavailable", 0);
            }
        }
    }

    private enum Confirmation { WAITING, VALID, RETRY, CAPPED }

    /** Confirm one discovered core at a time; metadata alone is never emitted as a signal. */
    private static Confirmation confirm(Job job, State state) {
        if (job.confirmation != null) {
            if (!job.confirmation.isDone()) return Confirmation.WAITING;
            var checked = job.confirmingPosition;
            try {
                var result = job.confirmation.getNow(null);
                var chunk = result == null ? null : result.orElse(null);
                var block = chunk == null ? null : chunk.getBlockState(checked);
                boolean matches = block != null && job.target.matches(block)
                        && job.level.getWorldBorder().isWithinBounds(checked);
                releaseConfirmation(job);
                if (matches) return confirmed(job, checked, block.getBlock().getDescriptionId());
            } catch (RuntimeException error) {
                releaseConfirmation(job);
            }
            return failedConfirmation(job, checked);
        }
        if (job.sites.isEmpty()) return Confirmation.WAITING;
        if (state.tick - job.nextSignal < 0)
            return Confirmation.WAITING;
        var pos = job.sites.peek().position;
        var cp = new ChunkPos(pos);
        var cache = job.level.getChunkSource();
        var loaded = cache.getChunkNow(cp.x, cp.z);
        if (loaded != null) {
            job.sites.remove();
            var block = loaded.getBlockState(pos);
            return job.target.matches(block) && job.level.getWorldBorder().isWithinBounds(pos)
                    ? confirmed(job, pos, block.getBlock().getDescriptionId()) : failedConfirmation(job, pos);
        }
        state.confirmations.removeIf(CompletableFuture::isDone);
        if (state.confirmations.size() >= MAX_CONFIRMATIONS) return Confirmation.WAITING;
        job.sites.remove();
        job.confirmationChunk = cp;
        job.confirmingPosition = pos;
        cache.addRegionTicket(CONFIRM_TICKET, cp, 0, job.confirmationTicket);
        // The off-thread branch of this native API queues its world work onto the
        // server thread. Only dispatch there; consume futures and read blocks here.
        CompletableFuture<CompletableFuture<ChunkResult<ChunkAccess>>> dispatched = CompletableFuture.supplyAsync(
                () -> job.detached ? CompletableFuture.completedFuture(null)
                        : cache.getChunkFuture(cp.x, cp.z, ChunkStatus.FULL, true), Util.backgroundExecutor());
        job.confirmation = dispatched.thenCompose(Function.identity());
        state.confirmations.add(job.confirmation);
        return Confirmation.WAITING;
    }

    private static Confirmation confirmed(Job job, BlockPos pos, String description) {
        if (!job.confirmed.add(pos)) return Confirmation.RETRY;
        job.reportPosition = pos;
        job.reportDescription = description;
        return Confirmation.VALID;
    }

    private static Confirmation failedConfirmation(Job job, BlockPos pos) {
        job.invalid.add(pos);
        job.failedConfirmations++;
        return job.failedConfirmations >= MAX_FAILED_CONFIRMATIONS ? Confirmation.CAPPED : Confirmation.RETRY;
    }

    private static void releaseConfirmation(Job job) {
        if (job.confirmationChunk != null)
            job.level.getChunkSource().removeRegionTicket(CONFIRM_TICKET, job.confirmationChunk, 0, job.confirmationTicket);
        // Vanilla may share its generation future with another caller. Never cancel it.
        job.confirmation = null;
        job.confirmationChunk = null;
        job.confirmingPosition = null;
    }

    private static void releaseMetadata(Job job) {
        if (job.metadataCandidate != null)
            job.level.getChunkSource().removeRegionTicket(METADATA_TICKET, job.metadataCandidate.chunk,
                    METADATA_TICKET_DISTANCE, job.confirmationTicket);
        // Futures can be shared by vanilla callers; detach without cancelling them.
        job.metadata = null;
        job.metadataCandidate = null;
    }

    private static void discard(State state, UUID uuid, Job job) {
        job.detached = true;
        releaseConfirmation(job);
        releaseMetadata(job);
        state.jobs.remove(uuid);
        state.queue.remove(uuid);
    }

    /** Preserve only metadata from vent-bearing chunks seen during this server session.
     * This makes nearby saved starts from older placement grids discoverable as well. */
    @SubscribeEvent
    public static void loaded(ChunkEvent.Load event) {
        if (!(event.getLevel() instanceof ServerLevel level) || !level.dimension().equals(Level.OVERWORLD)) return;
        var registry = level.registryAccess().registryOrThrow(Registries.STRUCTURE);
        var starts = new ArrayList<PendingStart>();
        for (var entry : event.getChunk().getAllStarts().entrySet()) {
            if (!entry.getValue().isValid()) continue;
            var id = registry.getKey(entry.getKey());
            if (id == null) continue;
            for (var target : ScannerTarget.values()) {
                if (target.structures().stream().anyMatch(key -> key.location().equals(id))) {
                    starts.add(new PendingStart(target, entry.getValue()));
                    break;
                }
            }
        }
        if (starts.isEmpty()) return;
        // Chunk load may run before FULL promotion. Capture references only; defer all
        // piece serialization to the server tick, without asking for another chunk.
        var pending = PENDING_STARTS.computeIfAbsent(level.getServer(), ignored -> new ConcurrentHashMap<>());
        var cp = event.getChunk().getPos();
        if (pending.size() < MAX_KNOWN_VENT_CHUNKS || pending.containsKey(cp))
            pending.put(cp, new PendingChunk(level, List.copyOf(starts)));
    }

    private static void drainLoadedStarts(MinecraftServer server, State state, long deadline) {
        var pending = PENDING_STARTS.get(server);
        if (pending == null) return;
        int count = 0;
        for (var entry : pending.entrySet()) {
            if (count++ >= 32 || System.nanoTime() >= deadline) break;
            if (!pending.remove(entry.getKey(), entry.getValue())) continue;
            var data = entry.getValue();
            var serialization = StructurePieceSerializationContext.fromLevel(data.level);
            var known = new HashMap<ScannerTarget, List<BlockPos>>();
            for (var start : data.starts) {
                var origins = new ArrayList<BlockPos>();
                for (var piece : start.start.getPieces()) {
                    var tag = piece.createTag(serialization);
                    if (tag.contains("Origin", Tag.TAG_LONG)) origins.add(BlockPos.of(tag.getLong("Origin")));
                }
                if (!origins.isEmpty()) known.computeIfAbsent(start.target, ignored -> new ArrayList<>()).addAll(origins);
            }
            if (known.isEmpty()) continue;
            state.knownVents.remove(entry.getKey());
            state.knownVents.put(entry.getKey(), known);
            while (state.knownVents.size() > MAX_KNOWN_VENT_CHUNKS)
                state.knownVents.remove(state.knownVents.keySet().iterator().next());
            state.knownRevision++;
        }
    }

    private static void refreshKnown(Job job, State state) {
        if (job.knownRevision == state.knownRevision) return;
        job.knownRevision = state.knownRevision;
        // Include observed starts from older grids, deduplicating every origin with
        // current placement candidates. Each is still checked against an actual core.
        for (var chunk : state.knownVents.values()) {
            var origins = chunk.get(job.target);
            if (origins != null) origins.forEach(job::add);
        }
    }

    private static boolean hasPendingStarts(Job job) {
        var pending = PENDING_STARTS.get(job.level.getServer());
        if (pending == null || pending.isEmpty()) return false;
        long radiusSquared = (long) job.target.radius() * job.target.radius();
        for (var data : pending.values()) {
            if (data.level != job.level) continue;
            for (var start : data.starts) {
                if (start.target != job.target) continue;
                // Saved bounds include the cores, including old-grid companions.
                // Reading bounds needs neither piece serialization nor chunk loading.
                var box = start.start.getBoundingBox();
                long dx = Math.max(0, Math.max(box.minX() - job.origin.getX(), job.origin.getX() - box.maxX()));
                long dz = Math.max(0, Math.max(box.minZ() - job.origin.getZ(), job.origin.getZ() - box.maxZ()));
                if (dx * dx + dz * dz <= radiusSquared) return true;
            }
        }
        return false;
    }

    private static void reject(ServerPlayer player, ScannerNetwork.Request request, String reason, int retry) {
        PacketDistributor.sendToPlayer(player, new ScannerNetwork.Result(request.requestId(), request.targetId(),
                ScannerNetwork.Status.REJECTED, player.level().dimension(), BlockPos.ZERO, 0,
                "scanner.create_water_vents." + reason, retry));
    }

    private static void reply(ServerPlayer player, Job job, ScannerNetwork.Status status, BlockPos pos, String reason, int retry) {
        PacketDistributor.sendToPlayer(player, new ScannerNetwork.Result(job.requestId, job.target.id(), status,
                job.level.dimension(), pos, status == ScannerNetwork.Status.FOUND ? PING_TICKS : 0,
                reason.isEmpty() ? "" : status == ScannerNetwork.Status.FOUND ? reason
                        : "scanner.create_water_vents." + reason, retry));
    }

    @SubscribeEvent
    public static void logout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        var state = STATES.get(player.server);
        if (state == null) return;
        var job = state.jobs.get(player.getUUID());
        if (job != null) discard(state, player.getUUID(), job);
        state.nextScan.remove(player.getUUID());
        state.lastPacket.remove(player.getUUID());
    }

    @SubscribeEvent
    public static void stopped(ServerStoppedEvent event) {
        var state = STATES.remove(event.getServer());
        if (state != null) for (var entry : new ArrayList<>(state.jobs.entrySet())) discard(state, entry.getKey(), entry.getValue());
        PENDING_STARTS.remove(event.getServer());
    }

    private ScannerSearch() {}
}
