package me.javavirtualenv.client.debug;

import com.mojang.blaze3d.vertex.PoseStack;
import me.javavirtualenv.ecology.EcologyComponent;
import me.javavirtualenv.ecology.api.EcologyAccess;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

import java.util.List;

/**
 * Renders ecology debug information (hunger, energy, condition, social, combat/retreat state)
 * above mobs when debug mode is enabled. Values are color-coded based on thresholds.
 * Combat state shown as [CBT], retreat state shown as [RET].
 */
public final class EcologyDebugRenderer {

    private static final double MAX_RENDER_DISTANCE = 32.0;
    private static final double MAX_RENDER_DISTANCE_SQUARED = MAX_RENDER_DISTANCE * MAX_RENDER_DISTANCE;

    private static final int COLOR_RED = 0xFF5555;
    private static final int COLOR_YELLOW = 0xFFFF55;
    private static final int COLOR_GREEN = 0x55FF55;
    private static final int COLOR_GRAY = 0xAAAAAA;

    private static final int THRESHOLD_LOW = 30;
    private static final int THRESHOLD_MEDIUM = 60;

    private EcologyDebugRenderer() {
        // Static utility class
    }

    /**
     * Registers the world render event for debug rendering.
     * Should be called during client initialization.
     */
    public static void register() {
        WorldRenderEvents.AFTER_ENTITIES.register(EcologyDebugRenderer::onRenderWorld);
    }

    /**
     * Called after entities are rendered to draw debug overlays.
     *
     * @param context the world render context
     */
    private static void onRenderWorld(WorldRenderContext context) {
        if (!DebugConfig.isDebugEnabled()) {
            return;
        }

        Minecraft client = Minecraft.getInstance();
        if (client.level == null || client.player == null) {
            return;
        }

        Vec3 cameraPos = context.camera().getPosition();
        List<Entity> nearbyEntities = getNearbyMobs(client, cameraPos);

        for (Entity entity : nearbyEntities) {
            if (!(entity instanceof Mob mob)) {
                continue;
            }

            double distanceSquared = mob.distanceToSqr(cameraPos);
            if (distanceSquared > MAX_RENDER_DISTANCE_SQUARED) {
                continue;
            }

            renderMobDebugInfo(context, mob, cameraPos);
        }
    }

    /**
     * Gets all mobs within render distance of the camera.
     *
     * @param client the Minecraft client
     * @param cameraPos the camera position
     * @return list of nearby entities
     */
    private static List<Entity> getNearbyMobs(Minecraft client, Vec3 cameraPos) {
        AABB searchBox = new AABB(
                cameraPos.x - MAX_RENDER_DISTANCE,
                cameraPos.y - MAX_RENDER_DISTANCE,
                cameraPos.z - MAX_RENDER_DISTANCE,
                cameraPos.x + MAX_RENDER_DISTANCE,
                cameraPos.y + MAX_RENDER_DISTANCE,
                cameraPos.z + MAX_RENDER_DISTANCE
        );

        return client.level.getEntities(client.player, searchBox, entity -> entity instanceof Mob);
    }

    /**
     * Renders debug information above a single mob.
     *
     * @param context the world render context
     * @param mob the mob to render debug info for
     * @param cameraPos the camera position
     */
    private static void renderMobDebugInfo(WorldRenderContext context, Mob mob, Vec3 cameraPos) {
        EcologyComponent component = getEcologyComponent(mob);
        if (component == null) {
            return;
        }

        String debugText = buildDebugText(component);
        if (debugText.isEmpty()) {
            return;
        }

        float partialTicks = context.tickCounter().getRealtimeDeltaTicks();
        Vec3 mobPos = mob.getPosition(partialTicks);
        double renderX = mobPos.x - cameraPos.x;
        double renderY = mobPos.y + mob.getBbHeight() + 0.5 - cameraPos.y;
        double renderZ = mobPos.z - cameraPos.z;

        PoseStack poseStack = context.matrixStack();
        poseStack.pushPose();
        poseStack.translate(renderX, renderY, renderZ);

        // Make text face the camera (billboard effect)
        poseStack.mulPose(context.camera().rotation());
        poseStack.scale(-0.025f, -0.025f, 0.025f);

        renderDebugText(poseStack, context.consumers(), component, debugText);

        poseStack.popPose();
    }

