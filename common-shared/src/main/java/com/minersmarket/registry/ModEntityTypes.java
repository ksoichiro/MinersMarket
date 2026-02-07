package com.minersmarket.registry;

import com.minersmarket.MinersMarket;
import dev.architectury.registry.registries.DeferredRegister;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;

public class ModEntityTypes {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(MinersMarket.MOD_ID, Registries.ENTITY_TYPE);

    public static void register() {
        ENTITY_TYPES.register();
    }
}
