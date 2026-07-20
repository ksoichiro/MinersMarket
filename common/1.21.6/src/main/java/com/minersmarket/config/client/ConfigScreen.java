package com.minersmarket.config.client;

import com.minersmarket.MinersMarket;
import com.minersmarket.config.ConfigDefaults;
import com.minersmarket.config.ConfigRanges;
import com.minersmarket.config.ConfigWriter;
import com.minersmarket.config.MinersMarketConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
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

    private final Screen parent;
    private SettingsList list;
    private Button doneButton;

    private NumberEntry targetSales;
    private NumberEntry countdownSeconds;
    private NumberEntry intervalSeconds;
    private NumberEntry durationMinSeconds;
    private NumberEntry durationMaxSeconds;
    private NumberEntry changeMinPercent;
    private NumberEntry changeMaxPercent;
    private ToggleEntry starterEnabled;
    private NumberEntry playerCount;
    private NumberEntry breadCount;
    private NumberEntry pickaxeFortuneLevel;
    private ToggleEntry generateOnWorldLoad;

    public ConfigScreen(Screen parent) {
        super(Component.translatable("config.minersmarket.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        MinersMarketConfig config = MinersMarketConfig.get();
        this.list = new SettingsList(this.minecraft, this.width,
                this.height - LIST_TOP - FOOTER_HEIGHT, LIST_TOP, ITEM_HEIGHT);

        this.list.addEntry(new HeaderEntry(this.font, Component.translatable("config.minersmarket.category.game")));
        this.targetSales = addNumberRow("target_sales", String.valueOf(config.game().targetSales()),
                ConfigRanges.MIN_TARGET_SALES, ConfigRanges.MAX_TARGET_SALES);
        this.countdownSeconds = addNumberRow("countdown_seconds", String.valueOf(config.game().countdownSeconds()),
                ConfigRanges.MIN_COUNTDOWN_SECONDS, ConfigRanges.MAX_COUNTDOWN_SECONDS);

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

    private ToggleEntry addToggleRow(String key, boolean initialValue) {
        ToggleEntry entry = new ToggleEntry(this.font,
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

    private void updateDoneButton() {
        if (this.doneButton != null) {
            this.doneButton.active = validate();
        }
    }

    private boolean validate() {
        boolean fieldsValid = this.targetSales.isValid()
                & this.countdownSeconds.isValid()
                & this.intervalSeconds.isValid()
                & this.durationMinSeconds.isValid()
                & this.durationMaxSeconds.isValid()
                & this.changeMinPercent.isValid()
                & this.changeMaxPercent.isValid()
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
                new MinersMarketConfig.Game(this.targetSales.longValue(), this.countdownSeconds.intValue()),
                new MinersMarketConfig.PriceEvent(
                        this.intervalSeconds.intValue(),
                        this.durationMinSeconds.intValue(),
                        this.durationMaxSeconds.intValue(),
                        this.changeMinPercent.intValue(),
                        this.changeMaxPercent.intValue()),
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
        this.targetSales.setValue(String.valueOf(defaults.game().targetSales()));
        this.countdownSeconds.setValue(String.valueOf(defaults.game().countdownSeconds()));
        this.intervalSeconds.setValue(String.valueOf(defaults.priceEvent().intervalSeconds()));
        this.durationMinSeconds.setValue(String.valueOf(defaults.priceEvent().durationMinSeconds()));
        this.durationMaxSeconds.setValue(String.valueOf(defaults.priceEvent().durationMaxSeconds()));
        this.changeMinPercent.setValue(String.valueOf(defaults.priceEvent().changeMinPercent()));
        this.changeMaxPercent.setValue(String.valueOf(defaults.priceEvent().changeMaxPercent()));
        this.starterEnabled.setValue(defaults.starterItems().enabled());
        this.playerCount.setValue(String.valueOf(defaults.starterItems().playerCount()));
        this.breadCount.setValue(String.valueOf(defaults.starterItems().breadCount()));
        this.pickaxeFortuneLevel.setValue(String.valueOf(defaults.starterItems().pickaxeFortuneLevel()));
        this.generateOnWorldLoad.setValue(defaults.market().generateOnWorldLoad());
    }

    @Override
    public void resize(Minecraft minecraft, int width, int height) {
        // init() rebuilds every widget; snapshot unsaved edits so a window resize
        // does not silently discard them.
        String[] numbers = {
                this.targetSales.getValue(), this.countdownSeconds.getValue(),
                this.intervalSeconds.getValue(), this.durationMinSeconds.getValue(),
                this.durationMaxSeconds.getValue(), this.changeMinPercent.getValue(),
                this.changeMaxPercent.getValue(), this.playerCount.getValue(),
                this.breadCount.getValue(), this.pickaxeFortuneLevel.getValue()
        };
        boolean starter = this.starterEnabled.getValue();
        boolean generate = this.generateOnWorldLoad.getValue();
        super.resize(minecraft, width, height);
        this.targetSales.setValue(numbers[0]);
        this.countdownSeconds.setValue(numbers[1]);
        this.intervalSeconds.setValue(numbers[2]);
        this.durationMinSeconds.setValue(numbers[3]);
        this.durationMaxSeconds.setValue(numbers[4]);
        this.changeMinPercent.setValue(numbers[5]);
        this.changeMaxPercent.setValue(numbers[6]);
        this.playerCount.setValue(numbers[7]);
        this.breadCount.setValue(numbers[8]);
        this.pickaxeFortuneLevel.setValue(numbers[9]);
        this.starterEnabled.setValue(starter);
        this.generateOnWorldLoad.setValue(generate);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, 12, 0xFFFFFFFF);
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

        HeaderEntry(Font font, Component label) {
            this.font = font;
            this.label = label;
        }

        @Override
        public void render(GuiGraphics guiGraphics, int index, int top, int left, int width, int height,
                           int mouseX, int mouseY, boolean hovering, float partialTick) {
            guiGraphics.drawCenteredString(this.font, this.label, left + width / 2, top + 7, 0xFFFFFF55);
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
        public void render(GuiGraphics guiGraphics, int index, int top, int left, int width, int height,
                           int mouseX, int mouseY, boolean hovering, float partialTick) {
            guiGraphics.drawString(this.font, this.label, left, top + 6, 0xFFFFFFFF);
            this.editBox.setPosition(left + width - WIDGET_WIDTH, top);
            this.editBox.render(guiGraphics, mouseX, mouseY, partialTick);
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

    static class ToggleEntry extends Entry {
        private final Font font;
        private final Component label;
        private final CycleButton<Boolean> button;

        ToggleEntry(Font font, Component label, Component tooltip, boolean initialValue) {
            this.font = font;
            this.label = label;
            this.button = CycleButton.onOffBuilder(initialValue)
                    .displayOnlyValue()
                    .create(0, 0, WIDGET_WIDTH, WIDGET_HEIGHT, label, (btn, value) -> {
                    });
            this.button.setTooltip(Tooltip.create(tooltip));
        }

        boolean getValue() {
            return this.button.getValue();
        }

        void setValue(boolean value) {
            this.button.setValue(value);
        }

        @Override
        public void render(GuiGraphics guiGraphics, int index, int top, int left, int width, int height,
                           int mouseX, int mouseY, boolean hovering, float partialTick) {
            guiGraphics.drawString(this.font, this.label, left, top + 6, 0xFFFFFFFF);
            this.button.setPosition(left + width - WIDGET_WIDTH, top);
            this.button.render(guiGraphics, mouseX, mouseY, partialTick);
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
