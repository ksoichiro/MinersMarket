package com.minersmarket.structure;

import com.minersmarket.registry.ModBlocks;
import com.minersmarket.registry.ModEntityTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.block.Blocks;

public class MarketGenerator {
    private static final int WIDTH = 9;
    private static final int DEPTH = 9;
    private static final int WALL_HEIGHT = 4;

    /**
     * Generate a simple market building at the given position.
     * The position is the front-left corner of the building.
     */
    public static void generate(ServerLevel level, BlockPos origin) {
        // Floor
        for (int x = 0; x < WIDTH; x++) {
            for (int z = 0; z < DEPTH; z++) {
                level.setBlock(origin.offset(x, 0, z), Blocks.STONE_BRICKS.defaultBlockState(), 3);
            }
        }

        // Walls
        for (int y = 1; y <= WALL_HEIGHT; y++) {
            for (int x = 0; x < WIDTH; x++) {
                for (int z = 0; z < DEPTH; z++) {
                    boolean isWall = x == 0 || x == WIDTH - 1 || z == 0 || z == DEPTH - 1;
                    if (!isWall) continue;

                    // Entrance: 3-block wide opening on the front (z=0), leaving y=1,2,3 open
                    if (z == 0 && x >= 3 && x <= 5 && y <= 3) {
                        continue;
                    }

                    level.setBlock(origin.offset(x, y, z), Blocks.STONE_BRICKS.defaultBlockState(), 3);
                }
            }
        }

        // Roof (flat)
        for (int x = 0; x < WIDTH; x++) {
            for (int z = 0; z < DEPTH; z++) {
                level.setBlock(origin.offset(x, WALL_HEIGHT + 1, z), Blocks.DARK_OAK_PLANKS.defaultBlockState(), 3);
            }
        }

        // Clear interior
        for (int y = 1; y <= WALL_HEIGHT; y++) {
            for (int x = 1; x < WIDTH - 1; x++) {
                for (int z = 1; z < DEPTH - 1; z++) {
                    level.setBlock(origin.offset(x, y, z), Blocks.AIR.defaultBlockState(), 3);
                }
            }
        }

        // Lanterns on walls
        level.setBlock(origin.offset(1, 3, 1), Blocks.LANTERN.defaultBlockState(), 3);
        level.setBlock(origin.offset(WIDTH - 2, 3, 1), Blocks.LANTERN.defaultBlockState(), 3);
        level.setBlock(origin.offset(1, 3, DEPTH - 2), Blocks.LANTERN.defaultBlockState(), 3);
        level.setBlock(origin.offset(WIDTH - 2, 3, DEPTH - 2), Blocks.LANTERN.defaultBlockState(), 3);

        // Game Start Block (left of entrance inside)
        level.setBlock(origin.offset(1, 1, 1), ModBlocks.GAME_START_BLOCK.get().defaultBlockState(), 3);

        // Game Reset Block (right of entrance inside)
        level.setBlock(origin.offset(WIDTH - 2, 1, 1), ModBlocks.GAME_RESET_BLOCK.get().defaultBlockState(), 3);

        // Merchant entity (center back of building)
        BlockPos merchantPos = origin.offset(WIDTH / 2, 1, DEPTH - 2);
        var merchant = ModEntityTypes.MERCHANT.get().create(level, null, merchantPos, MobSpawnType.COMMAND, false, false);
        if (merchant != null) {
            merchant.setPos(merchantPos.getX() + 0.5, merchantPos.getY(), merchantPos.getZ() + 0.5);
            level.addFreshEntity(merchant);
        }
    }
}
