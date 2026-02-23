package com.minersmarket.registry;

import com.minersmarket.MinersMarket;
import com.minersmarket.block.GameResetBlock;
import com.minersmarket.block.GameStartBlock;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;

import java.util.function.Supplier;

public class ModBlocks {
    public static Supplier<Block> GAME_START_BLOCK;
    public static Supplier<Block> GAME_RESET_BLOCK;
    public static Supplier<Item> GAME_START_BLOCK_ITEM;
    public static Supplier<Item> GAME_RESET_BLOCK_ITEM;

    public static Block createGameStartBlock() {
        return new GameStartBlock(BlockBehaviour.Properties.of()
                .setId(ResourceKey.create(Registries.BLOCK,
                        ResourceLocation.fromNamespaceAndPath(MinersMarket.MOD_ID, "game_start_block")))
                .strength(5.0F, 6.0F));
    }

    public static Block createGameResetBlock() {
        return new GameResetBlock(BlockBehaviour.Properties.of()
                .setId(ResourceKey.create(Registries.BLOCK,
                        ResourceLocation.fromNamespaceAndPath(MinersMarket.MOD_ID, "game_reset_block")))
                .strength(5.0F, 6.0F));
    }

    public static BlockItem createBlockItem(Block block, String name) {
        return new BlockItem(block, new Item.Properties()
                .setId(ResourceKey.create(Registries.ITEM,
                        ResourceLocation.fromNamespaceAndPath(MinersMarket.MOD_ID, name)))
                .useBlockDescriptionPrefix());
    }
}
