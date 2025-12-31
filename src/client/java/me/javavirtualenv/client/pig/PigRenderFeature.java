package me.javavirtualenv.client.pig;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import me.javavirtualenv.pig.PigBehaviorHandle;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.animal.Pig;
import org.joml.Vector3f;

/**
 * Render feature that adds a mud overlay to pigs.
 * Applies a brown tint when pig has recently bathed in mud.
 */
public class PigRenderFeature extends RenderLayer<Pig, net.minecraft.client.model.EntityModel<Pig>> {

    private static final Vector3f MUD_COLOR = new Vector3f(0.4f, 0.3f, 0.2f);
    private static final int MAX_MUD_DURATION = 6000;

    public PigRenderFeature(RenderLayerParent<Pig, net.minecraft.client.model.EntityModel<Pig>> renderer) {
        super(renderer);
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource bufferSource, int packedLight,
                      Pig pig, float limbSwing, float limbSwingAmount, float partialTicks,
                      float ageInTicks, float netHeadYaw, float headPitch) {

        if (!PigBehaviorHandle.hasMudEffect(pig)) {
            return;
        }

        float mudIntensity = getMudIntensity(pig);
        if (mudIntensity <= 0.0f) {
            return;
        }

        poseStack.pushPose();

        VertexConsumer consumer = bufferSource.getBuffer(
            RenderType.entityTranslucent(
                this.getParentModel().getTextureLocation(pig)
            )
        );

        float alpha = mudIntensity * 0.4f;
        float red = MUD_COLOR.x() * alpha;
        float green = MUD_COLOR.y() * alpha;
        float blue = MUD_COLOR.z() * alpha;

        this.getParentModel().renderToBuffer(
            poseStack,
            consumer,
            packedLight,
            OverlayTexture.NO_OVERLAY,
            red, green, blue, alpha
        );

        poseStack.popPose();
    }

    private float getMudIntensity(Pig pig) {
        if (!(pig instanceof me.javavirtualenv.ecology.api.EcologyAccess access)) {
            return 0.0f;
        }

        var component = access.betterEcology$getEcologyComponent();
        if (component == null) {
            return 0.0f;
        }

        var pigData = component.getHandleTag("pig_behavior");
        int mudTimer = pigData.getInt("mudEffectTimer");

        if (mudTimer <= 0) {
            return 0.0f;
        }

        return Math.min(1.0f, (float) mudTimer / MAX_MUD_DURATION);
    }
}
