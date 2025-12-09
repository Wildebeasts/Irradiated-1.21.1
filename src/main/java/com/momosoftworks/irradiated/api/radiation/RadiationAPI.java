package com.momosoftworks.irradiated.api.radiation;

import com.momosoftworks.irradiated.common.temp.ColdSweatApiBridge;
import com.momosoftworks.irradiated.common.radiation.DynamicRadiationHandler;
import com.momosoftworks.irradiated.core.init.ModEffects;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

/**
 * Public API for managing radiation effects in the Irradiated mod.
 *
 * <p>This API provides methods to get, set, add, reduce, and clear radiation from entities.
 * It integrates with Cold Sweat's temperature system to make radiation increase body heat.</p>
 *
 * <p><b>Radiation Scale:</b></p>
 * <ul>
 *   <li>0: No radiation (safe)</li>
 *   <li>1-20: Minor radiation (minimal effects)</li>
 *   <li>20-40: Moderate radiation (noticeable symptoms)</li>
 *   <li>40-60: High radiation (serious health effects)</li>
 *   <li>60-80: Severe radiation (life-threatening)</li>
 *   <li>80-100: Critical radiation (near-death)</li>
 *   <li>100: Instant death</li>
 * </ul>
 *
 * <p><b>Player vs Entity Behavior:</b></p>
 * <ul>
 *   <li><b>Players:</b> Use dynamic radiation system with gradual buildup and decay</li>
 *   <li><b>Other entities:</b> Use standard mob effect system</li>
 * </ul>
 *
 * <p><b>Integration with Cold Sweat:</b></p>
 * Radiation automatically increases body heat based on radiation level. Higher radiation
 * causes more body heat, simulating fever and radiation sickness symptoms.
 *
 * <p><b>Thread Safety:</b> This API is designed to be called from the server thread only.</p>
 *
 * @since 0.3.5
 * @see DynamicRadiationHandler
 * @see ColdSweatApiBridge
 */
public class RadiationAPI {
    
    /**
     * Get the current radiation level of an entity
     * @param entity The entity to check
     * @return Current radiation level (0 if no radiation, 1-100 for radiation severity)
     */
    public static int getRadiationLevel(LivingEntity entity) {
        // For players, ALWAYS use dynamic radiation system as source of truth
        if (entity instanceof Player player) {
            float exposure = DynamicRadiationHandler.getCurrentRadiationExposure(player);
            // Dynamic exposure is now on the same 1-100 scale
            // Use ceiling for values very close to 100 to ensure death at 100 exposure
            if (exposure >= 99.5f) {
                return 100; // Ensure death threshold is reached
            } else if (exposure < 0.5f) {
                return 0; // Ensure very low exposure is treated as 0
            } else {
                return Math.round(exposure);
            }
        }
        
        // Fall back to MobEffectInstance for non-players only
        MobEffectInstance effect = entity.getEffect(ModEffects.radiationHolder());
        if (effect != null) {
            return effect.getAmplifier() + 1; // +1 because amplifier is 0-based
        }
        
        return 0;
    }    /**
     * Set the radiation level of an entity
     * @param entity The entity to affect
     * @param level Radiation level (0 = remove radiation, 1-100 = radiation severity)
     * @param duration Duration in ticks (20 ticks = 1 second)
     */
    public static void setRadiationLevel(LivingEntity entity, int level, int duration) {
        // Remove existing radiation effect
        entity.removeEffect(ModEffects.radiationHolder());
        
        // Add new radiation effect if level > 0
        if (level > 0) {
            // Clamp level to valid range (1-100)
            int clampedLevel = Math.max(1, Math.min(100, level));
            entity.addEffect(new MobEffectInstance(ModEffects.radiationHolder(), 
                    duration, clampedLevel - 1, false, true, true));
        }
        
        // Also update dynamic radiation system if this is a player
        if (entity instanceof Player player) {
            // Dynamic system now uses the same 1-100 scale as radiation level
            float exposure = (float) level;
            // Use override method to prevent dynamic system from immediately overwriting
            DynamicRadiationHandler.setCurrentRadiationExposureWithOverride(player, exposure, duration);
        }
    }
    
