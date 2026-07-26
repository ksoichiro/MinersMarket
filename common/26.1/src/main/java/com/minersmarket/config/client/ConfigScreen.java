package com.minersmarket.config.client;

import com.minersmarket.MinersMarket;
import com.minersmarket.config.ConfigDefaults;
import com.minersmarket.config.ConfigRanges;
import com.minersmarket.config.ConfigWriter;
import com.minersmarket.config.GameMode;
import com.minersmarket.config.MinersMarketConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.nio.file.Path;
import java.util.List;

public class ConfigScreen extends Screen {
    private static final int LIST_TOP = 32;
    private static final int FOOTER_HEIGHT = 36;
    private static final int ROW_WIDTH = 310;
    private static final int ITEM_HEIGHT = 25;
    private static final int WIDGET_WIDTH = 100;
    private static final int WIDGET_HEIGHT = 20;
    private static final int VALID_TEXT_COLOR = 0xFFE0E0E0;
    private static final int INVALID_TEXT_COLOR = 0xFFFF5555;
    private static final int HEADER_TEXT_COLOR = 0xFFFFFF55;
    private static final int NOTE_TEXT_COLOR = 0xFFAAAAAA;
    private static final int NOTE_WARN_COLOR = 0xFFFFAA00;

    private final Screen parent;
    private SettingsList list;
    private Button doneButton;

    private CycleEntry<GameMode> mode;
    private NumberEntry targetSales;
    private NumberEntry timeLimitSeconds;
    private NumberEntry countdownSeconds;
    private NumberEntry intervalSeconds;
    private NumberEntry durationMinSeconds;
    private NumberEntry durationMaxSeconds;
    private NumberEntry changeMinPercent;
    private NumberEntry changeMaxPercent;
    private CycleEntry<Boolean> scoreAlwaysShow;
    private NumberEntry scoreHideRemainingSeconds;
    private CycleEntry<Boolean> starterEnabled;
    private NumberEntry playerCount;
    private NumberEntry breadCount;
    private NumberEntry pickaxeFortuneLevel;
    private CycleEntry<Boolean> generateOnWorldLoad;

