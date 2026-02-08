package com.minersmarket.item;

import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.crafting.Ingredient;

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
            return 1.0f;
        }

        @Override
        public int getLevel() {
            return 4; // Netherite level
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
        // attackDamage=1.0F (same as stone pickaxe), attackSpeed=-2.8F
        super(TIER, 1, -2.8F, properties);
    }
}
