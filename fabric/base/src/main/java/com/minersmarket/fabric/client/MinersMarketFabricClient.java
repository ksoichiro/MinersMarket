package com.minersmarket.fabric.client;

import com.minersmarket.fabric.FabricPlatform;
import net.fabricmc.api.ClientModInitializer;

public class MinersMarketFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        FabricPlatform.registerClient();
    }
}
