package me.javavirtualenv.behavior.chicken;

import me.javavirtualenv.behavior.core.BehaviorContext;
import me.javavirtualenv.behavior.core.SteeringBehavior;
import me.javavirtualenv.behavior.core.Vec3d;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Items;
import net.minecraft.core.BlockPos;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * Seed dropping behavior for chickens.
 * Chickens periodically drop seeds based on what they've eaten.
 * This simulates natural seed dispersal through digestion.
 */
public class SeedDroppingBehavior extends SteeringBehavior {
    private final Random random;
    private final int dropInterval;
    private final double dropChance;

    private int ticksSinceLastDrop;
    private int seedsEaten;
    private final Map<SeedType, Integer> seedStomach;

    private static final int MAX_SEEDS_IN_STOMACH = 5;
    private static final double DIGESTION_EFFICIENCY = 0.3;

    public SeedDroppingBehavior(int dropInterval, double dropChance) {
        this.random = new Random();
        this.dropInterval = dropInterval;
        this.dropChance = dropChance;
        this.ticksSinceLastDrop = 0;
        this.seedsEaten = 0;
        this.seedStomach = new HashMap<>();
    }

    @Override
    public Vec3d calculate(BehaviorContext context) {
        ticksSinceLastDrop++;

        if (ticksSinceLastDrop >= dropInterval) {
            attemptSeedDrop(context);
            ticksSinceLastDrop = 0;
        }

        return new Vec3d();
    }

    /**
     * Records that the chicken ate a seed type.
     * Seeds accumulate in the "stomach" and can be dropped later.
     */
    public void ateSeed(SeedType seedType) {
        seedsEaten++;
        seedStomach.put(seedType, seedStomach.getOrDefault(seedType, 0) + 1);

        if (seedsEaten > MAX_SEEDS_IN_STOMACH) {
            digestOldestSeeds();
        }
    }

    /**
     * Attempts to drop a seed based on chance and stomach contents.
     * Seeds have a chance to pass through undigested (viable for planting).
     */
    private void attemptSeedDrop(BehaviorContext context) {
        if (seedStomach.isEmpty() || random.nextDouble() > dropChance) {
            return;
        }

        SeedType seedToDrop = selectRandomSeedType();
        if (seedToDrop == null) {
            return;
        }

        boolean seedViable = random.nextDouble() < DIGESTION_EFFICIENCY;

        if (seedViable && context.getLevel() != null && context.getPosition() != null) {
            dropSeedAtLocation(context, seedToDrop);
        }

        seedStomach.put(seedToDrop, seedStomach.get(seedToDrop) - 1);
        if (seedStomach.get(seedToDrop) <= 0) {
            seedStomach.remove(seedToDrop);
        }
    }

    private SeedType selectRandomSeedType() {
        if (seedStomach.isEmpty()) {
            return null;
        }

        int totalSeeds = seedStomach.values().stream().mapToInt(Integer::intValue).sum();
        int selected = random.nextInt(totalSeeds);
        int currentCount = 0;

        for (Map.Entry<SeedType, Integer> entry : seedStomach.entrySet()) {
            currentCount += entry.getValue();
            if (currentCount > selected) {
                return entry.getKey();
            }
        }

        return null;
    }

    private void dropSeedAtLocation(BehaviorContext context, SeedType seedType) {
        if (!(context.getLevel() instanceof net.minecraft.world.level.Level level)) {
            return;
        }

        BlockPos pos = context.getBlockPos();
        double x = pos.getX() + 0.5;
        double y = pos.getY();
        double z = pos.getZ() + 0.5;

        ItemEntity itemEntity = new ItemEntity(
            level,
            x,
            y,
            z,
            seedType.getItemStack()
        );

        itemEntity.setDeltaMovement(
            (random.nextDouble() - 0.5) * 0.1,
            random.nextDouble() * 0.1,
            (random.nextDouble() - 0.5) * 0.1
        );

        level.addFreshEntity(itemEntity);
    }

    private void digestOldestSeeds() {
        int seedsToRemove = seedsEaten - MAX_SEEDS_IN_STOMACH;

        for (Map.Entry<SeedType, Integer> entry : seedStomach.entrySet()) {
            if (seedsToRemove <= 0) {
                break;
            }

            int remove = Math.min(entry.getValue(), seedsToRemove);
            entry.setValue(entry.getValue() - remove);
            seedsToRemove -= remove;
        }

        seedStomach.entrySet().removeIf(entry -> entry.getValue() <= 0);
        seedsEaten = seedStomach.values().stream().mapToInt(Integer::intValue).sum();
    }

    public int getSeedsEaten() {
        return seedsEaten;
    }

    public Map<SeedType, Integer> getSeedStomach() {
        return new HashMap<>(seedStomach);
    }

    public void clearSeedStomach() {
        seedStomach.clear();
        seedsEaten = 0;
    }

    /**
     * Types of seeds chickens can eat and potentially disperse.
     */
    public enum SeedType {
        WHEAT(Items.WHEAT_SEEDS),
        BEETROOT(Items.BEETROOT_SEEDS),
        MELON(Items.MELON_SEEDS),
        PUMPKIN(Items.PUMPKIN_SEEDS),
        TORCHFLOWER(Items.TORCHFLOWER_SEEDS),
        PITCHER_POD(Items.PITCHER_POD);

        private final net.minecraft.world.item.Item item;

        SeedType(net.minecraft.world.item.Item item) {
            this.item = item;
        }

        public net.minecraft.world.item.Item getItem() {
            return item;
        }

        public net.minecraft.world.item.ItemStack getItemStack() {
            return new net.minecraft.world.item.ItemStack(item);
        }
    }
}
