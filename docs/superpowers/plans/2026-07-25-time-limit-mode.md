# Time-Limit Game Mode Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a configurable time-limit game mode in which the player with the highest sales when the timer expires wins, with configurable score visibility.

**Architecture:** A new `game.mode` config key selects between the existing `target` rules and new `time_limit` rules. The mode and the time limit are snapshotted into `GameStateSavedData` when a game starts, so mid-game config edits cannot corrupt a running game. `GameStateManager` owns the timer, the ranking, and the decision of whether the ranking may be seen; `GameStateSyncPacket` omits the ranking entirely when it must be hidden, so hidden scores never reach any client. The HUD branches on mode and leaves `target` mode rendering untouched.

**Tech Stack:** Java 17/21/25, Minecraft 1.20.1–1.21.11 and 26.x, Fabric Loom + ModDevGradle, night-config (TOML), Mojang official mappings.

**Spec:** `docs/superpowers/specs/2026-07-25-time-limit-mode-design.md`

## Global Constraints

- **No test framework exists in this project.** The mod modules have no test source sets and builds run with `-x test` (`CLAUDE.md`). Adding JUnit would mean new source sets across 15 version modules for a feature whose logic is entangled with Minecraft types. This was considered and rejected as disproportionate. **Verification for every task is: the module compiles, plus the manual in-game checks listed in that task.** Do not claim a task passes without running the build command and reading its output.
- **Canonical version is `1.21.1`.** Implement and verify everything there first (Tasks 1–5), then propagate (Tasks 6–11).
- `common/shared/` sources are compiled into **both** `1.20.1` and `1.21.1`. Editing a shared file makes the `1.20.1` build red until Task 7 lands. This is expected and called out where it happens.
- **Bash hook rules on this machine** (`~/.claude/hooks/`): no pipes or `;`/`&&` chains without a trailing `#allow-compound`; no `git -C`. Gradle needs `dangerouslyDisableSandbox: true` (the JDK comes from `mise` and the sandbox blocks `libjli.dylib`), and its output must be redirected to a **fixed hard-coded log path** — not `$TMPDIR`, which resolves differently inside and outside the sandbox.
- **Build command used throughout this plan:**
  ```
  ./gradlew build -Ptarget_mc_version=<VERSION> > /tmp/minersmarket-build/build.log 2>&1 #allow-direct
  ```
  run with `dangerouslyDisableSandbox: true`, then inspect with a separate call:
  ```
  grep -E "^BUILD|error:|FAILED" /tmp/minersmarket-build/build.log
  ```
  Create `/tmp/minersmarket-build/` once with `mkdir -p` before the first build.
- Config default values, fixed by the spec: `mode = "target"`, `time_limit_seconds = 1800`, `always_show = false`, `hide_remaining_seconds = 300`. `CURRENT_SCHEMA_VERSION` becomes `2`.
- Comments in code are English (project policy). Explain *why* only where the reason is non-obvious.
- Do not commit unless the user asks. Each task's final step stages the files and states the commit message; run `git commit` only on the user's instruction.

---

## File Structure

### New files (identical content in every module that gets a copy)

| File | Responsibility |
|---|---|
| `config/GameMode.java` | `TARGET` / `TIME_LIMIT` enum plus `id()` / `fromId(String)` for the TOML string form. |
| `state/RankedPlayer.java` | `record RankedPlayer(int rank, String playerName, long salesAmount)` — one row of a score ranking. Used by the server, the packet, and the client. Plain Java, no Minecraft imports, so one text works everywhere. |

### Modified files, grouped by identical content

Version modules that currently hold **byte-identical** copies of a file must receive byte-identical copies afterwards. Verified 2026-07-25:

| File | Identical-content groups |
|---|---|
| `config/MinersMarketConfig.java`, `config/ConfigDefaults.java`, `config/ConfigRanges.java`, `config/ConfigLoader.java`, `config/ConfigWriter.java` | **all 15 versions** |
| `resources/minersmarket-default-config.toml` | **all 15 versions** |
| `lang/en_us.json`, `lang/ja_jp.json` | (a) 1.20.1, 1.21.1, 1.21.3–1.21.8 (b) 1.21.9, 1.21.10, 1.21.11, 26.1, 26.1.1, 26.1.2, 26.2 — group (b) has one extra key, `key.category.minersmarket.main` |
| `config/client/ConfigScreen.java` | (a) 1.20.1 (b) 1.21.1, 1.21.3–1.21.8 (c) 1.21.9, 1.21.10 (d) 1.21.11 (e) 26.1, 26.1.1, 26.1.2 (f) 26.2 |
| `state/GameStateSavedData.java` | (a) 1.20.1 (b) 1.21.1, 1.21.3, 1.21.4 (c) 1.21.5–1.21.11, 26.x |
| `state/GameStateManager.java` | (a) 1.20.1 (b) 1.21.1 (c) 1.21.3, 1.21.4 (d) 1.21.5–1.21.10 (e) 1.21.11 (f) 26.x |
| `network/GameStateSyncPacket.java` | (a) 1.20.1 (b) 1.21.1 (c) 1.21.3–1.21.10 (d) 1.21.11, 26.x |
| `hud/GameHudOverlay.java` | (a) 1.20.1 (b) 1.21.1 (c) 1.21.3–1.21.5 (d) 1.21.6–1.21.10 (e) 1.21.11 (f) 26.x |
| `entity/MerchantEntity.java` | (a) 1.20.1 (b) 1.21.1 (c) 1.21.3, 1.21.4, 1.21.6–1.21.10 (d) 1.21.5 (e) 1.21.11 (f) 26.x |
| `state/ClientGameState.java` | `common/shared` (serves 1.20.1 + 1.21.1); one further group covering 1.21.3–26.2 |

`state/GameState.java`, `state/FinishedPlayer.java` and `event/GameTickHandler.java` need **no changes**.

### Known per-version API deltas (for the propagation tasks)

| Where | 1.20.1 | 1.21.1–1.21.4 | 1.21.5+ / 26.x |
|---|---|---|---|
| `GameStateSavedData` persistence | `load(CompoundTag)` / `save(CompoundTag)` | `load(CompoundTag, HolderLookup.Provider)` / `save(CompoundTag, …)` | **`Codec` (`RecordCodecBuilder`)**, no `CompoundTag` |
| HUD colours | RGB, no alpha (`0xFFD700`) | RGB, no alpha | **1.21.6+ requires full alpha** (`0xFFFFD700`) |
| HUD draw call | `graphics.drawString(...)` | `graphics.drawString(...)` | **26.x: `graphics.text(...)` on `GuiGraphicsExtractor`** |
| `ConfigScreen` entry render | `render(GuiGraphics, index, top, left, width, height, …)` | same | **1.21.9+: `renderContent(GuiGraphics, mouseX, mouseY, hovering, partialTick)` using `getX()/getY()/getWidth()`** |
| `ConfigScreen.resize` | `resize(Minecraft, int, int)` | same | `resize(Minecraft, int, int)` through **1.21.10**; **`resize(int, int)` only from 1.21.11 and on 26.x** |

| `CycleButton` builder | `CycleButton.<T>builder(fn).withValues(…).withInitialValue(v)` | same | **1.21.11+: `withInitialValue` is gone — pass the initial value as `builder(fn, v)`'s second argument** |

> Added during execution: the `CycleButton` row was not in the original table and cost a
> compile failure on 1.21.11. Confirm which form each 26.x module needs before editing it.

> Corrected during execution: an earlier draft of this table claimed `resize(int, int)`
> from 1.21.9. It is not — 1.21.9 and 1.21.10 keep the `Minecraft` parameter, and only
> 1.21.11 and 26.x drop it. Verified against the files at `3d08ad0`. The entry-render
> rename to `renderContent` *does* start at 1.21.9, so the two boundaries differ.

**Rule for every propagation task: change only the lines this feature needs, and keep each file's existing per-version idioms exactly as they are.**

---

## Task 1: Config layer — new keys, records, loader, writer

**Files:**
- Create: `common/1.21.1/src/main/java/com/minersmarket/config/GameMode.java`
- Modify: `common/1.21.1/src/main/java/com/minersmarket/config/MinersMarketConfig.java`
- Modify: `common/1.21.1/src/main/java/com/minersmarket/config/ConfigDefaults.java`
- Modify: `common/1.21.1/src/main/java/com/minersmarket/config/ConfigRanges.java`
- Modify: `common/1.21.1/src/main/java/com/minersmarket/config/ConfigLoader.java`
- Modify: `common/1.21.1/src/main/java/com/minersmarket/config/ConfigWriter.java`
- Modify: `common/1.21.1/src/main/resources/minersmarket-default-config.toml`

**Interfaces:**
- Produces: `GameMode.TARGET` / `GameMode.TIME_LIMIT`, `GameMode.id()` → `"target"` / `"time_limit"`, `GameMode.fromId(String)` → `GameMode` or `null`; `MinersMarketConfig.Game(GameMode mode, long targetSales, int timeLimitSeconds, int countdownSeconds)`; `MinersMarketConfig.ScoreDisplay(boolean alwaysShow, int hideRemainingSeconds)`; `MinersMarketConfig.scoreDisplay()`; `ConfigDefaults.GAME_MODE`, `.GAME_TIME_LIMIT_SECONDS`, `.SCORE_DISPLAY_ALWAYS_SHOW`, `.SCORE_DISPLAY_HIDE_REMAINING_SECONDS`; `ConfigRanges.MIN/MAX_TIME_LIMIT_SECONDS`, `.MIN/MAX_HIDE_REMAINING_SECONDS`.

> **Note:** changing the `Game` record signature breaks `ConfigScreen`, which is fixed in Task 2. The module will not compile until then — that is why Task 1's verification is limited to a review of the written files, and Task 2 carries the first build.

- [ ] **Step 1: Create `GameMode`**

```java
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
```

- [ ] **Step 2: Update `MinersMarketConfig`**

Replace the whole file body with:

```java
package com.minersmarket.config;

public record MinersMarketConfig(
        int schemaVersion,
        Game game,
        PriceEvent priceEvent,
        ScoreDisplay scoreDisplay,
        StarterItems starterItems,
        Market market
) {
    public static final int CURRENT_SCHEMA_VERSION = 2;

    private static volatile MinersMarketConfig INSTANCE = ConfigDefaults.defaults();

    public static MinersMarketConfig get() {
        return INSTANCE;
    }

    public static void set(MinersMarketConfig config) {
        INSTANCE = config;
    }

    public record Game(GameMode mode, long targetSales, int timeLimitSeconds, int countdownSeconds) {
    }

    public record PriceEvent(
            int intervalSeconds,
            int durationMinSeconds,
            int durationMaxSeconds,
            int changeMinPercent,
            int changeMaxPercent
    ) {
    }

    public record ScoreDisplay(boolean alwaysShow, int hideRemainingSeconds) {
    }

    public record StarterItems(
            boolean enabled,
            int playerCount,
            int breadCount,
            int pickaxeFortuneLevel
    ) {
    }

    public record Market(boolean generateOnWorldLoad) {
    }
}
```

