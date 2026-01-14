package me.javavirtualenv.behavior.core;

import java.util.EnumSet;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.animal.Chicken;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Enhanced egg laying goal that ties into the hunger system.
 * Chickens will only lay eggs when well-fed (above a certain hunger threshold).
 * The egg laying frequency is influenced by the chicken's hunger level.
 */
public class EnhancedEggLayingGoal extends Goal {
    private static final Logger LOGGER = LoggerFactory.getLogger(EnhancedEggLayingGoal.class);

    private static final int WELL_FED_EGG_INTERVAL = 6000;
    private static final int NORMAL_EGG_INTERVAL = 8000;
    private static final float WELL_FED_THRESHOLD = 70f;
    private static final float MINIMUM_EGG_LAYING_HUNGER = 40f;
    private static final float HUNGER_COST_PER_EGG = 8f;

    private final Chicken chicken;
    private final Level level;

    private int eggLayTime;

    public EnhancedEggLayingGoal(Chicken chicken) {
        this.chicken = chicken;
        this.level = chicken.level();
        this.eggLayTime = calculateNextEggTime();
        this.setFlags(EnumSet.of(Goal.Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        float hunger = AnimalNeeds.getHunger(this.chicken);

        if (hunger < MINIMUM_EGG_LAYING_HUNGER) {
            return false;
        }

        if (this.chicken.isBaby()) {
            return false;
        }

        return this.eggLayTime <= 0;
    }

    @Override
    public boolean canContinueToUse() {
        return false;
    }

    @Override
    public void start() {
        layEgg();
        this.eggLayTime = calculateNextEggTime();
    }

    @Override
    public void tick() {
        if (this.eggLayTime > 0) {
            this.eggLayTime--;
        }
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    private void layEgg() {
        float currentHunger = AnimalNeeds.getHunger(this.chicken);

        this.level.playSound(
            null,
            this.chicken.getX(),
            this.chicken.getY(),
            this.chicken.getZ(),
            SoundEvents.CHICKEN_EGG,
            SoundSource.NEUTRAL,
            1.0F,
            (this.chicken.getRandom().nextFloat() - this.chicken.getRandom().nextFloat()) * 0.2F + 1.0F
        );

        this.chicken.spawnAtLocation(Items.EGG);

        AnimalNeeds.modifyHunger(this.chicken, -HUNGER_COST_PER_EGG);

        LOGGER.debug("{} laid an egg. Hunger decreased by {} (now: {})",
            this.chicken.getName().getString(),
            HUNGER_COST_PER_EGG,
            AnimalNeeds.getHunger(this.chicken)
        );
    }

    private int calculateNextEggTime() {
        float hunger = AnimalNeeds.getHunger(this.chicken);

        int baseInterval;
        if (hunger >= WELL_FED_THRESHOLD) {
            baseInterval = WELL_FED_EGG_INTERVAL;
        } else {
            baseInterval = NORMAL_EGG_INTERVAL;
        }

        int variance = this.chicken.getRandom().nextInt(baseInterval / 4);
        int nextTime = baseInterval + variance;

        LOGGER.debug("{} next egg in {} ticks (hunger: {})",
            this.chicken.getName().getString(),
            nextTime,
            hunger
        );

        return nextTime;
    }
}