    public ConfigScreen(Screen parent) {
        super(Component.translatable("config.minersmarket.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        MinersMarketConfig config = MinersMarketConfig.get();
        this.list = new SettingsList(this.minecraft, this.width,
                this.height - LIST_TOP - FOOTER_HEIGHT, LIST_TOP, ITEM_HEIGHT);

        boolean joinedRemoteWorld = isJoinedRemoteWorld();
        this.list.addEntry(new HeaderEntry(this.font,
                Component.translatable(joinedRemoteWorld
                        ? "config.minersmarket.note_not_host"
                        : "config.minersmarket.note_local"),
                joinedRemoteWorld ? NOTE_WARN_COLOR : NOTE_TEXT_COLOR));

        this.list.addEntry(new HeaderEntry(this.font, Component.translatable("config.minersmarket.category.game")));
        this.mode = addModeRow("mode", config.game().mode());
        this.targetSales = addNumberRow("target_sales", String.valueOf(config.game().targetSales()),
                ConfigRanges.MIN_TARGET_SALES, ConfigRanges.MAX_TARGET_SALES);
        this.timeLimitSeconds = addNumberRow("time_limit_seconds", String.valueOf(config.game().timeLimitSeconds()),
                ConfigRanges.MIN_TIME_LIMIT_SECONDS, ConfigRanges.MAX_TIME_LIMIT_SECONDS);
        this.countdownSeconds = addNumberRow("countdown_seconds", String.valueOf(config.game().countdownSeconds()),
                ConfigRanges.MIN_COUNTDOWN_SECONDS, ConfigRanges.MAX_COUNTDOWN_SECONDS);

        this.list.addEntry(new HeaderEntry(this.font, Component.translatable("config.minersmarket.category.score_display")));
        this.scoreAlwaysShow = addToggleRow("score_always_show", config.scoreDisplay().alwaysShow());
        this.scoreHideRemainingSeconds = addNumberRow("score_hide_remaining_seconds",
                String.valueOf(config.scoreDisplay().hideRemainingSeconds()),
                ConfigRanges.MIN_HIDE_REMAINING_SECONDS, ConfigRanges.MAX_HIDE_REMAINING_SECONDS);

        this.list.addEntry(new HeaderEntry(this.font, Component.translatable("config.minersmarket.category.price_event")));
        this.intervalSeconds = addNumberRow("interval_seconds", String.valueOf(config.priceEvent().intervalSeconds()),
                ConfigRanges.MIN_INTERVAL_SECONDS, ConfigRanges.MAX_INTERVAL_SECONDS);
        this.durationMinSeconds = addNumberRow("duration_min_seconds", String.valueOf(config.priceEvent().durationMinSeconds()),
                ConfigRanges.MIN_DURATION_SECONDS, ConfigRanges.MAX_DURATION_SECONDS);
        this.durationMaxSeconds = addNumberRow("duration_max_seconds", String.valueOf(config.priceEvent().durationMaxSeconds()),
                ConfigRanges.MIN_DURATION_SECONDS, ConfigRanges.MAX_DURATION_SECONDS);
        this.changeMinPercent = addNumberRow("change_min_percent", String.valueOf(config.priceEvent().changeMinPercent()),
                ConfigRanges.MIN_CHANGE_PERCENT, ConfigRanges.MAX_CHANGE_PERCENT);
        this.changeMaxPercent = addNumberRow("change_max_percent", String.valueOf(config.priceEvent().changeMaxPercent()),
                ConfigRanges.MIN_CHANGE_PERCENT, ConfigRanges.MAX_CHANGE_PERCENT);

        this.list.addEntry(new HeaderEntry(this.font, Component.translatable("config.minersmarket.category.starter_items")));
        this.starterEnabled = addToggleRow("starter_enabled", config.starterItems().enabled());
        this.playerCount = addNumberRow("player_count", String.valueOf(config.starterItems().playerCount()),
                ConfigRanges.MIN_PLAYER_COUNT, ConfigRanges.MAX_PLAYER_COUNT);
        this.breadCount = addNumberRow("bread_count", String.valueOf(config.starterItems().breadCount()),
                ConfigRanges.MIN_BREAD_COUNT, ConfigRanges.MAX_BREAD_COUNT);
        this.pickaxeFortuneLevel = addNumberRow("pickaxe_fortune_level", String.valueOf(config.starterItems().pickaxeFortuneLevel()),
                ConfigRanges.MIN_FORTUNE_LEVEL, ConfigRanges.MAX_FORTUNE_LEVEL);

        this.list.addEntry(new HeaderEntry(this.font, Component.translatable("config.minersmarket.category.market")));
        this.generateOnWorldLoad = addToggleRow("generate_on_world_load", config.market().generateOnWorldLoad());

        this.addRenderableWidget(this.list);

        int buttonY = this.height - 28;
        this.addRenderableWidget(Button.builder(Component.translatable("config.minersmarket.reset"), b -> resetToDefaults())
                .bounds(this.width / 2 - 155, buttonY, 100, 20).build());
        this.addRenderableWidget(Button.builder(CommonComponents.GUI_CANCEL, b -> onClose())
                .bounds(this.width / 2 - 50, buttonY, 100, 20).build());
        this.doneButton = this.addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, b -> saveAndClose())
                .bounds(this.width / 2 + 55, buttonY, 100, 20).build());
        updateDoneButton();
    }

    private NumberEntry addNumberRow(String key, String initialValue, long min, long max) {
        NumberEntry entry = new NumberEntry(this.font,
                Component.translatable("config.minersmarket.option." + key),
                rangeTooltip("config.minersmarket.option." + key + ".tooltip", min, max),
                initialValue, min, max, this::updateDoneButton);
        this.list.addEntry(entry);
        return entry;
    }

    private CycleEntry<Boolean> addToggleRow(String key, boolean initialValue) {
        CycleEntry<Boolean> entry = CycleEntry.ofBoolean(this.font,
                Component.translatable("config.minersmarket.option." + key),
                Component.translatable("config.minersmarket.option." + key + ".tooltip"),
                initialValue);
        this.list.addEntry(entry);
        return entry;
    }

    private CycleEntry<GameMode> addModeRow(String key, GameMode initialValue) {
        CycleEntry<GameMode> entry = CycleEntry.ofGameMode(this.font,
                Component.translatable("config.minersmarket.option." + key),
                Component.translatable("config.minersmarket.option." + key + ".tooltip"),
                initialValue);
        this.list.addEntry(entry);
        return entry;
    }

