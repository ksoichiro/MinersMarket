# Miner's Market - Modrinth Description

---

## ⛏️ Miner's Market - Competitive Mining Challenge

**A competitive mining mod for Minecraft**

Compete to strike it rich — mine ores, sell to the merchant, and be the first to earn 10,000 gold! By default, the first player to reach the goal wins!

---

## ✨ Features

### 🏪 Market at Spawn
- **Auto-Generated Market**: A market structure spawns automatically at the world spawn point
- **NPC Merchant**: A merchant NPC stands ready to buy your ores and ingots
- **Starter Equipment**: Each player receives a powerful Miner's Pickaxe and supplies from a chest

### ⛏️ Miner's Pickaxe
- **Fortune III**: Built-in Fortune III enchantment for maximum ore drops
- **High Durability**: 4096 durability — lasts through long mining sessions
- **Fast Mining**: Mining speed of 12.0 — faster than diamond tools

### 💰 Ore Trading
Sell your mined items to the merchant for gold:

| Item | Price | Item | Price |
|------|-------|------|-------|
| Coal | 1 | Raw Copper | 1 |
| Copper Ingot | 2 | Lapis Lazuli | 3 |
| Raw Iron | 5 | Iron Ingot | 10 |
| Raw Gold | 7 | Gold Ingot | 15 |
| Redstone | 5 | Diamond | 30 |
| Emerald | 10 | Amethyst Shard | 10 |
| Netherite Ingot | 100 | | |

### 📈 Price Events
- **Dynamic Pricing**: Every 10 minutes, a random price event occurs — ore prices go up or down!
- **Timed Events**: Each event lasts 3–5 minutes, keeping the game exciting
- **HUD Indicator**: Active price events are shown on your HUD with the current multiplier
- **Title Announcements**: Price changes are announced with on-screen titles so you never miss them
- **Strategy Element**: Decide whether to sell now or wait for better prices
- **Fully Configurable**: Server admins can tune the interval, duration, and price change magnitude

### 📊 Real-Time HUD
- **Earnings Display**: Track your current gold with a coin icon
- **Play Time**: See your elapsed time in MM:SS format
- **Rankings**: View finished players and their times
- **Floating Notifications**: Sale amounts pop up when you sell items
- **Market Direction Marker**: Always know which way to the market
- **Price Event Status**: See when a price event is active and the current price multiplier

### 🎮 Game Flow
- **Game Start Block**: Press to begin a 5-second countdown, then the game starts
- **Two Game Modes**: Race to a target amount, or play to a time limit where the highest earner when the clock runs out wins
- **Game Reset Block**: Double-click within 5 seconds to reset the game
- **Night Vision**: All players receive permanent night vision for mining
- **Victory Notifications**: Announcements when players reach the goal (target mode) or when time runs out (time-limit mode)

### 🛠️ Server Configuration
- **TOML Config**: Tune gameplay in `config/minersmarket.toml` (server-side only — no client setup needed)
- **Live Reload**: Apply changes on a running server with `/minersmarket config reload`
- **In-Game Config Screen**: Edit all settings from a built-in screen (Mods list, ModMenu, or a key binding) — applies to the world you host
- **Tunable Values**: Game mode and win target or time limit, score display visibility, start countdown, price event timing and magnitude, and starter items

---

## 📖 How to Play

1. **Create a New World**: Start a new world with the mod installed
2. **Find the Market**: A market structure is generated at the spawn point
3. **Grab Your Gear**: Take a Miner's Pickaxe and bread from the chest
4. **Start the Game**: Press the Game Start block to begin the countdown
5. **Mine Ores**: Head underground and mine as many ores as you can
6. **Sell to Merchant**: Return to the market and right-click the merchant while holding items
7. **Reach 10,000 Gold**: By default, the first player to earn 10,000 gold wins!

---

## 🛠️ Technical Details

### Multi-Loader Support
This mod supports **multiple mod loaders**!
- **26.1.1–26.2**: Fabric and NeoForge (requires Java 25)
- **26.1**: NeoForge only (requires Java 25)
- **1.21.1–1.21.11**: Fabric and NeoForge
- **1.20.1**: Fabric and Forge
- Download the correct version for your mod loader
- Shared codebase ensures consistent experience across loaders

### Requirements

> Minecraft 26.x requires a **Java 25** runtime.

