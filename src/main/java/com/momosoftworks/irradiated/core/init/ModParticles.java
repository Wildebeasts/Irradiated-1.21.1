package com.momosoftworks.irradiated.core.init;

import com.momosoftworks.irradiated.Irradiated;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModParticles {
    public static final DeferredRegister<ParticleType<?>> REGISTER = DeferredRegister.create(Registries.PARTICLE_TYPE, Irradiated.MOD_ID);
    
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> HEARTH_AIR = REGISTER.register("hearth_air", () -> new SimpleParticleType(true));
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> STEAM = REGISTER.register("steam", () -> new SimpleParticleType(true));
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> MIST = REGISTER.register("mist", () -> new SimpleParticleType(true));
}
