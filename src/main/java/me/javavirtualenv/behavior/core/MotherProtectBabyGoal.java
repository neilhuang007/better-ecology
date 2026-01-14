package me.javavirtualenv.behavior.core;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;

/**
 * Goal that makes adult animals protect nearby baby animals from predators.
 *
 * <p>When a predator threatens a baby nearby, the adult will:
 * <ul>
 *   <li>Target the predator and attack</li>
 *   <li>Move between the baby and predator</li>
 *   <li>Continue attacking until the predator flees or dies</li>
 * </ul>
 *
 * <p>Based on research showing maternal aggression during lactation periods
 * as a defense mechanism against predators and infanticidal conspecifics.
 */
public class MotherProtectBabyGoal extends Goal {
    private static final Logger LOGGER = LoggerFactory.getLogger(MotherProtectBabyGoal.class);

    private static final int SEARCH_INTERVAL_TICKS = 10;
    private static final int PROTECTION_DURATION = 200;
    private static final int ATTACK_COOLDOWN_TICKS = 20;

    private final Mob mob;
    private final Class<? extends Mob> babyType;
    private final double protectionRange;
    private final double threatRange;
    private final double speedModifier;
    private final Class<?>[] predatorClasses;

    @Nullable
    private Mob targetBaby;
    @Nullable
    private LivingEntity targetThreat;
    private int searchCooldown;
    private int protectTicks;
    private int attackCooldown;

