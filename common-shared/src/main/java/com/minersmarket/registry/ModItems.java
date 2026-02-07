package com.minersmarket.registry;

import com.minersmarket.MinersMarket;
import com.minersmarket.item.MinersPickaxeItem;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.PickaxeItem;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(MinersMarket.MOD_ID, Registries.ITEM);

    public static final RegistrySupplier<Item> MINERS_PICKAXE = ITEMS.register("minerspickaxe",
            () -> new MinersPickaxeItem(
                    new Item.Properties()
                            .attributes(PickaxeItem.createAttributes(MinersPickaxeItem.TIER, 1.0F, -2.8F))
            ));

    public static void register() {
        ITEMS.register();
    }
}
