package com.minersmarket.config;

public record MinersMarketConfig(
        int schemaVersion,
        Game game,
        PriceEvent priceEvent,
        StarterItems starterItems,
        Market market
) {
    public static final int CURRENT_SCHEMA_VERSION = 1;

    private static volatile MinersMarketConfig INSTANCE = ConfigDefaults.defaults();

    public static MinersMarketConfig get() {
        return INSTANCE;
    }

    public static void set(MinersMarketConfig config) {
        INSTANCE = config;
    }

    public record Game(long targetSales, int countdownSeconds) {
    }

    public record PriceEvent(
            int intervalSeconds,
            int durationMinSeconds,
            int durationMaxSeconds,
            int changeMinPercent,
            int changeMaxPercent
    ) {
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
