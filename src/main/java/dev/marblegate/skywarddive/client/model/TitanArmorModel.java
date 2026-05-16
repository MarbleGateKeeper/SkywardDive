package dev.marblegate.skywarddive.client.model;

import dev.marblegate.skywarddive.common.SkywardDive;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.resources.Identifier;

public class TitanArmorModel extends HumanoidModel<AvatarRenderState> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(
            Identifier.fromNamespaceAndPath(SkywardDive.MODID, "titan_armor"), "main");

    public TitanArmorModel(ModelPart root) {
        super(root);
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();

        PartDefinition Body = partdefinition.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0).addBox(-5.0F, -1.0F, -3.0F, 10.0F, 9.0F, 6.0F, new CubeDeformation(0.0F))
                .texOffs(0, 25).addBox(-2.5F, 0.0F, 3.0F, 5.0F, 4.0F, 3.0F, new CubeDeformation(0.0F))
                .texOffs(32, 0).addBox(-2.0F, -2.0F, 3.0F, 4.0F, 2.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.ZERO);

        PartDefinition cube_r1 = Body.addOrReplaceChild("cube_r1", CubeListBuilder.create().texOffs(30, 24).mirror().addBox(-1.0F, 0.0F, -1.0F, 3.0F, 3.0F, 4.0F, new CubeDeformation(0.001F)).mirror(false)
                .texOffs(30, 24).addBox(6.0F, 0.0F, -1.0F, 3.0F, 3.0F, 4.0F, new CubeDeformation(0.001F)), PartPose.offsetAndRotation(-4.0F, 1.0F, 4.0F, -0.2138F, 0.0F, 0.0F));

        PartDefinition cube_r2 = Body.addOrReplaceChild("cube_r2", CubeListBuilder.create().texOffs(0, 32).mirror().addBox(-3.0F, 2.0F, -2.0F, 1.0F, 3.0F, 4.0F, new CubeDeformation(0.0F)).mirror(false)
                .texOffs(0, 32).addBox(2.0F, 2.0F, -2.0F, 1.0F, 3.0F, 4.0F, new CubeDeformation(0.0F))
                .texOffs(16, 15).addBox(-3.0F, 0.0F, -2.0F, 6.0F, 2.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 3.0F, 5.0F, -0.3927F, 0.0F, 0.0F));

        PartDefinition cube_r3 = Body.addOrReplaceChild("cube_r3", CubeListBuilder.create().texOffs(30, 31).addBox(-1.5F, 0.0F, -1.5F, 3.0F, 4.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 5.0F, 3.75F, 0.1069F, 0.0F, 0.0F));

        PartDefinition leftlauncher = Body.addOrReplaceChild("leftlauncher", CubeListBuilder.create().texOffs(22, 37).addBox(0.0F, -2.0F, -1.0F, 0.0F, 2.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(5.0F, 3.0F, 5.0F, 0.0F, 0.0F, 0.2138F));

        PartDefinition group4 = leftlauncher.addOrReplaceChild("group4", CubeListBuilder.create().texOffs(32, 11).addBox(0.0F, -1.0F, -1.0F, 2.0F, 1.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -2.0F, 0.0F, 0.0F, 0.0F, -0.2138F));

        PartDefinition group5 = group4.addOrReplaceChild("group5", CubeListBuilder.create().texOffs(22, 37).addBox(0.9772F, 0.2122F, -1.0F, 0.0F, 2.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(1.0F, 0.0F, 0.0F, 0.0F, 0.0F, -0.2138F));

        PartDefinition group6 = group5.addOrReplaceChild("group6", CubeListBuilder.create().texOffs(0, 15).addBox(1.0F, -2.5F, -2.5F, 3.0F, 5.0F, 5.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 2.0F, 0.0F, 0.0F, 0.0F, 0.2138F));

        PartDefinition rightlauncher = Body.addOrReplaceChild("rightlauncher", CubeListBuilder.create().texOffs(22, 37).mirror().addBox(0.0F, -2.0F, -1.0F, 0.0F, 2.0F, 2.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offsetAndRotation(-5.0F, 3.0F, 5.0F, 0.0F, 0.0F, -0.2138F));

        PartDefinition group3 = rightlauncher.addOrReplaceChild("group3", CubeListBuilder.create().texOffs(32, 11).mirror().addBox(-2.0F, -1.0F, -1.0F, 2.0F, 1.0F, 2.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offsetAndRotation(0.0F, -2.0F, 0.0F, 0.0F, 0.0F, 0.2138F));

        PartDefinition group7 = group3.addOrReplaceChild("group7", CubeListBuilder.create().texOffs(22, 37).mirror().addBox(-0.9772F, 0.2122F, -1.0F, 0.0F, 2.0F, 2.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offsetAndRotation(-1.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.2138F));

        PartDefinition group8 = group7.addOrReplaceChild("group8", CubeListBuilder.create().texOffs(0, 15).mirror().addBox(-4.0F, -2.5F, -2.5F, 3.0F, 5.0F, 5.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offsetAndRotation(0.0F, 2.0F, 0.0F, 0.0F, 0.0F, -0.2138F));

        PartDefinition leftwing = Body.addOrReplaceChild("leftwing", CubeListBuilder.create().texOffs(32, 4).addBox(0.0F, -1.5F, -1.0F, 2.0F, 3.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(2.0F, -1.0F, 4.0F, 0.0F, -0.2138F, 0.0F));

        PartDefinition group = leftwing.addOrReplaceChild("group", CubeListBuilder.create().texOffs(16, 34).addBox(0.0F, 0.5F, -0.5F, 3.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
                .texOffs(16, 21).addBox(0.0F, -1.5F, -0.5F, 10.0F, 2.0F, 1.0F, new CubeDeformation(0.0F))
                .texOffs(36, 14).addBox(9.75F, -1.5F, -0.5F, 2.0F, 1.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(2.0F, 0.0F, 0.0F, 0.0F, 0.0F, -0.3927F));

        PartDefinition cube_r4 = group.addOrReplaceChild("cube_r4", CubeListBuilder.create().texOffs(36, 16).addBox(0.0F, -1.0F, -0.5F, 2.0F, 1.0F, 1.0F, new CubeDeformation(0.0125F)), PartPose.offsetAndRotation(10.0F, 0.5F, 0.0F, 0.0F, 0.0F, -0.5236F));

        PartDefinition cube_r5 = group.addOrReplaceChild("cube_r5", CubeListBuilder.create().texOffs(16, 36).addBox(0.0F, -1.0F, -0.5F, 2.0F, 1.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(3.0F, 1.5F, 0.0F, 0.0F, 0.0F, -0.5236F));

        PartDefinition rightwing = Body.addOrReplaceChild("rightwing", CubeListBuilder.create().texOffs(32, 4).mirror().addBox(-2.0F, -1.5F, -1.0F, 2.0F, 3.0F, 2.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offsetAndRotation(-2.0F, -1.0F, 4.0F, 0.0F, 0.2138F, 0.0F));

        PartDefinition group2 = rightwing.addOrReplaceChild("group2", CubeListBuilder.create().texOffs(16, 34).mirror().addBox(-3.0F, 0.5F, -0.5F, 3.0F, 1.0F, 1.0F, new CubeDeformation(0.0F)).mirror(false)
                .texOffs(16, 21).mirror().addBox(-10.0F, -1.5F, -0.5F, 10.0F, 2.0F, 1.0F, new CubeDeformation(0.0F)).mirror(false)
                .texOffs(36, 14).mirror().addBox(-11.75F, -1.5F, -0.5F, 2.0F, 1.0F, 1.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offsetAndRotation(-2.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.3927F));

        PartDefinition cube_r6 = group2.addOrReplaceChild("cube_r6", CubeListBuilder.create().texOffs(36, 16).mirror().addBox(-2.0F, -1.0F, -0.5F, 2.0F, 1.0F, 1.0F, new CubeDeformation(0.0125F)).mirror(false), PartPose.offsetAndRotation(-10.0F, 0.5F, 0.0F, 0.0F, 0.0F, 0.5236F));

        PartDefinition cube_r7 = group2.addOrReplaceChild("cube_r7", CubeListBuilder.create().texOffs(16, 36).mirror().addBox(-2.0F, -1.0F, -0.5F, 2.0F, 1.0F, 1.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offsetAndRotation(-3.0F, 1.5F, 0.0F, 0.0F, 0.0F, 0.5236F));

        PartDefinition leftwing2 = Body.addOrReplaceChild("leftwing2", CubeListBuilder.create().texOffs(10, 32).addBox(0.0F, -1.5F, -0.5F, 2.0F, 3.0F, 1.0F, new CubeDeformation(0.0F))
                .texOffs(32, 9).addBox(2.0F, 0.5F, -0.5F, 4.0F, 1.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(3.0F, 6.0F, 4.0F, 0.0F, -0.2138F, 0.2138F));

        PartDefinition cube_r8 = leftwing2.addOrReplaceChild("cube_r8", CubeListBuilder.create().texOffs(36, 18).addBox(-4.5F, 0.0F, -0.5F, 1.0F, 2.0F, 1.0F, new CubeDeformation(0.0F))
                .texOffs(24, 34).addBox(-4.0F, 0.0F, -0.5F, 2.0F, 2.0F, 1.0F, new CubeDeformation(0.001F))
                .texOffs(10, 36).addBox(-2.0F, 0.0F, -0.5F, 2.0F, 1.0F, 1.0F, new CubeDeformation(0.001F)), PartPose.offsetAndRotation(6.0F, 0.5F, 0.0F, 0.0F, 0.0F, 0.4625F));

        PartDefinition rightwing2 = Body.addOrReplaceChild("rightwing2", CubeListBuilder.create().texOffs(10, 32).mirror().addBox(-2.0F, -1.5F, -0.5F, 2.0F, 3.0F, 1.0F, new CubeDeformation(0.0F)).mirror(false)
                .texOffs(32, 9).mirror().addBox(-6.0F, 0.5F, -0.5F, 4.0F, 1.0F, 1.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offsetAndRotation(-3.0F, 6.0F, 4.0F, 0.0F, 0.2138F, -0.2138F));

        PartDefinition cube_r9 = rightwing2.addOrReplaceChild("cube_r9", CubeListBuilder.create().texOffs(36, 18).mirror().addBox(3.5F, 0.0F, -0.5F, 1.0F, 2.0F, 1.0F, new CubeDeformation(0.0F)).mirror(false)
                .texOffs(24, 34).mirror().addBox(2.0F, 0.0F, -0.5F, 2.0F, 2.0F, 1.0F, new CubeDeformation(0.001F)).mirror(false)
                .texOffs(10, 36).mirror().addBox(0.0F, 0.0F, -0.5F, 2.0F, 1.0F, 1.0F, new CubeDeformation(0.001F)).mirror(false), PartPose.offsetAndRotation(-6.0F, 0.5F, 0.0F, 0.0F, 0.0F, -0.4625F));

        PartDefinition RightArm = partdefinition.addOrReplaceChild("right_arm", CubeListBuilder.create(), PartPose.offset(-3.5F, +2.0F, 0.0F));

        PartDefinition cube_r10 = RightArm.addOrReplaceChild("cube_r10", CubeListBuilder.create().texOffs(16, 24).addBox(-1.0F, -2.5F, -2.5F, 2.0F, 5.0F, 5.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-3.25F, -1.0F, 0.0F, 0.0F, 0.0F, 0.3927F));

        PartDefinition LeftArm = partdefinition.addOrReplaceChild("left_arm", CubeListBuilder.create(), PartPose.offset(3.5F, +2.0F, 0.0F));

        PartDefinition cube_r11 = LeftArm.addOrReplaceChild("cube_r11", CubeListBuilder.create().texOffs(16, 24).mirror().addBox(-1.0F, -2.5F, -2.5F, 2.0F, 5.0F, 5.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offsetAndRotation(3.25F, -1.0F, 0.0F, 0.0F, 0.0F, -0.3927F));

        PartDefinition dummyHead = partdefinition.addOrReplaceChild("head", CubeListBuilder.create(), PartPose.ZERO);
        PartDefinition dummyHat = dummyHead.addOrReplaceChild("hat", CubeListBuilder.create(), PartPose.ZERO);
        PartDefinition dummyRightLeg = partdefinition.addOrReplaceChild("right_leg", CubeListBuilder.create(), PartPose.ZERO);
        PartDefinition dummyLeftLeg = partdefinition.addOrReplaceChild("left_leg", CubeListBuilder.create(), PartPose.ZERO);

        return LayerDefinition.create(meshdefinition, 64, 64);
    }
}
