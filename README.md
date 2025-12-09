
# Irradiated Mod

A Minecraft mod for NeoForge 1.21.1 that adds radiation mechanics, consumable items, and survival elements inspired by the Fallout universe.

## Features

- **Radiation System**: Environmental radiation tracking with visual indicators
- **Consumable Items**: RadX, Radaway, Stimpack, Med-X, Psycho, Cateye, Jet, Buffout, and Nuka Cola variants
- **Geiger Counter**: Equippable radiation detection device
- **Visual Overlays**: HUD elements showing radiation levels and equipped items
- **Curios Integration**: Charm slots for equipping radiation detection equipment

## Installation

1. Install **NeoForge 1.21.1**
2. Install **Curios API** (required dependency)
3. Place the Irradiated mod file in your `mods` folder

## Curios Configuration

This mod requires **2 charm slots** to function properly. The mod includes a default configuration file that should automatically set this up. If you're experiencing issues with only 1 charm slot available:

1. Navigate to your Minecraft instance's `config` folder
2. Edit `curios-common.toml`
3. Set the slots configuration to: `slots = ["id=charm;size=2"]`
4. Restart Minecraft

## Development

If at any point you are missing libraries in your IDE, or you've run into problems you can
run `gradlew --refresh-dependencies` to refresh the local cache. `gradlew clean` to reset everything 
{this does not affect your code} and then start the process again.

Mapping Names:
============
By default, the MDK is configured to use the official mapping names from Mojang for methods and fields 
in the Minecraft codebase. These names are covered by a specific license. All modders should be aware of this
license. For the latest license text, refer to the mapping file itself, or the reference copy here:
https://github.com/NeoForged/NeoForm/blob/main/Mojang.md

Additional Resources: 
==========
Community Documentation: https://docs.neoforged.net/  
NeoForged Discord: https://discord.neoforged.net/
