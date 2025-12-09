package com.momosoftworks.irradiated.common.temp;

import net.neoforged.fml.ModList;

public class ColdSweatCompat {
	static {
		boolean present = ModList.get().isLoaded("coldsweat");
		if (present) {
			// Wire Cold Sweat temp modifier registration here when API is needed.
		}
	}
}
