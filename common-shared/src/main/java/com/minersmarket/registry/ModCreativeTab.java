package com.minersmarket.registry;

import com.minersmarket.MinersMarket;
import dev.architectury.registry.CreativeTabRegistry;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;

public class ModCreativeTab {
    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(MinersMarket.MOD_ID, Registries.CREATIVE_MODE_TAB);

    public static final RegistrySupplier<CreativeModeTab> MINERS_MARKET_TAB = TABS.register(
            "minersmarket_tab",
            () -> CreativeTabRegistry.create(
                    Component.translatable("itemGroup.minersmarket"),
                    () -> new ItemStack(ModItems.MINERS_PICKAXE.get())
            )
    );

    public static void register() {
        TABS.register();
    }
}
