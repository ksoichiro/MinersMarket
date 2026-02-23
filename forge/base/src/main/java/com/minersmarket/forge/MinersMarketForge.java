package com.minersmarket.forge;

import com.minersmarket.MinersMarket;
import com.minersmarket.forge.client.MinersMarketForgeClient;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;

@Mod(MinersMarket.MOD_ID)
public class MinersMarketForge {
    public MinersMarketForge() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        ForgePlatform.registerAll(modBus);
        ForgePlatform.registerEvents();
        MinersMarket.init();
        if (FMLEnvironment.dist == Dist.CLIENT) {
            MinersMarketForgeClient.init(modBus);
        }
    }
}
