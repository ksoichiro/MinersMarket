package com.minersmarket.neoforge;

import com.minersmarket.MinersMarket;
import com.minersmarket.neoforge.client.MinersMarketNeoForgeClient;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.fml.loading.FMLEnvironment;

@Mod(MinersMarket.MOD_ID)
public class MinersMarketNeoForge {
    public MinersMarketNeoForge(IEventBus modBus, ModContainer container) {
        NeoForgePlatform.registerAll(modBus);
        NeoForgePlatform.registerEvents();
        MinersMarket.init(FMLPaths.CONFIGDIR.get());
        if (FMLEnvironment.getDist() == Dist.CLIENT) {
            MinersMarketNeoForgeClient.init(modBus, container);
        }
    }
}
