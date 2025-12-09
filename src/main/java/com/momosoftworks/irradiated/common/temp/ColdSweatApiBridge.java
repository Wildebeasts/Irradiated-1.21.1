package com.momosoftworks.irradiated.common.temp;

import net.minecraft.world.entity.LivingEntity;
import net.neoforged.fml.ModList;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

public class ColdSweatApiBridge {
	private static final String MODID = "cold_sweat";
	private static final MethodHandle TEMPERATURE_ADD;
	private static final Object TRAIT_BODY;
	private static final boolean AVAILABLE;

	static {
		MethodHandle add = null;
		Object body = null;
		boolean available = false;
		if (ModList.get().isLoaded(MODID)) {
			try {
				// Try likely class names
				Class<?> tempClass = tryLoad(
						"com.momosoftworks.coldsweat.api.util.Temperature",
						"dev.momosoftworks.coldsweat.api.util.Temperature",
						"com.momosoftworks.coldsweat.api.temperature.Temperature"
				);
				Class<?> traitClass = tryLoad(
						"com.momosoftworks.coldsweat.api.util.Temperature$Trait",
						"dev.momosoftworks.coldsweat.api.util.Temperature$Trait",
						"com.momosoftworks.coldsweat.api.temperature.Temperature$Trait"
				);
				Object traitBody = null;
				for (Object c : traitClass.getEnumConstants()) {
					if (String.valueOf(c).equalsIgnoreCase("BODY") || String.valueOf(c).equalsIgnoreCase("BODY_TEMPERATURE")) {
						traitBody = c;
						break;
					}
				}
				MethodHandles.Lookup lookup = MethodHandles.publicLookup();
				MethodHandle mh = lookup.findStatic(tempClass, "add",
						MethodType.methodType(void.class, LivingEntity.class, traitClass, double.class));
				add = mh;
				body = traitBody;
				available = (add != null && body != null);
			} catch (Throwable ignored) {
				available = false;
			}
		}
		TEMPERATURE_ADD = add;
		TRAIT_BODY = body;
		AVAILABLE = available;
	}

	private static Class<?> tryLoad(String... names) throws ClassNotFoundException {
		for (String n : names) {
			try { return Class.forName(n); } catch (ClassNotFoundException ignored) {}
		}
		throw new ClassNotFoundException(names[0]);
	}

	public static boolean isAvailable() {
		return AVAILABLE;
	}

	public static void addBodyHeat(LivingEntity entity, double value) {
		if (!AVAILABLE) return;
		try {
			TEMPERATURE_ADD.invokeExact(entity, TRAIT_BODY, value);
		} catch (Throwable ignored) {
		}
	}
}
