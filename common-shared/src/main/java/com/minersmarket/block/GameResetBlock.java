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

public class GameResetBlock extends Block {
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
        if (manager.canReset()) {
            manager.reset();
            player.sendSystemMessage(Component.translatable("message.minersmarket.game_reset"));
            return InteractionResult.SUCCESS;
        }
        player.sendSystemMessage(Component.translatable("message.minersmarket.cannot_reset"));
        return InteractionResult.CONSUME;
    }
}
