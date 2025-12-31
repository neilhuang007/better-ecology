package me.javavirtualenv.behavior.feline;

import me.javavirtualenv.behavior.core.BehaviorContext;
import me.javavirtualenv.behavior.core.Vec3d;
import me.javavirtualenv.behavior.steering.SteeringBehavior;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;

/**
 * Rubbing affection behavior for social felines.
 * <p>
 * Cats show affection by:
 * - Rubbing against players' legs
 * - Circling around trusted entities
 * - Head-butting (bunting) as a greeting
 * - Marking territory with scent glands
 */
public class RubAffectionBehavior extends SteeringBehavior {

    private final double rubRange;
    private final double affectionGain;
    private final int rubDuration;

    private Entity targetEntity;
    private boolean isRubbing = false;
    private int rubTicks = 0;
    private int rubStage = 0; // 0=approach, 1=rub left, 2=behind, 3=rub right, 4=complete

    public RubAffectionBehavior(double rubRange, double affectionGain, int rubDuration) {
        super(0.7);
        this.rubRange = rubRange;
        this.affectionGain = affectionGain;
        this.rubDuration = rubDuration;
    }

    public RubAffectionBehavior() {
        this(1.5, 5.0, 60);
    }

    @Override
    public Vec3d calculate(BehaviorContext context) {
        Mob mob = context.getEntity();

        // Find entity to rub against
        if (targetEntity == null || !targetEntity.isAlive()) {
            targetEntity = findAffectionTarget(mob);
            if (targetEntity == null) {
                isRubbing = false;
                return new Vec3d();
            }
        }

        Vec3d mobPos = context.getPosition();
        Vec3d targetPos = new Vec3d(targetEntity.getX(), targetEntity.getY(), targetEntity.getZ());
        double distance = mobPos.distanceTo(targetPos);

        // Check if close enough to rub
        if (distance > rubRange + 1.0) {
            isRubbing = false;
            rubStage = 0;
            return approachTarget(mobPos, targetPos);
        }

        // Perform rubbing sequence
        isRubbing = true;
        return performRubSequence(mob, mobPos, targetPos, targetEntity);
    }

    private Vec3d approachTarget(Vec3d mobPos, Vec3d targetPos) {
        Vec3d toTarget = Vec3d.sub(targetPos, mobPos);
        toTarget.normalize();
        toTarget.mult(0.3);
        return toTarget;
    }

    private Vec3d performRubSequence(Mob mob, Vec3d mobPos, Vec3d targetPos, Entity target) {
        rubTicks++;

        // Calculate rub positions around the target
        Vec3d toTarget = Vec3d.sub(targetPos, mobPos);
        double angle = Math.atan2(toTarget.x, toTarget.z);

        Vec3d rubPosition;

        switch (rubStage) {
            case 0 -> {
                // Approach from left side
                rubStage = 1;
                rubPosition = calculateRubPosition(targetPos, angle - Math.PI / 4, 0.8);
            }
            case 1 -> {
                // Rub left side (20 ticks)
                if (rubTicks > rubDuration / 4) {
                    rubStage = 2;
                    rubTicks = 0;
                }
                rubPosition = calculateRubPosition(targetPos, angle - Math.PI / 4, 0.8);
            }
            case 2 -> {
                // Move behind (20 ticks)
                if (rubTicks > rubDuration / 4) {
                    rubStage = 3;
                    rubTicks = 0;
                }
                rubPosition = calculateRubPosition(targetPos, angle + Math.PI, 0.8);
            }
            case 3 -> {
                // Rub right side (20 ticks)
                if (rubTicks > rubDuration / 4) {
                    rubStage = 4;
                    rubTicks = 0;
                    // Affection gain complete
                }
                rubPosition = calculateRubPosition(targetPos, angle + Math.PI / 4, 0.8);
            }
            default -> {
                // Complete
                targetEntity = null;
                isRubbing = false;
                rubStage = 0;
                rubTicks = 0;
                return new Vec3d();
            }
        }

        // Move toward rub position
        Vec3d toRubPosition = Vec3d.sub(rubPosition, mobPos);
        double distance = toRubPosition.magnitude();

        if (distance > 0.2) {
            toRubPosition.normalize();
            toRubPosition.mult(0.3);
            return toRubPosition;
        }

        return new Vec3d();
    }

    private Vec3d calculateRubPosition(Vec3d targetPos, double angle, double distance) {
        double x = targetPos.x + Math.sin(angle) * distance;
        double z = targetPos.z + Math.cos(angle) * distance;
        return new Vec3d(x, targetPos.y, z);
    }

    private Entity findAffectionTarget(Mob mob) {
        // Prioritize tamed owner
        if (mob instanceof net.minecraft.world.entity.animal.Cat cat) {
            if (cat.isTame() && cat.getOwner() != null) {
                Entity owner = cat.level().getEntity(cat.getOwnerUUID());
                if (owner != null && owner.isAlive()) {
                    double distance = mob.position().distanceTo(owner.position());
                    if (distance < 8.0) {
                        return owner;
                    }
                }
            }
        }

        // Find nearby trusted player
        for (Player player : mob.level().getEntitiesOfClass(
                Player.class,
                mob.getBoundingBox().inflate(8.0))) {
            double distance = mob.position().distanceTo(player.position());
            if (distance < 8.0) {
                return player;
            }
        }

        return null;
    }

    public boolean isRubbing() {
        return isRubbing;
    }

    public Entity getTargetEntity() {
        return targetEntity;
    }

    public void reset() {
        targetEntity = null;
        isRubbing = false;
        rubTicks = 0;
        rubStage = 0;
    }
}
