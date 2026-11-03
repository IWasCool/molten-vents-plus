package com.ourcreation.moltenventsplus.client.scanner;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.neoforged.fml.ModList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.Optional;

/** Optional client map pins. Only this scanner's transient objects are ever removed. */
public final class ScannerMapBridge {
    private static final Logger LOGGER = LoggerFactory.getLogger(ScannerMapBridge.class);
    private static final ResourceLocation ORIGIN = ResourceLocation.fromNamespaceAndPath("create_water_vents", "vent_surveyor");
    private static final List<Bridge> bridges = new ArrayList<>();
    private static ClientLevel ownerLevel;
    private static final List<Signal> signals = new ArrayList<>();
    private static boolean initialized;

    private ScannerMapBridge() {}

    /** Called on the client thread with the same duration as the scanner's own ping. */
    public static void show(ResourceKey<Level> targetDimension, BlockPos target, Component name, int lifetimeTicks) {
        var level = Minecraft.getInstance().level;
        if (ownerLevel != null && level != ownerLevel) clear();
        if (level == null || !level.dimension().equals(targetDimension) || lifetimeTicks <= 0) return;
        if (!initialized) {
            initialized = true;
            if (ModList.get().isLoaded("xaerominimap")) bridges.add(new XaeroBridge());
            if (ModList.get().isLoaded("ftbchunks")) bridges.add(new FtbBridge());
        }
        // Network duplicate results must neither duplicate a pin nor restart its lifetime.
        for (var signal : signals)
            if (signal.dimension.equals(targetDimension) && signal.position.equals(target)) return;
        ownerLevel = level;
        var signal = new Signal(targetDimension, target.immutable(), name.getString(), Math.min(1200, lifetimeTicks));
        signals.add(signal);
        for (var bridge : bridges) bridge.create(signal);
    }

    /** Call exactly once per client tick; each streamed result keeps its own lifetime. */
    public static void tick() {
        if (signals.isEmpty()) return;
        if (Minecraft.getInstance().level != ownerLevel) {
            clear();
            return;
        }
        var iterator = signals.iterator();
        while (iterator.hasNext()) {
            var signal = iterator.next();
            if (--signal.remaining <= 0) {
                for (var bridge : bridges) bridge.clear(signal);
                iterator.remove();
                continue;
            }
            signal.elapsed++;
            for (var bridge : bridges) {
                // Map managers can become ready a little later than the client world.
                if (signal.elapsed % 10 == 0) bridge.create(signal);
                bridge.fade(signal, Math.min(1F, signal.remaining / 100F));
            }
        }
        if (signals.isEmpty()) ownerLevel = null;
    }

    /** New scans and logout remove all scanner-owned pins, preserving user waypoints. */
    public static void clear() {
        for (var signal : signals) for (var bridge : bridges) bridge.clear(signal);
        signals.clear();
        ownerLevel = null;
    }

    private static final class Signal {
        private final ResourceKey<Level> dimension;
        private final BlockPos position;
        private final String label;
        private final Map<Bridge, Pin> pins = new IdentityHashMap<>();
        private int remaining;
        private int elapsed;

        private Signal(ResourceKey<Level> dimension, BlockPos position, String label, int lifetime) {
            this.dimension = dimension;
            this.position = position;
            this.label = label;
            this.remaining = lifetime;
        }
    }

    private abstract static class Bridge {
        private final String modName;
        private boolean unavailable;

        Bridge(String modName) { this.modName = modName; }

        abstract Pin add(ResourceKey<Level> dim, BlockPos pos, String name) throws ReflectiveOperationException;

        final void create(Signal signal) {
            if (unavailable || signal.pins.containsKey(this)) return;
            try {
                var pin = add(signal.dimension, signal.position, signal.label);
                if (pin != null) signal.pins.put(this, pin);
            } catch (ReflectiveOperationException | LinkageError | RuntimeException error) {
                unavailable(error);
            }
        }

        final void fade(Signal signal, float brightness) {
            var pin = signal.pins.get(this);
            if (pin == null) return;
            try {
                pin.fade(brightness);
            } catch (ReflectiveOperationException | LinkageError | RuntimeException error) {
                unavailable(error);
                clear(signal);
            }
        }

        final void clear(Signal signal) {
            var previous = signal.pins.remove(this);
            if (previous == null) return;
            try {
                previous.remove();
            } catch (ReflectiveOperationException | LinkageError | RuntimeException error) {
                unavailable(error);
            }
        }

        private void unavailable(Throwable error) {
            if (!unavailable) LOGGER.warn("Vent Surveyor could not use {} map pins; the scanner's own ping remains available", modName, error);
            unavailable = true;
        }
    }

    private interface Pin {
        void fade(float brightness) throws ReflectiveOperationException;
        void remove() throws ReflectiveOperationException;
    }

