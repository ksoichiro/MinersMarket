package com.minersmarket.registry;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;

import java.util.function.Supplier;

public class ModCreativeTab {
    public static Supplier<CreativeModeTab> MINERS_MARKET_TAB;

    public static CreativeModeTab createCreativeTab() {
        // MC 26.1: CreativeModeTab.Output is a protected nested interface, so its type is
        // not accessible from this package and a displayItems() lambda cannot compile here
        // (in the vanilla/NeoForm common module). Tab contents are populated platform-side
        // via NeoForge's BuildCreativeModeTabContentsEvent, where Output is exposed.
        return CreativeModeTab.builder(CreativeModeTab.Row.TOP, 0)
                .title(Component.translatable("itemGroup.minersmarket"))
                .icon(() -> new ItemStack(ModItems.MINERS_PICKAXE.get()))
                .build();
    }
}
