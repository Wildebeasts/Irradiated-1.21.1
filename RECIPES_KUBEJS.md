# Irradiated Mod - Recipe Setup

## Important Note
The JSON recipes in `src/main/resources/data/irradiated/recipes/` are not being loaded by NeoForge due to a data loading issue.

## Working Solution: KubeJS
All recipes are implemented using KubeJS. Users need to have KubeJS installed and create the following file:

**File:** `kubejs/server_scripts/irradiated_recipes.js`

**Content:** See `RECIPES_KUBEJS.js` in the root directory.

## Early Game Radiation Removal (NEW!)
Before you can craft RadAway, you can use these vanilla/modded items to remove radiation:

### Vanilla Items
- **Milk Bucket**: Removes **10 radiation** (configurable)
  - Available early game
  - Not renewable without cows

### Farmer's Delight Items
- **Tomato**: Removes **5 radiation** (configurable)
  - Whole tomato from farming
  - Renewable crop
  
- **Tomato Slices**: Removes **2 radiation** (configurable)
  - Crafted by slicing tomatoes
  - Less effective but useful for small radiation
  
- **Tomato Sauce**: Removes **7 radiation** (configurable)
  - Concentrated tomato product
  - More effective than whole tomatoes!

### Some Assembly Required Items
- **Sandwiches with Tomatoes**: Removes **6 radiation** (configurable)
  - Any sandwich containing tomato ingredients
  - Fills hunger AND removes radiation
  - Great for sustained exploration

### Configuration
Edit amounts in: `config/irradiated/irradiated-common.toml`
```toml
[Early Game Radiation Removal]
    milkRadiationRemovalAmount = 10.0
    tomatoRadiationRemovalAmount = 5.0
    tomatoSliceRadiationRemovalAmount = 2.0
    tomatoSauceRadiationRemovalAmount = 7.0
    tomatoSandwichRadiationRemovalAmount = 6.0
```

### Effectiveness Comparison
1. **RadAway** (25) - Best but requires crafting
2. **Milk** (10) - Early game option
3. **Tomato Sauce** (7) - Concentrated & renewable
4. **Tomato Sandwich** (6) - Food + radiation removal
5. **Tomato** (5) - Basic renewable option
6. **Tomato Slices** (2) - Minimal effect

## Water Decontamination
Water provides three types of radiation protection - **all fully configurable!**

### 1. Reduced Buildup
- Radiation builds up **50% slower** when in water/rain
- Configurable: `waterBuildupReduction` (0.0 to 1.0)
- Default: 0.5 (50% reduction)

### 2. Faster Decay
- Radiation decays much faster in water
- Normal decay delay: 180 seconds (3 minutes)
- Water decay delay: **5 seconds** (36x faster!)
- Configurable: `waterDecayDelaySeconds` (0 to 60)

### 3. Active Decontamination
- Water actively removes **0.1 RAD per second**
- Works even while exposed to radiation sources
- Configurable: `waterActiveDecontaminationRate` (0.0 to 10.0)
- Default: 0.1 RAD/sec = 6 RAD per minute

### Configuration
Edit in: `config/irradiated/irradiated-common.toml`
```toml
[Water Decontamination]
    # Enable/disable all water protection
    enableWaterDecontamination = true
    
    # Buildup reduction multiplier (0.5 = 50% less buildup)
    waterBuildupReduction = 0.5
    
    # Decay delay in water (5 seconds vs 180 normally)
    waterDecayDelaySeconds = 5
    
    # Active removal rate (0.1 RAD per second)
    waterActiveDecontaminationRate = 0.1
```

### Gameplay Tips
- **Jump in water** when radiation gets high
- **Build underwater bases** in radioactive biomes
- **Rain provides protection** (counts as being in water)
- **Combine with RadAway** for fastest decontamination

## Crafted Items & Recipes
- Med-X (4x): 3 redstone (top/bottom rows) + water bucket (center)
- Rad-X (4x): 3 glowstone dust (bottom) + water bucket + 2 glowstone dust (top corners)
- Radaway (4x): 3 glowstone dust (top) + 2 redstone + water bucket + 3 redstone (bottom)
- Stimpack (4x): Gold nugget + gunpowder + water bucket pattern
- Psycho (4x): Gunpowder + glowstone dust + water bucket pattern
- Cat Eye (4x): Glowstone dust + gold nugget + water bucket pattern
- Jet (4x): 3 gunpowder (bottom) + water bucket + 2 gunpowder (top corners)
- Buffout (4x): Gunpowder + redstone + water bucket pattern
- Nuka Cola (2x): Sugar + glowstone dust + glass bottle + redstone pattern
- Nuka Cola Quantum (1x): Ender pearl + Nuka Cola + diamond + lapis lazuli
- Geiger Counter (1x): Redstone + iron ingot + clock + glowstone dust pattern
