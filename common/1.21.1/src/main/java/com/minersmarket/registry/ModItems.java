package com.minersmarket.registry;

import com.minersmarket.item.MinersPickaxeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.PickaxeItem;

import java.util.function.Supplier;

public class ModItems {
    public static Supplier<Item> MINERS_PICKAXE;

    public static Item createMinersPickaxe() {
        return new MinersPickaxeItem(
                new Item.Properties()
                        .attributes(PickaxeItem.createAttributes(MinersPickaxeItem.TIER, 1.0F, -2.8F))
        );
    }
}
