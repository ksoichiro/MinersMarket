# Config Screen: Local-Settings Notice

Date: 2026-07-25

## Problem

The in-game config screen is reachable by every player (Mods list / ModMenu / key binding), but
a player who is not hosting the world has no way to tell that what they see and edit is
irrelevant to the session they are in.

Investigation of the current code confirms the situation is worse than a simple "changes don't
apply" case:

- `MinersMarketConfig.INSTANCE` is written from exactly two places:
  - `ConfigLoader.load(configDir)` — called from `MinersMarket.init()` (`MinersMarket.java:27`)
    and from `/minersmarket config reload` (`MinersMarket.java:78`). Both read the **local**
    `config/minersmarket.toml` on the machine running that code.
  - `ConfigScreen.saveAndClose()` — writes the local file and updates the local instance.
- The only network packet in the mod is `GameStateSyncPacket`. There is **no** config
  synchronisation of any kind.
- The config is consumed only by `GameStateManager` and `MarketGenerator`, i.e. server-side
  logic.

Therefore the values shown to a joined (non-host) player are not a stale snapshot of the host's
settings — they are the player's own local file contents, unrelated to the host. The screen
currently gives no indication of this.

## Decision

Keep the screen fully open and editable in all situations, and add a permanent one-line notice
at the top of the settings list whose wording depends on whether the player is hosting.

Editing is deliberately left enabled: a player connected to someone else's world may legitimately
want to prepare settings for a world they will host later, and a Config button that does nothing
is worse than one that explains itself.

## Scope

In scope:

- A notice row at the top of the config screen's settings list.
- Two new translation keys (en + ja).
- Documentation updates: `README.md`, `docs/modrinth_description.md`,
  `docs/curseforge_description.md`.

Out of scope:

- Synchronising the host's config to clients over the network.
- Disabling inputs or the Done button for non-host players.
- Any change to how settings are loaded, validated, or saved.

## Design

### Notice content

The notice is added as the first entry of the settings list, above the first category header.
It is a non-interactive, centred, single-line text row.

Host-state test, evaluated in `init()`:

```java
minecraft.level != null && !minecraft.hasSingleplayerServer()
```

`true` means "connected to a world hosted by someone else" (dedicated server, or another
player's LAN world). Singleplayer and LAN-hosting both keep an integrated server, so they
evaluate to `false`, as does the title screen (`level == null`).

| State | Translation key | Colour |
| --- | --- | --- |
| Title screen, singleplayer, or LAN host | `config.minersmarket.note_local` | `0xFFAAAAAA` (grey) |
| Joined a world hosted by someone else | `config.minersmarket.note_not_host` | `0xFFFFAA00` (orange) |

Strings:

| Key | en_us | ja_jp |
| --- | --- | --- |
| `config.minersmarket.note_local` | `Local settings: applied to worlds you host.` | `ローカル設定です。自分がホストするワールドに適用されます` |
| `config.minersmarket.note_not_host` | `Not the host: changes won't affect this world.` | `ホストではないため、この変更は現在のワールドに反映されません` |

These are ordinary localisation keys: each player sees one line in their own language.

The wording leads with "these are local settings" rather than "only the host's settings apply",
because the value displayed *is* the local file's value. Framing it as a property of the host
would imply the screen is showing the host's configuration.

Orange rather than red: the non-host case is a caution, not an input error. Red
(`0xFFFF5555`) is already used on this screen for invalid field values.

All colours are written with full alpha, which 1.21.9+ and 26.x require for correct text
rendering.

### Rendering

`HeaderEntry` already renders exactly what the notice needs — a centred, non-interactive line of
text — so it is reused rather than duplicated into a new class.

Changes to `HeaderEntry`:

1. Add a `private final int color` field.
2. Add a three-argument constructor `HeaderEntry(Font, Component, int)`.
3. Keep the existing two-argument constructor, delegating with `0xFFFFFF55` so the four existing
   category-header call sites are untouched.
4. Replace the literal `0xFFFFFF55` in the draw call with `this.color`.

Reuse matters here because the draw call differs across three API families:

| Family | Draw method | Call |
| --- | --- | --- |
| 1.20.1 – 1.21.8 | `render(GuiGraphics, index, top, left, width, height, mouseX, mouseY, hovering, partialTick)` | `guiGraphics.drawCenteredString(font, label, left + width / 2, top + 7, colour)` |
| 1.21.9 – 1.21.11 | `renderContent(GuiGraphics, mouseX, mouseY, hovering, partialTick)` | `guiGraphics.drawCenteredString(font, label, getX() + getWidth() / 2, getY() + 7, colour)` |
| 26.x | `extractContent(GuiGraphicsExtractor, mouseX, mouseY, hovering, partialTick)` | `guiGraphics.centeredText(font, label, getX() + getWidth() / 2, getY() + 7, colour)` |

The colour is the final argument in all three, so a single textual substitution applies
everywhere. A new entry class would require writing three separate draw methods.

`ConfigScreen.init()` adds the notice row before the first category header, choosing key and
colour from the host-state test.

### Width constraint

`ContainerObjectSelectionList` uses a fixed `ITEM_HEIGHT` of 25px for every entry, so the notice
cannot wrap onto a second line. Text must fit `ROW_WIDTH` (310px).

Estimated widths of the chosen strings: en ≈ 265px, ja ≈ 270px. Any future edit to these strings
must preserve the single-line fit.

## Files affected

`ConfigScreen.java` — 15 files across 6 distinct variants:

| Variant | Versions |
| --- | --- |
| 1 | 1.21.1, 1.21.3, 1.21.4, 1.21.5, 1.21.6, 1.21.7, 1.21.8 |
| 2 | 1.21.9, 1.21.10 |
| 3 | 1.21.11 |
| 4 | 26.1, 26.1.1, 26.1.2 |
| 5 | 26.2 |
| 6 | 1.20.1 |

Language files — 30 files (`en_us.json` + `ja_jp.json` in each of the 15 common modules). Two
variants exist per language, differing only by the presence of `key.category.minersmarket.main`;
both are handled by inserting the new keys after the `config.minersmarket.reset` line.

Changes are applied across modules with a Python script, matching how config files are already
propagated in this repository.

## Documentation

`README.md`, "In-Game Config Screen" section (around lines 163-166): add a sentence stating that
a player connected to another player's world sees their own local values, not the host's, and
that edits made there do not affect that session.

`docs/modrinth_description.md` and `docs/curseforge_description.md`, "🛠️ Server Configuration"
section: neither file currently mentions the in-game config screen. Add one bullet to each:

```
- **In-Game Config Screen**: Edit all settings from a built-in screen (Mods list, ModMenu, or a key binding) — applies to the world you host
```

The two files are otherwise near-identical; the bullet is the same in both.

## Verification

Mapping check: resolved. `hasSingleplayerServer` is present in the mapped `Minecraft` class for
1.20.1, 26.1, and 26.2, and `mc.level` is already used by `MarketMarkerRenderer` in every version
family. No per-version substitution is needed.

Build one representative per variant: 1.20.1, 1.21.1, 1.21.9, 26.2.

In-game check on 1.20.1 and on 1.21.9 or later / 26.x, since those are the version families where
this project has previously hit screen-rendering differences. For each: open the screen from the
title screen, from a singleplayer world, and as a client joined to a second instance's LAN world,
and confirm the correct notice text, colour, and single-line fit.
