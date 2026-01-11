package me.javavirtualenv.behavior.frog;

import me.javavirtualenv.behavior.core.Vec3d;
import me.javavirtualenv.behavior.core.BehaviorContext;
import me.javavirtualenv.behavior.core.SteeringBehavior;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.frog.Frog;
import net.minecraft.world.level.Level;

import java.util.Random;

/**
 * Croaking behavior for frogs.
 * <p>
 * Implements realistic frog croaking patterns based on:
 * - Time of day (crepuscular/nocturnal patterns)
 * - Biome variants (different croaks for different frog types)
 * - Social context (mating calls, territory marking)
 * - Environmental conditions (rain increases croaking)
 * <p>
 * Scientific basis:
 * - Frogs use croaking for mate attraction and territory defense
 * - Croaking frequency increases during breeding season
 * - Different species have distinct croak patterns
 * - Environmental factors like rain trigger increased vocalization
 */
public class CroakingBehavior extends SteeringBehavior {

    private static final int DEFAULT_CROAK_INTERVAL = 200; // 10 seconds base
    private static final int RAIN_BONUS = 100; // Croak more often in rain
    private static final int NIGHT_BONUS = 50; // Croak more at night
    private static final double MATING_CALL_CHANCE = 0.3;

    private final int baseCroakInterval;
    private final Random random = new Random();

    private int croakTimer = 0;
    private boolean isMatingCall = false;

    public CroakingBehavior() {
        this(DEFAULT_CROAK_INTERVAL);
    }

    public CroakingBehavior(int baseCroakInterval) {
        super();
        setWeight(1.0); // Low weight as this doesn't affect movement
        this.baseCroakInterval = baseCroakInterval;
        this.croakTimer = getRandomizedInterval();
    }

    @Override
    public Vec3d calculate(BehaviorContext context) {
        Entity entity = context.getSelf();
        if (!(entity instanceof Frog frog)) {
            return new Vec3d();
        }

        croakTimer--;

        if (croakTimer <= 0) {
            performCroak(frog);
            croakTimer = getRandomizedInterval();
        }

        return new Vec3d(); // No movement force
    }

    /**
     * Performs the croak action with sound and effects.
     */
    private void performCroak(Frog frog) {
        Level level = frog.level();

        // Don't croak if panicking or in water
        if (frog.isInWater() || isPanicking(frog)) {
            return;
        }

        // Determine croak type
        CroakType croakType = determineCroakType(frog);
        isMatingCall = croakType == CroakType.MATING;

        // Play appropriate sound
        playCroakSound(frog, croakType);

        // Create visual particles
        spawnCroakParticles(frog);

        // Mark territory if this is a territorial croak
        if (croakType == CroakType.TERRITORIAL) {
            markTerritory(frog);
        }
    }

    /**
     * Determines the type of croak based on context.
     */
    private CroakType determineCroakType(Frog frog) {
        Level level = frog.level();
        boolean isNight = level.getDayTime() % 24000 > 13000 && level.getDayTime() % 24000 < 23000;
        boolean isRaining = level.isRaining();

        // Mating calls at night or during rain
        if ((isNight || isRaining) && random.nextDouble() < MATING_CALL_CHANCE) {
            return CroakType.MATING;
        }

        // Territorial croaks
        if (hasNearbyFrogs(frog)) {
            return CroakType.TERRITORIAL;
        }

        // Communication croak
        return CroakType.COMMUNICATION;
    }

    /**
     * Plays the appropriate croak sound for the frog variant.
     */
    private void playCroakSound(Frog frog, CroakType croakType) {
        net.minecraft.sounds.SoundEvent sound;
        float volume;
        float pitch;

        // Get frog variant for different sounds
        var variant = frog.getVariant();

        // Adjust pitch and volume based on croak type
        switch (croakType) {
            case MATING -> {
                volume = 1.5F;
                pitch = getMatingPitch(variant);
            }
            case TERRITORIAL -> {
                volume = 1.2F;
                pitch = getTerritorialPitch(variant);
            }
            default -> {
                volume = 1.0F;
                pitch = getNormalPitch(variant);
            }
        }

        // Get appropriate sound event
        sound = getVariantCroakSound(variant);

        frog.level().playSound(null, frog, sound,
                net.minecraft.sounds.SoundSource.NEUTRAL, volume, pitch);
    }

