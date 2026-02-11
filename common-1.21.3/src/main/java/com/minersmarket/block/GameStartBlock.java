package com.minersmarket.block;

import com.minersmarket.state.GameStateManager;
import net.minecraft.core.BlockPos;
import com.minersmarket.state.GameState;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public class GameStartBlock extends Block {
    public GameStartBlock(Properties properties) {
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
        if (manager.canStart()) {
            manager.startCountdown();
            return InteractionResult.SUCCESS;
        }
        if (manager.isCountdownActive()) {
            player.displayClientMessage(Component.translatable("message.minersmarket.countdown_in_progress"), false);
        } else if (manager.getState() == GameState.IN_PROGRESS) {
            player.displayClientMessage(Component.translatable("message.minersmarket.already_in_progress"), false);
        } else if (manager.getState() == GameState.ENDED) {
            player.displayClientMessage(Component.translatable("message.minersmarket.game_ended_reset_required"), false);
        }
        return InteractionResult.CONSUME;
    }
}
