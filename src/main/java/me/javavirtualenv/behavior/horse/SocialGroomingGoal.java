package me.javavirtualenv.behavior.horse;

import me.javavirtualenv.BetterEcology;
import me.javavirtualenv.debug.BehaviorLogger;
import me.javavirtualenv.ecology.api.EcologyAccess;
import me.javavirtualenv.ecology.EcologyComponent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;
import java.util.List;

/**
 * AI Goal for social grooming behavior in horse herds.
 * <p>
 * Horses groom each other to strengthen social bonds.
 * This behavior reduces stress and maintains herd cohesion.
 * <p>
 * Grooming features:
 * <ul>
 *   <li>Mutual grooming between herd members</li>
 *   <li>Heart particles to show social bonding</li>
 *   <li>Soft breathing sounds during grooming</li>
 *   <li>Only same-type horses groom each other</li>
 *   <li>Wild and tame horses don't mix</li>
 * </ul>
 */
public class SocialGroomingGoal extends Goal {

    // Configuration constants
    private static final String FLEEING_KEY = "fleeing";

    private static final double GROOMING_SEARCH_RADIUS = 8.0; // Search for partners
    private static final double MAX_GROOMING_DISTANCE = 4.0; // Max distance to groom
    private static final double MOVE_TO_DISTANCE = 2.0; // Distance to stop moving
    private static final int GROOMING_DURATION_TICKS = 200; // How long to groom
    private static final int COOLDOWN_TICKS = 600; // Cooldown after grooming
    private static final double GROOMING_INITIATION_CHANCE = 0.15; // Chance to initiate
    private static final double MUTUAL_GROOMING_CHANCE = 0.7; // Chance partner grooms back
    private static final int SOUND_INTERVAL_TICKS = 60; // Ticks between grooming sounds
    private static final int PARTICLE_INTERVAL_TICKS = 40; // Ticks between heart particles
    private static final int MUTUAL_CHECK_INTERVAL = 20; // Ticks between mutual grooming checks
    private static final double MOVE_SPEED = 0.8; // Speed to move to partner

    // Instance fields
    private final AbstractHorse horse;
    private final EntityType<?> horseType;

    private AbstractHorse groomingPartner;
    private int groomingTicks;
    private int cooldownTicks;

    // Debug info
    private String lastDebugMessage = "";
    private boolean hadPartnerLastCheck = false;

    public SocialGroomingGoal(AbstractHorse horse) {
        this.horse = horse;
        this.horseType = horse.getType();
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        // Client-side only runs visual logic
        if (horse.level().isClientSide) {
            return false;
        }

        // Update cooldown
        if (cooldownTicks > 0) {
            cooldownTicks--;
            return false;
        }

        // Must be alive
        if (!horse.isAlive()) {
            return false;
        }

        // Cannot groom while being ridden
        if (horse.isVehicle()) {
            return false;
        }

        // Cannot groom if fleeing
        if (isFleeing()) {
            if (hadPartnerLastCheck) {
                hadPartnerLastCheck = false;
            }
            return false;
        }

        // Find a grooming partner
        groomingPartner = findGroomingPartner();

        if (groomingPartner == null) {
            if (hadPartnerLastCheck) {
                debug("no grooming partner found");
                hadPartnerLastCheck = false;
            }
            return false;
        }

        hadPartnerLastCheck = true;
        return true;
    }

    @Override
    public boolean canContinueToUse() {
        if (groomingPartner == null || !groomingPartner.isAlive()) {
            debug("partner no longer valid");
            return false;
        }

        if (horse.isVehicle() || groomingPartner.isVehicle()) {
            debug("partner or self being ridden");
            return false;
        }

        if (isFleeing() || isPartnerFleeing()) {
            debug("partner or self fleeing");
            return false;
        }

        double distance = horse.distanceToSqr(groomingPartner);
        if (distance > MAX_GROOMING_DISTANCE * MAX_GROOMING_DISTANCE) {
            debug("partner moved too far");
            return false;
        }

        return groomingTicks < GROOMING_DURATION_TICKS;
    }

    @Override
    public void start() {
        groomingTicks = 0;
        String partnerId = "#" + groomingPartner.getId();
        debug("STARTING: grooming with " + partnerId);
    }

    @Override
    public void stop() {
        groomingPartner = null;
        groomingTicks = 0;
        cooldownTicks = COOLDOWN_TICKS;
        debug("grooming stopped, cooldown=" + cooldownTicks);
    }

