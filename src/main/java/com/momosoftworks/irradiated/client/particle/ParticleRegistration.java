package com.momosoftworks.irradiated.client.particle;

import com.momosoftworks.irradiated.core.init.ModParticles;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.core.particles.SimpleParticleType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;

@EventBusSubscriber(modid = "irradiated", value = Dist.CLIENT)
public class ParticleRegistration {
    
    @SubscribeEvent
    public static void registerParticleProviders(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(ModParticles.HEARTH_AIR.get(), HearthParticle.Factory::new);
        event.registerSpriteSet(ModParticles.STEAM.get(), VaporParticle.SteamFactory::new);
        event.registerSpriteSet(ModParticles.MIST.get(), VaporParticle.MistFactory::new);
    }
}
