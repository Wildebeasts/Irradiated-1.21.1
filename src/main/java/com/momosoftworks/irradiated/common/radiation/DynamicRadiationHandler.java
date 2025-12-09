package com.momosoftworks.irradiated.common.radiation;

import com.momosoftworks.irradiated.api.radiation.RadiationAPI;
import com.momosoftworks.irradiated.core.init.ModEffects;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.nbt.CompoundTag;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Dynamic radiation system similar to Cold Sweat's temperature.
 * Radiation gradually builds up when near sources and slowly decays over time.
 * This replaces the instant radiation system with a more realistic approach.
 */
@EventBusSubscriber
public class DynamicRadiationHandler {
    
    // Dynamic radiation data for each player
    private static final Map<UUID, DynamicRadiationData> PLAYER_RADIATION_DATA = new HashMap<>();
    
    // Track player game modes to detect switches
    private static final Map<UUID, GameType> PLAYER_GAME_MODES = new HashMap<>();
    
    // Radiation source configurations
    private static final Map<String, RadiationSource> BIOME_SOURCES = new HashMap<>();
    private static final Map<String, RadiationSource> DIMENSION_SOURCES = new HashMap<>();
    private static final Map<Block, RadiationSource> BLOCK_SOURCES = new HashMap<>();
    
    // Cached per-block radiation values from config
    private static final Map<String, BlockRadiationData> BLOCK_RADIATION_VALUES = new HashMap<>();
    
    // Cached shielding values from config
    private static final Map<String, Double> SHIELDING_VALUES = new HashMap<>();
    
    // Flag to track if radiation sources have been initialized
    private static boolean sourcesInitialized = false;
    
    /**
     * Ensure radiation sources are initialized (lazy initialization)
     */
    private static void ensureSourcesInitialized() {
        if (!sourcesInitialized) {
            initializeRadiationSources();
            sourcesInitialized = true;
        }
    }
    
    private static void initializeRadiationSources() {
        // These will be calculated dynamically at runtime, not during static initialization
        // Biome radiation sources - using placeholder values
        BIOME_SOURCES.put("minecraft:desert", new RadiationSource(0.001f, 100.0f, 0, 0.1f));
        BIOME_SOURCES.put("minecraft:badlands", new RadiationSource(0.001f, 100.0f, 0, 0.1f));
        BIOME_SOURCES.put("minecraft:mushroom_fields", new RadiationSource(0.001f, 100.0f, 0, 0.08f));
        
        // Dimension radiation sources  
        DIMENSION_SOURCES.put("minecraft:the_nether", new RadiationSource(0.001f, 100.0f, 0, 0.12f));
        
        // Parse per-block radiation values from config
        parseBlockRadiationValues();
        
        // Parse shielding values from config
        parseShieldingValues();
    }
    
    private static void parseBlockRadiationValues() {
        BLOCK_RADIATION_VALUES.clear();
        List<? extends String> blockValues = RadiationConfig.RADIOACTIVE_BLOCKS.get();
        for (String entry : blockValues) {
            String[] parts = entry.split(":");
            if (parts.length >= 4) {
                String blockId = parts[0] + ":" + parts[1];
                try {
                    double chance = Double.parseDouble(parts[2]);
                    int maxLevel = Integer.parseInt(parts[3]);
                    BLOCK_RADIATION_VALUES.put(blockId, new BlockRadiationData(chance, maxLevel));
                } catch (NumberFormatException e) {
                    // Invalid format, skip this entry
                }
            }
        }
    }
    
    private static void parseShieldingValues() {
        SHIELDING_VALUES.clear();
        List<? extends String> shieldingEntries = RadiationConfig.SHIELDING_BLOCKS.get();
        for (String entry : shieldingEntries) {
            String[] parts = entry.split(":");
            if (parts.length >= 3) {
                String blockId = parts[0] + ":" + parts[1];
                try {
                    double shielding = Double.parseDouble(parts[2]);
                    SHIELDING_VALUES.put(blockId, shielding);
                } catch (NumberFormatException e) {
                    // Invalid format, skip this entry
                }
            }
        }
    }
    
    private static class BlockRadiationData {
        final double chancePerSecond;
        final int maxLevel;
        
