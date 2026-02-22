package com.minersmarket.forge;

import com.minersmarket.MinersMarket;
import com.minersmarket.forge.client.MinersMarketForgeClient;
import dev.architectury.platform.forge.EventBuses;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;

@Mod(MinersMarket.MOD_ID)
public class MinersMarketForge {
    public MinersMarketForge() {
        EventBuses.registerModEventBus(MinersMarket.MOD_ID, FMLJavaModLoadingContext.get().getModEventBus());
        MinersMarket.init();
        if (FMLEnvironment.dist == Dist.CLIENT) {
            MinersMarketForgeClient.init();
        }
    }
}
