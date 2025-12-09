package com.momosoftworks.irradiated.common.item.chems;

import com.momosoftworks.irradiated.common.temp.ColdSweatApiBridge;
import net.minecraft.ChatFormatting;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
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

public class NukaColaQuantumItem extends Item {
    private final int useTime;

    public NukaColaQuantumItem(Properties properties, int useTime) {
        super(properties);
        this.useTime = useTime;
    }

    public int getUseDuration(ItemStack stack) {
        return 32;
    }

    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.DRINK;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        return ItemUtils.startUsingInstantly(level, player, hand);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        Player player = entity instanceof Player ? (Player) entity : null;
        if (player instanceof ServerPlayer) {
            CriteriaTriggers.CONSUME_ITEM.trigger((ServerPlayer) player, stack);
        }
        if (player != null) {
            player.awardStat(Stats.ITEM_USED.get(this));
            player.getCooldowns().addCooldown(this, 400); // 20 second cooldown for quantum
            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }
        }
        
        level.playSound(null, entity.getX(), entity.getY(), entity.getZ(), 
                SoundEvents.GENERIC_DRINK, SoundSource.PLAYERS, 1.0F, 1.0F);
        
        if (!level.isClientSide) {
            entity.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 400, 1, true, false));
            entity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 2400, 1, true, false));
            entity.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 1200, 0, true, false));
        }
        // Legacy also added body heat by +4
        ColdSweatApiBridge.addBodyHeat(entity, 4.0d);
        return stack;
    }
    
    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        tooltipComponents.add(Component.translatable("info.irradiated.nuka_cola_quantum").withStyle(ChatFormatting.GRAY));
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
    }
}


