package com.ourcreation.moltenventsplus.client.scanner;

import com.ourcreation.moltenventsplus.scanner.ScannerSearch;
import com.ourcreation.moltenventsplus.scanner.ScannerTarget;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.status.ChunkStatus;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.function.Consumer;

/** Reads only terrain already received by this client. Never asks a server to locate or load anything. */
final class ClientScannerSearch {
    static final int BLOCKS_PER_TICK = 4096;
    static final int SECTIONS_PER_TICK = 1024;
    static final int CHUNKS_PER_TICK = 64;
    static final long WORK_BUDGET_NANOS = 1_000_000L;

    private final ClientLevel level;
    private final ScannerTarget target;
    private final BlockPos origin;
    private final List<ChunkPos> chunks = new ArrayList<>();
    private final Set<BlockPos> seen = new HashSet<>();
    private final PriorityQueue<BlockPos> signals;
    private final BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
    private int age, nextSignal, found, chunkIndex, sectionIndex, blockIndex;
    private ChunkPos current;
    private boolean done;

    ClientScannerSearch(ClientLevel level, BlockPos origin, ScannerTarget target, int viewDistance) {
        this.level = level;
        this.origin = origin.immutable();
        this.target = target;
        signals = new PriorityQueue<>(Comparator.comparingLong(this::distanceSquared)
                .thenComparingInt(BlockPos::getX).thenComparingInt(BlockPos::getZ).thenComparingInt(BlockPos::getY));
        // The cache contains only this bounded square of full client chunks. Checking false below
        // returns null for absent terrain; it cannot generate chunks or issue network requests.
        int range = Math.min((target.radius() + 15) / 16, Math.min(64, Math.max(2, viewDistance) + 3));
        int cx = origin.getX() >> 4, cz = origin.getZ() >> 4;
        for (int x = cx - range; x <= cx + range; x++) {
            for (int z = cz - range; z <= cz + range; z++) {
                var chunk = new ChunkPos(x, z);
                if (lowerDistanceSquared(chunk) <= (long) target.radius() * target.radius() && loaded(x, z) != null) chunks.add(chunk);
            }
        }
        chunks.sort(Comparator.comparingLong(this::lowerDistanceSquared));
    }

    boolean isDone() { return done; }
    boolean belongsTo(ClientLevel currentLevel) { return level == currentLevel; }

    void tick(Consumer<BlockPos> report) {
        if (done) return;
        age++;
        if (age >= ScannerSearch.MAX_SEARCH_TICKS) { done = true; return; }
        if (age < target.warmupTicks()) return;
        if (found + signals.size() < target.maxResults()) inspect();
        while (!signals.isEmpty() && age >= nextSignal) {
            var position = signals.remove();
            var chunk = loaded(position.getX() >> 4, position.getZ() >> 4);
            // Terrain can unload, or the core can be mined, between discovery and its paced ping.
            if (chunk == null || !target.matches(chunk.getBlockState(position))) continue;
            report.accept(position);
            found++;
            nextSignal = age + target.signalIntervalTicks();
            if (found >= target.maxResults()) { signals.clear(); done = true; return; }
        }
        if (chunkIndex >= chunks.size() && current == null && signals.isEmpty()) done = true;
    }

    private void inspect() {
        long deadline = System.nanoTime() + WORK_BUDGET_NANOS;
        int blocks = 0, sections = 0, visitedChunks = 0;
        while (blocks < BLOCKS_PER_TICK && sections < SECTIONS_PER_TICK
                && visitedChunks < CHUNKS_PER_TICK && System.nanoTime() < deadline
                && found + signals.size() < target.maxResults()) {
            if (current == null) {
                if (chunkIndex >= chunks.size()) return;
                current = chunks.get(chunkIndex++);
                sectionIndex = blockIndex = 0;
                visitedChunks++;
            }
            var chunk = loaded(current.x, current.z);
            if (chunk == null || sectionIndex >= chunk.getSections().length) { current = null; continue; }
            var section = chunk.getSection(sectionIndex);
            if (blockIndex == 0) {
                sections++;
                if (section.hasOnlyAir() || !section.maybeHas(target::matches)) { sectionIndex++; continue; }
            }
            int index = blockIndex++;
            blocks++;
            int x = index & 15, z = (index >> 4) & 15, y = index >> 8;
            if (target.matches(section.getBlockState(x, y, z))) {
                cursor.set((current.x << 4) + x, level.getMinBuildHeight() + (sectionIndex << 4) + y,
                        (current.z << 4) + z);
                if (distanceSquared(cursor) <= (long) target.radius() * target.radius()
                        && level.getWorldBorder().isWithinBounds(cursor)) {
                    var position = cursor.immutable();
                    if (seen.add(position)) signals.add(position);
                }
            }
            if (blockIndex == 4096) { blockIndex = 0; sectionIndex++; }
        }
    }

    private LevelChunk loaded(int x, int z) {
        return level.getChunkSource().getChunk(x, z, ChunkStatus.FULL, false);
    }

    private long lowerDistanceSquared(ChunkPos chunk) {
        long dx = Math.max(0, Math.max(chunk.getMinBlockX() - origin.getX(), origin.getX() - chunk.getMaxBlockX()));
        long dz = Math.max(0, Math.max(chunk.getMinBlockZ() - origin.getZ(), origin.getZ() - chunk.getMaxBlockZ()));
        return dx * dx + dz * dz;
    }

    private long distanceSquared(BlockPos pos) {
        long dx = (long) pos.getX() - origin.getX(), dz = (long) pos.getZ() - origin.getZ();
        return dx * dx + dz * dz;
    }
}
