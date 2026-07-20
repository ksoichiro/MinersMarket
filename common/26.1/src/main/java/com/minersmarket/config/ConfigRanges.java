package com.minersmarket.config;

/**
 * Validation ranges for config values, shared by ConfigLoader and the config screen
 * so both enforce identical rules.
 */
public final class ConfigRanges {
    public static final long MIN_TARGET_SALES = 1L;
    public static final long MAX_TARGET_SALES = Long.MAX_VALUE;
    public static final int MIN_COUNTDOWN_SECONDS = 0;
    public static final int MAX_COUNTDOWN_SECONDS = 300;
    public static final int MIN_INTERVAL_SECONDS = 1;
    public static final int MAX_INTERVAL_SECONDS = Integer.MAX_VALUE;
    public static final int MIN_DURATION_SECONDS = 1;
    public static final int MAX_DURATION_SECONDS = Integer.MAX_VALUE;
    public static final int MIN_CHANGE_PERCENT = 0;
    public static final int MAX_CHANGE_PERCENT = Integer.MAX_VALUE;
    // Large chest holds 54 slots; each player takes one pickaxe slot and one bread slot.
    public static final int MIN_PLAYER_COUNT = 0;
    public static final int MAX_PLAYER_COUNT = 27;
    public static final int MIN_BREAD_COUNT = 1;
    public static final int MAX_BREAD_COUNT = 64;
    public static final int MIN_FORTUNE_LEVEL = 0;
    public static final int MAX_FORTUNE_LEVEL = 255;

    private ConfigRanges() {
    }
}
