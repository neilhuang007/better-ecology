package me.javavirtualenv.behavior.aquatic;

import me.javavirtualenv.behavior.core.BehaviorContext;
import me.javavirtualenv.behavior.core.SteeringBehavior;
import me.javavirtualenv.behavior.core.Vec3d;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.Dolphin;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Dolphin wave riding and leaping behavior.
 * Dolphins ride waves and leap out of water, gaining speed bursts.
 * <p>
 * Scientific basis: Dolphins are known for riding bow waves from boats and
 * leaping out of water. They can gain speed by surfing waves and performing
 * acrobatic jumps for play and communication.
 */
public class DolphinWaveRidingBehavior extends SteeringBehavior {
    private final AquaticConfig config;
    private boolean isLeaping = false;
    private int leapTimer = 0;
    private long lastLeapTime = 0;
    private Vec3d leapDirection = new Vec3d();

    public DolphinWaveRidingBehavior(AquaticConfig config) {
        super(1.3, true);
        this.config = config;
    }

    public DolphinWaveRidingBehavior() {
        this(AquaticConfig.createDefault());
    }

    @Override
    public Vec3d calculate(BehaviorContext context) {
        Entity self = context.getEntity();

        if (!(self instanceof Dolphin)) {
            return new Vec3d();
        }

        Level level = context.getLevel();
        long currentTime = level.getGameTime();

        // Handle active leap
        if (isLeaping) {
            return handleLeap(context);
        }

        // Check if should start leap
        if (shouldStartLeap(context, currentTime)) {
            startLeap(context);
            return new Vec3d();
        }

        // Check for boat/ship to ride waves
        Vec3d waveForce = detectAndRideWaves(context);

        if (waveForce.magnitude() > 0) {
            return waveForce;
        }

        return new Vec3d();
    }

    private boolean shouldStartLeap(BehaviorContext context, long currentTime) {
        Entity self = context.getEntity();
        Level level = context.getLevel();

        // Check cooldown
        if (currentTime - lastLeapTime < getLeapCooldown()) {
            return false;
        }

        // Must be in water
        BlockPos pos = self.blockPosition();
        BlockState block = level.getBlockState(pos);
        if (!block.is(Blocks.WATER)) {
            return false;
        }

        // Random chance to leap when moving fast
        Vec3d velocity = context.getVelocity();
        double speed = velocity.magnitude();

        if (speed > 0.3 && Math.random() < 0.02) {
            return true;
        }

        // Always leap when near surface with upward momentum
        if (velocity.y > 0.1 && isNearSurface(context)) {
            return Math.random() < 0.1;
        }

        return false;
    }

    private void startLeap(BehaviorContext context) {
        Entity self = context.getEntity();
        Vec3d velocity = context.getVelocity();

        isLeaping = true;
        leapTimer = 20; // Leap duration in ticks
        lastLeapTime = context.getLevel().getGameTime();

        // Store current direction for leap
        leapDirection = velocity.copy();
        if (leapDirection.magnitude() > 0) {
            leapDirection.normalize();
        } else {
            leapDirection = new Vec3d(1, 0.5, 0);
        }

        // Play leap sound
        if (!self.level().isClientSide) {
            self.playSound(net.minecraft.sounds.SoundEvents.DOLPHIN_JUMP, 1.0F, 1.0F);
        }
    }

    private Vec3d handleLeap(BehaviorContext context) {
        leapTimer--;

        // Upward and forward force during leap
        Vec3d leapForce = leapDirection.copy();

        // Add upward component
        leapForce.y = 0.8;

        // Scale force
        leapForce.mult(config.getMaxSpeed() * 2.0);

        // End leap when timer expires or back in water
        if (leapTimer <= 0 || isDeepUnderwater(context)) {
            isLeaping = false;
            return new Vec3d();
        }

        return leapForce;
    }

    private Vec3d detectAndRideWaves(BehaviorContext context) {
        Entity self = context.getEntity();
        Vec3d position = context.getPosition();
        Level level = context.getLevel();

        // Find nearby boats to ride
        double boatSearchRadius = 16.0;

        for (Entity entity : level.getEntitiesOfClass(
                net.minecraft.world.entity.vehicle.Boat.class,
                self.getBoundingBox().inflate(boatSearchRadius))) {

            Vec3d boatPos = new Vec3d(entity.getX(), entity.getY(), entity.getZ());
            double distance = position.distanceTo(boatPos);

            if (distance < 8.0) {
                // Ride the boat's wave
                return rideBoatWave(context, entity);
            }
        }

        return new Vec3d();
    }

    private Vec3d rideBoatWave(BehaviorContext context, Entity boat) {
        Entity self = context.getEntity();
        Vec3d position = context.getPosition();
        Vec3d boatPos = new Vec3d(boat.getX(), boat.getY(), boat.getZ());
        Vec3d boatVelocity = new Vec3d(
            boat.getDeltaMovement().x,
            boat.getDeltaMovement().y,
            boat.getDeltaMovement().z
        );

        // Position dolphin in front of boat (surfing the bow wave)
        Vec3d toDolphin = Vec3d.sub(position, boatPos);

        // Desired position is in front of boat
        Vec3d boatDir = boatVelocity.copy();
        if (boatDir.magnitude() > 0) {
            boatDir.normalize();
        } else {
            boatDir = new Vec3d(1, 0, 0);
        }

        Vec3d desiredPos = boatPos.copy();
        Vec3d frontOffset = boatDir.copy();
        frontOffset.mult(6.0); // 6 blocks in front
        desiredPos.add(frontOffset);
        desiredPos.y += 1.0; // Slightly above water

        // Calculate steering to desired position
        Vec3d desiredVelocity = Vec3d.sub(desiredPos, position);
        desiredVelocity.normalize();
        desiredVelocity.mult(config.getMaxSpeed() * 1.5); // Speed boost when riding

        Vec3d steering = Vec3d.sub(desiredVelocity, context.getVelocity());

        // Limit force
        if (steering.magnitude() > config.getMaxForce()) {
            steering.normalize();
            steering.mult(config.getMaxForce());
        }

        return steering;
    }

    private boolean isNearSurface(BehaviorContext context) {
        Entity self = context.getEntity();
        BlockPos pos = self.blockPosition();
        Level level = context.getLevel();

        // Check if block above is air
        BlockPos abovePos = pos.above();
        BlockState above = level.getBlockState(abovePos);

        return above.isAir();
    }

    private boolean isDeepUnderwater(BehaviorContext context) {
        Entity self = context.getEntity();
        Vec3d position = context.getPosition();
        Level level = context.getLevel();

        // Check depth
        int seaLevel = level.getSeaLevel();
        return position.y < seaLevel - 2;
    }

    private long getLeapCooldown() {
        return 100; // 5 seconds between leaps
    }

    /**
     * Check if dolphin is currently leaping.
     */
    public boolean isLeaping() {
        return isLeaping;
    }

    /**
     * Get remaining leap timer.
     */
    public int getLeapTimer() {
        return leapTimer;
    }

    /**
     * Force dolphin to leap (for external triggers).
     */
    public void triggerLeap(BehaviorContext context) {
        if (!isLeaping) {
            startLeap(context);
        }
    }
}
