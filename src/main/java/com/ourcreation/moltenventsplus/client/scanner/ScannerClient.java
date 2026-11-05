package com.ourcreation.moltenventsplus.client.scanner;

import com.mojang.blaze3d.systems.RenderSystem;
import com.ourcreation.moltenventsplus.MoltenVentsPlus;
import com.ourcreation.moltenventsplus.scanner.ScannerClientHooks;
import com.ourcreation.moltenventsplus.scanner.ScannerNetwork;
import com.ourcreation.moltenventsplus.scanner.ScannerSearch;
import com.ourcreation.moltenventsplus.scanner.ScannerTarget;
import com.ourcreation.moltenventsplus.scanner.VentSurveyorItem;
import com.ourcreation.moltenventsplus.vent.VentTier;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.locale.Language;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.joml.Vector4f;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/** Temporary local signals, using bounded server searches when the server supports them. */
@EventBusSubscriber(modid = MoltenVentsPlus.MODID, value = Dist.CLIENT)
public final class ScannerClient {
    private static final ResourceLocation DIRECTION_STRIP = graphic("surveyor_direction_strip");
    private static final ResourceLocation POINTER = graphic("surveyor_pointer");
    private static final ResourceLocation PIN = graphic("surveyor_pin");
    private static final ResourceLocation PULSE = graphic("surveyor_pulse");
    private static ScannerTarget selected = ScannerTarget.WATER_ASURINE;
    private static ScannerTarget activeTarget = ScannerTarget.WATER_ASURINE;
    private static VentTier availableTier = VentTier.WATER;
    private static int sequence, pending = -1, cooldown, foundCount;
    private static ClientScannerSearch search;
    private static boolean remote;
    private static int remoteAge;
    private static BlockPos scanOrigin;
    private static final Map<BlockPos, Ping> signals = new LinkedHashMap<>();
    private static final Set<BlockPos> received = new HashSet<>();
    private static ResourceKey<Level> scanDimension;

    private static final class Ping {
        final BlockPos position;
        final Component label;
        final int lifetime;
        int age;
        boolean projected;
        float projectedX, projectedY;

        Ping(BlockPos position, Component label, int lifetime) {
            this.position = position;
            this.label = label;
            this.lifetime = lifetime;
        }

        float opacity(float partial) { return Mth.clamp((lifetime - age - partial) / 100F, 0, 1); }
        double distance() { return Minecraft.getInstance().player.position().distanceTo(Vec3.atCenterOf(position)); }
    }

    public static void open(VentTier tier) {
        var mc = Minecraft.getInstance();
        if (mc.player != null && mc.screen == null) {
            availableTier = tier;
            selected = ScannerTarget.of(tier, selected.stone());
            mc.setScreen(new ScannerScreen(tier));
        }
    }

    static void select(ScannerTarget target) { selected = target; }

    public static void stopScan() { clear(); }

    @SubscribeEvent
    public static void usingBlock(PlayerInteractEvent.RightClickBlock event) { stopForInteraction(event); }

    @SubscribeEvent
    public static void usingEntity(PlayerInteractEvent.EntityInteract event) { stopForInteraction(event); }

    @SubscribeEvent
    public static void usingEntityPart(PlayerInteractEvent.EntityInteractSpecific event) { stopForInteraction(event); }

    private static void stopForInteraction(PlayerInteractEvent event) {
        var mc = Minecraft.getInstance();
        if (event.getEntity() != mc.player || !(event.getItemStack().getItem() instanceof VentSurveyorItem)) return;
        stopScan();
    }

