package me.javavirtualenv.ecology.ai;

import me.javavirtualenv.BetterEcology;
import me.javavirtualenv.behavior.chicken.EggLayingBehavior;
import me.javavirtualenv.debug.BehaviorLogger;
import me.javavirtualenv.ecology.EcologyComponent;
import me.javavirtualenv.ecology.api.EcologyAccess;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.animal.Chicken;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

import java.util.EnumSet;

/**
 * Goal for chickens to lay eggs.
 * Chickens will seek out nesting locations and lay eggs periodically.
 * Egg laying is influenced by hunger and energy levels.
 */
public class ChickenLayEggGoal extends Goal {

    // NBT keys
    private static final String EGG_COOLDOWN_KEY = "egg_lay_cooldown";
    private static final String LAST_NEST_POS_KEY = "last_nest_pos";

    // Configuration constants
    private static final int MIN_LAY_INTERVAL = 4800; // ~4 minutes
    private static final int MAX_LAY_INTERVAL = 9600; // ~8 minutes
    private static final double HUNGER_THRESHOLD = 0.3;
    private static final double ENERGY_THRESHOLD = 0.3;
    private static final double GOLDEN_EGG_CHANCE = 0.01;

    // Instance fields
    private final Chicken chicken;
    private final Level level;
    private final EggLayingBehavior eggLayingBehavior;

    private int cooldownTicks;
    private BlockPos nestPos;
    private boolean isNesting;

    // Debug info
    private String lastDebugMessage = "";

    public ChickenLayEggGoal(Chicken chicken, EggLayingBehavior eggLayingBehavior) {
        this.chicken = chicken;
        this.level = chicken.level();
        this.eggLayingBehavior = eggLayingBehavior;
        this.setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        // Client-side only runs visual logic
        if (chicken.level().isClientSide) {
            return false;
        }

        if (chicken.isBaby()) {
            return false;
        }

        // Load cooldown from NBT
        CompoundTag breedingTag = getBreedingTag();
        if (breedingTag != null) {
            cooldownTicks = breedingTag.getInt(EGG_COOLDOWN_KEY);
            if (cooldownTicks > 0) {
                breedingTag.putInt(EGG_COOLDOWN_KEY, cooldownTicks - 1);
                return false;
            }
        }

        // Check if meets requirements
        if (!meetsRequirements()) {
            return false;
        }

        // Ready to lay
        eggLayingBehavior.setReadyToLay(true);
        isNesting = eggLayingBehavior.getState() != EggLayingBehavior.NestingState.NOT_READY;

        if (isNesting) {
            nestPos = eggLayingBehavior.getCurrentNest();
            debug("STARTING: egg laying behavior (nest at " +
                  (nestPos != null ? (nestPos.getX() + "," + nestPos.getZ()) : "none") + ")");
        }

        return isNesting;
    }

    @Override
    public boolean canContinueToUse() {
        return !eggLayingBehavior.shouldLayNow();
    }

    @Override
    public void start() {
        nestPos = eggLayingBehavior.getCurrentNest();
    }

    @Override
    public void stop() {
        if (eggLayingBehavior.shouldLayNow()) {
            layEgg();
        }

        eggLayingBehavior.resetAfterLaying();

        // Set new cooldown in NBT
        CompoundTag breedingTag = getBreedingTag();
        if (breedingTag != null) {
            int newCooldown = level.getRandom().nextIntBetweenInclusive(MIN_LAY_INTERVAL, MAX_LAY_INTERVAL);
            breedingTag.putInt(EGG_COOLDOWN_KEY, newCooldown);
        }

        nestPos = null;
        isNesting = false;

        debug("egg laid, next egg in " + cooldownTicks + " ticks");
    }

