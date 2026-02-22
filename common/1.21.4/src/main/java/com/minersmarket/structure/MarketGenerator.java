package com.minersmarket.structure;

import com.minersmarket.MinersMarket;
import com.minersmarket.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import net.minecraft.world.phys.AABB;

import java.util.List;
import java.util.Optional;

public class MarketGenerator {
    private static final ResourceLocation TEMPLATE_ID =
            ResourceLocation.fromNamespaceAndPath(MinersMarket.MOD_ID, "market");
    private static final int FILL_DEPTH = 10;
    // UPDATE_CLIENTS | UPDATE_KNOWN_SHAPE: suppress shape update propagation so
    // multi-block structures (doors, etc.) don't break during placement.
    private static final int BLOCK_UPDATE_FLAGS = 2 | 16;
    private static final int CLEANUP_EXTEND = 5;
    private static final int SEARCH_RADIUS = 48;
    private static final int SEARCH_STEP = 4;
    // Y offset from structure origin to player foot level (merchants are at Y=2 in the template)
    private static final int FLOOR_HEIGHT = 2;
    private static final int PLAYER_COUNT = 8;

    /**
     * Place the market NBT structure at the best location near the given X/Z.
     * Searches for a flat, above-sea-level area within a few chunks.
     * Sets the world spawn inside the market.
     *
     * @return true if successfully placed, false if template not found
     */
    public static boolean generate(ServerLevel level, BlockPos center) {
        StructureTemplateManager templateManager = level.getStructureManager();
        Optional<StructureTemplate> template = templateManager.get(TEMPLATE_ID);

        if (template.isEmpty()) {
            MinersMarket.LOGGER.warn("Market template not found: {}", TEMPLATE_ID);
            return false;
        }

        StructureTemplate tmpl = template.get();
        Vec3i size = tmpl.getSize();

        // Find a suitable flat location near the center
        BlockPos bestOrigin = findSuitableLocation(level, center, size);

        // Find minimum solid ground placement Y across the footprint
        int minPlacementY = Integer.MAX_VALUE;
        for (int x = 0; x < size.getX(); x++) {
            for (int z = 0; z < size.getZ(); z++) {
                int placementY = findPlacementY(level, bestOrigin.getX() + x, bestOrigin.getZ() + z);
                minPlacementY = Math.min(minPlacementY, placementY);
            }
        }

        if (minPlacementY == Integer.MAX_VALUE) {
            minPlacementY = level.getSeaLevel();
        }

        BlockPos placePos = new BlockPos(bestOrigin.getX(), minPlacementY, bestOrigin.getZ());

        // Place the structure
        StructurePlaceSettings settings = new StructurePlaceSettings()
                .setIgnoreEntities(false);
        tmpl.placeInWorld(level, placePos, placePos, settings, level.random, BLOCK_UPDATE_FLAGS);
        removeDroppedItems(level, placePos, size);

        // Fill gaps below the structure floor
        for (int x = 0; x < size.getX(); x++) {
            for (int z = 0; z < size.getZ(); z++) {
                for (int dy = 1; dy <= FILL_DEPTH; dy++) {
                    BlockPos fillPos = new BlockPos(bestOrigin.getX() + x, minPlacementY - dy, bestOrigin.getZ() + z);
                    BlockState state = level.getBlockState(fillPos);
                    if (state.isCollisionShapeFullBlock(level, fillPos)) {
                        break;
                    }
                    level.setBlock(fillPos, Blocks.DIRT.defaultBlockState(), BLOCK_UPDATE_FLAGS);
                }
            }
        }
        removeDroppedItems(level, placePos, size);

        // Fill large chest with equipment for players
        fillChest(level, placePos, size);

        // Set world spawn inside the market (center, above the floor)
        BlockPos spawnPos = new BlockPos(
                placePos.getX() + size.getX() / 2,
                placePos.getY() + FLOOR_HEIGHT,
                placePos.getZ() + size.getZ() / 2
        );
        level.setDefaultSpawnPos(spawnPos, 0);

        // Set spawn radius to 0 so players spawn exactly at the set position
        level.getGameRules().getRule(net.minecraft.world.level.GameRules.RULE_SPAWN_RADIUS).set(0, level.getServer());

        MinersMarket.LOGGER.info("Market placed at {}, spawn set to {}", placePos, spawnPos);
        return true;
    }

