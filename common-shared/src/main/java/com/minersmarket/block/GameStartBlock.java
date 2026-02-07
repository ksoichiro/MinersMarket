package com.minersmarket.block;

import com.minersmarket.state.GameStateManager;
import net.minecraft.core.BlockPos;
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
        player.sendSystemMessage(Component.translatable("message.minersmarket.cannot_start"));
        return InteractionResult.CONSUME;
    }
}
