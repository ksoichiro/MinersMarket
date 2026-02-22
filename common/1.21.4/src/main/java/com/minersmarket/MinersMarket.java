package com.minersmarket;

import com.minersmarket.entity.MerchantEntityRenderer;
import com.minersmarket.entity.MerchantModel;
import com.minersmarket.event.GameTickHandler;
import com.minersmarket.event.PlayerSpawnHandler;
import com.minersmarket.hud.GameHudOverlay;
import com.minersmarket.network.GameStateSyncPacket;
import com.minersmarket.registry.ModBlocks;
import com.minersmarket.registry.ModCreativeTab;
import com.minersmarket.registry.ModEntityTypes;
import com.minersmarket.registry.ModItems;
import com.minersmarket.state.ClientGameState;
import com.minersmarket.state.GameStateManager;
import com.minersmarket.structure.MarketGenerator;
import dev.architectury.event.events.client.ClientGuiEvent;
import dev.architectury.event.events.client.ClientPlayerEvent;
import dev.architectury.event.events.common.CommandRegistrationEvent;
import dev.architectury.event.events.common.LifecycleEvent;
import dev.architectury.event.events.common.PlayerEvent;
import dev.architectury.event.events.common.TickEvent;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import dev.architectury.registry.client.level.entity.EntityModelLayerRegistry;
import dev.architectury.registry.client.level.entity.EntityRendererRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MinersMarket {
    public static final String MOD_ID = "minersmarket";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static void init() {
        // ModBlocks must be registered before ModItems because ModBlocks
        // adds block items to ModItems.ITEMS during class loading
        ModBlocks.register();
        ModItems.register();
        ModEntityTypes.register();
        ModCreativeTab.register();

        LifecycleEvent.SERVER_LEVEL_LOAD.register(level -> {
            if (level.dimension() == net.minecraft.world.level.Level.OVERWORLD) {
                GameStateManager.init(level);
            }
        });
        LifecycleEvent.SERVER_STARTED.register(server -> {
            ServerLevel overworld = server.getLevel(net.minecraft.world.level.Level.OVERWORLD);
            if (overworld != null) {
                generateMarketIfNeeded(overworld);
            }
        });
        LifecycleEvent.SERVER_STOPPING.register(server -> GameStateManager.clear());
        TickEvent.SERVER_PRE.register(GameTickHandler::onServerTick);
        PlayerEvent.PLAYER_RESPAWN.register(PlayerSpawnHandler::onPlayerRespawn);
        PlayerEvent.PLAYER_JOIN.register(PlayerSpawnHandler::onPlayerJoin);
        CommandRegistrationEvent.EVENT.register((dispatcher, registry, selection) -> {
            dispatcher.register(Commands.literal("minersmarket")
                    .requires(source -> source.hasPermission(2))
                    .then(Commands.literal("priceevent")
                            .executes(context -> {
                                GameStateManager manager = GameStateManager.getInstance();
                                if (manager == null) {
                                    context.getSource().sendFailure(Component.literal("Game not initialized"));
                                    return 0;
                                }
                                manager.startPriceEvent();
                                context.getSource().sendSuccess(() -> Component.literal("Price event triggered"), true);
                                return 1;
                            })));
        });

        LOGGER.info("Miner's Market initialized");
    }

    private static void generateMarketIfNeeded(ServerLevel level) {
        GameStateManager manager = GameStateManager.getInstance();
        if (manager == null || manager.isMarketGenerated()) return;

        BlockPos spawnPos = level.getSharedSpawnPos();
        if (MarketGenerator.generate(level, spawnPos)) {
            manager.setMarketGenerated();
        }
    }

    public static void initClient() {
        GameStateSyncPacket.registerClientReceiver();
        EntityModelLayerRegistry.register(MerchantModel.LAYER_LOCATION, MerchantModel::createBodyLayer);
        EntityRendererRegistry.register(ModEntityTypes.MERCHANT, MerchantEntityRenderer::new);
        ClientGuiEvent.RENDER_HUD.register(GameHudOverlay::render);
        ClientPlayerEvent.CLIENT_PLAYER_QUIT.register(player -> {
            ClientGameState.reset();
            GameHudOverlay.clearFloatingTexts();
        });
    }
}
