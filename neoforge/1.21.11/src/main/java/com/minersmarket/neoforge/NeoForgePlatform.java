package com.minersmarket.neoforge;

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
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class NeoForgePlatform {

    private static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(Registries.BLOCK, MinersMarket.MOD_ID);
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Registries.ITEM, MinersMarket.MOD_ID);
    private static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(Registries.ENTITY_TYPE, MinersMarket.MOD_ID);
    private static final DeferredRegister<CreativeModeTab> CREATIVE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MinersMarket.MOD_ID);

    private static final DeferredHolder<Block, Block> GAME_START_BLOCK =
            BLOCKS.register("game_start_block", ModBlocks::createGameStartBlock);
    private static final DeferredHolder<Block, Block> GAME_RESET_BLOCK =
            BLOCKS.register("game_reset_block", ModBlocks::createGameResetBlock);

    private static final DeferredHolder<Item, Item> MINERS_PICKAXE =
            ITEMS.register("minerspickaxe", ModItems::createMinersPickaxe);
    private static final DeferredHolder<Item, Item> GAME_START_BLOCK_ITEM =
            ITEMS.register("game_start_block", () -> ModBlocks.createBlockItem(GAME_START_BLOCK.get(), "game_start_block"));
    private static final DeferredHolder<Item, Item> GAME_RESET_BLOCK_ITEM =
            ITEMS.register("game_reset_block", () -> ModBlocks.createBlockItem(GAME_RESET_BLOCK.get(), "game_reset_block"));

    private static final DeferredHolder<EntityType<?>, EntityType<MerchantEntity>> MERCHANT =
            ENTITY_TYPES.register("merchant", ModEntityTypes::createMerchantEntityType);

    private static final DeferredHolder<CreativeModeTab, CreativeModeTab> MINERS_MARKET_TAB =
            CREATIVE_TABS.register("minersmarket_tab", ModCreativeTab::createCreativeTab);

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

    public static void registerAll(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        ENTITY_TYPES.register(modBus);
        CREATIVE_TABS.register(modBus);

        ModBlocks.GAME_START_BLOCK = GAME_START_BLOCK;
        ModBlocks.GAME_RESET_BLOCK = GAME_RESET_BLOCK;
        ModBlocks.GAME_START_BLOCK_ITEM = GAME_START_BLOCK_ITEM;
        ModBlocks.GAME_RESET_BLOCK_ITEM = GAME_RESET_BLOCK_ITEM;
        ModItems.MINERS_PICKAXE = MINERS_PICKAXE;
        ModEntityTypes.MERCHANT = MERCHANT;
        ModCreativeTab.MINERS_MARKET_TAB = MINERS_MARKET_TAB;

        modBus.addListener((EntityAttributeCreationEvent event) ->
                event.put(MERCHANT.get(), Mob.createMobAttributes().build()));

        modBus.addListener((RegisterPayloadHandlersEvent event) -> {
            var registrar = event.registrar(MinersMarket.MOD_ID);
            registrar.playToClient(GameStateSyncPayload.TYPE, GameStateSyncPayload.STREAM_CODEC,
                    (payload, context) -> context.enqueueWork(() -> {
                        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.wrappedBuffer(payload.data));
                        GameStateSyncPacket.applyOnClient(buf);
                    }));
        });

        GameStateSyncPacket.setPacketSender((player, manager) -> {
            FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
            GameStateSyncPacket.encode(buf, player, manager);
            byte[] data = new byte[buf.readableBytes()];
            buf.readBytes(data);
            buf.release();
            PacketDistributor.sendToPlayer(player, new GameStateSyncPayload(data));
        });
    }

    public static void registerEvents() {
        NeoForge.EVENT_BUS.addListener((LevelEvent.Load event) -> {
            if (event.getLevel() instanceof ServerLevel level) {
                MinersMarket.onServerLevelLoad(level);
            }
        });
        NeoForge.EVENT_BUS.addListener((ServerStartedEvent event) ->
                MinersMarket.onServerStarted(event.getServer()));
        NeoForge.EVENT_BUS.addListener((ServerStoppingEvent event) ->
                MinersMarket.onServerStopping(event.getServer()));
        NeoForge.EVENT_BUS.addListener((ServerTickEvent.Pre event) ->
                GameTickHandler.onServerTick(event.getServer()));
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerRespawnEvent event) -> {
            if (event.getEntity() instanceof ServerPlayer player) {
                PlayerSpawnHandler.onPlayerRespawn(player, event.isEndConquered(), null);
            }
        });
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedInEvent event) -> {
            if (event.getEntity() instanceof ServerPlayer player) {
                PlayerSpawnHandler.onPlayerJoin(player);
            }
        });
        NeoForge.EVENT_BUS.addListener((RegisterCommandsEvent event) ->
                MinersMarket.registerCommands(event.getDispatcher(), event.getBuildContext(), event.getCommandSelection()));
    }

    public static void registerClient(IEventBus modBus) {
        ClientGameState.setOnSaleCallback(GameHudOverlay::addFloatingText);
        modBus.addListener((EntityRenderersEvent.RegisterRenderers event) ->
                event.registerEntityRenderer(MERCHANT.get(), MerchantEntityRenderer::new));
        modBus.addListener((EntityRenderersEvent.RegisterLayerDefinitions event) ->
                event.registerLayerDefinition(MerchantModel.LAYER_LOCATION, MerchantModel::createBodyLayer));
        modBus.addListener((RegisterGuiLayersEvent event) ->
                event.registerAboveAll(
                        Identifier.fromNamespaceAndPath(MinersMarket.MOD_ID, "game_hud"),
                        GameHudOverlay::render));
        NeoForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingOut event) -> {
            ClientGameState.reset();
            GameHudOverlay.clearFloatingTexts();
        });
    }
}