    /** Xaero 26.6 uses a separate, unsaved third-party bucket for each origin and dimension. */
    private static final class XaeroBridge extends Bridge {
        private Method currentSession, waypointsManager, worldManager, autoWorld, worldDimension, container;
        private Method thirdPartyManager, originGroup, groupAdd, groupClear, setTemporary, setVisibility, setColor;
        private Field worldMapSupport;
        private Method refreshWorldMap;
        private Constructor<?> constructor;
        private Object gold, gray, darkGray, global;
        private final Map<Object, Map<String, Object>> groups = new IdentityHashMap<>();

        XaeroBridge() { super("Xaero's Minimap"); }

        private void bind() throws ReflectiveOperationException {
            if (currentSession != null) return;
            var session = Class.forName("xaero.common.XaeroMinimapSession");
            var minimapSession = Class.forName("xaero.hud.minimap.module.MinimapSession");
            var manager = Class.forName("xaero.hud.minimap.world.MinimapWorldManager");
            var world = Class.forName("xaero.hud.minimap.world.MinimapWorld");
            var worldContainer = Class.forName("xaero.hud.minimap.world.container.MinimapWorldContainer");
            var thirdParty = Class.forName("xaero.hud.minimap.waypoint.thirdparty.ThirdPartyWaypointManager");
            var group = Class.forName("xaero.hud.minimap.waypoint.thirdparty.ThirdPartyWaypoints");
            var waypoint = Class.forName("xaero.common.minimap.waypoints.Waypoint");
            var color = Class.forName("xaero.hud.minimap.waypoint.WaypointColor");
            var visibility = Class.forName("xaero.common.minimap.waypoints.WaypointVisibilityType");
            gold = color.getField("GOLD").get(null);
            gray = color.getField("GRAY").get(null);
            darkGray = color.getField("DARK_GRAY").get(null);
            global = visibility.getField("GLOBAL").get(null);
            waypointsManager = session.getMethod("getWaypointsManager");
            worldManager = minimapSession.getMethod("getWorldManager");
            autoWorld = manager.getMethod("getAutoWorld");
            worldDimension = world.getMethod("getDimId");
            container = world.getMethod("getContainer");
            thirdPartyManager = worldContainer.getMethod("getThirdPartyWaypointManager");
            originGroup = thirdParty.getMethod("get", ResourceLocation.class);
            groupAdd = group.getMethod("add", String.class, waypoint);
            groupClear = group.getMethod("clear");
            constructor = waypoint.getConstructor(int.class, int.class, int.class, String.class, String.class, color);
            setTemporary = waypoint.getMethod("setTemporary", boolean.class);
            setVisibility = waypoint.getMethod("setVisibility", visibility);
            setColor = waypoint.getMethod("setWaypointColor", color);
            if (ModList.get().isLoaded("xaeroworldmap")) {
                worldMapSupport = Class.forName("xaero.map.mods.SupportMods").getField("xaeroMinimap");
                refreshWorldMap = Class.forName("xaero.map.mods.SupportXaeroMinimap").getMethod("requestWaypointsRefresh");
            }
            currentSession = session.getMethod("getCurrentSession");
        }

        private void refreshWorldMap() throws ReflectiveOperationException {
            if (worldMapSupport == null) return;
            var support = worldMapSupport.get(null);
            if (support != null) refreshWorldMap.invoke(support);
        }

        private void rebuild(Object group, Map<String, Object> owned) throws ReflectiveOperationException {
            // This origin is exclusively ours. clear() changes memory without saving preferences.
            groupClear.invoke(group);
            for (var entry : owned.entrySet()) groupAdd.invoke(group, entry.getKey(), entry.getValue());
            refreshWorldMap();
        }

        @Override Pin add(ResourceKey<Level> dim, BlockPos pos, String name) throws ReflectiveOperationException {
            bind();
            var session = currentSession.invoke(null);
            if (session == null) return null;
            var manager = worldManager.invoke(waypointsManager.invoke(session));
            var world = autoWorld.invoke(manager);
            if (world == null || !dim.equals(worldDimension.invoke(world))) return null;
            var group = originGroup.invoke(thirdPartyManager.invoke(container.invoke(world)), ORIGIN);
            var owned = groups.computeIfAbsent(group, ignored -> new LinkedHashMap<>());
            // Stable per-position IDs avoid growing preference entries on repeated scans.
            String id = "scan_" + Long.toUnsignedString(pos.asLong(), 16);
            var waypoint = constructor.newInstance(pos.getX(), pos.getY(), pos.getZ(), name, "V", gold);
            setTemporary.invoke(waypoint, true);
            setVisibility.invoke(waypoint, global);
            owned.put(id, waypoint);
            try {
                rebuild(group, owned);
            } catch (ReflectiveOperationException | LinkageError | RuntimeException error) {
                owned.remove(id);
                try { rebuild(group, owned); }
                catch (ReflectiveOperationException | LinkageError | RuntimeException cleanup) { error.addSuppressed(cleanup); }
                if (owned.isEmpty()) groups.remove(group);
                throw error;
            }
            return new Pin() {
                private Object color = gold;

                @Override public void fade(float brightness) throws ReflectiveOperationException {
                    // Xaero exposes a fixed color palette rather than per-pin alpha/RGB.
                    var next = brightness > 0.66F ? gold : brightness > 0.33F ? gray : darkGray;
                    if (next != color) {
                        setColor.invoke(waypoint, next);
                        color = next;
                        refreshWorldMap();
                    }
                }

                @Override public void remove() throws ReflectiveOperationException {
                    // Never call remove(id): Xaero saves its root config from that API.
                    if (owned.get(id) != waypoint) return;
                    owned.remove(id);
                    rebuild(group, owned);
                    if (owned.isEmpty()) groups.remove(group);
                }
            };
        }
    }