    @Override
    public void tick() {
        if (eggLayingBehavior.getState() == EggLayingBehavior.NestingState.NESTING) {
            // Occasional clucking while nesting
            if (level.getRandom().nextFloat() < 0.02f) {
                playCluckingSound();
            }

            // Occasional heart particles while nesting
            if (level.getRandom().nextFloat() < 0.01f) {
                spawnNestingParticles();
            }

            // Log progress every second
            if (chicken.tickCount % 20 == 0) {
                debug("nesting at " +
                      (nestPos != null ? (nestPos.getX() + "," + nestPos.getZ()) : "current position"));
            }
        }
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    /**
     * Check if chicken meets requirements to lay egg.
     */
    private boolean meetsRequirements() {
        EcologyComponent component = getComponent();
        if (component == null) {
            return true;
        }

        int currentHunger = getHungerLevel();
        int maxHunger = getMaxHunger();
        double hungerPercent = maxHunger > 0 ? (double) currentHunger / maxHunger : 1.0;

        int currentEnergy = getEnergyLevel();
        int maxEnergy = getMaxEnergy();
        double energyPercent = maxEnergy > 0 ? (double) currentEnergy / maxEnergy : 1.0;

        return hungerPercent >= HUNGER_THRESHOLD && energyPercent >= ENERGY_THRESHOLD;
    }

    /**
     * Lay the egg at current position or nest.
     */
    private void layEgg() {
        BlockPos layPos = getEggPosition();

        boolean isGolden = GOLDEN_EGG_CHANCE > 0 &&
                          level.getRandom().nextDouble() < GOLDEN_EGG_CHANCE;

        if (isGolden) {
            layGoldenEgg(layPos);
            debug("laid GOLDEN EGG at " + layPos.getX() + "," + layPos.getZ());
        } else {
            layNormalEgg(layPos);
        }

        playLaySound();
        spawnLayParticles(layPos);
    }

    /**
     * Get position to lay egg.
     */
    private BlockPos getEggPosition() {
        BlockPos nest = eggLayingBehavior.getCurrentNest();
        if (nest != null) {
            return nest.above();
        }
        return chicken.blockPosition();
    }

    /**
     * Lay a normal egg.
     */
    private void layNormalEgg(BlockPos pos) {
        net.minecraft.world.entity.Entity egg = chicken.spawnAtLocation(Items.EGG);
        if (egg != null) {
            egg.setDeltaMovement(
                (level.getRandom().nextFloat() - 0.5) * 0.1,
                level.getRandom().nextFloat() * 0.1,
                (level.getRandom().nextFloat() - 0.5) * 0.1
            );
        }
    }

    /**
     * Lay a golden egg (rare).
     */
    private void layGoldenEgg(BlockPos pos) {
        net.minecraft.world.entity.Entity goldenEgg = chicken.spawnAtLocation(Items.GOLDEN_APPLE);
        if (goldenEgg != null) {
            goldenEgg.setDeltaMovement(
                (level.getRandom().nextFloat() - 0.5) * 0.1,
                level.getRandom().nextFloat() * 0.1,
                (level.getRandom().nextFloat() - 0.5) * 0.1
            );
        }

        level.playSound(null, pos, SoundEvents.GOAT_SCREAMING_RAM_IMPACT,
                       SoundSource.NEUTRAL, 1.0f, 1.0f);

        for (int i = 0; i < 10; i++) {
            double offsetX = level.getRandom().nextGaussian() * 0.3;
            double offsetY = level.getRandom().nextGaussian() * 0.3;
            double offsetZ = level.getRandom().nextGaussian() * 0.3;

            level.addParticle(
                ParticleTypes.TOTEM_OF_UNDYING,
                pos.getX() + 0.5 + offsetX,
                pos.getY() + 0.5 + offsetY,
                pos.getZ() + 0.5 + offsetZ,
                0, 0.1, 0
            );
        }
    }

    /**
     * Play clucking sound.
     */
    private void playCluckingSound() {
        level.playSound(
            null,
            chicken.getX(),
            chicken.getY(),
            chicken.getZ(),
            SoundEvents.CHICKEN_AMBIENT,
            SoundSource.NEUTRAL,
            0.5f,
            level.getRandom().nextFloat() * 0.2f + 0.9f
        );
    }

    /**
     * Play egg laying sound.
     */
    private void playLaySound() {
        level.playSound(
            null,
            chicken.getX(),
            chicken.getY(),
            chicken.getZ(),
            SoundEvents.CHICKEN_EGG,
            SoundSource.NEUTRAL,
            1.0f,
            level.getRandom().nextFloat() * 0.2f + 0.9f
        );
    }

    /**
     * Spawn heart particles while nesting.
     */
    private void spawnNestingParticles() {
        BlockPos pos = chicken.blockPosition();

        for (int i = 0; i < 3; i++) {
            double offsetX = level.getRandom().nextGaussian() * 0.2;
            double offsetY = level.getRandom().nextGaussian() * 0.2;
            double offsetZ = level.getRandom().nextGaussian() * 0.2;

            level.addParticle(
                ParticleTypes.HEART,
                pos.getX() + 0.5 + offsetX,
                pos.getY() + 0.5 + offsetY,
                pos.getZ() + 0.5 + offsetZ,
                0, 0.05, 0
            );
        }
    }

    /**
     * Spawn particles when egg is laid.
     */
    private void spawnLayParticles(BlockPos pos) {
        for (int i = 0; i < 5; i++) {
            double offsetX = level.getRandom().nextGaussian() * 0.2;
            double offsetY = level.getRandom().nextGaussian() * 0.2;
            double offsetZ = level.getRandom().nextGaussian() * 0.2;

            level.addParticle(
                ParticleTypes.ITEM_SLIME,
                pos.getX() + 0.5 + offsetX,
                pos.getY() + 0.5 + offsetY,
                pos.getZ() + 0.5 + offsetZ,
                0, 0, 0
            );
        }
    }

    /**
     * Get breeding tag from NBT.
     */
    private CompoundTag getBreedingTag() {
        EcologyComponent component = getComponent();
        if (component == null) {
            return null;
        }
        return component.getHandleTag("breeding");
    }

    /**
     * Get hunger level from NBT.
     */
    private int getHungerLevel() {
        CompoundTag tag = getHungerTag();
        if (tag == null) {
            return 100;
        }
        return tag.getInt("hunger");
    }

    /**
     * Get max hunger from profile.
     */
    private int getMaxHunger() {
        EcologyComponent component = getComponent();
        if (component == null || component.profile() == null) {
            return 100;
        }
        return component.profile().getInt("hunger.max_value", 100);
    }

    /**
     * Get energy level from NBT.
     */
    private int getEnergyLevel() {
        CompoundTag tag = getEnergyTag();
        if (tag == null) {
            return 100;
        }
        return tag.getInt("energy");
    }

    /**
     * Get max energy from profile.
     */
    private int getMaxEnergy() {
        EcologyComponent component = getComponent();
        if (component == null || component.profile() == null) {
            return 100;
        }
        return component.profile().getInt("energy.max_value", 100);
    }

    /**
     * Get hunger tag from NBT.
     */
    private CompoundTag getHungerTag() {
        EcologyComponent component = getComponent();
        if (component == null) {
            return null;
        }
        return component.getHandleTag("hunger");
    }

    /**
     * Get energy tag from NBT.
     */
    private CompoundTag getEnergyTag() {
        EcologyComponent component = getComponent();
        if (component == null) {
            return null;
        }
        return component.getHandleTag("energy");
    }

    /**
     * Get the ecology component for this chicken.
     */
    private EcologyComponent getComponent() {
        if (!(chicken instanceof EcologyAccess access)) {
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
            String prefix = "[ChickenLayEgg] Chicken #" + chicken.getId() + " ";
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
        int hunger = getHungerLevel();
        int energy = getEnergyLevel();
        EggLayingBehavior.NestingState state = eggLayingBehavior.getState();

        return String.format(
            "hunger=%d/%d, energy=%d/%d, state=%s, nest=%s, cooldown=%d",
            hunger,
            getMaxHunger(),
            energy,
            getMaxEnergy(),
            state.name().toLowerCase(),
            nestPos != null ? (nestPos.getX() + "," + nestPos.getZ()) : "none",
            cooldownTicks
        );
    }
}
