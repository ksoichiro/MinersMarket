# Miner's Market

## Theme

> Collect the ores the market demands and deliver them before anyone else

## Setting and Basic Rules

- The world has a "market" where players can sell ores
- Purchase prices are set for various ores; selling them adds (ore quantity x purchase price) to the player's sales amount
- The first player to reach a sales amount of 10,000 wins, ending the game
- Mod name: Miner's Market
- Mod ID: minersmarket

## Gameplay Image

- In multiplayer, players compete to be the first to finish
- In singleplayer, it can be enjoyed as an RTA (Real Time Attack) to minimize completion time

## Market Structure

- One structure per world, generated once at the initial spawn point
	- ID: minersmarket:market
- Generated only once in a new world; not generated in existing worlds
	- Can be manually placed via command: /place structure minersmarket:market
- Players initially spawn at a designated location near the market
- This means the initial spawn point is fixed at a common location for all players
- Upon death, players respawn at this initial spawn point
- However, players can set a different respawn point by using a bed

## Ore Purchase System at the Market

### Ore Purchase

- For the MVP, the purchase price list is fixed
- However, events that change purchase prices may occur (may be excluded from the MVP)
- In addition to raw ores, smelted items are also eligible for purchase
- Smelted items have higher purchase prices than raw ores
- The proceeds from selling only serve as a sales amount value and are not represented as an item like emeralds used in vanilla villager trading

### Sales Amount

- The player's own sales amount is always displayed on screen as a HUD element
- The sales amount is represented with a gold coin icon and a numerical value
- Other players' sales amounts cannot be viewed

### Market Merchant NPC

- A dedicated mob "Merchant" using the villager model is placed inside the market structure
	- ID: minersmarket:merchant
- Players can sell ores by interacting with the merchant
- Not affected by vanilla trading system limitations such as daily trade limits or price fluctuations
	- As mentioned above, purchase price fluctuation events may be provided, but as a system independent of vanilla trading
- Selling will use one of the following approaches, decided based on implementation difficulty and future extensibility:
	- Selling via a price list item and using items on the merchant
		- The price list is referenced as a book-like item and is not directly tied to a trading UI
		- Ore items are consumed/sold by using them on the merchant mob
	- Reusing the vanilla trading UI
		- If customization is possible
	- Custom trading UI
		- If implementing something close to the vanilla trading UI is not difficult

## Sellable Items and Purchase Price List

- Coal: 1
- Raw Copper: 3
- Copper Ingot: 5
- Lapis Lazuli: 3
- Raw Iron: 5
- Iron Ingot: 10
- Raw Gold: 7
- Gold Ingot: 15
- Redstone: 5
- Diamond: 30
- Emerald: 10
- Amethyst Shard: 10
- Netherite Ingot: 100

### Notes

- Items related to the above sellable items but not on the list (such as iron ore blocks, raw iron blocks, iron blocks, etc.) cannot be sold
- Netherite is priced high due to its difficulty to obtain, but this does not mean entering the Nether is a recommended play strategy

## Purchase Price Fluctuation Events

- May be excluded from the MVP if implementation is difficult
- A purchase price fluctuation event occurs once every 20 minutes
	- Prices increase or decrease by 10-30% (random)
- Events are active for 5 minutes only
- A message is displayed on all players' screens at the start and end of each event

## Game Start and End

- Start
	- A block is provided to declare the start of the game
		- ID: minersmarket:game_start_block
		- Name: EN: Game Start Block / JA: ゲーム開始ブロック
	- The Game Start Block is placed inside the market structure
	- Right-clicking the Game Start Block prompts whether to start the game
		- If this mechanism is difficult to implement, this confirmation step may be excluded from the MVP
	- Upon confirming the intent to start, a countdown begins, and the game enters the "In Progress" state when it reaches zero
	- The Game Start Block can only be used when the game is in the "Not Started" state
	- Play time is counted from game start and displayed on screen as a HUD element
- End
	- The game enters the "Ended" state when the first player reaches the target sales amount
		- Even if multiple players reach the target amount in the same tick, the first one detected is considered the winner
	- A message is displayed on all players' screens along with the winner's player name
	- The play time counter stops
- Reset
	- A reset operation returns the game to the "Not Started" state
	- A block is provided to reset the game
		- ID: minersmarket:game_reset_block
		- Name: EN: Game Reset Block / JA: ゲームリセットブロック
	- The Game Reset Block is placed inside the market structure
	- Right-clicking the Game Reset Block prompts whether to reset
		- If this mechanism is difficult to implement, the reset operation itself may be excluded from the MVP
	- Play time counter and sales amounts are cleared

## Game States

- Not Started
	- Internal State
		- Sales Amount: 0
		- Play Time Counter: 0:00 (0 min 0 sec)
	- Operations
		- Selling: Not allowed
		- Game Start Block: Usable
		- Game Reset Block: Not usable
- In Progress
	- Internal State
		- Sales Amount: Changes based on sales activity
		- Play Time Counter: Time elapsed since entering "In Progress" state (displayed as min:sec)
	- Operations
		- Selling: Allowed
		- Game Start Block: Not usable
		- Game Reset Block: Usable
- Ended
	- Internal State
		- Sales Amount: Retains the value from the "In Progress" state and does not change
		- Play Time Counter: Retains the value from the "In Progress" state and does not change
	- Operations
		- Selling: Not allowed
		- Game Start Block: Not usable
		- Game Reset Block: Usable

## Provided Items and Effects

- Players receive the following items upon initial spawn:
	- Pickaxe
		- A dedicated mining item
			- Mining speed: High
			- Durability: High
			- Fortune III effect (increased drop rate)
			- A new mining pickaxe is granted upon death
			- Players may also prepare their own pickaxes
			- ID: minersmarket:minerspickaxe
			- Name: EN: Miner's Pickaxe / JA: 採掘者のツルハシ
	- Food
		- Bread: 1 stack
- Players are automatically granted a permanent Night Vision effect (no time limit)

## HUD Display

- Sales Amount
	- Position: Top-right of screen, right-aligned
	- Format: 💰 3,250 / 10,000
		- If using the "💰" emoji is problematic, a dedicated image will be prepared similar to the health hearts
- Play Time (elapsed time since start)
	- Position: Top-right of screen, right-aligned, below the sales amount
	- Format: 00:00 (MM:SS)

## Architecture

- Minecraft mod using Architectury
- Compatible with both Fabric and NeoForge
- Initially implemented for Minecraft version 1.21.1
- The project will be configured with Gradle as a multi-project setup

## Directory structure

- common-shared
    - Common code without loader dependencies or version dependencies. Not a Gradle subproject, but incorporated as one of the srcDirs from each version-specific subproject
- common-1.21.1
    - Common code for Minecraft 1.21.1 without loader dependencies. Gradle subproject.
- fabric-base
    - Code for Fabric without Minecraft version dependencies. Gradle subproject.
- fabric-1.21.1
    - Code for Fabric and Minecraft 1.21.1. Gradle subproject. Depends on fabric-base.
- neoforge-base
    - Code for NeoForge without Minecraft version dependencies. Gradle subproject.
- neoforge-1.21.1
    - Code for NeoForge and Minecraft 1.21.1. Gradle subproject. Depends on neoforge-base.

## License

- LGPL-3.0-only
