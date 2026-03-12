package com.minersmarket.fabric;

import com.minersmarket.MinersMarket;
import com.minersmarket.entity.MerchantEntity;
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
import io.netty.buffer.Unpooled;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.EntityModelLayerRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

public class FabricPlatform {

    public static void registerAll() {
        // Register blocks
        Block gameStartBlock = ModBlocks.createGameStartBlock();
        Registry.register(BuiltInRegistries.BLOCK,
                new ResourceLocation(MinersMarket.MOD_ID, "game_start_block"), gameStartBlock);
        ModBlocks.GAME_START_BLOCK = () -> gameStartBlock;

        Block gameResetBlock = ModBlocks.createGameResetBlock();
        Registry.register(BuiltInRegistries.BLOCK,
                new ResourceLocation(MinersMarket.MOD_ID, "game_reset_block"), gameResetBlock);
        ModBlocks.GAME_RESET_BLOCK = () -> gameResetBlock;

        // Register items
        Item minersPickaxe = ModItems.createMinersPickaxe();
        Registry.register(BuiltInRegistries.ITEM,
                new ResourceLocation(MinersMarket.MOD_ID, "minerspickaxe"), minersPickaxe);
        ModItems.MINERS_PICKAXE = () -> minersPickaxe;

        // Register block items
        Item gameStartBlockItem = ModBlocks.createBlockItem(gameStartBlock);
        Registry.register(BuiltInRegistries.ITEM,
                new ResourceLocation(MinersMarket.MOD_ID, "game_start_block"), gameStartBlockItem);
        ModBlocks.GAME_START_BLOCK_ITEM = () -> gameStartBlockItem;

        Item gameResetBlockItem = ModBlocks.createBlockItem(gameResetBlock);
        Registry.register(BuiltInRegistries.ITEM,
                new ResourceLocation(MinersMarket.MOD_ID, "game_reset_block"), gameResetBlockItem);
        ModBlocks.GAME_RESET_BLOCK_ITEM = () -> gameResetBlockItem;

        // Register entity type
        EntityType<MerchantEntity> merchantType = ModEntityTypes.createMerchantEntityType();
        Registry.register(BuiltInRegistries.ENTITY_TYPE,
                new ResourceLocation(MinersMarket.MOD_ID, "merchant"), merchantType);
        ModEntityTypes.MERCHANT = () -> merchantType;
        FabricDefaultAttributeRegistry.register(merchantType, Mob.createMobAttributes());

        // Register creative tab
        CreativeModeTab tab = CreativeModeTab.builder(CreativeModeTab.Row.TOP, 0)
                .title(Component.translatable("itemGroup.minersmarket"))
                .icon(() -> new ItemStack(ModItems.MINERS_PICKAXE.get()))
                .displayItems((params, output) -> {
                    output.accept(ModItems.MINERS_PICKAXE.get());
                    output.accept(ModBlocks.GAME_START_BLOCK_ITEM.get());
                    output.accept(ModBlocks.GAME_RESET_BLOCK_ITEM.get());
                })
                .build();
        Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB,
                new ResourceLocation(MinersMarket.MOD_ID, "minersmarket_tab"), tab);
        ModCreativeTab.MINERS_MARKET_TAB = () -> tab;
    }

    public static void registerNetworking() {
        GameStateSyncPacket.setPacketSender((player, manager) -> {
            FriendlyByteBuf buf = PacketByteBufs.create();
            GameStateSyncPacket.encode(buf, player, manager);
            ServerPlayNetworking.send(player, GameStateSyncPacket.ID, buf);
        });
    }

    public static void registerEvents() {
        ServerWorldEvents.LOAD.register((server, level) -> MinersMarket.onServerLevelLoad(level));
        ServerLifecycleEvents.SERVER_STARTED.register(MinersMarket::onServerStarted);
        ServerLifecycleEvents.SERVER_STOPPING.register(MinersMarket::onServerStopping);
        ServerTickEvents.START_SERVER_TICK.register(GameTickHandler::onServerTick);
        ServerPlayerEvents.AFTER_RESPAWN.register(
                (oldPlayer, newPlayer, alive) -> PlayerSpawnHandler.onPlayerRespawn(newPlayer, alive));
        ServerPlayConnectionEvents.JOIN.register(
                (handler, sender, server) -> PlayerSpawnHandler.onPlayerJoin(handler.player));
        CommandRegistrationCallback.EVENT.register(MinersMarket::registerCommands);
    }

    public static void registerClient() {
        ClientGameState.setOnSaleCallback(GameHudOverlay::addFloatingText);
        ClientPlayNetworking.registerGlobalReceiver(GameStateSyncPacket.ID,
                (client, handler, buf, responseSender) -> {
                    // Read data on network thread
                    byte[] data = new byte[buf.readableBytes()];
                    buf.readBytes(data);
                    client.execute(() -> {
                        FriendlyByteBuf decodeBuf = new FriendlyByteBuf(Unpooled.wrappedBuffer(data));
                        GameStateSyncPacket.applyOnClient(decodeBuf);
                    });
                });

        EntityModelLayerRegistry.registerModelLayer(MerchantModel.LAYER_LOCATION, MerchantModel::createBodyLayer);
        EntityRendererRegistry.register(ModEntityTypes.MERCHANT.get(), MerchantEntityRenderer::new);
        HudRenderCallback.EVENT.register(GameHudOverlay::render);
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            ClientGameState.reset();
            GameHudOverlay.clearFloatingTexts();
        });
    }
}