        BlockRadiationData(double chancePerSecond, int maxLevel) {
            this.chancePerSecond = chancePerSecond;
            this.maxLevel = maxLevel;
        }
    }
    
    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (!(player instanceof ServerPlayer) || !player.isAlive()) {
            return;
        }
        
        // Ensure radiation sources are initialized before first use
        ensureSourcesInitialized();
        
        // Track player game mode for future use
        UUID playerId = player.getUUID();
        GameType currentGameMode = ((ServerPlayer) player).gameMode.getGameModeForPlayer();
        PLAYER_GAME_MODES.put(playerId, currentGameMode);
        
        // Check every tick for smooth radiation changes
        if (!RadiationConfig.ENABLE_ENVIRONMENTAL_RADIATION.get() || !RadiationConfig.ENABLE_DYNAMIC_RADIATION.get()) {
            return;
        }
        
        // Periodic save every 5 minutes (6000 ticks) to prevent data loss
        if (player.tickCount % 6000 == 0) {
            savePlayerRadiationData(player);
        }
        
        DynamicRadiationData data = PLAYER_RADIATION_DATA.computeIfAbsent(playerId, k -> new DynamicRadiationData());
        
        // Calculate radiation exposure from all sources
        float totalRadiationIntensity = 0.0f;
        float maxPossibleExposure = 0.0f;
        
        // Check biome radiation
        if (RadiationConfig.ENABLE_BIOME_RADIATION.get()) {
            float biomeIntensity = getBiomeRadiationIntensity(player);
            float biomeMaxExposure = getBiomeMaxExposure(player);
            totalRadiationIntensity += biomeIntensity;
            maxPossibleExposure = Math.max(maxPossibleExposure, biomeMaxExposure);
        }
        
        // Check dimension radiation
        if (RadiationConfig.ENABLE_DIMENSION_RADIATION.get()) {
            float dimIntensity = getDimensionRadiationIntensity(player);
            float dimMaxExposure = getDimensionMaxExposure(player);
            totalRadiationIntensity += dimIntensity;
            maxPossibleExposure = Math.max(maxPossibleExposure, dimMaxExposure);
        }
        
        // Check block radiation
        if (RadiationConfig.ENABLE_BLOCK_RADIATION.get()) {
            float blockIntensity = getBlockRadiationIntensity(player);
            float blockMaxExposure = getBlockMaxExposure(player);
            totalRadiationIntensity += blockIntensity;
            maxPossibleExposure = Math.max(maxPossibleExposure, blockMaxExposure);
        }
        
        // Apply radiation resistance
        totalRadiationIntensity = applyRadiationResistance(player, totalRadiationIntensity);
        
        // Apply armor protection
        totalRadiationIntensity = applyArmorProtection(player, totalRadiationIntensity);
        
        // Update dynamic radiation
        updateDynamicRadiation(player, data, totalRadiationIntensity, maxPossibleExposure);
        
