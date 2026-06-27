package com.minersmarket.config;

public final class ConfigDefaults {
    public static final long GAME_TARGET_SALES = 10000L;
    public static final int GAME_COUNTDOWN_SECONDS = 5;

    public static final int PRICE_EVENT_INTERVAL_SECONDS = 600;
    public static final int PRICE_EVENT_DURATION_MIN_SECONDS = 180;
    public static final int PRICE_EVENT_DURATION_MAX_SECONDS = 300;
    public static final int PRICE_EVENT_CHANGE_MIN_PERCENT = 10;
    public static final int PRICE_EVENT_CHANGE_MAX_PERCENT = 30;

    public static final boolean STARTER_ITEMS_ENABLED = true;
    public static final int STARTER_ITEMS_PLAYER_COUNT = 8;
    public static final int STARTER_ITEMS_BREAD_COUNT = 64;
    public static final int STARTER_ITEMS_PICKAXE_FORTUNE_LEVEL = 3;

    public static final boolean MARKET_GENERATE_ON_WORLD_LOAD = true;

    private ConfigDefaults() {
    }

    public static MinersMarketConfig defaults() {
        return new MinersMarketConfig(
                MinersMarketConfig.CURRENT_SCHEMA_VERSION,
                new MinersMarketConfig.Game(GAME_TARGET_SALES, GAME_COUNTDOWN_SECONDS),
                new MinersMarketConfig.PriceEvent(
                        PRICE_EVENT_INTERVAL_SECONDS,
                        PRICE_EVENT_DURATION_MIN_SECONDS,
                        PRICE_EVENT_DURATION_MAX_SECONDS,
                        PRICE_EVENT_CHANGE_MIN_PERCENT,
                        PRICE_EVENT_CHANGE_MAX_PERCENT
                ),
                new MinersMarketConfig.StarterItems(
                        STARTER_ITEMS_ENABLED,
                        STARTER_ITEMS_PLAYER_COUNT,
                        STARTER_ITEMS_BREAD_COUNT,
                        STARTER_ITEMS_PICKAXE_FORTUNE_LEVEL
                ),
                new MinersMarketConfig.Market(MARKET_GENERATE_ON_WORLD_LOAD)
        );
    }
}
