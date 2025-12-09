package com.momosoftworks.irradiated;

import com.momosoftworks.irradiated.common.command.RadiationCommand;
import com.momosoftworks.irradiated.common.debug.RadiationDebugServer;
import com.momosoftworks.irradiated.common.radiation.DynamicRadiationHandler;
import com.momosoftworks.irradiated.common.radiation.RadiationConfig;
import com.momosoftworks.irradiated.core.init.ModCreativeTab;
import com.momosoftworks.irradiated.core.init.ModEffects;
import com.momosoftworks.irradiated.core.init.ModItems;
import com.momosoftworks.irradiated.core.init.ModParticles;
import com.momosoftworks.irradiated.core.init.ModSounds;
import com.momosoftworks.irradiated.common.temp.RadiationTempHandler;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;

@Mod(Irradiated.MOD_ID)
public class Irradiated {
	public static final String MOD_ID = "irradiated";

	public Irradiated(IEventBus modBus, ModContainer modContainer) {
		// Register items, effects, particles, sounds, and creative tabs
		ModItems.ITEMS.register(modBus);
		ModEffects.REGISTER.register(modBus);
		ModParticles.REGISTER.register(modBus);
		ModSounds.REGISTER.register(modBus);
		ModCreativeTab.REGISTER.register(modBus);

		// Register common config (works on both client and server)
		// Config file will be at: config/irradiated/irradiated-common.toml
		modContainer.registerConfig(net.neoforged.fml.config.ModConfig.Type.COMMON, RadiationConfig.SPEC, "irradiated/irradiated-common.toml");

		// Register client config and extensions only on client side
		if (FMLEnvironment.dist == Dist.CLIENT) {
			registerClientConfig(modContainer);
			registerClientExtensions(modContainer);
		}

		// Register radiation systems (server-side mechanics)
		NeoForge.EVENT_BUS.register(DynamicRadiationHandler.class);
		NeoForge.EVENT_BUS.addListener(RadiationTempHandler::onPlayerTick);

		// Register commands (server-side)
		NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.RegisterCommandsEvent e) ->
			RadiationCommand.register(e.getDispatcher()));

		// Register debug server events (server-side)
		NeoForge.EVENT_BUS.addListener(this::onServerStarted);
		NeoForge.EVENT_BUS.addListener(this::onServerStopping);

		// Register client setup event listener only on client side
		if (FMLEnvironment.dist == Dist.CLIENT) {
			modBus.addListener(this::onClientSetup);
		}
	}

	private void onServerStarted(ServerStartedEvent event) {
		RadiationDebugServer.getInstance().start(event.getServer());
	}

	private void onServerStopping(ServerStoppingEvent event) {
		RadiationDebugServer.getInstance().stop();
	}

	/**
	 * Client-only initialization method. This is only called on the physical client.
	 * DO NOT call this method or reference it from server code.
	 */
	private void onClientSetup(FMLClientSetupEvent event) {
		// Use reflection to avoid direct class loading on server
		try {
			Class<?> clientClass = Class.forName("com.momosoftworks.irradiated.client.IrradiatedClient");
			clientClass.getMethod("init").invoke(null);
		} catch (Exception e) {
			throw new RuntimeException("Failed to initialize client", e);
		}
	}

	/**
	 * Registers client-only config.
	 * This method uses reflection to avoid loading client classes on server.
	 */
	private void registerClientConfig(ModContainer modContainer) {
		try {
			Class<?> clientConfigClass = Class.forName("com.momosoftworks.irradiated.client.config.ClientConfig");
			Object clientConfigSpec = clientConfigClass.getField("SPEC").get(null);
			// Config file will be at: config/irradiated/irradiated-client.toml
			modContainer.registerConfig(net.neoforged.fml.config.ModConfig.Type.CLIENT, (net.neoforged.neoforge.common.ModConfigSpec) clientConfigSpec, "irradiated/irradiated-client.toml");
		} catch (Exception e) {
			throw new RuntimeException("Failed to register client config", e);
		}
	}

	/**
	 * Registers client-only extension points like config screens.
	 * This method uses reflection to avoid loading client classes on server.
	 */
	private void registerClientExtensions(ModContainer modContainer) {
		try {
			Class<?> clientClass = Class.forName("com.momosoftworks.irradiated.client.IrradiatedClient");
			clientClass.getMethod("registerConfigScreen", ModContainer.class).invoke(null, modContainer);
		} catch (Exception e) {
			throw new RuntimeException("Failed to register client extensions", e);
		}
	}
}
