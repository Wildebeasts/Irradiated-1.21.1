package com.momosoftworks.irradiated.core.init;

import com.momosoftworks.irradiated.common.effect.IncurableEffect;
import com.momosoftworks.irradiated.common.effect.RadiationEffect;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.core.Holder;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModEffects {
	public static final DeferredRegister<MobEffect> REGISTER = DeferredRegister.create(Registries.MOB_EFFECT, "irradiated");

	public static final DeferredHolder<MobEffect, MobEffect> CHEM_SICKNESS = REGISTER.register("chem_sickness", () -> new IncurableEffect(MobEffectCategory.HARMFUL, 0));
	public static final DeferredHolder<MobEffect, MobEffect> RAD_RESISTANCE = REGISTER.register("rad_resistance", () -> new IncurableEffect(MobEffectCategory.BENEFICIAL, 0));
	public static final DeferredHolder<MobEffect, MobEffect> RADIATION = REGISTER.register("radiation", () -> new RadiationEffect(MobEffectCategory.HARMFUL, 0x00FF00));

	public static Holder<MobEffect> chemSicknessHolder() { return CHEM_SICKNESS.getDelegate(); }
	public static Holder<MobEffect> radResistanceHolder() { return RAD_RESISTANCE.getDelegate(); }
	public static Holder<MobEffect> radiationHolder() { return RADIATION.getDelegate(); }
}
