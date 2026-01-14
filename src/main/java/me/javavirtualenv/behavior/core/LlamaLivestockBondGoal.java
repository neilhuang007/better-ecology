package me.javavirtualenv.behavior.core;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.animal.Sheep;
import net.minecraft.world.entity.animal.goat.Goat;
import net.minecraft.world.entity.animal.horse.Llama;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.EnumSet;
import java.util.List;

/**
 * Goal that makes llamas bond with and follow sheep/goat herds they guard.
 *
 * <p>Based on real-world llama guardian behavior:
 * <ul>
 *   <li>Llamas naturally bond with sheep and goat herds</li>
 *   <li>They stay within close proximity of the herd (typically 16 blocks)</li>
 *   <li>Follow herd movements to maintain protective position</li>
 *   <li>Bond forms after spending time near livestock (1 Minecraft day)</li>
 * </ul>
 *
 * <p>This goal makes wild/untamed llamas act as livestock guardians by:
 * <ul>
 *   <li>Detecting nearby sheep and goat herds</li>
 *   <li>Following the herd center to maintain proximity</li>
 *   <li>Staying within bond distance of bonded livestock</li>
 * </ul>
 */
public class LlamaLivestockBondGoal extends Goal {
    private static final Logger LOGGER = LoggerFactory.getLogger(LlamaLivestockBondGoal.class);

    private static final int SEARCH_INTERVAL_TICKS = 40;
    private static final int BOND_FORMATION_TIME = 24000;

    private final Llama llama;
    private final double bondDistance;
    private final double followDistance;
    private final double speedModifier;

    @Nullable
    private List<Mob> bondedHerd;
    @Nullable
    private Vec3 herdCenter;
    private int searchCooldown;
    private int timeNearHerd;
    private boolean bondFormed;

    /**
     * Creates a new LlamaLivestockBondGoal.
     *
     * @param llama the llama that will bond with livestock
     * @param bondDistance radius to detect and bond with livestock
     * @param followDistance distance at which to follow the herd
     * @param speedModifier movement speed when following herd
     */
    public LlamaLivestockBondGoal(
            Llama llama,
            double bondDistance,
            double followDistance,
            double speedModifier) {
        this.llama = llama;
        this.bondDistance = bondDistance;
        this.followDistance = followDistance;
        this.speedModifier = speedModifier;
        this.searchCooldown = 0;
        this.timeNearHerd = 0;
        this.bondFormed = false;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        if (this.searchCooldown > 0) {
            this.searchCooldown--;
            return false;
        }

        this.searchCooldown = reducedTickDelay(SEARCH_INTERVAL_TICKS);

        if (isLlamaTamed()) {
            return false;
        }

        List<Mob> nearbyLivestock = findNearbyLivestock();

        if (nearbyLivestock.isEmpty()) {
            this.bondFormed = false;
            this.timeNearHerd = 0;
            return false;
        }

        updateBondFormation(nearbyLivestock);

        Vec3 center = calculateHerdCenter(nearbyLivestock);
        double distanceToHerd = this.llama.position().distanceTo(center);

        if (distanceToHerd < this.followDistance) {
            return false;
        }

        this.bondedHerd = nearbyLivestock;
        this.herdCenter = center;

        LOGGER.debug("{} following bonded livestock herd ({} animals, distance: {})",
            this.llama.getName().getString(),
            nearbyLivestock.size(),
            String.format("%.1f", distanceToHerd));

        return true;
    }

    @Override
    public boolean canContinueToUse() {
        if (this.herdCenter == null) {
            return false;
        }

        if (this.bondedHerd == null || this.bondedHerd.isEmpty()) {
            return false;
        }

        double distanceToHerd = this.llama.position().distanceTo(this.herdCenter);

        if (distanceToHerd < this.followDistance * 0.5) {
            return false;
        }

        if (distanceToHerd > this.bondDistance * 2) {
            LOGGER.debug("{} lost bonded herd - too far away",
                this.llama.getName().getString());
            return false;
        }

        return true;
    }

    @Override
    public void start() {
        navigateToHerd();
    }

    @Override
    public void stop() {
        if (this.bondedHerd != null) {
            LOGGER.debug("{} stopped following bonded herd",
                this.llama.getName().getString());
        }
        this.bondedHerd = null;
        this.herdCenter = null;
        this.llama.getNavigation().stop();
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        if (this.bondedHerd == null) {
            return;
        }

        if (this.llama.tickCount % 20 == 0) {
            this.herdCenter = calculateHerdCenter(this.bondedHerd);
            navigateToHerd();
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
     * Finds nearby sheep and goats within bond distance.
     *
     * @return list of nearby livestock
     */
    private List<Mob> findNearbyLivestock() {
        AABB searchBox = this.llama.getBoundingBox().inflate(this.bondDistance);

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
     * Updates bond formation progress based on time spent near livestock.
     *
     * @param livestock list of nearby livestock
     */
    private void updateBondFormation(List<Mob> livestock) {
        if (livestock.isEmpty()) {
            this.timeNearHerd = 0;
            this.bondFormed = false;
            return;
        }

        this.timeNearHerd++;

        if (!this.bondFormed && this.timeNearHerd >= BOND_FORMATION_TIME) {
            this.bondFormed = true;
            LOGGER.info("{} has formed a bond with nearby livestock herd ({} animals)",
                this.llama.getName().getString(),
                livestock.size());
        }
    }

    /**
     * Calculates the center position of the livestock herd.
     *
     * @param livestock list of livestock animals
     * @return center position of the herd
     */
    private Vec3 calculateHerdCenter(List<Mob> livestock) {
        if (livestock.isEmpty()) {
            return this.llama.position();
        }

        double sumX = 0, sumY = 0, sumZ = 0;

        for (Mob animal : livestock) {
            sumX += animal.getX();
            sumY += animal.getY();
            sumZ += animal.getZ();
        }

        int count = livestock.size();
        return new Vec3(sumX / count, sumY / count, sumZ / count);
    }

    /**
     * Navigates the llama toward the herd center.
     */
    private void navigateToHerd() {
        if (this.herdCenter == null) {
            return;
        }

        this.llama.getNavigation().moveTo(
            this.herdCenter.x,
            this.herdCenter.y,
            this.herdCenter.z,
            this.speedModifier
        );
    }
}
