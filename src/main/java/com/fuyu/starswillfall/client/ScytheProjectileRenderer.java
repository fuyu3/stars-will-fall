package com.fuyu.starswillfall.client;

import com.fuyu.starswillfall.scythe.ScytheProjectile;
import com.fuyu.starswillfall.scythe.ScytheTuning;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemDisplayContext;

/**
 * Desenha a foice arremessada: o próprio item, deitado e girando como um bumerangue.
 * Usa o modelo do item no contexto "fixed" (aba Display > Frame do Blockbench), então um modelo 3D
 * da foice aparece aqui sem mudar nada neste arquivo.
 */
public class ScytheProjectileRenderer extends EntityRenderer<ScytheProjectile> {
    private static final float SCALE = 2.0F;

    private final ItemRenderer itemRenderer;

    public ScytheProjectileRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.itemRenderer = context.getItemRenderer();
    }

    @Override
    public void render(ScytheProjectile entity, float entityYaw, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffer, int packedLight) {
        poseStack.pushPose();
        poseStack.translate(0.0D, entity.getBbHeight() / 2.0D, 0.0D);
        poseStack.mulPose(Axis.XP.rotationDegrees(90.0F)); // deita o item na horizontal
        poseStack.mulPose(Axis.ZP.rotationDegrees((entity.tickCount + partialTick) * ScytheTuning.BOOMERANG_SPIN_DEGREES));
        poseStack.scale(SCALE, SCALE, SCALE);
        this.itemRenderer.renderStatic(entity.getItem(), ItemDisplayContext.FIXED, packedLight,
                OverlayTexture.NO_OVERLAY, poseStack, buffer, entity.level(), entity.getId());
        poseStack.popPose();
        super.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(ScytheProjectile entity) {
        return InventoryMenu.BLOCK_ATLAS;
    }
}