    /**
     * Add radiation to an entity
     * @param entity The entity to affect
     * @param levels Number of levels to add
     * @param duration Duration in ticks for new/extended effect
     */
    public static void addRadiation(LivingEntity entity, int levels, int duration) {
        int currentLevel = getRadiationLevel(entity);
        int newLevel = Math.min(100, currentLevel + levels);
        setRadiationLevel(entity, newLevel, duration);
    }
    
        /**
     * Reduce radiation level of an entity
     * @param entity The entity to affect
     * @param amount Amount of radiation to reduce
     */
    public static void reduceRadiation(LivingEntity entity, float amount) {
        // Update dynamic radiation system first if this is a player
        if (entity instanceof Player player) {
            DynamicRadiationHandler.reduceRadiationExposure(player, amount);
        }
        
        MobEffectInstance currentEffect = entity.getEffect(ModEffects.radiationHolder());
        if (currentEffect != null) {
            // Get current level and duration
            int currentLevel = currentEffect.getAmplifier() + 1; // +1 because amplifier is 0-based
            int currentDuration = currentEffect.getDuration();
            
            // Calculate new level
            int newLevel = Math.max(0, (int)(currentLevel - amount));
            
            // Remove old effect first
            entity.removeEffect(ModEffects.radiationHolder());
            
            // Add new effect if level > 0, otherwise it stays removed
            if (newLevel > 0) {
                entity.addEffect(new MobEffectInstance(ModEffects.radiationHolder(), 
                        currentDuration, newLevel - 1, false, true, true));
            }
            // If newLevel is 0, effect stays removed - this should sync to client immediately
        }
    }
    
    /**
     * Remove all radiation effects from an entity
     * @param entity The entity to clear radiation from
     */
    public static void clearRadiation(LivingEntity entity) {
        entity.removeEffect(ModEffects.radiationHolder());
        
        // Also clear dynamic radiation system if this is a player
        if (entity instanceof Player player) {
            DynamicRadiationHandler.clearRadiationExposure(player);
        }
    }    /**
     * Get the effective body heat contribution from radiation
     * This matches the calculation in RadiationTempHandler
     * @param entity The entity to check
     * @return Body heat value being added per tick
     */
    public static double getRadiationHeatContribution(LivingEntity entity) {
        int level = getRadiationLevel(entity);
        return switch (level) {
            case 1 -> 0.05D;
            case 2 -> 0.075D;
            case 3 -> 0.10D;
            case 4 -> 0.25D;
            case 5 -> 0.50D;
            case 6 -> 1.00D;
            case 7 -> 2.00D;
            default -> 0.00D;
        };
    }
    
    /**
     * Apply a RadAway-like effect that reduces both radiation and body heat.
     * This mimics the effect of taking RadAway or similar treatment items.
     *
     * @param entity The entity to treat
     * @param radiationReduction How many radiation levels to reduce
     * @param heatReduction How much body heat to remove (only applies if Cold Sweat is installed)
     */
    public static void applyRadAwayEffect(LivingEntity entity, int radiationReduction, double heatReduction) {
        // Reduce radiation effect
        reduceRadiation(entity, radiationReduction);

        // Reduce body heat if Cold Sweat is available
        if (ColdSweatApiBridge.isAvailable()) {
            ColdSweatApiBridge.addBodyHeat(entity, -heatReduction);
        }
    }
    
    /**
     * Check if an entity has any radiation
     * @param entity The entity to check
     * @return True if entity has radiation effect
     */
    public static boolean hasRadiation(LivingEntity entity) {
        return entity.hasEffect(ModEffects.radiationHolder());
    }
    
    /**
     * Get the remaining duration of radiation effect
     * @param entity The entity to check
     * @return Duration in ticks, or 0 if no radiation
     */
    public static int getRadiationDuration(LivingEntity entity) {
        MobEffectInstance radiation = entity.getEffect(ModEffects.radiationHolder());
        return radiation != null ? radiation.getDuration() : 0;
    }
}
