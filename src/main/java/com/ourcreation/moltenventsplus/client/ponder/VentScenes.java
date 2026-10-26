package com.ourcreation.moltenventsplus.client.ponder;

import com.ourcreation.moltenventsplus.vent.*;
import com.ourcreation.moltenventsplus.registry.VentRegistry;

import com.simibubi.create.foundation.ponder.CreateSceneBuilder;

import net.createmod.catnip.math.Pointing;
import net.createmod.ponder.api.PonderPalette;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.createmod.ponder.api.scene.Selection;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.Vec3;

import org.joml.Matrix4f;
import org.joml.Vector3f;

public final class VentScenes {
    private VentScenes() {}

    public static void activation(SceneBuilder builder, SceneBuildingUtil util, VentTier tier, String stone) {
        var scene = new CreateSceneBuilder(builder);
        scene.title(tier.id + "_vent_activation", "Using a " + tier.id + " vent");
        int center = tier == VentTier.SUPERHEATED ? 6 : 5;
        frame(scene, tier == VentTier.SUPERHEATED ? 13 : 11, 5.5F, -55, 145, 0.62F);
        BlockPos core = util.grid().at(center, 1, center);
        BlockPos output = core.east();
        BlockPos explosive = core.above();
        BlockState orestone = output(tier, stone);
        // Templates are captured from the corresponding production vent formations.
        scene.world()
                .modifyBlocks(util.select().everywhere(), state -> material(state, tier, stone), false);
        scene.world().setBlock(core, vent(tier, stone, false), false);
        scene.showBasePlate();
        scene.world().showSection(util.select().layersFrom(1), Direction.UP);
        scene.idle(20);
        caption(scene, util, core, 90, "Find the exposed core in the vent's center.");
        scene.idle(100);
        scene.overlay().showOutline(PonderPalette.GREEN, "core", util.select().position(core), 90);
        caption(scene, util, core, 90, "Clear space around the core. Keep the core intact.");
        scene.idle(20);
        scene.overlay()
                .showControls(util.vector().topOf(output.above()), Pointing.DOWN, 50)
                .leftClick()
                .withItem(new ItemStack(Items.IRON_PICKAXE));
        for (int z = center - 2; z <= center + 2; z++)
            for (int x = center - 2; x <= center + 2; x++) {
                for (int y = 4; y >= 1; y--) {
                    var surrounding = util.grid().at(x, y, z);
                    if (!surrounding.equals(core)) scene.world().destroyBlock(surrounding);
                }
                scene.idle(2);
            }
        scene.idle(30);
        scene.world().setBlock(explosive, Blocks.TNT.defaultBlockState(), true);
        scene.overlay()
                .showControls(util.vector().topOf(explosive), Pointing.DOWN, 40)
                .rightClick()
                .withItem(new ItemStack(Items.FLINT_AND_STEEL));
        caption(scene, util, explosive, 90, "Activate the core with an explosion.");
        scene.idle(40);
        scene.world().setBlock(explosive, Blocks.AIR.defaultBlockState(), false);
        var tnt =
                scene.world()
                        .createEntity(
                                level -> {
                                    var entity =
                                            new PrimedTnt(
                                                    level,
                                                    explosive.getX() + 0.5,
                                                    explosive.getY(),
                                                    explosive.getZ() + 0.5,
                                                    null);
                                    entity.setFuse(80);
                                    return entity;
                                });
        scene.idle(40);
        scene.world().modifyEntity(tnt, Entity::discard);
        scene.effects()
                .emitParticles(
                        util.vector().centerOf(explosive),
                        scene.effects().simpleParticleEmitter(ParticleTypes.EXPLOSION, Vec3.ZERO),
                        1,
                        1);
        scene.world().setBlock(core, vent(tier, stone, true), false);
        scene.effects().indicateSuccess(core);
        scene.idle(20);
        scene.overlay()
                .showControls(util.vector().topOf(output), Pointing.DOWN, 50)
                .rightClick()
                .withItem(new ItemStack(VentFluids.primary(tier).map(fluid -> fluid.getBucket()).orElse(Items.BUCKET)));
        scene.world().setBlock(output, fluid(tier), false);
        caption(scene, util, output, 100, "Place a lava source beside the active core.");
        scene.idle(45);
        convert(scene, util, output, orestone, tier);
        scene.idle(65);
        scene.overlay()
                .showControls(util.vector().topOf(output), Pointing.DOWN, 50)
                .leftClick()
                .withItem(new ItemStack(Items.IRON_PICKAXE));
        caption(scene, util, output, 90, "Mine the stone, then add another lava source.");
        breakStone(scene, output, 5);
        var drop =
                scene.world()
                        .createItemEntity(
                                util.vector().centerOf(output),
                                new Vec3(0.08, 0.12, 0),
                                new ItemStack(orestone.getBlock()));
        scene.idle(40);
        scene.world().modifyEntity(drop, Entity::discard);
        scene.world().setBlock(output, fluid(tier), false);
        scene.idle(25);
        convert(scene, util, output, orestone, tier);
        scene.idle(30);
        caption(
                scene,
                util,
                output,
                100,
                "Crush the vent's stone for resources. Check JEI for drops.");
        scene.idle(110);
        scene.markAsFinished();
    }

