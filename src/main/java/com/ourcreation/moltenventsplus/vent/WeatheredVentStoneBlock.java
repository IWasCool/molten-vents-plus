package com.ourcreation.moltenventsplus.vent;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/** Natural rim details drop the matching ordinary stone; placed items remain full blocks. */
public final class WeatheredVentStoneBlock extends Block implements SimpleWaterloggedBlock {
    public enum Profile implements StringRepresentable {
        NATURAL, CUT, SLAB, POST;
        @Override public String getSerializedName() { return name().toLowerCase(java.util.Locale.ROOT); }
        public boolean partial() { return this == SLAB || this == POST; }
    }

    public static final EnumProperty<Profile> PROFILE = EnumProperty.create("profile", Profile.class);
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
    private static final VoxelShape SLAB = Block.box(0, 0, 0, 16, 8, 16);
    private static final VoxelShape POST = Block.box(4, 0, 4, 12, 16, 12);

    public WeatheredVentStoneBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(PROFILE, Profile.NATURAL).setValue(WATERLOGGED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(PROFILE, WATERLOGGED);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return switch (state.getValue(PROFILE)) {
            case SLAB -> SLAB;
            case POST -> POST;
            default -> Shapes.block();
        };
    }

    @Override
    protected boolean useShapeForLightOcclusion(BlockState state) { return true; }

    @Override
    protected FluidState getFluidState(BlockState state) {
        return state.getValue(PROFILE).partial() && state.getValue(WATERLOGGED)
                ? Fluids.WATER.getSource(false) : super.getFluidState(state);
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighbor,
                                     LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        if (state.getValue(PROFILE).partial() && state.getValue(WATERLOGGED))
            level.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
        return super.updateShape(state, direction, neighbor, level, pos, neighborPos);
    }

    @Override
    public boolean canPlaceLiquid(@Nullable Player player, BlockGetter level, BlockPos pos, BlockState state, Fluid fluid) {
        return state.getValue(PROFILE).partial() && SimpleWaterloggedBlock.super.canPlaceLiquid(player, level, pos, state, fluid);
    }

    @Override
    public boolean placeLiquid(LevelAccessor level, BlockPos pos, BlockState state, FluidState fluid) {
        return state.getValue(PROFILE).partial() && SimpleWaterloggedBlock.super.placeLiquid(level, pos, state, fluid);
    }
}
