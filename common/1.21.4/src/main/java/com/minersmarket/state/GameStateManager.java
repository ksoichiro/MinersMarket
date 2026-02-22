package com.minersmarket.state;

import com.minersmarket.trade.PriceList;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

public class GameStateManager {
    private static final String DATA_ID = "minersmarket_game_state";
    public static final long TARGET_SALES = 10000;

    private static final int COUNTDOWN_SECONDS = 5;
    private static final int TICKS_PER_SECOND = 20;
    private static final int PRICE_EVENT_INTERVAL = 12000;  // 10 minutes
    private static final int PRICE_EVENT_DURATION_MIN_MINUTES = 3;
    private static final int PRICE_EVENT_DURATION_MAX_MINUTES = 5;

    private static GameStateManager instance;
    private final GameStateSavedData savedData;
    private ServerLevel serverLevel;
    private int countdownTicks = -1;
    private int priceEventCooldownTicks = PRICE_EVENT_INTERVAL;
    private int priceEventDurationTicks = 0;
    private float priceMultiplier = 1.0f;
    private final Random random = new Random();

    private GameStateManager(GameStateSavedData savedData) {
        this.savedData = savedData;
    }

    public static void init(ServerLevel level) {
        GameStateSavedData data = level.getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(GameStateSavedData::new, GameStateSavedData::load, null),
                DATA_ID
        );
        instance = new GameStateManager(data);
        instance.serverLevel = level;
    }

    public static GameStateManager getInstance() {
        return instance;
    }

    public static void clear() {
        instance = null;
    }

    // State

    public GameState getState() {
        return savedData.state;
    }

    // State transitions

    public void startCountdown() {
        countdownTicks = COUNTDOWN_SECONDS * TICKS_PER_SECOND;
        savedData.salesAmounts.clear();
        savedData.playTime = 0;
        savedData.setDirty();
    }

    public boolean isCountdownActive() {
        return countdownTicks >= 0;
    }

    private void start() {
        savedData.state = GameState.IN_PROGRESS;
        priceEventCooldownTicks = PRICE_EVENT_INTERVAL;
        priceEventDurationTicks = 0;
        priceMultiplier = 1.0f;
        savedData.setDirty();
    }

    public void end() {
        savedData.state = GameState.ENDED;
        savedData.setDirty();
    }

    public void reset() {
        savedData.state = GameState.NOT_STARTED;
        savedData.playTime = 0;
        savedData.salesAmounts.clear();
        savedData.finishedPlayers.clear();
        countdownTicks = -1;
        priceEventCooldownTicks = PRICE_EVENT_INTERVAL;
        priceEventDurationTicks = 0;
        priceMultiplier = 1.0f;
        savedData.setDirty();
    }

    // Sales

    public long getSalesAmount(UUID playerId) {
        return savedData.salesAmounts.getOrDefault(playerId, 0L);
    }

    public Map<UUID, Long> getAllSalesAmounts() {
        return savedData.salesAmounts;
    }

    public void addSalesAmount(UUID playerId, long amount) {
        long current = getSalesAmount(playerId);
        savedData.salesAmounts.put(playerId, current + amount);
        savedData.setDirty();
    }

    // Play time

    public int getPlayTime() {
        return savedData.playTime;
    }

    public void tick() {
        if (countdownTicks >= 0) {
            tickCountdown();
        }
        if (savedData.state == GameState.IN_PROGRESS || savedData.state == GameState.ENDED) {
            savedData.playTime++;
            tickPriceEvent();
            savedData.setDirty();
        }
    }

    private void tickCountdown() {
        if (countdownTicks > 0 && countdownTicks % TICKS_PER_SECOND == 0) {
            int secondsLeft = countdownTicks / TICKS_PER_SECOND;
            broadcastTitle(Component.literal(String.valueOf(secondsLeft)), 0, 25, 0);
        } else if (countdownTicks == 0) {
            broadcastTitle(
                    Component.translatable("message.minersmarket.game_started"),
                    0, 40, 10
            );
            start();
        }
        countdownTicks--;
    }

    private void tickPriceEvent() {
        if (priceEventDurationTicks > 0) {
            priceEventDurationTicks--;
            if (priceEventDurationTicks == 0) {
                priceMultiplier = 1.0f;
                priceEventCooldownTicks = PRICE_EVENT_INTERVAL;
                broadcastMessage(Component.translatable("message.minersmarket.price_event_end"));
            }
        } else {
            priceEventCooldownTicks--;
            if (priceEventCooldownTicks <= 0) {
                startPriceEvent();
            }
        }
    }

    public void startPriceEvent() {
        // Randomly choose up or down, then 10-30% change
        boolean up = random.nextBoolean();
        float percentage = 0.1f + random.nextFloat() * 0.2f;
        priceMultiplier = up ? 1.0f + percentage : 1.0f - percentage;
        int durationMinutes = PRICE_EVENT_DURATION_MIN_MINUTES
                + random.nextInt(PRICE_EVENT_DURATION_MAX_MINUTES - PRICE_EVENT_DURATION_MIN_MINUTES + 1);
        priceEventDurationTicks = durationMinutes * 60 * 20;
        broadcastTitleWithSubtitle(
                Component.translatable("message.minersmarket.price_event_start"),
                Component.translatable("message.minersmarket.price_event_start_subtitle", durationMinutes),
                10, 60, 20
        );
    }

    public int getEffectivePrice(Item item) {
        int basePrice = PriceList.getPrice(item);
        if (priceMultiplier == 1.0f) {
            return basePrice;
        }
        return Math.max(1, (int) Math.ceil(basePrice * priceMultiplier));
    }

    public float getPriceMultiplier() {
        return priceMultiplier;
    }

    public boolean isPriceEventActive() {
        return priceEventDurationTicks > 0;
    }

    public int getPriceEventRemainingTicks() {
        return priceEventDurationTicks;
    }

    private void broadcastTitle(Component title, int fadeIn, int stay, int fadeOut) {
        if (serverLevel == null || serverLevel.getServer() == null) return;
        for (ServerPlayer player : serverLevel.getServer().getPlayerList().getPlayers()) {
            player.connection.send(new ClientboundSetTitlesAnimationPacket(fadeIn, stay, fadeOut));
            player.connection.send(new ClientboundSetTitleTextPacket(title));
        }
    }

    private void broadcastTitleWithSubtitle(Component title, Component subtitle,
                                              int fadeIn, int stay, int fadeOut) {
        if (serverLevel == null || serverLevel.getServer() == null) return;
        for (ServerPlayer player : serverLevel.getServer().getPlayerList().getPlayers()) {
            player.connection.send(new ClientboundSetTitlesAnimationPacket(fadeIn, stay, fadeOut));
            player.connection.send(new ClientboundSetTitleTextPacket(title));
            player.connection.send(new ClientboundSetSubtitleTextPacket(subtitle));
        }
    }

    private void broadcastMessage(Component message) {
        if (serverLevel == null || serverLevel.getServer() == null) return;
        for (ServerPlayer player : serverLevel.getServer().getPlayerList().getPlayers()) {
            player.sendSystemMessage(message);
        }
    }

    public void broadcastWinner(ServerPlayer winner) {
        Component title = Component.translatable("message.minersmarket.winner_title");
        Component subtitle = Component.translatable("message.minersmarket.winner_subtitle", winner.getDisplayName());
        if (serverLevel == null || serverLevel.getServer() == null) return;
        for (ServerPlayer player : serverLevel.getServer().getPlayerList().getPlayers()) {
            player.connection.send(new ClientboundSetTitlesAnimationPacket(10, 60, 20));
            player.connection.send(new ClientboundSetTitleTextPacket(title));
            player.connection.send(new ClientboundSetSubtitleTextPacket(subtitle));
            player.playNotifySound(SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 1.0f, 1.0f);
        }
    }

    public void broadcastGoalReached(ServerPlayer finisher) {
        Component title = Component.translatable("message.minersmarket.goal_reached_title");
        Component subtitle = Component.translatable("message.minersmarket.goal_reached_subtitle", finisher.getDisplayName());
        if (serverLevel == null || serverLevel.getServer() == null) return;
        for (ServerPlayer player : serverLevel.getServer().getPlayerList().getPlayers()) {
            player.connection.send(new ClientboundSetTitlesAnimationPacket(10, 60, 20));
            player.connection.send(new ClientboundSetTitleTextPacket(title));
            player.connection.send(new ClientboundSetSubtitleTextPacket(subtitle));
            player.playNotifySound(SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 1.0f, 1.0f);
        }
    }

    // State checks

    public boolean canSell() {
        return savedData.state == GameState.IN_PROGRESS || savedData.state == GameState.ENDED;
    }

    public boolean canStart() {
        return savedData.state == GameState.NOT_STARTED && !isCountdownActive();
    }

    public boolean canReset() {
        return savedData.state == GameState.IN_PROGRESS || savedData.state == GameState.ENDED;
    }

    // Finish tracking

    public boolean hasFinished(UUID playerId) {
        return savedData.finishedPlayers.stream()
                .anyMatch(fp -> fp.playerId().equals(playerId));
    }

    public void recordFinish(UUID playerId, String playerName) {
        savedData.finishedPlayers.add(new FinishedPlayer(playerId, playerName, savedData.playTime));
        savedData.setDirty();
    }

    public List<FinishedPlayer> getFinishedPlayers() {
        return savedData.finishedPlayers;
    }

    // Win check

    public boolean hasReachedTarget(UUID playerId) {
        return getSalesAmount(playerId) >= TARGET_SALES;
    }

    // Market generation

    public boolean isMarketGenerated() {
        return savedData.marketGenerated;
    }

    public void setMarketGenerated() {
        savedData.marketGenerated = true;
        savedData.setDirty();
    }

    public ServerLevel getServerLevel() {
        return serverLevel;
    }
}
