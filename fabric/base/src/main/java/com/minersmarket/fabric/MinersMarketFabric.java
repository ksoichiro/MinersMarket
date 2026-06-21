package com.minersmarket.fabric;

import com.minersmarket.MinersMarket;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;

public class MinersMarketFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        FabricPlatform.registerAll();
        FabricPlatform.registerNetworking();
        FabricPlatform.registerEvents();
        MinersMarket.init(FabricLoader.getInstance().getConfigDir());
    }
}
