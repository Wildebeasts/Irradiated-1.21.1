# Irradiated Mod - Recipe Setup

## Important Note
The JSON recipes in `src/main/resources/data/irradiated/recipes/` are not being loaded by NeoForge due to a data loading issue.

## Working Solution: KubeJS
All recipes are implemented using KubeJS. Users need to have KubeJS installed and create the following file:

**File:** `kubejs/server_scripts/irradiated_recipes.js`

**Content:** See `kubejs_recipes_temp.txt` in the root directory.

## Recipes Implemented
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
