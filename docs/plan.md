# Miner's Market Implementation Plan

This document outlines the phased implementation plan for the Miner's Market Minecraft mod based on [draft.md](draft.md).

## Package Structure

- Base package: `com.minersmarket`
- Sub-packages:
  - `registry` - Item, Block, Entity, Structure registration
  - `state` - Game state management
  - `entity` - Merchant entity
  - `block` - Game Start Block, Game Reset Block
  - `item` - Miner's Pickaxe
  - `trade` - Selling system, price list
  - `hud` - HUD rendering (client-only)
  - `network` - Network packet synchronization
  - `event` - Event listeners (player spawn, tick, etc.)
  - `structure` - Market structure placement

## Phases

### Phase 1: Mod Entry Points and Registration Framework

Set up the basic mod initialization and registration system.

- [ ] 1-1. Create common mod initializer `MinersMarket` in `common-shared`
  - Mod ID constant: `minersmarket`
  - Common `init()` method called from platform-specific initializers
- [ ] 1-2. Create Fabric mod initializer in `fabric-base`
  - Implement `ModInitializer`
  - Call common `init()`
  - Create `fabric.mod.json`
- [ ] 1-3. Create NeoForge mod initializer in `neoforge-base`
  - Use `@Mod` annotation
  - Call common `init()`
  - Create `META-INF/neoforge.mods.toml`
- [ ] 1-4. Create deferred registration helpers using Architectury API
  - `ModItems` - Item registry
  - `ModBlocks` - Block registry
  - `ModEntityTypes` - Entity type registry
  - `ModCreativeTab` - Creative tab for mod items

### Phase 2: Game State Management

Implement the core state machine and persistence.

- [ ] 2-1. Create `GameState` enum: `NOT_STARTED`, `IN_PROGRESS`, `ENDED`
- [ ] 2-2. Create `GameStateManager` (server-side singleton)
  - Current state
  - Per-player sales amounts (`Map<UUID, Long>`)
  - Play time counter (ticks since start)
  - Target sales amount (10,000)
  - State transition methods: `start()`, `end(player)`, `reset()`
  - Allowed operation checks per state (selling, start block, reset block)
- [ ] 2-3. Persist game state using `SavedData` (vanilla level data)
  - Save/load state, sales amounts, play time on world save/load
- [ ] 2-4. Create network packets to sync state to clients
  - `GameStateSyncPacket` - Sync game state, player's own sales amount, play time
  - Use Architectury networking API
- [ ] 2-5. Create `ClientGameState` for client-side state cache
  - Stores current state, own sales amount, play time for HUD rendering

### Phase 3: Custom Items

Implement mod-specific items.

- [ ] 3-1. **Miner's Pickaxe** (`minersmarket:minerspickaxe`)
  - Custom pickaxe item with high mining speed and durability
  - Fortune III enchantment applied by default
  - Localized names: EN "Miner's Pickaxe" / JA "採掘者のツルハシ"
- [ ] 3-2. Register items in `ModItems`
- [ ] 3-3. Add item models and textures
  - `assets/minersmarket/models/item/minerspickaxe.json`
  - `assets/minersmarket/textures/item/minerspickaxe.png`

### Phase 4: Custom Blocks

Implement game control blocks.

- [ ] 4-1. **Game Start Block** (`minersmarket:game_start_block`)
  - Right-click interaction: prompt to start the game (or start directly for MVP)
  - Only functional when game state is `NOT_STARTED`
  - Localized names: EN "Game Start Block" / JA "ゲーム開始ブロック"
- [ ] 4-2. **Game Reset Block** (`minersmarket:game_reset_block`)
  - Right-click interaction: prompt to reset the game (or reset directly for MVP)
  - Only functional when game state is `IN_PROGRESS` or `ENDED`
  - Localized names: EN "Game Reset Block" / JA "ゲームリセットブロック"
- [ ] 4-3. Register blocks and block items in `ModBlocks`
- [ ] 4-4. Add block models, textures, and blockstates
- [ ] 4-5. Add loot tables for the blocks

### Phase 5: Merchant Entity

Implement the market NPC.

- [ ] 5-1. Create `MerchantEntity` extending `Mob` (using villager model)
  - ID: `minersmarket:merchant`
  - Non-hostile, immovable (NoAI), invulnerable
  - Right-click interaction opens selling UI
