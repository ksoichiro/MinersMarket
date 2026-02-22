package com.minersmarket.fabric.client;

import com.minersmarket.MinersMarket;
import net.fabricmc.api.ClientModInitializer;

public class MinersMarketFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        MinersMarket.initClient();
    }
}
