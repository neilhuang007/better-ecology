package me.javavirtualenv.ecology.ai;

import me.javavirtualenv.behavior.chicken.GrainEatingBehavior;
import me.javavirtualenv.behavior.chicken.SeedDroppingBehavior;
import me.javavirtualenv.ecology.EcologyComponent;
import me.javavirtualenv.ecology.api.EcologyAccess;
import me.javavirtualenv.ecology.handles.HungerHandle;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.animal.Chicken;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.core.particles.ParticleTypes;

/**
 * Goal for chickens to eat crops from farmland.
 * Chickens actively seek out and consume crops like wheat, carrots, and potatoes.
 * Eating crops triggers seed dropping behavior for natural dispersal.
 */
public class ChickenEatGrainGoal extends Goal {
    private final Chicken chicken;
    private final Level level;
    private final GrainEatingBehavior grainEatingBehavior;
    private final SeedDroppingBehavior seedDroppingBehavior;
    private final EcologyComponent component;

    private final double hungerThreshold;
    private final int searchCooldown;
    private int cooldownTicks;

    public ChickenEatGrainGoal(Chicken chicken,
                              GrainEatingBehavior grainEatingBehavior,
                              SeedDroppingBehavior seedDroppingBehavior,
                              double hungerThreshold,
                              int searchCooldown) {
        this.chicken = chicken;
        this.level = chicken.level();
        this.grainEatingBehavior = grainEatingBehavior;
        this.seedDroppingBehavior = seedDroppingBehavior;
        this.component = getEcologyComponent();

        this.hungerThreshold = hungerThreshold;
        this.searchCooldown = searchCooldown;
        this.cooldownTicks = 0;
    }

    @Override
    public boolean canUse() {
        if (cooldownTicks > 0) {
            cooldownTicks--;
            return false;
        }

        if (chicken.isBaby()) {
            return false;
        }

        if (!isHungryEnough()) {
            return false;
        }

        if (grainEatingBehavior.getState() != GrainEatingBehavior.EatingState.SEARCHING) {
            return true;
        }

        BlockPos nearbyCrop = grainEatingBehavior.findNearestMatureCrop(level, chicken.blockPosition());
        if (nearbyCrop == null) {
            nearbyCrop = grainEatingBehavior.findNearestImmatureCrop(level, chicken.blockPosition());
        }

        return nearbyCrop != null;
    }

    @Override
    public boolean canContinueToUse() {
        return grainEatingBehavior.getState() == GrainEatingBehavior.EatingState.APPROACHING ||
               grainEatingBehavior.getState() == GrainEatingBehavior.EatingState.EATING;
    }

    @Override
    public void start() {
        playSearchSound();
    }

    @Override
    public void stop() {
        if (grainEatingBehavior.recentlyEaten()) {
            onCropEaten();
        }
        grainEatingBehavior.resetRecentlyEaten();
        cooldownTicks = searchCooldown;
    }

    @Override
    public void tick() {
        GrainEatingBehavior.EatingState state = grainEatingBehavior.getState();

        switch (state) {
            case EATING -> {
                if (level.getRandom().nextFloat() < 0.1f) {
                    playEatingSound();
                }

                if (level.getRandom().nextFloat() < 0.05f) {
                    spawnEatingParticles();
                }

                chicken.getNavigation().stop();
            }
            case APPROACHING -> {
                BlockPos target = grainEatingBehavior.getCurrentCrop();
                if (target != null) {
                    chicken.getLookControl().setLookAt(
                        target.getX() + 0.5,
                        target.getY() + 0.5,
                        target.getZ() + 0.5
                    );
                }
            }
            case SEARCHING -> {
                if (level.getRandom().nextFloat() < 0.02f) {
                    chicken.getLookControl().setLookAt(
                        chicken.getX() + (level.getRandom().nextFloat() - 0.5) * 10,
                        chicken.getY() + level.getRandom().nextFloat() * 2,
                        chicken.getZ() + (level.getRandom().nextFloat() - 0.5) * 10
                    );
                }
            }
        }
    }

    private boolean isHungryEnough() {
        if (component == null || !component.hasProfile()) {
            return chicken.getRandom().nextFloat() < 0.3;
        }

        double currentHunger = getCurrentHunger();
        double maxHunger = getMaxHunger();
        double hungerPercent = currentHunger / maxHunger;

        return hungerPercent <= hungerThreshold;
    }

