package me.javavirtualenv.ecology.ai;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;
import java.util.List;
import java.util.UUID;

/**
 * AI goal for bull cows to compete for dominance and mating rights.
 *
 * Bulls will:
 * - Display dominance through posturing and vocalizations
 * - Engage in mock battles with other bulls
 * - Establish dominance hierarchy
 * - Compete for access to females during breeding season
 *
 * Scientific basis:
 * - Bulls establish dominance hierarchies through displays and mock fighting
 * - Dominance determines priority access to mates
 * - Fighting includes head-butting, pushing matches, and vocal displays
 * - Less physical conflict than in wild bovids due to domestication
 */
public class BullCompetitionGoal extends Goal {
    private final Mob bull;
    private final Level level;
    private final double challengeRange;
    private final double combatSpeed;
    private Mob rival;
    private int displayTimer;
    private int combatTimer;
    private int cooldownTimer;
    private boolean isDisplaying;
    private boolean isFighting;
    private UUID lastRivalUuid;
    private int dominanceScore;

    public BullCompetitionGoal(Mob bull, double challengeRange) {
        this.bull = bull;
        this.level = bull.level();
        this.challengeRange = challengeRange;
        this.combatSpeed = 0.8;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        this.displayTimer = 0;
        this.combatTimer = 0;
        this.cooldownTimer = 0;
        this.isDisplaying = false;
        this.isFighting = false;
        this.dominanceScore = 50; // Start neutral
    }

    @Override
    public boolean canUse() {
        // Only adult males compete
        if (bull.isBaby() || cooldownTimer > 0) {
            return false;
        }

        // Find rival bull
        rival = findRival();
        return rival != null && rival.isAlive();
    }

    @Override
    public boolean canContinueToUse() {
        if (rival == null || !rival.isAlive()) {
            return false;
        }

        double distance = bull.distanceToSqr(rival);

        // Continue while rival is close and we're displaying or fighting
        return distance < challengeRange * challengeRange &&
               (isDisplaying || isFighting || combatTimer > 0);
    }

    @Override
    public void start() {
        lastRivalUuid = rival.getUUID();

        // Decide between display and combat based on dominance difference
        int rivalDominance = getRivalDominance();
        int dominanceDiff = Math.abs(dominanceScore - rivalDominance);

        if (dominanceDiff > 20) {
            // Clear dominance difference - just display
            startDisplay();
        } else {
            // Close competition - may escalate to combat
            if (bull.getRandom().nextFloat() < 0.6f) {
                startDisplay();
            } else {
                startCombat();
            }
        }
    }

    @Override
    public void stop() {
        rival = null;
        isDisplaying = false;
        isFighting = false;
        displayTimer = 0;
        combatTimer = 0;
        bull.getNavigation().stop();
        cooldownTimer = 1200; // 1 minute cooldown
    }

    @Override
    public void tick() {
        if (rival == null) {
            return;
        }

        // Face the rival
        bull.getLookControl().setLookAt(rival);

        if (isDisplaying) {
            tickDisplay();
        } else if (isFighting) {
            tickCombat();
        }
    }

    /**
     * Handle dominance display behavior.
     */
    private void tickDisplay() {
        Vec3 bullPos = bull.position();
        Vec3 rivalPos = rival.position();
        double distance = bull.distanceToSqr(rival);

        displayTimer++;

        // Maintain display distance (3-5 blocks)
        if (distance < 9.0) {
            // Too close, back up slightly
            Vec3 awayDir = bullPos.subtract(rivalPos).normalize();
            Vec3 targetPos = bullPos.add(awayDir.scale(2.0));
            bull.getNavigation().moveTo(targetPos.x, targetPos.y, targetPos.z, combatSpeed * 0.5);
        } else if (distance > 25.0) {
            // Too far, approach
            bull.getNavigation().moveTo(rival, combatSpeed * 0.6);
        }

        // Vocal display every 40 ticks
        if (displayTimer % 40 == 0) {
            emitDisplaySound();
        }

        // Paw the ground occasionally
        if (displayTimer % 60 == 0 && bull.getRandom().nextFloat() < 0.3f) {
            pawGround();
        }

        // End display after 200 ticks (10 seconds)
        if (displayTimer >= 200) {
            resolveDisplay();
        }
    }

    /**
     * Handle combat behavior.
     */
    private void tickCombat() {
        Vec3 bullPos = bull.position();
        Vec3 rivalPos = rival.position();
        double distance = bull.distanceToSqr(rival);

        combatTimer++;

        // Circle and charge behavior
        if (distance > 9.0) {
            // Approach rival
            bull.getNavigation().moveTo(rival, combatSpeed);
        } else if (distance < 4.0) {
            // Close enough for head-butting simulation
            simulateHeadButt();

            // Back off periodically
            if (combatTimer % 80 == 0) {
                Vec3 awayDir = bullPos.subtract(rivalPos).normalize();
                Vec3 targetPos = bullPos.add(awayDir.scale(4.0));
                bull.getNavigation().moveTo(targetPos.x, targetPos.y, targetPos.z, combatSpeed);
            }
        } else {
            // Circle around rival
            Vec3 toRival = rivalPos.subtract(bullPos).normalize();
            Vec3 circleDir = new Vec3(-toRival.z, 0, toRival.x).normalize();
            Vec3 targetPos = bullPos.add(circleDir.scale(3.0));
            bull.getNavigation().moveTo(targetPos.x, targetPos.y, targetPos.z, combatSpeed);
        }

        // Aggressive vocalizations
        if (combatTimer % 60 == 0) {
            emitCombatSound();
        }

        // End combat after 400 ticks (20 seconds)
        if (combatTimer >= 400) {
            resolveCombat();
        }
    }

