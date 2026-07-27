# Time-Limit Game Mode

Date: 2026-07-25

## Problem

The mod has exactly one win condition: the first player whose cumulative sales reach
`game.target_sales` wins, the game moves to `ENDED`, and later finishers are recorded in a
"goal reached" ranking (`MerchantEntity.mobInteract` → `GameStateManager.hasReachedTarget` /
`recordFinish` / `end`).

This shape has two consequences:

- The session length is unbounded and unpredictable. A host who wants a party game that fits a
  known slot has no way to bound it.
- The race is decided the instant somebody crosses the line, so there is no "everyone is
  revealed at once at the end" moment.

There is currently no notion of a per-player score comparison either. The HUD shows only the
local player's own `sales / target` plus the list of players who already reached the target
(`GameHudOverlay.render`). A player never sees how much anybody else has earned.

## Decision

Add a second, mutually exclusive game mode selected by config: `game.mode = "target"`
(the existing behaviour, default) or `game.mode = "time_limit"`.

In `time_limit` mode the game runs for a configured duration; when the timer expires the game
ends and the player with the highest sales wins. Ties share a rank and are announced together.

Score visibility becomes configurable, because the two natural ways to play the mode pull in
opposite directions: a hidden score makes the final reveal dramatic, a live scoreboard makes the
race legible. A third setting bridges them — show the scoreboard for most of the game, then hide
it once the remaining time drops below a threshold, so the ending is still a surprise.

Visibility is enforced **server-side**: when the ranking is hidden the server does not put it in
the sync packet at all. Sending data the client is merely asked not to draw would leak through
logs and modified clients, defeating the point of the setting.

## Scope

In scope:

- `game.mode` config key and the `time_limit` rule set (timer, end-by-timeout, highest-sales
  winner, shared ranks on ties).
- `game.time_limit_seconds` config key.
- `[score_display]` config table: `always_show`, `hide_remaining_seconds`.
- Server-side ranking construction and visibility gating; extension of `GameStateSyncPacket`.
- HUD changes that apply **only** to `time_limit` mode (remaining time, own amount without a
  target, full ranking).
- In-game config screen entries for the new keys.
- Config schema bump 1 → 2, `en_us` / `ja_jp` strings, propagation to every supported version
  module.

Out of scope:

- Any change to `target` mode behaviour or its HUD. It must render and play exactly as today.
- Config synchronisation between host and joined players (still absent mod-wide; see
  `2026-07-25-config-screen-host-only-note-design.md`).
- Per-mode presets, scheduled/automatic game start, or a lobby.

## Configuration

`minersmarket.toml`, `schema_version` becomes `2`:

```toml
schema_version = 2

[game]
mode = "target"              # "target" | "time_limit"
target_sales = 10000         # used by "target" mode only
time_limit_seconds = 1800    # used by "time_limit" mode only
countdown_seconds = 5

[score_display]              # applies to "time_limit" mode only
always_show = false          # false = reveal everyone's totals only when the game ends
hide_remaining_seconds = 300 # with always_show = true, hide the ranking once remaining time
                             # drops to this value. 0 = never hide.
```

Rules:

- `mode` is a string. An unrecognised value falls back to `target` and logs an error, matching
  how `ConfigLoader` already handles out-of-range and wrong-typed values.
- `target_sales` and `time_limit_seconds` are both always parsed and always written back, even
  in the mode that does not use them. Switching modes never destroys the other mode's value.
- New `ConfigRanges` entries:
  - `MIN_TIME_LIMIT_SECONDS = 1`, `MAX_TIME_LIMIT_SECONDS = Integer.MAX_VALUE / 20`
  - `MIN_HIDE_REMAINING_SECONDS = 0`, `MAX_HIDE_REMAINING_SECONDS = Integer.MAX_VALUE / 20`

  The upper bounds are divided by 20 because durations are converted to ticks and stored in an
  `int`; without the bound `seconds * 20` overflows.
- `hide_remaining_seconds` is not validated against `time_limit_seconds`. A threshold larger
  than the limit simply means the ranking is hidden for the whole game, which is a coherent
  configuration.
- Migrating an existing `schema_version = 1` file needs no special code path. `ConfigLoader`
  already defaults every absent key, so an old file loads as `mode = "target"` with the new
  defaults filled in, i.e. today's behaviour. The bump to `2` exists so a future build can tell
  the two layouts apart.

Record shape:

```java
public record Game(GameMode mode, long targetSales, int timeLimitSeconds, int countdownSeconds) {}
public record ScoreDisplay(boolean alwaysShow, int hideRemainingSeconds) {}
```

`GameMode` is a new enum (`TARGET`, `TIME_LIMIT`) in `com.minersmarket.config`.
`MinersMarketConfig` gains a `ScoreDisplay scoreDisplay()` component.

## Game State

`GameStateSavedData` already snapshots `targetSales` at countdown start so that editing the
config mid-game cannot corrupt a running game. The new rule-affecting values follow the same
pattern; the purely cosmetic ones do not.

Snapshotted at `startCountdown()`:

| Field | Notes |
|---|---|
| `GameMode mode` | Persisted as an ordinal with a range check, consistent with how `state` is stored today. |
| `int timeLimitTicks` | `time_limit_seconds * 20`; `0` in `target` mode. |

Also added, not snapshotted:

| Field | Notes |
|---|---|
| `Map<UUID, String> playerNames` | Last known display name per player who has sold something, so offline players still appear in the final ranking by name. Written whenever sales are recorded. |

