package com.momosoftworks.irradiated.client.ui.config;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

public class ConfigLabel extends AbstractWidget {
    private final String id;
    
    public ConfigLabel(String id, Component text, int x, int y) {
        super(x, y, 0, 0, text);
        this.id = id;
    }
    
    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.drawString(Minecraft.getInstance().font, this.getMessage(), this.getX(), this.getY(), 0xFFFFFF);
    }
    
    @Override
    protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
        this.defaultButtonNarrationText(narrationElementOutput);
    }
    
    public String getId() {
        return this.id;
    }
}
