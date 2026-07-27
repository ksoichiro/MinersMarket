package com.minersmarket.state;

import com.minersmarket.MinersMarket;
import com.minersmarket.config.ConfigDefaults;
import com.minersmarket.config.GameMode;
import com.minersmarket.config.MinersMarketConfig;
import com.minersmarket.network.GameStateSyncPacket;
import com.minersmarket.trade.PriceList;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.function.Supplier;

public class GameStateManager {
    private static final SavedDataType<GameStateSavedData> DATA_TYPE =
            new SavedDataType<>(Identifier.fromNamespaceAndPath(MinersMarket.MOD_ID, "game_state"),
                    (Supplier<GameStateSavedData>) GameStateSavedData::new,
                    GameStateSavedData.CODEC, null);
    private static final int TICKS_PER_SECOND = 20;

    private static int priceEventIntervalTicks() {
        return MinersMarketConfig.get().priceEvent().intervalSeconds() * TICKS_PER_SECOND;
    }

    private static GameStateManager instance;
    private final GameStateSavedData savedData;
    private ServerLevel serverLevel;
    private int countdownTicks = -1;
    private int priceEventCooldownTicks = priceEventIntervalTicks();
    private int priceEventDurationTicks = 0;
    private float priceMultiplier = 1.0f;
    private final Random random = new Random();

    private GameStateManager(GameStateSavedData savedData) {
        this.savedData = savedData;
    }

    public static void init(ServerLevel level) {
        GameStateSavedData data = level.getDataStorage().computeIfAbsent(DATA_TYPE);
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
        countdownTicks = MinersMarketConfig.get().game().countdownSeconds() * TICKS_PER_SECOND;
        savedData.salesAmounts.clear();
        savedData.playerNames.clear();
        savedData.playTime = 0;
        savedData.targetSales = MinersMarketConfig.get().game().targetSales();
        // Snapshot the rule-affecting settings so editing the config mid-game
        // cannot change the rules of a game already running.
        savedData.mode = MinersMarketConfig.get().game().mode();
        // Target mode has no time limit; leaving a stale value here would put a
        // meaningless countdown in every sync packet.
        savedData.timeLimitTicks = savedData.mode == GameMode.TIME_LIMIT
                ? MinersMarketConfig.get().game().timeLimitSeconds() * TICKS_PER_SECOND
                : 0;
        savedData.setDirty();
    }

    public boolean isCountdownActive() {
        return countdownTicks >= 0;
    }

    private void start() {
        savedData.state = GameState.IN_PROGRESS;
        priceEventCooldownTicks = priceEventIntervalTicks();
        priceEventDurationTicks = 0;
        priceMultiplier = 1.0f;
        savedData.setDirty();
    }

    public void end() {
        savedData.state = GameState.ENDED;
        savedData.setDirty();
    }

    private void endByTimeLimit() {
        savedData.state = GameState.ENDED;
        // The clock stops here, so a price event still running would otherwise stay
        // frozen on the HUD until the game is reset.
        priceEventDurationTicks = 0;
        priceMultiplier = 1.0f;
        savedData.setDirty();

        List<RankedPlayer> ranking = getRanking();
        List<String> winners = new ArrayList<>();
        for (RankedPlayer entry : ranking) {
            if (entry.rank() == 1 && entry.salesAmount() > 0) {
                winners.add(entry.playerName());
            }
        }
        // With everyone on zero the shared-rank rule would make every player a joint
        // winner, which is not a result worth announcing.
        Component subtitle = winners.isEmpty()
                ? Component.translatable("message.minersmarket.time_up_no_winner")
                : Component.translatable("message.minersmarket.time_up_subtitle", String.join(", ", winners));
        broadcastTitleWithSubtitle(
                Component.translatable("message.minersmarket.time_up_title")
                        .withStyle(style -> style.withColor(ChatFormatting.GOLD).withBold(true)),
                subtitle, 10, 80, 20);
        broadcastSound(SoundEvents.UI_TOAST_CHALLENGE_COMPLETE);

        broadcastMessage(Component.translatable("message.minersmarket.final_ranking_header"));
        for (RankedPlayer entry : ranking) {
            broadcastMessage(Component.translatable("message.minersmarket.final_ranking_entry",
                    entry.rank(), entry.playerName(), String.format("%,d", entry.salesAmount())));
        }

        if (serverLevel != null && serverLevel.getServer() != null) {
            for (ServerPlayer player : serverLevel.getServer().getPlayerList().getPlayers()) {
                GameStateSyncPacket.sendToPlayer(player, this);
            }
        }
    }

    public void reset() {
        savedData.state = GameState.NOT_STARTED;
        savedData.playTime = 0;
        savedData.salesAmounts.clear();
        savedData.finishedPlayers.clear();
        savedData.playerNames.clear();
        savedData.mode = ConfigDefaults.GAME_MODE;
        savedData.timeLimitTicks = 0;
        countdownTicks = -1;
        priceEventCooldownTicks = priceEventIntervalTicks();
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

    public void addSalesAmount(UUID playerId, String playerName, long amount) {
        long current = getSalesAmount(playerId);
        savedData.salesAmounts.put(playerId, current + amount);
        savedData.playerNames.put(playerId, playerName);
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
            // Once the limit is reached the clock and the price events stop, so the
            // result stays fixed and the HUD keeps showing 00:00.
            if (!isTimeLimitExpired()) {
                savedData.playTime++;
                tickPriceEvent();
            }
            savedData.setDirty();
            if (savedData.state == GameState.IN_PROGRESS && isTimeLimitExpired()) {
                endByTimeLimit();
            }
        }
    }

