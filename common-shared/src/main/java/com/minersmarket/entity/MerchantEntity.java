package com.minersmarket.entity;

import com.minersmarket.network.GameStateSyncPacket;
import com.minersmarket.state.GameState;
import com.minersmarket.state.GameStateManager;
import com.minersmarket.trade.PriceList;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class MerchantEntity extends Mob {
    public MerchantEntity(EntityType<? extends Mob> entityType, Level level) {
        super(entityType, level);
        this.setNoAi(true);
        this.setInvulnerable(true);
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND) {
            return InteractionResult.PASS;
        }
        if (level().isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        GameStateManager manager = GameStateManager.getInstance();
        if (manager == null) {
            return InteractionResult.FAIL;
        }

        if (!manager.canSell()) {
            player.sendSystemMessage(Component.translatable("message.minersmarket.cannot_sell"));
            return InteractionResult.CONSUME;
        }

        ItemStack heldItem = player.getItemInHand(hand);
        if (heldItem.isEmpty() || !PriceList.isSellable(heldItem.getItem())) {
            player.sendSystemMessage(Component.translatable("message.minersmarket.not_sellable"));
            return InteractionResult.CONSUME;
        }

        int pricePerItem = PriceList.getPrice(heldItem.getItem());
        int sellCount = heldItem.getCount();
        long totalEarned = (long) pricePerItem * sellCount;

        heldItem.shrink(sellCount);
        manager.addSalesAmount(player.getUUID(), totalEarned);
        ServerPlayer serverPlayer = (ServerPlayer) player;
        serverPlayer.playNotifySound(SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 1.0f, 1.0f);

        // Check if this player just reached the target
        if (manager.hasReachedTarget(player.getUUID())
                && !manager.hasFinished(player.getUUID())) {
            manager.recordFinish(player.getUUID(), player.getDisplayName().getString());
            if (manager.getState() == GameState.IN_PROGRESS) {
                manager.end();
                manager.broadcastWinner(serverPlayer);
            }
            // Sync ranking to all players
            if (level().getServer() != null) {
                for (ServerPlayer p : level().getServer().getPlayerList().getPlayers()) {
                    GameStateSyncPacket.sendToPlayer(p, manager);
                }
            }
        } else {
            GameStateSyncPacket.sendToPlayer(serverPlayer, manager);
        }

        return InteractionResult.SUCCESS;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    protected void pushEntities() {
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;
    }

    @Override
    public boolean canBeLeashed() {
        return false;
    }
}
