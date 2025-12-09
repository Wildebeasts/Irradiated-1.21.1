package com.momosoftworks.irradiated.client.config;

import org.joml.Vector2i;

/**
 * Client-side configuration settings for the Irradiated mod.
 * These settings control UI elements like the config button visibility and position.
 *
 * DO NOT reference this class from server-side code.
 */
public class IrradiatedConfigSettings {

    // Config button settings
    private static boolean showConfigButton = true;
    private static Vector2i configButtonPos = new Vector2i(-24, 0); // Positioned to the left of Cold Sweat button

    /**
     * Gets whether the config button should be shown in the options menu.
     *
     * @return true if the config button should be visible
     */
    public static boolean getShowConfigButton() {
        return showConfigButton;
    }

    /**
     * Sets whether the config button should be shown in the options menu.
     *
     * @param show true to show the config button, false to hide it
     */
    public static void setShowConfigButton(boolean show) {
        showConfigButton = show;
    }

    /**
     * Gets the position offset for the config button.
     *
     * @return Vector2i containing X and Y offset from the default position
     */
    public static Vector2i getConfigButtonPos() {
        return configButtonPos;
    }

    /**
     * Sets the position offset for the config button.
     *
     * @param pos Vector2i containing X and Y offset from the default position
     */
    public static void setConfigButtonPos(Vector2i pos) {
        configButtonPos = pos;
    }
}