    /**
     * Start dominance display.
     */
    private void startDisplay() {
        isDisplaying = true;
        isFighting = false;
        displayTimer = 0;

        // Initial vocalization
        emitDisplaySound();

        // Store rival UUID for tracking
        if (rival != null) {
            lastRivalUuid = rival.getUUID();
        }
    }

    /**
     * Start combat.
     */
    private void startCombat() {
        isFighting = true;
        isDisplaying = false;
        combatTimer = 0;

        // Initial aggressive vocalization
        emitCombatSound();
    }

    /**
     * Resolve dominance display.
     */
    private void resolveDisplay() {
        // Compare dominance scores
        int rivalDominance = getRivalDominance();

        if (dominanceScore > rivalDominance) {
            // We win, gain dominance
            dominanceScore = Math.min(100, dominanceScore + 5);
        } else if (dominanceScore < rivalDominance) {
            // We lose, lose dominance
            dominanceScore = Math.max(0, dominanceScore - 3);
        }

        // Play resolution sound
        level.playSound(null, bull.blockPosition(),
                SoundEvents.COW_MILK,
                SoundSource.NEUTRAL,
                1.2F, 0.9F);
    }

    /**
     * Resolve combat.
     */
    private void resolveCombat() {
        // Combat resolution based on random chance weighted by dominance
        double winChance = 0.4 + (dominanceScore / 100.0) * 0.3;

        if (bull.getRandom().nextDouble() < winChance) {
            // We win
            dominanceScore = Math.min(100, dominanceScore + 10);

            // Victory display
            level.playSound(null, bull.blockPosition(),
                    SoundEvents.COW_MILK,
                    SoundSource.NEUTRAL,
                    1.5F, 0.7F);
        } else {
            // We lose
            dominanceScore = Math.max(0, dominanceScore - 7);
        }

        // Stop fighting
        isFighting = false;
    }

    /**
     * Simulate head-butting (no actual damage, just animation and sound).
     */
    private void simulateHeadButt() {
        if (level.getRandom().nextFloat() < 0.1f) {
            // Play impact sound
            level.playSound(null, bull.blockPosition(),
                    SoundEvents.ZOMBIE_ATTACK_WOODEN_DOOR,
                    SoundSource.HOSTILE,
                    0.8F, 1.2F);

            // Spawn impact particles
            if (level instanceof ServerLevel serverLevel) {
                Vec3 direction = rival.position().subtract(bull.position()).normalize();
                serverLevel.sendParticles(
                        net.minecraft.core.particles.ParticleTypes.CLOUD,
                        bull.getX() + direction.x * 0.5,
                        bull.getY() + bull.getEyeHeight(),
                        bull.getZ() + direction.z * 0.5,
                        3, 0.1, 0.1, 0.1, 0.05
                );
            }
        }
    }

    /**
     * Emit dominance display sound.
     */
    private void emitDisplaySound() {
        level.playSound(null, bull.blockPosition(),
                SoundEvents.COW_MILK,
                SoundSource.NEUTRAL,
                1.3F, 0.85F);
    }

    /**
     * Emit combat sound (more aggressive).
     */
    private void emitCombatSound() {
        level.playSound(null, bull.blockPosition(),
                SoundEvents.COW_MILK,
                SoundSource.HOSTILE,
                1.8F, 0.7F);
    }

    /**
     * Paw the ground as a threat display.
     */
    private void pawGround() {
        level.playSound(null, bull.blockPosition(),
                SoundEvents.HOE_TILL,
                SoundSource.NEUTRAL,
                0.5F, 1.0F);

        // Spawn dirt particles
        if (level instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(
                    net.minecraft.core.particles.ParticleTypes.BLOCK,
                    bull.getX(),
                    bull.getY(),
                    bull.getZ(),
                    5, 0.2, 0.1, 0.2, 0.05
            );
        }
    }

    /**
     * Find a rival bull to compete with.
     */
    private Mob findRival() {
        List<Mob> nearbyBulls = level.getEntitiesOfClass(
                Mob.class,
                bull.getBoundingBox().inflate(challengeRange),
                mob -> !mob.isBaby() &&
                       mob.isAlive() &&
                       !mob.equals(bull) &&
                       isSameSpecies(bull, mob)
        );

        if (nearbyBulls.isEmpty()) {
            return null;
        }

        // Prefer to compete with bulls we've competed with before
        if (lastRivalUuid != null) {
            for (Mob mob : nearbyBulls) {
                if (mob.getUUID().equals(lastRivalUuid)) {
                    return mob;
                }
            }
        }

        // Otherwise pick nearest rival
        Mob nearestRival = null;
        double nearestDistance = Double.MAX_VALUE;

        for (Mob mob : nearbyBulls) {
            double distance = bull.distanceToSqr(mob);
            if (distance < nearestDistance) {
                nearestDistance = distance;
                nearestRival = mob;
            }
        }

        return nearestRival;
    }

    /**
     * Get rival's dominance score from persistent data.
     */
    private int getRivalDominance() {
        if (rival == null) {
            return 50;
        }

        return rival.getPersistentData().getInt("better-ecology:dominance_score");
    }

    /**
     * Check if two mobs are the same species.
     */
    private boolean isSameSpecies(Mob mob1, Mob mob2) {
        return mob1.getType().equals(mob2.getType());
    }

    // Getters for external access

    public int getDominanceScore() {
        return dominanceScore;
    }

    public void setDominanceScore(int score) {
        this.dominanceScore = Math.max(0, Math.min(100, score));
        bull.getPersistentData().putInt("better-ecology:dominance_score", dominanceScore);
    }

    public boolean isDisplaying() {
        return isDisplaying;
    }

    public boolean isFighting() {
        return isFighting;
    }
}