- [ ] 5-2. Create `MerchantEntityRenderer` (client-side)
  - Reuse villager model and texture (or custom texture)
- [ ] 5-3. Register entity type in `ModEntityTypes`
- [ ] 5-4. Register renderer on client side (Fabric/NeoForge specific)

### Phase 6: Selling System

Implement the ore purchase/selling mechanism.

- [ ] 6-1. Create `PriceList` defining sellable items and prices
  - Coal: 1, Raw Copper: 3, Copper Ingot: 5, Lapis Lazuli: 3
  - Raw Iron: 5, Iron Ingot: 10, Raw Gold: 7, Gold Ingot: 15
  - Redstone: 5, Diamond: 30, Emerald: 10, Amethyst Shard: 10
  - Netherite Ingot: 100
- [ ] 6-2. Choose and implement selling UI approach
  - **Option A**: Reuse vanilla trading UI (MerchantMenu) - preferred if feasible
  - **Option B**: Item-use on merchant (simpler, use items directly on NPC)
  - **Option C**: Custom GUI
  - Decision: Start with **Option B** (use items on merchant) for MVP simplicity
    - Right-clicking merchant with a sellable item sells 1 item
    - Shift+right-click sells the entire stack
    - Feedback via action bar message showing amount earned
- [ ] 6-3. Validate selling is only allowed during `IN_PROGRESS` state
- [ ] 6-4. Update `GameStateManager` sales amounts on sell
- [ ] 6-5. Sync updated sales amount to the selling player
- [ ] 6-6. Check win condition after each sale (sales >= 10,000)

### Phase 7: HUD Display

Implement client-side HUD overlay.

- [ ] 7-1. Create `GameHudOverlay` (client-side)
  - Rendered using Architectury client events or platform-specific render events
- [ ] 7-2. **Sales Amount Display**
  - Position: Top-right, right-aligned
  - Format: `💰 3,250 / 10,000` (use gold coin sprite if emoji is problematic)
  - Create gold coin texture: `assets/minersmarket/textures/gui/coin.png`
- [ ] 7-3. **Play Time Display**
  - Position: Top-right, below sales amount
  - Format: `MM:SS`
  - Only displayed when game is `IN_PROGRESS` or `ENDED`
- [ ] 7-4. Register HUD overlay on client side (Fabric/NeoForge specific)

### Phase 8: Market Structure

Implement the market building structure.

- [ ] 8-1. Design market structure using structure block / NBT
  - Include placement positions for: Merchant NPC, Game Start Block, Game Reset Block
  - Save as `data/minersmarket/structure/market.nbt`
- [ ] 8-2. Create structure set and template pool
  - `data/minersmarket/worldgen/structure/market.json`
  - `data/minersmarket/worldgen/structure_set/market.json`
  - `data/minersmarket/worldgen/template_pool/market.json`
- [ ] 8-3. Implement single-generation logic at world spawn
  - Generate market at initial spawn point on new world creation
  - Ensure it only generates once (flag in saved data)
- [ ] 8-4. Support manual placement via `/place structure minersmarket:market`
- [ ] 8-5. Set player initial spawn point near the market

### Phase 9: Game Flow

Implement start, end, and reset logic.

- [ ] 9-1. **Game Start**
  - Game Start Block triggers countdown (3, 2, 1, Start!)
  - Countdown displayed as title text to all players
  - State transitions to `IN_PROGRESS` when countdown reaches zero
  - Play time counter begins
- [ ] 9-2. **Game End (Win Detection)**
  - Check after each sale if player's sales amount >= 10,000
  - First player to reach target wins
  - Display winner announcement to all players via title text
  - State transitions to `ENDED`
  - Play time counter stops
- [ ] 9-3. **Game Reset**
  - Game Reset Block clears all sales amounts and play time
  - State transitions to `NOT_STARTED`
  - Announce reset to all players

### Phase 10: Player Spawn and Effects

Implement initial equipment and permanent effects.

- [ ] 10-1. **Initial Equipment on Spawn**
  - Grant Miner's Pickaxe on first spawn and respawn (death)
  - Grant 1 stack of Bread on first spawn and respawn
  - Use player respawn event (Architectury)
- [ ] 10-2. **Night Vision**
  - Apply permanent Night Vision effect (duration: effectively infinite, e.g., `Integer.MAX_VALUE` ticks)
  - Reapply on respawn
  - Use player tick event or respawn event

