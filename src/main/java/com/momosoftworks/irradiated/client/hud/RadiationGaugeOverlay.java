package com.momosoftworks.irradiated.client.hud;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.momosoftworks.irradiated.core.init.ModEffects;
import com.momosoftworks.irradiated.util.RadiationUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.neoforged.neoforge.client.event.RenderGuiEvent;

public class RadiationGaugeOverlay {
	private static final ResourceLocation VAGUE_GAUGE = ResourceLocation.fromNamespaceAndPath("irradiated", "textures/gui/overlay/vague_temp_gauge.png");
    private static final ResourceLocation BODY_GAUGE  = ResourceLocation.fromNamespaceAndPath("irradiated", "textures/gui/overlay/body_temp_gauge.png");
    private static final ResourceLocation FREEZE_OVERLAY  = ResourceLocation.fromNamespaceAndPath("irradiated", "textures/gui/overlay/freeze_overlay.png");
    private static final int GAUGE_OFFSET_X = com.momosoftworks.irradiated.client.hud.RadiationHudSettings.gaugeOffsetX;
    private static final int GAUGE_OFFSET_Y = com.momosoftworks.irradiated.client.hud.RadiationHudSettings.gaugeOffsetY;
    private static final int BODY_OFFSET_X  = com.momosoftworks.irradiated.client.hud.RadiationHudSettings.bodyOffsetX;
    private static final int BODY_OFFSET_Y  = com.momosoftworks.irradiated.client.hud.RadiationHudSettings.bodyOffsetY;
    
    private static int iconBob = 0;
    private static boolean enableIconBobbing = true; // Can be config option

	public static void onRenderGui(RenderGuiEvent.Post event) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null || mc.options.hideGui) return;

		// Use RadiationAPI to get current radiation level (integrates both old and new systems)
		int radiationLevel = com.momosoftworks.irradiated.api.radiation.RadiationAPI.getRadiationLevel(mc.player);
		
		// Check if we should show numbers (creative mode OR has geiger counter)
		boolean showNumbers = RadiationUtil.shouldShowRadiationNumbers(mc.player);

		GuiGraphics g = event.getGuiGraphics();
		PoseStack pose = g.pose();
		int width = mc.getWindow().getGuiScaledWidth();
		int height = mc.getWindow().getGuiScaledHeight();

		// Use radiation level from API, default to 0 if no radiation
		int level = Math.max(0, radiationLevel);
		
		// Check if thermometer is equipped to adjust body icon positioning only
		boolean hasThermometer = RadiationUtil.hasThermometer(mc.player);
		
		// Check if player has an item in off-hand to adjust positioning
		boolean hasOffhandItem = !mc.player.getOffhandItem().isEmpty();
		
		// Map radiation level (1-100) to gauge severity (1-4) for visual representation
		// Using the full 100-scale range properly
		int severity;
		if (level <= 25) {
			severity = 1; // Low radiation (green) - 1-25
		} else if (level <= 50) {
			severity = 2; // Moderate radiation (yellow) - 26-50
		} else if (level <= 75) {
			severity = 3; // High radiation (orange) - 51-75
		} else {
			severity = 4; // Dangerous radiation (red) - 76-100
		}
		
		// Icon bobbing for dangerous radiation levels
		int bobOffset = 0;
		if (enableIconBobbing && severity >= 3) {
			if (severity == 3) {
				bobOffset = iconBob;
			} else if (severity == 4) {
				bobOffset = mc.player.tickCount % 2; // Fast bobbing for extreme danger
			}
		}
		
		int renderOffset = Math.max(-1, Math.min(1, severity)) * 2;

		pose.pushPose();
		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();
		
		// Adjust X position when off-hand item is present
		int offhandOffset = hasOffhandItem ? -30 : 0; // Move left by 30 pixels when off-hand has item
		
		// Only show vague gauge (indicator) when showing numbers
		if (showNumbers) {
			// World/radiation vague gauge (positioned to the left of hotbar)
			g.blit(VAGUE_GAUGE,
				width / 2 - 110 + offhandOffset, // Adjusted for off-hand items
				height - 17 - renderOffset, // Positioned next to hotbar
				0,
				64 - severity * 16,
				16,
				16,
				16,
				144);
		}

		// Only show body icon when NOT showing numbers (like ColdSweat)
		if (!showNumbers) {
			// Adjust Y position when thermometer is equipped to prevent overlap with thermometer body icon
			int thermometerOffset = hasThermometer ? -8 : 0; // Move up by 8 pixels when thermometer is present
			
			// Body icon above hotbar, positioned in the middle of hotbar
			int bodyFrame = Math.min(3, Math.max(0, severity));
			g.blit(BODY_GAUGE,
				width / 2 + BODY_OFFSET_X,
				height - 53 + BODY_OFFSET_Y - bobOffset + thermometerOffset, // Apply bobbing offset and thermometer adjustment
				0,
				30 - bodyFrame * 10,
				10,
				10,
				10,
				70);
		}

		// Update icon bob animation
		if (mc.player.tickCount % 3 == 0 && Math.random() < 0.3) {
			iconBob = 1;
		} else {
			iconBob = 0;
		}

		// Only show radiation level text when in creative or has geiger counter
		if (showNumbers) {
			// Radiation level text display (shows actual radiation level 1-100)
			String radText = "" + level;
			// Calculate text width for right alignment
			int textWidth = mc.font.width(radText);
			int textX = width / 2 - 110 - 5 - textWidth + offhandOffset; // Right-aligned to the left of indicator, adjusted for off-hand
			int textY = height - 15; // Centered with hotbar and indicator
			
			// Color based on radiation level (1-100 scale)
			int textColor;
			int textColorBG;
			if (level >= 90) {
				textColor = 0xFF0000; // Bright red for near-death levels
				textColorBG = 0x800000;
			} else if (level >= 60) {
				textColor = 0xFF6600; // Orange for dangerous levels
				textColorBG = 0x803300;
			} else if (level >= 30) {
				textColor = 0xFFFF00; // Yellow for moderate levels
				textColorBG = 0x808000;
			} else if (level >= 10) {
				textColor = 0x00FF00; // Green for low levels
				textColorBG = 0x008000;
			} else {
				textColor = 0x99FF99; // Light green for very low levels
				textColorBG = 0x004400;
			}
			
			// Draw text with outline (like original)
			g.drawString(mc.font, radText, textX + 1, textY, textColorBG);
			g.drawString(mc.font, radText, textX - 1, textY, textColorBG);
			g.drawString(mc.font, radText, textX, textY + 1, textColorBG);
			g.drawString(mc.font, radText, textX, textY - 1, textColorBG);
			g.drawString(mc.font, radText, textX, textY, textColor);
		}

		pose.popPose();
	}
}
