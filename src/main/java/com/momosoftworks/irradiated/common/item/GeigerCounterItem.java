package com.momosoftworks.irradiated.common.item;

import com.momosoftworks.irradiated.api.radiation.RadiationAPI;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

public class GeigerCounterItem extends Item {
    
    public GeigerCounterItem(Properties properties) {
        super(properties);
    }
    
    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        tooltipComponents.add(Component.translatable("item.irradiated.geiger_counter.tooltip"));
        tooltipComponents.add(Component.translatable("item.irradiated.geiger_counter.curios_tooltip"));
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
    }
    
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        // Display the current radiation level on right-click
        if (!player.level().isClientSide) {
            int radiation = RadiationAPI.getRadiationLevel(player);
            // Display the radiation level to the player
            player.displayClientMessage(Component.literal(radiation + " RAD"), true);
            player.swing(hand, true);
        }
        return super.use(level, player, hand);
    }
}
