package com.momosoftworks.irradiated.util;

import com.momosoftworks.irradiated.core.init.ModItems;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.BuiltInRegistries;
import top.theillusivec4.curios.api.CuriosApi;

public class RadiationUtil {
    
    /**
     * Checks if the player has a geiger counter equipped in any valid slot (following Cold Sweat's pattern)
     */
    public static boolean hasGeigerCounter(Player player) {
        if (player == null) return false;
        
        // Check hotbar slots (first 9 inventory slots)
        boolean inHotbar = player.getInventory().items.stream().limit(9)
                .anyMatch(stack -> stack.getItem() == ModItems.GEIGER_COUNTER.value());
        
        // Check offhand
        boolean inOffhand = player.getOffhandItem().getItem() == ModItems.GEIGER_COUNTER.value();
        
        // Check Curios slots
        boolean inCurios = CuriosApi.getCuriosInventory(player).map(curiosInventory -> {
            return curiosInventory.findFirstCurio(stack -> stack.getItem() == ModItems.GEIGER_COUNTER.value()).isPresent();
        }).orElse(false);
        
        return inHotbar || inOffhand || inCurios;
    }

        
    /**
     * Checks if the player has a thermometer equipped in a charm curios slot
     */
    public static boolean hasThermometer(Player player) {
        if (player == null) return false;
        
        // Check charm slots specifically for thermometer
        return CuriosApi.getCuriosInventory(player).map(curiosInventory -> {
            // Look for Cold Sweat thermometer item
            ItemStack thermometerStack = new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("cold_sweat", "thermometer")));
            if (!thermometerStack.isEmpty()) {
                return curiosInventory.findFirstCurio(stack -> stack.getItem() == thermometerStack.getItem()).isPresent();
            }
            return false;
        }).orElse(false);
    }
    
    /**
     * Determines if radiation numbers should be shown based on creative mode OR geiger counter presence
     */
    public static boolean shouldShowRadiationNumbers(Player player) {
        return player.isCreative() || hasGeigerCounter(player);
    }
}
