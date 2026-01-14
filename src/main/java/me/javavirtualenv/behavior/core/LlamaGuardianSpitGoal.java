package me.javavirtualenv.behavior.core;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.animal.Sheep;
import net.minecraft.world.entity.animal.goat.Goat;
import net.minecraft.world.entity.animal.horse.Llama;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.EnumSet;
import java.util.List;

/**
 * Goal that makes llamas act as livestock guardians by spitting at wolves.
 *
 * <p>Based on real-world llama guardian behavior:
 * <ul>
 *   <li>Llamas bond with sheep and goat herds they protect</li>
 *   <li>When wolves threaten the herd, llamas confront rather than flee</li>
 *   <li>Spitting at predators with 80-90% success rate against canines</li>
 *   <li>Wolves retreat after being hit by llama spit</li>
 * </ul>
 *
 * <p>When a wolf approaches within detection range of the llama or nearby livestock,
 * the llama will target and spit at the wolf instead of fleeing. A successful hit
 * applies Slowness II for 10 seconds, causing the wolf to retreat.
 */
public class LlamaGuardianSpitGoal extends Goal {
    private static final Logger LOGGER = LoggerFactory.getLogger(LlamaGuardianSpitGoal.class);

    private static final int SEARCH_INTERVAL_TICKS = 10;
    private static final int SPIT_COOLDOWN_TICKS = 60;
    private static final int DEFENSE_DURATION_TICKS = 400;
    private static final int SLOWNESS_DURATION_TICKS = 200;

    private final Llama llama;
    private final double detectionRange;
    private final double defenseRadius;
    private final double speedModifier;

    @Nullable
    private Wolf targetWolf;
    @Nullable
    private List<Mob> protectedLivestock;
    private int searchCooldown;
    private int spitCooldown;
    private int defenseTicks;
    private boolean defendingLivestock;

