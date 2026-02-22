package com.minersmarket.registry;

import com.minersmarket.MinersMarket;
import com.minersmarket.block.GameResetBlock;
import com.minersmarket.block.GameStartBlock;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;

public class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(MinersMarket.MOD_ID, Registries.BLOCK);

    public static final RegistrySupplier<Block> GAME_START_BLOCK = BLOCKS.register("game_start_block",
            () -> new GameStartBlock(BlockBehaviour.Properties.of()
                    .setId(ResourceKey.create(Registries.BLOCK,
                            ResourceLocation.fromNamespaceAndPath(MinersMarket.MOD_ID, "game_start_block")))
                    .strength(5.0F, 6.0F)));

    public static final RegistrySupplier<Block> GAME_RESET_BLOCK = BLOCKS.register("game_reset_block",
            () -> new GameResetBlock(BlockBehaviour.Properties.of()
                    .setId(ResourceKey.create(Registries.BLOCK,
                            ResourceLocation.fromNamespaceAndPath(MinersMarket.MOD_ID, "game_reset_block")))
                    .strength(5.0F, 6.0F)));

    // Block items
    public static final RegistrySupplier<Item> GAME_START_BLOCK_ITEM = ModItems.ITEMS.register("game_start_block",
            () -> new BlockItem(GAME_START_BLOCK.get(), new Item.Properties()
                    .setId(ResourceKey.create(Registries.ITEM,
                            ResourceLocation.fromNamespaceAndPath(MinersMarket.MOD_ID, "game_start_block")))
                    .useBlockDescriptionPrefix()));

    public static final RegistrySupplier<Item> GAME_RESET_BLOCK_ITEM = ModItems.ITEMS.register("game_reset_block",
            () -> new BlockItem(GAME_RESET_BLOCK.get(), new Item.Properties()
                    .setId(ResourceKey.create(Registries.ITEM,
                            ResourceLocation.fromNamespaceAndPath(MinersMarket.MOD_ID, "game_reset_block")))
                    .useBlockDescriptionPrefix()));

    public static void register() {
        BLOCKS.register();
    }
}
