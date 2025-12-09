package com.momosoftworks.irradiated.client.ui.config.pages;

import com.momosoftworks.irradiated.client.config.ClientConfig;
import com.momosoftworks.irradiated.client.ui.config.AbstractConfigPage;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Configuration page for HUD positioning and appearance.
 * All settings on this page are client-side only.
 */
public class HUDConfigPage extends AbstractConfigPage {

    public HUDConfigPage(Screen parentScreen) {
        super(parentScreen, Component.translatable("irradiated.config.hud.title"));
    }

    @Override
    public int getPageIndex() {
        return 1;
    }

    @Override
    public Component sectionOneTitle() {
        return Component.translatable("irradiated.config.section.hud_position");
    }

    @Override
    public Component sectionTwoTitle() {
        return Component.translatable("irradiated.config.section.hud_controls");
    }

    @Override
    protected void init() {
        super.init();

        // HUD Position settings (Left side) - ALL CLIENT-SIDE
        this.addDecimalInput("gauge_x", Side.LEFT,
            Component.translatable("irradiated.config.gauge_x"),
            value -> ClientConfig.HUD_GAUGE_OFFSET_X.set(value.intValue()),
            input -> input.setValue(String.valueOf(ClientConfig.HUD_GAUGE_OFFSET_X.get())),
            true,  // clientside
            false,
            true,
            Component.translatable("irradiated.config.gauge_x.desc"));

        this.addDecimalInput("gauge_y", Side.LEFT,
            Component.translatable("irradiated.config.gauge_y"),
            value -> ClientConfig.HUD_GAUGE_OFFSET_Y.set(value.intValue()),
            input -> input.setValue(String.valueOf(ClientConfig.HUD_GAUGE_OFFSET_Y.get())),
            true,  // clientside
            false,
            true,
            Component.translatable("irradiated.config.gauge_y.desc"));

        this.addDecimalInput("icon_x", Side.LEFT,
            Component.translatable("irradiated.config.icon_x"),
            value -> ClientConfig.HUD_BODY_OFFSET_X.set(value.intValue()),
            input -> input.setValue(String.valueOf(ClientConfig.HUD_BODY_OFFSET_X.get())),
            true,  // clientside
            false,
            true,
            Component.translatable("irradiated.config.icon_x.desc"));

        this.addDecimalInput("icon_y", Side.LEFT,
            Component.translatable("irradiated.config.icon_y"),
            value -> ClientConfig.HUD_BODY_OFFSET_Y.set(value.intValue()),
            input -> input.setValue(String.valueOf(ClientConfig.HUD_BODY_OFFSET_Y.get())),
            true,  // clientside
            false,
            true,
            Component.translatable("irradiated.config.icon_y.desc"));

        // HUD Control buttons (Right side)
        this.addButton("reset_positions", Side.RIGHT,
            () -> Component.translatable("irradiated.config.reset_positions"),
            button -> {
                // Reset to defaults
                ClientConfig.HUD_GAUGE_OFFSET_X.set(80);
                ClientConfig.HUD_GAUGE_OFFSET_Y.set(0);
                ClientConfig.HUD_BODY_OFFSET_X.set(-5);
                ClientConfig.HUD_BODY_OFFSET_Y.set(-5);
                ClientConfig.SPEC.save();

                // Refresh the screen to update input values
                this.minecraft.setScreen(new HUDConfigPage(this.parentScreen));
            },
            true,  // clientside
            false,
            true,
            Component.translatable("irradiated.config.reset_positions.desc"));

        this.addButton("preview_hud", Side.RIGHT,
            () -> Component.translatable("irradiated.config.preview_hud"),
            button -> {
                // Close the config screen to show the HUD in action
                this.minecraft.setScreen(null);
            },
            true,  // clientside
            false,
            true,
            Component.translatable("irradiated.config.preview_hud.desc"));
    }

    /**
     * Saves all HUD settings to the config file.
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