    private static Component rangeTooltip(String tooltipKey, long min, long max) {
        MutableComponent range = (max == Integer.MAX_VALUE || max == Long.MAX_VALUE)
                ? Component.translatable("config.minersmarket.valid_min", min)
                : Component.translatable("config.minersmarket.valid_range", min, max);
        return Component.translatable(tooltipKey).append("\n").append(range);
    }

    // The config is read only by server-side logic from the local file, and nothing
    // syncs it over the network. On a world hosted by someone else these values are the
    // player's own and do not affect that session, so the notice must say which case
    // the player is in. Singleplayer and LAN hosting both keep an integrated server.
    private boolean isJoinedRemoteWorld() {
        return this.minecraft != null && this.minecraft.level != null
                && !this.minecraft.hasSingleplayerServer();
    }

    private void updateDoneButton() {
        if (this.doneButton != null) {
            this.doneButton.active = validate();
        }
    }

    private boolean validate() {
        boolean fieldsValid = this.targetSales.isValid()
                & this.timeLimitSeconds.isValid()
                & this.countdownSeconds.isValid()
                & this.intervalSeconds.isValid()
                & this.durationMinSeconds.isValid()
                & this.durationMaxSeconds.isValid()
                & this.changeMinPercent.isValid()
                & this.changeMaxPercent.isValid()
                & this.scoreHideRemainingSeconds.isValid()
                & this.playerCount.isValid()
                & this.breadCount.isValid()
                & this.pickaxeFortuneLevel.isValid();

        boolean durationsOk = !this.durationMinSeconds.isValid() || !this.durationMaxSeconds.isValid()
                || this.durationMinSeconds.longValue() <= this.durationMaxSeconds.longValue();
        this.durationMaxSeconds.markCrossFieldError(!durationsOk);
        boolean changesOk = !this.changeMinPercent.isValid() || !this.changeMaxPercent.isValid()
                || this.changeMinPercent.longValue() <= this.changeMaxPercent.longValue();
        this.changeMaxPercent.markCrossFieldError(!changesOk);

        return fieldsValid && durationsOk && changesOk;
    }

    private void saveAndClose() {
        if (!validate()) {
            return;
        }
        MinersMarketConfig config = new MinersMarketConfig(
                MinersMarketConfig.CURRENT_SCHEMA_VERSION,
                new MinersMarketConfig.Game(this.mode.getValue(), this.targetSales.longValue(),
                        this.timeLimitSeconds.intValue(), this.countdownSeconds.intValue()),
                new MinersMarketConfig.PriceEvent(
                        this.intervalSeconds.intValue(),
                        this.durationMinSeconds.intValue(),
                        this.durationMaxSeconds.intValue(),
                        this.changeMinPercent.intValue(),
                        this.changeMaxPercent.intValue()),
                new MinersMarketConfig.ScoreDisplay(
                        this.scoreAlwaysShow.getValue(),
                        this.scoreHideRemainingSeconds.intValue()),
                new MinersMarketConfig.StarterItems(
                        this.starterEnabled.getValue(),
                        this.playerCount.intValue(),
                        this.breadCount.intValue(),
                        this.pickaxeFortuneLevel.intValue()),
                new MinersMarketConfig.Market(this.generateOnWorldLoad.getValue())
        );
        MinersMarketConfig.set(config);
        Path configDir = MinersMarket.getConfigDir();
        if (configDir != null) {
            ConfigWriter.save(configDir, config);
        }
        onClose();
    }

