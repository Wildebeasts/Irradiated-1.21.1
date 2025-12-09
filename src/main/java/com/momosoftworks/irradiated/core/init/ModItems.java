package com.momosoftworks.irradiated.core.init;

import com.momosoftworks.irradiated.common.item.chems.*;
import com.momosoftworks.irradiated.common.item.GeigerCounterItem;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModItems {
	public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems("irradiated");

	public static final DeferredItem<Item> RADX = ITEMS.register("radx", () -> new RadxItem(new Item.Properties().stacksTo(16), 22));
	public static final DeferredItem<Item> RADAWAY = ITEMS.register("radaway", () -> new RadawayItem(new Item.Properties().stacksTo(16), 22));
	public static final DeferredItem<Item> STIMPACK = ITEMS.register("stimpack", () -> new StimpackItem(new Item.Properties().stacksTo(16), 22, 6.0f));

	public static final DeferredItem<Item> MEDX = ITEMS.register("medx", () -> new MedxItem(new Item.Properties().stacksTo(16), 22));
	public static final DeferredItem<Item> PSYCHO = ITEMS.register("psycho", () -> new PsychoItem(new Item.Properties().stacksTo(16), 22));
	public static final DeferredItem<Item> CATEYE = ITEMS.register("cateye", () -> new CateyeItem(new Item.Properties().stacksTo(16), 22));
	public static final DeferredItem<Item> JET = ITEMS.register("jet", () -> new JetItem(new Item.Properties().stacksTo(16), 22));
	public static final DeferredItem<Item> BUFFOUT = ITEMS.register("buffout", () -> new BuffoutItem(new Item.Properties().stacksTo(16), 22));
	public static final DeferredItem<Item> NUKA_COLA = ITEMS.register("nuka_cola", () -> new NukaColaItem(new Item.Properties().stacksTo(16), 32));
	public static final DeferredItem<Item> NUKA_COLA_QUANTUM = ITEMS.register("nuka_cola_quantum", () -> new NukaColaQuantumItem(new Item.Properties().stacksTo(16), 32));
	
	// Geiger Counter for radiation detection
	public static final DeferredItem<Item> GEIGER_COUNTER = ITEMS.register("geiger_counter", () -> new GeigerCounterItem(new Item.Properties().stacksTo(1)));
}
