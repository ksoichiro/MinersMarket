package com.minersmarket.fabric;

import com.minersmarket.MinersMarket;
import net.fabricmc.api.ModInitializer;

public class MinersMarketFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        MinersMarket.init();
    }
}
