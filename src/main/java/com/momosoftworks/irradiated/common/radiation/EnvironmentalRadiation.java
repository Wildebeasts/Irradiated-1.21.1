package com.momosoftworks.irradiated.common.radiation;

import com.momosoftworks.irradiated.api.radiation.RadiationAPI;
import com.momosoftworks.irradiated.core.init.ModEffects;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

import java.util.HashMap;
import java.util.Map;

/**
 * Environmental radiation system for the new Irradiated mod.
 * This handles radiation from biomes, dimensions, and blocks.
 * DISABLED: Replaced by DynamicRadiationHandler
 */
//@EventBusSubscriber
public class EnvironmentalRadiation {
    
    // Configuration for biome radiation levels (can be moved to config later)
    private static final Map<String, RadiationZone> BIOME_RADIATION = new HashMap<>();
    private static final Map<String, RadiationZone> DIMENSION_RADIATION = new HashMap<>();
    private static final Map<Block, RadiationZone> BLOCK_RADIATION = new HashMap<>();
    
    static {
        // Configure radiation biomes (like Fallout's wasteland areas)
        BIOME_RADIATION.put("minecraft:desert", new RadiationZone(1, 0.02f, 16));
        BIOME_RADIATION.put("minecraft:badlands", new RadiationZone(2, 0.05f, 24));
        BIOME_RADIATION.put("minecraft:mushroom_fields", new RadiationZone(3, 0.1f, 32));
        
        // Configure radiation dimensions
        DIMENSION_RADIATION.put("minecraft:the_nether", new RadiationZone(2, 0.03f, 20));
        
        // Configure radiation blocks
        BLOCK_RADIATION.put(Blocks.DEEPSLATE_COPPER_ORE, new RadiationZone(4, 0.15f, 8)); // Using copper ore as uranium
        BLOCK_RADIATION.put(Blocks.COPPER_ORE, new RadiationZone(3, 0.12f, 6)); // Regular uranium
        BLOCK_RADIATION.put(Blocks.REDSTONE_ORE, new RadiationZone(1, 0.01f, 4));
        BLOCK_RADIATION.put(Blocks.DEEPSLATE_REDSTONE_ORE, new RadiationZone(1, 0.015f, 5));
        BLOCK_RADIATION.put(Blocks.NETHERITE_BLOCK, new RadiationZone(3, 0.08f, 12));
    }
    