    /**
     * Gets the ecology component from a mob if available.
     *
     * @param mob the mob to get the component from
     * @return the ecology component or null if not available
     */
    private static EcologyComponent getEcologyComponent(Mob mob) {
        if (!(mob instanceof EcologyAccess access)) {
            return null;
        }
        return access.betterEcology$getEcologyComponent();
    }

    /**
     * Builds the debug text string from component data.
     *
     * @param component the ecology component
     * @return formatted debug text string
     */
    private static String buildDebugText(EcologyComponent component) {
        int hunger = getHungerValue(component);
        int energy = getEnergyValue(component);
        int condition = getConditionValue(component);
        int social = getSocialValue(component);

        // Only show values that are actually tracked (non-default)
        boolean hasHunger = hasHandleData(component, "hunger");
        boolean hasEnergy = hasHandleData(component, "energy");
        boolean hasCondition = hasHandleData(component, "condition");
        boolean hasSocial = hasHandleData(component, "social");

        if (!hasHunger && !hasEnergy && !hasCondition && !hasSocial) {
            return "";
        }

        StringBuilder builder = new StringBuilder();
        if (hasHunger) {
            builder.append("H:").append(hunger);
        }
        if (hasEnergy) {
            if (builder.length() > 0) builder.append(" ");
            builder.append("E:").append(energy);
        }
        if (hasCondition) {
            if (builder.length() > 0) builder.append(" ");
            builder.append("C:").append(condition);
        }
        if (hasSocial) {
            if (builder.length() > 0) builder.append(" ");
            builder.append("S:").append(social);
        }

        // Add combat/retreat state indicators
        if (component.state().isInCombat()) {
            builder.append(" [CBT]");
        }
        if (component.state().isRetreating()) {
            builder.append(" [RET]");
        }

        return builder.toString();
    }

    /**
     * Renders the debug text with color-coded values.
     *
     * @param poseStack the pose stack for transformations
     * @param bufferSource the buffer source for rendering
     * @param component the ecology component for value lookup
     * @param debugText the full debug text string
     */
    private static void renderDebugText(PoseStack poseStack, MultiBufferSource bufferSource,
                                         EcologyComponent component, String debugText) {
        Minecraft client = Minecraft.getInstance();
        Font font = client.font;

        Matrix4f matrix = poseStack.last().pose();
        float textWidth = font.width(debugText);
        float xOffset = -textWidth / 2.0f;

        // Render background for readability
        int backgroundColor = 0x80000000;
        font.drawInBatch(
                debugText,
                xOffset,
                0,
                0xFFFFFFFF,
                false,
                matrix,
                bufferSource,
                Font.DisplayMode.SEE_THROUGH,
                backgroundColor,
                15728880
        );

        // Render colored segments on top
        renderColoredSegments(poseStack, bufferSource, font, component, xOffset);
    }

