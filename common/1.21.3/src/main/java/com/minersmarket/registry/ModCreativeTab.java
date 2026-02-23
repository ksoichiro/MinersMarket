package com.minersmarket.registry;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;

import java.util.function.Supplier;

public class ModCreativeTab {
    public static Supplier<CreativeModeTab> MINERS_MARKET_TAB;

    public static CreativeModeTab createCreativeTab() {
        return CreativeModeTab.builder(CreativeModeTab.Row.TOP, 0)
                .title(Component.translatable("itemGroup.minersmarket"))
                .icon(() -> new ItemStack(ModItems.MINERS_PICKAXE.get()))
                .displayItems((params, output) -> {
                    output.accept(ModItems.MINERS_PICKAXE.get());
                    output.accept(ModBlocks.GAME_START_BLOCK_ITEM.get());
                    output.accept(ModBlocks.GAME_RESET_BLOCK_ITEM.get());
                })
                .build();
    }
}
