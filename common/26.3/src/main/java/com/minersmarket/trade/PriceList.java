package com.minersmarket.trade;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import java.util.LinkedHashMap;
import java.util.Map;

public class PriceList {
    private static final Map<Item, Integer> PRICES = new LinkedHashMap<>();

    static {
        PRICES.put(Items.COAL, 1);
        PRICES.put(Items.RAW_COPPER, 1);
        PRICES.put(Items.COPPER_INGOT, 2);
        PRICES.put(Items.LAPIS_LAZULI, 3);
        PRICES.put(Items.RAW_IRON, 5);
        PRICES.put(Items.IRON_INGOT, 10);
        PRICES.put(Items.RAW_GOLD, 7);
        PRICES.put(Items.GOLD_INGOT, 15);
        PRICES.put(Items.REDSTONE, 5);
        PRICES.put(Items.DIAMOND, 30);
        PRICES.put(Items.EMERALD, 10);
        PRICES.put(Items.AMETHYST_SHARD, 10);
        PRICES.put(Items.NETHERITE_INGOT, 100);
    }

    public static int getPrice(Item item) {
        return PRICES.getOrDefault(item, 0);
    }

    public static boolean isSellable(Item item) {
        return PRICES.containsKey(item);
    }

    public static Map<Item, Integer> getAllPrices() {
        return PRICES;
    }
}