    /**
     * Renders individual value segments with appropriate colors.
     *
     * @param poseStack the pose stack
     * @param bufferSource the buffer source
     * @param font the font renderer
     * @param component the ecology component
     * @param startX the starting X position
     */
    private static void renderColoredSegments(PoseStack poseStack, MultiBufferSource bufferSource,
                                               Font font, EcologyComponent component, float startX) {
        Matrix4f matrix = poseStack.last().pose();
        float currentX = startX;

        boolean hasHunger = hasHandleData(component, "hunger");
        boolean hasEnergy = hasHandleData(component, "energy");
        boolean hasCondition = hasHandleData(component, "condition");
        boolean hasSocial = hasHandleData(component, "social");

        if (hasHunger) {
            int hunger = getHungerValue(component);
            String hungerText = "H:" + hunger;
            int color = getColorForValue(hunger);
            font.drawInBatch(hungerText, currentX, 0, color, false, matrix, bufferSource,
                    Font.DisplayMode.NORMAL, 0, 15728880);
            currentX += font.width(hungerText + " ");
        }

        if (hasEnergy) {
            int energy = getEnergyValue(component);
            String energyText = "E:" + energy;
            int color = getColorForValue(energy);
            font.drawInBatch(energyText, currentX, 0, color, false, matrix, bufferSource,
                    Font.DisplayMode.NORMAL, 0, 15728880);
            currentX += font.width(energyText + " ");
        }

        if (hasCondition) {
            int condition = getConditionValue(component);
            String conditionText = "C:" + condition;
            int color = getColorForValue(condition);
            font.drawInBatch(conditionText, currentX, 0, color, false, matrix, bufferSource,
                    Font.DisplayMode.NORMAL, 0, 15728880);
            currentX += font.width(conditionText + " ");
        }

        if (hasSocial) {
            int social = getSocialValue(component);
            String socialText = "S:" + social;
            int color = getColorForValue(social);
            font.drawInBatch(socialText, currentX, 0, color, false, matrix, bufferSource,
                    Font.DisplayMode.NORMAL, 0, 15728880);
            currentX += font.width(socialText + " ");
        }

        // Render combat/retreat state indicators
        if (component.state().isInCombat()) {
            String combatText = "[CBT]";
            font.drawInBatch(combatText, currentX, 0, COLOR_RED, false, matrix, bufferSource,
                    Font.DisplayMode.NORMAL, 0, 15728880);
            currentX += font.width(combatText);
        }
        if (component.state().isRetreating()) {
            String retreatText = "[RET]";
            font.drawInBatch(retreatText, currentX, 0, COLOR_YELLOW, false, matrix, bufferSource,
                    Font.DisplayMode.NORMAL, 0, 15728880);
        }
    }

    /**
     * Determines the color for a value based on thresholds.
     * Red for values below 30, yellow for 30-60, green for above 60.
     *
     * @param value the value to get color for
     * @return the appropriate color as an ARGB integer
     */
    private static int getColorForValue(int value) {
        if (value < THRESHOLD_LOW) {
            return COLOR_RED;
        }
        if (value < THRESHOLD_MEDIUM) {
            return COLOR_YELLOW;
        }
        return COLOR_GREEN;
    }

    /**
     * Checks if a handle has data in the component.
     *
     * @param component the ecology component
     * @param handleId the handle ID to check
     * @return true if the handle has data
     */
    private static boolean hasHandleData(EcologyComponent component, String handleId) {
        CompoundTag tag = component.getHandleTag(handleId);
        return tag != null && !tag.isEmpty();
    }

    /**
     * Gets the hunger value from the component.
     *
     * @param component the ecology component
     * @return the hunger value (0-100 range typically)
     */
    private static int getHungerValue(EcologyComponent component) {
        CompoundTag tag = component.getHandleTag("hunger");
        if (!tag.contains("hunger")) {
            return 100;
        }
        return tag.getInt("hunger");
    }

    /**
     * Gets the energy value from the component.
     *
     * @param component the ecology component
     * @return the energy value (0-100 range typically)
     */
    private static int getEnergyValue(EcologyComponent component) {
        CompoundTag tag = component.getHandleTag("energy");
        if (!tag.contains("energy")) {
            return 100;
        }
        return tag.getInt("energy");
    }

    /**
     * Gets the condition value from the component.
     *
     * @param component the ecology component
     * @return the condition value (0-100 range typically)
     */
    private static int getConditionValue(EcologyComponent component) {
        CompoundTag tag = component.getHandleTag("condition");
        if (!tag.contains("condition")) {
            return 70;
        }
        return tag.getInt("condition");
    }

    /**
     * Gets the social value from the component.
     *
     * @param component the ecology component
     * @return the social value (0-100 range typically)
     */
    private static int getSocialValue(EcologyComponent component) {
        CompoundTag tag = component.getHandleTag("social");
        if (!tag.contains("social")) {
            return 100;
        }
        return tag.getInt("social");
    }
}
