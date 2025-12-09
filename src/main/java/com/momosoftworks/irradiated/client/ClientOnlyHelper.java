package com.momosoftworks.irradiated.client;

import com.momosoftworks.irradiated.client.ui.config.IrradiatedConfigScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;

/**
 * Helper class for client-only operations.
 * This class provides utility methods that should ONLY be called from client-side code.
 *
 * DO NOT reference this class from server-side code.
 */
public class ClientOnlyHelper {

    /**
     * Opens the mod's configuration screen.
     * This method uses Minecraft.getInstance() which is only available on the client.
     */
    public static void openConfigScreen() {
        Minecraft mc = Minecraft.getInstance();
        mc.setScreen(IrradiatedConfigScreen.getPage(0, mc.screen));
    }

    /**
     * Gets the client's local player.
     * This method uses Minecraft.getInstance() which is only available on the client.
     *
     * @return The client player, or null if not in a world
     */
    public static Player getClientPlayer() {
        return Minecraft.getInstance().player;
    }
}