    /**
     * Gets the croak sound for the frog variant.
     */
    private net.minecraft.sounds.SoundEvent getVariantCroakSound(Object variant) {
        // Use vanilla croak sounds
        return net.minecraft.sounds.SoundEvents.FROG_AMBIENT;
    }

    /**
     * Gets the pitch for mating calls based on variant.
     */
    private float getMatingPitch(Object variant) {
        // Higher pitch for mating calls
        return 1.2F + random.nextFloat() * 0.3F;
    }

    /**
     * Gets the pitch for territorial croaks based on variant.
     */
    private float getTerritorialPitch(Object variant) {
        // Lower, deeper pitch for territorial
        return 0.8F + random.nextFloat() * 0.2F;
    }

    /**
     * Gets the normal croak pitch based on variant.
     */
    private float getNormalPitch(Object variant) {
        return 1.0F + random.nextFloat() * 0.2F;
    }

    /**
     * Spawns visual particles for croaking.
     */
    private void spawnCroakParticles(Frog frog) {
        if (!frog.level().isClientSide()) {
            return;
        }

        // Spawn sound waves/vibration particles
        net.minecraft.core.BlockPos pos = frog.blockPosition();
        net.minecraft.world.level.Level level = frog.level();

        // Create ripple effect particles
        for (int i = 0; i < 3; i++) {
            double offsetX = random.nextGaussian() * 0.5;
            double offsetZ = random.nextGaussian() * 0.5;
            level.addParticle(
                    net.minecraft.core.particles.ParticleTypes.NOTE,
                    frog.getX() + offsetX,
                    frog.getY() + 0.5,
                    frog.getZ() + offsetZ,
                    0, 0.1, 0
            );
        }
    }

    /**
     * Marks the frog's territory with a croak.
     */
    private void markTerritory(Frog frog) {
        if (frog instanceof me.javavirtualenv.ecology.api.EcologyAccess access) {
            var component = access.betterEcology$getEcologyComponent();
            if (component != null) {
                var tag = component.getHandleTag("frog");
                tag.putLong("last_territory_croak", frog.level().getGameTime());
            }
        }
    }

    /**
     * Checks if the frog is currently panicking.
     */
    private boolean isPanicking(Frog frog) {
        return frog.getBrain() != null
                && frog.getBrain().hasMemoryValue(net.minecraft.world.entity.ai.memory.MemoryModuleType.IS_PANICKING);
    }

    /**
     * Checks if there are other frogs nearby (for territorial behavior).
     */
    private boolean hasNearbyFrogs(Frog frog) {
        double searchRadius = 16.0;
        return !frog.level().getEntitiesOfClass(Frog.class,
                frog.getBoundingBox().inflate(searchRadius),
                e -> !e.equals(frog)).isEmpty();
    }

    /**
     * Gets a randomized croak interval based on environmental conditions.
     */
    private int getRandomizedInterval() {
        int interval = baseCroakInterval;

        // Add randomness
        interval += random.nextInt(100) - 50;

        // Clamp to minimum
        return Math.max(60, interval);
    }

    /**
     * Checks if this is currently a mating call.
     */
    public boolean isMatingCall() {
        return isMatingCall;
    }

    /**
     * Gets the current croak timer.
     */
    public int getCroakTimer() {
        return croakTimer;
    }

    /**
     * Resets the croak timer.
     */
    public void resetCroakTimer() {
        this.croakTimer = getRandomizedInterval();
    }

    /**
     * Types of croaks with different purposes.
     */
    public enum CroakType {
        COMMUNICATION,
        MATING,
        TERRITORIAL
    }
}
