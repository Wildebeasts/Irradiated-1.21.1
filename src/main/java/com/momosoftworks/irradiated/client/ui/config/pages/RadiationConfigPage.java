package com.momosoftworks.irradiated.client.ui.config.pages;

import com.momosoftworks.irradiated.client.config.ClientConfig;
import com.momosoftworks.irradiated.client.ui.config.AbstractConfigPage;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Configuration page for radiation-related settings.
 * This page displays both client-side and server-side settings with visual indicators.
 */
public class RadiationConfigPage extends AbstractConfigPage {

    public RadiationConfigPage(Screen parentScreen) {
        super(parentScreen, Component.translatable("irradiated.config.title"));
    }

    @Override
    public int getPageIndex() {
        return 0;
    }

    @Override
    public Component sectionOneTitle() {
        return Component.translatable("irradiated.config.section.visual");
    }

    @Override
    public Component sectionTwoTitle() {
        return Component.translatable("irradiated.config.section.audio");
    }

    @Override
    protected void init() {
        super.init();

        // Visual Effects (Left side) - ALL CLIENT-SIDE
        this.addButton("enable_hud", Side.LEFT,
            () -> this.getToggleButtonText(
                Component.translatable("irradiated.config.show_radiation_hud"),
                ClientConfig.SHOW_RADIATION_HUD.get()),
            button -> {
                ClientConfig.SHOW_RADIATION_HUD.set(!ClientConfig.SHOW_RADIATION_HUD.get());
            },
            true,  // clientside = true (shows indicator)
            false, // requireOP = false
            true,  // sync = true
            Component.translatable("irradiated.config.show_radiation_hud.desc"));

        this.addButton("enable_body_icon", Side.LEFT,
            () -> this.getToggleButtonText(
                Component.translatable("irradiated.config.show_body_icon"),
                ClientConfig.SHOW_BODY_ICON.get()),
            button -> {
                ClientConfig.SHOW_BODY_ICON.set(!ClientConfig.SHOW_BODY_ICON.get());
            },
            true,  // clientside
            false,
            true,
            Component.translatable("irradiated.config.show_body_icon.desc"));

        this.addButton("enable_vignette", Side.LEFT,
            () -> this.getToggleButtonText(
                Component.translatable("irradiated.config.enable_vignette"),
                ClientConfig.ENABLE_RADIATION_VIGNETTE.get()),
            button -> {
                ClientConfig.ENABLE_RADIATION_VIGNETTE.set(!ClientConfig.ENABLE_RADIATION_VIGNETTE.get());
            },
            true,  // clientside
            false,
            true,
            Component.translatable("irradiated.config.enable_vignette.desc"));

        this.addDecimalInput("vignette_intensity", Side.LEFT,
            Component.translatable("irradiated.config.vignette_intensity"),
            value -> ClientConfig.VIGNETTE_INTENSITY.set(value),
            input -> input.setValue(String.format("%.1f", ClientConfig.VIGNETTE_INTENSITY.get())),
            true,  // clientside
            false,
            false,
            Component.translatable("irradiated.config.vignette_intensity.desc"));

        this.addButton("enable_distortion", Side.LEFT,
            () -> this.getToggleButtonText(
                Component.translatable("irradiated.config.enable_distortion"),
                ClientConfig.ENABLE_SCREEN_DISTORTION.get()),
            button -> {
                ClientConfig.ENABLE_SCREEN_DISTORTION.set(!ClientConfig.ENABLE_SCREEN_DISTORTION.get());
            },
            true,  // clientside
            false,
            true,
            Component.translatable("irradiated.config.enable_distortion.desc"));

        // Audio Settings (Right side) - ALL CLIENT-SIDE
        this.addButton("enable_geiger", Side.RIGHT,
            () -> this.getToggleButtonText(
                Component.translatable("irradiated.config.enable_geiger"),
                ClientConfig.ENABLE_GEIGER_SOUNDS.get()),
            button -> {
                ClientConfig.ENABLE_GEIGER_SOUNDS.set(!ClientConfig.ENABLE_GEIGER_SOUNDS.get());
            },
            true,  // clientside
            false,
            true,
            Component.translatable("irradiated.config.enable_geiger.desc"));

        this.addDecimalInput("geiger_volume", Side.RIGHT,
            Component.translatable("irradiated.config.geiger_volume"),
            value -> ClientConfig.GEIGER_VOLUME.set(value),
            input -> input.setValue(String.format("%.1f", ClientConfig.GEIGER_VOLUME.get())),
            true,  // clientside
            false,
            false,
            Component.translatable("irradiated.config.geiger_volume.desc"));

        // Tooltip Settings (Right side) - ALL CLIENT-SIDE
        this.addButton("show_tooltips", Side.RIGHT,
            () -> this.getToggleButtonText(
                Component.translatable("irradiated.config.show_tooltips"),
                ClientConfig.SHOW_RADIATION_TOOLTIPS.get()),
            button -> {
                ClientConfig.SHOW_RADIATION_TOOLTIPS.set(!ClientConfig.SHOW_RADIATION_TOOLTIPS.get());
            },
            true,  // clientside
            false,
            true,
            Component.translatable("irradiated.config.show_tooltips.desc"));

        this.addButton("show_detailed_stats", Side.RIGHT,
            () -> this.getToggleButtonText(
                Component.translatable("irradiated.config.show_detailed_stats"),
                ClientConfig.SHOW_DETAILED_STATS.get()),
            button -> {
                ClientConfig.SHOW_DETAILED_STATS.set(!ClientConfig.SHOW_DETAILED_STATS.get());
            },
            true,  // clientside
            false,
            true,
            Component.translatable("irradiated.config.show_detailed_stats.desc"));
    }

    /**
     * Saves all client-side settings to the config file.
     * Called when the user clicks "Done" or "Save".
     */
    public static void saveSettings() {
        // Client config is automatically saved by NeoForge's config system
        ClientConfig.SPEC.save();
    }

    /**
     * Loads settings from the config file.
     * Called when the page is opened.
     */
    public static void loadSettings() {
        // Config values are automatically loaded by NeoForge
        // This method exists for consistency but doesn't need to do anything
    }
}
