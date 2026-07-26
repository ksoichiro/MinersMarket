package com.minersmarket.config;

import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import com.electronwill.nightconfig.toml.TomlFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;

/**
 * Writes config values back to minersmarket.toml. Loads the existing file first and
 * only replaces values, so comments in the file are preserved.
 */
public final class ConfigWriter {
    private static final Logger LOGGER = LoggerFactory.getLogger(ConfigWriter.class);
    private static final String CONFIG_FILE_NAME = "minersmarket.toml";

    private ConfigWriter() {
    }

    public static boolean save(Path configDir, MinersMarketConfig config) {
        Path configFile = configDir.resolve(CONFIG_FILE_NAME);
        try (CommentedFileConfig fileConfig = CommentedFileConfig.builder(configFile, TomlFormat.instance()).build()) {
            fileConfig.load();
            fileConfig.set("schema_version", config.schemaVersion());
            fileConfig.set("game.target_sales", config.game().targetSales());
            fileConfig.set("game.mode", config.game().mode().id());
            fileConfig.set("game.time_limit_seconds", config.game().timeLimitSeconds());
            fileConfig.set("game.countdown_seconds", config.game().countdownSeconds());
            fileConfig.set("price_event.interval_seconds", config.priceEvent().intervalSeconds());
            fileConfig.set("price_event.duration_min_seconds", config.priceEvent().durationMinSeconds());
            fileConfig.set("price_event.duration_max_seconds", config.priceEvent().durationMaxSeconds());
            fileConfig.set("price_event.change_min_percent", config.priceEvent().changeMinPercent());
            fileConfig.set("price_event.change_max_percent", config.priceEvent().changeMaxPercent());
            fileConfig.set("score_display.always_show", config.scoreDisplay().alwaysShow());
            fileConfig.set("score_display.hide_remaining_seconds", config.scoreDisplay().hideRemainingSeconds());
            fileConfig.set("starter_items.enabled", config.starterItems().enabled());
            fileConfig.set("starter_items.player_count", config.starterItems().playerCount());
            fileConfig.set("starter_items.bread_count", config.starterItems().breadCount());
            fileConfig.set("starter_items.pickaxe_fortune_level", config.starterItems().pickaxeFortuneLevel());
            fileConfig.set("market.generate_on_world_load", config.market().generateOnWorldLoad());
            fileConfig.save();
            return true;
        } catch (RuntimeException e) {
            LOGGER.error("Failed to save {}", configFile, e);
            return false;
        }
    }
}
