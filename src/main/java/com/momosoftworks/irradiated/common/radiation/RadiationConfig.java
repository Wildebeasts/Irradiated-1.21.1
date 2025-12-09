package com.momosoftworks.irradiated.common.radiation;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Configuration for the Irradiated mod's radiation system.
 * This config works on both client and server sides.
 *
 * <p>Configuration file is located at: config/irradiated-common.toml</p>
 *
 * <p><b>Radiation Sources:</b></p>
 * <ul>
 *   <li><b>Environmental:</b> Biomes (deserts, badlands, mushroom fields) and dimensions (Nether)</li>
 *   <li><b>Block-based:</b> Uranium ore, redstone, netherite blocks within range</li>
 *   <li><b>Mob attacks:</b> Certain monsters can inflict radiation (if enabled)</li>
 * </ul>
 *
 * <p><b>Radiation Levels:</b></p>
 * Radiation is measured on a 0-100 scale:
 * <ul>
 *   <li>0-20: Safe (no effects)</li>
 *   <li>20-40: Minor radiation sickness</li>
 *   <li>40-60: Moderate radiation sickness</li>
 *   <li>60-80: Severe radiation sickness</li>
 *   <li>80-100: Critical radiation (life-threatening)</li>
 * </ul>
 *
 * <p><b>Protection:</b></p>
 * Use RadAway to remove radiation and Rad-X to provide temporary immunity.
 */
public class RadiationConfig {
    public static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();
    public static final ModConfigSpec SPEC;
    
    // Environmental radiation settings
    public static final ModConfigSpec.BooleanValue ENABLE_ENVIRONMENTAL_RADIATION;
    public static final ModConfigSpec.BooleanValue ENABLE_BIOME_RADIATION;
    public static final ModConfigSpec.BooleanValue ENABLE_DIMENSION_RADIATION;
    public static final ModConfigSpec.BooleanValue ENABLE_BLOCK_RADIATION;
    
    // Mob radiation attack settings
    public static final ModConfigSpec.BooleanValue ENABLE_MOB_RADIATION;
    public static final ModConfigSpec.DoubleValue SLIME_RADIATION_MULTIPLIER;
    public static final ModConfigSpec.DoubleValue ZOMBIE_RADIATION_MULTIPLIER;
    
    // Radiation zone configurations
    public static final ModConfigSpec.DoubleValue DESERT_RADIATION_CHANCE;
    public static final ModConfigSpec.IntValue DESERT_MAX_LEVEL;
    public static final ModConfigSpec.DoubleValue BADLANDS_RADIATION_CHANCE;
    public static final ModConfigSpec.IntValue BADLANDS_MAX_LEVEL;
    public static final ModConfigSpec.DoubleValue MUSHROOM_RADIATION_CHANCE;
    public static final ModConfigSpec.IntValue MUSHROOM_MAX_LEVEL;
    
    // Nether radiation
    public static final ModConfigSpec.DoubleValue NETHER_RADIATION_CHANCE;
    public static final ModConfigSpec.IntValue NETHER_MAX_LEVEL;
    
    // Block radiation
    public static final ModConfigSpec.IntValue BLOCK_RADIATION_RANGE;
    public static final ModConfigSpec.DoubleValue URANIUM_RADIATION_CHANCE;
    public static final ModConfigSpec.IntValue URANIUM_MAX_LEVEL;
    public static final ModConfigSpec.ConfigValue<java.util.List<? extends String>> RADIOACTIVE_BLOCKS;
    
    // Dynamic radiation system settings
    public static final ModConfigSpec.BooleanValue ENABLE_DYNAMIC_RADIATION;
    public static final ModConfigSpec.DoubleValue RADIATION_BUILDUP_RATE;
    public static final ModConfigSpec.DoubleValue RADIATION_DECAY_RATE;
    public static final ModConfigSpec.IntValue RADIATION_DECAY_DELAY;
    
