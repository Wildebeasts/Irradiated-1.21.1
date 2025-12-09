package com.momosoftworks.irradiated.common.item.chems;

import com.momosoftworks.irradiated.api.radiation.RadiationAPI;
import com.momosoftworks.irradiated.core.init.ModSounds;
import com.momosoftworks.irradiated.core.init.ModEffects;
import com.momosoftworks.coldsweat.api.util.Temperature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.stats.Stats;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.level.Level;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.Item.TooltipContext;

import java.util.List;

public class RadawayItem extends Item {
	private static final Logger LOGGER = LoggerFactory.getLogger(RadawayItem.class);
	private final int useTime;

	public RadawayItem(Properties properties, int useTime) {
		super(properties);
		this.useTime = useTime;
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
		LOGGER.info("DEBUG: RadawayItem.use() called");
		ItemStack itemStack = player.getItemInHand(hand);
		if (player.getCooldowns().isOnCooldown(this)) {
			return InteractionResultHolder.pass(itemStack);
		}
		return ItemUtils.startUsingInstantly(level, player, hand);
	}

	@Override
	public UseAnim getUseAnimation(ItemStack stack) {
		return UseAnim.DRINK;
	}

	@Override
	public int getUseDuration(ItemStack stack, LivingEntity entity) {
		return 1; // Instant consumption
	}

	@Override
	public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
		LOGGER.info("DEBUG: RadawayItem.finishUsingItem() called");
		
		Player player = entity instanceof Player ? (Player) entity : null;
		if (player != null) {
			player.awardStat(Stats.ITEM_USED.get(this));
			player.getCooldowns().addCooldown(this, 200); // 10 second cooldown
			if (!player.getAbilities().instabuild) {
				stack.shrink(1);
			}
		}
		
		level.playSound(null, entity.getX(), entity.getY(), entity.getZ(), 
				ModSounds.RADAWAY.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
		
		LOGGER.info("DEBUG: About to call direct radiation reduction, level.isClientSide=" + level.isClientSide);
		
		// Check radiation level before
		int radiationBefore = RadiationAPI.getRadiationLevel(entity);
		LOGGER.info("DEBUG: Radiation level before RadAway: " + radiationBefore);
		
		// Only apply radiation reduction on server side for proper synchronization
		if (!level.isClientSide) {
			try {
				// Use RadiationAPI for proper integration with dynamic system
				LOGGER.info("DEBUG: Using RadiationAPI.reduceRadiation() method");
				
				int currentLevel = RadiationAPI.getRadiationLevel(entity);
				LOGGER.info("DEBUG: Current radiation level: " + currentLevel);
				
				// Reduce radiation by 25 levels using the API
				RadiationAPI.reduceRadiation(entity, 25.0f);
				
				int newLevel = RadiationAPI.getRadiationLevel(entity);
				LOGGER.info("DEBUG: New radiation level after reduction: " + newLevel);
				
				LOGGER.info("DEBUG: RadiationAPI radiation reduction completed successfully");
			} catch (Exception e) {
				LOGGER.info("DEBUG: Exception in RadiationAPI radiation reduction: " + e.getMessage());
				e.printStackTrace();
			}
			
			// Apply body heat reduction using ColdSweat API (server side only)
			try {
				LOGGER.info("DEBUG: Trying body heat reduction (SERVER SIDE)");
				
				// Get current body temperature
				double currentBodyTemp = Temperature.get(entity, Temperature.Trait.BODY);
				LOGGER.info("DEBUG: Current body temperature: " + currentBodyTemp);
				
				// Reduce body temperature by 50 units (significant cooling effect)
				// Using CORE temperature trait as it directly affects body temperature
				double currentCoreTemp = Temperature.get(entity, Temperature.Trait.CORE);
				double newCoreTemp = currentCoreTemp - 50.0;
				LOGGER.info("DEBUG: Body heat - Current core: " + currentCoreTemp + ", New core: " + newCoreTemp);
				
				Temperature.set(entity, Temperature.Trait.CORE, newCoreTemp);
				LOGGER.info("DEBUG: Body heat reduction completed successfully");
				
				// Check final body temperature
				double finalBodyTemp = Temperature.get(entity, Temperature.Trait.BODY);
				LOGGER.info("DEBUG: Final body temperature: " + finalBodyTemp);
				
			} catch (Exception e) {
				LOGGER.info("DEBUG: Exception in body heat reduction: " + e.getMessage());
				e.printStackTrace();
			}
		} else {
			LOGGER.info("DEBUG: Skipping radiation reduction on client side");
		}
		
		// Check radiation level after (immediate)
		int radiationAfter = RadiationAPI.getRadiationLevel(entity);
		LOGGER.info("DEBUG: Radiation level after RadAway (immediate): " + radiationAfter);
		
		// Check radiation level after with delay (server side only)
		if (!level.isClientSide) {
			// Schedule a delayed check to see if the radiation level changed
			level.getServer().tell(new net.minecraft.server.TickTask(5, () -> {
				int radiationAfterDelay = RadiationAPI.getRadiationLevel(entity);
				LOGGER.info("DEBUG: Radiation level after RadAway (5 ticks later): " + radiationAfterDelay);
			}));
		}
		
		return stack;
	}
	
	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
		tooltipComponents.add(Component.translatable("info.irradiated.radaway").withStyle(ChatFormatting.GRAY));
		super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
	}
}
