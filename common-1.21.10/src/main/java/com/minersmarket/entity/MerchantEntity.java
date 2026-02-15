package com.minersmarket.entity;

import com.minersmarket.network.GameStateSyncPacket;
import com.minersmarket.state.GameState;
import com.minersmarket.state.GameStateManager;
import com.minersmarket.trade.PriceList;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;

public class MerchantEntity extends Mob {
    private static final EntityDataAccessor<Integer> DATA_UNHAPPY_COUNTER =
            SynchedEntityData.defineId(MerchantEntity.class, EntityDataSerializers.INT);
    private int lookAtCounter;
    private float originalYRot;
    private float originalYBodyRot;
    private float originalYHeadRot;

    public MerchantEntity(EntityType<? extends Mob> entityType, Level level) {
        super(entityType, level);
        this.setNoAi(true);
        this.setInvulnerable(true);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_UNHAPPY_COUNTER, 0);
    }

    public int getUnhappyCounter() {
        return this.entityData.get(DATA_UNHAPPY_COUNTER);
    }

    private void setUnhappyCounter(int counter) {
        this.entityData.set(DATA_UNHAPPY_COUNTER, counter);
    }

    @Override
    public void tick() {
        super.tick();
        if (getUnhappyCounter() > 0) {
            setUnhappyCounter(getUnhappyCounter() - 1);
        }
        if (level().isClientSide()) {
            // Stationary noAi mob: always keep body facing same direction as head
            yBodyRot = yHeadRot;
        } else if (lookAtCounter > 0) {
            lookAtCounter--;
            if (lookAtCounter == 0) {
                setYRot(originalYRot);
                yBodyRot = originalYBodyRot;
                yHeadRot = originalYHeadRot;
            }
        }
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND) {
            return InteractionResult.PASS;
        }
        if (level().isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        // Face the player
        if (lookAtCounter <= 0) {
            originalYRot = getYRot();
            originalYBodyRot = yBodyRot;
            originalYHeadRot = yHeadRot;
        }
        double dx = player.getX() - this.getX();
        double dz = player.getZ() - this.getZ();
        float targetYRot = (float) (Mth.atan2(dz, dx) * (180.0F / (float) Math.PI)) - 90.0F;
        this.setYRot(targetYRot);
        this.yBodyRot = targetYRot;
        this.yHeadRot = targetYRot;
        lookAtCounter = 10;

        GameStateManager manager = GameStateManager.getInstance();
        if (manager == null) {
            return InteractionResult.FAIL;
        }

        if (!manager.canSell()) {
            player.displayClientMessage(Component.translatable("message.minersmarket.game_not_started"), false);
            setUnhappyCounter(20);
            this.playSound(SoundEvents.VILLAGER_NO, 1.0f, 1.0f);
            return InteractionResult.CONSUME;
        }

        ItemStack heldItem = player.getItemInHand(hand);
        if (heldItem.isEmpty() || !PriceList.isSellable(heldItem.getItem())) {
            player.displayClientMessage(Component.translatable("message.minersmarket.not_sellable"), false);
            setUnhappyCounter(20);
            this.playSound(SoundEvents.VILLAGER_NO, 1.0f, 1.0f);
            return InteractionResult.CONSUME;
        }

        int pricePerItem = manager.getEffectivePrice(heldItem.getItem());
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
            } else {
                manager.broadcastGoalReached(serverPlayer);
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
