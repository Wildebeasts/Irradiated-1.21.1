package com.momosoftworks.irradiated.core.init;

import com.momosoftworks.irradiated.Irradiated;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModCreativeTab {
    public static final DeferredRegister<CreativeModeTab> REGISTER = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, Irradiated.MOD_ID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> IRRADIATED_TAB = REGISTER.register("irradiated", () -> CreativeModeTab.builder()
        .title(Component.translatable("itemGroup." + Irradiated.MOD_ID))
        .icon(() -> new ItemStack(ModItems.RADX.value()))
        .displayItems((parameters, output) -> {
            output.accept(ModItems.GEIGER_COUNTER.value());
            output.accept(ModItems.RADX.value());
            output.accept(ModItems.RADAWAY.value());
            output.accept(ModItems.STIMPACK.value());
            output.accept(ModItems.MEDX.value());
            output.accept(ModItems.PSYCHO.value());
            output.accept(ModItems.CATEYE.value());
            output.accept(ModItems.JET.value());
            output.accept(ModItems.BUFFOUT.value());
            output.accept(ModItems.NUKA_COLA.value());
            output.accept(ModItems.NUKA_COLA_QUANTUM.value());
        })
        .build());
}


