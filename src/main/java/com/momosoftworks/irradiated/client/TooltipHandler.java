package com.momosoftworks.irradiated.client;

import com.momosoftworks.irradiated.core.init.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

public class TooltipHandler {
    public static void onItemTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        if (stack.isEmpty()) return;

        // Shift for details, plain hover for summary
        boolean detailed = event.getFlags().isAdvanced();

        if (stack.is(ModItems.RADX.get())) {
            add(event, "tooltip.irradiated.radx", detailed,
                Component.literal("+Rad Resistance").withStyle(ChatFormatting.GREEN));
        } else if (stack.is(ModItems.RADAWAY.get())) {
            add(event, "tooltip.irradiated.radaway", detailed,
                Component.literal("-Radiation").withStyle(ChatFormatting.AQUA));
        } else if (stack.is(ModItems.STIMPACK.get())) {
            add(event, "tooltip.irradiated.stimpack", detailed,
                Component.literal("Instant Heal").withStyle(ChatFormatting.RED));
        } else if (stack.is(ModItems.MEDX.get())) {
            add(event, "tooltip.irradiated.medx", detailed,
                Component.literal("Damage Resistance").withStyle(ChatFormatting.GRAY));
        } else if (stack.is(ModItems.PSYCHO.get())) {
            add(event, "tooltip.irradiated.psycho", detailed,
                Component.literal("+Melee Damage").withStyle(ChatFormatting.DARK_RED));
        } else if (stack.is(ModItems.CATEYE.get())) {
            add(event, "tooltip.irradiated.cateye", detailed,
                Component.literal("Night Vision").withStyle(ChatFormatting.BLUE));
        } else if (stack.is(ModItems.JET.get())) {
            add(event, "tooltip.irradiated.jet", detailed,
                Component.literal("Slow Time (flavor)").withStyle(ChatFormatting.LIGHT_PURPLE));
        } else if (stack.is(ModItems.BUFFOUT.get())) {
            add(event, "tooltip.irradiated.buffout", detailed,
                Component.literal("+Strength +Health").withStyle(ChatFormatting.GOLD));
        } else if (stack.is(ModItems.NUKA_COLA.get())) {
            add(event, "tooltip.irradiated.nuka_cola", detailed,
                Component.literal("Speed + Regen").withStyle(ChatFormatting.AQUA));
        } else if (stack.is(ModItems.NUKA_COLA_QUANTUM.get())) {
            add(event, "tooltip.irradiated.nuka_cola_quantum", detailed,
                Component.literal("Stronger buffs").withStyle(ChatFormatting.DARK_AQUA));
        }
    }

    private static void add(ItemTooltipEvent event, String translationKey, boolean detailed, Component summary) {
        if (detailed) {
            event.getToolTip().add(Component.translatable(translationKey + ".desc").withStyle(ChatFormatting.DARK_GRAY));
        } else {
            event.getToolTip().add(summary);
            event.getToolTip().add(Component.translatable("tooltip.irradiated.hold_shift").withStyle(ChatFormatting.GRAY));
        }
    }
}


