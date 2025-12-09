package com.momosoftworks.irradiated.client;

import com.momosoftworks.irradiated.client.hud.RadiationGaugeOverlay;
import com.momosoftworks.irradiated.client.hud.RadiationHudSettings;
import com.momosoftworks.irradiated.client.hud.WorldTempGaugeOverlay;
import com.momosoftworks.irradiated.client.TooltipHandler;
import com.momosoftworks.irradiated.client.effect.TempEffectsClient;
import com.momosoftworks.irradiated.client.ui.config.IrradiatedConfigScreen;
import net.neoforged.fml.ModContainer;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import net.neoforged.neoforge.common.NeoForge;

/**
 * Client-side initialization for the Irradiated mod.
 * This class handles all client-only setup including HUD overlays,
 * visual effects, tooltips, and config screen registration.
 *
 * DO NOT reference this class from server-side code.
 */
public class IrradiatedClient {

    /**
     * Initializes all client-side features.
     * Called during FMLClientSetupEvent from the main mod class.
     */
    public static void init() {
        // Load client-side HUD settings from JSON config
        RadiationHudSettings.load();

        // Register HUD overlay renderers
        NeoForge.EVENT_BUS.addListener(RenderGuiEvent.Post.class, RadiationGaugeOverlay::onRenderGui);
        NeoForge.EVENT_BUS.addListener(RenderGuiEvent.Post.class, WorldTempGaugeOverlay::onRenderGui);

        // Register tooltip handler for items
        NeoForge.EVENT_BUS.addListener(ItemTooltipEvent.class, TooltipHandler::onItemTooltip);

        // Initialize visual effects (radiation vignette, etc.)
        TempEffectsClient.init();
    }

    /**
     * Registers the in-game configuration screen.
     * This allows players to configure the mod from the mod list screen.
     * Called from the main mod class during construction (client-side only).
     *
     * @param modContainer The mod container to register the config screen with
     */
    public static void registerConfigScreen(ModContainer modContainer) {
        modContainer.registerExtensionPoint(
            IConfigScreenFactory.class,
            new ConfigScreenFactory()
        );
    }

    /**
     * Factory for creating the config screen.
     * Uses the new page-based config system.
     */
    private static class ConfigScreenFactory implements IConfigScreenFactory {
        @Override
        public net.minecraft.client.gui.screens.Screen createScreen(ModContainer container, net.minecraft.client.gui.screens.Screen screen) {
            return (net.minecraft.client.gui.screens.Screen) IrradiatedConfigScreen.getPage(0, screen);
        }
    }
}


