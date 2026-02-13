package com.minersmarket.neoforge;

import com.minersmarket.MinersMarket;
import com.minersmarket.neoforge.client.MinersMarketNeoForgeClient;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;

@Mod(MinersMarket.MOD_ID)
public class MinersMarketNeoForge {
    public MinersMarketNeoForge() {
        MinersMarket.init();
        if (FMLEnvironment.getDist() == Dist.CLIENT) {
            MinersMarketNeoForgeClient.init();
        }
    }
}
