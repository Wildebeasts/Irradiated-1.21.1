package com.momosoftworks.irradiated.common.item;

import com.momosoftworks.irradiated.Irradiated;
import com.momosoftworks.irradiated.api.radiation.RadiationAPI;
import com.momosoftworks.irradiated.common.radiation.RadiationConfig;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles radiation removal from early-game items like milk and tomatoes.
 * Provides cheaper alternatives to RadAway before players can craft it.
 */
@EventBusSubscriber(modid = Irradiated.MOD_ID)
public class EarlyGameRadiationRemoval {
    private static final Logger LOGGER = LoggerFactory.getLogger(EarlyGameRadiationRemoval.class);
    
    /**
     * Listens for when items are consumed and applies radiation removal effects.
     * Triggers when an item's use animation completes (eating/drinking finished).
     */
    @SubscribeEvent
    public static void onItemFinishUse(LivingEntityUseItemEvent.Finish event) {
        LivingEntity entity = event.getEntity();
        ItemStack item = event.getItem();
        
        // Only process on server side and for players
        if (entity.level().isClientSide || !(entity instanceof Player)) {
            return;
        }
        
        Player player = (Player) entity;
        int currentRadiation = RadiationAPI.getRadiationLevel(player);
        
        // Only process if player has radiation to remove
        if (currentRadiation <= 0) {
            return;
        }
        
        // Check for milk bucket
        if (item.is(Items.MILK_BUCKET)) {
            if (RadiationConfig.ENABLE_MILK_RADIATION_REMOVAL.get()) {
                float removalAmount = RadiationConfig.MILK_RADIATION_REMOVAL_AMOUNT.get().floatValue();
                RadiationAPI.reduceRadiation(player, removalAmount);
                
                LOGGER.debug("Player {} drank milk. Reduced radiation by {} (was: {}, now: {})",
                        player.getName().getString(), removalAmount, currentRadiation,
                        RadiationAPI.getRadiationLevel(player));
            }
            return;
        }
        
        // Check for tomato-based items
        // Using item registry name to avoid hard dependency on mods
        String itemId = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(item.getItem()).toString();
        
        if (RadiationConfig.ENABLE_TOMATO_RADIATION_REMOVAL.get()) {
            float removalAmount = 0;
            String itemName = "";
            
            // Farmer's Delight tomato items
            if (itemId.equals("farmersdelight:tomato")) {
                removalAmount = RadiationConfig.TOMATO_RADIATION_REMOVAL_AMOUNT.get().floatValue();
                itemName = "tomato";
            } else if (itemId.equals("farmersdelight:tomato_slice")) {
                removalAmount = RadiationConfig.TOMATO_SLICE_RADIATION_REMOVAL_AMOUNT.get().floatValue();
                itemName = "tomato slice";
            } else if (itemId.equals("farmersdelight:tomato_sauce")) {
                removalAmount = RadiationConfig.TOMATO_SAUCE_RADIATION_REMOVAL_AMOUNT.get().floatValue();
                itemName = "tomato sauce";
            }
            // Some Assembly Required sandwiches with tomatoes
            // Check if the item has tomato in its NBT/components (sandwiches are dynamic)
            else if (itemId.startsWith("someassemblyrequired:sandwich")) {
                // Check if the sandwich contains tomato ingredients
                if (hasTomatoIngredient(item)) {
                    removalAmount = RadiationConfig.TOMATO_SANDWICH_RADIATION_REMOVAL_AMOUNT.get().floatValue();
                    itemName = "tomato sandwich";
                }
            }
            
            if (removalAmount > 0) {
                RadiationAPI.reduceRadiation(player, removalAmount);
                
                LOGGER.debug("Player {} ate {}. Reduced radiation by {} (was: {}, now: {})",
                        player.getName().getString(), itemName, removalAmount, currentRadiation,
                        RadiationAPI.getRadiationLevel(player));
            }
        }
    }
    
    /**
     * Checks if a sandwich contains tomato ingredients.
     * Some Assembly Required stores ingredients in item components/NBT.
     */
    private static boolean hasTomatoIngredient(ItemStack sandwich) {
        try {
            // Check item components for ingredients
            var components = sandwich.getComponents();
            if (components.isEmpty()) {
                return false;
            }
            
            // Some Assembly Required stores ingredients as a list
            // We'll check the item's display name or lore which often contains ingredient info
            var hoverName = sandwich.getHoverName().getString().toLowerCase();
            
            // Check if the name/description contains tomato-related terms
            return hoverName.contains("tomato");
            
        } catch (Exception e) {
            LOGGER.debug("Error checking sandwich ingredients: {}", e.getMessage());
            return false;
        }
    }
}