- [ ] **Step 3: Update `ConfigDefaults`**

Add these constants directly below `GAME_COUNTDOWN_SECONDS`:

```java
    public static final GameMode GAME_MODE = GameMode.TARGET;
    public static final int GAME_TIME_LIMIT_SECONDS = 1800;

    public static final boolean SCORE_DISPLAY_ALWAYS_SHOW = false;
    public static final int SCORE_DISPLAY_HIDE_REMAINING_SECONDS = 300;
```

and rewrite `defaults()` as:

```java
    public static MinersMarketConfig defaults() {
        return new MinersMarketConfig(
                MinersMarketConfig.CURRENT_SCHEMA_VERSION,
                new MinersMarketConfig.Game(GAME_MODE, GAME_TARGET_SALES, GAME_TIME_LIMIT_SECONDS,
                        GAME_COUNTDOWN_SECONDS),
                new MinersMarketConfig.PriceEvent(
                        PRICE_EVENT_INTERVAL_SECONDS,
                        PRICE_EVENT_DURATION_MIN_SECONDS,
                        PRICE_EVENT_DURATION_MAX_SECONDS,
                        PRICE_EVENT_CHANGE_MIN_PERCENT,
                        PRICE_EVENT_CHANGE_MAX_PERCENT
                ),
                new MinersMarketConfig.ScoreDisplay(
                        SCORE_DISPLAY_ALWAYS_SHOW,
                        SCORE_DISPLAY_HIDE_REMAINING_SECONDS
                ),
                new MinersMarketConfig.StarterItems(
                        STARTER_ITEMS_ENABLED,
                        STARTER_ITEMS_PLAYER_COUNT,
                        STARTER_ITEMS_BREAD_COUNT,
                        STARTER_ITEMS_PICKAXE_FORTUNE_LEVEL
                ),
                new MinersMarketConfig.Market(MARKET_GENERATE_ON_WORLD_LOAD)
        );
    }
```

- [ ] **Step 4: Update `ConfigRanges`**

Add below `MAX_COUNTDOWN_SECONDS`:

```java
    // Durations are converted to ticks and held in an int, so the upper bound is
    // divided by the tick rate to keep seconds * 20 from overflowing.
    public static final int MIN_TIME_LIMIT_SECONDS = 1;
    public static final int MAX_TIME_LIMIT_SECONDS = Integer.MAX_VALUE / 20;
    public static final int MIN_HIDE_REMAINING_SECONDS = 0;
    public static final int MAX_HIDE_REMAINING_SECONDS = Integer.MAX_VALUE / 20;
```

- [ ] **Step 5: Update `ConfigLoader` — key constants**

Add next to the existing `K_` constants:

```java
    private static final String K_MODE = "mode";
    private static final String K_TIME_LIMIT_SECONDS = "time_limit_seconds";
    private static final String K_SCORE_DISPLAY = "score_display";
    private static final String K_ALWAYS_SHOW = "always_show";
    private static final String K_HIDE_REMAINING_SECONDS = "hide_remaining_seconds";
```

- [ ] **Step 6: Update `ConfigLoader` — read the new values**

Insert immediately after the existing `countdownSeconds` block in `parseOrDefaults`:

```java
        GameMode mode = readGameMode(parsed, K_GAME + "." + K_MODE, ConfigDefaults.GAME_MODE);
        int timeLimitSeconds = readInt(
                parsed,
                K_GAME + "." + K_TIME_LIMIT_SECONDS,
                ConfigDefaults.GAME_TIME_LIMIT_SECONDS,
                ConfigRanges.MIN_TIME_LIMIT_SECONDS,
                ConfigRanges.MAX_TIME_LIMIT_SECONDS
        );

        boolean scoreAlwaysShow = readBoolean(
                parsed,
                K_SCORE_DISPLAY + "." + K_ALWAYS_SHOW,
                ConfigDefaults.SCORE_DISPLAY_ALWAYS_SHOW
        );
        int scoreHideRemainingSeconds = readInt(
                parsed,
                K_SCORE_DISPLAY + "." + K_HIDE_REMAINING_SECONDS,
                ConfigDefaults.SCORE_DISPLAY_HIDE_REMAINING_SECONDS,
                ConfigRanges.MIN_HIDE_REMAINING_SECONDS,
                ConfigRanges.MAX_HIDE_REMAINING_SECONDS
        );
```

Add this reader next to `readBoolean`:

```java
    private static GameMode readGameMode(CommentedConfig parsed, String path, GameMode defaultValue) {
        Object value = parsed.get(path);
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof String string) {
            GameMode mode = GameMode.fromId(string.trim());
            if (mode != null) {
                return mode;
            }
        }
        LOGGER.error("Invalid {}.{} = {} (must be \"{}\" or \"{}\"); using default {}",
                CONFIG_FILE_NAME, path, value,
                GameMode.TARGET.id(), GameMode.TIME_LIMIT.id(), defaultValue.id());
        return defaultValue;
    }
```

- [ ] **Step 7: Update `ConfigLoader` — unknown-key check and return value**

In the unknown-top-level-key loop, add `K_SCORE_DISPLAY` to the accepted set:

```java
            if (!key.equals(K_SCHEMA_VERSION) && !key.equals(K_GAME) && !key.equals(K_PRICE_EVENT)
                    && !key.equals(K_SCORE_DISPLAY)
                    && !key.equals(K_STARTER_ITEMS) && !key.equals(K_MARKET)) {
```

and update the returned record:

```java
        return new MinersMarketConfig(
                schemaVersion,
                new MinersMarketConfig.Game(mode, targetSales, timeLimitSeconds, countdownSeconds),
                new MinersMarketConfig.PriceEvent(
                        intervalSeconds,
                        durationMinSeconds,
                        durationMaxSeconds,
                        changeMinPercent,
                        changeMaxPercent
                ),
                new MinersMarketConfig.ScoreDisplay(scoreAlwaysShow, scoreHideRemainingSeconds),
                new MinersMarketConfig.StarterItems(
                        starterEnabled,
                        playerCount,
                        breadCount,
                        pickaxeFortuneLevel
                ),
                new MinersMarketConfig.Market(generateOnWorldLoad)
        );
```

- [ ] **Step 8: Update `ConfigWriter`**

Add these four `set` calls in `save`, keeping the existing ordering style (game keys after `game.target_sales`, the score block after the price-event block):

```java
            fileConfig.set("game.mode", config.game().mode().id());
            fileConfig.set("game.time_limit_seconds", config.game().timeLimitSeconds());
            fileConfig.set("score_display.always_show", config.scoreDisplay().alwaysShow());
            fileConfig.set("score_display.hide_remaining_seconds", config.scoreDisplay().hideRemainingSeconds());
```

- [ ] **Step 9: Update the bundled default TOML**

Set `schema_version = 2`, and edit the `[game]` block and insert `[score_display]` so the file reads:

```toml
schema_version = 2

[game]
# Win condition. "target" = first player to reach target_sales wins.
# "time_limit" = the highest sales amount when time_limit_seconds runs out wins.
mode = "target"
# Sales amount required for a player to finish the game ("target" mode only).
target_sales = 10000
# Length of a game in seconds ("time_limit" mode only).
time_limit_seconds = 1800
# Countdown length in seconds before the game starts.
countdown_seconds = 5

[price_event]
# ... unchanged ...

[score_display]
# "time_limit" mode only.
# If true, every player's sales amount is shown on the HUD during the game.
# If false, the totals are revealed only when the time limit expires.
always_show = false
# With always_show = true, hide the ranking once this many seconds remain,
# so the finish is a surprise. 0 keeps it visible to the end.
hide_remaining_seconds = 300
```

Leave `[price_event]`, `[starter_items]` and `[market]` exactly as they are.

- [ ] **Step 10: Stage the files**

```bash
git add common/1.21.1/src/main/java/com/minersmarket/config/ common/1.21.1/src/main/resources/minersmarket-default-config.toml
```

Commit message (run only when the user asks): `feat: add game mode and score display config keys`

---

## Task 2: Config screen rows for the new keys

**Files:**
- Modify: `common/1.21.1/src/main/java/com/minersmarket/config/client/ConfigScreen.java`
- Modify: `common/1.21.1/src/main/resources/assets/minersmarket/lang/en_us.json`
- Modify: `common/1.21.1/src/main/resources/assets/minersmarket/lang/ja_jp.json`

**Interfaces:**
- Consumes: everything Task 1 produced.
- Produces: nothing consumed by later tasks; this is the first task whose build must pass.

The existing `ToggleEntry` wraps a `CycleButton<Boolean>`. The mode row needs a `CycleButton<GameMode>` with the same layout, so `ToggleEntry` becomes a generic `CycleEntry<T>` rather than gaining a near-duplicate sibling. That keeps **one** entry-render method to adapt per version instead of two.

- [ ] **Step 1: Replace `ToggleEntry` with a generic `CycleEntry`**

Replace the whole `static class ToggleEntry` block with:

```java
    static class CycleEntry<T> extends Entry {
        private final Font font;
        private final Component label;
        private final CycleButton<T> button;

        static CycleEntry<Boolean> ofBoolean(Font font, Component label, Component tooltip, boolean initialValue) {
            return new CycleEntry<>(font, label,
                    CycleButton.onOffBuilder(initialValue).displayOnlyValue(), tooltip);
        }

        static CycleEntry<GameMode> ofGameMode(Font font, Component label, Component tooltip, GameMode initialValue) {
            return new CycleEntry<>(font, label,
                    CycleButton.<GameMode>builder(mode ->
                                    Component.translatable("config.minersmarket.mode." + mode.id()))
                            .withValues(GameMode.values())
                            .withInitialValue(initialValue)
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
```

Add `import com.minersmarket.config.GameMode;` to the imports.

- [ ] **Step 2: Update the field declarations**

Replace the `ToggleEntry starterEnabled;` / `ToggleEntry generateOnWorldLoad;` declarations and add the new fields, so the block reads:

```java
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
```

- [ ] **Step 3: Update the row helpers**

Replace `addToggleRow` and add `addModeRow`:

