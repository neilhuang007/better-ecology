package me.javavirtualenv.ecology.ai;

import me.javavirtualenv.behavior.chicken.EggLayingBehavior;
import me.javavirtualenv.ecology.EcologyComponent;
import me.javavirtualenv.ecology.api.EcologyAccess;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.animal.Chicken;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.item.Items;

/**
 * Goal for chickens to lay eggs.
 * Chickens will seek out nesting locations and lay eggs periodically.
 * Egg laying is influenced by hunger and energy levels.
 */
public class ChickenLayEggGoal extends Goal {
    private final Chicken chicken;
    private final Level level;
    private final EggLayingBehavior eggLayingBehavior;
    private final EcologyComponent component;

    private final int minLayInterval;
    private final int maxLayInterval;
    private final double hungerThreshold;
    private final double energyThreshold;
    private final double goldenEggChance;

    private int timeUntilNextEgg;
    private boolean hasFoundNest;

    public ChickenLayEggGoal(Chicken chicken, EggLayingBehavior eggLayingBehavior,
                            int minLayInterval, int maxLayInterval,
                            double hungerThreshold, double energyThreshold,
                            double goldenEggChance) {
        this.chicken = chicken;
        this.level = chicken.level();
        this.eggLayingBehavior = eggLayingBehavior;
        this.component = getEcologyComponent();

        this.minLayInterval = minLayInterval;
        this.maxLayInterval = maxLayInterval;
        this.hungerThreshold = hungerThreshold;
        this.energyThreshold = energyThreshold;
        this.goldenEggChance = goldenEggChance;

        this.timeUntilNextEgg = randomInterval();
        this.hasFoundNest = false;
    }

    @Override
    public boolean canUse() {
        if (chicken.isBaby()) {
            return false;
        }

        if (timeUntilNextEgg > 0) {
            timeUntilNextEgg--;
            return false;
        }

        if (!meetsRequirements()) {
            return false;
        }

        eggLayingBehavior.setReadyToLay(true);
        return eggLayingBehavior.getState() != EggLayingBehavior.NestingState.NOT_READY;
    }

    @Override
    public boolean canContinueToUse() {
        return !eggLayingBehavior.shouldLayNow();
    }

    @Override
    public void start() {
        hasFoundNest = eggLayingBehavior.getCurrentNest() != null;
    }

    @Override
    public void stop() {
        if (eggLayingBehavior.shouldLayNow()) {
            layEgg();
        }

        eggLayingBehavior.resetAfterLaying();
        timeUntilNextEgg = randomInterval();
        hasFoundNest = false;
    }

    @Override
    public void tick() {
        if (eggLayingBehavior.getState() == EggLayingBehavior.NestingState.NESTING) {
            if (level.getRandom().nextFloat() < 0.02f) {
                playCluckingSound();
            }

            if (level.getRandom().nextFloat() < 0.01f) {
                spawnNestingParticles();
            }
        }
    }

    private boolean meetsRequirements() {
        if (component == null || !component.hasProfile()) {
            return true;
        }

        double currentHunger = getCurrentHunger();
        double maxHunger = getMaxHunger();
        double hungerPercent = currentHunger / maxHunger;

        double currentEnergy = getCurrentEnergy();
        double maxEnergy = getMaxEnergy();
        double energyPercent = currentEnergy / maxEnergy;

        return hungerPercent >= hungerThreshold && energyPercent >= energyThreshold;
    }

    private void layEgg() {
        BlockPos layPos = getEggPosition();

        if (goldenEggChance > 0 && level.getRandom().nextDouble() < goldenEggChance) {
            layGoldenEgg(layPos);
        } else {
            layNormalEgg(layPos);
        }

        playLaySound();
        spawnLayParticles(layPos);
    }

    private BlockPos getEggPosition() {
        BlockPos nestPos = eggLayingBehavior.getCurrentNest();
        if (nestPos != null) {
            return nestPos.above();
        }

        return chicken.blockPosition();
    }

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

    private void layGoldenEgg(BlockPos pos) {
        net.minecraft.world.entity.Entity goldenEgg = chicken.spawnAtLocation(Items.GOLDEN_APPLE);
        if (goldenEgg != null) {
            goldenEgg.setDeltaMovement(
                (level.getRandom().nextFloat() - 0.5) * 0.1,
                level.getRandom().nextFloat() * 0.1,
                (level.getRandom().nextFloat() - 0.5) * 0.1
            );
        }

        level.playSound(null, pos, SoundEvents.GOAT_SCREAMING_RAM_IMPACT, SoundSource.NEUTRAL, 1.0f, 1.0f);

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

    private int randomInterval() {
        return level.getRandom().nextIntBetweenInclusive(minLayInterval, maxLayInterval);
    }

    private double getCurrentHunger() {
        if (component == null) {
            return 100;
        }
        var tag = component.getHandleTag("hunger");
        return tag.contains("hunger") ? tag.getInt("hunger") : 100;
    }

    private double getMaxHunger() {
        if (component == null) {
            return 100;
        }
        var profile = component.profile();
        if (profile == null) {
            return 100;
        }
        return profile.getInt("hunger.max_value", 100);
    }

    private double getCurrentEnergy() {
        if (component == null) {
            return 100;
        }
        var tag = component.getHandleTag("energy");
        return tag.contains("energy") ? tag.getInt("energy") : 100;
    }

    private double getMaxEnergy() {
        if (component == null) {
            return 100;
        }
        var profile = component.profile();
        if (profile == null) {
            return 100;
        }
        return profile.getInt("energy.max_value", 100);
    }

    private EcologyComponent getEcologyComponent() {
        if (!(chicken instanceof EcologyAccess access)) {
            return null;
        }
        return access.betterEcology$getEcologyComponent();
    }
}
