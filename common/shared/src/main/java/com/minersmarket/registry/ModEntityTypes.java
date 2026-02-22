package com.minersmarket.registry;

import com.minersmarket.MinersMarket;
import com.minersmarket.entity.MerchantEntity;
import dev.architectury.registry.level.entity.EntityAttributeRegistry;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;

public class ModEntityTypes {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(MinersMarket.MOD_ID, Registries.ENTITY_TYPE);

    public static final RegistrySupplier<EntityType<MerchantEntity>> MERCHANT =
            ENTITY_TYPES.register("merchant",
                    () -> EntityType.Builder.<MerchantEntity>of(MerchantEntity::new, MobCategory.MISC)
                            .sized(0.6F, 1.95F)
                            .build("merchant"));

    public static void register() {
        ENTITY_TYPES.register();
        EntityAttributeRegistry.register(MERCHANT, Mob::createMobAttributes);
    }
}
