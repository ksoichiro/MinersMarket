package com.minersmarket.item;

import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.ToolMaterial;

public class MinersPickaxeItem extends PickaxeItem {
    public static final ToolMaterial MATERIAL = new ToolMaterial(
            BlockTags.INCORRECT_FOR_NETHERITE_TOOL,
            4096,     // uses (durability)
            12.0f,    // speed (mining speed)
            1.0f,     // attackDamageBonus
            22,       // enchantmentValue
            ItemTags.STONE_TOOL_MATERIALS // repairItems (placeholder, not practically repairable)
    );

    public MinersPickaxeItem(Properties properties) {
        super(MATERIAL, 1.0F, -2.8F, properties);
    }
}
