package me.javavirtualenv.behavior.camel;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.animal.camel.Camel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;

import java.util.EnumSet;

/**
 * AI goal for camel spitting defense behavior.
 * <p>
 * This goal manages the spitting defense mechanism:
 * - Detects threats
 * - Triggers warning animation
 * - Fires spit projectile
 * - Manages cooldowns
 */
public class SpittingDefenseGoal extends Goal {

    private final Camel camel;
    private final CamelConfig config;

    private LivingEntity target;
    private int windupTicks;
    private int cooldownTicks;

    public SpittingDefenseGoal(Camel camel, CamelConfig config) {
        this.camel = camel;
        this.config = config;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (cooldownTicks > 0) {
            cooldownTicks--;
            return false;
        }

        if (!camel.isControllable()) {
            return false;
        }

        // Check if player is controlling the camel
        if (camel.isVehicle() && camel.getPassengers().stream()
                .anyMatch(passenger -> passenger instanceof Player)) {
            return false;
        }

        target = findThreat();
        return target != null;
    }

    @Override
    public boolean canContinueToUse() {
        return target != null && target.isAlive() && windupTicks > 0;
    }

    @Override
    public void start() {
        windupTicks = config.getSpittingWindupTicks();
    }

    @Override
    public void tick() {
        if (target == null) {
            return;
        }

        // Look at target
        camel.getLookControl().setLookAt(target, 30.0f, 30.0f);

        // Wind down windup
        if (windupTicks > 0) {
            windupTicks--;

            // Spawn warning particles
            if (camel.level().isClientSide && windupTicks % 5 == 0) {
                spawnWarningParticles();
            }

            // Fire spit when windup completes
            if (windupTicks <= 0) {
                fireSpit();
                cooldownTicks = config.getSpittingCooldown();
            }
        }
    }

    @Override
    public void stop() {
        target = null;
        windupTicks = 0;
    }

    /**
     * Finds a nearby threat to spit at.
     */
    private LivingEntity findThreat() {
        double range = config.getSpittingRange();

        for (LivingEntity entity : camel.level().getEntitiesOfClass(
                LivingEntity.class,
                camel.getBoundingBox().inflate(range))) {
            if (isThreat(entity) && isProvoking(entity)) {
                return entity;
            }
        }

        return null;
    }

    /**
     * Determines if an entity is a threat.
     */
    private boolean isThreat(LivingEntity entity) {
        // Players are threats if not sneaking
        if (entity instanceof Player player) {
            return !player.isCreative() && !player.isSpectator() && !player.isShiftKeyDown();
        }

        // Hostile mobs are threats
        String typeName = entity.getType().toString().toLowerCase();
        return typeName.contains("zombie") ||
               typeName.contains("skeleton") ||
               typeName.contains("spider") ||
               typeName.contains("creeper") ||
               typeName.contains("phantom") ||
               typeName.contains("drowned") ||
               typeName.contains("pillager") ||
               typeName.contains("vindicator") ||
               typeName.contains("wolf") ||
               typeName.contains("fox");
    }

    /**
     * Determines if a threat is actively provoking the camel.
     */
    private boolean isProvoking(LivingEntity threat) {
        double distance = camel.distanceTo(threat);

        // Very close threats always provoke
        if (distance < 4.0) {
            return true;
        }

        // Players provoke if not sneaking
        if (threat instanceof Player player) {
            return !player.isShiftKeyDown() && distance < config.getSpittingRange() * 0.8;
        }

        // Aggressive mobs provoke if targeting this camel
        if (threat instanceof net.minecraft.world.entity.Mob mob && mob.isAggressive()) {
            return mob.getTarget() == camel;
        }

        return false;
    }

    /**
     * Spawns warning particles during windup.
     */
    private void spawnWarningParticles() {
        // Client-side particle effect for warning
        // This would be handled by a client-side renderer in full implementation
    }

    /**
     * Fires the spit projectile.
     */
    private void fireSpit() {
        if (camel.level().isClientSide) {
            return;
        }

        // Calculate direction to target
        double dx = target.getX() - camel.getX();
        double dy = target.getY() - camel.getY() - 1.5; // Account for height difference
        double dz = target.getZ() - camel.getZ();
        double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);

        // Normalize and apply accuracy variation
        double accuracy = config.getSpittingAccuracy();
        double variation = (1.0 - accuracy) * 0.5;

        dx = dx / distance + (Math.random() - 0.5) * variation;
        dy = dy / distance + (Math.random() - 0.5) * variation * 0.5;
        dz = dz / distance + (Math.random() - 0.5) * variation;

        // Create and launch spit entity
        CamelSpitEntity spit = new CamelSpitEntity(camel.level(), camel);
        spit.setPos(
            camel.getX(),
            camel.getY() + 1.5,
            camel.getZ()
        );
        spit.shoot(dx, dy, dz, 0.8f, 0.1f);

        spit.setDamage(config.getSpittingDamage());
        spit.setSlowDuration(config.getSpittingSlowDuration());

        camel.level().addFreshEntity(spit);
    }
}