    static {
        BUILDER.comment(
            "=====================================",
            "  IRRADIATED MOD - RADIATION CONFIG",
            "=====================================",
            "",
            "This config controls all radiation mechanics in the game.",
            "Radiation builds up gradually when exposed to sources and decays slowly over time.",
            "Use RadAway to remove radiation and Rad-X for temporary immunity.",
            ""
        );

        BUILDER.push("Environmental Radiation");
        BUILDER.comment(
            "Master toggles for different types of environmental radiation sources.",
            "Disabling these will completely turn off the respective radiation types."
        );

        ENABLE_ENVIRONMENTAL_RADIATION = BUILDER
                .comment(
                    "Master switch for all environmental radiation (biomes, dimensions, blocks).",
                    "If disabled, only mob attacks and item effects will cause radiation.",
                    "Default: true"
                )
                .define("enableEnvironmentalRadiation", true);

        ENABLE_BIOME_RADIATION = BUILDER
                .comment(
                    "Enable radiation from radioactive biomes (deserts, badlands, mushroom fields).",
                    "Requires enableEnvironmentalRadiation to be true.",
                    "Default: true"
                )
                .define("enableBiomeRadiation", true);

        ENABLE_DIMENSION_RADIATION = BUILDER
                .comment(
                    "Enable radiation from radioactive dimensions (Nether).",
                    "Requires enableEnvironmentalRadiation to be true.",
                    "Default: true"
                )
                .define("enableDimensionRadiation", true);

        ENABLE_BLOCK_RADIATION = BUILDER
                .comment(
                    "Enable radiation from radioactive blocks (uranium ore, redstone, netherite).",
                    "Requires enableEnvironmentalRadiation to be true.",
                    "Default: true"
                )
                .define("enableBlockRadiation", true);

        BUILDER.pop();
        
        BUILDER.push("Mob Radiation Attacks");
        
        ENABLE_MOB_RADIATION = BUILDER
                .comment("Enable radiation attacks from monsters")
                .define("enableMobRadiation", true);
        
        SLIME_RADIATION_MULTIPLIER = BUILDER
                .comment("Multiplier for slime radiation attacks")
                .defineInRange("slimeRadiationMultiplier", 1.0, 0.0, 10.0);
        
        ZOMBIE_RADIATION_MULTIPLIER = BUILDER
                .comment("Multiplier for zombie radiation attacks")
                .defineInRange("zombieRadiationMultiplier", 1.0, 0.0, 10.0);
        
        BUILDER.pop();
        
        BUILDER.push("Biome Radiation Settings");
        
        DESERT_RADIATION_CHANCE = BUILDER
                .comment("Chance per second for desert radiation (0.0-1.0)")
                .defineInRange("desertRadiationChance", 0.02, 0.0, 1.0);
        
        DESERT_MAX_LEVEL = BUILDER
                .comment("Maximum radiation level from deserts (1-100 scale)")
                .defineInRange("desertMaxLevel", 5, 0, 100);
        
        BADLANDS_RADIATION_CHANCE = BUILDER
                .comment("Chance per second for badlands radiation (0.0-1.0)")
                .defineInRange("badlandsRadiationChance", 0.05, 0.0, 1.0);
        
        BADLANDS_MAX_LEVEL = BUILDER
                .comment("Maximum radiation level from badlands (1-100 scale)")
                .defineInRange("badlandsMaxLevel", 10, 0, 100);
        
        MUSHROOM_RADIATION_CHANCE = BUILDER
                .comment("Chance per second for mushroom fields radiation (0.0-1.0)")
                .defineInRange("mushroomRadiationChance", 0.1, 0.0, 1.0);
        
        MUSHROOM_MAX_LEVEL = BUILDER
                .comment("Maximum radiation level from mushroom fields (1-100 scale)")
                .defineInRange("mushroomMaxLevel", 15, 0, 100);
        
        BUILDER.pop();
        
        BUILDER.push("Dimension Radiation Settings");
        
        NETHER_RADIATION_CHANCE = BUILDER
                .comment("Chance per second for nether radiation (0.0-1.0)")
                .defineInRange("netherRadiationChance", 0.03, 0.0, 1.0);
        
        NETHER_MAX_LEVEL = BUILDER
                .comment("Maximum radiation level from the nether (1-100 scale)")
                .defineInRange("netherMaxLevel", 8, 0, 100);
        
        BUILDER.pop();
        
        BUILDER.push("Block Radiation Settings");
        
        BLOCK_RADIATION_RANGE = BUILDER
                .comment("Range in blocks to check for radiation sources")
                .defineInRange("blockRadiationRange", 8, 1, 32);
        
        URANIUM_RADIATION_CHANCE = BUILDER
                .comment("Chance per second for uranium ore radiation (0.0-1.0)")
                .defineInRange("uraniumRadiationChance", 0.15, 0.0, 1.0);
        
        URANIUM_MAX_LEVEL = BUILDER
                .comment("Maximum radiation level from uranium ore (1-100 scale)")
                .defineInRange("uraniumMaxLevel", 20, 0, 100);

        RADIOACTIVE_BLOCKS = BUILDER
                .comment(
                    "List of blocks that emit radiation.",
                    "Format: \"modid:block_name\" (one per line)",
                    "Examples:",
                    "  - minecraft:redstone_block",
                    "  - minecraft:netherite_block",
                    "  - minecraft:ancient_debris",
                    "  - minecraft:uranium_ore (if added by other mods)",
                    "",
                    "Each block in this list will emit radiation based on URANIUM_RADIATION_CHANCE and URANIUM_MAX_LEVEL.",
                    "Add or remove blocks to customize which blocks are radioactive.",
                    "Default includes: redstone_block, netherite_block, ancient_debris"
                )
                .defineList("radioactiveBlocks",
                    java.util.Arrays.asList(
                        "minecraft:redstone_block",
                        "minecraft:netherite_block",
                        "minecraft:ancient_debris"
                    ),
                    obj -> obj instanceof String && ((String) obj).contains(":"));

        BUILDER.pop();
        
        BUILDER.push("Dynamic Radiation System");
        BUILDER.comment(
            "Controls how radiation accumulates and dissipates over time.",
            "The dynamic system makes radiation feel more realistic - it builds up gradually when exposed",
            "and decays slowly after leaving the radiation source."
        );

        ENABLE_DYNAMIC_RADIATION = BUILDER
                .comment(
                    "Enable dynamic radiation system with gradual buildup and decay.",
                    "If disabled, radiation will apply instantly at full strength.",
                    "Default: true"
                )
                .define("enableDynamicRadiation", true);

        RADIATION_BUILDUP_RATE = BUILDER
                .comment(
                    "Multiplier for how quickly radiation builds up when exposed to sources.",
                    "Higher = faster buildup. Lower = slower buildup (more time to escape).",
                    "Range: 0.001 to 10.0",
                    "Default: 1.0"
                )
                .defineInRange("radiationBuildupRate", 1.0, 0.001, 10.0);

        RADIATION_DECAY_RATE = BUILDER
                .comment(
                    "Base rate at which radiation decays naturally when not exposed.",
                    "Higher = faster decay. Lower = radiation lingers longer.",
                    "Note: RadAway and other items provide faster decay rates.",
                    "Range: 0.001 to 0.1",
                    "Default: 0.01"
                )
                .defineInRange("radiationDecayRate", 0.01, 0.001, 0.1);

        RADIATION_DECAY_DELAY = BUILDER
                .comment(
                    "Time in seconds before radiation starts decaying after exposure ends.",
                    "During this delay, radiation level stays constant.",
                    "Simulates the 'half-life' concept where radiation doesn't immediately dissipate.",
                    "Range: 30 to 1800 seconds (30 sec to 30 min)",
                    "Default: 180 (3 minutes)"
                )
                .defineInRange("radiationDecayDelay", 180, 30, 1800);

        BUILDER.pop();
        
        SPEC = BUILDER.build();
    }
}
