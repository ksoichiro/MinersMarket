# Miner's Market

**A competitive mining mod for Minecraft**

Compete to strike it rich — mine ores, sell to the merchant, and be the first to earn 10,000 gold! Supports Fabric + NeoForge/Forge.

![Miner's Market Overview](docs/screenshots/featured-for-readme.png)

## Features

- **Competitive Mining**: Race against other players to be the first to earn 10,000 gold
- **Two Game Modes**: Race to a target amount, or play to a time limit where the highest earner when the clock runs out wins (both [configurable](#server-configuration))
- **NPC Merchant**: Sell your mined ores and ingots to a merchant NPC at the market
- **Auto-Generated Market**: A market structure spawns automatically at the world spawn point
- **Miner's Pickaxe**: A powerful custom pickaxe with Fortune III, high durability (4096), and fast mining speed
- **Price Events**: Dynamic price fluctuations occur every 10 minutes, lasting 3–5 minutes — sell at the right time for maximum profit! (timing and magnitude are [configurable](#server-configuration))
- **Real-Time HUD**: Track your earnings, play time, rankings, price events, and market direction on screen
- **Multiplayer Support**: Designed for competitive multiplayer sessions with rankings and finish notifications

## Supported Versions

> Minecraft 26.x requires a **Java 25** runtime.

| Minecraft | Mod Loader |
|-----------|-----------|
| 26.2 | Fabric Loader 0.18.5+ with Fabric API 0.152.2+26.2 |
| 26.2 | NeoForge 26.2.0.6-beta+ |
| 26.1.2 | Fabric Loader 0.18.5+ with Fabric API 0.145.4+26.1.2 |
| 26.1.2 | NeoForge 26.1.2.4-beta+ |
| 26.1.1 | Fabric Loader 0.18.5+ with Fabric API 0.145.4+26.1.1 |
| 26.1.1 | NeoForge 26.1.1.15-beta+ |
| 26.1 | NeoForge 26.1.0.1-beta+ (NeoForge only) |
| 1.21.11 | Fabric Loader 0.18.4+ with Fabric API 0.141.3+1.21.11 |
| 1.21.11 | NeoForge 21.11.38-beta+ |
| 1.21.10 | Fabric Loader 0.18.4+ with Fabric API 0.138.4+1.21.10 |
| 1.21.10 | NeoForge 21.10.64+ |
| 1.21.9 | Fabric Loader 0.18.4+ with Fabric API 0.134.1+1.21.9 |
| 1.21.9 | NeoForge 21.9.16-beta+ |
| 1.21.8 | Fabric Loader 0.18.4+ with Fabric API 0.136.0+1.21.8 |
| 1.21.8 | NeoForge 21.8.52+ |
| 1.21.7 | Fabric Loader 0.18.4+ with Fabric API 0.129.0+1.21.7 |
| 1.21.7 | NeoForge 21.7.25-beta+ |
| 1.21.6 | Fabric Loader 0.18.4+ with Fabric API 0.128.2+1.21.6 |
| 1.21.6 | NeoForge 21.6.20-beta+ |
| 1.21.5 | Fabric Loader 0.18.4+ with Fabric API 0.126.0+1.21.5 |
| 1.21.5 | NeoForge 21.5.96+ |
| 1.21.4 | Fabric Loader 0.18.4+ with Fabric API 0.119.3+1.21.4 |
| 1.21.4 | NeoForge 21.4.156+ |
| 1.21.3 | Fabric Loader 0.16.10+ with Fabric API 0.107.3+1.21.3 |
| 1.21.3 | NeoForge 21.3.95+ |
| 1.21.1 | Fabric Loader 0.17.3+ with Fabric API 0.116.7+1.21.1 |
| 1.21.1 | NeoForge 21.1.209+ |
| 1.20.1 | Fabric Loader 0.16.10+ with Fabric API 0.92.2+1.20.1 |
| 1.20.1 | Forge 47.3.0+ |

## Requirements

### For Players
- **Minecraft**: Java Edition 1.20.1–26.2
- **Mod Loader** (choose one):
  - **26.1.1–26.2**: Fabric with Fabric API, or NeoForge (requires Java 25)
  - **26.1**: NeoForge only (requires Java 25)
  - **1.21.1–1.21.11**: Fabric with Fabric API, or NeoForge
  - **1.20.1**: Fabric with Fabric API, or Forge
### For Developers
- **Java Development Kit (JDK)**: 25 for 26.x, 21 for 1.21.1+, 17 for 1.20.1 (auto-downloaded by toolchain)
- **IDE**: IntelliJ IDEA (recommended) or Eclipse

## Building from Source

```bash
# --recurse-submodules fetches gradle/shared (shared Gradle release tasks)
git clone --recurse-submodules https://github.com/ksoichiro/MinersMarket.git
cd MinersMarket
# If you cloned without the flag above:
# git submodule update --init

# Build for a specific version
./gradlew build -Ptarget_mc_version=1.21.1
./gradlew build -Ptarget_mc_version=1.20.1

# Build all supported versions
./gradlew buildAll

# Full release (clean + buildAll + collectJars)
./gradlew release
```

### Publishing

After `./gradlew release` populates `build/release/`, publish the JARs with the
shared Gradle tasks (from the `gradle/shared` submodule). API tokens are read
from the `MODRINTH_TOKEN` / `CURSEFORGE_TOKEN` environment variables.

```bash
./gradlew releaseModrinth      # publish all JARs to Modrinth
./gradlew releaseCurseForge    # publish all JARs to CurseForge
# Single file: ./gradlew releaseModrinth -Pjar=minersmarket-<ver>+<mc>-<loader>.jar
```

**Output Files** (examples):
- `fabric-<version>/build/libs/minersmarket-*-fabric.jar` - Fabric JAR
- `neoforge-<version>/build/libs/minersmarket-*-neoforge.jar` - NeoForge JAR (1.21.1+)
- `forge-1.20.1/build/libs/minersmarket-*-forge.jar` - Forge JAR (1.20.1)

## Development Setup

### Import to IDE

#### IntelliJ IDEA (Recommended)
1. Open IntelliJ IDEA
2. File → Open → Select `build.gradle` in project root
3. Choose "Open as Project"
4. Wait for Gradle sync to complete

### Run in Development Environment

```bash
# Fabric client (target version determined by gradle.properties or -P flag)
./gradlew :fabric:runClient

# NeoForge client (1.21.1)
./gradlew :neoforge:runClient

# Forge client (1.20.1)
./gradlew :forge:runClient -Ptarget_mc_version=1.20.1
```

## Installation

1. Install the desired Minecraft version
2. Install a supported mod loader (see [Supported Versions](#supported-versions) for details)
3. Download and install the required Fabric API (Fabric only)
4. Copy the corresponding Miner's Market JAR to `.minecraft/mods/` folder
5. Launch Minecraft with the mod loader profile

## Server Configuration

Server-side settings live in `config/minersmarket.toml`, which is created with default
values the first time the mod runs. The file is read on the server only; clients do not
need it.

Apply changes to a running server with:

```
/minersmarket config reload
```

Reloading affects future behavior only — it does not rewrite an already-generated market
or change the target of an in-progress game. The market can also be (re)generated manually
with `/minersmarket market generate`.

The game mode and time limit are captured when a game starts, so changing them mid-game does
not affect the game in progress — start a new game for them to take effect. The score-display
settings are read continuously, so toggling them applies straight away.

### In-Game Config Screen

All settings can also be edited from an in-game screen (built with vanilla widgets — no
extra mods required):

- **NeoForge / Forge**: Mods list → Miner's Market → **Config** button
- **Fabric**: mod list entry via [ModMenu](https://modrinth.com/mod/modmenu), if installed
  (optional — the mod does not depend on it)
- **Any platform**: bind the "Open Config Screen" key (unbound by default, under the
  "Miner's Market" key-binding category) and press it in game

The screen validates input against the same rules as the file loader and writes changes
back to your local `minersmarket.toml`, preserving comments. Changes apply immediately,
with the same semantics as `/minersmarket config reload`. In multiplayer, this edits the
local file only — dedicated servers are still configured server-side as described above.
When you open the screen while connected to a world hosted by someone else, the values shown
are your own local ones rather than the host's, and editing them has no effect on that
session; the screen displays a notice saying so.

### Settings

| Key | Default | Description |
| --- | --- | --- |
| `game.mode` | `"target"` | Win condition: `"target"` (first to reach `target_sales`) or `"time_limit"` (highest sales when time runs out) |
| `game.target_sales` | `10000` | Gold a player must earn to win (`target` mode only) |
| `game.time_limit_seconds` | `1800` | Length of a game in seconds (`time_limit` mode only) |
| `game.countdown_seconds` | `5` | Countdown length before a game starts (0–300) |
| `price_event.interval_seconds` | `600` | Seconds to wait after a price event ends before the next can start |
| `price_event.duration_min_seconds` | `180` | Minimum price event duration |
| `price_event.duration_max_seconds` | `300` | Maximum price event duration (must be ≥ min) |
| `price_event.change_min_percent` | `10` | Minimum price change percentage |
| `price_event.change_max_percent` | `30` | Maximum price change percentage (must be ≥ min) |
| `score_display.always_show` | `false` | Show every player's sales on the HUD during the game; when `false`, totals are revealed at time-up (`time_limit` mode only) |
| `score_display.hide_remaining_seconds` | `300` | With `always_show` on, hide the ranking once this many seconds remain; `0` keeps it visible to the end |
| `starter_items.enabled` | `true` | Whether the market chest is filled with starting equipment |
| `starter_items.player_count` | `8` | Number of players to equip, one pickaxe and one bread stack each (0–27) |
| `starter_items.bread_count` | `64` | Bread items per stack (1–64) |
| `starter_items.pickaxe_fortune_level` | `3` | Fortune level on the starter pickaxes; 0 disables the enchantment (0–255) |
| `market.generate_on_world_load` | `true` | Generate the market when an Overworld loads and none exists yet |

Out-of-range or inconsistent values are ignored with a logged warning and fall back to the
default.

## Project Structure

```
MinersMarket/
├── common/
│   ├── shared/              # Shared version-agnostic sources (included via srcDir)
│   └── <version>/           # Common module per MC version (version-specific APIs)
├── fabric/
│   ├── base/                # Shared Fabric sources
│   └── <version>/           # Fabric subproject per MC version
├── neoforge/
│   ├── base/                # Shared NeoForge sources
│   └── <version>/           # NeoForge subproject per MC version (1.21.1+)
├── forge/
│   ├── base/                # Shared Forge sources
│   └── 1.20.1/              # Forge subproject for MC 1.20.1
├── props/                   # Version-specific properties
├── build.gradle             # Root build configuration (Groovy DSL)
├── settings.gradle          # Multi-module settings
└── gradle.properties        # Version configuration
```

## Technical Notes

- **Build DSL**: Groovy DSL
- **Build Tooling**: Fabric Loom (Fabric/common) and ModDevGradle (NeoForge/Forge), selected by MC version in `settings.gradle`
- **Mappings**: Mojang mappings (official Minecraft class names)
- **Common Sharing**: The common module exposes sources/resources to platform modules via `commonJava`/`commonResources` configurations
- **Persistence**: Uses `SavedData` to track game state across server restarts

## License

This project is licensed under the **GNU Lesser General Public License v3.0 (LGPL-3.0)**.

Copyright (C) 2025 Soichiro Kashima

See the [COPYING](COPYING) and [COPYING.LESSER](COPYING.LESSER) files for full license text.

## Credits

- Built with [Fabric Loom](https://github.com/FabricMC/fabric-loom) and [ModDevGradle](https://github.com/neoforged/ModDevGradle) (build tooling)

## Support

For issues, feature requests, or questions:
- Open an issue on [GitHub Issues](https://github.com/ksoichiro/MinersMarket/issues)

---

**Developed for Minecraft Java Edition 1.20.1–26.2**
