package com.minersmarket.forge;

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
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.util.Optional;
import java.util.function.Supplier;

public class ForgePlatform {

    private static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, MinersMarket.MOD_ID);
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MinersMarket.MOD_ID);
    private static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, MinersMarket.MOD_ID);
    private static final DeferredRegister<CreativeModeTab> CREATIVE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MinersMarket.MOD_ID);

    private static final RegistryObject<Block> GAME_START_BLOCK =
            BLOCKS.register("game_start_block", ModBlocks::createGameStartBlock);
    private static final RegistryObject<Block> GAME_RESET_BLOCK =
            BLOCKS.register("game_reset_block", ModBlocks::createGameResetBlock);

    private static final RegistryObject<Item> MINERS_PICKAXE =
            ITEMS.register("minerspickaxe", ModItems::createMinersPickaxe);
    private static final RegistryObject<Item> GAME_START_BLOCK_ITEM =
            ITEMS.register("game_start_block", () -> ModBlocks.createBlockItem(GAME_START_BLOCK.get()));
    private static final RegistryObject<Item> GAME_RESET_BLOCK_ITEM =
            ITEMS.register("game_reset_block", () -> ModBlocks.createBlockItem(GAME_RESET_BLOCK.get()));

    private static final RegistryObject<EntityType<MerchantEntity>> MERCHANT =
            ENTITY_TYPES.register("merchant", ModEntityTypes::createMerchantEntityType);

    private static final RegistryObject<CreativeModeTab> MINERS_MARKET_TAB =
            CREATIVE_TABS.register("minersmarket_tab", () -> CreativeModeTab.builder(CreativeModeTab.Row.TOP, 0)
                    .title(Component.translatable("itemGroup.minersmarket"))
                    .icon(() -> new ItemStack(ModItems.MINERS_PICKAXE.get()))
                    .displayItems((params, output) -> {
                        output.accept(ModItems.MINERS_PICKAXE.get());
                        output.accept(ModBlocks.GAME_START_BLOCK_ITEM.get());
                        output.accept(ModBlocks.GAME_RESET_BLOCK_ITEM.get());
                    })
                    .build());

    private static SimpleChannel CHANNEL;

    public static class GameStateSyncMsg {
        private final byte[] data;

        public GameStateSyncMsg(byte[] data) {
            this.data = data;
        }

        public static void encode(GameStateSyncMsg msg, FriendlyByteBuf buf) {
            buf.writeBytes(msg.data);
        }

        public static GameStateSyncMsg decode(FriendlyByteBuf buf) {
            byte[] bytes = new byte[buf.readableBytes()];
            buf.readBytes(bytes);
            return new GameStateSyncMsg(bytes);
        }

        public static void handle(GameStateSyncMsg msg, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> {
                FriendlyByteBuf decodeBuf = new FriendlyByteBuf(Unpooled.wrappedBuffer(msg.data));
                GameStateSyncPacket.applyOnClient(decodeBuf);
            });
            ctx.get().setPacketHandled(true);
        }
    }

    public static void registerAll(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        ENTITY_TYPES.register(modBus);
        CREATIVE_TABS.register(modBus);

        // Set suppliers for common code access
        ModBlocks.GAME_START_BLOCK = GAME_START_BLOCK;
        ModBlocks.GAME_RESET_BLOCK = GAME_RESET_BLOCK;
        ModBlocks.GAME_START_BLOCK_ITEM = GAME_START_BLOCK_ITEM;
        ModBlocks.GAME_RESET_BLOCK_ITEM = GAME_RESET_BLOCK_ITEM;
        ModItems.MINERS_PICKAXE = MINERS_PICKAXE;
        ModEntityTypes.MERCHANT = MERCHANT;
        ModCreativeTab.MINERS_MARKET_TAB = MINERS_MARKET_TAB;

        // Entity attributes
        modBus.addListener((EntityAttributeCreationEvent event) ->
                event.put(MERCHANT.get(), Mob.createMobAttributes().build()));

        // Networking
        CHANNEL = NetworkRegistry.newSimpleChannel(
                GameStateSyncPacket.ID,
                () -> "1",
                s -> true,
                s -> true);
        CHANNEL.registerMessage(0, GameStateSyncMsg.class,
                GameStateSyncMsg::encode, GameStateSyncMsg::decode, GameStateSyncMsg::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT));

        GameStateSyncPacket.setPacketSender((player, manager) -> {
            FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
            GameStateSyncPacket.encode(buf, player, manager);
            byte[] data = new byte[buf.readableBytes()];
            buf.readBytes(data);
            buf.release();
            CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new GameStateSyncMsg(data));
        });
    }

    public static void registerEvents() {
        MinecraftForge.EVENT_BUS.addListener((LevelEvent.Load event) -> {
            if (event.getLevel() instanceof ServerLevel level) {
                MinersMarket.onServerLevelLoad(level);
            }
        });
        MinecraftForge.EVENT_BUS.addListener((ServerStartedEvent event) ->
                MinersMarket.onServerStarted(event.getServer()));
        MinecraftForge.EVENT_BUS.addListener((ServerStoppingEvent event) ->
                MinersMarket.onServerStopping(event.getServer()));
        MinecraftForge.EVENT_BUS.addListener((TickEvent.ServerTickEvent event) -> {
            if (event.phase == TickEvent.Phase.START) {
                net.minecraft.server.MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
                if (server != null) {
                    GameTickHandler.onServerTick(server);
                }
            }
        });
        MinecraftForge.EVENT_BUS.addListener((PlayerEvent.PlayerRespawnEvent event) -> {
            if (event.getEntity() instanceof ServerPlayer player) {
                PlayerSpawnHandler.onPlayerRespawn(player, event.isEndConquered());
            }
        });
        MinecraftForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedInEvent event) -> {
            if (event.getEntity() instanceof ServerPlayer player) {
                PlayerSpawnHandler.onPlayerJoin(player);
            }
        });
        MinecraftForge.EVENT_BUS.addListener((RegisterCommandsEvent event) ->
                MinersMarket.registerCommands(event.getDispatcher(), event.getBuildContext(), event.getCommandSelection()));
    }

    public static void registerClient(IEventBus modBus) {
        modBus.addListener((EntityRenderersEvent.RegisterRenderers event) ->
                event.registerEntityRenderer(MERCHANT.get(), MerchantEntityRenderer::new));
        modBus.addListener((EntityRenderersEvent.RegisterLayerDefinitions event) ->
                event.registerLayerDefinition(MerchantModel.LAYER_LOCATION, MerchantModel::createBodyLayer));
        modBus.addListener((RegisterGuiOverlaysEvent event) ->
                event.registerAboveAll("game_hud",
                        (gui, guiGraphics, partialTick, screenWidth, screenHeight) ->
                                GameHudOverlay.render(guiGraphics, partialTick)));
        MinecraftForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingOut event) -> {
            ClientGameState.reset();
            GameHudOverlay.clearFloatingTexts();
        });
    }
}
