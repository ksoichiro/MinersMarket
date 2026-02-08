package com.minersmarket.block;

import com.minersmarket.network.GameStateSyncPacket;
import com.minersmarket.state.GameStateManager;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
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
            player.sendSystemMessage(Component.translatable("message.minersmarket.cannot_reset"));
            return InteractionResult.CONSUME;
        }

        UUID playerId = player.getUUID();
        long now = System.currentTimeMillis();
        Long lastClick = pendingConfirmations.get(playerId);

        if (lastClick != null && (now - lastClick) <= CONFIRM_TIMEOUT_MS) {
            // Confirmed: execute reset
            pendingConfirmations.remove(playerId);
            manager.reset();
            for (ServerPlayer sp : player.getServer().getPlayerList().getPlayers()) {
                sp.sendSystemMessage(Component.translatable("message.minersmarket.game_reset"));
                GameStateSyncPacket.sendToPlayer(sp, manager);
            }
            return InteractionResult.SUCCESS;
        }

        // First click: ask for confirmation
        pendingConfirmations.put(playerId, now);
        player.sendSystemMessage(Component.translatable("message.minersmarket.confirm_reset"));
        return InteractionResult.CONSUME;
    }
}