    private void resetToDefaults() {
        MinersMarketConfig defaults = ConfigDefaults.defaults();
        this.mode.setValue(defaults.game().mode());
        this.targetSales.setValue(String.valueOf(defaults.game().targetSales()));
        this.timeLimitSeconds.setValue(String.valueOf(defaults.game().timeLimitSeconds()));
        this.countdownSeconds.setValue(String.valueOf(defaults.game().countdownSeconds()));
        this.intervalSeconds.setValue(String.valueOf(defaults.priceEvent().intervalSeconds()));
        this.durationMinSeconds.setValue(String.valueOf(defaults.priceEvent().durationMinSeconds()));
        this.durationMaxSeconds.setValue(String.valueOf(defaults.priceEvent().durationMaxSeconds()));
        this.changeMinPercent.setValue(String.valueOf(defaults.priceEvent().changeMinPercent()));
        this.changeMaxPercent.setValue(String.valueOf(defaults.priceEvent().changeMaxPercent()));
        this.scoreAlwaysShow.setValue(defaults.scoreDisplay().alwaysShow());
        this.scoreHideRemainingSeconds.setValue(String.valueOf(defaults.scoreDisplay().hideRemainingSeconds()));
        this.starterEnabled.setValue(defaults.starterItems().enabled());
        this.playerCount.setValue(String.valueOf(defaults.starterItems().playerCount()));
        this.breadCount.setValue(String.valueOf(defaults.starterItems().breadCount()));
        this.pickaxeFortuneLevel.setValue(String.valueOf(defaults.starterItems().pickaxeFortuneLevel()));
        this.generateOnWorldLoad.setValue(defaults.market().generateOnWorldLoad());
    }

    @Override
    public void resize(int width, int height) {
        // init() rebuilds every widget; snapshot unsaved edits so a window resize
        // does not silently discard them.
        String[] numbers = {
                this.targetSales.getValue(), this.timeLimitSeconds.getValue(),
                this.countdownSeconds.getValue(),
                this.intervalSeconds.getValue(), this.durationMinSeconds.getValue(),
                this.durationMaxSeconds.getValue(), this.changeMinPercent.getValue(),
                this.changeMaxPercent.getValue(), this.scoreHideRemainingSeconds.getValue(),
                this.playerCount.getValue(),
                this.breadCount.getValue(), this.pickaxeFortuneLevel.getValue()
        };
        GameMode selectedMode = this.mode.getValue();
        boolean scoreAlways = this.scoreAlwaysShow.getValue();
        boolean starter = this.starterEnabled.getValue();
        boolean generate = this.generateOnWorldLoad.getValue();
        super.resize(width, height);
        this.targetSales.setValue(numbers[0]);
        this.timeLimitSeconds.setValue(numbers[1]);
        this.countdownSeconds.setValue(numbers[2]);
        this.intervalSeconds.setValue(numbers[3]);
        this.durationMinSeconds.setValue(numbers[4]);
        this.durationMaxSeconds.setValue(numbers[5]);
        this.changeMinPercent.setValue(numbers[6]);
        this.changeMaxPercent.setValue(numbers[7]);
        this.scoreHideRemainingSeconds.setValue(numbers[8]);
        this.playerCount.setValue(numbers[9]);
        this.breadCount.setValue(numbers[10]);
        this.pickaxeFortuneLevel.setValue(numbers[11]);
        this.mode.setValue(selectedMode);
        this.scoreAlwaysShow.setValue(scoreAlways);
        this.starterEnabled.setValue(starter);
        this.generateOnWorldLoad.setValue(generate);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(guiGraphics, mouseX, mouseY, partialTick);
        guiGraphics.centeredText(this.font, this.title, this.width / 2, 12, 0xFFFFFFFF);
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(this.parent);
    }

    static class SettingsList extends ContainerObjectSelectionList<Entry> {
        SettingsList(Minecraft minecraft, int width, int height, int y, int itemHeight) {
            super(minecraft, width, height, y, itemHeight);
        }

        @Override
        public int getRowWidth() {
            return ROW_WIDTH;
        }

        @Override
        public int addEntry(ConfigScreen.Entry entry) {
            return super.addEntry(entry);
        }
    }

    abstract static class Entry extends ContainerObjectSelectionList.Entry<Entry> {
    }

    static class HeaderEntry extends Entry {
        private final Font font;
        private final Component label;
        private final int color;

        HeaderEntry(Font font, Component label) {
            this(font, label, HEADER_TEXT_COLOR);
        }

        HeaderEntry(Font font, Component label, int color) {
            this.font = font;
            this.label = label;
            this.color = color;
        }

        @Override
        public void extractContent(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, boolean hovering, float partialTick) {
            guiGraphics.centeredText(this.font, this.label, getX() + getWidth() / 2, getY() + 7, this.color);
        }

        @Override
        public List<? extends GuiEventListener> children() {
            return List.of();
        }

        @Override
        public List<? extends NarratableEntry> narratables() {
            return List.of();
        }
    }

