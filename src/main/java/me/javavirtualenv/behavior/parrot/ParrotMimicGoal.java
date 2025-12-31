package me.javavirtualenv.behavior.parrot;

import me.javavirtualenv.ecology.EcologyComponent;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.monster.Monster;

import java.util.EnumSet;
import java.util.List;

/**
 * AI goal for parrot mimicking behavior.
 * Parrots mimic hostile mob sounds randomly and as warnings.
 */
public class ParrotMimicGoal extends Goal {
    private final PathfinderMob parrot;
    private final MimicBehavior mimicBehavior;
    private final MimicBehavior.MimicConfig config;

    private int warningCheckInterval = 40;
    private int warningCheckTimer = 0;

    public ParrotMimicGoal(PathfinderMob parrot, MimicBehavior mimicBehavior, MimicBehavior.MimicConfig config) {
        this.parrot = parrot;
        this.mimicBehavior = mimicBehavior;
        this.config = config;
        this.setFlags(EnumSet.of(Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        return parrot.isAlive();
    }

    @Override
    public boolean canContinueToUse() {
        return canUse();
    }

    @Override
    public void tick() {
        // Try random mimics
        if (mimicBehavior.tryMimic()) {
            return;
        }

        // Periodic check for hostile mobs (warning mimics)
        warningCheckTimer++;
        if (warningCheckTimer >= warningCheckInterval) {
            warningCheckTimer = 0;
            checkForWarnings();
        }
    }

    /**
     * Checks for nearby hostile mobs and performs warning mimics.
     */
    private void checkForWarnings() {
        List<Monster> nearbyHostiles = parrot.level().getNearbyEntities(
            Monster.class,
            TargetingConditions.forNonCombat(),
            parrot,
            parrot.getBoundingBox().inflate(config.warningRange)
        );

        if (!nearbyHostiles.isEmpty()) {
            // Find closest hostile
            Monster closest = null;
            double closestDistance = Double.MAX_VALUE;

            for (Monster hostile : nearbyHostiles) {
                double distance = parrot.distanceToSqr(hostile);
                if (distance < closestDistance) {
                    closestDistance = distance;
                    closest = hostile;
                }
            }

            if (closest != null) {
                mimicBehavior.tryWarningMimic(Math.sqrt(closestDistance));
            }
        }
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }
}
