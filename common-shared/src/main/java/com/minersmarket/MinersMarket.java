package com.minersmarket;

import com.minersmarket.entity.MerchantEntityRenderer;
import com.minersmarket.network.GameStateSyncPacket;
import com.minersmarket.registry.ModBlocks;
import com.minersmarket.registry.ModCreativeTab;
import com.minersmarket.registry.ModEntityTypes;
import com.minersmarket.registry.ModItems;
import dev.architectury.registry.client.level.entity.EntityRendererRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MinersMarket {
    public static final String MOD_ID = "minersmarket";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static void init() {
        ModItems.register();
        ModBlocks.register();
        ModEntityTypes.register();
        ModCreativeTab.register();
        LOGGER.info("Miner's Market initialized");
    }

    public static void initClient() {
        GameStateSyncPacket.registerClientReceiver();
        EntityRendererRegistry.register(ModEntityTypes.MERCHANT, MerchantEntityRenderer::new);
    }
}
