package me.javavirtualenv.behavior.aquatic;

import me.javavirtualenv.behavior.core.BehaviorContext;
import me.javavirtualenv.behavior.core.SteeringBehavior;
import me.javavirtualenv.behavior.core.Vec3d;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

import java.util.List;
import java.util.UUID;

/**
 * Pufferfish inflate/deflate behavior.
 * When threatened, pufferfish inflate to larger size as a defense mechanism.
 * Inflated state causes poison damage to nearby entities.
 * <p>
 * Scientific basis: Pufferfish inflate by swallowing water to appear larger
 * and more threatening to predators. They contain tetrodotoxin, a potent neurotoxin.
 */
public class PufferfishInflateBehavior extends SteeringBehavior {
    private static final UUID INFLATE_SIZE_MODIFIER_UUID = UUID.fromString("7f101910-8c23-11ee-b9d1-0242ac120002");
    private static final UUID INFLATE_SPEED_MODIFIER_UUID = UUID.fromString("7f101911-8c23-11ee-b9d1-0242ac120002");

    private final AquaticConfig config;
    private boolean isInflated = false;
    private long lastInflateTime = 0;
    private long inflateStartTime = 0;

    public PufferfishInflateBehavior(AquaticConfig config) {
        super(1.2, true);
        this.config = config;
    }

    public PufferfishInflateBehavior() {
        this(AquaticConfig.createForPufferfish());
    }

    @Override
    public Vec3d calculate(BehaviorContext context) {
        Entity self = context.getEntity();
        Level level = context.getLevel();
        long currentTime = level.getGameTime();

        // Check if should deflate
        if (isInflated && currentTime - inflateStartTime >= config.getInflateDuration()) {
            deflate(self);
            return new Vec3d();
        }

        // Check cooldown
        if (isInflated || currentTime - lastInflateTime < config.getDeflateCooldown()) {
            // While inflated, apply slow movement but can still flee
            if (isInflated) {
                return calculateSlowEscape(context);
            }
            return new Vec3d();
        }

        // Check for threats
        Entity threat = findNearestThreat(context);

        if (threat != null) {
            double distanceToThreat = context.getPosition().distanceTo(
                new Vec3d(threat.getX(), threat.getY(), threat.getZ())
            );

            if (distanceToThreat < config.getInflateThreshold()) {
                inflate(self);
                return calculateSlowEscape(context);
            }
        }

        return new Vec3d();
    }

    private Entity findNearestThreat(BehaviorContext context) {
        Entity self = context.getEntity();
        Vec3d position = context.getPosition();
        Level level = context.getLevel();
        double detectionRange = 8.0;

        AABB searchBox = new AABB(
            position.x - detectionRange, position.y - detectionRange, position.z - detectionRange,
            position.x + detectionRange, position.y + detectionRange, position.z + detectionRange
        );

        List<Entity> potentialThreats = level.getEntities(self, searchBox, entity -> {
            if (entity == self) return false;
            if (!entity.isAlive()) return false;

            // Players are threats
            if (entity instanceof net.minecraft.world.entity.player.Player) {
                return true;
            }

            // Aquatic predators
            String entityId = net.minecraft.core.Registry.ENTITY_TYPE.getKey(entity.getType()).toString();
            return entityId.equals("minecraft:drowned") ||
                   entityId.equals("minecraft:guardian") ||
                   entityId.equals("minecraft:elder_guardian");
        });

        Entity nearestThreat = null;
        double nearestDistance = Double.MAX_VALUE;

        for (Entity threat : potentialThreats) {
            Vec3d threatPos = new Vec3d(threat.getX(), threat.getY(), threat.getZ());
            double distance = position.distanceTo(threatPos);
            if (distance < nearestDistance) {
                nearestDistance = distance;
                nearestThreat = threat;
            }
        }

        return nearestThreat;
    }

    private Vec3d calculateSlowEscape(BehaviorContext context) {
        Entity self = context.getEntity();
        Vec3d position = context.getPosition();

        // Pufferfish move slowly when inflated
        // Just add some random movement
        Vec3d randomMove = new Vec3d(
            (Math.random() - 0.5) * 0.05,
            (Math.random() - 0.5) * 0.05,
            (Math.random() - 0.5) * 0.05
        );

        return randomMove;
    }

    private void inflate(Entity self) {
        if (!(self instanceof net.minecraft.world.entity.animal.Pufferfish)) return;

        isInflated = true;
        inflateStartTime = self.level().getGameTime();
        lastInflateTime = inflateStartTime;

        // Apply size increase
        if (self.getAttribute(Attributes.SCALE) != null) {
            AttributeModifier sizeModifier = new AttributeModifier(
                INFLATE_SIZE_MODIFIER_UUID,
                "Inflated size",
                1.5,
                AttributeModifier.Operation.ADD_MULTIPLIED_BASE
            );
            self.getAttribute(Attributes.SCALE).addPermanentModifier(sizeModifier);
        }

        // Apply speed decrease
        if (self.getAttribute(Attributes.MOVEMENT_SPEED) != null) {
            AttributeModifier speedModifier = new AttributeModifier(
                INFLATE_SPEED_MODIFIER_UUID,
                "Inflated slow",
                -0.5,
                AttributeModifier.Operation.ADD_MULTIPLIED_BASE
            );
            self.getAttribute(Attributes.MOVEMENT_SPEED).addPermanentModifier(speedModifier);
        }
    }

    private void deflate(Entity self) {
        if (!(self instanceof net.minecraft.world.entity.animal.Pufferfish)) return;

        isInflated = false;

        // Remove modifiers
        if (self.getAttribute(Attributes.SCALE) != null) {
            self.getAttribute(Attributes.SCALE).removeModifier(INFLATE_SIZE_MODIFIER_UUID);
        }

        if (self.getAttribute(Attributes.MOVEMENT_SPEED) != null) {
            self.getAttribute(Attributes.MOVEMENT_SPEED).removeModifier(INFLATE_SPEED_MODIFIER_UUID);
        }
    }

    public boolean isInflated() {
        return isInflated;
    }

    public boolean canInflate(BehaviorContext context) {
        long currentTime = context.getLevel().getGameTime();
        return !isInflated && (currentTime - lastInflateTime) >= config.getDeflateCooldown();
    }

    public boolean isPoisonousTo(Entity entity) {
        if (!isInflated) return false;

        String entityId = net.minecraft.core.Registry.ENTITY_TYPE.getKey(entity.getType()).toString();
        // Pufferfish are poisonous to most entities except guardians and drowned
        return !entityId.equals("minecraft:guardian") &&
               !entityId.equals("minecraft:elder_guardian") &&
               !entityId.equals("minecraft:drowned");
    }
}