        // Apply radiation effect to player every 20 ticks (1 second)
        if (player.tickCount % 20 == 0) {
            applyRadiationEffect(player, data);
        }
    }
    
    private static float getBiomeRadiationIntensity(Player player) {
        Biome biome = player.level().getBiome(player.blockPosition()).value();
        ResourceLocation biomeLocation = player.level().registryAccess()
                .registryOrThrow(Registries.BIOME)
                .getKey(biome);
        
        if (biomeLocation == null) {
            return 0.0f;
        }
        
        String biomeName = biomeLocation.toString();
        
        // Use config values to determine radiation intensity
        float configChance = 0.0f;
        if (biomeName.contains("desert")) {
            configChance = RadiationConfig.DESERT_RADIATION_CHANCE.get().floatValue();
        } else if (biomeName.contains("badlands") || biomeName.contains("mesa")) {
            configChance = RadiationConfig.BADLANDS_RADIATION_CHANCE.get().floatValue();
        } else if (biomeName.contains("mushroom")) {
            configChance = RadiationConfig.MUSHROOM_RADIATION_CHANCE.get().floatValue();
        }
        
        if (configChance > 0) {
            // Convert config chance (per second) to per-tick intensity
            return configChance / 20.0f; // 20 ticks per second
        }
        
        return 0.0f;
    }
    
    private static float getBiomeMaxExposure(Player player) {
        Biome biome = player.level().getBiome(player.blockPosition()).value();
        ResourceLocation biomeLocation = player.level().registryAccess()
                .registryOrThrow(Registries.BIOME)
                .getKey(biome);
        
        if (biomeLocation == null) {
            return 0.0f;
        }
        
        String biomeName = biomeLocation.toString();
        
        // Use config values directly (already on 1-100 scale)
        if (biomeName.contains("desert")) {
            return RadiationConfig.DESERT_MAX_LEVEL.get().floatValue();
        } else if (biomeName.contains("badlands") || biomeName.contains("mesa")) {
            return RadiationConfig.BADLANDS_MAX_LEVEL.get().floatValue();
        } else if (biomeName.contains("mushroom")) {
            return RadiationConfig.MUSHROOM_MAX_LEVEL.get().floatValue();
        }
        
        return 0.0f;
    }
    
    private static float getDimensionMaxExposure(Player player) {
        ResourceLocation dimensionLocation = player.level().dimension().location();
        String dimensionName = dimensionLocation.toString();
        
        // Use config values directly (already on 1-100 scale)
        if (dimensionName.equals("minecraft:the_nether")) {
            return RadiationConfig.NETHER_MAX_LEVEL.get().floatValue();
        }
        
        return 0.0f;
    }
    
    private static float getBlockMaxExposure(Player player) {
        BlockPos playerPos = player.blockPosition();
        Level world = player.level();
        float maxExposure = 0.0f;
        
        // Get range from config
        int range = RadiationConfig.BLOCK_RADIATION_RANGE.get();
        
        // Check blocks in range using config value
        for (int x = -range; x <= range; x++) {
            for (int y = -range; y <= range; y++) {
                for (int z = -range; z <= range; z++) {
                    BlockPos checkPos = playerPos.offset(x, y, z);
                    BlockState blockState = world.getBlockState(checkPos);
                    ResourceLocation blockId = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(blockState.getBlock());
                    String blockIdString = blockId.toString();
                    
                    // Check if this block has radiation values configured
                    BlockRadiationData radiationData = BLOCK_RADIATION_VALUES.get(blockIdString);
                    if (radiationData != null) {
                        maxExposure = Math.max(maxExposure, radiationData.maxLevel);
                    }
                }
            }
        }
        
        return maxExposure;
    }
    
    private static float getDimensionRadiationIntensity(Player player) {
        ResourceLocation dimensionLocation = player.level().dimension().location();
        String dimensionName = dimensionLocation.toString();
        
        // Use config values for Nether radiation
        if (dimensionName.equals("minecraft:the_nether")) {
            float netherChance = RadiationConfig.NETHER_RADIATION_CHANCE.get().floatValue();
            // Make Nether radiation much more aggressive - multiply by 5 for faster buildup
            // Convert config chance (per second) to per-tick intensity and boost it significantly
            return (netherChance / 20.0f) * 5.0f; // 5x multiplier for Nether intensity
        }
        
        return 0.0f;
    }
    
    private static float getBlockRadiationIntensity(Player player) {
        BlockPos playerPos = player.blockPosition();
        Level world = player.level();
        float totalIntensity = 0.0f;
        
        // Get range from config
        int range = RadiationConfig.BLOCK_RADIATION_RANGE.get();
        
        // Check if shielding is enabled
        boolean shieldingEnabled = RadiationConfig.ENABLE_RADIATION_SHIELDING.get();
        
        // Check blocks in range using config value
        for (int x = -range; x <= range; x++) {
            for (int y = -range; y <= range; y++) {
                for (int z = -range; z <= range; z++) {
                    BlockPos checkPos = playerPos.offset(x, y, z);
                    BlockState blockState = world.getBlockState(checkPos);
                    ResourceLocation blockId = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(blockState.getBlock());
                    String blockIdString = blockId.toString();
                    
                    // Check if this block has radiation values configured
                    BlockRadiationData radiationData = BLOCK_RADIATION_VALUES.get(blockIdString);
                    if (radiationData != null) {
                        double distance = Math.sqrt(x * x + y * y + z * z);
                        
                        // Get per-block intensity
                        float baseIntensity = (float) radiationData.chancePerSecond / 20.0f;
                        
                        // Intensity decreases with distance
                        float adjustedIntensity = baseIntensity * (1.0f - (float)(distance / (range * 1.2)));
                        adjustedIntensity = Math.max(0, adjustedIntensity);
                        
                        // Apply shielding reduction if enabled
                        if (shieldingEnabled && adjustedIntensity > 0) {
                            float shieldingReduction = calculateShieldingReduction(world, checkPos, playerPos);
                            adjustedIntensity *= (1.0f - shieldingReduction);
                        }
                        
                        totalIntensity += adjustedIntensity;
                    }
                }
            }
        }
        
        return totalIntensity;
    }
    
    public static float calculateShieldingReduction(Level world, BlockPos sourcePos, BlockPos playerPos) {
        double totalShielding = 0.0;
        
        // Use Bresenham-style line to trace blocks between source and player
        double dx = playerPos.getX() - sourcePos.getX();
        double dy = playerPos.getY() - sourcePos.getY();
        double dz = playerPos.getZ() - sourcePos.getZ();
        double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
        
        if (distance < 1.0) {
            return 0.0f; // No blocks between source and player
        }
        
        // Normalize direction
        dx /= distance;
        dy /= distance;
        dz /= distance;
        
        // Step size for ray marching (smaller = more accurate but slower)
        double stepSize = 0.5;
        double defaultShielding = RadiationConfig.DEFAULT_BLOCK_SHIELDING.get();
        
        // Track visited block positions to avoid counting the same block multiple times
        java.util.Set<BlockPos> visitedBlocks = new java.util.HashSet<>();
        
        // March along the ray from source to player
        for (double t = stepSize; t < distance - 0.5; t += stepSize) {
            int checkX = (int) Math.floor(sourcePos.getX() + dx * t + 0.5);
            int checkY = (int) Math.floor(sourcePos.getY() + dy * t + 0.5);
            int checkZ = (int) Math.floor(sourcePos.getZ() + dz * t + 0.5);
            BlockPos checkPos = new BlockPos(checkX, checkY, checkZ);
            
            // Skip source and player positions
            if (checkPos.equals(sourcePos) || checkPos.equals(playerPos)) {
                continue;
            }
            
            // Skip already visited blocks
            if (visitedBlocks.contains(checkPos)) {
                continue;
            }
            visitedBlocks.add(checkPos);
            
            BlockState blockState = world.getBlockState(checkPos);
            
            // Skip air and non-solid blocks
            if (blockState.isAir() || !blockState.isSolidRender(world, checkPos)) {
                continue;
            }
            
            // Get shielding value for this block
            ResourceLocation blockId = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(blockState.getBlock());
            String blockIdString = blockId.toString();
            
            double shieldingValue;
            if (SHIELDING_VALUES.containsKey(blockIdString)) {
                shieldingValue = SHIELDING_VALUES.get(blockIdString);
            } else {
                // Use default shielding for solid blocks not in the list
                shieldingValue = defaultShielding;
            }
            
            totalShielding += shieldingValue;
            
            // Cap at 100% shielding
            if (totalShielding >= 100.0) {
                return 1.0f;
            }
        }
        
        // Convert percentage to reduction factor (0.0 to 1.0)
        return (float) Math.min(1.0, totalShielding / 100.0);
    }
    
    private static float applyRadiationResistance(Player player, float intensity) {
        if (player.hasEffect(ModEffects.radResistanceHolder())) {
            int amplifier = player.getEffect(ModEffects.radResistanceHolder()).getAmplifier();
            float resistance = Math.min(0.95f, 0.25f * (amplifier + 1)); // 25% reduction per level, capped at 95%
            float reducedIntensity = intensity * (1.0f - resistance);
            return reducedIntensity;
        }
        return intensity;
    }
    
    private static float applyArmorProtection(Player player, float intensity) {
        if (!RadiationConfig.ENABLE_ARMOR_PROTECTION.get()) {
            return intensity;
        }
        
        // Calculate total armor points from equipped armor
        float totalArmor = 0.0f;
        for (net.minecraft.world.item.ItemStack armorItem : player.getArmorSlots()) {
            if (!armorItem.isEmpty() && armorItem.getItem() instanceof net.minecraft.world.item.ArmorItem armorItemType) {
                totalArmor += armorItemType.getDefense();
            }
        }
        
        // Calculate protection percentage based on armor points
        float protectionPercent = (float) (totalArmor * RadiationConfig.ARMOR_PROTECTION_PER_POINT.get());
        protectionPercent = Math.min(95.0f, protectionPercent); // Cap at 95% protection
        
        // Apply protection
        float reducedIntensity = intensity * (1.0f - (protectionPercent / 100.0f));
        return reducedIntensity;
    }
    
    private static void updateDynamicRadiation(Player player, DynamicRadiationData data, float exposureIntensity, float maxPossibleExposure) {
        // Check if player is in water for decontamination
        boolean isInWater = (player.isInWater() || player.isInWaterOrBubble() || player.isInWaterOrRain()) 
                && RadiationConfig.ENABLE_WATER_DECONTAMINATION.get();
        
        // If exposed to radiation sources, gradually increase
        if (exposureIntensity > 0) {
            float buildupRate = RadiationConfig.RADIATION_BUILDUP_RATE.get().floatValue();
            
            // Allow buildup up to 100 regardless of individual source limits
            // Individual source max exposure only affects buildup rate, not hard cap
            if (data.currentExposure < 100.0f) {
                // Radiation builds up at 1-5 per SECOND (not per tick)
                // exposureIntensity comes from config as per-second values, converted to per-tick
                // buildupRate is a multiplier (default 1.0)
                // We scale to get 1-5 radiation per second range
                // Since there are 20 ticks per second, divide by 20 to get per-tick increase
                float radiationPerSecond = exposureIntensity * buildupRate * 100.0f; // Scale intensity to meaningful range
                float actualIncrease = radiationPerSecond / 20.0f; // Convert to per-tick (divide by 20 ticks per second)
                
                // Water reduces radiation buildup
                if (isInWater) {
                    float reductionMultiplier = RadiationConfig.WATER_BUILDUP_REDUCTION.get().floatValue();
                    actualIncrease *= reductionMultiplier;
                }
                
                data.currentExposure = Math.min(100.0f, data.currentExposure + actualIncrease);
            }
            data.timeSinceLastExposure = 0;
        } else {
            // If not exposed, start decay timer
            data.timeSinceLastExposure++;
            
            // Get decay delay from config (convert seconds to ticks)
            int decayDelayTicks = RadiationConfig.RADIATION_DECAY_DELAY.get() * 20;
            
            // Only start decaying after the configured delay
            if (data.timeSinceLastExposure >= decayDelayTicks) {
                // Determine decay rate based on environment
                int decayIntervalTicks;
                
                if (isInWater) {
                    decayIntervalTicks = RadiationConfig.WATER_DECAY_DELAY_SECONDS.get() * 20;
                } else {
                    decayIntervalTicks = RadiationConfig.RADIATION_DECAY_DELAY.get() * 20;
                }
                
                // Get decay rate from config
                float decayRate = RadiationConfig.RADIATION_DECAY_RATE.get().floatValue();
                
                // Apply decay at the specified interval
                if ((data.timeSinceLastExposure - decayDelayTicks) % decayIntervalTicks == 0) {
                    if (data.currentExposure > 0) {
                        // Use config decay rate (scaled to be meaningful)
                        float decayAmount = decayRate * 100.0f; // Scale up since config is 0.001-0.1 range
                        float previousExposure = data.currentExposure;
                        data.currentExposure = Math.max(0, data.currentExposure - decayAmount);
                        
                        // If exposure just dropped to zero, immediately clear the radiation effect
                        if (previousExposure > 0 && data.currentExposure == 0) {
                            player.removeEffect(ModEffects.radiationHolder());
                        }
                    }
                }
            }
        }
        
        // Water actively decontaminates (removes radiation slowly even when exposed)
        if (isInWater && data.currentExposure > 0) {
            // Active decontamination rate from config (per second, converted to per tick)
            float waterDecontaminationPerSecond = RadiationConfig.WATER_ACTIVE_DECONTAMINATION_RATE.get().floatValue();
            float waterDecontamination = waterDecontaminationPerSecond / 20.0f; // Convert to per-tick
            float previousExposure = data.currentExposure;
            data.currentExposure = Math.max(0, data.currentExposure - waterDecontamination);
            
            // If exposure just dropped to zero, immediately clear the radiation effect
            if (previousExposure > 0 && data.currentExposure == 0) {
                player.removeEffect(ModEffects.radiationHolder());
            }
        }
        
        // Clamp exposure to reasonable limits
        data.currentExposure = Math.max(0, Math.min(100.0f, data.currentExposure));
        
        // If exposure reached exactly 0, immediately clear the effect (don't wait for next second)
        if (data.currentExposure == 0 && player.hasEffect(ModEffects.radiationHolder())) {
            player.removeEffect(ModEffects.radiationHolder());
        }
    }
    
    private static void applyRadiationEffect(Player player, DynamicRadiationData data) {
        // Check if radiation has been manually overridden
        if (data.manuallyOverridden) {
            data.overrideDuration--;
            if (data.overrideDuration <= 0) {
                // Override has expired, return to dynamic control
                data.manuallyOverridden = false;
            } else {
                // Even when overridden, if exposure is 0, clear the effect
                if (data.currentExposure == 0 && player.hasEffect(ModEffects.radiationHolder())) {
                    player.removeEffect(ModEffects.radiationHolder());
                }
                // Skip normal dynamic radiation application while override is active
                return;
            }
        }
        
        // Dynamic exposure is now on the same 1-100 scale as radiation level
        // Use ceiling for values very close to 100 to ensure death at 100 exposure
        int radiationLevel;
        if (data.currentExposure >= 99.5f) {
            radiationLevel = 100; // Ensure death threshold is reached
        } else if (data.currentExposure < 0.5f) {
            radiationLevel = 0; // Ensure very low exposure counts as 0
        } else {
            radiationLevel = Math.round(data.currentExposure);
        }
        
        // Always remove the old effect first to ensure clean state
        player.removeEffect(ModEffects.radiationHolder());
        
        if (radiationLevel > 0) {
            // Apply new radiation effect
            int clampedLevel = Math.max(1, Math.min(100, radiationLevel));
            player.addEffect(new MobEffectInstance(ModEffects.radiationHolder(), 
                    100, clampedLevel - 1, false, true, true));
        }
        // If radiationLevel is 0, effect stays removed (already removed above)
    }
    
    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        savePlayerRadiationData(event.getEntity());
        UUID playerId = event.getEntity().getUUID();
        removePlayerData(playerId);
        PLAYER_GAME_MODES.remove(playerId); // Clean up game mode tracking
    }
    
    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        loadPlayerRadiationData(event.getEntity());
        // Initialize game mode tracking for new sessions
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            PLAYER_GAME_MODES.put(serverPlayer.getUUID(), serverPlayer.gameMode.getGameModeForPlayer());
        }
    }
    
    @SubscribeEvent
    public static void onPlayerDeath(LivingDeathEvent event) {
        // Clear radiation when player dies to prevent death loops
        if (event.getEntity() instanceof Player player) {
            clearRadiationExposure(player);
        }
    }
    
    /**
     * Save player radiation data to NBT persistent data
     */
    private static void savePlayerRadiationData(Player player) {
        DynamicRadiationData data = PLAYER_RADIATION_DATA.get(player.getUUID());
        if (data != null) {
            CompoundTag radiationTag = data.saveToNBT();
            player.getPersistentData().put("irradiated:dynamicRadiation", radiationTag);
        }
    }
    
    /**
     * Load player radiation data from NBT persistent data
     */
    private static void loadPlayerRadiationData(Player player) {
        CompoundTag persistentData = player.getPersistentData();
        if (persistentData.contains("irradiated:dynamicRadiation")) {
            CompoundTag radiationTag = persistentData.getCompound("irradiated:dynamicRadiation");
            DynamicRadiationData data = DynamicRadiationData.fromNBT(radiationTag);
            PLAYER_RADIATION_DATA.put(player.getUUID(), data);
        }
    }
    
    /**
     * Clean up player data when they leave
     */
    public static void removePlayerData(UUID playerId) {
        PLAYER_RADIATION_DATA.remove(playerId);
    }
    
    /**
     * Get current radiation exposure for a player (for debugging/monitoring)
     */
    public static float getPlayerRadiationExposure(Player player) {
        DynamicRadiationData data = PLAYER_RADIATION_DATA.get(player.getUUID());
        return data != null ? data.currentExposure : 0.0f;
    }
    
    /**
     * Configuration for radiation sources
     */
    private static class RadiationSource {
        final float intensity;    // Radiation intensity per tick
        final float maxExposure;  // Maximum exposure this source can cause
        final int range;          // Range in blocks (0 = no range limit for biomes/dimensions)
        final float decayRate;    // How fast this radiation type decays
        
        RadiationSource(float intensity, float maxExposure, int range, float decayRate) {
            this.intensity = intensity;
            this.maxExposure = maxExposure;
            this.range = range;
            this.decayRate = decayRate;
        }
    }
    
    /**
     * Player radiation data with NBT serialization support
     */
    private static class DynamicRadiationData {
        float currentExposure = 0.0f;     // Current radiation exposure level
        int timeSinceLastExposure = 0;    // Ticks since last radiation source contact
        boolean manuallyOverridden = false; // True if radiation was set manually via API
        int overrideDuration = 0;         // Ticks remaining for manual override
        
        /**
         * Save this data to NBT
         */
        public CompoundTag saveToNBT() {
            CompoundTag tag = new CompoundTag();
            tag.putFloat("currentExposure", currentExposure);
            tag.putInt("timeSinceLastExposure", timeSinceLastExposure);
            tag.putBoolean("manuallyOverridden", manuallyOverridden);
            tag.putInt("overrideDuration", overrideDuration);
            return tag;
        }
        
        /**
         * Load this data from NBT
         */
        public void loadFromNBT(CompoundTag tag) {
            currentExposure = tag.getFloat("currentExposure");
            timeSinceLastExposure = tag.getInt("timeSinceLastExposure");
            manuallyOverridden = tag.getBoolean("manuallyOverridden");
            overrideDuration = tag.getInt("overrideDuration");
        }
        
        /**
         * Create a new instance from NBT data
         */
        public static DynamicRadiationData fromNBT(CompoundTag tag) {
            DynamicRadiationData data = new DynamicRadiationData();
            data.loadFromNBT(tag);
            return data;
        }
    }
    
    // Public API methods for RadiationAPI integration
    
    /**
     * Get the current dynamic radiation exposure for a player
     * @param player The player to check
     * @return Current radiation exposure (0.0 to max possible)
     */
    public static float getCurrentRadiationExposure(Player player) {
        DynamicRadiationData data = PLAYER_RADIATION_DATA.get(player.getUUID());
        return data != null ? data.currentExposure : 0.0f;
    }
    
    /**
     * Set the current dynamic radiation exposure for a player
     * @param player The player to modify
     * @param exposure New radiation exposure level
     */
    public static void setCurrentRadiationExposure(Player player, float exposure) {
        DynamicRadiationData data = PLAYER_RADIATION_DATA.computeIfAbsent(player.getUUID(), k -> new DynamicRadiationData());
        data.currentExposure = Math.max(0.0f, exposure);
        data.timeSinceLastExposure = 0; // Reset decay timer
    }
    
    /**
     * Set radiation exposure with manual override (used by RadiationAPI commands)
     * @param player The player to modify
     * @param exposure New radiation exposure level
     * @param overrideDurationTicks How long to prevent dynamic system from overriding
     */
    public static void setCurrentRadiationExposureWithOverride(Player player, float exposure, int overrideDurationTicks) {
        DynamicRadiationData data = PLAYER_RADIATION_DATA.computeIfAbsent(player.getUUID(), k -> new DynamicRadiationData());
        data.currentExposure = Math.max(0.0f, exposure);
        data.timeSinceLastExposure = 0; // Reset decay timer
        data.manuallyOverridden = true;
        data.overrideDuration = overrideDurationTicks;
    }
    
    /**
     * Reduce dynamic radiation exposure for a player (like RadAway)
     * @param player The player to modify
     * @param reduction Amount to reduce exposure by
     */
    public static void reduceRadiationExposure(Player player, float reduction) {
        DynamicRadiationData data = PLAYER_RADIATION_DATA.get(player.getUUID());
        if (data != null) {
            data.currentExposure = Math.max(0.0f, data.currentExposure - reduction);
            data.timeSinceLastExposure = 0; // Reset decay timer
            // Set manual override to prevent dynamic system from overwriting immediately
            data.manuallyOverridden = true;
            data.overrideDuration = 1200; // 1 minute (1200 ticks) override duration
        }
    }
    
    /**
     * Clear all dynamic radiation exposure for a player
     * @param player The player to clear
     */
    public static void clearRadiationExposure(Player player) {
        DynamicRadiationData data = PLAYER_RADIATION_DATA.get(player.getUUID());
        if (data != null) {
            data.currentExposure = 0.0f;
            data.timeSinceLastExposure = 0;
            data.manuallyOverridden = false; // Clear any manual override
            data.overrideDuration = 0;
        }
    }
}