```java
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
```

- [ ] **Step 4: Add the rows in `init()`**

Replace the Game category block with:

```java
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
```

Leave the Price Event, Starter Items and Market blocks unchanged; they follow this one.

- [ ] **Step 5: Update `validate()`**

Add the two new numeric fields to the `fieldsValid` chain (keep the single-`&` style, which deliberately evaluates every field so all of them get recoloured):

```java
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
```

Do not add a cross-field rule between `scoreHideRemainingSeconds` and `timeLimitSeconds`: a threshold larger than the limit legitimately means "hidden for the whole game".

- [ ] **Step 6: Update `saveAndClose()`**

```java
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
```

- [ ] **Step 7: Update `resetToDefaults()`**

Add these lines alongside the existing ones:

```java
        this.mode.setValue(defaults.game().mode());
        this.timeLimitSeconds.setValue(String.valueOf(defaults.game().timeLimitSeconds()));
        this.scoreAlwaysShow.setValue(defaults.scoreDisplay().alwaysShow());
        this.scoreHideRemainingSeconds.setValue(String.valueOf(defaults.scoreDisplay().hideRemainingSeconds()));
```

- [ ] **Step 8: Update `resize()`**

Extend the snapshot so unsaved edits to the new rows survive a window resize:

```java
    @Override
    public void resize(Minecraft minecraft, int width, int height) {
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
        super.resize(minecraft, width, height);
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
```

- [ ] **Step 9: Add the English strings**

Insert into `en_us.json`, keeping the existing grouping (config keys together, before `config.minersmarket.valid_range`):

```json
  "config.minersmarket.category.score_display": "Score Display",
  "config.minersmarket.option.mode": "Game Mode",
  "config.minersmarket.option.mode.tooltip": "Target: first player to reach the target sales amount wins. Time limit: the highest sales amount when time runs out wins.",
  "config.minersmarket.mode.target": "Target Amount",
  "config.minersmarket.mode.time_limit": "Time Limit",
  "config.minersmarket.option.time_limit_seconds": "Time Limit Seconds",
  "config.minersmarket.option.time_limit_seconds.tooltip": "Length of a game in seconds. Time limit mode only.",
  "config.minersmarket.option.score_always_show": "Always Show Scores",
  "config.minersmarket.option.score_always_show.tooltip": "If ON, every player's sales amount is shown during the game. If OFF, totals are revealed only when time runs out. Time limit mode only.",
  "config.minersmarket.option.score_hide_remaining_seconds": "Hide Below Remaining",
  "config.minersmarket.option.score_hide_remaining_seconds.tooltip": "With Always Show Scores ON, hide the ranking once this many seconds remain. 0 keeps it visible to the end.",
```

Also add the messages used later by Task 4 so the language files are touched once:

```json
  "message.minersmarket.time_up_title": "Time Up!",
  "message.minersmarket.time_up_subtitle": "%s wins!",
  "message.minersmarket.time_up_no_winner": "Nobody sold anything.",
  "message.minersmarket.game_ended_time_limit": "The game has ended.",
  "message.minersmarket.final_ranking_header": "Final results:",
  "message.minersmarket.final_ranking_entry": "#%1$s %2$s  %3$s gold",
```

- [ ] **Step 10: Add the Japanese strings**

Insert the matching keys into `ja_jp.json` in the same positions:

```json
  "config.minersmarket.category.score_display": "スコア表示",
  "config.minersmarket.option.mode": "ゲームモード",
  "config.minersmarket.option.mode.tooltip": "目標金額: 目標金額に最初に到達したプレイヤーの勝ち。時間制限: 制限時間の終了時に最も稼いでいたプレイヤーの勝ち。",
  "config.minersmarket.mode.target": "目標金額",
  "config.minersmarket.mode.time_limit": "時間制限",
  "config.minersmarket.option.time_limit_seconds": "制限時間（秒）",
  "config.minersmarket.option.time_limit_seconds.tooltip": "1 ゲームの長さ（秒）。時間制限モードでのみ使用します。",
  "config.minersmarket.option.score_always_show": "スコアを常時表示",
  "config.minersmarket.option.score_always_show.tooltip": "ON にすると全員の売上金額をゲーム中も表示します。OFF の場合は時間切れ時にまとめて発表します。時間制限モードでのみ使用します。",
  "config.minersmarket.option.score_hide_remaining_seconds": "非表示にする残り時間",
  "config.minersmarket.option.score_hide_remaining_seconds.tooltip": "スコアを常時表示が ON のとき、残りがこの秒数を切ったらランキングを隠します。0 なら最後まで表示します。",
  "message.minersmarket.time_up_title": "タイムアップ！",
  "message.minersmarket.time_up_subtitle": "%s の勝利！",
  "message.minersmarket.time_up_no_winner": "誰も売却しませんでした。",
  "message.minersmarket.game_ended_time_limit": "ゲームは終了しました。",
  "message.minersmarket.final_ranking_header": "最終結果:",
  "message.minersmarket.final_ranking_entry": "#%1$s %2$s  %3$s ゴールド",
```

- [ ] **Step 11: Build**

```
mkdir -p /tmp/minersmarket-build
```
then, with `dangerouslyDisableSandbox: true`:
```
./gradlew build -Ptarget_mc_version=1.21.1 > /tmp/minersmarket-build/build.log 2>&1 #allow-direct
```
then in a separate call:
```
grep -E "^BUILD|error:|FAILED" /tmp/minersmarket-build/build.log
```
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 12: Manual check**

Launch the 1.21.1 Fabric client (`./gradlew :fabric:runClient -Ptarget_mc_version=1.21.1`), open the config screen via the "Open Config Screen" key binding, and confirm:
- a "Game Mode" row cycles between "Target Amount" and "Time Limit";
- "Time Limit Seconds" shows `1800`;
- a "Score Display" section shows "Always Show Scores" = OFF and "Hide Below Remaining" = `300`;
- pressing Done writes `mode`, `time_limit_seconds` and a `[score_display]` table into `run/config/minersmarket.toml`, and the comments in that file are preserved.

The last point is the one worth checking carefully: `ConfigWriter` writes into a file that may predate the `[score_display]` table, so confirm night-config created the missing table rather than dropping the values.

- [ ] **Step 13: Stage the files**

```bash
git add common/1.21.1/src/main/java/com/minersmarket/config/client/ConfigScreen.java common/1.21.1/src/main/resources/assets/minersmarket/lang/
```

Commit message: `feat: add game mode and score display rows to the config screen`

---

## Task 3: Game state — mode snapshot, timer, ranking

**Files:**
- Create: `common/shared/src/main/java/com/minersmarket/state/RankedPlayer.java`
- Modify: `common/1.21.1/src/main/java/com/minersmarket/state/GameStateSavedData.java`
- Modify: `common/1.21.1/src/main/java/com/minersmarket/state/GameStateManager.java`
- Modify: `common/1.21.1/src/main/java/com/minersmarket/entity/MerchantEntity.java`

**Interfaces:**
- Consumes: `GameMode`, `MinersMarketConfig.Game`, `MinersMarketConfig.ScoreDisplay` from Task 1.
- Produces: `RankedPlayer(int rank, String playerName, long salesAmount)`; `GameStateManager.getMode()`, `.getRemainingTicks()`, `.getRanking()`, `.isRankingVisible()`; `GameStateManager.addSalesAmount(UUID, String, long)` (signature changed — the old two-argument form is gone).

`RankedPlayer` goes into `common/shared` because 1.21.1 and 1.20.1 both compile it from there; the other version modules get their own copy in Task 8 onwards.

- [ ] **Step 1: Create `RankedPlayer`**

```java
package com.minersmarket.state;

/**
 * One row of a sales ranking. Ranks are 1-based and shared by players with equal
 * amounts, so a tie for first produces two entries with rank 1 and the next
 * distinct amount is rank 3.
 */
public record RankedPlayer(int rank, String playerName, long salesAmount) {
}
```

- [ ] **Step 2: Add the new fields to `GameStateSavedData`**

Add to the field block:

```java
    GameMode mode = ConfigDefaults.GAME_MODE;
    int timeLimitTicks = 0;
    final Map<UUID, String> playerNames = new HashMap<>();
```

with `import com.minersmarket.config.GameMode;`.

In `load`, after the `targetSales` line:

```java
        data.mode = readMode(tag);
        data.timeLimitTicks = tag.getInt("timeLimitTicks");
```

and after the `salesAmounts` loop:

```java
        CompoundTag namesTag = tag.getCompound("playerNames");
        for (String key : namesTag.getAllKeys()) {
            data.playerNames.put(UUID.fromString(key), namesTag.getString(key));
        }
```

Add the reader:

```java
    // Worlds saved before the time-limit mode existed have no "mode" tag, and an
    // out-of-range ordinal would throw, so both cases fall back to the default.
    private static GameMode readMode(CompoundTag tag) {
        if (!tag.contains("mode")) {
            return ConfigDefaults.GAME_MODE;
        }
        int ordinal = tag.getInt("mode");
        GameMode[] values = GameMode.values();
        return ordinal >= 0 && ordinal < values.length ? values[ordinal] : ConfigDefaults.GAME_MODE;
    }
```

In `save`, after `tag.putLong("targetSales", targetSales);`:

```java
        tag.putInt("mode", mode.ordinal());
        tag.putInt("timeLimitTicks", timeLimitTicks);
```

and after the `salesAmounts` block:

```java
        CompoundTag namesTag = new CompoundTag();
        playerNames.forEach((uuid, name) -> namesTag.putString(uuid.toString(), name));
        tag.put("playerNames", namesTag);
```

- [ ] **Step 3: Snapshot the mode in `GameStateManager.startCountdown()` and clear it in `reset()`**

```java
    public void startCountdown() {
        countdownTicks = MinersMarketConfig.get().game().countdownSeconds() * TICKS_PER_SECOND;
        savedData.salesAmounts.clear();
        savedData.playerNames.clear();
        savedData.playTime = 0;
        savedData.targetSales = MinersMarketConfig.get().game().targetSales();
        // Snapshot the rule-affecting settings so editing the config mid-game
        // cannot change the rules of a game already running.
        savedData.mode = MinersMarketConfig.get().game().mode();
        savedData.timeLimitTicks = MinersMarketConfig.get().game().timeLimitSeconds() * TICKS_PER_SECOND;
        savedData.setDirty();
    }
```

In `reset()`, add alongside the existing clears:

```java
        savedData.playerNames.clear();
        savedData.mode = ConfigDefaults.GAME_MODE;
        savedData.timeLimitTicks = 0;
```

