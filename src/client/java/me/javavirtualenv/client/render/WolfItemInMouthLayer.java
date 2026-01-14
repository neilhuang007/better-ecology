package me.javavirtualenv.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import me.javavirtualenv.mixin.client.WolfModelAccessor;
import net.minecraft.client.model.WolfModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/**
 * Render layer that shows items held in a wolf's mouth.
 *
 * <p>This replicates the vanilla fox behavior where items picked up
 * are visually rendered in the animal's mouth, providing better
 * visual feedback to players about what the animal is carrying.
 *
 * <p>This is more "vanilla-like" because:
 * <ul>
 *   <li>Foxes already have this behavior in vanilla Minecraft</li>
 *   <li>Wolves are similar to foxes (canids) so the behavior makes sense</li>
 *   <li>It provides clear visual feedback without UI elements</li>
 *   <li>It uses existing item rendering systems</li>
 * </ul>
 *
 * <p>Implementation based on vanilla FoxHeldItemLayer for consistency.
 */
public class WolfItemInMouthLayer extends RenderLayer<Wolf, WolfModel<Wolf>> {

    private final ItemInHandRenderer itemInHandRenderer;

    public WolfItemInMouthLayer(RenderLayerParent<Wolf, WolfModel<Wolf>> renderer, ItemInHandRenderer itemInHandRenderer) {
        super(renderer);
        this.itemInHandRenderer = itemInHandRenderer;
    }

    @Override
    public void render(
            PoseStack poseStack,
            MultiBufferSource buffer,
            int packedLight,
            Wolf wolf,
            float limbSwing,
            float limbSwingAmount,
            float partialTick,
            float ageInTicks,
            float netHeadYaw,
            float headPitch) {

        // Get the item the wolf is holding
        ItemStack heldItem = wolf.getItemBySlot(EquipmentSlot.MAINHAND);

        if (heldItem.isEmpty()) {
            return;
        }

        boolean isBaby = wolf.isBaby();
        boolean isSitting = wolf.isInSittingPose();

        poseStack.pushPose();

        // Scale down for baby wolves
        if (isBaby) {
            poseStack.scale(0.75F, 0.75F, 0.75F);
            poseStack.translate(0.0F, 0.5F, 0.209375F);
        }

        // Get the head model part via accessor
        WolfModel<Wolf> model = this.getParentModel();
        ModelPart head = ((WolfModelAccessor) model).betterEcology$getHead();

        // Translate to head position (convert from model units to world units)
        poseStack.translate(head.x / 16.0F, head.y / 16.0F, head.z / 16.0F);

        // Apply head roll angle (for head tilting animations)
        float headRoll = wolf.getHeadRollAngle(partialTick);
        poseStack.mulPose(Axis.ZP.rotation(headRoll));

        // Apply head yaw and pitch
        poseStack.mulPose(Axis.YP.rotationDegrees(netHeadYaw));
        poseStack.mulPose(Axis.XP.rotationDegrees(headPitch));

        // Position the item in the mouth based on wolf state
        if (isBaby) {
            if (isSitting) {
                poseStack.translate(0.1F, 0.26F, 0.15F);
            } else {
                poseStack.translate(0.06F, 0.26F, -0.5F);
            }
        } else if (isSitting) {
            // Sitting wolves have a different head position
            poseStack.translate(0.1F, 0.26F, 0.22F);
        } else {
            // Standing/walking wolves
            poseStack.translate(0.06F, 0.27F, -0.5F);
        }

        // Rotate item to lie flat in mouth
        poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));

        // If sitting, rotate to match head angle
        if (isSitting) {
            poseStack.mulPose(Axis.ZP.rotationDegrees(90.0F));
        }

        // Render the item
        this.itemInHandRenderer.renderItem(wolf, heldItem, ItemDisplayContext.GROUND, false, poseStack, buffer, packedLight);

        poseStack.popPose();
    }
}
