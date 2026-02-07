package com.minersmarket.registry;

import com.minersmarket.MinersMarket;
import dev.architectury.registry.registries.DeferredRegister;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Block;

public class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(MinersMarket.MOD_ID, Registries.BLOCK);

    public static void register() {
        BLOCKS.register();
    }
}
