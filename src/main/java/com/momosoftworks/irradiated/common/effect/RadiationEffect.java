package com.momosoftworks.irradiated.common.effect;

import com.momosoftworks.irradiated.core.init.ModSounds;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Radiation effect that implements a 100-scale system with progressive damage.
 * At 100 radiation, the player instantly dies.
 */
public class RadiationEffect extends MobEffect {
    private static final Logger LOGGER = LoggerFactory.getLogger(RadiationEffect.class);
    
    public RadiationEffect(MobEffectCategory category, int color) {
        super(category, color);
    }
    
    @Override
    public boolean applyEffectTick(LivingEntity entity, int amplifier) {
        if (entity.level().isClientSide()) {
            return true;
        }
        
        // Convert amplifier (0-99) to radiation level (1-100)
        int radiationLevel = amplifier + 1;
        
        // Skip damage for creative mode players (but still play sounds)
        boolean isCreativePlayer = entity instanceof Player player && player.isCreative();
        
        // Progressive damage based on radiation level (but not in creative mode)
        if (entity.tickCount % 40 == 0 && !isCreativePlayer) { // Every 2 seconds
            applyRadiationDamage(entity, radiationLevel);
        }
        
        // Play geiger counter sounds based on radiation level
        if (entity instanceof Player && entity.tickCount % getGeigerInterval(radiationLevel) == 0) {
            playGeigerSound(entity, radiationLevel);
        }
        
        return true;
    }
    
    /**
     * Apply damage based on radiation level
     */
    private void applyRadiationDamage(LivingEntity entity, int radiationLevel) {
        float damage = calculateDamage(radiationLevel);
        
        if (damage > 0) {
            DamageSource radiationDamage = entity.level().damageSources().generic();
            entity.hurt(radiationDamage, damage);
        }
    }
    
    /**
     * Calculate damage based on radiation level (1-100 scale)
     */
    private float calculateDamage(int radiationLevel) {
        if (radiationLevel < 10) {
            return 0.0f; // No damage below 10 radiation
        } else if (radiationLevel < 25) {
            return 0.5f; // Minor damage 10-24
        } else if (radiationLevel < 50) {
            return 1.0f; // Light damage 25-49
        } else if (radiationLevel < 75) {
            return 2.0f; // Moderate damage 50-74
        } else if (radiationLevel < 90) {
            return 3.0f; // Heavy damage 75-89
        } else if (radiationLevel < 100) {
            return 5.0f; // Severe damage 90-99
        } else {
            return 8.0f; // Extreme damage at 100 - very dangerous but not instant death
        }
    }
    
    /**
     * Get geiger counter sound interval based on radiation level
     */
    private int getGeigerInterval(int radiationLevel) {
        if (radiationLevel < 10) {
            return 200; // Very slow clicking (10 seconds)
        } else if (radiationLevel < 25) {
            return 100; // Slow clicking (5 seconds)
        } else if (radiationLevel < 50) {
            return 60; // Moderate clicking (3 seconds)
        } else if (radiationLevel < 75) {
            return 40; // Fast clicking (2 seconds)
        } else if (radiationLevel < 90) {
            return 25; // Very fast clicking (1.25 seconds)
        } else if (radiationLevel < 95) {
            return 15; // Extremely fast clicking (0.75 seconds)
        } else {
            return 8; // Panic clicking (0.4 seconds) for near-death levels
        }
    }
    
    /**
     * Play geiger counter sound
     */
    private void playGeigerSound(LivingEntity entity, int radiationLevel) {
        RandomSource random = entity.level().getRandom();
        float volume = Math.min(1.0f, radiationLevel / 50.0f); // Volume increases with radiation
        float pitch = 0.8f + (radiationLevel / 100.0f) * 0.4f; // Pitch increases with radiation
        
        entity.level().playSound(null, entity.getX(), entity.getY(), entity.getZ(),
                ModSounds.GEIGER.get(), SoundSource.PLAYERS, volume, pitch);
    }
    
    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        // Apply effect every tick for precise control
        return true;
    }
    
    @Override
    public boolean isInstantenous() {
        return false;
    }
}