#### Minecraft 26.2
- **Mod Loader**: Fabric Loader 0.18.5+ with Fabric API 0.152.2+26.2, OR NeoForge 26.2.0.6-beta+

#### Minecraft 26.1.2
- **Mod Loader**: Fabric Loader 0.18.5+ with Fabric API 0.145.4+26.1.2, OR NeoForge 26.1.2.4-beta+

#### Minecraft 26.1.1
- **Mod Loader**: Fabric Loader 0.18.5+ with Fabric API 0.145.4+26.1.1, OR NeoForge 26.1.1.15-beta+

#### Minecraft 26.1
- **Mod Loader**: NeoForge 26.1.0.1-beta+ (NeoForge only)

#### Minecraft 1.21.11
- **Mod Loader**: Fabric Loader 0.18.4+ with Fabric API 0.141.3+1.21.11, OR NeoForge 21.11.38-beta+

#### Minecraft 1.21.10
- **Mod Loader**: Fabric Loader 0.18.4+ with Fabric API 0.138.4+1.21.10, OR NeoForge 21.10.64+

#### Minecraft 1.21.9
- **Mod Loader**: Fabric Loader 0.18.4+ with Fabric API 0.134.1+1.21.9, OR NeoForge 21.9.16-beta+

#### Minecraft 1.21.8
- **Mod Loader**: Fabric Loader 0.18.4+ with Fabric API 0.136.0+1.21.8, OR NeoForge 21.8.52+

#### Minecraft 1.21.7
- **Mod Loader**: Fabric Loader 0.18.4+ with Fabric API 0.129.0+1.21.7, OR NeoForge 21.7.25-beta+

#### Minecraft 1.21.6
- **Mod Loader**: Fabric Loader 0.18.4+ with Fabric API 0.128.2+1.21.6, OR NeoForge 21.6.20-beta+

#### Minecraft 1.21.5
- **Mod Loader**: Fabric Loader 0.18.4+ with Fabric API 0.126.0+1.21.5, OR NeoForge 21.5.96+

#### Minecraft 1.21.4
- **Mod Loader**: Fabric Loader 0.18.4+ with Fabric API 0.119.3+1.21.4, OR NeoForge 21.4.156+

#### Minecraft 1.21.3
- **Mod Loader**: Fabric Loader 0.16.10+ with Fabric API 0.107.3+1.21.3, OR NeoForge 21.3.95+

#### Minecraft 1.21.1
- **Mod Loader**: Fabric Loader 0.17.3+ with Fabric API 0.116.7+1.21.1, OR NeoForge 21.1.209+

#### Minecraft 1.20.1
- **Mod Loader**: Fabric Loader 0.16.10+ with Fabric API 0.92.2+1.20.1, OR Forge 47.3.0+

### Compatibility
- Works alongside other mods
- Compatible with performance mods (Sodium, Starlight, etc.)
- Designed for multiplayer but also works in singleplayer

---

## 🐛 Bug Reports & Suggestions

Found a bug or have a suggestion? Report it on our [GitHub Issues](https://github.com/ksoichiro/MinersMarket/issues) page!

---

## 📷 Screenshots

Check out the **Gallery** above for screenshots showcasing:
- The market structure at spawn
- NPC merchant trading
- HUD overlay with earnings and rankings
- Miner's Pickaxe in action

---

## 🤝 Credits

- Built with [Fabric Loom](https://github.com/FabricMC/fabric-loom) and [ModDevGradle](https://github.com/neoforged/ModDevGradle) (build tooling)

---

## 📜 License & Permissions

**License**: LGPL-3.0 - Copyright (C) 2025 Soichiro Kashima

**Source Code**: Available on [GitHub](https://github.com/ksoichiro/MinersMarket)

**Freedom to Use**: ✅ Free to use, modify, and redistribute under LGPL-3.0 terms

**Modpack Inclusion**: ✅ Allowed - You may include this mod in modpacks

**Redistribution**: ✅ Allowed - You may redistribute this mod (must remain LGPL-3.0)

---

## 🔗 Links

- **GitHub Repository**: [https://github.com/ksoichiro/MinersMarket](https://github.com/ksoichiro/MinersMarket)
- **CurseForge**: [Link to CurseForge page]

---

**Developed for Minecraft Java Edition 1.20.1–26.2**

Race to riches! ⛏️
