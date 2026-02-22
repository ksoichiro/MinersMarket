package com.minersmarket.entity;

import com.minersmarket.MinersMarket;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

@Environment(EnvType.CLIENT)
public class MerchantModel extends EntityModel<MerchantRenderState> {
    public static final ModelLayerLocation LAYER_LOCATION =
            new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(MinersMarket.MOD_ID, "merchant"), "main");

    private final ModelPart head;
    private final ModelPart nose;
    private final ModelPart headwear;
    private final ModelPart headwear2;
    private final ModelPart body;
    private final ModelPart bodywear;
    private final ModelPart arms;
    private final ModelPart right_leg;
    private final ModelPart left_leg;

    public MerchantModel(ModelPart root) {
        super(root);
        this.head = root.getChild("head");
        this.nose = root.getChild("nose");
        this.headwear = root.getChild("headwear");
        this.headwear2 = root.getChild("headwear2");
        this.body = root.getChild("body");
        this.bodywear = root.getChild("bodywear");
        this.arms = root.getChild("arms");
        this.right_leg = root.getChild("right_leg");
        this.left_leg = root.getChild("left_leg");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();

        partdefinition.addOrReplaceChild("head",
                CubeListBuilder.create().texOffs(0, 0)
                        .addBox(-4.0F, -10.0F, -4.0F, 8.0F, 10.0F, 8.0F, new CubeDeformation(0.0F)),
                PartPose.offset(0.0F, 0.0F, 0.0F));

        partdefinition.addOrReplaceChild("nose",
                CubeListBuilder.create().texOffs(24, 0)
                        .addBox(-1.0F, -1.0F, -6.0F, 2.0F, 4.0F, 2.0F, new CubeDeformation(0.0F)),
                PartPose.offset(0.0F, -2.0F, 0.0F));

        partdefinition.addOrReplaceChild("headwear",
                CubeListBuilder.create().texOffs(32, 0)
                        .addBox(-4.0F, -10.0F, -4.0F, 8.0F, 10.0F, 8.0F, new CubeDeformation(0.51F)),
                PartPose.offset(0.0F, 0.0F, 0.0F));

        partdefinition.addOrReplaceChild("headwear2",
                CubeListBuilder.create().texOffs(30, 47)
                        .addBox(-8.0F, -8.0F, -6.0F, 16.0F, 16.0F, 1.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, -1.5708F, 0.0F, 0.0F));

        partdefinition.addOrReplaceChild("body",
                CubeListBuilder.create().texOffs(16, 20)
                        .addBox(-4.0F, 0.0F, -3.0F, 8.0F, 12.0F, 6.0F, new CubeDeformation(0.0F)),
                PartPose.offset(0.0F, 0.0F, 0.0F));

        partdefinition.addOrReplaceChild("bodywear",
                CubeListBuilder.create().texOffs(0, 38)
                        .addBox(-4.0F, 0.0F, -3.0F, 8.0F, 20.0F, 6.0F, new CubeDeformation(0.5F)),
                PartPose.offset(0.0F, 0.0F, 0.0F));

        PartDefinition arms = partdefinition.addOrReplaceChild("arms",
                CubeListBuilder.create().texOffs(40, 38)
                        .addBox(-4.0F, 2.0F, -2.0F, 8.0F, 4.0F, 4.0F, new CubeDeformation(0.0F))
                        .texOffs(44, 22)
                        .addBox(-8.0F, -2.0F, -2.0F, 4.0F, 8.0F, 4.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.0F, 2.95F, -1.05F, -0.7505F, 0.0F, 0.0F));

        arms.addOrReplaceChild("mirrored",
                CubeListBuilder.create().texOffs(44, 22).mirror()
                        .addBox(4.0F, -23.05F, -3.05F, 4.0F, 8.0F, 4.0F, new CubeDeformation(0.0F)).mirror(false),
                PartPose.offset(0.0F, 21.05F, 1.05F));

        partdefinition.addOrReplaceChild("right_leg",
                CubeListBuilder.create().texOffs(0, 22)
                        .addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F, new CubeDeformation(0.0F)),
                PartPose.offset(-2.0F, 12.0F, 0.0F));

        partdefinition.addOrReplaceChild("left_leg",
                CubeListBuilder.create().texOffs(0, 22)
                        .addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F, new CubeDeformation(0.0F)),
                PartPose.offset(2.0F, 12.0F, 0.0F));

        return LayerDefinition.create(meshdefinition, 64, 64);
    }

    @Override
    public void setupAnim(MerchantRenderState state) {
        super.setupAnim(state);

        // Head follows player look direction
        this.head.yRot = state.yRot * ((float) Math.PI / 180F);
        this.head.xRot = state.xRot * ((float) Math.PI / 180F);

        // Head shake animation when unhappy
        if (state.unhappyCounter > 0) {
            this.head.zRot = 0.3F * Mth.sin(state.ageInTicks * 0.7F);
        } else {
            this.head.zRot = 0.0F;
        }

        // Headwear follows head
        this.headwear.yRot = this.head.yRot;
        this.headwear.xRot = this.head.xRot;
        this.headwear.zRot = this.head.zRot;

        // Nose follows head
        this.nose.yRot = this.head.yRot;
        this.nose.xRot = this.head.xRot;
        this.nose.zRot = this.head.zRot;

        // Walking leg animation
        this.right_leg.xRot = Mth.cos(state.walkAnimationPos * 0.6662F) * 1.4F * state.walkAnimationSpeed;
        this.left_leg.xRot = Mth.cos(state.walkAnimationPos * 0.6662F + (float) Math.PI) * 1.4F * state.walkAnimationSpeed;
    }
}
