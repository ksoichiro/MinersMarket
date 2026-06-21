package com.minersmarket;

import com.minersmarket.config.ConfigLoader;
import com.minersmarket.config.MinersMarketConfig;
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

import java.nio.file.Path;

public class MinersMarket {
    public static final String MOD_ID = "minersmarket";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    private static Path configDir;

    public static void init(Path configDir) {
        MinersMarket.configDir = configDir;
        ConfigLoader.load(configDir);
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
                        }))
                .then(Commands.literal("market")
                        .then(Commands.literal("generate")
                                .executes(context -> generateMarketFromCommand(context.getSource()))))
                .then(Commands.literal("config")
                        .then(Commands.literal("reload")
                                .executes(context -> {
                                    if (configDir == null) {
                                        context.getSource().sendFailure(Component.literal("Config directory is not initialized"));
                                        return 0;
                                    }
                                    MinersMarketConfig config = ConfigLoader.load(configDir);
                                    boolean generateOnWorldLoad = config.market().generateOnWorldLoad();
                                    context.getSource().sendSuccess(
                                            () -> Component.literal("Reloaded Miner's Market config: market.generate_on_world_load="
                                                    + generateOnWorldLoad),
                                            true
                                    );
                                    return 1;
                                }))));
    }

    private static int generateMarketFromCommand(CommandSourceStack source) {
        if (source.getLevel().dimension() != net.minecraft.world.level.Level.OVERWORLD) {
            source.sendFailure(Component.literal("Market can only be generated in the Overworld"));
            return 0;
        }

        GameStateManager manager = GameStateManager.getInstance();
        if (manager == null) {
            source.sendFailure(Component.literal("Game not initialized"));
            return 0;
        }
        if (manager.isMarketGenerated()) {
            source.sendFailure(Component.literal("Market is already generated"));
            return 0;
        }

        BlockPos center = BlockPos.containing(source.getPosition());
        if (MarketGenerator.generate(source.getLevel(), center)) {
            manager.setMarketGenerated();
            source.sendSuccess(() -> Component.literal("Market generated near " + center), true);
            return 1;
        }

        source.sendFailure(Component.literal("Failed to generate market"));
        return 0;
    }

    private static void generateMarketIfNeeded(ServerLevel level) {
        if (!MinersMarketConfig.get().market().generateOnWorldLoad()) return;

        GameStateManager manager = GameStateManager.getInstance();
        if (manager == null || manager.isMarketGenerated()) return;

        BlockPos spawnPos = level.getSharedSpawnPos();
        if (MarketGenerator.generate(level, spawnPos)) {
            manager.setMarketGenerated();
        }
    }
}