    private void tickCountdown() {
        if (countdownTicks > 0 && countdownTicks % TICKS_PER_SECOND == 0) {
            int secondsLeft = countdownTicks / TICKS_PER_SECOND;
            broadcastTitle(Component.literal(String.valueOf(secondsLeft)), 0, 25, 0);
        } else if (countdownTicks == 0) {
            broadcastTitle(
                    Component.translatable("message.minersmarket.game_started")
                            .withStyle(style -> style.withColor(ChatFormatting.GOLD).withBold(true)),
                    0, 40, 10
            );
            broadcastSound(SoundEvents.ANVIL_PLACE);
            start();
        }
        countdownTicks--;
    }

    private void tickPriceEvent() {
        if (priceEventDurationTicks > 0) {
            priceEventDurationTicks--;
            if (priceEventDurationTicks == 0) {
                priceMultiplier = 1.0f;
                priceEventCooldownTicks = priceEventIntervalTicks();
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
        MinersMarketConfig.PriceEvent config = MinersMarketConfig.get().priceEvent();
        // Randomly choose up or down, then apply a configured percentage change.
        boolean up = random.nextBoolean();
        float minPercent = config.changeMinPercent() / 100.0f;
        float maxPercent = config.changeMaxPercent() / 100.0f;
        float percentage = minPercent + random.nextFloat() * (maxPercent - minPercent);
        priceMultiplier = up ? 1.0f + percentage : 1.0f - percentage;
        int durationSeconds = config.durationMinSeconds()
                + random.nextInt(config.durationMaxSeconds() - config.durationMinSeconds() + 1);
        priceEventDurationTicks = durationSeconds * TICKS_PER_SECOND;
        broadcastTitleWithSubtitle(
                Component.translatable("message.minersmarket.price_event_start"),
                Component.translatable("message.minersmarket.price_event_start_subtitle", durationSeconds),
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

    private void broadcastSound(SoundEvent sound) {
        if (serverLevel == null || serverLevel.getServer() == null) return;
        for (ServerPlayer player : serverLevel.getServer().getPlayerList().getPlayers()) {
            playNotifySound(player, sound, SoundSource.PLAYERS, 1.0f, 1.0f);
        }
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
            playNotifySound(player, SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 1.0f, 1.0f);
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
            playNotifySound(player, SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 1.0f, 1.0f);
        }
    }

    // State checks

    public boolean canSell() {
        if (savedData.mode == GameMode.TIME_LIMIT) {
            // The result is final the moment the timer expires.
            return savedData.state == GameState.IN_PROGRESS;
        }
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
        return getSalesAmount(playerId) >= savedData.targetSales;
    }

    public long getTargetSales() {
        return savedData.targetSales;
    }

    public GameMode getMode() {
        return savedData.mode;
    }

    public int getRemainingTicks() {
        return Math.max(0, savedData.timeLimitTicks - savedData.playTime);
    }

    private boolean isTimeLimitExpired() {
        return savedData.mode == GameMode.TIME_LIMIT && savedData.playTime >= savedData.timeLimitTicks;
    }

    /**
     * Whether the full ranking may be shown. The score-display settings are read live
     * rather than snapshotted, so a host can toggle the scoreboard mid-game.
     */
    public boolean isRankingVisible() {
        if (savedData.mode != GameMode.TIME_LIMIT) {
            return false;
        }
        if (savedData.state == GameState.ENDED) {
            return true;
        }
        MinersMarketConfig.ScoreDisplay display = MinersMarketConfig.get().scoreDisplay();
        if (!display.alwaysShow()) {
            return false;
        }
        return display.hideRemainingSeconds() == 0
                || getRemainingTicks() > display.hideRemainingSeconds() * TICKS_PER_SECOND;
    }

    public List<RankedPlayer> getRanking() {
        Map<UUID, Long> amounts = new HashMap<>(savedData.salesAmounts);
        Map<UUID, String> names = new HashMap<>(savedData.playerNames);
        if (serverLevel != null && serverLevel.getServer() != null) {
            // Online players who have not sold anything still belong in the ranking,
            // and their names are fresher than the stored ones.
            for (ServerPlayer player : serverLevel.getServer().getPlayerList().getPlayers()) {
                amounts.putIfAbsent(player.getUUID(), 0L);
                names.put(player.getUUID(), player.getDisplayName().getString());
            }
        }

        List<Map.Entry<UUID, Long>> sorted = new ArrayList<>(amounts.entrySet());
        sorted.sort(Comparator.<Map.Entry<UUID, Long>>comparingLong(Map.Entry::getValue).reversed());

        List<RankedPlayer> ranking = new ArrayList<>();
        long previousAmount = Long.MIN_VALUE;
        int previousRank = 0;
        for (int i = 0; i < sorted.size(); i++) {
            Map.Entry<UUID, Long> entry = sorted.get(i);
            long amount = entry.getValue();
            // Equal amounts share a rank; the next distinct amount skips the tied places.
            int rank = amount == previousAmount ? previousRank : i + 1;
            previousAmount = amount;
            previousRank = rank;
            String name = names.getOrDefault(entry.getKey(), entry.getKey().toString());
            ranking.add(new RankedPlayer(rank, name, amount));
        }
        return ranking;
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

    // 1.21.11: ServerPlayer.playNotifySound removed; send sound packet directly
    private static void playNotifySound(ServerPlayer player, SoundEvent sound, SoundSource source, float volume, float pitch) {
        player.connection.send(new ClientboundSoundPacket(
                Holder.direct(sound), source,
                player.getX(), player.getY(), player.getZ(),
                volume, pitch, player.level().getRandom().nextLong()));
    }
}
