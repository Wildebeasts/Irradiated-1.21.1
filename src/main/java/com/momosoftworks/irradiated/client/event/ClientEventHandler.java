package com.momosoftworks.irradiated.client.event;

import com.momosoftworks.irradiated.client.ClientOnlyHelper;
import com.momosoftworks.irradiated.client.config.IrradiatedConfigSettings;
import com.momosoftworks.irradiated.client.ui.config.AbstractConfigPage;
import com.momosoftworks.irradiated.client.ui.config.IrradiatedConfigScreen;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.options.OptionsScreen;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;
import org.joml.Vector2i;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

@EventBusSubscriber(modid = "irradiated", value = Dist.CLIENT)
public class ClientEventHandler {
    
    @SubscribeEvent
    public static void onScreenInit(ScreenEvent.Init.Post event) {
        if (event.getScreen() instanceof OptionsScreen && IrradiatedConfigSettings.getShowConfigButton()) {
            // The offset from the config
            Supplier<Vector2i> buttonPos = () -> IrradiatedConfigSettings.getConfigButtonPos();
            AtomicInteger xOffset = new AtomicInteger(buttonPos.get().x());
            AtomicInteger yOffset = new AtomicInteger(buttonPos.get().y());
            int buttonX = event.getScreen().width / 2 - 183;
            int buttonY = event.getScreen().height / 6 + 110;
            
            if (xOffset.get() + buttonX < -1 || yOffset.get() + buttonY < -1) {
                xOffset.set(-24); // Default position to the left of Cold Sweat
                yOffset.set(0);
                IrradiatedConfigSettings.setConfigButtonPos(new Vector2i(-24, 0));
            }
            
            // Main config button
            ImageButton mainButton = new ImageButton(
                buttonX + xOffset.get(), 
                buttonY + yOffset.get(), 
                24, 24, 
                AbstractConfigPage.CONFIG_BUTTON_SPRITES,
                button -> {
                    ClientOnlyHelper.openConfigScreen();
                }
            );
            
            if (Minecraft.getInstance().level == null) {
                mainButton.active = false;
                mainButton.setTooltip(Tooltip.create(Component.translatable("tooltip.irradiated.config.must_be_in_game").withStyle(ChatFormatting.RED)));
            } else {
                mainButton.setTooltip(Tooltip.create(Component.translatable("tooltip.irradiated.config.open")));
            }
            
            // Add main button
            event.addListener(mainButton);
        }
    }
}
