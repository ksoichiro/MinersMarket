package com.minersmarket.registry;

import com.minersmarket.entity.MerchantEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

import java.util.function.Supplier;

public class ModEntityTypes {
    public static Supplier<EntityType<MerchantEntity>> MERCHANT;

    public static EntityType<MerchantEntity> createMerchantEntityType() {
        return EntityType.Builder.<MerchantEntity>of(MerchantEntity::new, MobCategory.MISC)
                .sized(0.6F, 1.95F)
                .build("merchant");
    }
}
