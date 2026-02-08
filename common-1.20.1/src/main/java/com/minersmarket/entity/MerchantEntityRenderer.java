package com.minersmarket.entity;

import com.minersmarket.MinersMarket;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

@Environment(EnvType.CLIENT)
public class MerchantEntityRenderer extends MobRenderer<MerchantEntity, MerchantModel> {
    private static final ResourceLocation TEXTURE =
            new ResourceLocation(MinersMarket.MOD_ID, "textures/entity/merchant.png");

    public MerchantEntityRenderer(EntityRendererProvider.Context context) {
        super(context, new MerchantModel(context.bakeLayer(MerchantModel.LAYER_LOCATION)), 0.5F);
    }

    @Override
    public ResourceLocation getTextureLocation(MerchantEntity entity) {
        return TEXTURE;
    }
}