with `import com.minersmarket.config.ConfigDefaults;` and `import com.minersmarket.config.GameMode;`.

- [ ] **Step 4: Add the timer accessors and visibility rule**

Add to `GameStateManager`, next to `getTargetSales()`:

```java
    public GameMode getMode() {
        return savedData.mode;
    }

    public int getRemainingTicks() {
        return Math.max(0, savedData.timeLimitTicks - savedData.playTime);
    }

    private boolean isTimeLimitExpired() {
        return savedData.mode == GameMode.TIME_LIMIT && savedData.playTime >= savedData.timeLimitTicks;
    }

    /**
     * Whether the full ranking may be shown. The score-display settings are read live
     * rather than snapshotted, so a host can toggle the scoreboard mid-game.
     */
    public boolean isRankingVisible() {
        if (savedData.mode != GameMode.TIME_LIMIT) {
            return false;
        }
        if (savedData.state == GameState.ENDED) {
            return true;
        }
        MinersMarketConfig.ScoreDisplay display = MinersMarketConfig.get().scoreDisplay();
        if (!display.alwaysShow()) {
            return false;
        }
        return display.hideRemainingSeconds() == 0
                || getRemainingTicks() > display.hideRemainingSeconds() * TICKS_PER_SECOND;
    }
```

- [ ] **Step 5: Add `getRanking()`**

```java
    public List<RankedPlayer> getRanking() {
        Map<UUID, Long> amounts = new HashMap<>(savedData.salesAmounts);
        Map<UUID, String> names = new HashMap<>(savedData.playerNames);
        if (serverLevel != null && serverLevel.getServer() != null) {
            // Online players who have not sold anything still belong in the ranking,
            // and their names are fresher than the stored ones.
            for (ServerPlayer player : serverLevel.getServer().getPlayerList().getPlayers()) {
                amounts.putIfAbsent(player.getUUID(), 0L);
                names.put(player.getUUID(), player.getDisplayName().getString());
            }
        }

        List<Map.Entry<UUID, Long>> sorted = new ArrayList<>(amounts.entrySet());
        sorted.sort(Comparator.<Map.Entry<UUID, Long>>comparingLong(Map.Entry::getValue).reversed());

        List<RankedPlayer> ranking = new ArrayList<>();
        long previousAmount = Long.MIN_VALUE;
        int previousRank = 0;
        for (int i = 0; i < sorted.size(); i++) {
            Map.Entry<UUID, Long> entry = sorted.get(i);
            long amount = entry.getValue();
            // Equal amounts share a rank; the next distinct amount skips the tied places.
            int rank = amount == previousAmount ? previousRank : i + 1;
            previousAmount = amount;
            previousRank = rank;
            String name = names.getOrDefault(entry.getKey(), entry.getKey().toString());
            ranking.add(new RankedPlayer(rank, name, amount));
        }
        return ranking;
    }
```

Add `import java.util.ArrayList;`, `import java.util.Comparator;` and `import java.util.HashMap;`.

- [ ] **Step 6: End the game when the timer expires**

Replace `tick()` with:

```java
    public void tick() {
        if (countdownTicks >= 0) {
            tickCountdown();
        }
        if (savedData.state == GameState.IN_PROGRESS || savedData.state == GameState.ENDED) {
            // Once the limit is reached the clock and the price events stop, so the
            // result stays fixed and the HUD keeps showing 00:00.
            if (!isTimeLimitExpired()) {
                savedData.playTime++;
                tickPriceEvent();
            }
            savedData.setDirty();
            if (savedData.state == GameState.IN_PROGRESS && isTimeLimitExpired()) {
                endByTimeLimit();
            }
        }
    }
```

In `target` mode `isTimeLimitExpired()` is always false, so this is behaviourally identical to the current code.

Add `endByTimeLimit()` next to `end()`:

```java
    private void endByTimeLimit() {
        savedData.state = GameState.ENDED;
        savedData.setDirty();

        List<RankedPlayer> ranking = getRanking();
        List<String> winners = new ArrayList<>();
        for (RankedPlayer entry : ranking) {
            if (entry.rank() == 1 && entry.salesAmount() > 0) {
                winners.add(entry.playerName());
            }
        }
        // With everyone on zero the shared-rank rule would make every player a joint
        // winner, which is not a result worth announcing.
        Component subtitle = winners.isEmpty()
                ? Component.translatable("message.minersmarket.time_up_no_winner")
                : Component.translatable("message.minersmarket.time_up_subtitle", String.join(", ", winners));
        broadcastTitleWithSubtitle(
                Component.translatable("message.minersmarket.time_up_title")
                        .withStyle(style -> style.withColor(ChatFormatting.GOLD).withBold(true)),
                subtitle, 10, 80, 20);
        broadcastSound(SoundEvents.UI_TOAST_CHALLENGE_COMPLETE);

        broadcastMessage(Component.translatable("message.minersmarket.final_ranking_header"));
        for (RankedPlayer entry : ranking) {
            broadcastMessage(Component.translatable("message.minersmarket.final_ranking_entry",
                    entry.rank(), entry.playerName(), String.format("%,d", entry.salesAmount())));
        }

        if (serverLevel != null && serverLevel.getServer() != null) {
            for (ServerPlayer player : serverLevel.getServer().getPlayerList().getPlayers()) {
                GameStateSyncPacket.sendToPlayer(player, this);
            }
        }
    }
```

Add `import com.minersmarket.network.GameStateSyncPacket;`.

- [ ] **Step 7: Stop selling when the timer expires**

```java
    public boolean canSell() {
        if (savedData.mode == GameMode.TIME_LIMIT) {
            // The result is final the moment the timer expires.
            return savedData.state == GameState.IN_PROGRESS;
        }
        return savedData.state == GameState.IN_PROGRESS || savedData.state == GameState.ENDED;
    }
```

- [ ] **Step 8: Record player names with sales**

```java
    public void addSalesAmount(UUID playerId, String playerName, long amount) {
        long current = getSalesAmount(playerId);
        savedData.salesAmounts.put(playerId, current + amount);
        savedData.playerNames.put(playerId, playerName);
        savedData.setDirty();
    }
```

- [ ] **Step 9: Update `MerchantEntity`**

Change the sale call to pass the name:

```java
        manager.addSalesAmount(player.getUUID(), player.getDisplayName().getString(), totalEarned);
```

and gate the target-reached branch on the mode:

```java
        // Reaching a particular amount is not an event in time-limit mode.
        if (manager.getMode() == GameMode.TARGET
                && manager.hasReachedTarget(player.getUUID())
                && !manager.hasFinished(player.getUUID())) {
```

Add `import com.minersmarket.config.GameMode;`.

Also make the "cannot sell" message mode-aware, so a player who interacts after time-up is told the game is over rather than that it has not started. Replace the `!manager.canSell()` message line with:

```java
            player.sendSystemMessage(Component.translatable(
                    manager.getState() == GameState.NOT_STARTED
                            ? "message.minersmarket.game_not_started"
                            : "message.minersmarket.game_ended_time_limit"));
```

- [ ] **Step 10: Build**

With `dangerouslyDisableSandbox: true`:
```
./gradlew build -Ptarget_mc_version=1.21.1 > /tmp/minersmarket-build/build.log 2>&1 #allow-direct
```
then:
```
grep -E "^BUILD|error:|FAILED" /tmp/minersmarket-build/build.log
```
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 11: Stage the files**

```bash
git add common/shared/src/main/java/com/minersmarket/state/RankedPlayer.java common/1.21.1/src/main/java/com/minersmarket/state/ common/1.21.1/src/main/java/com/minersmarket/entity/MerchantEntity.java
```

Commit message: `feat: add time-limit rules and sales ranking to the game state`

---

## Task 4: Sync the mode, remaining time and ranking

**Files:**
- Modify: `common/1.21.1/src/main/java/com/minersmarket/network/GameStateSyncPacket.java`
- Modify: `common/shared/src/main/java/com/minersmarket/state/ClientGameState.java`

**Interfaces:**
- Consumes: `GameStateManager.getMode()`, `.getRemainingTicks()`, `.getRanking()`, `.isRankingVisible()`, `RankedPlayer` from Task 3.
- Produces: `ClientGameState.getMode()` → `GameMode`, `.getRemainingTicks()` → `int`, `.getRanking()` → `List<RankedPlayer>` (empty when hidden); `ClientGameState.update(GameState, long, long, int, List<FinishedEntry>, boolean, int, float, GameMode, int, List<RankedPlayer>)`.

> **Note:** `ClientGameState` lives in `common/shared`, so this task makes the **1.20.1 build red** until Task 7 updates that module's packet and HUD. That is expected; do not try to fix 1.20.1 here.

- [ ] **Step 1: Extend the packet encoder**

Append to the end of `encode`, after the price-event block:

```java
        buf.writeInt(manager.getMode().ordinal());
        buf.writeInt(manager.getRemainingTicks());
        // A hidden ranking is not written at all, so it cannot be recovered from the
        // packet by a modified client.
        boolean showRanking = manager.isRankingVisible();
        buf.writeBoolean(showRanking);
        if (showRanking) {
            List<RankedPlayer> ranking = manager.getRanking();
            buf.writeInt(ranking.size());
            for (RankedPlayer entry : ranking) {
                buf.writeInt(entry.rank());
                buf.writeUtf(entry.playerName());
                buf.writeLong(entry.salesAmount());
            }
        }
```

Add `import com.minersmarket.config.GameMode;` and `import com.minersmarket.state.RankedPlayer;`.

- [ ] **Step 2: Extend the packet decoder**

Append to `applyOnClient`, before the `ClientGameState.update(...)` call:

```java
        int modeOrdinal = buf.readInt();
        GameMode mode = GameMode.values()[modeOrdinal];
        int remainingTicks = buf.readInt();
        boolean showRanking = buf.readBoolean();
        List<RankedPlayer> ranking = new ArrayList<>();
        if (showRanking) {
            int rankingCount = buf.readInt();
            for (int i = 0; i < rankingCount; i++) {
                int rank = buf.readInt();
                String name = buf.readUtf();
                long amount = buf.readLong();
                ranking.add(new RankedPlayer(rank, name, amount));
            }
        }
```

and change the call to:

```java
        ClientGameState.update(GameState.values()[stateOrdinal], salesAmount, targetSales, playTime,
                finishedEntries, hasEvent, eventRemainingTicks, multiplier,
                mode, remainingTicks, ranking);
```

- [ ] **Step 3: Extend `ClientGameState`**

Add the fields:

```java
    private static GameMode mode = ConfigDefaults.GAME_MODE;
    private static int remainingTicks = 0;
    private static List<RankedPlayer> ranking = Collections.emptyList();
```

extend `update` (keeping the existing parameter order and appending the new ones):

```java
    public static void update(GameState state, long salesAmount, long targetSales, int playTime,
                              List<FinishedEntry> finishedPlayers,
                              boolean priceEventActive, int priceEventRemainingTicks,
                              float priceMultiplier,
                              GameMode mode, int remainingTicks, List<RankedPlayer> ranking) {
```

with the matching assignments at the end of the method body:

```java
        ClientGameState.mode = mode;
        ClientGameState.remainingTicks = remainingTicks;
        ClientGameState.ranking = ranking;
```

add the getters:

```java
    public static GameMode getMode() {
        return mode;
    }

    public static int getRemainingTicks() {
        return remainingTicks;
    }

    /** Empty while the ranking is hidden — the server does not send it. */
    public static List<RankedPlayer> getRanking() {
        return ranking;
    }
```

and extend `reset()`:

```java
        mode = ConfigDefaults.GAME_MODE;
        remainingTicks = 0;
        ranking = Collections.emptyList();
```

Add `import com.minersmarket.config.GameMode;`.

- [ ] **Step 4: Build**

With `dangerouslyDisableSandbox: true`:
```
./gradlew build -Ptarget_mc_version=1.21.1 > /tmp/minersmarket-build/build.log 2>&1 #allow-direct
```
then:
```
grep -E "^BUILD|error:|FAILED" /tmp/minersmarket-build/build.log
```
Expected: `BUILD SUCCESSFUL`. (1.20.1 is expected to fail at this point and is fixed in Task 7.)

- [ ] **Step 5: Stage the files**

```bash
git add common/1.21.1/src/main/java/com/minersmarket/network/GameStateSyncPacket.java common/shared/src/main/java/com/minersmarket/state/ClientGameState.java
```

Commit message: `feat: sync game mode, remaining time and ranking to clients`

---

## Task 5: HUD for time-limit mode

**Files:**
- Modify: `common/1.21.1/src/main/java/com/minersmarket/hud/GameHudOverlay.java`

**Interfaces:**
- Consumes: `ClientGameState.getMode()`, `.getRemainingTicks()`, `.getRanking()`, `RankedPlayer` from Task 4.
- Produces: nothing consumed by later tasks.

This is where the feature becomes visible, so its manual verification is the real gate on Tasks 1–5.

- [ ] **Step 1: Branch the sales and time lines on the mode**

Replace the block from `// Sales amount display` through the play-time `drawString` with:

```java
        boolean timeLimit = ClientGameState.getMode() == GameMode.TIME_LIMIT;

        // Sales amount display. Time-limit mode has no target to compare against.
        long sales = ClientGameState.getSalesAmount();
        String salesText = timeLimit
                ? String.format("%,d", sales)
                : String.format("%,d / %,d", sales, ClientGameState.getTargetSales());
        int textWidth = mc.font.width(salesText);
        int x = screenWidth - textWidth - MARGIN - COIN_SIZE - 2;

        graphics.blit(COIN_TEXTURE, x, y, 0, 0, COIN_SIZE, COIN_SIZE, COIN_SIZE, COIN_SIZE);
        graphics.drawString(mc.font, salesText, x + COIN_SIZE + 2, y, 0xFFD700, true);

        // Time display: remaining time while a limit is running, elapsed time otherwise.
        int ticks = timeLimit ? ClientGameState.getRemainingTicks() : ClientGameState.getPlayTime();
        int totalSeconds = ticks / 20;
        int minutes = totalSeconds / 60;
        int seconds = totalSeconds % 60;
        String timeText = String.format("%02d:%02d", minutes, seconds);
        int timeWidth = mc.font.width(timeText);
        int timeColor = timeLimit && ticks <= 60 * 20 ? 0xFF5555 : 0xFFFFFF;
        graphics.drawString(mc.font, timeText, screenWidth - timeWidth - MARGIN, y + 12, timeColor, true);
```

Add `import com.minersmarket.config.GameMode;` and `import com.minersmarket.state.RankedPlayer;`.

- [ ] **Step 2: Branch the ranking block**

Replace the whole `// Ranking display (finished players)` block with:

```java
        int nextY = y + 26;
        if (timeLimit) {
            // Empty while the ranking is hidden — the server withholds it.
            for (RankedPlayer entry : ClientGameState.getRanking()) {
                String rankText = String.format("#%d %s  %,d",
                        entry.rank(), entry.playerName(), entry.salesAmount());
                int rankWidth = mc.font.width(rankText);
                int color = (entry.rank() == 1) ? 0xFFD700 : 0xCCCCCC;
                graphics.drawString(mc.font, rankText, screenWidth - rankWidth - MARGIN, nextY, color, true);
                nextY += 11;
            }
        } else {
            var finishedPlayers = ClientGameState.getFinishedPlayers();
            for (int i = 0; i < finishedPlayers.size(); i++) {
                var entry = finishedPlayers.get(i);
                int ft = entry.finishTimeTicks() / 20;
                String rankText = String.format("#%d %s  %02d:%02d",
                        i + 1, entry.playerName(), ft / 60, ft % 60);
                int rankWidth = mc.font.width(rankText);
                int color = (i == 0) ? 0xFFD700 : 0xCCCCCC;
                graphics.drawString(mc.font, rankText, screenWidth - rankWidth - MARGIN, nextY, color, true);
                nextY += 11;
            }
        }
```

- [ ] **Step 3: Build**

With `dangerouslyDisableSandbox: true`:
```
./gradlew build -Ptarget_mc_version=1.21.1 > /tmp/minersmarket-build/build.log 2>&1 #allow-direct
```
then:
```
grep -E "^BUILD|error:|FAILED" /tmp/minersmarket-build/build.log
```
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 4: Manual verification — target mode unchanged**

Run `./gradlew :fabric:runClient -Ptarget_mc_version=1.21.1`, create a world, and with the default config (`mode = "target"`) start a game from the Game Start Block. Confirm the HUD still shows `0 / 10,000`, an elapsed timer counting up, and that reaching the target still ends the game with the winner title.

- [ ] **Step 5: Manual verification — time-limit mode**

Edit `run/config/minersmarket.toml` to `mode = "time_limit"`, `time_limit_seconds = 60`, run `/minersmarket config reload`, reset the game with the Game Reset Block and start a new one. Confirm:

| Check | Expected |
|---|---|
| Sales line | shows a bare amount with no `/ 10,000` |
| Timer | counts **down** from `01:00`, turns red at `01:00` and below |
| Ranking during the game | absent (`always_show = false`) |
| Selling to the merchant | works while the timer runs |
| At `00:00` | "Time Up!" title, winner name subtitle, chat lists every player with rank and amount, sound plays |
| After time-up | timer stays `00:00`; ranking now shown on the HUD; interacting with the merchant says the game has ended and sells nothing |
| Game Reset Block | returns to `NOT_STARTED`, HUD clears |

- [ ] **Step 6: Manual verification — score display settings**

With `time_limit_seconds = 120`:

| Setting | Expected |
|---|---|
| `always_show = true`, `hide_remaining_seconds = 0` | ranking visible for the whole game |
| `always_show = true`, `hide_remaining_seconds = 60` | ranking visible above `01:00`, disappears at `01:00`, reappears at time-up |
| toggling `always_show` mid-game via the config screen + `/minersmarket config reload` | takes effect within a second, without restarting the game |

Also confirm a tie: give two players the same amount and check both get `#1` on the HUD and both names appear in the subtitle.

- [ ] **Step 7: Stage the file**

```bash
git add common/1.21.1/src/main/java/com/minersmarket/hud/GameHudOverlay.java
```

Commit message: `feat: show remaining time and score ranking on the HUD`

---

## Task 6: Propagate the config layer to all other version modules

**Files:**
- Copy to the other 14 modules: `config/GameMode.java`, `config/MinersMarketConfig.java`, `config/ConfigDefaults.java`, `config/ConfigRanges.java`, `config/ConfigLoader.java`, `config/ConfigWriter.java`, `resources/minersmarket-default-config.toml`
- Modify: `lang/en_us.json` and `lang/ja_jp.json` in all 14 other modules

**Interfaces:**
- Consumes: the canonical files from Tasks 1–2.
- Produces: identical config APIs in every module, which Tasks 7–11 depend on.

These files are byte-identical across all 15 modules today and must stay that way, so this is a copy, not a re-edit.

- [ ] **Step 1: Copy the six Java files and the TOML**

```bash
for v in 1.20.1 1.21.3 1.21.4 1.21.5 1.21.6 1.21.7 1.21.8 1.21.9 1.21.10 1.21.11 26.1 26.1.1 26.1.2 26.2; do
  cp common/1.21.1/src/main/java/com/minersmarket/config/GameMode.java common/$v/src/main/java/com/minersmarket/config/GameMode.java
  cp common/1.21.1/src/main/java/com/minersmarket/config/MinersMarketConfig.java common/$v/src/main/java/com/minersmarket/config/MinersMarketConfig.java
  cp common/1.21.1/src/main/java/com/minersmarket/config/ConfigDefaults.java common/$v/src/main/java/com/minersmarket/config/ConfigDefaults.java
  cp common/1.21.1/src/main/java/com/minersmarket/config/ConfigRanges.java common/$v/src/main/java/com/minersmarket/config/ConfigRanges.java
  cp common/1.21.1/src/main/java/com/minersmarket/config/ConfigLoader.java common/$v/src/main/java/com/minersmarket/config/ConfigLoader.java
  cp common/1.21.1/src/main/java/com/minersmarket/config/ConfigWriter.java common/$v/src/main/java/com/minersmarket/config/ConfigWriter.java
  cp common/1.21.1/src/main/resources/minersmarket-default-config.toml common/$v/src/main/resources/minersmarket-default-config.toml
done #allow-compound
```

- [ ] **Step 2: Copy the language files to the group that matches 1.21.1**

1.20.1 and 1.21.3–1.21.8 hold the same language files as 1.21.1:

```bash
for v in 1.20.1 1.21.3 1.21.4 1.21.5 1.21.6 1.21.7 1.21.8; do
  cp common/1.21.1/src/main/resources/assets/minersmarket/lang/en_us.json common/$v/src/main/resources/assets/minersmarket/lang/en_us.json
  cp common/1.21.1/src/main/resources/assets/minersmarket/lang/ja_jp.json common/$v/src/main/resources/assets/minersmarket/lang/ja_jp.json
done #allow-compound
```