    // DISABLED: Replaced by DynamicRadiationHandler
    //@SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide() || !player.isAlive()) {
            return;
        }
        
        // Check every 20 ticks (once per second)
        if (player.tickCount % 20 != 0) {
            return;
        }
        
        if (!RadiationConfig.ENABLE_ENVIRONMENTAL_RADIATION.get()) {
            return;
        }
        
        int radiationLevel = RadiationAPI.getRadiationLevel(player);
        if (radiationLevel >= 100) {
            return; // Already at max radiation
        }
        
        // Check biome radiation
        if (RadiationConfig.ENABLE_BIOME_RADIATION.get()) {
            checkBiomeRadiation(player);
        }
        
        // Check dimension radiation
        if (RadiationConfig.ENABLE_DIMENSION_RADIATION.get()) {
            checkDimensionRadiation(player);
        }
        
        // Check block radiation
        if (RadiationConfig.ENABLE_BLOCK_RADIATION.get()) {
            checkBlockRadiation(player);
        }
    }    /**
     * Configuration class for radiation zones
     */
    public static class RadiationZone {
        public final int maxLevel;       // Maximum radiation level this source can cause
        public final float radiationChance; // Chance per second to add radiation (0.0-1.0)
        public final double range;       // Range in blocks (for block sources)
        
        public RadiationZone(int maxLevel, float radiationChance, double range) {
            this.maxLevel = maxLevel;
            this.radiationChance = radiationChance;
            this.range = range;
        }
    }
    
    /**
     * Add a custom biome radiation source
     */
    public static void addBiomeRadiation(String biomeName, int maxLevel, float chance, double range) {
        BIOME_RADIATION.put(biomeName, new RadiationZone(maxLevel, chance, range));
    }
    
    /**
     * Add a custom dimension radiation source
     */
    public static void addDimensionRadiation(String dimensionName, int maxLevel, float chance, double range) {
        DIMENSION_RADIATION.put(dimensionName, new RadiationZone(maxLevel, chance, range));
    }
    
    /**
     * Add a custom block radiation source
     */
    public static void addBlockRadiation(Block block, int maxLevel, float chance, double range) {
        BLOCK_RADIATION.put(block, new RadiationZone(maxLevel, chance, range));
    }
    
    /**
     * Remove a biome radiation source
     */
    public static void removeBiomeRadiation(String biomeName) {
        BIOME_RADIATION.remove(biomeName);
    }
    
    /**
     * Get all configured biome radiation sources
     */
    public static Map<String, RadiationZone> getBiomeRadiation() {
        return new HashMap<>(BIOME_RADIATION);
    }
    
    /**
     * Get all configured block radiation sources
     */
    public static Map<Block, RadiationZone> getBlockRadiation() {
        return new HashMap<>(BLOCK_RADIATION);
    }
    
    /**
     * Check for biome-based radiation
     */
    private static void checkBiomeRadiation(Player player) {
        Biome biome = player.level().getBiome(player.blockPosition()).value();
        ResourceLocation biomeLocation = player.level().registryAccess()
                .registryOrThrow(Registries.BIOME)
                .getKey(biome);
        
        if (biomeLocation == null) {
            return;
        }
        
        String biomeName = biomeLocation.getPath();
        double chance = 0.0;
        int maxLevel = 0;
        
        // Check for desert biomes
        if (biomeName.contains("desert")) {
            chance = RadiationConfig.DESERT_RADIATION_CHANCE.get();
            maxLevel = RadiationConfig.DESERT_MAX_LEVEL.get();
        }
        // Check for badlands biomes
        else if (biomeName.contains("badlands") || biomeName.contains("mesa")) {
            chance = RadiationConfig.BADLANDS_RADIATION_CHANCE.get();
            maxLevel = RadiationConfig.BADLANDS_MAX_LEVEL.get();
        }
        // Check for mushroom fields
        else if (biomeName.contains("mushroom")) {
            chance = RadiationConfig.MUSHROOM_RADIATION_CHANCE.get();
            maxLevel = RadiationConfig.MUSHROOM_MAX_LEVEL.get();
        }
        
        // Apply radiation based on chance and current level
        if (chance > 0 && maxLevel > 0 && player.getRandom().nextDouble() < chance) {
            int currentLevel = RadiationAPI.getRadiationLevel(player);
            if (currentLevel < maxLevel) {
                // Check if player has radiation resistance
                double resistanceReduction = getRadiationResistance(player);
                if (player.getRandom().nextDouble() >= resistanceReduction) {
                    RadiationAPI.addRadiation(player, 1, 1200); // 1 minute duration
                }
            }
        }
    }
    
    /**
     * Check for dimension-based radiation
     */
    private static void checkDimensionRadiation(Player player) {
        ResourceLocation dimensionLocation = player.level().dimension().location();
        String dimensionName = dimensionLocation.getPath();
        
        double chance = 0.0;
        int maxLevel = 0;
        
        // Check for Nether
        if (dimensionName.equals("the_nether")) {
            chance = RadiationConfig.NETHER_RADIATION_CHANCE.get();
            maxLevel = RadiationConfig.NETHER_MAX_LEVEL.get();
        }
        
        // Apply radiation based on chance and current level
        if (chance > 0 && maxLevel > 0 && player.getRandom().nextDouble() < chance) {
            int currentLevel = RadiationAPI.getRadiationLevel(player);
            if (currentLevel < maxLevel) {
                // Check if player has radiation resistance
                double resistanceReduction = getRadiationResistance(player);
                if (player.getRandom().nextDouble() >= resistanceReduction) {
                    RadiationAPI.addRadiation(player, 1, 1200); // 1 minute duration
                }
            }
        }
    }
    
    /**
     * Check for block-based radiation
     */
    private static void checkBlockRadiation(Player player) {
        BlockPos playerPos = player.blockPosition();
        Level world = player.level();
        int range = RadiationConfig.BLOCK_RADIATION_RANGE.get();
        
        // Check blocks in range
        for (int x = -range; x <= range; x++) {
            for (int y = -range; y <= range; y++) {
                for (int z = -range; z <= range; z++) {
                    BlockPos checkPos = playerPos.offset(x, y, z);
                    BlockState blockState = world.getBlockState(checkPos);
                    Block block = blockState.getBlock();
                    
                    double distance = Math.sqrt(x * x + y * y + z * z);
                    
                    // Check uranium ore (LEGACY - hardcoded values since this class is disabled)
                    if (block == Blocks.DEEPSLATE_COPPER_ORE) { // Using copper ore as uranium ore
                        double chance = 0.15; // Legacy hardcoded value
                        int maxLevel = 20; // Legacy hardcoded value
                        
                        // Reduce chance based on distance
                        chance = chance * (1.0 - (distance / (range * 1.5)));
                        
                        if (chance > 0 && player.getRandom().nextDouble() < chance) {
                            int currentLevel = RadiationAPI.getRadiationLevel(player);
                            if (currentLevel < maxLevel) {
                                // Check if player has radiation resistance
                                double resistanceReduction = getRadiationResistance(player);
                                if (player.getRandom().nextDouble() >= resistanceReduction) {
                                    RadiationAPI.addRadiation(player, 1, 1200); // 1 minute duration
                                }
                            }
                        }
                    }
                    // Check redstone ore
                    else if (block == Blocks.REDSTONE_ORE || block == Blocks.DEEPSLATE_REDSTONE_ORE) {
                        double chance = 0.01; // 1% chance per second
                        int maxLevel = 1;
                        
                        // Reduce chance based on distance
                        chance = chance * (1.0 - (distance / (range * 1.5)));
                        
                        if (chance > 0 && player.getRandom().nextDouble() < chance) {
                            int currentLevel = RadiationAPI.getRadiationLevel(player);
                            if (currentLevel < maxLevel) {
                                // Check if player has radiation resistance
                                double resistanceReduction = getRadiationResistance(player);
                                if (player.getRandom().nextDouble() >= resistanceReduction) {
                                    RadiationAPI.addRadiation(player, 1, 1200); // 1 minute duration
                                }
                            }
                        }
                    }
                    // Check netherite block
                    else if (block == Blocks.NETHERITE_BLOCK) {
                        double chance = 0.08; // 8% chance per second
                        int maxLevel = 3;
                        
                        // Reduce chance based on distance
                        chance = chance * (1.0 - (distance / (range * 1.5)));
                        
                        if (chance > 0 && player.getRandom().nextDouble() < chance) {
                            int currentLevel = RadiationAPI.getRadiationLevel(player);
                            if (currentLevel < maxLevel) {
                                // Check if player has radiation resistance
                                double resistanceReduction = getRadiationResistance(player);
                                if (player.getRandom().nextDouble() >= resistanceReduction) {
                                    RadiationAPI.addRadiation(player, 1, 1200); // 1 minute duration
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    
    /**
     * Calculate radiation resistance reduction
     */
    private static double getRadiationResistance(Player player) {
        if (player.hasEffect(ModEffects.radResistanceHolder())) {
            int amplifier = player.getEffect(ModEffects.radResistanceHolder()).getAmplifier();
            return 0.25 * (amplifier + 1); // 25% reduction per level
        }
        return 0.0;
    }
}
