package com.minersmarket.config;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.toml.TomlParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public final class ConfigLoader {
    private static final Logger LOGGER = LoggerFactory.getLogger(ConfigLoader.class);

    private static final String CONFIG_FILE_NAME = "minersmarket.toml";
    private static final String BUNDLED_DEFAULT_RESOURCE = "/minersmarket-default-config.toml";

    private static final String K_SCHEMA_VERSION = "schema_version";
    private static final String K_GAME = "game";
    private static final String K_TARGET_SALES = "target_sales";
    private static final String K_COUNTDOWN_SECONDS = "countdown_seconds";
    private static final String K_PRICE_EVENT = "price_event";
    private static final String K_INTERVAL_SECONDS = "interval_seconds";
    private static final String K_DURATION_MIN_SECONDS = "duration_min_seconds";
    private static final String K_DURATION_MAX_SECONDS = "duration_max_seconds";
    private static final String K_CHANGE_MIN_PERCENT = "change_min_percent";
    private static final String K_CHANGE_MAX_PERCENT = "change_max_percent";
    private static final String K_STARTER_ITEMS = "starter_items";
    private static final String K_ENABLED = "enabled";
    private static final String K_PLAYER_COUNT = "player_count";
    private static final String K_BREAD_COUNT = "bread_count";
    private static final String K_PICKAXE_FORTUNE_LEVEL = "pickaxe_fortune_level";
    private static final String K_MARKET = "market";
    private static final String K_GENERATE_ON_WORLD_LOAD = "generate_on_world_load";

    private ConfigLoader() {
    }

    public static MinersMarketConfig load(Path configDir) {
        Path configFile = configDir.resolve(CONFIG_FILE_NAME);
        try {
            ensureFileExists(configFile);
        } catch (IOException e) {
            LOGGER.error("Failed to write default config at {}; using built-in defaults", configFile, e);
            MinersMarketConfig defaults = ConfigDefaults.defaults();
            MinersMarketConfig.set(defaults);
            return defaults;
        }

        MinersMarketConfig config = parseOrDefaults(configFile);
        MinersMarketConfig.set(config);
        return config;
    }

    private static void ensureFileExists(Path configFile) throws IOException {
        if (Files.exists(configFile)) {
            return;
        }
        Files.createDirectories(configFile.getParent());
        try (InputStream in = ConfigLoader.class.getResourceAsStream(BUNDLED_DEFAULT_RESOURCE)) {
            if (in == null) {
                throw new IOException("Bundled default config resource not found: " + BUNDLED_DEFAULT_RESOURCE);
            }
            Files.copy(in, configFile, StandardCopyOption.REPLACE_EXISTING);
        }
        LOGGER.info("Wrote default config to {}", configFile);
    }

    private static MinersMarketConfig parseOrDefaults(Path configFile) {
        CommentedConfig parsed;
        try (InputStream in = Files.newInputStream(configFile)) {
            parsed = new TomlParser().parse(in);
        } catch (IOException e) {
            LOGGER.error("Failed to read {}; using built-in defaults", configFile, e);
            return ConfigDefaults.defaults();
        } catch (RuntimeException e) {
            LOGGER.error("Failed to parse {}; using built-in defaults", configFile, e);
            return ConfigDefaults.defaults();
        }

        int schemaVersion = readSchemaVersion(parsed);
        if (schemaVersion > MinersMarketConfig.CURRENT_SCHEMA_VERSION) {
            LOGGER.warn(
                    "{} declares schema_version={} but this build only knows up to {}; reading what we can",
                    CONFIG_FILE_NAME, schemaVersion, MinersMarketConfig.CURRENT_SCHEMA_VERSION
            );
        }

        boolean generateOnWorldLoad = readBoolean(
                parsed,
                K_MARKET + "." + K_GENERATE_ON_WORLD_LOAD,
                ConfigDefaults.MARKET_GENERATE_ON_WORLD_LOAD
        );

        long targetSales = readLong(
                parsed,
                K_GAME + "." + K_TARGET_SALES,
                ConfigDefaults.GAME_TARGET_SALES,
                ConfigRanges.MIN_TARGET_SALES,
                ConfigRanges.MAX_TARGET_SALES
        );
        int countdownSeconds = readInt(
                parsed,
                K_GAME + "." + K_COUNTDOWN_SECONDS,
                ConfigDefaults.GAME_COUNTDOWN_SECONDS,
                ConfigRanges.MIN_COUNTDOWN_SECONDS,
                ConfigRanges.MAX_COUNTDOWN_SECONDS
        );

        int intervalSeconds = readInt(
                parsed,
                K_PRICE_EVENT + "." + K_INTERVAL_SECONDS,
                ConfigDefaults.PRICE_EVENT_INTERVAL_SECONDS,
                ConfigRanges.MIN_INTERVAL_SECONDS,
                ConfigRanges.MAX_INTERVAL_SECONDS
        );
        int durationMinSeconds = readInt(
                parsed,
                K_PRICE_EVENT + "." + K_DURATION_MIN_SECONDS,
                ConfigDefaults.PRICE_EVENT_DURATION_MIN_SECONDS,
                ConfigRanges.MIN_DURATION_SECONDS,
                ConfigRanges.MAX_DURATION_SECONDS
        );
        int durationMaxSeconds = readInt(
                parsed,
                K_PRICE_EVENT + "." + K_DURATION_MAX_SECONDS,
                ConfigDefaults.PRICE_EVENT_DURATION_MAX_SECONDS,
                ConfigRanges.MIN_DURATION_SECONDS,
                ConfigRanges.MAX_DURATION_SECONDS
        );
        if (durationMaxSeconds < durationMinSeconds) {
            LOGGER.error("Invalid {}: {}.{} ({}) must be >= {}.{} ({}); using defaults {} and {}",
                    CONFIG_FILE_NAME, K_PRICE_EVENT, K_DURATION_MAX_SECONDS, durationMaxSeconds,
                    K_PRICE_EVENT, K_DURATION_MIN_SECONDS, durationMinSeconds,
                    ConfigDefaults.PRICE_EVENT_DURATION_MIN_SECONDS, ConfigDefaults.PRICE_EVENT_DURATION_MAX_SECONDS);
            durationMinSeconds = ConfigDefaults.PRICE_EVENT_DURATION_MIN_SECONDS;
            durationMaxSeconds = ConfigDefaults.PRICE_EVENT_DURATION_MAX_SECONDS;
        }
        int changeMinPercent = readInt(
                parsed,
                K_PRICE_EVENT + "." + K_CHANGE_MIN_PERCENT,
                ConfigDefaults.PRICE_EVENT_CHANGE_MIN_PERCENT,
                ConfigRanges.MIN_CHANGE_PERCENT,
                ConfigRanges.MAX_CHANGE_PERCENT
        );
        int changeMaxPercent = readInt(
                parsed,
                K_PRICE_EVENT + "." + K_CHANGE_MAX_PERCENT,
                ConfigDefaults.PRICE_EVENT_CHANGE_MAX_PERCENT,
                ConfigRanges.MIN_CHANGE_PERCENT,
                ConfigRanges.MAX_CHANGE_PERCENT
        );
        if (changeMaxPercent < changeMinPercent) {
            LOGGER.error("Invalid {}: {}.{} ({}) must be >= {}.{} ({}); using defaults {} and {}",
                    CONFIG_FILE_NAME, K_PRICE_EVENT, K_CHANGE_MAX_PERCENT, changeMaxPercent,
                    K_PRICE_EVENT, K_CHANGE_MIN_PERCENT, changeMinPercent,
                    ConfigDefaults.PRICE_EVENT_CHANGE_MIN_PERCENT, ConfigDefaults.PRICE_EVENT_CHANGE_MAX_PERCENT);
            changeMinPercent = ConfigDefaults.PRICE_EVENT_CHANGE_MIN_PERCENT;
            changeMaxPercent = ConfigDefaults.PRICE_EVENT_CHANGE_MAX_PERCENT;
        }

        boolean starterEnabled = readBoolean(
                parsed,
                K_STARTER_ITEMS + "." + K_ENABLED,
                ConfigDefaults.STARTER_ITEMS_ENABLED
        );
        int playerCount = readInt(
                parsed,
                K_STARTER_ITEMS + "." + K_PLAYER_COUNT,
                ConfigDefaults.STARTER_ITEMS_PLAYER_COUNT,
                ConfigRanges.MIN_PLAYER_COUNT,
                ConfigRanges.MAX_PLAYER_COUNT
        );
        int breadCount = readInt(
                parsed,
                K_STARTER_ITEMS + "." + K_BREAD_COUNT,
                ConfigDefaults.STARTER_ITEMS_BREAD_COUNT,
                ConfigRanges.MIN_BREAD_COUNT,
                ConfigRanges.MAX_BREAD_COUNT
        );
        int pickaxeFortuneLevel = readInt(
                parsed,
                K_STARTER_ITEMS + "." + K_PICKAXE_FORTUNE_LEVEL,
                ConfigDefaults.STARTER_ITEMS_PICKAXE_FORTUNE_LEVEL,
                ConfigRanges.MIN_FORTUNE_LEVEL,
                ConfigRanges.MAX_FORTUNE_LEVEL
        );

        for (CommentedConfig.Entry entry : parsed.entrySet()) {
            String key = entry.getKey();
            if (!key.equals(K_SCHEMA_VERSION) && !key.equals(K_GAME) && !key.equals(K_PRICE_EVENT)
                    && !key.equals(K_STARTER_ITEMS) && !key.equals(K_MARKET)) {
                LOGGER.warn("Unknown top-level key in {}: {}", CONFIG_FILE_NAME, key);
            }
        }

        return new MinersMarketConfig(
                schemaVersion,
                new MinersMarketConfig.Game(targetSales, countdownSeconds),
                new MinersMarketConfig.PriceEvent(
                        intervalSeconds,
                        durationMinSeconds,
                        durationMaxSeconds,
                        changeMinPercent,
                        changeMaxPercent
                ),
                new MinersMarketConfig.StarterItems(
                        starterEnabled,
                        playerCount,
                        breadCount,
                        pickaxeFortuneLevel
                ),
                new MinersMarketConfig.Market(generateOnWorldLoad)
        );
    }

    private static int readSchemaVersion(CommentedConfig parsed) {
        Object value = parsed.get(K_SCHEMA_VERSION);
        if (value == null) {
            return MinersMarketConfig.CURRENT_SCHEMA_VERSION;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        LOGGER.error("Invalid {}.{} = {} (must be a number); using default {}",
                CONFIG_FILE_NAME, K_SCHEMA_VERSION, value, MinersMarketConfig.CURRENT_SCHEMA_VERSION);
        return MinersMarketConfig.CURRENT_SCHEMA_VERSION;
    }

    private static boolean readBoolean(CommentedConfig parsed, String path, boolean defaultValue) {
        Object value = parsed.get(path);
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Boolean bool) {
            return bool;
        }
        LOGGER.error("Invalid {}.{} = {} (must be true or false); using default {}",
                CONFIG_FILE_NAME, path, value, defaultValue);
        return defaultValue;
    }

    private static long readLong(CommentedConfig parsed, String path, long defaultValue, long min, long max) {
        Object value = parsed.get(path);
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Number number) {
            long longValue = number.longValue();
            if (longValue >= min && longValue <= max) {
                return longValue;
            }
        }
        LOGGER.error("Invalid {}.{} = {} (must be in [{}, {}]); using default {}",
                CONFIG_FILE_NAME, path, value, min, max, defaultValue);
        return defaultValue;
    }

    private static int readInt(CommentedConfig parsed, String path, int defaultValue, int min, int max) {
        Object value = parsed.get(path);
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Number number) {
            int intValue = number.intValue();
            if (intValue >= min && intValue <= max) {
                return intValue;
            }
        }
        LOGGER.error("Invalid {}.{} = {} (must be in [{}, {}]); using default {}",
                CONFIG_FILE_NAME, path, value, min, max, defaultValue);
        return defaultValue;
    }
}
