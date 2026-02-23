package com.minersmarket.neoforge.client;

import com.minersmarket.neoforge.NeoForgePlatform;
import net.neoforged.bus.api.IEventBus;

public class MinersMarketNeoForgeClient {
    public static void init(IEventBus modBus) {
        NeoForgePlatform.registerClient(modBus);
    }
}