- [ ] **Step 3: Hand-edit the 1.21.9+ language files, then copy within that group**

1.21.9, 1.21.10, 1.21.11 and the 26.x modules carry one extra key (`key.category.minersmarket.main`) that must not be lost. Add the same new keys from Task 2 Steps 9–10 to `common/1.21.9/src/main/resources/assets/minersmarket/lang/en_us.json` and `ja_jp.json`, keeping that extra key, then:

```bash
for v in 1.21.10 1.21.11 26.1 26.1.1 26.1.2 26.2; do
  cp common/1.21.9/src/main/resources/assets/minersmarket/lang/en_us.json common/$v/src/main/resources/assets/minersmarket/lang/en_us.json
  cp common/1.21.9/src/main/resources/assets/minersmarket/lang/ja_jp.json common/$v/src/main/resources/assets/minersmarket/lang/ja_jp.json
done #allow-compound
```

- [ ] **Step 4: Verify the copies are byte-identical**

```bash
md5 -q common/*/src/main/java/com/minersmarket/config/ConfigLoader.java common/*/src/main/resources/minersmarket-default-config.toml
```
Expected: one hash repeated 15 times for `ConfigLoader.java`, then one hash repeated 15 times for the TOML.

- [ ] **Step 5: Stage the files**

```bash
git add common/*/src/main/java/com/minersmarket/config/ common/*/src/main/resources/minersmarket-default-config.toml common/*/src/main/resources/assets/minersmarket/lang/
```

Commit message: `chore: propagate config layer changes to all version modules`

> No build here — the other modules do not compile until their own `ConfigScreen`, state and HUD files are updated in Tasks 7–11.

---

## Task 7: Port to 1.20.1

**Files:**
- Modify: `common/1.20.1/src/main/java/com/minersmarket/config/client/ConfigScreen.java`
- Modify: `common/1.20.1/src/main/java/com/minersmarket/state/GameStateSavedData.java`
- Modify: `common/1.20.1/src/main/java/com/minersmarket/state/GameStateManager.java`
- Modify: `common/1.20.1/src/main/java/com/minersmarket/network/GameStateSyncPacket.java`
- Modify: `common/1.20.1/src/main/java/com/minersmarket/hud/GameHudOverlay.java`
- Modify: `common/1.20.1/src/main/java/com/minersmarket/entity/MerchantEntity.java`

**Interfaces:**
- Consumes: everything from Tasks 1–6. `RankedPlayer` and `ClientGameState` come from `common/shared` and are already done.
- Produces: a green 1.20.1 build, which Task 4 deliberately left red.

Apply the same edits as Tasks 2–5, keeping 1.20.1's existing idioms:

- `GameStateSavedData` uses `load(CompoundTag)` / `save(CompoundTag)` with no `HolderLookup.Provider` — otherwise the Task 3 Step 2 code applies verbatim.
- `GameHudOverlay` uses RGB colours without an alpha byte, exactly as in Task 5.
- `ConfigScreen` entries use `render(GuiGraphics, index, top, left, width, height, mouseX, mouseY, hovering, partialTick)` and `resize(Minecraft, int, int)`, matching Task 2.

- [ ] **Step 1: Apply the Task 2 config-screen edits**

Follow Task 2 Steps 1–8 against `common/1.20.1/.../config/client/ConfigScreen.java`. The render signature there already matches the code in Task 2 Step 1, so it can be used as written.

- [ ] **Step 2: Apply the Task 3 state edits**

Follow Task 3 Steps 2–9 against the 1.20.1 files. In `GameStateSavedData`, drop the `HolderLookup.Provider` parameters from `load`/`save` to match the existing signatures; the field, `readMode`, and the `playerNames` read/write code are unchanged.

- [ ] **Step 3: Apply the Task 4 packet edits**

Follow Task 4 Steps 1–2 against `common/1.20.1/.../network/GameStateSyncPacket.java`. Only `ClientGameState.update(...)` and the new read/write blocks change; leave the platform-specific buffer type alone.

- [ ] **Step 4: Apply the Task 5 HUD edits**

Follow Task 5 Steps 1–2 against `common/1.20.1/.../hud/GameHudOverlay.java`, keeping the file's existing colour literals (no alpha byte).

- [ ] **Step 5: Build**

With `dangerouslyDisableSandbox: true`:
```
./gradlew build -Ptarget_mc_version=1.20.1 > /tmp/minersmarket-build/build.log 2>&1 #allow-direct
```
then:
```
grep -E "^BUILD|error:|FAILED" /tmp/minersmarket-build/build.log
```
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 6: Manual check**

Run `./gradlew :fabric:runClient -Ptarget_mc_version=1.20.1` and repeat the Task 5 Step 5 checks in short form: the config screen renders all rows without overlap (1.20.1 has a known scrollbar-position quirk — confirm the new rows scroll correctly), and a 60-second time-limit game ends with the ranking.

- [ ] **Step 7: Stage the files**

```bash
git add common/1.20.1/src/main/java/com/minersmarket/
```

Commit message: `feat: port time-limit mode to 1.20.1`

---

## Task 8: Port to 1.21.3 and 1.21.4

**Files:**
- Create: `common/1.21.3/src/main/java/com/minersmarket/state/RankedPlayer.java` (and the 1.21.4 copy)
- Modify, in `common/1.21.3/` then copied to `common/1.21.4/`: `state/ClientGameState.java`, `state/GameStateSavedData.java`, `state/GameStateManager.java`, `network/GameStateSyncPacket.java`, `hud/GameHudOverlay.java`, `entity/MerchantEntity.java`, `config/client/ConfigScreen.java`

**Interfaces:**
- Consumes: Tasks 1–6.
- Produces: green 1.21.3 and 1.21.4 builds.

1.21.3 and 1.21.4 hold identical copies of every one of these files, so edit 1.21.3 and copy across. Their `GameStateSavedData` matches 1.21.1's (`load`/`save` with `HolderLookup.Provider`), and their `ConfigScreen` matches 1.21.1's render signatures, so Tasks 2–5 apply almost verbatim.

- [ ] **Step 1: Copy `RankedPlayer` in**

```bash
cp common/shared/src/main/java/com/minersmarket/state/RankedPlayer.java common/1.21.3/src/main/java/com/minersmarket/state/RankedPlayer.java
```

- [ ] **Step 2: Apply the `ClientGameState` edits**

Apply Task 4 Step 3 to `common/1.21.3/src/main/java/com/minersmarket/state/ClientGameState.java` (this module has its own copy rather than using `common/shared`).

- [ ] **Step 3: Apply the state, packet, HUD, merchant and screen edits**

Apply Task 2 Steps 1–8, Task 3 Steps 2–9, Task 4 Steps 1–2 and Task 5 Steps 1–2 against the 1.21.3 files. No API adjustments are needed — these files match the 1.21.1 idioms this plan's code was written against.

- [ ] **Step 4: Build 1.21.3**

With `dangerouslyDisableSandbox: true`:
```
./gradlew build -Ptarget_mc_version=1.21.3 > /tmp/minersmarket-build/build.log 2>&1 #allow-direct
```
then:
```
grep -E "^BUILD|error:|FAILED" /tmp/minersmarket-build/build.log
```
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 5: Copy to 1.21.4 and build**

```bash
for f in state/RankedPlayer.java state/ClientGameState.java state/GameStateSavedData.java state/GameStateManager.java network/GameStateSyncPacket.java hud/GameHudOverlay.java entity/MerchantEntity.java config/client/ConfigScreen.java; do
  cp common/1.21.3/src/main/java/com/minersmarket/$f common/1.21.4/src/main/java/com/minersmarket/$f
done #allow-compound
```

Then build 1.21.4 the same way. Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 6: Stage the files**

```bash
git add common/1.21.3/src/main/java/com/minersmarket/ common/1.21.4/src/main/java/com/minersmarket/
```

Commit message: `feat: port time-limit mode to 1.21.3 and 1.21.4`

---

## Task 9: Port to 1.21.5–1.21.10

**Files:**
- Create: `common/1.21.5/src/main/java/com/minersmarket/state/RankedPlayer.java` (copied on to 1.21.6–1.21.10)
- Modify in `common/1.21.5/`, then copy per the group table: `state/ClientGameState.java`, `state/GameStateSavedData.java`, `state/GameStateManager.java`, `network/GameStateSyncPacket.java`, `hud/GameHudOverlay.java`, `entity/MerchantEntity.java`, `config/client/ConfigScreen.java`

**Interfaces:**
- Consumes: Tasks 1–6.
- Produces: green builds for 1.21.5, 1.21.6, 1.21.7, 1.21.8, 1.21.9, 1.21.10.

Two real API differences apply here:

1. **`GameStateSavedData` is a `Codec`, not `CompoundTag` I/O.** The new state must be added to the `RecordCodecBuilder` group and to `fromCodec`.
2. **From 1.21.6, HUD colours need a full alpha byte.** `0xFFD700` must be written `0xFFFFD700`, `0xFFFFFF` as `0xFFFFFFFF`, `0xCCCCCC` as `0xFFCCCCCC`, `0xFF5555` as `0xFFFF5555`. 1.21.5 keeps the alpha-less form. Getting this wrong makes the text invisible.

- [ ] **Step 1: Copy in the files that 1.21.5 shares with the 1.21.3 group**

`RankedPlayer` is identical everywhere; `GameStateSyncPacket` is one group spanning 1.21.3–1.21.10; `ClientGameState` is one group spanning 1.21.3–26.2. All three come from the already-finished 1.21.3 module rather than being re-edited:

```bash
cp common/1.21.3/src/main/java/com/minersmarket/state/RankedPlayer.java common/1.21.5/src/main/java/com/minersmarket/state/RankedPlayer.java
cp common/1.21.3/src/main/java/com/minersmarket/state/ClientGameState.java common/1.21.5/src/main/java/com/minersmarket/state/ClientGameState.java
cp common/1.21.3/src/main/java/com/minersmarket/network/GameStateSyncPacket.java common/1.21.5/src/main/java/com/minersmarket/network/GameStateSyncPacket.java
```

- [ ] **Step 2: Add the new fields to the codec in `GameStateSavedData`**

Add the three fields next to the existing ones:

```java
    GameMode mode = ConfigDefaults.GAME_MODE;
    int timeLimitTicks = 0;
    final Map<UUID, String> playerNames = new HashMap<>();
```

extend the codec group (insert after the `marketGenerated` entry, before the `salesAmounts` entry):

