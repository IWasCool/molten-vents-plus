package com.ourcreation.moltenventsplus.registry;

import com.simibubi.create.AllFluids;
import com.simibubi.create.api.event.PipeCollisionEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.dispenser.BlockSource;
import net.minecraft.core.dispenser.DefaultDispenseItemBehavior;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.pathfinder.PathType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.NeoForgeMod;
import net.neoforged.neoforge.common.SoundActions;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;
import net.neoforged.neoforge.fluids.FluidInteractionRegistry;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.registries.*;
import org.joml.Vector3f;

import static com.ourcreation.moltenventsplus.MoltenVentsPlus.MODID;

/** Standalone counterpart of QoL's MIT-licensed superheated lava; see LICENSES. */
public final class SuperheatedFluidRegistry {
    public static final ResourceLocation QOL_FLUID = ResourceLocation.parse("createqol:superheated_lava");
    private static final DeferredRegister<FluidType> TYPES = DeferredRegister.create(NeoForgeRegistries.Keys.FLUID_TYPES, MODID);
    private static final DeferredRegister<Fluid> FLUIDS = DeferredRegister.create(Registries.FLUID, MODID);
    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MODID);
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MODID);

    public static final DeferredHolder<FluidType, FluidType> TYPE = TYPES.register("superheated_lava", SuperheatedFluidType::new);
    public static final DeferredHolder<Fluid, BaseFlowingFluid.Source> SOURCE = FLUIDS.register("superheated_lava",
            () -> new BaseFlowingFluid.Source(properties()));
    public static final DeferredHolder<Fluid, BaseFlowingFluid.Flowing> FLOWING = FLUIDS.register("flowing_superheated_lava",
            () -> new BaseFlowingFluid.Flowing(properties()));
    public static final DeferredBlock<LiquidBlock> BLOCK = BLOCKS.register("superheated_lava",
            () -> new LiquidBlock(SOURCE.get(), BlockBehaviour.Properties.ofFullCopy(Blocks.LAVA).mapColor(MapColor.COLOR_BLUE)));
    public static final DeferredItem<BucketItem> BUCKET = ITEMS.register("superheated_lava_bucket",
            () -> new BucketItem(SOURCE.get(), new Item.Properties().craftRemainder(Items.BUCKET).stacksTo(1).fireResistant()));

    private static BaseFlowingFluid.Properties properties() {
        return new BaseFlowingFluid.Properties(TYPE, SOURCE, FLOWING).block(BLOCK).bucket(BUCKET)
                .levelDecreasePerBlock(2).tickRate(25).slopeFindDistance(3).explosionResistance(100);
    }

    public static boolean hasProvider() {
        return BuiltInRegistries.FLUID.containsKey(QOL_FLUID);
    }

    public static boolean isFallback(Fluid fluid) {
        return fluid == SOURCE.get() || fluid == FLOWING.get();
    }

    public static void register(IEventBus bus) {
        TYPES.register(bus);
        FLUIDS.register(bus);
        BLOCKS.register(bus);
        ITEMS.register(bus);
        bus.addListener(SuperheatedFluidRegistry::setup);
        NeoForge.EVENT_BUS.addListener(SuperheatedFluidRegistry::pipeFlow);
        NeoForge.EVENT_BUS.addListener(SuperheatedFluidRegistry::pipeSpill);
    }

    private static void setup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            FluidInteractionRegistry.addInteraction(TYPE.get(), new FluidInteractionRegistry.InteractionInformation(
                    NeoForgeMod.WATER_TYPE.value(), state -> Blocks.OBSIDIAN.defaultBlockState()));
            DispenserBlock.registerBehavior(BUCKET.get(), new DefaultDispenseItemBehavior() {
                private final DefaultDispenseItemBehavior fallback = new DefaultDispenseItemBehavior();

                @Override
                protected ItemStack execute(BlockSource source, ItemStack stack) {
                    var pos = source.pos().relative(source.state().getValue(DispenserBlock.FACING));
                    if (BUCKET.get().emptyContents(null, source.level(), pos, null, stack)) {
                        BUCKET.get().checkExtraContent(null, source.level(), stack, pos);
                        return new ItemStack(Items.BUCKET);
                    }
                    return fallback.dispense(source, stack);
                }
            });
        });
    }

    private static void pipeFlow(PipeCollisionEvent.Flow event) {
        if (waterCollision(event.getFirstFluid(), event.getSecondFluid()))
            event.setState(Blocks.OBSIDIAN.defaultBlockState());
    }

    private static void pipeSpill(PipeCollisionEvent.Spill event) {
        if (waterCollision(event.getPipeFluid(), event.getWorldFluid()))
            event.setState(Blocks.OBSIDIAN.defaultBlockState());
    }

    private static boolean waterCollision(Fluid first, Fluid second) {
        return first.isSame(Fluids.WATER) && isFallback(second)
                || second.isSame(Fluids.WATER) && isFallback(first);
    }

    private static final class SuperheatedFluidType extends AllFluids.TintedFluidType {
        private SuperheatedFluidType() {
            super(FluidType.Properties.create().descriptionId("fluid." + MODID + ".superheated_lava")
                            .viscosity(6200).temperature(1500).density(3000)
                            .pathType(PathType.LAVA).adjacentPathType(null)
                            .supportsBoating(false).canDrown(false).canSwim(false)
                            .sound(SoundActions.BUCKET_FILL, SoundEvents.BUCKET_FILL_LAVA)
                            .sound(SoundActions.BUCKET_EMPTY, SoundEvents.BUCKET_EMPTY_LAVA),
                    ResourceLocation.fromNamespaceAndPath(MODID, "fluid/superheated_lava_still"),
                    ResourceLocation.fromNamespaceAndPath(MODID, "fluid/superheated_lava_flow"));
        }

        @Override protected int getTintColor(FluidStack stack) { return NO_TINT; }
        @Override protected int getTintColor(FluidState state, BlockAndTintGetter level, BlockPos pos) { return 0x00FFFFFF; }
        @Override protected Vector3f getCustomFogColor() { return new Vector3f(0x4B / 255F, 0x7D / 255F, 0xEA / 255F); }
        @Override protected float getFogDistanceModifier() { return 1F / 16F; }
        @Override public double motionScale(Entity entity) { return entity.level().dimensionType().ultraWarm() ? 0.007 : 0.0023333333333333335; }

        @Override
        public void setItemMovement(ItemEntity entity) {
            var movement = entity.getDeltaMovement();
            entity.setDeltaMovement(movement.x * 0.95F,
                    movement.y + (movement.y < 0.06F ? 5.0E-4F : 0), movement.z * 0.95F);
        }
    }

    private SuperheatedFluidRegistry() {}
}
