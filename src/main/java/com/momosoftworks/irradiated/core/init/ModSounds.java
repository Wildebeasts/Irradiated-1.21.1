package com.momosoftworks.irradiated.core.init;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModSounds {
	public static final DeferredRegister<SoundEvent> REGISTER = DeferredRegister.create(Registries.SOUND_EVENT, "irradiated");

	public static final DeferredHolder<SoundEvent, SoundEvent> STIMPACK = REGISTER.register("entity.player.stimpack",
			() -> SoundEvent.createVariableRangeEvent(ResourceLocation.parse("irradiated:entity.player.stimpack")));
	public static final DeferredHolder<SoundEvent, SoundEvent> RADAWAY = REGISTER.register("entity.player.radaway",
			() -> SoundEvent.createVariableRangeEvent(ResourceLocation.parse("irradiated:entity.player.radaway")));
	public static final DeferredHolder<SoundEvent, SoundEvent> JET = REGISTER.register("entity.player.jet",
			() -> SoundEvent.createVariableRangeEvent(ResourceLocation.parse("irradiated:entity.player.jet")));
	public static final DeferredHolder<SoundEvent, SoundEvent> PILLS = REGISTER.register("entity.player.pills",
			() -> SoundEvent.createVariableRangeEvent(ResourceLocation.parse("irradiated:entity.player.pills")));
	public static final DeferredHolder<SoundEvent, SoundEvent> GEIGER = REGISTER.register("entity.player.geiger",
			() -> SoundEvent.createVariableRangeEvent(ResourceLocation.parse("irradiated:entity.player.geiger")));
}