```java
            Codec.INT.optionalFieldOf("mode", ConfigDefaults.GAME_MODE.ordinal()).forGetter(d -> d.mode.ordinal()),
            Codec.INT.optionalFieldOf("timeLimitTicks", 0).forGetter(d -> d.timeLimitTicks),
            Codec.unboundedMap(Codec.STRING, Codec.STRING).optionalFieldOf("playerNames", Map.of())
                    .forGetter(d -> {
                        Map<String, String> map = new HashMap<>();
                        d.playerNames.forEach((uuid, name) -> map.put(uuid.toString(), name));
                        return map;
                    }),
```

and extend `fromCodec` to match the new parameter order:

```java
    private static GameStateSavedData fromCodec(int stateOrdinal, int playTime, long targetSales, boolean marketGenerated,
                                                int modeOrdinal, int timeLimitTicks, Map<String, String> nameMap,
                                                Map<String, Long> salesMap, List<FinishedPlayer> finishedPlayers) {
        GameStateSavedData data = new GameStateSavedData();
        data.state = GameState.values()[stateOrdinal];
        data.playTime = playTime;
        data.targetSales = targetSales;
        data.marketGenerated = marketGenerated;
        // An out-of-range ordinal from a newer build would throw, so fall back instead.
        GameMode[] modes = GameMode.values();
        data.mode = modeOrdinal >= 0 && modeOrdinal < modes.length ? modes[modeOrdinal] : ConfigDefaults.GAME_MODE;
        data.timeLimitTicks = timeLimitTicks;
        nameMap.forEach((key, value) -> data.playerNames.put(UUID.fromString(key), value));
        salesMap.forEach((key, value) -> data.salesAmounts.put(UUID.fromString(key), value));
        data.finishedPlayers.addAll(finishedPlayers);
        return data;
    }
```

Add `import com.minersmarket.config.GameMode;`.

- [ ] **Step 3: Apply the remaining edits to 1.21.5**

Apply Task 3 Steps 3–9 (`GameStateManager`, `MerchantEntity`), Task 5 Steps 1–2 (`GameHudOverlay`) and Task 2 Steps 1–8 (`ConfigScreen`) against `common/1.21.5/`. `ClientGameState` and `GameStateSyncPacket` are already done — they were copied in Step 1. 1.21.5 keeps the alpha-less HUD colours and the 1.21.1-style `ConfigScreen` render signatures, so the code in those tasks applies as written.

- [ ] **Step 4: Build 1.21.5**

With `dangerouslyDisableSandbox: true`:
```
./gradlew build -Ptarget_mc_version=1.21.5 > /tmp/minersmarket-build/build.log 2>&1 #allow-direct
```
then:
```
grep -E "^BUILD|error:|FAILED" /tmp/minersmarket-build/build.log
```
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 5: Copy the files that are shared with 1.21.6–1.21.10**

`GameStateSavedData`, `ClientGameState`, `RankedPlayer` and `GameStateManager` are identical across 1.21.5–1.21.10, and `GameStateSyncPacket` across 1.21.3–1.21.10:

```bash
for v in 1.21.6 1.21.7 1.21.8 1.21.9 1.21.10; do
  cp common/1.21.5/src/main/java/com/minersmarket/state/RankedPlayer.java common/$v/src/main/java/com/minersmarket/state/RankedPlayer.java
  cp common/1.21.5/src/main/java/com/minersmarket/state/ClientGameState.java common/$v/src/main/java/com/minersmarket/state/ClientGameState.java
  cp common/1.21.5/src/main/java/com/minersmarket/state/GameStateSavedData.java common/$v/src/main/java/com/minersmarket/state/GameStateSavedData.java
  cp common/1.21.5/src/main/java/com/minersmarket/state/GameStateManager.java common/$v/src/main/java/com/minersmarket/state/GameStateManager.java
  cp common/1.21.5/src/main/java/com/minersmarket/network/GameStateSyncPacket.java common/$v/src/main/java/com/minersmarket/network/GameStateSyncPacket.java
done #allow-compound
```

- [ ] **Step 6: Hand-edit `GameHudOverlay` for 1.21.6, then copy to 1.21.7–1.21.10**

Apply Task 5 Steps 1–2 to `common/1.21.6/src/main/java/com/minersmarket/hud/GameHudOverlay.java`, writing every colour with a full alpha byte: `0xFFFFD700` for the coin and rank-1 text, `0xFFCCCCCC` for lower ranks, `0xFFFFFFFF` for a normal timer and `0xFFFF5555` for the sub-minute timer. Keep the existing `graphics.blit(RenderPipelines.GUI_TEXTURED, ...)` call.

```bash
for v in 1.21.7 1.21.8 1.21.9 1.21.10; do
  cp common/1.21.6/src/main/java/com/minersmarket/hud/GameHudOverlay.java common/$v/src/main/java/com/minersmarket/hud/GameHudOverlay.java
done #allow-compound
```

- [ ] **Step 7: Hand-edit `MerchantEntity` for 1.21.6 and copy**

`MerchantEntity` is identical across 1.21.3, 1.21.4 and 1.21.6–1.21.10 (1.21.5 differs). Copy the already-edited 1.21.3 file:

```bash
for v in 1.21.6 1.21.7 1.21.8 1.21.9 1.21.10; do
  cp common/1.21.3/src/main/java/com/minersmarket/entity/MerchantEntity.java common/$v/src/main/java/com/minersmarket/entity/MerchantEntity.java
done #allow-compound
```

- [ ] **Step 8: `ConfigScreen` for 1.21.6–1.21.8, then 1.21.9/1.21.10**

1.21.6–1.21.8 share 1.21.1's `ConfigScreen`:

```bash
for v in 1.21.6 1.21.7 1.21.8; do
  cp common/1.21.1/src/main/java/com/minersmarket/config/client/ConfigScreen.java common/$v/src/main/java/com/minersmarket/config/client/ConfigScreen.java
done #allow-compound
```

1.21.9 and 1.21.10 use the newer entry API. Apply Task 2 Steps 1–8 to `common/1.21.9/.../ConfigScreen.java`, but write the `CycleEntry` render method as:

```java
        @Override
        public void renderContent(GuiGraphics guiGraphics, int mouseX, int mouseY, boolean hovering, float partialTick) {
            guiGraphics.drawString(this.font, this.label, getX(), getY() + 6, 0xFFFFFFFF);
            this.button.setPosition(getX() + getWidth() - WIDGET_WIDTH, getY());
            this.button.render(guiGraphics, mouseX, mouseY, partialTick);
        }
```

and keep that file's existing `resize(Minecraft minecraft, int width, int height)` signature when applying Task 2 Step 8 — 1.21.9/1.21.10 still take the `Minecraft` parameter even though the entry render method was renamed. Then:

```bash
cp common/1.21.9/src/main/java/com/minersmarket/config/client/ConfigScreen.java common/1.21.10/src/main/java/com/minersmarket/config/client/ConfigScreen.java
```

- [ ] **Step 9: Build every version in this group**

Build 1.21.6, 1.21.7, 1.21.8, 1.21.9 and 1.21.10 with the standard command. Expected: `BUILD SUCCESSFUL` for each.

- [ ] **Step 10: Manual check on 1.21.9**

Run `./gradlew :fabric:runClient -Ptarget_mc_version=1.21.9` and confirm the HUD text is actually visible (this is the version family where a missing alpha byte silently makes text disappear) and the config screen rows render correctly.

- [ ] **Step 11: Stage the files**

```bash
git add common/1.21.5/src/main/java/com/minersmarket/ common/1.21.6/src/main/java/com/minersmarket/ common/1.21.7/src/main/java/com/minersmarket/ common/1.21.8/src/main/java/com/minersmarket/ common/1.21.9/src/main/java/com/minersmarket/ common/1.21.10/src/main/java/com/minersmarket/
```

Commit message: `feat: port time-limit mode to 1.21.5-1.21.10`

---

## Task 10: Port to 1.21.11

**Files:**
- Create: `common/1.21.11/src/main/java/com/minersmarket/state/RankedPlayer.java`
- Modify: `common/1.21.11/src/main/java/com/minersmarket/state/{ClientGameState,GameStateSavedData,GameStateManager}.java`, `network/GameStateSyncPacket.java`, `hud/GameHudOverlay.java`, `entity/MerchantEntity.java`, `config/client/ConfigScreen.java`

**Interfaces:**
- Consumes: Tasks 1–6, plus the codec form written in Task 9 Step 2 and the alpha-corrected HUD from Task 9 Step 6.
- Produces: a green 1.21.11 build.

1.21.11 shares `GameStateSavedData` and `ClientGameState` with the 1.21.5+ group and `GameStateSyncPacket` with 26.x. Its `GameStateManager` and `GameHudOverlay` are its own, and its `ConfigScreen` uses the 1.21.9+ entry API.

- [ ] **Step 1: Copy the files shared with the 1.21.5+ group**

```bash
cp common/1.21.5/src/main/java/com/minersmarket/state/RankedPlayer.java common/1.21.11/src/main/java/com/minersmarket/state/RankedPlayer.java
cp common/1.21.5/src/main/java/com/minersmarket/state/ClientGameState.java common/1.21.11/src/main/java/com/minersmarket/state/ClientGameState.java
cp common/1.21.5/src/main/java/com/minersmarket/state/GameStateSavedData.java common/1.21.11/src/main/java/com/minersmarket/state/GameStateSavedData.java
```

Then confirm nothing regressed: `diff` each copied file against its pre-change git version to check the only differences are this feature's.

- [ ] **Step 2: Apply the `GameStateManager` edits**

Apply Task 3 Steps 3–8 to `common/1.21.11/.../state/GameStateManager.java`. This file replaces `player.playNotifySound(...)` with a private `playNotifySound(player, ...)` helper; the new `endByTimeLimit` code calls `broadcastSound(...)`, which already routes through that helper, so no adjustment is needed.

- [ ] **Step 3: Apply the packet, merchant, HUD and screen edits**

- `network/GameStateSyncPacket.java`: apply Task 4 Steps 1–2.
- `entity/MerchantEntity.java`: apply Task 3 Step 9.
- `hud/GameHudOverlay.java`: apply Task 5 Steps 1–2 with full-alpha colours (`0xFFFFD700`, `0xFFCCCCCC`, `0xFFFFFFFF`, `0xFFFF5555`).
- `config/client/ConfigScreen.java`: apply Task 2 Steps 1–8 using the `renderContent(...)` form and `resize(int, int)` signature shown in Task 9 Step 8.

- [ ] **Step 4: Build**

With `dangerouslyDisableSandbox: true`:
```
./gradlew build -Ptarget_mc_version=1.21.11 > /tmp/minersmarket-build/build.log 2>&1 #allow-direct
```
then:
```
grep -E "^BUILD|error:|FAILED" /tmp/minersmarket-build/build.log
```
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 5: Manual check**

