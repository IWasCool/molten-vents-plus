package com.ourcreation.moltenventsplus.client.ponder;

import com.ourcreation.moltenventsplus.compat.OilFluids;
import com.ourcreation.moltenventsplus.compat.VentResource;
import com.ourcreation.moltenventsplus.registry.ResourceVentRegistry;
import com.ourcreation.moltenventsplus.vent.VentTier;
import com.simibubi.create.content.fluids.tank.FluidTankBlockEntity;
import com.simibubi.create.foundation.ponder.CreateSceneBuilder;
import net.createmod.ponder.api.PonderPalette;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.fluids.FluidStack;

public final class OilVentScene {
    public static void build(SceneBuilder builder, SceneBuildingUtil util) {
        var scene = new CreateSceneBuilder(builder);
        scene.title("oil_vent_automation", "Extracting oil from a vent");
        scene.configureBasePlate(0, 0, 7);
        scene.scaleSceneView(0.7F);
        scene.world().setBlocks(util.select().fromTo(0, 1, 0, 14, 8, 14), Blocks.AIR.defaultBlockState(), false);
        scene.world().setBlocks(util.select().fromTo(0, 0, 0, 6, 0, 6), Blocks.DEEPSLATE.defaultBlockState(), false);
        BlockPos core = util.grid().at(3, 1, 1), outlet = core.above();
        BlockPos intake = outlet.south(), pump = outlet.south(2), tank = outlet.south(4);
        scene.world().setBlock(core, ResourceVentRegistry.block(VentResource.OIL, VentTier.WATER, false).defaultBlockState(), false);
        scene.showBasePlate();
        scene.world().showSection(util.select().position(core), Direction.DOWN);
        scene.overlay().showText(80).text("Find oil vents in deserts, badlands, swamps and oceans.")
                .placeNearTarget().pointAt(util.vector().centerOf(core));
        scene.idle(90);
        scene.world().setBlock(outlet, Blocks.TNT.defaultBlockState(), false);
        scene.world().showSection(util.select().position(outlet), Direction.DOWN);
        scene.overlay().showText(70).text("Expose the core and activate it with an explosion.")
                .placeNearTarget().pointAt(util.vector().centerOf(outlet));
        scene.idle(75);
        scene.world().setBlock(outlet, Blocks.AIR.defaultBlockState(), false);
        scene.effects().emitParticles(util.vector().centerOf(outlet),
                scene.effects().simpleParticleEmitter(ParticleTypes.EXPLOSION, Vec3.ZERO), 1, 1);
        scene.world().setBlock(core, ResourceVentRegistry.block(VentResource.OIL, VentTier.WATER, true).defaultBlockState(), false);
        for (var direction : new Direction[] {Direction.NORTH, Direction.WEST, Direction.EAST})
            scene.world().setBlock(outlet.relative(direction), Blocks.GLASS.defaultBlockState(), false);
        scene.world().showSection(util.select().fromTo(2, 2, 0, 4, 2, 1), Direction.DOWN);
        OilFluids.preferred().ifPresent(fluid -> scene.world().setBlock(outlet, fluid.defaultFluidState().createLegacyBlock(), false));
        scene.overlay().showOutline(PonderPalette.GREEN, "outlet", util.select().position(outlet), 90);
        scene.overlay().showText(90).text("The active core refills one oil source directly above it. Contain this outlet.")
                .placeNearTarget().pointAt(util.vector().centerOf(outlet));
        scene.idle(100);
        scene.world().setBlock(intake, block("fluid_pipe")
                .setValue(BlockStateProperties.NORTH, true).setValue(BlockStateProperties.SOUTH, true), false);
        scene.world().showSection(util.select().position(intake), Direction.DOWN);
        scene.world().setBlock(pump, block("mechanical_pump")
                .setValue(BlockStateProperties.FACING, Direction.SOUTH), false);
        scene.world().setBlock(pump.south(), block("fluid_pipe")
                .setValue(BlockStateProperties.NORTH, true).setValue(BlockStateProperties.SOUTH, true), false);
        scene.world().setBlock(tank, block("fluid_tank"), false);
        for (int x = 4; x <= 5; x++) scene.world().setBlock(new BlockPos(x, 2, 3),
                block("shaft").setValue(BlockStateProperties.AXIS, Direction.Axis.X), false);
        scene.world().setBlock(new BlockPos(6, 2, 3), block("creative_motor")
                .setValue(BlockStateProperties.FACING, Direction.WEST), false);
        scene.world().showSection(util.select().fromTo(3, 2, 3, 6, 2, 5), Direction.DOWN);
        scene.world().setKineticSpeed(util.select().fromTo(3, 2, 3, 6, 2, 3), 32);
        scene.overlay().showText(90).text("Power a Create pump to drain the outlet into tanks and your refinery.")
                .placeNearTarget().pointAt(util.vector().centerOf(pump));
        for (int i = 1; i <= 4; i++) {
            int amount = i * 1000;
            scene.idle(20);
            scene.world().modifyBlockEntity(tank, FluidTankBlockEntity.class, entity -> OilFluids.preferred().ifPresent(fluid -> {
                entity.setWidth(1); entity.setHeight(1); entity.applyFluidTankSize(1);
                entity.getTankInventory().setFluid(new FluidStack(fluid, amount));
                entity.getFluidLevel().startWithValue(amount / 8000F);
            }));
        }
        scene.idle(25);
        scene.overlay().showText(100).text("Use a supported oil bucket on the active core to choose a provider. The choice is saved.")
                .placeNearTarget().pointAt(util.vector().centerOf(core));
        scene.idle(110);
        scene.markAsFinished();
    }

    private static net.minecraft.world.level.block.state.BlockState block(String name) {
        return net.minecraft.core.registries.BuiltInRegistries.BLOCK.get(
                net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("create", name)).defaultBlockState();
    }

    private OilVentScene() {}
}
