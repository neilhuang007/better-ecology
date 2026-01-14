package me.javavirtualenv.behavior.core;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.animal.Parrot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.EnumSet;
import java.util.List;

/**
 * Goal that makes parrots emit contact calls to maintain flock cohesion.
 * <p>
 * Based on real parrot behavior where parrots use contact calls to stay in touch
 * with flock members, especially when separated. This creates a "conversational"
 * soundscape where parrots call and answer each other.
 * <p>
 * Behavior:
 * <ul>
 *   <li>Emits contact calls every 30-60 seconds when with flock</li>
 *   <li>Increases call frequency (10-20 seconds) when separated from flock</li>
 *   <li>Nearby parrots respond with answering calls</li>
 *   <li>Creates realistic flock communication patterns</li>
 * </ul>
 */
public class ParrotContactCallingGoal extends Goal {
    private static final Logger LOGGER = LoggerFactory.getLogger(ParrotContactCallingGoal.class);

    private static final int NORMAL_CALL_INTERVAL_MIN = 600;
    private static final int NORMAL_CALL_INTERVAL_MAX = 1200;
    private static final int SEPARATED_CALL_INTERVAL_MIN = 200;
    private static final int SEPARATED_CALL_INTERVAL_MAX = 400;
    private static final double FLOCK_RADIUS = 12.0;
    private static final int RESPONSE_DELAY_MIN = 20;
    private static final int RESPONSE_DELAY_MAX = 60;
    private static final int MIN_FLOCK_SIZE = 1;

    private final Parrot parrot;
    private int ticksSinceLastCall;
    private int nextCallInterval;
    private boolean isSeparated;

    /**
     * Creates a new ParrotContactCallingGoal.
     *
     * @param parrot the parrot that will emit contact calls
     */
    public ParrotContactCallingGoal(Parrot parrot) {
        this.parrot = parrot;
        this.ticksSinceLastCall = 0;
        this.nextCallInterval = calculateNextCallInterval(false);
        this.setFlags(EnumSet.of(Goal.Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (this.parrot.isSilent()) {
            return false;
        }

        this.ticksSinceLastCall++;

        if (this.ticksSinceLastCall < this.nextCallInterval) {
            return false;
        }

        return true;
    }

    @Override
    public boolean canContinueToUse() {
        return false;
    }

    @Override
    public void start() {
        this.isSeparated = checkIfSeparated();

        emitContactCall();

        triggerNearbyResponses();

        this.ticksSinceLastCall = 0;
        this.nextCallInterval = calculateNextCallInterval(this.isSeparated);

        LOGGER.debug("{} emitted contact call (separated: {}). Next call in {} ticks",
                parrot.getName().getString(),
                isSeparated,
                nextCallInterval);
    }

    /**
     * Checks if the parrot is separated from its flock.
     *
     * @return true if no nearby flock members found
     */
    private boolean checkIfSeparated() {
        List<Parrot> nearbyParrots = findNearbyParrots();
        return nearbyParrots.size() < MIN_FLOCK_SIZE;
    }

    /**
     * Finds nearby parrots within flock radius.
     *
     * @return list of nearby parrots
     */
    private List<Parrot> findNearbyParrots() {
        return this.parrot.level().getEntitiesOfClass(
                Parrot.class,
                this.parrot.getBoundingBox().inflate(FLOCK_RADIUS),
                otherParrot -> otherParrot != this.parrot && otherParrot.isAlive()
        );
    }

    /**
     * Emits a contact call from this parrot.
     * Uses different sounds based on whether separated or with flock.
     */
    private void emitContactCall() {
        if (this.isSeparated) {
            this.parrot.playSound(SoundEvents.PARROT_AMBIENT, 1.0F, 1.0F);
        } else {
            float pitch = 0.8F + this.parrot.getRandom().nextFloat() * 0.4F;
            this.parrot.playSound(SoundEvents.PARROT_AMBIENT, 0.7F, pitch);
        }
    }

    /**
     * Triggers nearby parrots to respond with answering calls immediately.
     * Creates a conversational pattern where parrots answer each other.
     */
    private void triggerNearbyResponses() {
        List<Parrot> nearbyParrots = findNearbyParrots();

        for (Parrot nearbyParrot : nearbyParrots) {
            if (nearbyParrot.isSilent()) {
                continue;
            }

            float pitch = 0.9F + nearbyParrot.getRandom().nextFloat() * 0.3F;
            nearbyParrot.playSound(SoundEvents.PARROT_AMBIENT, 0.6F, pitch);

            LOGGER.debug("{} responded to contact call from {}",
                    nearbyParrot.getName().getString(),
                    parrot.getName().getString());
        }
    }

    /**
     * Calculates the interval until the next contact call.
     *
     * @param separated whether the parrot is separated from flock
     * @return ticks until next call
     */
    private int calculateNextCallInterval(boolean separated) {
        if (separated) {
            return SEPARATED_CALL_INTERVAL_MIN +
                    this.parrot.getRandom().nextInt(SEPARATED_CALL_INTERVAL_MAX - SEPARATED_CALL_INTERVAL_MIN + 1);
        } else {
            return NORMAL_CALL_INTERVAL_MIN +
                    this.parrot.getRandom().nextInt(NORMAL_CALL_INTERVAL_MAX - NORMAL_CALL_INTERVAL_MIN + 1);
        }
    }
}
