package com.ourcreation.moltenventsplus.vent;

import com.mojang.brigadier.Command;
import com.simibubi.create.api.contraption.BlockMovementChecks;
import com.simibubi.create.api.contraption.BlockMovementChecks.CheckResult;

import net.minecraft.commands.Commands;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.PistonEvent;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/** Protects vents from mining and lets operators opt into relocating them in this world. */
public final class VentSettings {
    public static final GameRules.Key<GameRules.BooleanValue> MOLTEN_VENTS_MOVABLE =
            GameRules.register("cnwMoltenVentsMovable", GameRules.Category.MISC, GameRules.BooleanValue.create(false));

    private static final Set<ResourceLocation> BLOCKS =
            Stream.of(
                            "active_molten_asurine",
                            "active_molten_veridium",
                            "active_molten_crimsite",
                            "active_molten_ochrum",
                            "active_molten_scorchia",
                            "active_molten_scoria",
                            "dormant_molten_asurine",
                            "dormant_molten_veridium",
                            "dormant_molten_crimsite",
                            "dormant_molten_ochrum",
                            "dormant_molten_scorchia",
                            "dormant_molten_scoria")
                    .map(path -> ResourceLocation.fromNamespaceAndPath("molten_vents", path))
                    .collect(Collectors.toUnmodifiableSet());

    private VentSettings() {}

    public static void register() {
        NeoForge.EVENT_BUS.register(VentSettings.class);
        BlockMovementChecks.registerMovementAllowedCheck(
                (state, level, pos) -> isVent(state) ? CheckResult.of(movementEnabled(level)) : CheckResult.PASS);
    }

    public static boolean isVent(BlockState state) {
        return isProtected(state.getBlock());
    }

    public static boolean movementEnabled(Level level) {
        return level.getGameRules().getBoolean(MOLTEN_VENTS_MOVABLE);
    }

    @SubscribeEvent
    public static void commands(RegisterCommandsEvent event) {
        for (boolean enabled : new boolean[] {true, false}) {
            for (String name : new String[] {"movable_vents", "movable_molten_vents"}) {
                event.getDispatcher()
                    .register(
                            Commands.literal(enabled ? "enable" : "disable")
                                    .requires(source -> source.hasPermission(2))
                                    .then(
                                            Commands.literal(name)
                                                    .executes(
                                                            context -> {
                                                                var source = context.getSource();
                                                                var server = source.getServer();
                                                                server.getGameRules()
                                                                        .getRule(MOLTEN_VENTS_MOVABLE)
                                                                        .set(enabled, server);
                                                                source.sendSuccess(
                                                                        () ->
                                                                                Component.translatable(
                                                                                        enabled
                                                                                                ? "command.create_water_vents.movement.enabled"
                                                                                                : "command.create_water_vents.movement.disabled"),
                                                                        true);
                                                                return Command.SINGLE_SUCCESS;
                                                            })));
            }
        }
    }

    @SubscribeEvent
    public static void onBreak(BlockEvent.BreakEvent event) {
        if (!event.getPlayer().isCreative() && isProtected(event.getState().getBlock())) event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onPistonMove(PistonEvent.Pre event) {
        if (event.getLevel() instanceof Level level && movementEnabled(level)) return;
        var helper = event.getStructureHelper();
        if (helper == null || !helper.resolve()) return;
        boolean protectedBlockMoved =
                Stream.concat(helper.getToPush().stream(), helper.getToDestroy().stream())
                        .anyMatch(pos -> isProtected(event.getLevel().getBlockState(pos).getBlock()));
        if (protectedBlockMoved) event.setCanceled(true);
    }

    private static boolean isProtected(net.minecraft.world.level.block.Block block) {
        return block instanceof VentBlock || block instanceof com.ourcreation.moltenventsplus.compat.ResourceVentBlock
                || BLOCKS.contains(BuiltInRegistries.BLOCK.getKey(block));
    }
}
