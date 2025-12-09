package com.momosoftworks.irradiated.client.hud;

import com.mojang.blaze3d.vertex.PoseStack;
import com.momosoftworks.irradiated.api.radiation.RadiationAPI;
import com.momosoftworks.irradiated.core.init.ModEffects;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.event.RenderGuiEvent;

public class RadiationHudOverlay {
	public static void renderOverlay(RenderGuiEvent.Post event) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null) return;
		
		// Use RadiationAPI to get current radiation level (integrates both old and new systems)
		int radiationLevel = RadiationAPI.getRadiationLevel(mc.player);
		
		// Only show text overlay when there's radiation AND should show numbers
		if (radiationLevel <= 0) return;
		
		// Check if we should show radiation numbers (creative mode OR has geiger counter)
		boolean showNumbers = com.momosoftworks.irradiated.util.RadiationUtil.shouldShowRadiationNumbers(mc.player);
		
		// Only show text HUD if we should show numbers (creative mode OR geiger counter)
		if (!showNumbers) return;
		
		GuiGraphics g = event.getGuiGraphics();
		PoseStack pose = g.pose();
		pose.pushPose();
		
		// Check if player has an item in off-hand and adjust position accordingly
		Player player = mc.player;
		ItemStack offhandItem = player.getOffhandItem();
		boolean hasOffhandItem = !offhandItem.isEmpty();
		
		// Default position is on the right, move to left if off-hand has item
		int x = hasOffhandItem ? 10 : mc.getWindow().getGuiScaledWidth() - mc.font.width("RAD Lv." + radiationLevel) - 10;
		int y = 10;
		String text = "RAD Lv." + radiationLevel;
		g.drawString(mc.font, text, x, y, 0xFFFF55, true);
		pose.popPose();
	}
}