    static class NumberEntry extends Entry {
        private final Font font;
        private final Component label;
        private final EditBox editBox;
        private final long min;
        private final long max;
        private boolean valid = true;
        private boolean crossFieldError;

        NumberEntry(Font font, Component label, Component tooltip, String initialValue,
                    long min, long max, Runnable onChanged) {
            this.font = font;
            this.label = label;
            this.min = min;
            this.max = max;
            this.editBox = new EditBox(font, 0, 0, WIDGET_WIDTH, WIDGET_HEIGHT, label);
            this.editBox.setMaxLength(19);
            this.editBox.setValue(initialValue);
            this.editBox.setTooltip(Tooltip.create(tooltip));
            this.editBox.setResponder(value -> {
                this.valid = parse(value) != null;
                updateColor();
                onChanged.run();
            });
        }

        private Long parse(String value) {
            try {
                long parsed = Long.parseLong(value.trim());
                return parsed >= this.min && parsed <= this.max ? parsed : null;
            } catch (NumberFormatException e) {
                return null;
            }
        }

        boolean isValid() {
            return this.valid;
        }

        long longValue() {
            Long parsed = parse(this.editBox.getValue());
            return parsed != null ? parsed : this.min;
        }

        int intValue() {
            return (int) longValue();
        }

        String getValue() {
            return this.editBox.getValue();
        }

        void setValue(String value) {
            this.editBox.setValue(value);
        }

        void markCrossFieldError(boolean error) {
            this.crossFieldError = error;
            updateColor();
        }

        private void updateColor() {
            this.editBox.setTextColor(this.valid && !this.crossFieldError ? VALID_TEXT_COLOR : INVALID_TEXT_COLOR);
        }

        @Override
        public void extractContent(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, boolean hovering, float partialTick) {
            guiGraphics.text(this.font, this.label, getX(), getY() + 6, 0xFFFFFFFF);
            this.editBox.setPosition(getX() + getWidth() - WIDGET_WIDTH, getY());
            this.editBox.extractRenderState(guiGraphics, mouseX, mouseY, partialTick);
        }

        @Override
        public List<? extends GuiEventListener> children() {
            return List.of(this.editBox);
        }

        @Override
        public List<? extends NarratableEntry> narratables() {
            return List.of(this.editBox);
        }
    }

    static class CycleEntry<T> extends Entry {
        private final Font font;
        private final Component label;
        private final CycleButton<T> button;

        static CycleEntry<Boolean> ofBoolean(Font font, Component label, Component tooltip, boolean initialValue) {
            return new CycleEntry<>(font, label,
                    CycleButton.onOffBuilder(initialValue).displayOnlyValue(), tooltip);
        }

        static CycleEntry<GameMode> ofGameMode(Font font, Component label, Component tooltip, GameMode initialValue) {
            // 26.1: CycleButton.builder() has no withInitialValue(); the initial
            // value is passed directly as the builder's second argument instead.
            return new CycleEntry<>(font, label,
                    CycleButton.builder((GameMode mode) ->
                                    Component.translatable("config.minersmarket.mode." + mode.id()), initialValue)
                            .withValues(GameMode.values())
                            .displayOnlyValue(),
                    tooltip);
        }

        private CycleEntry(Font font, Component label, CycleButton.Builder<T> builder, Component tooltip) {
            this.font = font;
            this.label = label;
            this.button = builder.create(0, 0, WIDGET_WIDTH, WIDGET_HEIGHT, label, (btn, value) -> {
            });
            this.button.setTooltip(Tooltip.create(tooltip));
        }

        T getValue() {
            return this.button.getValue();
        }

        void setValue(T value) {
            this.button.setValue(value);
        }

        @Override
        public void extractContent(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, boolean hovering, float partialTick) {
            guiGraphics.text(this.font, this.label, getX(), getY() + 6, 0xFFFFFFFF);
            this.button.setPosition(getX() + getWidth() - WIDGET_WIDTH, getY());
            this.button.extractRenderState(guiGraphics, mouseX, mouseY, partialTick);
        }

        @Override
        public List<? extends GuiEventListener> children() {
            return List.of(this.button);
        }

        @Override
        public List<? extends NarratableEntry> narratables() {
            return List.of(this.button);
        }
    }
}
