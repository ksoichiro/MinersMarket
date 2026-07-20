# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/).

## [Unreleased]

### Added

- In-game config screen built with vanilla widgets (no extra dependencies)
  - Opens from the mod list: Config button on NeoForge/Forge, ModMenu entry on Fabric (when ModMenu is installed)
  - Key binding "Open Config Screen" (unbound by default, category "Miner's Market")
  - Edits all `minersmarket.toml` settings with validation, tooltips, and Reset to Defaults
  - Saves to the local config file preserving comments and applies immediately (same semantics as `/minersmarket config reload`)

## [0.4.0]

### Added

- Minecraft 26.2 support (Fabric + NeoForge)
- Minecraft 26.1.2 support (Fabric + NeoForge)
- Minecraft 26.1.1 support (Fabric + NeoForge)
- Minecraft 26.1 support (NeoForge)
- Server-side configuration via `config/minersmarket.toml` with `/minersmarket config reload`
- Configurable automatic market generation on world load
- `/minersmarket market generate` command to manually generate the market
- Configurable game target sales and start countdown
- Configurable price event interval, duration, and change magnitude
- Configurable starter items (toggle, player count, bread count, pickaxe Fortune level)

## [0.3.0]

### Added

- Minecraft 1.21.11 support (Fabric + NeoForge)
- Minecraft 1.21.10 support (Fabric + NeoForge)
- Anvil sound effect and styled title on game start
- CurseForge release automation scripts
- Modrinth release automation scripts

### Changed

- Removed Architectury API runtime dependency (native platform registration)

## [0.2.0]

### Added

- Price event mechanics with dynamic ore pricing
- Minecraft 1.21.9 support (Fabric + NeoForge)
- Minecraft 1.21.8 support (Fabric + NeoForge)
- Minecraft 1.21.7 support (Fabric + NeoForge)
- Minecraft 1.21.6 support (Fabric + NeoForge)
- Minecraft 1.21.5 support (Fabric + NeoForge)
- Minecraft 1.21.4 support (Fabric + NeoForge)
- Minecraft 1.21.3 support (Fabric + NeoForge)
- Minecraft 1.20.1 support (Fabric + Forge)

### Fixed

- Swapped prices for gold ingot and gold ore on market signs
- Client state and HUD not resetting when leaving world

## [0.1.0]

### Added

- Core game loop: mine ores, sell to merchant, earn 10,000 gold to win
- Miner's Pickaxe special item
- Game Start and Reset blocks
- Merchant NPC entity with selling system
- HUD overlay for sales and play time display
- Game flow with countdown, win titles, and reset broadcast
- Programmatic market structure generation at spawn
- Equipment and night vision granted on spawn/respawn
- Minecraft 1.21.1 support (Fabric + NeoForge)
