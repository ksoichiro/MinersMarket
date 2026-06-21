package com.minersmarket.config;

public final class ConfigDefaults {
    public static final long GAME_TARGET_SALES = 10000L;
    public static final boolean MARKET_GENERATE_ON_WORLD_LOAD = true;

    private ConfigDefaults() {
    }

    public static MinersMarketConfig defaults() {
        return new MinersMarketConfig(
                MinersMarketConfig.CURRENT_SCHEMA_VERSION,
                new MinersMarketConfig.Game(GAME_TARGET_SALES),
                new MinersMarketConfig.Market(MARKET_GENERATE_ON_WORLD_LOAD)
        );
    }
}
