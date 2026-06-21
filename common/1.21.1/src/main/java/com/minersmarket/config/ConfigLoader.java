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

        for (CommentedConfig.Entry entry : parsed.entrySet()) {
            String key = entry.getKey();
            if (!key.equals(K_SCHEMA_VERSION) && !key.equals(K_MARKET)) {
                LOGGER.warn("Unknown top-level key in {}: {}", CONFIG_FILE_NAME, key);
            }
        }

        return new MinersMarketConfig(
                schemaVersion,
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
}
