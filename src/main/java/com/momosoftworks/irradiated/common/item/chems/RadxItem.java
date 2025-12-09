package com.momosoftworks.irradiated.common.item.chems;

import com.momosoftworks.irradiated.core.init.ModEffects;
import com.momosoftworks.irradiated.core.init.ModSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.stats.Stats;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;

import java.util.List;

public class RadxItem extends Item {
	private final int useTime;

	public RadxItem(Properties properties, int useTime) {
		super(properties);
		this.useTime = useTime;
	}

	public int getUseDuration(ItemStack stack) {
		return useTime;
	}

	public UseAnim getUseAnimation(ItemStack stack) {
		return UseAnim.BOW;
	}

	public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
		return ItemUtils.startUsingInstantly(level, player, hand);
	}

	public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
		stack.shrink(1);
		if (!level.isClientSide && entity instanceof Player player) {
			player.awardStat(Stats.ITEM_USED.get(this));
			// Legacy: side effects if already chem sick (1/3 chance)
			if (entity.hasEffect(ModEffects.chemSicknessHolder()) && level.random.nextInt(3) == 0) {
				entity.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 2400, 0, false, true));
				entity.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 1200, 0, false, true));
				entity.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 60, 0, false, true));
			}
			
			// Stack rad resistance levels when consuming multiple RadX
			int newAmplifier = 0; // Default level 1
			if (entity.hasEffect(ModEffects.radResistanceHolder())) {
				int currentAmplifier = entity.getEffect(ModEffects.radResistanceHolder()).getAmplifier();
				newAmplifier = Math.min(currentAmplifier + 1, 4); // Cap at level 5 (amplifier 4)
			}
			
			entity.addEffect(new MobEffectInstance(ModEffects.radResistanceHolder(), 6000, newAmplifier, false, true));
			entity.addEffect(new MobEffectInstance(ModEffects.chemSicknessHolder(), 6000, 0));
			level.playSound(null, entity.blockPosition(), ModSounds.PILLS.get(), net.minecraft.sounds.SoundSource.PLAYERS, 1.0f, 1.0f);
		}
		return stack;
	}
	
	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
		tooltipComponents.add(Component.translatable("info.irradiated.radx").withStyle(ChatFormatting.GRAY));
		tooltipComponents.add(Component.literal("Multiple uses stack up to level 5").withStyle(ChatFormatting.DARK_GRAY));
		super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
	}
}
