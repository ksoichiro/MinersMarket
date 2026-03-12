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
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

public class FabricPlatform {

    public record GameStateSyncPayload(byte[] data) implements CustomPacketPayload {
        public static final Type<GameStateSyncPayload> TYPE = new Type<>(GameStateSyncPacket.ID);
        public static final StreamCodec<RegistryFriendlyByteBuf, GameStateSyncPayload> STREAM_CODEC =
                new StreamCodec<>() {
                    @Override
                    public GameStateSyncPayload decode(RegistryFriendlyByteBuf buf) {
                        byte[] bytes = new byte[buf.readableBytes()];
                        buf.readBytes(bytes);
                        return new GameStateSyncPayload(bytes);
                    }

                    @Override
                    public void encode(RegistryFriendlyByteBuf buf, GameStateSyncPayload payload) {
                        buf.writeBytes(payload.data);
                    }
                };

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public static void registerAll() {
        // Register blocks
        Block gameStartBlock = ModBlocks.createGameStartBlock();
        Registry.register(BuiltInRegistries.BLOCK,
                ResourceLocation.fromNamespaceAndPath(MinersMarket.MOD_ID, "game_start_block"), gameStartBlock);
        ModBlocks.GAME_START_BLOCK = () -> gameStartBlock;

        Block gameResetBlock = ModBlocks.createGameResetBlock();
        Registry.register(BuiltInRegistries.BLOCK,
                ResourceLocation.fromNamespaceAndPath(MinersMarket.MOD_ID, "game_reset_block"), gameResetBlock);
        ModBlocks.GAME_RESET_BLOCK = () -> gameResetBlock;

        // Register items
        Item minersPickaxe = ModItems.createMinersPickaxe();
        Registry.register(BuiltInRegistries.ITEM,
                ResourceLocation.fromNamespaceAndPath(MinersMarket.MOD_ID, "minerspickaxe"), minersPickaxe);
        ModItems.MINERS_PICKAXE = () -> minersPickaxe;

        // Register block items
        Item gameStartBlockItem = ModBlocks.createBlockItem(gameStartBlock, "game_start_block");
        Registry.register(BuiltInRegistries.ITEM,
                ResourceLocation.fromNamespaceAndPath(MinersMarket.MOD_ID, "game_start_block"), gameStartBlockItem);
        ModBlocks.GAME_START_BLOCK_ITEM = () -> gameStartBlockItem;

        Item gameResetBlockItem = ModBlocks.createBlockItem(gameResetBlock, "game_reset_block");
        Registry.register(BuiltInRegistries.ITEM,
                ResourceLocation.fromNamespaceAndPath(MinersMarket.MOD_ID, "game_reset_block"), gameResetBlockItem);
        ModBlocks.GAME_RESET_BLOCK_ITEM = () -> gameResetBlockItem;

        // Register entity type
        EntityType<MerchantEntity> merchantType = ModEntityTypes.createMerchantEntityType();
        Registry.register(BuiltInRegistries.ENTITY_TYPE,
                ResourceLocation.fromNamespaceAndPath(MinersMarket.MOD_ID, "merchant"), merchantType);
        ModEntityTypes.MERCHANT = () -> merchantType;
        FabricDefaultAttributeRegistry.register(merchantType, Mob.createMobAttributes());

        // Register creative tab
        CreativeModeTab tab = ModCreativeTab.createCreativeTab();
        Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB,
                ResourceLocation.fromNamespaceAndPath(MinersMarket.MOD_ID, "minersmarket_tab"), tab);
        ModCreativeTab.MINERS_MARKET_TAB = () -> tab;
    }

    public static void registerNetworking() {
        PayloadTypeRegistry.playS2C().register(GameStateSyncPayload.TYPE, GameStateSyncPayload.STREAM_CODEC);

        GameStateSyncPacket.setPacketSender((player, manager) -> {
            FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
            GameStateSyncPacket.encode(buf, player, manager);
            byte[] data = new byte[buf.readableBytes()];
            buf.readBytes(data);
            buf.release();
            ServerPlayNetworking.send(player, new GameStateSyncPayload(data));
        });
    }

    public static void registerEvents() {
        ServerWorldEvents.LOAD.register((server, level) -> MinersMarket.onServerLevelLoad(level));
        ServerLifecycleEvents.SERVER_STARTED.register(MinersMarket::onServerStarted);
        ServerLifecycleEvents.SERVER_STOPPING.register(MinersMarket::onServerStopping);
        ServerTickEvents.START_SERVER_TICK.register(GameTickHandler::onServerTick);
        ServerPlayerEvents.AFTER_RESPAWN.register(
                (oldPlayer, newPlayer, alive) -> PlayerSpawnHandler.onPlayerRespawn(newPlayer, alive, null));
        ServerPlayConnectionEvents.JOIN.register(
                (handler, sender, server) -> PlayerSpawnHandler.onPlayerJoin(handler.player));
        CommandRegistrationCallback.EVENT.register(MinersMarket::registerCommands);
    }

    public static void registerClient() {
        ClientGameState.setOnSaleCallback(GameHudOverlay::addFloatingText);
        ClientPlayNetworking.registerGlobalReceiver(GameStateSyncPayload.TYPE, (payload, context) -> {
            context.client().execute(() -> {
                FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.wrappedBuffer(payload.data));
                GameStateSyncPacket.applyOnClient(buf);
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
