package com.momosoftworks.irradiated.client.effect;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import com.momosoftworks.irradiated.core.init.ModEffects;

/** Temperature and radiation visual effects client handler. */
public class TempEffectsClient {
    private static final ResourceLocation VIGNETTE = ResourceLocation.fromNamespaceAndPath("minecraft", "textures/misc/vignette.png");
    
    // Temperature state tracking
    private static float blendTemp = 0.0F;
    private static float prevBlendTemp = 0.0F;
    private static int coldImmunity = 0;
    private static int hotImmunity = 0;
    
    // Radiation state tracking
    private static double prevRadAmount = 0.0D;
    private static double rads = 0.0D;
    private static int worldRads = 0;
    
    // Visual effect state
    private static float xSway = 0.0F;
    private static float ySway = 0.0F;
    private static float xSwayPhase = 0.0F;
    private static float ySwayPhase = 0.0F;
    private static float timeSinceNewSway = 0.0F;

    public static void init() {
        NeoForge.EVENT_BUS.addListener(TempEffectsClient::onRenderGui);
    }

    @SuppressWarnings("unused")
    public static void onRenderGui(RenderGuiEvent.Post event) {
        var mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui) return;

        var graphics = event.getGuiGraphics();
        var width = mc.getWindow().getGuiScaledWidth();
        var height = mc.getWindow().getGuiScaledHeight();
        
        // Update tracking in render loop (simplified)
        updateRadiationTracking(mc.player);
        updateTemperatureTracking(mc.player);
        updateVisualEffects();
        
        // Only render radiation effects here (avoid stacking with Cold Sweat temperature vignette)
        renderRadiationEffects(graphics, width, height);
    }

    private static void updateRadiationTracking(net.minecraft.world.entity.player.Player player) {
        // Check for radiation effect
        MobEffectInstance radEffect = player.getEffect(ModEffects.radiationHolder());
        if (radEffect != null) {
            rads = radEffect.getAmplifier() + 1;
        } else {
            rads = 0.0D;
        }
        
        // Play Geiger sound when radiation changes
        if (rads != prevRadAmount && rads > 0) {
            // TODO: Play Geiger counter sound
            // mc.getSoundManager().play(SimpleSound.forUI(ModSounds.GEIGER.get(), 1.0F));
        }
        
        prevRadAmount = rads;
    }

    private static void updateTemperatureTracking(net.minecraft.world.entity.player.Player player) {
        // Placeholder temperature calculation to keep sway logic functional
        float temp = 0.0F;
        if (player.isOnFire()) temp = 50.0F; else if (player.getFoodData().getFoodLevel() <= 6) temp = -30.0F; else if (player.isInWater()) temp = -10.0F;
        prevBlendTemp = blendTemp;
        blendTemp += (temp - blendTemp) * 0.1F;
        coldImmunity = player.hasEffect(ModEffects.radResistanceHolder()) ? 4 : 0;
        hotImmunity = player.isOnFire() ? 0 : 4;
    }

    private static void updateVisualEffects() {
        timeSinceNewSway += 0.1F;
        if (timeSinceNewSway > 10.0F) {
            xSwayPhase = (float) (Math.random() * Math.PI * 2);
            ySwayPhase = (float) (Math.random() * Math.PI * 2);
            timeSinceNewSway = 0.0F;
        }
        xSway += Math.sin(xSwayPhase) * 0.1F;
        ySway += Math.cos(ySwayPhase) * 0.1F;
        xSway *= 0.95F;
        ySway *= 0.95F;
    }

    private static void renderRadiationEffects(GuiGraphics graphics, int width, int height) {
        if (rads <= 0) return;
        
        // Base opacity scales with radiation amount (1-100 scale)
        float opacity = Math.min(0.8F, (float)(rads / 100.0) * 0.6F);
        if (opacity <= 0.0F) return;
        
        // Old-mod style pulsing brightness (approximate with system time for smoothness)
        var mc = Minecraft.getInstance();
        float timeFrac = (System.nanoTime() % 1_000_000_000L) / 1_000_000_000f; // 0..1 within one second
        float tickTime = mc.player.tickCount + timeFrac;
        float pulse = (float)Math.sin((tickTime + 3.0F) / 3.18306181683261D) / 5.0F - 0.2F;
        float vignetteBrightness = Math.max(0.0F, Math.min(1.0F, opacity + pulse * opacity));
        
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(0.0F, 1.0F, 0.0F, vignetteBrightness);
        graphics.blit(VIGNETTE, 0, 0, width, height, 0, 0, 256, 256, 256, 256);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.disableBlend();
    }
}


