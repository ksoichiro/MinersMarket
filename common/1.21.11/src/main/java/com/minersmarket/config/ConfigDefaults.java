package com.minersmarket.config;

public final class ConfigDefaults {
    public static final boolean MARKET_GENERATE_ON_WORLD_LOAD = true;

    private ConfigDefaults() {
    }

    public static MinersMarketConfig defaults() {
        return new MinersMarketConfig(
                MinersMarketConfig.CURRENT_SCHEMA_VERSION,
                new MinersMarketConfig.Market(MARKET_GENERATE_ON_WORLD_LOAD)
        );
    }
}