    private void onCropEaten() {
        playEatSuccessSound();
        spawnEatSuccessParticles();

        if (seedDroppingBehavior != null) {
            SeedDroppingBehavior.SeedType seedType = determineSeedType();
            if (seedType != null) {
                seedDroppingBehavior.ateSeed(seedType);
            }
        }

        if (component != null) {
            restoreHunger(15);
        }
    }

    private SeedDroppingBehavior.SeedType determineSeedType() {
        BlockPos cropPos = grainEatingBehavior.getCurrentCrop();
        if (cropPos == null) {
            return null;
        }

        var blockState = level.getBlockState(cropPos);
        var block = blockState.getBlock();

        if (block == Blocks.WHEAT) {
            return SeedDroppingBehavior.SeedType.WHEAT;
        } else if (block == Blocks.BEETROOTS) {
            return SeedDroppingBehavior.SeedType.BEETROOT;
        } else if (block == Blocks.MELON_STEM) {
            return SeedDroppingBehavior.SeedType.MELON;
        } else if (block == Blocks.PUMPKIN_STEM) {
            return SeedDroppingBehavior.SeedType.PUMPKIN;
        }

        return SeedDroppingBehavior.SeedType.WHEAT;
    }

    private void restoreHunger(int amount) {
        if (component == null) {
            return;
        }

        var tag = component.getHandleTag("hunger");
        int currentHunger = tag.contains("hunger") ? tag.getInt("hunger") : 0;
        int maxHunger = getMaxHungerInt();
        int newHunger = Math.min(maxHunger, currentHunger + amount);
        tag.putInt("hunger", newHunger);
    }

    private void playSearchSound() {
        if (level.getRandom().nextFloat() < 0.3f) {
            level.playSound(
                null,
                chicken.getX(),
                chicken.getY(),
                chicken.getZ(),
                SoundEvents.CHICKEN_AMBIENT,
                SoundSource.NEUTRAL,
                0.3f,
                1.2f
            );
        }
    }

    private void playEatingSound() {
        level.playSound(
            null,
            chicken.getX(),
            chicken.getY(),
            chicken.getZ(),
            SoundEvents.CHICKEN_AMBIENT,
            SoundSource.NEUTRAL,
            0.4f,
            1.5f
        );
    }

    private void playEatSuccessSound() {
        level.playSound(
            null,
            chicken.getX(),
            chicken.getY(),
            chicken.getZ(),
            SoundEvents.CHICKEN_AMBIENT,
            SoundSource.NEUTRAL,
            0.5f,
            0.8f
        );
    }

    private void spawnEatingParticles() {
        BlockPos pos = chicken.blockPosition();

        for (int i = 0; i < 2; i++) {
            double offsetX = level.getRandom().nextGaussian() * 0.2;
            double offsetY = level.getRandom().nextGaussian() * 0.2;
            double offsetZ = level.getRandom().nextGaussian() * 0.2;

            level.addParticle(
                ParticleTypes.ITEM,
                pos.getX() + 0.5 + offsetX,
                pos.getY() + 0.5 + offsetY,
                pos.getZ() + 0.5 + offsetZ,
                0, -0.1, 0
            );
        }
    }

    private void spawnEatSuccessParticles() {
        BlockPos pos = chicken.blockPosition();

        for (int i = 0; i < 5; i++) {
            double offsetX = level.getRandom().nextGaussian() * 0.3;
            double offsetY = level.getRandom().nextGaussian() * 0.3;
            double offsetZ = level.getRandom().nextGaussian() * 0.3;

            level.addParticle(
                ParticleTypes.HEART,
                pos.getX() + 0.5 + offsetX,
                pos.getY() + 0.5 + offsetY,
                pos.getZ() + 0.5 + offsetZ,
                0, 0.05, 0
            );
        }
    }

    private double getCurrentHunger() {
        if (component == null) {
            return 50;
        }
        var tag = component.getHandleTag("hunger");
        return tag.contains("hunger") ? tag.getInt("hunger") : 50;
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

    private int getMaxHungerInt() {
        return (int) getMaxHunger();
    }

    private EcologyComponent getEcologyComponent() {
        if (!(chicken instanceof EcologyAccess access)) {
            return null;
        }
        return access.betterEcology$getEcologyComponent();
    }
}