    public static void automation(SceneBuilder builder, SceneBuildingUtil util, VentTier tier, String stone) {
        var scene = new CreateSceneBuilder(builder);
        scene.title(tier.id + "_vent_automation", "Automating a " + tier.id + " vent");
        frame(scene, 11, 5.5F, -35, 145, 0.72F);
        BlockPos core = util.grid().at(3, 3, 3);
        BlockPos output = core.east();
        BlockPos drill = output.east();
        BlockPos pump = util.grid().at(4, 2, 7);
        BlockPos wheel = util.grid().at(9, 1, 8);
        BlockPos[] cauldrons = {util.grid().at(1, 1, 7), util.grid().at(1, 1, 9)};
        BlockState orestone = output(tier, stone);
        scene.world()
                .modifyBlocks(util.select().everywhere(), state -> material(state, tier, stone), false);
        if (tier == VentTier.SUPERHEATED) {
            scene.world().modifyBlocks(util.select().fromTo(0, 2, 6, 2, 4, 10),
                    state -> BuiltInRegistries.BLOCK.getKey(state.getBlock()).getPath().contains("pipe")
                            ? state : Blocks.AIR.defaultBlockState(), false);
            for (var tank : cauldrons) {
                for (int height = 1; height <= 3; height++)
                    scene.world().setBlock(tank.above(height), Blocks.AIR.defaultBlockState(), false);
                scene.world().setBlock(tank, block("create", "fluid_tank"), false);
                fillTank(scene, tank, tier, 4000);
            }
        }
        scene.world().setBlock(core, vent(tier, stone, true), false);
        scene.world().setKineticSpeed(util.select().everywhere(), 0);
        scene.showBasePlate();
        scene.world().showSection(util.select().fromTo(3, 1, 3, 4, 3, 3), Direction.UP);
        scene.idle(20);
        caption(scene, util, core, 90, "Leave one space beside the active core.");
        scene.overlay()
                .showOutline(
                        PonderPalette.GREEN,
                        "generation_space",
                        util.select().position(output),
                        90);
        scene.idle(100);

        Selection generators =
                util.select()
                        .fromTo(0, 1, 6, 2, 4, 10)
                        .substract(util.select().fromTo(2, 1, 7, 2, 2, 9));
        scene.world().showSection(generators, Direction.UP);
        caption(
                scene,
                util,
                cauldrons[0].above(),
                120,
                "Use any lava supply. This example uses dripstone cauldrons.");
        if (tier != VentTier.SUPERHEATED)
            for (var cauldron : cauldrons) drip(scene, util, cauldron, tier, 70);
        scene.idle(60);
        if (tier != VentTier.SUPERHEATED)
            for (var cauldron : cauldrons)
                scene.world().setBlock(cauldron, cauldron(tier), false);
        scene.idle(70);

        Selection plumbing =
                util.select().fromTo(2, 1, 7, 4, 2, 9).add(util.select().fromTo(4, 2, 4, 4, 3, 6));
        scene.world().showSection(plumbing, Direction.UP);
        caption(scene, util, pump, 110, "Pump lava into the space beside the core.");
        scene.overlay()
                .showBigLine(
                        PonderPalette.RED,
                        util.vector().centerOf(cauldrons[0]),
                        util.vector().centerOf(pump),
                        100);
        scene.idle(120);

        scene.world().showSection(util.select().fromTo(5, 1, 3, 10, 3, 10), Direction.UP);
        scene.world().setKineticSpeed(util.select().everywhere(), -8);
        scene.world()
                .setKineticSpeed(
                        util.select()
                                .position(6, 2, 3)
                                .add(util.select().position(pump))
                                .add(util.select().position(5, 1, 7))
                                .add(util.select().fromTo(5, 1, 8, 6, 1, 8)),
                        8);
        scene.world().propagatePipeChange(pump);
        scene.effects().rotationDirectionIndicator(wheel);
        caption(scene, util, drill, 110, "Power the drill to harvest the generated stone.");
        scene.idle(120);
        farmCycle(scene, util, output, cauldrons[0], orestone, tier);
        farmCycle(scene, util, output, cauldrons[1], orestone, tier);
        scene.idle(10);
        caption(
                scene,
                util,
                output.below(),
                110,
                "Collect the stone below the drill, then process it.");
        farmCycle(scene, util, output, cauldrons[0], orestone, tier);
        farmCycle(scene, util, output, cauldrons[1], orestone, tier);
        scene.idle(15);
        scene.markAsFinished();
    }

