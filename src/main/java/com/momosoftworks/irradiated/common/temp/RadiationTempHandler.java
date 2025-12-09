package com.momosoftworks.irradiated.common.temp;

import com.momosoftworks.coldsweat.api.util.Temperature;
import com.momosoftworks.irradiated.core.init.ModEffects;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RadiationTempHandler {
	private static final Logger LOGGER = LoggerFactory.getLogger("RadiationTempHandler");
	
	public static void onPlayerTick(final PlayerTickEvent.Post event) {
		Player player = event.getEntity();
		if (player.level().isClientSide) return;

		MobEffectInstance rad = player.getEffect(ModEffects.radiationHolder());
		MobEffectInstance res = player.getEffect(ModEffects.radResistanceHolder());
		
		if (rad != null) {
			int radiationLevel = rad.getAmplifier() + 1; // 1-100 scale
			
			// Calculate base heat based on radiation level (1-100)
			double baseHeat;
			if (radiationLevel < 10) {
				baseHeat = 0.02D; // Very low heat
			} else if (radiationLevel < 25) {
				baseHeat = 0.05D; // Low heat
			} else if (radiationLevel < 50) {
				baseHeat = 0.10D; // Moderate heat
			} else if (radiationLevel < 75) {
				baseHeat = 0.20D; // High heat
			} else if (radiationLevel < 90) {
				baseHeat = 0.35D; // Very high heat
			} else {
				baseHeat = 0.50D; // Extreme heat near death
			}
			
			// Apply Rad-X resistance reduction (90% heat reduction)
			double resistanceMultiplier = 1.0D;
			if (res != null) {
				int resistanceLevel = res.getAmplifier() + 1;
				resistanceMultiplier = switch (resistanceLevel) {
					case 1 -> 0.10D; // 90% reduction
					case 2 -> 0.05D; // 95% reduction
					case 3 -> 0.025D; // 97.5% reduction
					case 4 -> 0.0125D; // 98.75% reduction
					case 5 -> 0.00625D; // 99.375% reduction
					default -> 1.0D;
				};
			}
			
			double finalHeat = baseHeat * resistanceMultiplier;
			
			try {
				// Get current core temperature and add heat using ColdSweat Temperature API
				double currentCoreTemp = Temperature.get(player, Temperature.Trait.CORE);
				double newCoreTemp = currentCoreTemp + finalHeat;
				Temperature.set(player, Temperature.Trait.CORE, newCoreTemp);
			} catch (Exception e) {
				LOGGER.error("Failed to add body heat from radiation: {}", e.getMessage());
			}
		}
	}
}
