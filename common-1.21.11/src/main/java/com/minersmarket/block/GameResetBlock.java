package com.minersmarket.block;

import com.minersmarket.event.PlayerSpawnHandler;
import com.minersmarket.network.GameStateSyncPacket;
import com.minersmarket.state.GameStateManager;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class GameResetBlock extends Block {
    private static final long CONFIRM_TIMEOUT_MS = 5000;
    private final Map<UUID, Long> pendingConfirmations = new HashMap<>();

    public GameResetBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                               Player player, BlockHitResult hitResult) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        GameStateManager manager = GameStateManager.getInstance();
        if (manager == null) {
            return InteractionResult.FAIL;
        }
        if (!manager.canReset()) {
            player.displayClientMessage(Component.translatable("message.minersmarket.game_not_started"), false);
            return InteractionResult.CONSUME;
        }

        UUID playerId = player.getUUID();
        long now = System.currentTimeMillis();
        Long lastClick = pendingConfirmations.get(playerId);

        if (lastClick != null && (now - lastClick) <= CONFIRM_TIMEOUT_MS) {
            // Confirmed: execute reset
            pendingConfirmations.remove(playerId);
            manager.reset();
            ServerLevel serverLevel = (ServerLevel) level;
            BlockPos spawnPos = serverLevel.getRespawnData().pos();
            for (ServerPlayer sp : serverLevel.getServer().getPlayerList().getPlayers()) {
                // Teleport to spawn and reset inventory/equipment
                sp.teleportTo(serverLevel,
                        spawnPos.getX() + 0.5, spawnPos.getY(), spawnPos.getZ() + 0.5,
                        java.util.Set.of(), sp.getYRot(), sp.getXRot(), true);
                PlayerSpawnHandler.resetPlayer(sp);
                sp.displayClientMessage(Component.translatable("message.minersmarket.game_reset"), false);
                GameStateSyncPacket.sendToPlayer(sp, manager);
            }
            return InteractionResult.SUCCESS;
        }

        // First click: ask for confirmation
        pendingConfirmations.put(playerId, now);
        player.displayClientMessage(Component.translatable("message.minersmarket.confirm_reset"), false);
        return InteractionResult.CONSUME;
    }
}