    @Override
    public void tick() {
        if (groomingPartner == null) {
            return;
        }

        double distance = horse.distanceTo(groomingPartner);

        // Move towards partner if too far
        if (distance > MOVE_TO_DISTANCE) {
            moveToPartner();
        } else {
            // Stop movement and groom
            horse.getNavigation().stop();

            // Look at partner
            horse.getLookControl().setLookAt(groomingPartner);

            groomingTicks++;

            // Play grooming sound periodically
            if (groomingTicks % SOUND_INTERVAL_TICKS == 0) {
                playGroomingSound();
            }

            // Create heart particles occasionally
            if (groomingTicks % PARTICLE_INTERVAL_TICKS == 0 &&
                horse.getRandom().nextFloat() < 0.3) {
                spawnSocialParticles();
            }

            // Partner also grooms back
            if (groomingTicks % MUTUAL_CHECK_INTERVAL == 0) {
                if (!groomingPartner.isVehicle() &&
                    horse.getRandom().nextFloat() < MUTUAL_GROOMING_CHANCE) {
                    // Mutual grooming - partner looks back
                    groomingPartner.getLookControl().setLookAt(horse);
                }
            }

            // Log progress
            if (groomingTicks % 100 == 0 && groomingTicks > 0) {
                debug("grooming progress: " + groomingTicks + "/" + GROOMING_DURATION_TICKS);
            }
        }
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    /**
     * Find a grooming partner.
     */
    private AbstractHorse findGroomingPartner() {
        List<AbstractHorse> nearbyHorses = horse.level().getEntitiesOfClass(
            AbstractHorse.class,
            horse.getBoundingBox().inflate(GROOMING_SEARCH_RADIUS)
        );

        for (AbstractHorse other : nearbyHorses) {
            if (!isValidGroomingPartner(other)) {
                continue;
            }

            // Check distance
            double distance = horse.distanceTo(other);
            if (distance > MAX_GROOMING_DISTANCE) {
                continue;
            }

            // Random chance for mutual interest
            if (horse.getRandom().nextFloat() < GROOMING_INITIATION_CHANCE) {
                return other;
            }
        }

        return null;
    }

    /**
     * Check if another horse is a valid grooming partner.
     */
    private boolean isValidGroomingPartner(AbstractHorse other) {
        // Not self
        if (other == horse) {
            return false;
        }

        // Must be alive
        if (!other.isAlive()) {
            return false;
        }

        // Must be same species
        if (other.getType() != horseType) {
            return false;
        }

        // Must be wild or tame together
        if (horse.isTamed() != other.isTamed()) {
            return false;
        }

        // Don't groom if panicking
        if (isPartnerFleeing()) {
            return false;
        }

        // Don't interrupt if partner is busy
        if (other.isVehicle()) {
            return false;
        }

        return true;
    }

    /**
     * Move towards the grooming partner with path validation.
     */
    private void moveToPartner() {
        PathNavigation navigation = horse.getNavigation();
        Path path = navigation.createPath(groomingPartner, 0);

        if (path != null && path.canReach()) {
            navigation.moveTo(groomingPartner, MOVE_SPEED);
        }
    }

    /**
     * Play the grooming sound.
     */
    private void playGroomingSound() {
        if (horse.level().isClientSide) {
            return;
        }

        var sound = getGroomingSound();
        horse.level().playSound(null, horse.blockPosition(), sound,
            net.minecraft.sounds.SoundSource.NEUTRAL, 0.5f, 1.0f);
    }

    /**
     * Get the grooming sound for this horse type.
     */
    private net.minecraft.sounds.SoundEvent getGroomingSound() {
        if (horseType == EntityType.DONKEY || horseType == EntityType.MULE) {
            return net.minecraft.sounds.SoundEvents.DONKEY_AMBIENT;
        }
        return net.minecraft.sounds.SoundEvents.HORSE_BREATHE;
    }

    /**
     * Spawn heart particles between horses.
     */
    private void spawnSocialParticles() {
        if (horse.level().isClientSide) {
            return;
        }

        // Spawn particles between the two horses
        Vec3 startPos = horse.position().add(0, horse.getBbHeight() * 0.6, 0);
        Vec3 endPos = groomingPartner.position().add(0, groomingPartner.getBbHeight() * 0.6, 0);
        Vec3 between = startPos.add(endPos).scale(0.5);

        for (int i = 0; i < 2; i++) {
            double offsetX = (horse.getRandom().nextDouble() - 0.5) * 0.3;
            double offsetY = (horse.getRandom().nextDouble() - 0.5) * 0.3;
            double offsetZ = (horse.getRandom().nextDouble() - 0.5) * 0.3;

            ((net.minecraft.server.level.ServerLevel) horse.level()).sendParticles(
                net.minecraft.core.particles.ParticleTypes.HEART,
                between.x + offsetX,
                between.y + offsetY,
                between.z + offsetZ,
                1, 0, 0, 0, 0
            );
        }
    }

    /**
     * Check if this horse is fleeing.
     */
    private boolean isFleeing() {
        EcologyComponent component = getComponent();
        if (component == null) {
            return false;
        }
        return component.getHandleTag(FLEEING_KEY).getBoolean("is_fleeing");
    }

    /**
     * Check if the grooming partner is fleeing.
     */
    private boolean isPartnerFleeing() {
        if (groomingPartner == null) {
            return false;
        }

        EcologyComponent component = getComponent(groomingPartner);
        if (component == null) {
            return false;
        }
        return component.getHandleTag(FLEEING_KEY).getBoolean("is_fleeing");
    }

    /**
     * Get the ecology component for this horse.
     */
    private EcologyComponent getComponent() {
        if (!(horse instanceof EcologyAccess access)) {
            return null;
        }
        return access.betterEcology$getEcologyComponent();
    }

    /**
     * Get the ecology component for another horse.
     */
    private EcologyComponent getComponent(AbstractHorse horse) {
        if (!(horse instanceof EcologyAccess access)) {
            return null;
        }
        return access.betterEcology$getEcologyComponent();
    }

    /**
     * Debug logging with consistent prefix.
     */
    private void debug(String message) {
        lastDebugMessage = message;
        if (BehaviorLogger.isMinimal() || BetterEcology.DEBUG_MODE) {
            String prefix = "[SocialGrooming] Horse #" + horse.getId() + " ";
            BehaviorLogger.info(prefix + message);
        }
    }

    /**
     * Get last debug message for external display.
     */
    public String getLastDebugMessage() {
        return lastDebugMessage;
    }

    /**
     * Get current state info for debug display.
     */
    public String getDebugState() {
        String partnerId = groomingPartner != null ? "#" + groomingPartner.getId() : "none";
        String typeName = horseType.toShortString();
        return String.format("type=%s, partner=%s, ticks=%d/%d, cooldown=%d, fleeing=%b",
            typeName, partnerId, groomingTicks, GROOMING_DURATION_TICKS, cooldownTicks, isFleeing());
    }
}
