package com.ourcreation.moltenventsplus.vent;

import com.ourcreation.moltenventsplus.registry.VentRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.entity.Entity;

import java.util.List;

public class VentBlock extends Block {
    public final VentTier tier;
    public final OreStone stone;
    public final boolean active;

    public VentBlock(VentTier tier, OreStone stone, boolean active, Properties properties) {
        super(properties);
        this.tier = tier;
        this.stone = stone;
        this.active = active;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        if (tier == VentTier.WATER) {
            tooltip.add(Component.translatable("create_water_vents.water_vent.finite").withStyle(ChatFormatting.GRAY));
        }
    }

    @Override
    public boolean canEntityDestroy(BlockState state, BlockGetter level, BlockPos pos, Entity entity) {
        return false;
    }

    @Override
    public void onBlockExploded(BlockState state, Level level, BlockPos pos, Explosion explosion) {
        // Activate in place: never remove a core or create a destruction/drop window.
        wasExploded(level, pos, explosion);
    }

    @Override
    public void wasExploded(Level level, BlockPos pos, Explosion explosion) {
        if (!active && !level.isClientSide) {
            level.setBlock(pos, VentRegistry.block(tier, stone, true).defaultBlockState(), UPDATE_ALL);
        }
    }

    @Override
    public boolean dropFromExplosion(Explosion explosion) {
        return false;
    }

}
