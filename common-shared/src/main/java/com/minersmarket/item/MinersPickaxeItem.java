package com.minersmarket.item;

import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.Block;

public class MinersPickaxeItem extends PickaxeItem {
    public static final Tier TIER = new Tier() {
        @Override
        public int getUses() {
            return 4096;
        }

        @Override
        public float getSpeed() {
            return 12.0f;
        }

        @Override
        public float getAttackDamageBonus() {
            return 3.0f;
        }

        @Override
        public TagKey<Block> getIncorrectBlocksForDrops() {
            return BlockTags.INCORRECT_FOR_NETHERITE_TOOL;
        }

        @Override
        public int getEnchantmentValue() {
            return 22;
        }

        @Override
        public Ingredient getRepairIngredient() {
            return Ingredient.EMPTY;
        }
    };

    public MinersPickaxeItem(Properties properties) {
        super(TIER, properties);
    }
}
