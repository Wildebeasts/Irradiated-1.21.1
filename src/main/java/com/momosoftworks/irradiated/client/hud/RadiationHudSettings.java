package com.momosoftworks.irradiated.client.hud;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import net.minecraft.util.GsonHelper;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Client-side settings for HUD positioning.
 * These settings are stored in config/irradiated_client.json and allow players
 * to customize the position of radiation overlays on their screen.
 *
 * <p><b>HUD Elements:</b></p>
 * <ul>
 *   <li><b>Radiation Gauge:</b> The main radiation meter display</li>
 *   <li><b>Body Icon:</b> The player body icon showing radiation effects</li>
 * </ul>
 *
 * <p>All positions are offsets from default anchor points and can be adjusted
 * in-game through the config screen.</p>
 *
 * DO NOT reference this class from server-side code.
 */
public class RadiationHudSettings {

    // Default positions for HUD elements
    private static final int DEFAULT_GAUGE_X = 80;
    private static final int DEFAULT_GAUGE_Y = 0;
    private static final int DEFAULT_BODY_X = -5;
    private static final int DEFAULT_BODY_Y = -5;

    /** X offset for the radiation gauge from its default position */
    public static int gaugeOffsetX = DEFAULT_GAUGE_X;

    /** Y offset for the radiation gauge from its default position */
    public static int gaugeOffsetY = DEFAULT_GAUGE_Y;

    /** X offset for the body icon from its default position (center of hotbar) */
    public static int bodyOffsetX = DEFAULT_BODY_X;

    /** Y offset for the body icon from its default position */
    public static int bodyOffsetY = DEFAULT_BODY_Y;
    
    // Getters for config system

    /** @return X offset for radiation gauge */
    public static int getGaugeX() {
        return gaugeOffsetX;
    }

    /** @return Y offset for radiation gauge */
    public static int getGaugeY() {
        return gaugeOffsetY;
    }

    /** @return X offset for body icon */
    public static int getIconX() {
        return bodyOffsetX;
    }

    /** @return Y offset for body icon */
    public static int getIconY() {
        return bodyOffsetY;
    }

    // Setters for config system

    /**
     * Sets the X offset for the radiation gauge.
     * @param x New X offset
     */
    public static void setGaugeX(int x) {
        gaugeOffsetX = x;
    }

    /**
     * Sets the Y offset for the radiation gauge.
     * @param y New Y offset
     */
    public static void setGaugeY(int y) {
        gaugeOffsetY = y;
    }

    /**
     * Sets the X offset for the body icon.
     * @param x New X offset
     */
    public static void setIconX(int x) {
        bodyOffsetX = x;
    }

    /**
     * Sets the Y offset for the body icon.
     * @param y New Y offset
     */
    public static void setIconY(int y) {
        bodyOffsetY = y;
    }

    /**
     * Resets all HUD elements to their default positions and saves the config.
     */
    public static void resetPositions() {
        gaugeOffsetX = DEFAULT_GAUGE_X;
        gaugeOffsetY = DEFAULT_GAUGE_Y;
        bodyOffsetX = DEFAULT_BODY_X;
        bodyOffsetY = DEFAULT_BODY_Y;
        save();
    }

    /**
     * Gets the path to the client config file.
     * @return Path to config/irradiated_client.json
     */
    private static Path configPath() {
        return Minecraft.getInstance().gameDirectory.toPath().resolve("config/irradiated_client.json");
    }

    /**
     * Loads HUD position settings from the client config file.
     * If the file doesn't exist, default values are used.
     * Called automatically during client initialization.
     */
    public static void load() {
        Path path = configPath();
        if (!Files.exists(path)) {
            return; // Use defaults
        }

        try (BufferedReader br = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            JsonObject json = GsonHelper.parse(br);

            if (json.has("gaugeOffsetX")) {
                gaugeOffsetX = json.get("gaugeOffsetX").getAsInt();
            }
            if (json.has("gaugeOffsetY")) {
                gaugeOffsetY = json.get("gaugeOffsetY").getAsInt();
            }
            if (json.has("bodyOffsetX")) {
                bodyOffsetX = json.get("bodyOffsetX").getAsInt();
            }
            if (json.has("bodyOffsetY")) {
                bodyOffsetY = json.get("bodyOffsetY").getAsInt();
            }
        } catch (IOException e) {
            // Silently ignore - will use defaults
        }
    }

    /**
     * Saves current HUD position settings to the client config file.
     * Creates the config directory if it doesn't exist.
     * The JSON file is formatted for easy manual editing.
     */
    public static void save() {
        Path path = configPath();
        try {
            Files.createDirectories(path.getParent());

            // Create formatted JSON for readability
            JsonObject json = new JsonObject();
            json.addProperty("_comment", "Irradiated Mod - Client HUD Settings");
            json.addProperty("_info", "Adjust these values to reposition HUD elements. Use the in-game config screen for a visual editor.");
            json.addProperty("gaugeOffsetX", gaugeOffsetX);
            json.addProperty("gaugeOffsetY", gaugeOffsetY);
            json.addProperty("bodyOffsetX", bodyOffsetX);
            json.addProperty("bodyOffsetY", bodyOffsetY);

            // Use pretty printing for better readability
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            String jsonString = gson.toJson(json);

            Files.writeString(path, jsonString, StandardCharsets.UTF_8);
        } catch (IOException e) {
            // Silently ignore - not critical
        }
    }
}


