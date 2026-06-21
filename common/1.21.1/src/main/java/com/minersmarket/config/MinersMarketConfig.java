package com.minersmarket.config;

public record MinersMarketConfig(
        int schemaVersion,
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

    public record Market(boolean generateOnWorldLoad) {
    }
}
