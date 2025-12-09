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
    public static final ModConfigSpec.ConfigValue<java.util.List<? extends String>> RADIOACTIVE_BLOCKS;
    
    // Radiation shielding
    public static final ModConfigSpec.BooleanValue ENABLE_RADIATION_SHIELDING;
    public static final ModConfigSpec.DoubleValue DEFAULT_BLOCK_SHIELDING;
    public static final ModConfigSpec.ConfigValue<java.util.List<? extends String>> SHIELDING_BLOCKS;
    
    // Armor protection
    public static final ModConfigSpec.BooleanValue ENABLE_ARMOR_PROTECTION;
    public static final ModConfigSpec.DoubleValue ARMOR_PROTECTION_PER_POINT;
    
    // Dynamic radiation system settings
    public static final ModConfigSpec.BooleanValue ENABLE_DYNAMIC_RADIATION;
    public static final ModConfigSpec.DoubleValue RADIATION_BUILDUP_RATE;
    public static final ModConfigSpec.DoubleValue RADIATION_DECAY_RATE;
    public static final ModConfigSpec.IntValue RADIATION_DECAY_DELAY;
    
    // Water decontamination settings
    public static final ModConfigSpec.BooleanValue ENABLE_WATER_DECONTAMINATION;
    public static final ModConfigSpec.DoubleValue WATER_BUILDUP_REDUCTION;
    public static final ModConfigSpec.IntValue WATER_DECAY_DELAY_SECONDS;
    public static final ModConfigSpec.DoubleValue WATER_ACTIVE_DECONTAMINATION_RATE;
    
    // Early game radiation removal items
    public static final ModConfigSpec.BooleanValue ENABLE_MILK_RADIATION_REMOVAL;
    public static final ModConfigSpec.DoubleValue MILK_RADIATION_REMOVAL_AMOUNT;
    public static final ModConfigSpec.BooleanValue ENABLE_TOMATO_RADIATION_REMOVAL;
    public static final ModConfigSpec.DoubleValue TOMATO_RADIATION_REMOVAL_AMOUNT;
    public static final ModConfigSpec.DoubleValue TOMATO_SLICE_RADIATION_REMOVAL_AMOUNT;
    public static final ModConfigSpec.DoubleValue TOMATO_SAUCE_RADIATION_REMOVAL_AMOUNT;
    public static final ModConfigSpec.DoubleValue TOMATO_SANDWICH_RADIATION_REMOVAL_AMOUNT;
    
    // Debug server settings
    public static final ModConfigSpec.BooleanValue ENABLE_DEBUG_SERVER;
    public static final ModConfigSpec.IntValue DEBUG_SERVER_PORT;
    public static final ModConfigSpec.IntValue DEBUG_UPDATE_INTERVAL_MS;
    
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

        RADIOACTIVE_BLOCKS = BUILDER
                .comment(
                    "List of blocks that emit radiation with their radiation values.",
                    "Format: \"modid:block_name:chance_per_second:max_level\"",
                    "  - chance_per_second: Radiation chance per second (0.0-1.0)",
                    "  - max_level: Maximum radiation level this block can cause (1-100)",
                    "",
                    "Examples:",
                    "  - minecraft:redstone_block:0.05:10",
                    "  - minecraft:netherite_block:0.15:25",
                    "  - minecraft:ancient_debris:0.20:30",
                    "",
                    "Only blocks in this list will emit radiation.",
                    "Add or remove entries to customize which blocks are radioactive."
                )
                .defineList("radioactiveBlocks",
                    java.util.Arrays.asList(
                        "minecraft:redstone_block:0.05:10",
                        "minecraft:netherite_block:0.15:25",
                        "minecraft:ancient_debris:0.20:30"
                    ),
                    obj -> obj instanceof String && ((String) obj).split(":").length >= 4);

        BUILDER.pop();
        
        BUILDER.push("Radiation Shielding");
        BUILDER.comment(
            "Controls how blocks between radiation sources and players reduce radiation.",
            "When enabled, solid blocks in the line of sight will reduce incoming radiation."
        );

        ENABLE_RADIATION_SHIELDING = BUILDER
                .comment(
                    "Enable radiation shielding from blocks.",
                    "When enabled, blocks between a radiation source and the player will reduce radiation.",
                    "Default: true"
                )
                .define("enableRadiationShielding", true);

        DEFAULT_BLOCK_SHIELDING = BUILDER
                .comment(
                    "Default shielding value for solid blocks not in the shieldingBlocks list.",
                    "This is a percentage of radiation blocked (0-100).",
                    "Set to 0 to make unlisted blocks provide no shielding.",
                    "Default: 5.0 (5% radiation blocked per block)"
                )
                .defineInRange("defaultBlockShielding", 5.0, 0.0, 100.0);

        SHIELDING_BLOCKS = BUILDER
                .comment(
                    "List of blocks with custom shielding values.",
                    "Format: \"modid:block_name:shielding_percent\"",
                    "  - shielding_percent: Percentage of radiation blocked by this block (0-100)",
                    "",
                    "Examples:",
                    "  - minecraft:obsidian:25 (blocks 25% of radiation)",
                    "  - minecraft:iron_block:15",
                    "  - minecraft:lead_block:50 (if added by other mods)",
                    "  - minecraft:stone:5",
                    "  - minecraft:deepslate:8",
                    "",
                    "Shielding is cumulative - multiple blocks stack their shielding values.",
                    "Total shielding is capped at 100% (full radiation block)."
                )
                .defineList("shieldingBlocks",
                    java.util.Arrays.asList(
                        "minecraft:obsidian:25",
                        "minecraft:crying_obsidian:25",
                        "minecraft:iron_block:15",
                        "minecraft:gold_block:12",
                        "minecraft:copper_block:10",
                        "minecraft:deepslate:8",
                        "minecraft:stone:5",
                        "minecraft:cobblestone:4",
                        "minecraft:dirt:2",
                        "minecraft:sand:1"
                    ),
                    obj -> obj instanceof String && ((String) obj).split(":").length >= 3);

        BUILDER.pop();
        
        BUILDER.push("Armor Protection");
        BUILDER.comment(
            "Controls how armor protects against radiation.",
            "Armor can reduce incoming radiation based on its defense points."
        );

        ENABLE_ARMOR_PROTECTION = BUILDER
                .comment(
                    "Enable armor-based radiation protection.",
                    "When enabled, wearing armor will reduce radiation exposure.",
                    "Default: true"
                )
                .define("enableArmorProtection", true);

        ARMOR_PROTECTION_PER_POINT = BUILDER
                .comment(
                    "Percentage of radiation blocked per armor point.",
                    "Full diamond armor (20 armor points) = 20 * 2.5 = 50% protection",
                    "Full iron armor (15 armor points) = 15 * 2.5 = 37.5% protection",
                    "Full leather armor (7 armor points) = 7 * 2.5 = 17.5% protection",
                    "Range: 0.0 to 10.0",
                    "Default: 2.5"
                )
                .defineInRange("armorProtectionPerPoint", 2.5, 0.0, 10.0);

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
        
        BUILDER.push("Water Decontamination");
        BUILDER.comment(
            "Settings for water-based radiation removal.",
            "Water provides three types of protection:",
            "1. Reduces radiation buildup while exposed",
            "2. Faster decay when not exposed",
            "3. Active decontamination (removes radiation slowly)"
        );

        ENABLE_WATER_DECONTAMINATION = BUILDER
                .comment(
                    "Enable water to reduce and remove radiation.",
                    "When enabled, being in water/rain provides radiation protection.",
                    "Default: true"
                )
                .define("enableWaterDecontamination", true);

        WATER_BUILDUP_REDUCTION = BUILDER
                .comment(
                    "Percentage of radiation buildup reduction when in water.",
                    "1.0 = no buildup reduction, 0.5 = 50% less buildup, 0.0 = no buildup at all.",
                    "Range: 0.0 to 1.0",
                    "Default: 0.5 (50% less radiation buildup in water)"
                )
                .defineInRange("waterBuildupReduction", 0.5, 0.0, 1.0);

        WATER_DECAY_DELAY_SECONDS = BUILDER
                .comment(
                    "Delay in seconds before radiation decays when in water.",
                    "Compare to normal decay delay (default 180 seconds).",
                    "Lower = faster decay in water.",
                    "Range: 0 to 60 seconds",
                    "Default: 5 (much faster than normal 180 seconds)"
                )
                .defineInRange("waterDecayDelaySeconds", 5, 0, 60);

        WATER_ACTIVE_DECONTAMINATION_RATE = BUILDER
                .comment(
                    "Amount of radiation actively removed per second when in water.",
                    "This removal happens even while being exposed to radiation.",
                    "Range: 0.0 to 10.0 radiation per second",
                    "Default: 0.1 (removes 0.1 RAD per second)"
                )
                .defineInRange("waterActiveDecontaminationRate", 0.1, 0.0, 10.0);

        BUILDER.pop();
        
        BUILDER.push("Early Game Radiation Removal");
        BUILDER.comment(
            "Settings for early-game radiation removal using vanilla/Farmer's Delight items.",
            "These provide a cheaper alternative to RadAway before you can craft it."
        );

        ENABLE_MILK_RADIATION_REMOVAL = BUILDER
                .comment(
                    "Enable milk buckets to remove radiation when consumed.",
                    "Provides an early-game option before you have RadAway.",
                    "Default: true"
                )
                .define("enableMilkRadiationRemoval", true);

        MILK_RADIATION_REMOVAL_AMOUNT = BUILDER
                .comment(
                    "Amount of radiation removed by drinking milk.",
                    "For comparison, RadAway removes 25 radiation.",
                    "Range: 0.0 to 100.0",
                    "Default: 10.0 (early game option)"
                )
                .defineInRange("milkRadiationRemovalAmount", 10.0, 0.0, 100.0);

        ENABLE_TOMATO_RADIATION_REMOVAL = BUILDER
                .comment(
                    "Enable tomatoes (from Farmer's Delight) to remove radiation when eaten.",
                    "Provides a renewable early-game option.",
                    "Requires Farmer's Delight mod to be installed.",
                    "Default: true"
                )
                .define("enableTomatoRadiationRemoval", true);

        TOMATO_RADIATION_REMOVAL_AMOUNT = BUILDER
                .comment(
                    "Amount of radiation removed by eating a whole tomato.",
                    "For comparison, RadAway removes 25, milk removes 10.",
                    "Range: 0.0 to 100.0",
                    "Default: 5.0 (small but renewable)"
                )
                .defineInRange("tomatoRadiationRemovalAmount", 5.0, 0.0, 100.0);

        TOMATO_SLICE_RADIATION_REMOVAL_AMOUNT = BUILDER
                .comment(
                    "Amount of radiation removed by eating tomato slices (Farmer's Delight).",
                    "Should be less than a whole tomato since it's sliced.",
                    "Range: 0.0 to 100.0",
                    "Default: 2.0"
                )
                .defineInRange("tomatoSliceRadiationRemovalAmount", 2.0, 0.0, 100.0);

        TOMATO_SAUCE_RADIATION_REMOVAL_AMOUNT = BUILDER
                .comment(
                    "Amount of radiation removed by eating tomato sauce (Farmer's Delight).",
                    "Concentrated tomato product, more effective than slices.",
                    "Range: 0.0 to 100.0",
                    "Default: 7.0"
                )
                .defineInRange("tomatoSauceRadiationRemovalAmount", 7.0, 0.0, 100.0);

        TOMATO_SANDWICH_RADIATION_REMOVAL_AMOUNT = BUILDER
                .comment(
                    "Amount of radiation removed by eating sandwiches containing tomatoes.",
                    "From Some Assembly Required mod. Fills hunger + removes radiation.",
                    "Range: 0.0 to 100.0",
                    "Default: 6.0 (good food + radiation removal)"
                )
                .defineInRange("tomatoSandwichRadiationRemovalAmount", 6.0, 0.0, 100.0);

        BUILDER.pop();
        
        BUILDER.push("Debug Server");
        BUILDER.comment(
            "Settings for the web-based radiation debug dashboard.",
            "WARNING: Only enable this for debugging purposes!",
            "The server binds to localhost only and is not accessible from other machines."
        );

        ENABLE_DEBUG_SERVER = BUILDER
                .comment(
                    "Enable the radiation debug web server.",
                    "When enabled, a web dashboard will be available at http://localhost:<port>/",
                    "Default: false (disabled for security)"
                )
                .define("enableDebugServer", false);

        DEBUG_SERVER_PORT = BUILDER
                .comment(
                    "Port for the debug web server.",
                    "Access the dashboard at http://localhost:<port>/",
                    "Range: 1024 to 65535",
                    "Default: 8765"
                )
                .defineInRange("debugServerPort", 8765, 1024, 65535);

        DEBUG_UPDATE_INTERVAL_MS = BUILDER
                .comment(
                    "How often to send updates to connected clients (in milliseconds).",
                    "Lower values = more real-time but more CPU usage.",
                    "Range: 100 to 5000 ms",
                    "Default: 1000 (1 second)"
                )
                .defineInRange("debugUpdateIntervalMs", 1000, 100, 5000);

        BUILDER.pop();
        
        SPEC = BUILDER.build();
    }
}
