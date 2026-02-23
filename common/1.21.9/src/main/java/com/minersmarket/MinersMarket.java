package com.minersmarket;

import com.minersmarket.state.GameStateManager;
import com.minersmarket.structure.MarketGenerator;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MinersMarket {
    public static final String MOD_ID = "minersmarket";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static void init() {
        LOGGER.info("Miner's Market initialized");
    }

    public static void onServerLevelLoad(ServerLevel level) {
        if (level.dimension() == net.minecraft.world.level.Level.OVERWORLD) {
            GameStateManager.init(level);
        }
    }

    public static void onServerStarted(MinecraftServer server) {
        ServerLevel overworld = server.getLevel(net.minecraft.world.level.Level.OVERWORLD);
        if (overworld != null) {
            generateMarketIfNeeded(overworld);
        }
    }

    public static void onServerStopping(MinecraftServer server) {
        GameStateManager.clear();
    }

    public static void registerCommands(CommandDispatcher<CommandSourceStack> dispatcher,
                                        CommandBuildContext registry,
                                        Commands.CommandSelection selection) {
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
    }

    private static void generateMarketIfNeeded(ServerLevel level) {
        GameStateManager manager = GameStateManager.getInstance();
        if (manager == null || manager.isMarketGenerated()) return;

        BlockPos spawnPos = level.getRespawnData().pos();
        if (MarketGenerator.generate(level, spawnPos)) {
            manager.setMarketGenerated();
        }
    }
}
