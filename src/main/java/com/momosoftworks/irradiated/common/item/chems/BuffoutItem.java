package com.momosoftworks.irradiated.common.item.chems;

import com.momosoftworks.irradiated.core.init.ModEffects;
import com.momosoftworks.irradiated.core.init.ModSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
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
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.level.Level;

import java.util.List;

public class BuffoutItem extends Item {
    private final int useTime;

    public BuffoutItem(Properties properties, int useTime) {
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

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        stack.shrink(1);
        level.playSound(null, entity.getX(), entity.getY(), entity.getZ(), 
                ModSounds.PILLS.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
        
        RandomSource random = level.getRandom();
        if (entity instanceof Player player) {
            player.awardStat(Stats.ITEM_USED.get(this));
            player.getCooldowns().addCooldown(this, 600);
            
            // Check for overdose effects if already has chem sickness
            if (player.hasEffect(ModEffects.chemSicknessHolder()) && random.nextInt(3) == 0) {
                entity.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 2400, 0, false, true));
                entity.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 1200, 0, false, true));
                entity.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 60, 0, false, true));
            }
        }

		// Give strength boost and chem sickness
		entity.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 1200, 0, true, false));
		entity.addEffect(new MobEffectInstance(ModEffects.chemSicknessHolder(), 2400, 0));
		
		return stack;
	}
	
	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
		tooltipComponents.add(Component.translatable("info.irradiated.buffout").withStyle(ChatFormatting.GRAY));
		super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
	}
}


