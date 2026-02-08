# Miner's Market

**A competitive mining mod for Minecraft**

Compete to strike it rich — mine ores, sell to the merchant, and be the first to earn 10,000 gold! Built with Architectury for cross-platform support (Fabric + NeoForge).

## Features

- **Competitive Mining**: Race against other players to be the first to earn 10,000 gold
- **NPC Merchant**: Sell your mined ores and ingots to a merchant NPC at the market
- **Auto-Generated Market**: A market structure spawns automatically at the world spawn point
- **Miner's Pickaxe**: A powerful custom pickaxe with Fortune III, high durability (4096), and fast mining speed
- **Real-Time HUD**: Track your earnings, play time, rankings, and market direction on screen
- **Multiplayer Support**: Designed for competitive multiplayer sessions with rankings and finish notifications

## Supported Versions

| Minecraft | Mod Loader | Dependencies |
|-----------|-----------|--------------|
| 1.21.1 | Fabric Loader 0.17.3+ with Fabric API 0.116.7+1.21.1 | Architectury API 13.0.8+ |
| 1.21.1 | NeoForge 21.1.209+ | Architectury API 13.0.8+ |

## Requirements

### For Players
- **Minecraft**: Java Edition 1.21.1
- **Mod Loader** (choose one):
  - Fabric Loader 0.17.3+ with Fabric API, OR
  - NeoForge 21.1.209+
- **Dependencies**:
  - Architectury API 13.0.8+

### For Developers
- **Java Development Kit (JDK)**: 21 or higher
- **IDE**: IntelliJ IDEA (recommended) or Eclipse

## Building from Source

```bash
git clone https://github.com/ksoichiro/MinersMarket.git
cd MinersMarket
./gradlew build
```

**Output Files**:
- `fabric-1.21.1/build/libs/minersmarket-0.1.0-fabric.jar` - Fabric loader JAR
- `neoforge-1.21.1/build/libs/minersmarket-0.1.0-neoforge.jar` - NeoForge loader JAR

## Development Setup

### Import to IDE

#### IntelliJ IDEA (Recommended)
1. Open IntelliJ IDEA
2. File → Open → Select `build.gradle` in project root
3. Choose "Open as Project"
4. Wait for Gradle sync to complete

### Run in Development Environment

```bash
# Fabric client
./gradlew :fabric:runClient

# NeoForge client
./gradlew :neoforge:runClient
```

## Installation

### Fabric
1. Install Minecraft 1.21.1
2. Install Fabric Loader 0.17.3+
3. Download and install Fabric API 0.116.7+1.21.1
4. Download and install Architectury API 13.0.8+
5. Copy the Fabric JAR to `.minecraft/mods/` folder
6. Launch Minecraft with Fabric profile

### NeoForge
1. Install Minecraft 1.21.1
2. Install NeoForge 21.1.209+
3. Download and install Architectury API 13.0.8+
4. Copy the NeoForge JAR to `.minecraft/mods/` folder
5. Launch Minecraft with NeoForge profile

## Project Structure

```
MinersMarket/
├── common-shared/           # Shared version-agnostic sources (included via srcDir)
├── common-1.21.1/           # Common module for MC 1.21.1
│   └── src/main/
│       ├── java/com/minersmarket/
│       │   ├── MinersMarket.java       # Common entry point
│       │   ├── registry/               # Items, blocks, entities, creative tab
│       │   ├── state/                  # Game state management
│       │   ├── entity/                 # Merchant NPC
│       │   ├── block/                  # Game start/reset blocks
│       │   ├── item/                   # Miner's pickaxe
│       │   ├── trade/                  # Price list
│       │   ├── network/               # State sync packets
│       │   ├── event/                  # Player spawn, game tick handlers
│       │   ├── hud/                    # HUD overlay, market marker
│       │   └── structure/             # Market generation
│       └── resources/
│           └── assets/minersmarket/    # Textures, models, lang files
├── fabric-base/             # Shared Fabric sources
├── fabric-1.21.1/           # Fabric subproject for MC 1.21.1
├── neoforge-base/           # Shared NeoForge sources
├── neoforge-1.21.1/         # NeoForge subproject for MC 1.21.1
├── props/                   # Version-specific properties
├── build.gradle             # Root build configuration (Groovy DSL)
├── settings.gradle          # Multi-module settings
└── gradle.properties        # Version configuration
```

## Technical Notes

- **Build DSL**: Groovy DSL (for Architectury Loom compatibility)
- **Mappings**: Mojang mappings (official Minecraft class names)
- **Shadow Plugin**: Bundles common module into loader-specific JARs
- **Persistence**: Uses `SavedData` to track game state across server restarts

## License

This project is licensed under the **GNU Lesser General Public License v3.0 (LGPL-3.0)**.

Copyright (C) 2025 Soichiro Kashima

See the [COPYING](COPYING) and [COPYING.LESSER](COPYING.LESSER) files for full license text.

## Credits

- Built with [Architectury](https://github.com/architectury/architectury-api)

## Support

For issues, feature requests, or questions:
- Open an issue on [GitHub Issues](https://github.com/ksoichiro/MinersMarket/issues)

---

**Developed for Minecraft Java Edition 1.21.1**
