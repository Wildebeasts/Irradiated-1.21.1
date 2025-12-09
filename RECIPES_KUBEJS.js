// Add this to: kubejs/server_scripts/irradiated_recipes.js
// (Create the file in your Minecraft instance: .minecraft/kubejs/server_scripts/irradiated_recipes.js)

ServerEvents.recipes(event => {
    // Med-X Recipe
    event.shaped('irradiated:medx', [
        '///',
        ' b ',
        '///'
    ], {
        '/': 'minecraft:redstone',
        'b': 'minecraft:water_bucket'
    })
    
    // Rad-X Recipe
    event.shaped('4x irradiated:radx', [
        '   ',
        '#b#',
        '###'
    ], {
        '#': 'minecraft:glowstone_dust',
        'b': 'minecraft:water_bucket'
    })
    
    // Radaway Recipe
    event.shaped('4x irradiated:radaway', [
        '###',
        '/b/',
        '///'
    ], {
        '#': 'minecraft:glowstone_dust',
        '/': 'minecraft:redstone',
        'b': 'minecraft:water_bucket'
    })
    
    // Stimpack Recipe
    event.shaped('4x irradiated:stimpack', [
        '#.#',
        '.b.',
        '###'
    ], {
        '#': 'minecraft:gold_nugget',
        '.': 'minecraft:gunpowder',
        'b': 'minecraft:water_bucket'
    })
    
    // Psycho Recipe
    event.shaped('4x irradiated:psycho', [
        '#.#',
        '.b.',
        '###'
    ], {
        '#': 'minecraft:gunpowder',
        '.': 'minecraft:glowstone_dust',
        'b': 'minecraft:water_bucket'
    })
    
    // Cat Eye Recipe  
    event.shaped('4x irradiated:cateye', [
        '#.#',
        '.b.',
        '###'
    ], {
        '#': 'minecraft:glowstone_dust',
        '.': 'minecraft:gold_nugget',
        'b': 'minecraft:water_bucket'
    })
    
    // Jet Recipe
    event.shaped('4x irradiated:jet', [
        '   ',
        '#b#',
        '###'
    ], {
        '#': 'minecraft:gunpowder',
        'b': 'minecraft:water_bucket'
    })
    
    // Buffout Recipe
    event.shaped('4x irradiated:buffout', [
        '#.#',
        '.b.',
        '###'
    ], {
        '#': 'minecraft:gunpowder',
        '.': 'minecraft:redstone',
        'b': 'minecraft:water_bucket'
    })
    
    // Nuka Cola Recipe
    event.shaped('2x irradiated:nuka_cola', [
        'SGS',
        '#B#',
        '###'
    ], {
        'S': 'minecraft:sugar',
        'G': 'minecraft:glowstone_dust',
        'B': 'minecraft:glass_bottle',
        '#': 'minecraft:redstone'
    })
    
    // Nuka Cola Quantum Recipe
    event.shaped('irradiated:nuka_cola_quantum', [
        'ENE',
        'DND',
        'LLL'
    ], {
        'E': 'minecraft:ender_pearl',
        'N': 'irradiated:nuka_cola',
        'D': 'minecraft:diamond',
        'L': 'minecraft:lapis_lazuli'
    })
    
    // Geiger Counter Recipe
    event.shaped('irradiated:geiger_counter', [
        'RIR',
        'ICI',
        'RGR'
    ], {
        'R': 'minecraft:redstone',
        'I': 'minecraft:iron_ingot',
        'C': 'minecraft:clock',
        'G': 'minecraft:glowstone_dust'
    })
})