    /**
     * Creates a new MotherProtectBabyGoal.
     *
     * @param mob the adult mob that protects babies
     * @param babyType the type of baby to protect
     * @param protectionRange radius to scan for babies
     * @param threatRange radius to scan for threats
     * @param speedModifier movement speed when attacking
     * @param predatorClasses classes of predators to defend against
     */
    @SafeVarargs
    public MotherProtectBabyGoal(
            Mob mob,
            Class<? extends Mob> babyType,
            double protectionRange,
            double threatRange,
            double speedModifier,
            Class<? extends LivingEntity>... predatorClasses) {
        this.mob = mob;
        this.babyType = babyType;
        this.protectionRange = protectionRange;
        this.threatRange = threatRange;
        this.speedModifier = speedModifier;
        this.predatorClasses = predatorClasses;
        this.searchCooldown = 0;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK, Goal.Flag.TARGET));
    }

    @Override
    public boolean canUse() {
        // Only adults can protect babies
        if (this.mob.isBaby()) {
            return false;
        }

        if (this.searchCooldown > 0) {
            this.searchCooldown--;
            return false;
        }

        this.searchCooldown = reducedTickDelay(SEARCH_INTERVAL_TICKS);

        // Find nearby babies
        Mob threatenedBaby = findThreatenedBaby();
        if (threatenedBaby == null) {
            return false;
        }

        // Find threat near that baby
        LivingEntity threat = findThreatNearBaby(threatenedBaby);
        if (threat == null) {
            return false;
        }

        this.targetBaby = threatenedBaby;
        this.targetThreat = threat;

        LOGGER.debug("{} will protect baby {} from threat {}",
            this.mob.getName().getString(),
            threatenedBaby.getName().getString(),
            threat.getName().getString());

        return true;
    }

    @Override
    public boolean canContinueToUse() {
        if (this.protectTicks > PROTECTION_DURATION) {
            return false;
        }

        if (this.targetThreat == null || !this.targetThreat.isAlive()) {
            return false;
        }

        // Stop if threat has fled far enough
        double threatDistance = this.mob.distanceTo(this.targetThreat);
        if (threatDistance > this.threatRange * 1.5) {
            LOGGER.debug("{} stops protecting - threat {} has fled",
                this.mob.getName().getString(),
                this.targetThreat.getName().getString());
            return false;
        }

        return true;
    }

    @Override
    public void start() {
        this.protectTicks = 0;
        this.attackCooldown = 0;
        navigateToThreat();
    }

    @Override
    public void stop() {
        if (this.targetThreat != null) {
            LOGGER.debug("{} finished protecting from {}",
                this.mob.getName().getString(),
                this.targetThreat.getName().getString());
        }
        this.targetBaby = null;
        this.targetThreat = null;
        this.protectTicks = 0;
        this.mob.setTarget(null);
        this.mob.getNavigation().stop();
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        this.protectTicks++;

        if (this.targetThreat == null) {
            return;
        }

        // Look at the threat
        this.mob.getLookControl().setLookAt(this.targetThreat, 30.0F, 30.0F);

        // Move toward threat
        double distance = this.mob.distanceTo(this.targetThreat);
        if (distance > 2.0) {
            if (this.protectTicks % 10 == 0 || this.mob.getNavigation().isDone()) {
                this.mob.getNavigation().moveTo(this.targetThreat, this.speedModifier);
            }
        }

        // Attack/push if in range
        if (distance <= 2.0 && this.attackCooldown <= 0) {
            // Check if mob has attack damage attribute (predators do, herbivores don't)
            if (this.mob.getAttributes().hasAttribute(Attributes.ATTACK_DAMAGE)) {
                this.mob.doHurtTarget(this.targetThreat);
                LOGGER.debug("{} attacks threat {}", this.mob.getName().getString(), this.targetThreat.getName().getString());
            } else {
                // Push/ram the threat instead (for herbivores like cows and sheep)
                Vec3 pushDirection = this.targetThreat.position().subtract(this.mob.position()).normalize();
                this.targetThreat.push(pushDirection.x * 0.5, 0.2, pushDirection.z * 0.5);
                LOGGER.debug("{} pushes threat {}", this.mob.getName().getString(), this.targetThreat.getName().getString());
            }
            this.attackCooldown = ATTACK_COOLDOWN_TICKS;
        }

        if (this.attackCooldown > 0) {
            this.attackCooldown--;
        }
    }

    /**
     * Finds the nearest baby that has a threat nearby.
     *
     * @return the threatened baby, or null if none
     */
    @Nullable
    private Mob findThreatenedBaby() {
        AABB searchBox = this.mob.getBoundingBox().inflate(this.protectionRange);
        List<? extends Mob> nearbyBabies = this.mob.level()
            .getEntitiesOfClass(this.babyType, searchBox, this::isValidBaby);

        // Check each baby for nearby threats
        for (Mob baby : nearbyBabies) {
            LivingEntity threat = findThreatNearBaby(baby);
            if (threat != null) {
                return baby;
            }
        }

        return null;
    }

    /**
     * Finds the nearest predator threatening a baby.
     *
     * @param baby the baby to check threats around
     * @return the nearest threat, or null if none
     */
    @Nullable
    private LivingEntity findThreatNearBaby(Mob baby) {
        AABB threatBox = baby.getBoundingBox().inflate(this.threatRange);

        LivingEntity nearestThreat = null;
        double nearestDistSq = Double.MAX_VALUE;

        for (Class<?> predatorClass : this.predatorClasses) {
            @SuppressWarnings("unchecked")
            List<? extends LivingEntity> predators = this.mob.level()
                .getEntitiesOfClass((Class<? extends LivingEntity>) predatorClass, threatBox,
                    entity -> entity.isAlive() && entity != this.mob);

            for (LivingEntity predator : predators) {
                // For wolves, only untamed wolves are threats
                if (predator instanceof Wolf wolf && wolf.isTame()) {
                    continue;
                }

                double distSq = baby.distanceToSqr(predator);
                if (distSq < nearestDistSq) {
                    nearestDistSq = distSq;
                    nearestThreat = predator;
                }
            }
        }

        return nearestThreat;
    }

    /**
     * Validates if an entity is a valid baby to protect.
     *
     * @param entity the entity to check
     * @return true if this is a valid baby
     */
    private boolean isValidBaby(Mob entity) {
        if (entity == this.mob) {
            return false;
        }
        if (!entity.isAlive()) {
            return false;
        }
        return entity.isBaby();
    }

    /**
     * Navigates the mob toward the threat.
     */
    private void navigateToThreat() {
        if (this.targetThreat == null) {
            return;
        }

        this.mob.getNavigation().moveTo(this.targetThreat, this.speedModifier);
    }
}
