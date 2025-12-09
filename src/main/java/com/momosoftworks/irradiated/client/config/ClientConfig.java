package com.momosoftworks.irradiated.client.config;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Client-side configuration for the Irradiated mod.
 * These settings are stored locally and only affect the client's display and experience.
 * They do NOT affect gameplay mechanics or server behavior.
 *
 * <p>Configuration file is located at: config/irradiated-client.toml</p>
 *
 * <p><b>Client-Only Settings Include:</b></p>
 * <ul>
 *   <li>HUD visibility and positioning</li>
 *   <li>Visual effects (vignette, screen distortion)</li>
 *   <li>Audio settings (Geiger counter sounds)</li>
 *   <li>Tooltip display options</li>
 * </ul>
 *
 * DO NOT reference this class from server-side code.
 */
public class ClientConfig {
    public static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();
    public static final ModConfigSpec SPEC;

    // HUD Settings
    public static final ModConfigSpec.BooleanValue SHOW_RADIATION_HUD;
    public static final ModConfigSpec.BooleanValue SHOW_BODY_ICON;
    public static final ModConfigSpec.IntValue HUD_GAUGE_OFFSET_X;
    public static final ModConfigSpec.IntValue HUD_GAUGE_OFFSET_Y;
    public static final ModConfigSpec.IntValue HUD_BODY_OFFSET_X;
    public static final ModConfigSpec.IntValue HUD_BODY_OFFSET_Y;

    // Visual Effects
    public static final ModConfigSpec.BooleanValue ENABLE_RADIATION_VIGNETTE;
    public static final ModConfigSpec.DoubleValue VIGNETTE_INTENSITY;
    public static final ModConfigSpec.BooleanValue ENABLE_SCREEN_DISTORTION;

    // Audio Settings
    public static final ModConfigSpec.BooleanValue ENABLE_GEIGER_SOUNDS;
    public static final ModConfigSpec.DoubleValue GEIGER_VOLUME;

    // Tooltip Settings
    public static final ModConfigSpec.BooleanValue SHOW_RADIATION_TOOLTIPS;
    public static final ModConfigSpec.BooleanValue SHOW_DETAILED_STATS;

    static {
        BUILDER.comment(
            "=====================================",
            "  IRRADIATED MOD - CLIENT SETTINGS",
            "=====================================",
            "",
            "These settings only affect YOUR client display and experience.",
            "They do NOT change gameplay mechanics or affect other players.",
            "Server gameplay settings are in irradiated-common.toml",
            ""
        );

        BUILDER.push("HUD Display");
        BUILDER.comment(
            "Control the display and positioning of radiation HUD elements.",
            "These settings let you customize where the radiation gauge and body icon appear on your screen."
        );

        SHOW_RADIATION_HUD = BUILDER
                .comment(
                    "Show the radiation gauge HUD element.",
                    "When disabled, you won't see your current radiation level on screen.",
                    "Default: true"
                )
                .define("showRadiationHud", true);

        SHOW_BODY_ICON = BUILDER
                .comment(
                    "Show the body icon that indicates radiation effects.",
                    "Default: true"
                )
                .define("showBodyIcon", true);

        HUD_GAUGE_OFFSET_X = BUILDER
                .comment(
                    "Horizontal offset for the radiation gauge from its default position.",
                    "Positive moves right, negative moves left.",
                    "Range: -1000 to 1000",
                    "Default: 80"
                )
                .defineInRange("hudGaugeOffsetX", 80, -1000, 1000);

        HUD_GAUGE_OFFSET_Y = BUILDER
                .comment(
                    "Vertical offset for the radiation gauge from its default position.",
                    "Positive moves down, negative moves up.",
                    "Range: -1000 to 1000",
                    "Default: 0"
                )
                .defineInRange("hudGaugeOffsetY", 0, -1000, 1000);

        HUD_BODY_OFFSET_X = BUILDER
                .comment(
                    "Horizontal offset for the body icon from its default position.",
                    "Default position is centered on the hotbar.",
                    "Range: -1000 to 1000",
                    "Default: -5"
                )
                .defineInRange("hudBodyOffsetX", -5, -1000, 1000);

        HUD_BODY_OFFSET_Y = BUILDER
                .comment(
                    "Vertical offset for the body icon from its default position.",
                    "Range: -1000 to 1000",
                    "Default: -5"
                )
                .defineInRange("hudBodyOffsetY", -5, -1000, 1000);

        BUILDER.pop();

        BUILDER.push("Visual Effects");
        BUILDER.comment(
            "Control visual effects that indicate radiation exposure.",
            "These are client-side only and don't affect gameplay."
        );

        ENABLE_RADIATION_VIGNETTE = BUILDER
                .comment(
                    "Enable the green vignette effect when exposed to high radiation.",
                    "Creates a pulsing green overlay at the edges of the screen.",
                    "Default: true"
                )
                .define("enableRadiationVignette", true);

        VIGNETTE_INTENSITY = BUILDER
                .comment(
                    "Intensity multiplier for the radiation vignette effect.",
                    "Higher values make the effect more visible.",
                    "Range: 0.0 to 2.0",
                    "Default: 1.0"
                )
                .defineInRange("vignetteIntensity", 1.0, 0.0, 2.0);

        ENABLE_SCREEN_DISTORTION = BUILDER
                .comment(
                    "Enable screen distortion effects at high radiation levels.",
                    "May cause visual discomfort for some players.",
                    "Default: true"
                )
                .define("enableScreenDistortion", true);

        BUILDER.pop();

        BUILDER.push("Audio Settings");
        BUILDER.comment(
            "Control audio feedback for radiation exposure."
        );

        ENABLE_GEIGER_SOUNDS = BUILDER
                .comment(
                    "Enable Geiger counter clicking sounds based on radiation level.",
                    "Sounds increase in frequency with higher radiation.",
                    "Default: true"
                )
                .define("enableGeigerSounds", true);

        GEIGER_VOLUME = BUILDER
                .comment(
                    "Volume multiplier for Geiger counter sounds.",
                    "Range: 0.0 (muted) to 1.0 (full volume)",
                    "Default: 0.5"
                )
                .defineInRange("geigerVolume", 0.5, 0.0, 1.0);

        BUILDER.pop();

        BUILDER.push("Tooltips");
        BUILDER.comment(
            "Control what information is displayed in item tooltips."
        );

        SHOW_RADIATION_TOOLTIPS = BUILDER
                .comment(
                    "Show radiation-related information in item tooltips.",
                    "Displays effects like 'Removes X radiation' on RadAway, etc.",
                    "Default: true"
                )
                .define("showRadiationTooltips", true);

        SHOW_DETAILED_STATS = BUILDER
                .comment(
                    "Show detailed statistics in tooltips.",
                    "Includes exact numbers for radiation removal, durations, etc.",
                    "Default: false"
                )
                .define("showDetailedStats", false);

        BUILDER.pop();

        SPEC = BUILDER.build();
    }
}