    /**
     * Creates a new LlamaGuardianSpitGoal.
     *
     * @param llama the llama that will act as guardian
     * @param detectionRange radius to detect wolves and livestock
     * @param defenseRadius radius around livestock to defend
     * @param speedModifier movement speed when defending
     */
    public LlamaGuardianSpitGoal(
            Llama llama,
            double detectionRange,
            double defenseRadius,
            double speedModifier) {
        this.llama = llama;
        this.detectionRange = detectionRange;
        this.defenseRadius = defenseRadius;
        this.speedModifier = speedModifier;
        this.searchCooldown = 0;
        this.spitCooldown = 0;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK, Goal.Flag.TARGET));
    }

    @Override
    public boolean canUse() {
        if (this.searchCooldown > 0) {
            this.searchCooldown--;
            return false;
        }

        if (this.spitCooldown > 0) {
            this.spitCooldown--;
        }

        this.searchCooldown = reducedTickDelay(SEARCH_INTERVAL_TICKS);

        if (isLlamaTamed()) {
            return false;
        }

        Wolf foundWolf = findNearbyWolf();
        if (foundWolf == null) {
            return false;
        }

        List<Mob> nearbyLivestock = findNearbyLivestock();
        boolean wolfThreateningLivestock = isWolfThreateningLivestock(foundWolf, nearbyLivestock);

        double distanceToWolf = this.llama.distanceTo(foundWolf);
        boolean wolfNearLlama = distanceToWolf <= this.detectionRange;

        if (!wolfNearLlama && !wolfThreateningLivestock) {
            return false;
        }

        this.targetWolf = foundWolf;
        this.protectedLivestock = nearbyLivestock;
        this.defendingLivestock = wolfThreateningLivestock;

        LOGGER.debug("{} guardian mode: targeting wolf {} (distance: {}, defending livestock: {})",
            this.llama.getName().getString(),
            foundWolf.getName().getString(),
            String.format("%.1f", distanceToWolf),
            wolfThreateningLivestock);

        return true;
    }

    @Override
    public boolean canContinueToUse() {
        if (this.defenseTicks > DEFENSE_DURATION_TICKS) {
            return false;
        }

        if (this.targetWolf == null || !this.targetWolf.isAlive()) {
            return false;
        }

        double distanceToWolf = this.llama.distanceTo(this.targetWolf);

        if (distanceToWolf > this.detectionRange * 2) {
            LOGGER.debug("{} stops defending - wolf {} has fled far enough",
                this.llama.getName().getString(),
                this.targetWolf.getName().getString());
            return false;
        }

        if (hasWolfRetreated()) {
            LOGGER.debug("{} stops defending - wolf {} is retreating",
                this.llama.getName().getString(),
                this.targetWolf.getName().getString());
            return false;
        }

        return true;
    }

    @Override
    public void start() {
        this.defenseTicks = 0;
        moveToDefensivePosition();
    }

    @Override
    public void stop() {
        if (this.targetWolf != null) {
            LOGGER.debug("{} finished defending from wolf {}",
                this.llama.getName().getString(),
                this.targetWolf.getName().getString());
        }
        this.targetWolf = null;
        this.protectedLivestock = null;
        this.defenseTicks = 0;
        this.defendingLivestock = false;
        this.llama.getNavigation().stop();
        this.llama.setTarget(null);
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        this.defenseTicks++;

        if (this.targetWolf == null) {
            return;
        }

        this.llama.getLookControl().setLookAt(this.targetWolf, 30.0F, 30.0F);

        double distanceToWolf = this.llama.distanceTo(this.targetWolf);

        if (this.defendingLivestock && this.protectedLivestock != null && !this.protectedLivestock.isEmpty()) {
            if (this.defenseTicks % 20 == 0 || this.llama.getNavigation().isDone()) {
                moveToDefensivePosition();
            }
        } else {
            if (distanceToWolf > 4.0 && (this.defenseTicks % 20 == 0 || this.llama.getNavigation().isDone())) {
                this.llama.getNavigation().moveTo(this.targetWolf, this.speedModifier);
            }
        }

        if (shouldSpitAtWolf(distanceToWolf)) {
            spitAtWolf();
        }
    }

    /**
     * Checks if the llama has a player owner.
     *
     * @return true if the llama is tamed and has an owner
     */
    private boolean isLlamaTamed() {
        return this.llama.isTamed() && this.llama.getOwner() != null;
    }

    /**
     * Finds the nearest wolf within detection range.
     *
     * @return the nearest wolf, or null if none found
     */
    @Nullable
    private Wolf findNearbyWolf() {
        AABB searchBox = this.llama.getBoundingBox().inflate(this.detectionRange);
        List<Wolf> nearbyWolves = this.llama.level()
            .getEntitiesOfClass(Wolf.class, searchBox, wolf -> wolf.isAlive() && !wolf.isTame());

        return nearbyWolves.stream()
            .min((w1, w2) -> Double.compare(this.llama.distanceToSqr(w1), this.llama.distanceToSqr(w2)))
            .orElse(null);
    }

    /**
     * Finds nearby sheep and goats that the llama can protect.
     *
     * @return list of nearby livestock
     */
    private List<Mob> findNearbyLivestock() {
        AABB searchBox = this.llama.getBoundingBox().inflate(this.defenseRadius);
        List<Mob> livestock = this.llama.level()
            .getEntitiesOfClass(Sheep.class, searchBox, Mob::isAlive)
            .stream()
            .map(sheep -> (Mob) sheep)
            .collect(java.util.stream.Collectors.toList());

        List<Goat> goats = this.llama.level()
            .getEntitiesOfClass(Goat.class, searchBox, Mob::isAlive);
        for (Goat goat : goats) {
            livestock.add(goat);
        }

        return livestock;
    }

    /**
     * Checks if the wolf is threatening nearby livestock.
     *
     * @param wolf the wolf to check
     * @param livestock list of nearby livestock
     * @return true if wolf is near livestock
     */
    private boolean isWolfThreateningLivestock(Wolf wolf, List<Mob> livestock) {
        if (livestock.isEmpty()) {
            return false;
        }

        for (Mob animal : livestock) {
            if (wolf.distanceTo(animal) <= this.detectionRange) {
                return true;
            }
        }

        return false;
    }

    /**
     * Checks if the wolf has retreated based on having slowness effect.
     *
     * @return true if wolf has slowness effect (from llama spit)
     */
    private boolean hasWolfRetreated() {
        if (this.targetWolf == null) {
            return false;
        }

        return this.targetWolf.hasEffect(MobEffects.MOVEMENT_SLOWDOWN);
    }

    /**
     * Determines if the llama should spit at the wolf.
     *
     * @param distanceToWolf current distance to the wolf
     * @return true if should spit
     */
    private boolean shouldSpitAtWolf(double distanceToWolf) {
        if (this.spitCooldown > 0) {
            return false;
        }

        if (distanceToWolf > 16.0) {
            return false;
        }

        return this.defenseTicks % 20 == 0;
    }

    /**
     * Commands the llama to spit at the wolf and applies slowness on hit.
     */
    private void spitAtWolf() {
        if (this.targetWolf == null) {
            return;
        }

        this.llama.setTarget(this.targetWolf);
        this.spitCooldown = SPIT_COOLDOWN_TICKS;

        applySlownessToWolf();

        LOGGER.debug("{} spits at wolf {} - applying slowness",
            this.llama.getName().getString(),
            this.targetWolf.getName().getString());
    }

    /**
     * Applies slowness effect to the wolf simulating successful spit hit.
     */
    private void applySlownessToWolf() {
        if (this.targetWolf == null) {
            return;
        }

        double hitChance = this.llama.getRandom().nextDouble();
        if (hitChance <= 0.85) {
            this.targetWolf.addEffect(
                new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, SLOWNESS_DURATION_TICKS, 1)
            );

            LOGGER.debug("{} successfully hit wolf {} with spit",
                this.llama.getName().getString(),
                this.targetWolf.getName().getString());
        }
    }

    /**
     * Moves the llama to a defensive position between livestock and wolf.
     */
    private void moveToDefensivePosition() {
        if (this.targetWolf == null) {
            return;
        }

        if (!this.defendingLivestock || this.protectedLivestock == null || this.protectedLivestock.isEmpty()) {
            return;
        }

        Mob nearestLivestock = this.protectedLivestock.stream()
            .min((a1, a2) -> Double.compare(this.targetWolf.distanceToSqr(a1), this.targetWolf.distanceToSqr(a2)))
            .orElse(null);

        if (nearestLivestock == null) {
            return;
        }

        double dx = this.targetWolf.getX() - nearestLivestock.getX();
        double dz = this.targetWolf.getZ() - nearestLivestock.getZ();
        double distance = Math.sqrt(dx * dx + dz * dz);

        if (distance > 0) {
            double normalizedX = dx / distance;
            double normalizedZ = dz / distance;
            double targetX = nearestLivestock.getX() + normalizedX * 4.0;
            double targetZ = nearestLivestock.getZ() + normalizedZ * 4.0;

            this.llama.getNavigation().moveTo(targetX, nearestLivestock.getY(), targetZ, this.speedModifier);
        }
    }
}
