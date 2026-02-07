package com.minersmarket;

import com.minersmarket.entity.MerchantEntityRenderer;
import com.minersmarket.event.GameTickHandler;
import com.minersmarket.event.PlayerSpawnHandler;
import com.minersmarket.hud.GameHudOverlay;
import com.minersmarket.network.GameStateSyncPacket;
import com.minersmarket.registry.ModBlocks;
import com.minersmarket.registry.ModCreativeTab;
import com.minersmarket.registry.ModEntityTypes;
import com.minersmarket.registry.ModItems;
import com.minersmarket.state.GameStateManager;
import dev.architectury.event.events.client.ClientGuiEvent;
import dev.architectury.event.events.common.LifecycleEvent;
import dev.architectury.event.events.common.PlayerEvent;
import dev.architectury.event.events.common.TickEvent;
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

        LifecycleEvent.SERVER_LEVEL_LOAD.register(level -> {
            if (level.dimension() == net.minecraft.world.level.Level.OVERWORLD) {
                GameStateManager.init(level);
            }
        });
        LifecycleEvent.SERVER_STOPPING.register(server -> GameStateManager.clear());
        TickEvent.SERVER_PRE.register(GameTickHandler::onServerTick);
        PlayerEvent.PLAYER_RESPAWN.register(PlayerSpawnHandler::onPlayerRespawn);
        PlayerEvent.PLAYER_JOIN.register(PlayerSpawnHandler::onPlayerJoin);

        LOGGER.info("Miner's Market initialized");
    }

    public static void initClient() {
        GameStateSyncPacket.registerClientReceiver();
        EntityRendererRegistry.register(ModEntityTypes.MERCHANT, MerchantEntityRenderer::new);
        ClientGuiEvent.RENDER_HUD.register(GameHudOverlay::render);
    }
}