    /**
     * Search for a flat, above-sea-level location near the center.
     * Evaluates height variance across the structure footprint.
     */
    private static BlockPos findSuitableLocation(ServerLevel level, BlockPos center, Vec3i size) {
        int seaLevel = level.getSeaLevel();
        BlockPos bestPos = center;
        int bestVariance = Integer.MAX_VALUE;

        for (int dx = -SEARCH_RADIUS; dx <= SEARCH_RADIUS; dx += SEARCH_STEP) {
            for (int dz = -SEARCH_RADIUS; dz <= SEARCH_RADIUS; dz += SEARCH_STEP) {
                int cx = center.getX() + dx;
                int cz = center.getZ() + dz;

                int minY = Integer.MAX_VALUE;
                int maxY = Integer.MIN_VALUE;
                boolean belowSea = false;

                // Sample heights across footprint (every 2 blocks for speed)
                for (int x = 0; x < size.getX(); x += 2) {
                    for (int z = 0; z < size.getZ(); z += 2) {
                        int y = findPlacementY(level, cx + x, cz + z);
                        minY = Math.min(minY, y);
                        maxY = Math.max(maxY, y);
                        if (y < seaLevel) {
                            belowSea = true;
                        }
                    }
                }

                if (belowSea || minY == Integer.MAX_VALUE) continue;

                int variance = maxY - minY;
                if (variance < bestVariance) {
                    bestVariance = variance;
                    bestPos = new BlockPos(cx, 0, cz);
                }
            }
        }

        return bestPos;
    }

    private static void removeDroppedItems(ServerLevel level, BlockPos placePos, Vec3i size) {
        AABB area = new AABB(
                placePos.getX() - CLEANUP_EXTEND,
                placePos.getY() - FILL_DEPTH,
                placePos.getZ() - CLEANUP_EXTEND,
                placePos.getX() + size.getX() + CLEANUP_EXTEND,
                placePos.getY() + size.getY() + FILL_DEPTH,
                placePos.getZ() + size.getZ() + CLEANUP_EXTEND);

        List<ItemEntity> items = level.getEntitiesOfClass(ItemEntity.class, area);
        for (ItemEntity item : items) {
            item.discard();
        }
    }

    /**
     * Find the first large chest in the structure and fill it with equipment for all players.
     */
    private static void fillChest(ServerLevel level, BlockPos placePos, Vec3i size) {
        for (int x = 0; x < size.getX(); x++) {
            for (int y = 0; y < size.getY(); y++) {
                for (int z = 0; z < size.getZ(); z++) {
                    BlockPos pos = placePos.offset(x, y, z);
                    BlockState state = level.getBlockState(pos);
                    if (state.getBlock() instanceof ChestBlock chestBlock) {
                        Container container = ChestBlock.getContainer(chestBlock, state, level, pos, true);
                        if (container != null && container.getContainerSize() > 27) {
                            fillChestWithEquipment(level, container);
                            return;
                        }
                    }
                }
            }
        }
    }

    private static void fillChestWithEquipment(ServerLevel level, Container container) {
        int slot = 0;
        for (int i = 0; i < PLAYER_COUNT; i++) {
            ItemStack pickaxe = new ItemStack(ModItems.MINERS_PICKAXE.get());
            pickaxe.enchant(
                    level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT)
                            .getOrThrow(Enchantments.FORTUNE),
                    3
            );
            container.setItem(slot++, pickaxe);
        }
        for (int i = 0; i < PLAYER_COUNT; i++) {
            container.setItem(slot++, new ItemStack(Items.BREAD, 64));
        }
    }

    private static int findPlacementY(ServerLevel level, int x, int z) {
        int y = level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z);
        while (y > level.getMinY()) {
            BlockPos below = new BlockPos(x, y - 1, z);
            BlockState state = level.getBlockState(below);
            if (state.isCollisionShapeFullBlock(level, below)) {
                return y;
            }
            y--;
        }
        return y;
    }
}