    private static void farmCycle(
            CreateSceneBuilder scene,
            SceneBuildingUtil util,
            BlockPos output,
            BlockPos cauldron,
            BlockState stone, VentTier tier) {
        if (tier == VentTier.SUPERHEATED) {
            fillTank(scene, cauldron, tier, 4000);
            scene.idle(10);
            fillTank(scene, cauldron, tier, 3000);
        } else {
            drip(scene, util, cauldron, tier, 10);
            scene.world().setBlock(cauldron, cauldron(tier), false);
            scene.idle(10);
            scene.world().setBlock(cauldron, Blocks.CAULDRON.defaultBlockState(), false);
        }
        scene.world().setBlock(output, fluid(tier), false);
        scene.idle(15);
        convert(scene, util, output, stone, tier);
        breakStone(scene, output, 3);
        var drop =
                scene.world()
                        .createItemEntity(
                                util.vector().centerOf(output),
                                new Vec3(0, -0.12, 0),
                                new ItemStack(stone.getBlock()));
        scene.idle(12);
        scene.world().modifyEntity(drop, Entity::discard);
        scene.effects().indicateSuccess(output.below(2));
    }

    private static void drip(
            CreateSceneBuilder scene, SceneBuildingUtil util, BlockPos cauldron, VentTier tier, int ticks) {
        scene.effects()
                .emitParticles(
                        util.vector().centerOf(cauldron.above()).add(0, -0.35, 0),
                        scene.effects()
                                .simpleParticleEmitter(
                                        tier == VentTier.WATER ? ParticleTypes.FALLING_WATER : ParticleTypes.FALLING_LAVA, new Vec3(0, -0.04, 0)),
                        0.15F,
                        ticks);
    }

    private static void convert(
            CreateSceneBuilder scene, SceneBuildingUtil util, BlockPos output, BlockState stone, VentTier tier) {
        scene.world().setBlock(output, stone, false);
        scene.effects()
                .emitParticles(
                        util.vector().centerOf(output),
                        scene.effects()
                                .simpleParticleEmitter(
                                        tier == VentTier.WATER ? ParticleTypes.SPLASH : ParticleTypes.LARGE_SMOKE, new Vec3(0, 0.04, 0)),
                        2,
                        4);
        scene.effects().indicateSuccess(output);
    }

    private static void breakStone(CreateSceneBuilder scene, BlockPos output, int delay) {
        for (int i = 0; i < 10; i++) {
            scene.world().incrementBlockBreakingProgress(output);
            scene.idle(delay);
        }
        scene.world().setBlock(output, Blocks.AIR.defaultBlockState(), true);
    }

    private static void caption(
            CreateSceneBuilder scene,
            SceneBuildingUtil util,
            BlockPos target,
            int duration,
            String text) {
        int guiHeight = Minecraft.getInstance().getWindow().getGuiScaledHeight();
        int top = Math.min(74, Math.round(guiHeight * 0.26F));
        scene.overlay()
                .showText(duration)
                .text(text)
                .attachKeyFrame()
                .independent(top + 8 - (guiHeight - 200) / 2);
        scene.overlay()
                .showOutline(
                        PonderPalette.GREEN,
                        "instruction_target",
                        util.select().position(target),
                        duration);
    }

    /** Fit the whole static layout between the title and controls at the current GUI scale. */
    private static void frame(
            CreateSceneBuilder scene,
            int size,
            float height,
            float pitch,
            float yaw,
            float maximumScale) {
        scene.configureBasePlate(0, 0, size);
        var window = Minecraft.getInstance().getWindow();
        float width = window.getGuiScaledWidth(), screenHeight = window.getGuiScaledHeight();
        float top = Math.min(72, screenHeight * 0.26F),
                bottom = screenHeight - Math.min(64, screenHeight * 0.22F);
        var matrix =
                new Matrix4f()
                        .rotateX((float) Math.toRadians(pitch))
                        .rotateY((float) Math.toRadians(yaw))
                        .scale(1, -1, 1)
                        .translate(-size / 2F, -1, -size / 2F);
        float minX = Float.POSITIVE_INFINITY, maxX = Float.NEGATIVE_INFINITY;
        float minY = Float.POSITIVE_INFINITY, maxY = Float.NEGATIVE_INFINITY;
        for (int x : new int[] {0, size})
            for (float y : new float[] {0, height})
                for (int z : new int[] {0, size}) {
                    var corner = matrix.transformPosition(new Vector3f(x, y, z));
                    minX = Math.min(minX, corner.x);
                    maxX = Math.max(maxX, corner.x);
                    minY = Math.min(minY, corner.y);
                    maxY = Math.max(maxY, corner.y);
                }
        float scale =
                Math.min(
                        maximumScale,
                        Math.min(
                                (width - 64) / (30 * (maxX - minX)),
                                (bottom - top) / (30 * (maxY - minY))));
        scene.scaleSceneView(scale);
        float center = (top + bottom - screenHeight) / (60 * scale);
        scene.setSceneOffsetY(
                ((minY + maxY) / 2 - center) / (float) Math.cos(Math.toRadians(pitch)));
        scene.addInstruction(
                s -> {
                    s.getTransform().xRotation.startWithValue(pitch);
                    s.getTransform().yRotation.startWithValue(yaw);
                });
    }

