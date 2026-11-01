package com.minersmarket.neoforge.client;

import com.minersmarket.neoforge.NeoForgePlatform;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;

public class MinersMarketNeoForgeClient {
    public static void init(IEventBus modBus, ModContainer container) {
        NeoForgePlatform.registerClient(modBus, container);
    }
}
