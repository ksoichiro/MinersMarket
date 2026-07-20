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
import com.minersmarket.config.client.ConfigScreen;
import io.netty.buffer.Unpooled;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.ModelLayerRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLevelEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.client.KeyMapping;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

public class FabricPlatform {

    private static final ResourceKey<CreativeModeTab> MINERS_MARKET_TAB_KEY = ResourceKey.create(
            Registries.CREATIVE_MODE_TAB,
            Identifier.fromNamespaceAndPath(MinersMarket.MOD_ID, "minersmarket_tab"));

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
                Identifier.fromNamespaceAndPath(MinersMarket.MOD_ID, "game_start_block"), gameStartBlock);
        ModBlocks.GAME_START_BLOCK = () -> gameStartBlock;

        Block gameResetBlock = ModBlocks.createGameResetBlock();
        Registry.register(BuiltInRegistries.BLOCK,
                Identifier.fromNamespaceAndPath(MinersMarket.MOD_ID, "game_reset_block"), gameResetBlock);
        ModBlocks.GAME_RESET_BLOCK = () -> gameResetBlock;

        // Register items
        Item minersPickaxe = ModItems.createMinersPickaxe();
        Registry.register(BuiltInRegistries.ITEM,
                Identifier.fromNamespaceAndPath(MinersMarket.MOD_ID, "minerspickaxe"), minersPickaxe);
        ModItems.MINERS_PICKAXE = () -> minersPickaxe;

        // Register block items
        Item gameStartBlockItem = ModBlocks.createBlockItem(gameStartBlock, "game_start_block");
        Registry.register(BuiltInRegistries.ITEM,
                Identifier.fromNamespaceAndPath(MinersMarket.MOD_ID, "game_start_block"), gameStartBlockItem);
        ModBlocks.GAME_START_BLOCK_ITEM = () -> gameStartBlockItem;

        Item gameResetBlockItem = ModBlocks.createBlockItem(gameResetBlock, "game_reset_block");
        Registry.register(BuiltInRegistries.ITEM,
                Identifier.fromNamespaceAndPath(MinersMarket.MOD_ID, "game_reset_block"), gameResetBlockItem);
        ModBlocks.GAME_RESET_BLOCK_ITEM = () -> gameResetBlockItem;

        // Register entity type
        EntityType<MerchantEntity> merchantType = ModEntityTypes.createMerchantEntityType();
        Registry.register(BuiltInRegistries.ENTITY_TYPE,
                Identifier.fromNamespaceAndPath(MinersMarket.MOD_ID, "merchant"), merchantType);
        ModEntityTypes.MERCHANT = () -> merchantType;
        FabricDefaultAttributeRegistry.register(merchantType, Mob.createMobAttributes());

        // Register creative tab
        CreativeModeTab tab = ModCreativeTab.createCreativeTab();
        Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB,
                Identifier.fromNamespaceAndPath(MinersMarket.MOD_ID, "minersmarket_tab"), tab);
        ModCreativeTab.MINERS_MARKET_TAB = () -> tab;
    }

    public static void registerNetworking() {
        PayloadTypeRegistry.clientboundPlay().register(GameStateSyncPayload.TYPE, GameStateSyncPayload.STREAM_CODEC);

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
        ServerLevelEvents.LOAD.register((server, level) -> MinersMarket.onServerLevelLoad(level));
        ServerLifecycleEvents.SERVER_STARTED.register(MinersMarket::onServerStarted);
        ServerLifecycleEvents.SERVER_STOPPING.register(MinersMarket::onServerStopping);
        ServerTickEvents.START_SERVER_TICK.register(GameTickHandler::onServerTick);
        ServerPlayerEvents.AFTER_RESPAWN.register(
                (oldPlayer, newPlayer, alive) -> PlayerSpawnHandler.onPlayerRespawn(newPlayer, alive, null));
        ServerPlayConnectionEvents.JOIN.register(
                (handler, sender, server) -> PlayerSpawnHandler.onPlayerJoin(handler.player));
        CommandRegistrationCallback.EVENT.register(MinersMarket::registerCommands);

        // Populate the creative tab here, since CreativeModeTab.Output is inaccessible
        // from the common module (MC 26.x) and the tab is built without contents there.
        // fabric-api 0.145 replaced fabric-item-group-api-v1 with fabric-creative-tab-api-v1.
        // FabricCreativeModeTabOutput implements CreativeModeTab.Output; accept(ItemLike) is the
        // inherited convenience overload.
        CreativeModeTabEvents.modifyOutputEvent(MINERS_MARKET_TAB_KEY).register(output -> {
            output.accept(ModItems.MINERS_PICKAXE.get());
            output.accept(ModBlocks.GAME_START_BLOCK_ITEM.get());
            output.accept(ModBlocks.GAME_RESET_BLOCK_ITEM.get());
        });
    }

    public static void registerClient() {
        ClientGameState.setOnSaleCallback(GameHudOverlay::addFloatingText);
        ClientPlayNetworking.registerGlobalReceiver(GameStateSyncPayload.TYPE, (payload, context) -> {
            context.client().execute(() -> {
                FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.wrappedBuffer(payload.data));
                GameStateSyncPacket.applyOnClient(buf);
            });
        });

        ModelLayerRegistry.registerModelLayer(MerchantModel.LAYER_LOCATION, MerchantModel::createBodyLayer);
        EntityRendererRegistry.register(ModEntityTypes.MERCHANT.get(), MerchantEntityRenderer::new);
        HudElementRegistry.addLast(
                Identifier.fromNamespaceAndPath(MinersMarket.MOD_ID, "game_hud"), GameHudOverlay::render);
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            ClientGameState.reset();
            GameHudOverlay.clearFloatingTexts();
        });

        KeyMapping.Category category = new KeyMapping.Category(
                Identifier.fromNamespaceAndPath(MinersMarket.MOD_ID, "main"));
        KeyMapping openConfigKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.minersmarket.open_config",
                InputConstants.UNKNOWN.getValue(),
                category));
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openConfigKey.consumeClick()) {
                client.setScreenAndShow(new ConfigScreen(client.gui.screen()));
            }
        });
    }
}
