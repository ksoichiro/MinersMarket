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
            () -> CreativeTabRegistry.create(builder ->
                    builder.title(Component.translatable("itemGroup.minersmarket"))
                            .icon(() -> new ItemStack(ModItems.MINERS_PICKAXE.get()))
                            .displayItems((params, output) -> {
                                output.accept(ModItems.MINERS_PICKAXE.get());
                                output.accept(ModBlocks.GAME_START_BLOCK_ITEM.get());
                                output.accept(ModBlocks.GAME_RESET_BLOCK_ITEM.get());
                            })
            )
    );

    public static void register() {
        TABS.register();
    }
}
