package com.momosoftworks.irradiated.common.event;

import com.momosoftworks.irradiated.api.radiation.RadiationAPI;
import com.momosoftworks.irradiated.common.radiation.RadiationConfig;
import com.momosoftworks.irradiated.core.init.ModEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Slime;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;

@EventBusSubscriber
public class SlimeRadAttack {
    
    @SubscribeEvent
    public static void onSlimeAttack(LivingDamageEvent.Pre event) {
        // Check if mob radiation is enabled
        if (!RadiationConfig.ENABLE_MOB_RADIATION.get()) {
            return;
        }
        
        LivingEntity victim = event.getEntity();
        Entity attacker = event.getSource().getEntity();
        
        if (attacker instanceof Slime && victim instanceof Player player) {
            float radiationAmount = calculateRadiation(player);
            
            // Apply radiation multiplier from config
            radiationAmount *= RadiationConfig.SLIME_RADIATION_MULTIPLIER.get().floatValue();
            
            if (radiationAmount > 0) {
                // Convert radiation amount to levels (1 level per 1.0 radiation)
                int levels = Math.round(radiationAmount);
                // Add radiation with 100 tick (5 second) duration
                RadiationAPI.addRadiation(player, levels, 100);
            }
        }
    }
    
    private static float calculateRadiation(Player player) {
        // Base radiation from slime attack
        float baseRadiation = 5.0f;
        
        // Check for rad resistance
        if (player.hasEffect(ModEffects.radResistanceHolder())) {
            int amplifier = player.getEffect(ModEffects.radResistanceHolder()).getAmplifier();
            float reduction = Math.min(0.95f, 0.25f * (amplifier + 1)); // 25% reduction per level, capped at 95%
            baseRadiation *= (1.0f - reduction);
        }
        
        return baseRadiation;
    }
}
