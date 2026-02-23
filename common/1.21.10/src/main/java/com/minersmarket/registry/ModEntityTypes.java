package com.minersmarket.registry;

import com.minersmarket.MinersMarket;
import com.minersmarket.entity.MerchantEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

import java.util.function.Supplier;

public class ModEntityTypes {
    public static Supplier<EntityType<MerchantEntity>> MERCHANT;

    public static EntityType<MerchantEntity> createMerchantEntityType() {
        return EntityType.Builder.<MerchantEntity>of(MerchantEntity::new, MobCategory.MISC)
                .sized(0.6F, 1.95F)
                .build(ResourceKey.create(Registries.ENTITY_TYPE,
                        ResourceLocation.fromNamespaceAndPath(MinersMarket.MOD_ID, "merchant")));
    }
}