    static void scan(ScannerTarget target) {
        var mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || !canScan() || target.tier() != availableTier
                || !mc.player.isAlive() || mc.player.isSpectator() || !mc.level.dimension().equals(Level.OVERWORLD)) return;
        boolean held = mc.player.getMainHandItem().getItem() instanceof VentSurveyorItem main && main.supports(target)
                || mc.player.getOffhandItem().getItem() instanceof VentSurveyorItem off && off.supports(target);
        if (!held) return;
        selected = activeTarget = target;
        clearSignals();
        scanDimension = mc.level.dimension();
        foundCount = 0;
        pending = sequence = sequence == Integer.MAX_VALUE ? 1 : sequence + 1;
        cooldown = ScannerSearch.COOLDOWN_TICKS;
        scanOrigin = mc.player.blockPosition().immutable();
        remoteAge = 0;
        remote = mc.getConnection() != null && mc.getConnection().hasChannel(ScannerNetwork.Request.TYPE);
        if (remote) PacketDistributor.sendToServer(new ScannerNetwork.Request(target.id(), pending));
        else search = new ClientScannerSearch(mc.level, scanOrigin, target, mc.options.renderDistance().get());
        mc.setScreen(null);
        mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.BEACON_POWER_SELECT, 1.35F, 0.45F));
    }

    private static void found(BlockPos position) {
        var mc = Minecraft.getInstance();
        if (mc.level == null || foundCount >= activeTarget.maxResults() || !received.add(position)) return;
        var ping = new Ping(position, activeTarget.label(),
                ScannerSearch.PING_TICKS);
        signals.put(position, ping);
        foundCount++;
        ScannerMapBridge.show(scanDimension, position, ping.label, ping.lifetime);
        sound(1.4F, 0.55F);
    }

    private static void result(ScannerNetwork.Result result) {
        var mc = Minecraft.getInstance();
        if (!remote || pending < 0 || result.requestId() != pending || !result.targetId().equals(activeTarget.id())
                || mc.level == null || !mc.level.dimension().equals(result.dimension())
                || !result.dimension().equals(scanDimension)) return;
        remoteAge = 0;
        switch (result.status()) {
            case SEARCHING -> { }
            case FOUND -> {
                long dx = (long) result.position().getX() - scanOrigin.getX();
                long dz = (long) result.position().getZ() - scanOrigin.getZ();
                if (dx * dx + dz * dz <= (long) activeTarget.radius() * activeTarget.radius()
                        && mc.level.getWorldBorder().isWithinBounds(result.position())) found(result.position());
            }
            case COMPLETE, NOT_FOUND -> { pending = -1; remote = false; }
            case REJECTED -> {
                pending = -1;
                remote = false;
                cooldown = Math.max(cooldown, result.retryTicks());
                if (mc.player != null && !result.reason().isEmpty()
                        && !result.reason().equals("scanner.create_water_vents.cancelled"))
                    mc.player.displayClientMessage(Component.translatable(result.reason()), true);
            }
        }
    }

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        var mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) { clear(); return; }
        if (mc.isPaused()) return;
        if (scanDimension != null && !mc.level.dimension().equals(scanDimension)) { clear(); return; }
        if (cooldown > 0) cooldown--;
        ScannerMapBridge.tick();
        if (remote && ++remoteAge > ScannerSearch.MAX_SEARCH_TICKS + 200) { clear(); return; }
        if (search != null) {
            if (!search.belongsTo(mc.level)) { clear(); return; }
            search.tick(ScannerClient::found);
            if (search.isDone()) {
                search = null;
                pending = -1;
            }
        }
        signals.values().removeIf(ping -> ++ping.age >= ping.lifetime);
    }

    private static void sound(float pitch, float volume) {
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.NOTE_BLOCK_BIT.value(), pitch, volume));
    }

    @SubscribeEvent
    public static void logout(ClientPlayerNetworkEvent.LoggingOut event) { clear(); }

    private static void clearSignals() {
        signals.clear();
        received.clear();
        ScannerMapBridge.clear();
    }

    private static void clear() {
        var mc = Minecraft.getInstance();
        if (remote && pending >= 0 && mc.getConnection() != null
                && mc.getConnection().hasChannel(ScannerNetwork.Cancel.TYPE))
            PacketDistributor.sendToServer(new ScannerNetwork.Cancel(pending));
        clearSignals();
        scanDimension = null;
        pending = -1;
        cooldown = 0;
        search = null;
        remote = false;
        remoteAge = 0;
        scanOrigin = null;
        foundCount = 0;
    }

    static boolean canScan() { return pending < 0 && cooldown <= 0; }

    @SubscribeEvent
    public static void project(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_LEVEL) return;
        for (var ping : signals.values()) {
            var relative = Vec3.atCenterOf(ping.position).add(0, 1, 0).subtract(event.getCamera().getPosition());
            var point = new Vector4f((float) relative.x, (float) relative.y, (float) relative.z, 1);
            event.getModelViewMatrix().transform(point);
            event.getProjectionMatrix().transform(point);
            ping.projected = point.w > 0.01F;
            if (ping.projected) {
                ping.projectedX = point.x / point.w;
                ping.projectedY = point.y / point.w;
            }
        }
    }

    static void render(GuiGraphics g, DeltaTracker delta) {
        var mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || mc.options.hideGui || mc.screen != null) return;
        if (pending < 0 && signals.isEmpty()) return;
        int mid = g.guiWidth() / 2;
        graphic(g, DIRECTION_STRIP, mid - 69, 3, 139, 1, 0, 1, 1);
        if (signals.isEmpty()) return;
        float partial = delta.getGameTimeDeltaPartialTick(false);
        Ping focused = null;
        float focusDistance = .25F;
        for (var ping : signals.values()) {
            float alpha = ping.opacity(partial);
            if (alpha < .02F) continue;
            int pointer = mid + Math.round(Mth.clamp(bearing(ping) / 90, -1, 1) * 65);
            graphic(g, POINTER, pointer - 3, 0, 7, 7, 0, 7, alpha);
            if (!ping.projected) continue;
            int x = Mth.clamp(Math.round((ping.projectedX + 1) * .5F * g.guiWidth()), 15, g.guiWidth() - 15);
            int y = Mth.clamp(Math.round((1 - ping.projectedY) * .5F * g.guiHeight()), 20, Math.max(20, g.guiHeight() - 65));
            float wave = ((ping.age + partial) % 32) / 32;
            int pulseFrame = Math.round(wave * 14);
            graphic(g, PULSE, x - 20, y - 20, 41, 41, pulseFrame * 41, 615,
                    alpha * (1 - wave) * .65F);
            graphic(g, PIN, x - 6, y - 6, 13, 13, 0, 13, alpha);
            float focus = ping.projectedX * ping.projectedX + ping.projectedY * ping.projectedY;
            if (focus < focusDistance) {
                focusDistance = focus;
                focused = ping;
            }
        }
        // Keep every location visible without piling dozens of distance labels together.
        if (focused != null) {
            int x = Mth.clamp(Math.round((focused.projectedX + 1) * .5F * g.guiWidth()), 15, g.guiWidth() - 15);
            int y = Mth.clamp(Math.round((1 - focused.projectedY) * .5F * g.guiHeight()), 20, Math.max(20, g.guiHeight() - 65));
            var label = Component.translatable("scanner.create_water_vents.signal", focused.label, Math.round(focused.distance()));
            var text = mc.font.substrByWidth(label, Math.max(40, g.guiWidth() - 30));
            int half = mc.font.width(text) / 2;
            int tx = Mth.clamp(x, half + 5, Math.max(half + 5, g.guiWidth() - half - 5));
            float alpha = focused.opacity(partial);
            g.drawCenteredString(mc.font, Language.getInstance().getVisualOrder(text), tx, y + 12, color(alpha, 0xD7FFF0));
        }
    }

    private static float bearing(Ping ping) {
        var camera = Minecraft.getInstance().gameRenderer.getMainCamera();
        double dx = ping.position.getX() + .5 - camera.getPosition().x;
        double dz = ping.position.getZ() + .5 - camera.getPosition().z;
        return Mth.wrapDegrees((float) Math.toDegrees(Math.atan2(-dx, dz)) - camera.getYRot());
    }

    private static int color(float alpha, int rgb) { return Mth.clamp(Math.round(alpha * 255), 0, 255) << 24 | rgb; }

    private static ResourceLocation graphic(String name) {
        return ResourceLocation.fromNamespaceAndPath(MoltenVentsPlus.MODID, "textures/gui/" + name + ".png");
    }

    /** Position and fading are dynamic; all signal artwork and colors belong to resource packs. */
    private static void graphic(GuiGraphics g, ResourceLocation texture, int x, int y, int width, int height,
                                int frameY, int textureHeight, float opacity) {
        g.flush();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        g.setColor(1, 1, 1, Mth.clamp(Math.round(opacity * 255), 0, 255) / 255F);
        g.blit(texture, x, y, 0, frameY, width, height, width, textureHeight);
        g.setColor(1, 1, 1, 1);
        RenderSystem.disableBlend();
    }

    @EventBusSubscriber(modid = MoltenVentsPlus.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static final class Setup {
        @SubscribeEvent
        public static void setup(FMLClientSetupEvent event) {
            event.enqueueWork(() -> {
                ScannerClientHooks.setOpen(ScannerClient::open);
                ScannerClientHooks.setStop(ScannerClient::stopScan);
                ScannerClientHooks.setResult(ScannerClient::result);
            });
        }

        @SubscribeEvent
        public static void layers(RegisterGuiLayersEvent event) {
            event.registerAboveAll(ResourceLocation.fromNamespaceAndPath(MoltenVentsPlus.MODID, "vent_signal"), ScannerClient::render);
        }
    }

    private ScannerClient() {}
}