    private static BlockState material(BlockState state, VentTier tier, String stone) {
        var id = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        if (state.is(Blocks.LAVA)) return fluid(tier);
        if (state.is(Blocks.LAVA_CAULDRON)) return cauldron(tier);
        if (state.getBlock() instanceof WeatheredVentStoneBlock) {
            BlockState replacement = output(tier, stone);
            for (Property<?> property : state.getProperties())
                if (replacement.hasProperty(property)) replacement = copyProperty(state, replacement, property);
            return replacement;
        }
        if (!id.getNamespace().equals("create") || !id.getPath().contains("crimsite")) return state;
        if (id.getPath().equals("crimsite")) return output(tier, stone);
        if (java.util.Arrays.stream(com.ourcreation.moltenventsplus.compat.VentResource.values()).anyMatch(value -> value.id.equals(stone)))
            return Blocks.DEEPSLATE.defaultBlockState();
        BlockState replacement = block("create", id.getPath().replace("crimsite", stone));
        for (Property<?> property : state.getProperties())
            if (replacement.hasProperty(property))
                replacement = copyProperty(state, replacement, property);
        return replacement;
    }

    private static <T extends Comparable<T>> BlockState copyProperty(
            BlockState from, BlockState to, Property<T> property) {
        return to.setValue(property, from.getValue(property));
    }

    private static BlockState output(VentTier tier, String stone) {
        var original = java.util.Arrays.stream(OreStone.values()).filter(value -> value.id.equals(stone)).findFirst();
        if (original.isPresent()) return original.get().output(tier).defaultBlockState();
        return java.util.Arrays.stream(com.ourcreation.moltenventsplus.compat.VentResource.values())
                .filter(value -> value.id.equals(stone)).findFirst().flatMap(value -> value.output(tier))
                .orElse(Blocks.AIR).defaultBlockState();
    }

    private static void fillTank(CreateSceneBuilder scene, BlockPos pos, VentTier tier, int amount) {
        scene.world().modifyBlockEntity(pos, com.simibubi.create.content.fluids.tank.FluidTankBlockEntity.class,
                tank -> VentFluids.primary(tier).ifPresent(fluid -> {
                    tank.setWidth(1);
                    tank.setHeight(1);
                    tank.applyFluidTankSize(1);
                    tank.getTankInventory().setFluid(new net.neoforged.neoforge.fluids.FluidStack(fluid, amount));
                    tank.getFluidLevel().startWithValue(amount / 8000F);
                }));
    }

    private static BlockState vent(VentTier tier, String stone, boolean active) {
        var optional = java.util.Arrays.stream(com.ourcreation.moltenventsplus.compat.VentResource.values())
                .filter(value -> value.id.equals(stone)).findFirst();
        if (optional.isPresent()) return com.ourcreation.moltenventsplus.registry.ResourceVentRegistry
                .block(optional.get(), tier, active).defaultBlockState();
        return block(tier == VentTier.MOLTEN ? "molten_vents" : "create_water_vents",
                (active ? "active_" : "dormant_") + tier.id + "_" + stone);
    }

    private static BlockState fluid(VentTier tier) {
        return VentFluids.primary(tier).map(value -> value.defaultFluidState().createLegacyBlock())
                .orElse(Blocks.AIR.defaultBlockState());
    }

    private static BlockState cauldron(VentTier tier) {
        return tier == VentTier.WATER ? Blocks.WATER_CAULDRON.defaultBlockState()
                .setValue(net.minecraft.world.level.block.LayeredCauldronBlock.LEVEL, 3)
                : Blocks.LAVA_CAULDRON.defaultBlockState();
    }

    private static BlockState block(String namespace, String path) {
        return BuiltInRegistries.BLOCK
                .get(ResourceLocation.fromNamespaceAndPath(namespace, path))
                .defaultBlockState();
    }
}