    /** Public FTB Chunks API, available with transient waypoints in 2101.1.10 and newer. */
    private static final class FtbBridge extends Bridge {
        private Method clientApi, manager, all, addTransient, remove, getPosition, isTransient, setColor, refresh;
        private Method refreshWaypointIcon, refreshFullMap;

        FtbBridge() { super("FTB Chunks"); }

        private void bind() throws ReflectiveOperationException {
            if (clientApi != null) return;
            var api = Class.forName("dev.ftb.mods.ftbchunks.api.FTBChunksAPI");
            var client = Class.forName("dev.ftb.mods.ftbchunks.api.client.FTBChunksClientAPI");
            var waypointManager = Class.forName("dev.ftb.mods.ftbchunks.api.client.waypoint.WaypointManager");
            var waypoint = Class.forName("dev.ftb.mods.ftbchunks.api.client.waypoint.Waypoint");
            manager = client.getMethod("getWaypointManager", ResourceKey.class);
            refresh = client.getMethod("requestMinimapIconRefresh");
            all = waypointManager.getMethod("getAllWaypoints");
            addTransient = waypointManager.getMethod("addTransientWaypointAt", BlockPos.class, String.class);
            remove = waypointManager.getMethod("removeWaypoint", waypoint);
            getPosition = waypoint.getMethod("getPos");
            isTransient = waypoint.getMethod("isTransient");
            setColor = waypoint.getMethod("setColor", int.class);
            refreshWaypointIcon = Class.forName("dev.ftb.mods.ftbchunks.client.map.WaypointImpl").getMethod("refreshIcon");
            refreshFullMap = Class.forName("dev.ftb.mods.ftbchunks.client.gui.LargeMapScreen").getMethod("refreshIconsIfOpen");
            clientApi = api.getMethod("clientApi");
        }

        @Override Pin add(ResourceKey<Level> dim, BlockPos pos, String name) throws ReflectiveOperationException {
            bind();
            Object api;
            try {
                api = clientApi.invoke(null);
            } catch (InvocationTargetException early) {
                if (early.getCause() instanceof NullPointerException) return null;
                throw early;
            }
            var possibleManager = (Optional<?>) manager.invoke(api, dim);
            if (possibleManager.isEmpty()) return null;
            var waypointManager = possibleManager.get();
            // FTB compares waypoint identity by block position; never borrow an existing user pin.
            for (var existing : (Collection<?>) all.invoke(waypointManager))
                if (pos.equals(getPosition.invoke(existing))) return null;
            var waypoint = addTransient.invoke(waypointManager, pos, name);
            if (!owns(waypointManager, waypoint)) return null;
            var pin = new Pin() {
                private int color = 0xFFBD59;

                @Override public void fade(float brightness) throws ReflectiveOperationException {
                    if (!owns(waypointManager, waypoint)) return;
                    int next = ((int) (255 * brightness) << 16) | ((int) (189 * brightness) << 8) | (int) (89 * brightness);
                    if (next != color) {
                        setColor.invoke(waypoint, next);
                        refreshWaypointIcon.invoke(waypoint);
                        color = next;
                        refresh.invoke(api);
                        refreshFullMap.invoke(null);
                    }
                }

                @Override public void remove() throws ReflectiveOperationException {
                    // A player may replace/delete the pin or turn it into a saved waypoint while open.
                    if (owns(waypointManager, waypoint)) {
                        remove.invoke(waypointManager, waypoint);
                        refresh.invoke(api);
                        refreshFullMap.invoke(null);
                    }
                }
            };
            try {
                setColor.invoke(waypoint, 0xFFBD59);
                refreshWaypointIcon.invoke(waypoint);
                refresh.invoke(api);
                refreshFullMap.invoke(null);
                return pin;
            } catch (ReflectiveOperationException | LinkageError | RuntimeException error) {
                try { pin.remove(); }
                catch (ReflectiveOperationException | LinkageError | RuntimeException cleanup) { error.addSuppressed(cleanup); }
                throw error;
            }
        }

        private boolean owns(Object waypointManager, Object waypoint) throws ReflectiveOperationException {
            if (!(boolean) isTransient.invoke(waypoint)) return false;
            for (var existing : (Collection<?>) all.invoke(waypointManager))
                if (existing == waypoint) return true;
            return false;
        }
    }
}
