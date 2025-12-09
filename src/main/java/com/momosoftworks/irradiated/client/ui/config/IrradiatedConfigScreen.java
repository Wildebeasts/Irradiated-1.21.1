package com.momosoftworks.irradiated.client.ui.config;

import com.momosoftworks.irradiated.client.ui.config.pages.RadiationConfigPage;
import com.momosoftworks.irradiated.client.ui.config.pages.HUDConfigPage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

@EventBusSubscriber(value = Dist.CLIENT)
public class IrradiatedConfigScreen {
    public static final int TITLE_HEIGHT = 16;
    public static final int BOTTOM_BUTTON_HEIGHT_OFFSET = 26;
    public static final int OPTION_SIZE = 25;
    public static final int BOTTOM_BUTTON_WIDTH = 150;

    public static Minecraft MC = Minecraft.getInstance();
    public static DecimalFormat TWO_PLACES = new DecimalFormat("#.##");

    public static boolean IS_MOUSE_DOWN = false;
    public static int MOUSE_X = 0;
    public static int MOUSE_Y = 0;

    static List<Function<Screen, AbstractConfigPage>> PAGES = new ArrayList<>(Arrays.asList(
        RadiationConfigPage::new,
        HUDConfigPage::new
    ));
    
    public static int FIRST_PAGE = 0;
    public static int LAST_PAGE = PAGES.size() - 1;
    public static int CURRENT_PAGE = 0;

    public static Screen getPage(int index, Screen parentScreen) {
        index = Math.max(FIRST_PAGE, Math.min(LAST_PAGE, index));
        CURRENT_PAGE = index;
        return PAGES.get(index).apply(parentScreen);
    }

    public static int getLastPage() {
        return LAST_PAGE;
    }

    /**
     * Saves all configuration changes made in the config screens.
     * This method saves client-side settings to the config file.
     * Called when the user closes the config screen or clicks "Done".
     */
    public static void saveConfig() {
        // Save all client-side settings
        RadiationConfigPage.saveSettings();
        HUDConfigPage.saveSettings();
    }

    @SubscribeEvent
    public static void onClicked(ScreenEvent.MouseButtonPressed.Pre event) {
        if (event.getButton() == 0 && Minecraft.getInstance().screen instanceof AbstractConfigPage)
            IS_MOUSE_DOWN = true;
    }

    @SubscribeEvent
    public static void onReleased(ScreenEvent.MouseButtonReleased.Pre event) {
        if (event.getButton() == 0 && Minecraft.getInstance().screen instanceof AbstractConfigPage)
            IS_MOUSE_DOWN = false;
    }
}
