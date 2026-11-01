package com.minersmarket.config;

import java.util.Locale;

public enum GameMode {
    /** First player to reach the target sales amount wins. */
    TARGET,
    /** Highest sales amount when the time limit expires wins. */
    TIME_LIMIT;

    /** The lower-case form used in minersmarket.toml, e.g. "time_limit". */
    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    /** Returns null for an unknown id so callers can log and fall back. */
    public static GameMode fromId(String id) {
        for (GameMode mode : values()) {
            if (mode.id().equals(id)) {
                return mode;
            }
        }
        return null;
    }
}
