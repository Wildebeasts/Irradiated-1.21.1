package com.momosoftworks.irradiated.common.item.chems;

import com.momosoftworks.irradiated.core.init.ModEffects;
import com.momosoftworks.irradiated.core.init.ModSounds;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.level.Level;

import java.util.List;

public class BaseChemItem extends Item {
	private final int useTime;
	private final List<MobEffectInstance> effects;
	private final boolean applyChemSickness;
	private final int chemSicknessDuration;
	private final int chemSicknessAmplifier;

	public BaseChemItem(Properties properties, int useTime, List<MobEffectInstance> effects, boolean applyChemSickness, int chemSicknessDuration, int chemSicknessAmplifier) {
		super(properties);
		this.useTime = useTime;
		this.effects = effects;
		this.applyChemSickness = applyChemSickness;
		this.chemSicknessDuration = chemSicknessDuration;
		this.chemSicknessAmplifier = chemSicknessAmplifier;
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
		level.playSound(null, entity.getX(), entity.getY(), entity.getZ(), 
				ModSounds.PILLS.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
		
		if (!level.isClientSide && entity instanceof Player player) {
			player.awardStat(Stats.ITEM_USED.get(this));
			player.getCooldowns().addCooldown(this, 600);
			
			RandomSource random = level.getRandom();
			// Check for overdose effects if already has chem sickness
			if (player.hasEffect(ModEffects.chemSicknessHolder()) && random.nextInt(3) == 0) {
				entity.addEffect(new MobEffectInstance(MobEffects.POISON, 2400, 0, false, true));
				entity.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 1200, 0, false, true));
				entity.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 60, 0, false, true));
			}
			
			for (MobEffectInstance inst : effects) {
				entity.addEffect(new MobEffectInstance(inst));
			}
			if (applyChemSickness) {
				entity.addEffect(new MobEffectInstance(ModEffects.chemSicknessHolder(), chemSicknessDuration, chemSicknessAmplifier));
			}
		}
		return stack;
	}
}