`score_display.*` is **not** snapshotted — it is read live on every sync. A host can therefore
toggle the scoreboard mid-game from the config screen and see it take effect immediately.

### `GameStateManager` changes

- `getRemainingTicks()` returns `max(0, timeLimitTicks - playTime)`.
- `tick()`: in `time_limit` mode, once `state == IN_PROGRESS` and the remaining time reaches 0,
  call `endByTimeLimit()`. After the game ends in this mode `playTime` stops incrementing, so
  remaining time stays pinned at 0.
- `canSell()`: unchanged for `target` mode (`IN_PROGRESS || ENDED`); in `time_limit` mode returns
  true only for `IN_PROGRESS`. Selling stops the moment the timer expires so the result is final.
- `getRanking()` returns entries sorted by amount descending, built from the union of:
  - every UUID in `salesAmounts` (name from `playerNames`), and
  - every currently online player (amount defaults to 0),

  so a player who never sold anything still appears at the bottom, and a player who sold and then
  disconnected still appears with their name. Equal amounts receive the same rank and the next
  distinct amount skips ranks (`#1, #1, #3`).
- `addSalesAmount` takes the player's display name in addition to the UUID and records it in
  `playerNames`.

### `MerchantEntity` changes

The `hasReachedTarget` / `recordFinish` / `end` / `broadcastWinner` branch is entered only in
`target` mode. In `time_limit` mode reaching any particular amount is not an event.

### End of game

`endByTimeLimit()`:

1. Sets `state = ENDED`.
2. Builds the final ranking.
3. Broadcasts a title ("TIME UP") with the winner name as the subtitle; if several players are
   tied for first, their names are joined with `, `.
4. Sends the full ranking to chat, one line per player (`#1 Alice 8,400`).
5. Plays `UI_TOAST_CHALLENGE_COMPLETE`.
6. Immediately syncs to all players rather than waiting for the next periodic sync.

If every player is at 0, the shared-rank rule would nominally make everyone a joint winner. That
is treated as a special case instead: the subtitle reports that there is no winner, and the
ranking still lists everyone at 0. The state becomes `ENDED` either way.

## Network

`GameStateSyncPacket` gains, after the existing fields:

- `mode` (ordinal)
- `remainingTicks`
- `showRanking` (boolean)
- when `showRanking` is true: the ranking as a length-prefixed list of `(rank, name, amount)`

The server decides visibility:

```
showRanking = mode == TIME_LIMIT
              && ( state == ENDED
                   || ( scoreDisplay.alwaysShow()
                        && ( scoreDisplay.hideRemainingSeconds() == 0
                             || remainingTicks > scoreDisplay.hideRemainingSeconds() * 20 ) ) )
```

When `showRanking` is false the list is omitted from the packet entirely, so a hidden ranking is
never transmitted.

`ClientGameState` gains matching fields (`mode`, `remainingTicks`, `ranking`) and resets them in
`reset()`.

## HUD

`target` mode renders exactly as it does today — same lines, same positions, same colours.

`time_limit` mode:

- Own sales are drawn as a bare amount (`8,400`). There is no target, so no `/ 10,000` suffix.
- The time line shows **remaining** time as `MM:SS` instead of elapsed time. It turns red at 60
  seconds or less.
- When `showRanking` is true, every player is listed as `#1 Alice 8,400`, sorted by amount
  descending, rank 1 in gold and the rest in grey (reusing the existing ranking colours).
- When `showRanking` is false, the ranking block is simply absent. No placeholder and no
  "hidden" hint is drawn — the absence is the point, and a hint would only add noise.
- The `finishedPlayers` ranking is not rendered in this mode; it belongs to `target` mode.

## Config Screen

- Game section: a mode toggle button (Target amount ↔ Time limit) and a `time_limit_seconds`
  field.
- A new "Score display" section with an `always_show` toggle and a `hide_remaining_seconds`
  field.
- Fields belonging to the inactive mode are **not** greyed out. Both remain editable, which keeps
  the screen's widget handling uniform and lets a host prepare both modes' values in one visit.
- Validation reuses the new `ConfigRanges` constants, as the screen already does for existing
  fields.

## Multi-Version Propagation

| Files | Approach |
|---|---|
| `config/` (`MinersMarketConfig`, `ConfigDefaults`, `ConfigRanges`, `ConfigLoader`, `ConfigWriter`), `minersmarket-default-config.toml`, `lang/*.json` | Identical across all version modules. Author once, propagate with the existing script. |
| `GameMode`, `GameState*`, `ClientGameState`, `GameTickHandler`, `GameStateSyncPacket`, `MerchantEntity` | `common/shared` covers 1.21.1 and 1.20.1; every 1.21.3+ and 26.x module needs its own copy. NBT and packet-buffer APIs differ on 1.21.5+ and 26.x. |
| `GameHudOverlay`, `ConfigScreen` | Rendering APIs differ per version family. 1.20.1 and 1.21.9+/26.x need individual verification. |

## Testing

- `./gradlew buildAll` must succeed for every supported version.
- Manual verification on 1.21.1 with `time_limit_seconds` set low (e.g. 60):
  - timer counts down, expires, announces the ranking, and blocks further selling;
  - `always_show = false` hides the ranking until the end;
  - `always_show = true` shows it live and hides it once `hide_remaining_seconds` is crossed;
  - `hide_remaining_seconds = 0` keeps it visible to the end;
  - a tie produces a shared rank and both names in the subtitle;
  - `mode = "target"` behaves identically to the current release.
- HUD and config screen rendering additionally verified on 1.20.1 and 1.21.11 (or 26.x).
