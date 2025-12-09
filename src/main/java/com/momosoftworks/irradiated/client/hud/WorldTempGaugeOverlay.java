package com.momosoftworks.irradiated.client.hud;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.momosoftworks.irradiated.core.init.ModEffects;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.neoforged.neoforge.client.event.RenderGuiEvent;

public class WorldTempGaugeOverlay {
    private static final ResourceLocation WORLD_TEMP_GAUGE = ResourceLocation.fromNamespaceAndPath("irradiated", "textures/gui/overlay/world_temp_gauge.png");
    private static final ResourceLocation WORLD_TEMP_GAUGE_HC = ResourceLocation.fromNamespaceAndPath("irradiated", "textures/gui/overlay/world_temp_gauge_hc.png");
    
    private static boolean advancedMode = false; // Can be toggled by config or key
    private static double worldTemp = 0.0;
    private static double prevWorldTemp = 0.0;
    
    public static void onRenderGui(RenderGuiEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui || !advancedMode) return;

        MobEffectInstance rad = mc.player.getEffect(ModEffects.radiationHolder());
        if (rad == null) return;

        GuiGraphics g = event.getGuiGraphics();
        PoseStack pose = g.pose();
        int width = mc.getWindow().getGuiScaledWidth();
        int height = mc.getWindow().getGuiScaledHeight();

        int level = Math.max(1, rad.getAmplifier() + 1); // 1-100 scale
        
        // Map radiation level to world temperature severity
        int severity = Math.min(4, Math.max(-4, (level - 50) / 12)); // -4 to 4 range
        
        // Color based on severity
        int color;
        switch(severity) {
            case -4: color = 0x409FFF; break; // Deep blue
            case -3:
            case -2: color = 0x809FFF; break; // Light blue
            case -1:
            case 0:
            case 1:
            default: color = 0xE0E0E0; break; // White/gray
            case 2:
            case 3: color = 0x5BAD56; break; // Green
            case 4: color = 0x1ED71E; break; // Bright green
        }

        pose.pushPose();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        // Render world temperature gauge (horizontal bar)
        g.blit(WORLD_TEMP_GAUGE,
            width / 2 + 92, 
            height - 19,
            0,
            64 - severity * 16,
            25,
            16,
            25,
            144);

        // Render temperature text
        String tempText = "" + level;
        int textX = width / 2 + 105 - tempText.length() * 3;
        int textY = height - 15;
        
        // Draw text with outline
        g.drawString(mc.font, tempText, textX + 1, textY, 0x000000);
        g.drawString(mc.font, tempText, textX - 1, textY, 0x000000);
        g.drawString(mc.font, tempText, textX, textY + 1, 0x000000);
        g.drawString(mc.font, tempText, textX, textY - 1, 0x000000);
        g.drawString(mc.font, tempText, textX, textY, color);

        pose.popPose();
    }
    
    public static void setAdvancedMode(boolean advanced) {
        advancedMode = advanced;
    }
    
    public static boolean isAdvancedMode() {
        return advancedMode;
    }
}
