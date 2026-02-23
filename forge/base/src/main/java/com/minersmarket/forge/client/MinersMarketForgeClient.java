package com.minersmarket.forge.client;

import com.minersmarket.forge.ForgePlatform;
import net.minecraftforge.eventbus.api.IEventBus;

public class MinersMarketForgeClient {
    public static void init(IEventBus modBus) {
        ForgePlatform.registerClient(modBus);
    }
}
