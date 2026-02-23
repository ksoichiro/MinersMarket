package com.minersmarket.registry;

import com.minersmarket.MinersMarket;
import com.minersmarket.item.MinersPickaxeItem;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;

import java.util.function.Supplier;

public class ModItems {
    public static Supplier<Item> MINERS_PICKAXE;

    public static Item createMinersPickaxe() {
        return new MinersPickaxeItem(new Item.Properties()
                .setId(ResourceKey.create(Registries.ITEM,
                        ResourceLocation.fromNamespaceAndPath(MinersMarket.MOD_ID, "minerspickaxe"))));
    }
}
