package com.minersmarket.config;

public record MinersMarketConfig(
        int schemaVersion,
        Game game,
        PriceEvent priceEvent,
        ScoreDisplay scoreDisplay,
        StarterItems starterItems,
        Market market
) {
    public static final int CURRENT_SCHEMA_VERSION = 2;

    private static volatile MinersMarketConfig INSTANCE = ConfigDefaults.defaults();

    public static MinersMarketConfig get() {
        return INSTANCE;
    }

    public static void set(MinersMarketConfig config) {
        INSTANCE = config;
    }

    public record Game(GameMode mode, long targetSales, int timeLimitSeconds, int countdownSeconds) {
    }

    public record PriceEvent(
            int intervalSeconds,
            int durationMinSeconds,
            int durationMaxSeconds,
            int changeMinPercent,
            int changeMaxPercent
    ) {
    }

    public record ScoreDisplay(boolean alwaysShow, int hideRemainingSeconds) {
    }

    public record StarterItems(
            boolean enabled,
            int playerCount,
            int breadCount,
            int pickaxeFortuneLevel
    ) {
    }

    public record Market(boolean generateOnWorldLoad) {
    }
}
