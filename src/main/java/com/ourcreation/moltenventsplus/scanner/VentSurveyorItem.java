package com.ourcreation.moltenventsplus.scanner;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import com.ourcreation.moltenventsplus.vent.VentTier;

import java.util.List;
import java.util.Objects;

public final class VentSurveyorItem extends Item {
    private final VentTier tier;

    public VentSurveyorItem(Properties properties, VentTier tier) {
        super(properties.stacksTo(1));
        this.tier = Objects.requireNonNull(tier);
    }

    public VentTier tier() { return tier; }
    public boolean supports(ScannerTarget target) { return target.tier() == tier; }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        interact(level, player);
        return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (context.getPlayer() == null) return InteractionResult.PASS;
        interact(context.getLevel(), context.getPlayer());
        return InteractionResult.sidedSuccess(context.getLevel().isClientSide);
    }

    private void interact(Level level, Player player) {
        if (!level.isClientSide) return;
        if (player.isShiftKeyDown()) ScannerClientHooks.stop();
        else ScannerClientHooks.open(tier);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("scanner.create_water_vents.tooltip").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("scanner.create_water_vents.range", ScannerTarget.radius(tier))
                .withStyle(ChatFormatting.DARK_GRAY));
        tooltip.add(Component.translatable("scanner.create_water_vents.limit", ScannerTarget.maxResults(tier))
                .withStyle(ChatFormatting.DARK_GRAY));
        tooltip.add(Component.translatable("scanner.create_water_vents.select").withStyle(ChatFormatting.GRAY));
    }
}
