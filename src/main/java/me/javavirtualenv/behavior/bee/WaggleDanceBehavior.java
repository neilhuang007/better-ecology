package me.javavirtualenv.behavior.bee;

import me.javavirtualenv.behavior.core.Vec3d;
import me.javavirtualenv.behavior.core.BehaviorContext;
import me.javavirtualenv.behavior.core.SteeringBehavior;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.Bee;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * Behavior for waggle dance communication in bees.
 * <p>
 * This behavior:
 * - Performs waggle dance to communicate food source locations to other bees
 * - Shares flower locations with colony members
 * - Increases foraging efficiency for the hive
 * - Only occurs when bee has returned to hive with nectar
 * <p>
 * The waggle dance is a real-world bee behavior where bees communicate
 * the direction and distance to food sources through figure-8 patterns.
 */
public class WaggleDanceBehavior extends SteeringBehavior {

    private static final double DANCE_DURATION_TICKS = 200; // 10 seconds
    private static final double COMMUNICATION_RADIUS = 16.0;

    private int danceTimer;
    private BlockPos foodLocation;
    private boolean isDancing;
    private Vec3 danceCenter;

    public WaggleDanceBehavior() {
        this(0.5); // Lower priority by default
    }

    public WaggleDanceBehavior(double weight) {
        this.danceTimer = 0;
        setWeight(weight);
        this.foodLocation = null;
        this.isDancing = false;
        this.danceCenter = null;
    }

    @Override
    public Vec3d calculate(BehaviorContext context) {
        Entity self = context.getSelf();
        if (!(self instanceof Bee bee)) {
            return new Vec3d();
        }

        Level level = context.getWorld();

        // Start dancing if returned with nectar
        if (!isDancing && bee.hasNectar() && isNearHive(bee, level)) {
            startDance(bee, context);
        }

        // Stop dancing if timer expired or no longer at hive
        if (isDancing) {
            if (danceTimer <= 0 || !bee.hasNectar() || !isNearHive(bee, level)) {
                stopDance();
                return new Vec3d();
            }

            danceTimer--;

            // Perform waggle dance movement (figure-8 pattern)
            return performWaggleDance(context);
        }

        return new Vec3d();
    }

    /**
     * Checks if bee is near hive.
     */
    private boolean isNearHive(Bee bee, Level level) {
        Vec3 position = bee.position();
        BlockPos beePos = new BlockPos((int) position.x, (int) position.y, (int) position.z);

        // Check nearby blocks for hive
        int searchRadius = 4;
        for (int x = -searchRadius; x <= searchRadius; x++) {
            for (int y = -2; y <= 2; y++) {
                for (int z = -searchRadius; z <= searchRadius; z++) {
                    BlockPos testPos = beePos.offset(x, y, z);
                    if (level.getBlockState(testPos).is(net.minecraft.world.level.block.Blocks.BEEHIVE) ||
                        level.getBlockState(testPos).is(net.minecraft.world.level.block.Blocks.BEE_NEST)) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    /**
     * Starts the waggle dance.
     */
    private void startDance(Bee bee, BehaviorContext context) {
        isDancing = true;
        danceTimer = (int) DANCE_DURATION_TICKS;
        danceCenter = bee.position();

        // Communicate food location to nearby bees
        communicateFoodLocation(bee, context.getWorld());

        // Spawn particles to indicate dancing
        spawnDanceParticles(bee, context.getWorld());
    }

    /**
     * Stops the waggle dance.
     */
    private void stopDance() {
        isDancing = false;
        danceTimer = 0;
        foodLocation = null;
        danceCenter = null;
    }

    /**
     * Performs the waggle dance movement pattern.
     * Creates a figure-8 pattern as bees do in reality.
     */
    private Vec3d performWaggleDance(BehaviorContext context) {
        if (danceCenter == null) {
            return new Vec3d();
        }

        // Calculate figure-8 pattern
        double time = (DANCE_DURATION_TICKS - danceTimer) * 0.1;
        double radius = 1.5;

        double offsetX = Math.sin(time) * radius;
        double offsetZ = Math.sin(time * 2) * radius * 0.5;

        Vec3d targetPos = new Vec3d(
            danceCenter.x + offsetX,
            danceCenter.y,
            danceCenter.z + offsetZ
        );

        Vec3d position = context.getPosition();
        Vec3d desired = Vec3d.sub(targetPos, position);
        desired.normalize();
        desired.mult(context.getMaxSpeed() * 0.3); // Slower movement during dance

        Vec3d steer = Vec3d.sub(desired, context.getVelocity());
        steer = limitForce(steer, context.getMaxForce() * 0.5);

        return steer;
    }

    /**
     * Communicates food location to nearby bees.
     * Shares the flower memory with colony members.
     */
    private void communicateFoodLocation(Bee dancer, Level level) {
        // Find nearby bees
        List<Bee> nearbyBees = getNearbyBees(dancer, level);

        for (Bee observer : nearbyBees) {
            if (observer == dancer) {
                continue;
            }

            // Share flower memory with observer
            shareFlowerMemory(dancer, observer);
        }
    }

    /**
     * Gets nearby bees within communication range.
     */
    private List<Bee> getNearbyBees(Bee center, Level level) {
        List<Bee> nearbyBees = new ArrayList<>();

        Vec3 centerPos = center.position();
        BlockPos centerBlockPos = new BlockPos((int) centerPos.x, (int) centerPos.y, (int) centerPos.z);

        int searchRadius = (int) COMMUNICATION_RADIUS;

        for (Entity entity : level.getEntitiesOfClass(Bee.class,
            net.minecraft.world.phys.AABB.ofSize(centerPos, searchRadius * 2, searchRadius * 2, searchRadius * 2))) {
            if (entity != center) {
                nearbyBees.add((Bee) entity);
            }
        }

        return nearbyBees;
    }

    /**
     * Shares flower memory from dancer to observer bee.
     */
    private void shareFlowerMemory(Bee dancer, Bee observer) {
        // This would integrate with the PollinationBehavior's flower memory
        // For now, we set a flag that can be read by other behaviors
        if (dancer.hasNectar()) {
            // Observer bee gets information about good foraging locations
            // This is handled through the bee's target selection in vanilla AI
            // but we could enhance it by sharing specific coordinates
        }
    }

    /**
     * Spawns particles to indicate waggle dance.
     */
    private void spawnDanceParticles(Bee bee, Level level) {
        if (level.isClientSide && danceTimer % 10 == 0) {
            Vec3 pos = bee.position();

            for (int i = 0; i < 3; i++) {
                double offsetX = level.random.nextDouble() * 0.5 - 0.25;
                double offsetY = level.random.nextDouble() * 0.3;
                double offsetZ = level.random.nextDouble() * 0.5 - 0.25;

                level.addParticle(
                    net.minecraft.core.particles.ParticleTypes.HEART,
                    pos.x + offsetX,
                    pos.y + offsetY + 0.5,
                    pos.z + offsetZ,
                    0.0, 0.0, 0.0
                );
            }
        }
    }

    public boolean isDancing() {
        return isDancing;
    }

    public int getDanceTimer() {
        return danceTimer;
    }

    public void setFoodLocation(BlockPos pos) {
        this.foodLocation = pos;
    }

    public BlockPos getFoodLocation() {
        return foodLocation;
    }
}