### Phase 11: Price Fluctuation Events (Post-MVP)

Optional feature - implement after core functionality is stable.

- [ ] 11-1. Create `PriceEventManager`
  - Timer: trigger event every 20 minutes (24,000 ticks)
  - Random price change: +/- 10-30% for each item
  - Event duration: 5 minutes (6,000 ticks)
- [ ] 11-2. Broadcast event start/end messages to all players
- [ ] 11-3. Sync current prices to clients for HUD/UI updates
- [ ] 11-4. Restore original prices when event ends

## MVP Scope

The MVP includes Phases 1-10. Phase 11 (Price Fluctuation Events) is deferred to post-MVP.

Within MVP, the following simplifications apply:
- Selling UI uses Option B (item-use on merchant) instead of a full trading GUI
- Game Start/Reset Block interactions execute immediately without confirmation prompts (confirmation can be added later)
- Market structure may be a simple placeholder building initially, refined in later iterations

## Implementation Order

Recommended order to enable incremental testing:

1. **Phase 1** - Entry points (required for everything)
2. **Phase 2** - Game state (core system)
3. **Phase 3** - Custom items (Miner's Pickaxe)
4. **Phase 4** - Custom blocks (Game Start/Reset)
5. **Phase 5** - Merchant entity
6. **Phase 6** - Selling system
7. **Phase 7** - HUD display
8. **Phase 9** - Game flow (start/end/reset)
9. **Phase 10** - Player spawn and effects
10. **Phase 8** - Market structure (can be tested last with manual placement)
11. **Phase 11** - Price events (post-MVP)

## File Structure (Expected)

```
common-shared/src/main/java/com/minersmarket/
├── MinersMarket.java              # Common mod initializer
├── registry/
│   ├── ModItems.java
│   ├── ModBlocks.java
│   ├── ModEntityTypes.java
│   └── ModCreativeTab.java
├── state/
│   ├── GameState.java
│   ├── GameStateManager.java
│   ├── GameStateSavedData.java
│   └── ClientGameState.java
├── entity/
│   └── MerchantEntity.java
├── block/
│   ├── GameStartBlock.java
│   └── GameResetBlock.java
├── item/
│   └── MinersPickaxeItem.java
├── trade/
│   └── PriceList.java
├── network/
│   └── GameStateSyncPacket.java
└── event/
    ├── PlayerSpawnHandler.java
    └── GameTickHandler.java

common-shared/src/main/resources/
└── (empty or shared resources)

common-1.21.1/src/main/resources/
├── assets/minersmarket/
│   ├── lang/
│   │   ├── en_us.json
│   │   └── ja_jp.json
│   ├── models/
│   │   ├── item/
│   │   │   ├── minerspickaxe.json
│   │   │   ├── game_start_block.json
│   │   │   └── game_reset_block.json
│   │   └── block/
│   │       ├── game_start_block.json
│   │       └── game_reset_block.json
│   ├── blockstates/
│   │   ├── game_start_block.json
│   │   └── game_reset_block.json
│   └── textures/
│       ├── item/
│       │   └── minerspickaxe.png
│       ├── block/
│       │   ├── game_start_block.png
│       │   └── game_reset_block.png
│       └── gui/
│           └── coin.png
└── data/minersmarket/
    ├── loot_table/blocks/
    │   ├── game_start_block.json
    │   └── game_reset_block.json
    ├── structure/
    │   └── market.nbt
    └── worldgen/
        ├── structure/
        │   └── market.json
        ├── structure_set/
        │   └── market.json
        └── template_pool/
            └── market.json

fabric-base/src/main/java/com/minersmarket/fabric/
├── MinersMarketFabric.java        # Fabric entry point
└── client/
    └── MinersMarketFabricClient.java

fabric-base/src/main/resources/
└── fabric.mod.json

fabric-1.21.1/src/main/resources/
└── (version-specific overrides if needed)

neoforge-base/src/main/java/com/minersmarket/neoforge/
├── MinersMarketNeoForge.java      # NeoForge entry point
└── client/
    └── MinersMarketNeoForgeClient.java

neoforge-base/src/main/resources/
└── META-INF/
    └── neoforge.mods.toml

neoforge-1.21.1/src/main/resources/
└── (version-specific overrides if needed)
```