Run `./gradlew :fabric:runClient -Ptarget_mc_version=1.21.11` and repeat the Task 5 Step 5 checks in short form, paying attention to HUD text visibility and config screen layout.

- [ ] **Step 6: Stage the files**

```bash
git add common/1.21.11/src/main/java/com/minersmarket/
```

Commit message: `feat: port time-limit mode to 1.21.11`

---

## Task 11: Port to 26.1, 26.1.1, 26.1.2 and 26.2

**Files:**
- Create: `common/26.1/src/main/java/com/minersmarket/state/RankedPlayer.java` (copied on to the other 26.x modules)
- Modify in `common/26.1/`, then copy: `state/{ClientGameState,GameStateSavedData,GameStateManager}.java`, `network/GameStateSyncPacket.java`, `hud/GameHudOverlay.java`, `entity/MerchantEntity.java`, `config/client/ConfigScreen.java`

**Interfaces:**
- Consumes: Tasks 1–6, 9 and 10.
- Produces: green builds for all four 26.x versions.

26.x differences that matter here:
- `GameHudOverlay` takes a `GuiGraphicsExtractor` and draws with `graphics.text(...)` instead of `graphics.drawString(...)`. Colours need full alpha.
- `ConfigScreen` is its own group for 26.1/26.1.1/26.1.2, with 26.2 differing again — treat them as two edits.
- `GameStateSavedData`, `ClientGameState` and `GameStateSyncPacket` match the 1.21.11 texts.

- [ ] **Step 1: Copy the files shared with 1.21.11**

```bash
cp common/1.21.11/src/main/java/com/minersmarket/state/RankedPlayer.java common/26.1/src/main/java/com/minersmarket/state/RankedPlayer.java
cp common/1.21.11/src/main/java/com/minersmarket/state/ClientGameState.java common/26.1/src/main/java/com/minersmarket/state/ClientGameState.java
cp common/1.21.11/src/main/java/com/minersmarket/state/GameStateSavedData.java common/26.1/src/main/java/com/minersmarket/state/GameStateSavedData.java
cp common/1.21.11/src/main/java/com/minersmarket/network/GameStateSyncPacket.java common/26.1/src/main/java/com/minersmarket/network/GameStateSyncPacket.java
```

- [ ] **Step 2: Apply the `GameStateManager` and `MerchantEntity` edits**

Apply Task 3 Steps 3–9 to `common/26.1/.../state/GameStateManager.java` and `common/26.1/.../entity/MerchantEntity.java`, keeping 26.x's `Identifier`-based `SavedDataType` and its private `playNotifySound(...)` helper untouched.

- [ ] **Step 3: Apply the HUD edits**

Apply Task 5 Steps 1–2 to `common/26.1/.../hud/GameHudOverlay.java`, replacing every `graphics.drawString(mc.font, ...)` in the new code with `graphics.text(mc.font, ...)` and using full-alpha colours (`0xFFFFD700`, `0xFFCCCCCC`, `0xFFFFFFFF`, `0xFFFF5555`).

- [ ] **Step 4: Apply the config screen edits for 26.1**

Apply Task 2 Steps 1–8 to `common/26.1/.../config/client/ConfigScreen.java`, matching that file's existing entry-render and `resize` signatures rather than the 1.21.1 ones.

- [ ] **Step 5: Copy to 26.1.1 and 26.1.2, and everything but `ConfigScreen` to 26.2**

```bash
for v in 26.1.1 26.1.2 26.2; do
  cp common/26.1/src/main/java/com/minersmarket/state/RankedPlayer.java common/$v/src/main/java/com/minersmarket/state/RankedPlayer.java
  cp common/26.1/src/main/java/com/minersmarket/state/ClientGameState.java common/$v/src/main/java/com/minersmarket/state/ClientGameState.java
  cp common/26.1/src/main/java/com/minersmarket/state/GameStateSavedData.java common/$v/src/main/java/com/minersmarket/state/GameStateSavedData.java
  cp common/26.1/src/main/java/com/minersmarket/state/GameStateManager.java common/$v/src/main/java/com/minersmarket/state/GameStateManager.java
  cp common/26.1/src/main/java/com/minersmarket/network/GameStateSyncPacket.java common/$v/src/main/java/com/minersmarket/network/GameStateSyncPacket.java
  cp common/26.1/src/main/java/com/minersmarket/hud/GameHudOverlay.java common/$v/src/main/java/com/minersmarket/hud/GameHudOverlay.java
  cp common/26.1/src/main/java/com/minersmarket/entity/MerchantEntity.java common/$v/src/main/java/com/minersmarket/entity/MerchantEntity.java
done #allow-compound
```

```bash
for v in 26.1.1 26.1.2; do
  cp common/26.1/src/main/java/com/minersmarket/config/client/ConfigScreen.java common/$v/src/main/java/com/minersmarket/config/client/ConfigScreen.java
done #allow-compound
```

- [ ] **Step 6: Apply the config screen edits for 26.2 separately**

26.2's `ConfigScreen` is its own variant. Apply Task 2 Steps 1–8 to `common/26.2/.../config/client/ConfigScreen.java`, matching that file's existing signatures. Do **not** copy 26.1's version over it.

- [ ] **Step 7: Build all four**

Build 26.1, 26.1.1, 26.1.2 and 26.2 with the standard command. Expected: `BUILD SUCCESSFUL` for each. 26.1 is NeoForge-only, so only `:neoforge` is built there.

- [ ] **Step 8: Manual check on 26.2**

Run `./gradlew :fabric:runClient -Ptarget_mc_version=26.2` and confirm HUD text visibility and config screen layout, as in Task 9 Step 10.

- [ ] **Step 9: Stage the files**

```bash
git add common/26.1/src/main/java/com/minersmarket/ common/26.1.1/src/main/java/com/minersmarket/ common/26.1.2/src/main/java/com/minersmarket/ common/26.2/src/main/java/com/minersmarket/
```

Commit message: `feat: port time-limit mode to 26.x`

---

## Task 12: Documentation and full build

**Files:**
- Modify: `README.md`
- Modify: `docs/modrinth_description.md`
- Modify: `docs/curseforge_description.md`
- Modify: `CHANGELOG.md`

**Interfaces:**
- Consumes: the finished feature from Tasks 1–11.
- Produces: nothing; this is the closing task.

- [ ] **Step 1: Update the README feature list**

Change the "Competitive Mining" bullet and add a mode bullet:

```markdown
- **Competitive Mining**: Race against other players to be the first to earn 10,000 gold
- **Two Game Modes**: Race to a target amount, or play to a time limit where the highest earner when the clock runs out wins (both [configurable](#server-configuration))
```

- [ ] **Step 2: Update the README settings table**

Replace the two `game.*` rows and add the new ones, so the `game` and `score_display` entries read:

```markdown
| `game.mode` | `"target"` | Win condition: `"target"` (first to reach `target_sales`) or `"time_limit"` (highest sales when time runs out) |
| `game.target_sales` | `10000` | Gold a player must earn to win (`target` mode only) |
| `game.time_limit_seconds` | `1800` | Length of a game in seconds (`time_limit` mode only) |
| `game.countdown_seconds` | `5` | Countdown length before a game starts (0–300) |
```

and add after the `price_event.*` rows:

```markdown
| `score_display.always_show` | `false` | Show every player's sales on the HUD during the game; when `false`, totals are revealed at time-up (`time_limit` mode only) |
| `score_display.hide_remaining_seconds` | `300` | With `always_show` on, hide the ranking once this many seconds remain; `0` keeps it visible to the end |
```

- [ ] **Step 3: Note the mode-switching semantics in the README**

Add a paragraph after the existing "Reloading affects future behavior only …" paragraph:

```markdown
The game mode and time limit are captured when a game starts, so changing them mid-game does
not affect the game in progress — start a new game for them to take effect. The score-display
settings are read continuously, so toggling them applies straight away.
```

- [ ] **Step 4: Update the store descriptions**

Add the same two-mode summary to the feature lists in `docs/modrinth_description.md` and `docs/curseforge_description.md`, matching each file's existing wording and markup.

- [ ] **Step 5: Update the changelog**

Add an entry under a new unreleased version heading, following the file's existing format:

```markdown
### Added
- Time-limit game mode: the player with the highest sales when the timer expires wins
  (`game.mode = "time_limit"`, `game.time_limit_seconds`).
- Score display settings for time-limit mode (`score_display.always_show`,
  `score_display.hide_remaining_seconds`): show the full ranking during the game, and
  optionally hide it once the remaining time drops below a threshold.
- Game mode, time limit and score display rows on the in-game config screen.

### Changed
- Config schema version is now `2`. Existing `minersmarket.toml` files load unchanged and
  keep the previous target-amount behaviour.
```

- [ ] **Step 6: Full multi-version build**

With `dangerouslyDisableSandbox: true`:
```
./gradlew buildAll > /tmp/minersmarket-build/buildall.log 2>&1 #allow-direct
```
then:
```
grep -E "^BUILD|error:|FAILED" /tmp/minersmarket-build/buildall.log
```
Expected: `BUILD SUCCESSFUL`. If any version fails, fix it in that version's module and rerun before finishing.

- [ ] **Step 7: Stage the files**

```bash
git add README.md docs/modrinth_description.md docs/curseforge_description.md CHANGELOG.md
```

Commit message: `docs: document the time-limit game mode`

---

## Self-Review Notes

Spec coverage check, section by section:

| Spec section | Task |
|---|---|
| Configuration (keys, ranges, string fallback, both amounts persisted, schema migration) | 1 |
| `GameMode` enum | 1 |
| Game state snapshot fields, `playerNames` | 3, 9 (codec form) |
| `getRemainingTicks`, `tick()` timeout, `canSell`, `getRanking` shared ranks, `addSalesAmount` | 3 |
| `MerchantEntity` target-branch gate | 3 |
| End-of-game title, chat ranking, sound, forced sync, all-zero case | 3 |
| Network fields and server-side visibility gate | 4 |
| `ClientGameState` new fields and reset | 4 |
| HUD: bare amount, countdown, red under 60s, ranking, no placeholder, `finishedPlayers` only in target mode | 5 |
| Config screen rows, no grey-out, shared validation ranges | 2 |
| Multi-version propagation | 6–11 |
| Testing (`buildAll`, manual matrix incl. 1.20.1 and 1.21.11/26.x) | 5, 7, 9, 10, 11, 12 |

No spec requirement is unassigned.
