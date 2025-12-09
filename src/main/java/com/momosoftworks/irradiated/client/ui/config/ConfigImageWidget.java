package com.momosoftworks.irradiated.client.ui.config;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public class ConfigImageWidget extends AbstractWidget {
    private final ResourceLocation texture;
    private final int u;
    private final int v;
    
    public ConfigImageWidget(ResourceLocation texture, int x, int y, int width, int height, int u, int v) {
        super(x, y, width, height, Component.empty());
        this.texture = texture;
        this.u = u;
        this.v = v;
    }
    
    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.blit(this.texture, this.getX(), this.getY(), this.u, this.v, this.getWidth(), this.getHeight());
    }
    
    @Override
    protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
        // Image widgets don't need narration
    }
}
