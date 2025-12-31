package me.javavirtualenv.behavior.strider;

import me.javavirtualenv.behavior.core.BehaviorContext;
import me.javavirtualenv.behavior.core.Vec3d;
import me.javavirtualenv.behavior.steering.SteeringBehavior;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.Strider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

/**
 * Behavior for strider riding mechanics.
 * <p>
 * This behavior:
 * - Can be ridden with warped fungus on a stick
 * - Implements steering mechanics while ridden
 * - Allows breeding while riding
 * - Provides special movement bonuses when ridden
 * - Handles dismount mechanics in safe locations
 */
public class RidingBehavior extends SteeringBehavior {

    private static final double RIDING_SPEED_MULTIPLIER = 1.5;
    private static final double STEERING_FORCE = 0.3;
    private static final int BREEDING_COOLDOWN_TICKS = 6000; // 5 minutes

    private boolean isBeingRidden;
    private Entity rider;
    private int breedingCooldown;
    private double rideComfort;
    private Vec3d lastRiderInput;

    public RidingBehavior() {
        this(1.0);
    }

    public RidingBehavior(double weight) {
        super(weight);
        this.isBeingRidden = false;
        this.rider = null;
        this.breedingCooldown = 0;
        this.rideComfort = 1.0;
        this.lastRiderInput = new Vec3d();
    }

    @Override
    public Vec3d calculate(BehaviorContext context) {
        Entity self = context.getSelf();
        if (!(self instanceof Strider strider)) {
            return new Vec3d();
        }

        Level level = context.getWorld();

        // Update state
        updateRidingState(strider);

        // Update breeding cooldown
        if (breedingCooldown > 0) {
            breedingCooldown--;
        }

        // If not being ridden, no special behavior
        if (!isBeingRidden || rider == null) {
            return new Vec3d();
        }

        // Calculate riding behavior
        return calculateRidingForce(strider, context);
    }

    /**
     * Updates the riding state.
     */
    private void updateRidingState(Strider strider) {
        isBeingRidden = strider.isVehicle();

        if (isBeingRidden) {
            rider = strider.getControllingPassenger();

            // Update comfort based on riding skill
            if (rider instanceof Player) {
                rideComfort = Math.min(2.0, rideComfort + 0.0005);
            }
        } else {
            rider = null;
            rideComfort = Math.max(1.0, rideComfort - 0.001);
        }
    }

    /**
     * Calculates steering force while being ridden.
     */
    private Vec3d calculateRidingForce(Strider strider, BehaviorContext context) {
        if (rider == null) {
            return new Vec3d();
        }

        Vec3d position = context.getPosition();
        Vec3d velocity = context.getVelocity();

        // Get rider input (simplified - in vanilla this is more complex)
        Vec3d riderInput = getRiderInput(strider);

        if (riderInput.x == 0 && riderInput.z == 0) {
            // No input, slow down
            Vec3d brake = velocity.copy();
            brake.mult(-0.1);
            return brake;
        }

        // Calculate desired velocity based on input
        Vec3d desired = riderInput.copy();
        desired.normalize();

        // Apply speed bonuses based on conditions
        double speedBonus = 1.0;
        if (strider.isInLava()) {
            speedBonus += 0.5; // Faster in lava
        }
        speedBonus *= rideComfort;
        speedBonus *= RIDING_SPEED_MULTIPLIER;

        desired.mult(context.getMaxSpeed() * speedBonus);

        // Calculate steering force
        Vec3d steer = Vec3d.sub(desired, velocity);
        return limitForce(steer, STEERING_FORCE);
    }

    /**
     * Gets the rider's input direction.
     */
    private Vec3d getRiderInput(Strider strider) {
        if (rider == null) {
            return new Vec3d();
        }

        // Get rider's look direction and movement input
        float forward = 0;
        float strafe = 0;

        if (rider instanceof Player player) {
            // Check if player is holding warped fungus on a stick
            boolean hasWarpedFungus = player.getMainHandItem().is(Items.WARPED_FUNGUS_ON_A_STICK) ||
                                    player.getOffhandItem().is(Items.WARPED_FUNGUS_ON_A_STICK);

            if (!hasWarpedFungus) {
                return new Vec3d(); // Can't steer without warped fungus on a stick
            }

            // Get input from player's movement
            forward = player.zza;
            strafe = player.xxa;
        }

        // Convert input to world-space direction
        float yaw = rider.getYRot();
        float cos = (float) Math.cos(yaw * 0.017453292f);
        float sin = (float) Math.sin(yaw * 0.017453292f);

        double dx = (double) (strafe * cos - forward * sin);
        double dz = (double) (forward * cos + strafe * sin);

        lastRiderInput = new Vec3d(dx, 0, dz);
        return lastRiderInput;
    }

    /**
     * Checks if breeding can occur while riding.
     */
    public boolean canBreedWhileRiding() {
        return breedingCooldown == 0 && isBeingRidden && rider instanceof Player;
    }

    /**
     * Attempts to breed while riding.
     */
    public boolean breedWhileRidding(Strider strider, Strider partner) {
        if (!canBreedWhileRidding()) {
            return false;
        }

        // Check if partner is close enough
        double distance = strider.position().distanceTo(partner.position());
        if (distance > 8.0) {
            return false;
        }

        // Both striders must be being ridden
        if (!partner.isVehicle()) {
            return false;
        }

        // Start breeding cooldown
        breedingCooldown = BREEDING_COOLDOWN_TICKS;

        // Trigger breeding (vanilla handles the actual breeding logic)
        strider.spawnAnim();
        partner.spawnAnim();

        return true;
    }

    /**
     * Handles safe dismount.
     */
    public net.minecraft.core.BlockPos safeDismountPos(Strider strider) {
        Level level = strider.level();

        // Find a safe position to dismount (preferably on land or near lava)
        net.minecraft.core.BlockPos striderPos = strider.blockPosition();
        int searchRadius = 8;

        for (int y = -2; y <= 2; y++) {
            for (int x = -searchRadius; x <= searchRadius; x++) {
                for (int z = -searchRadius; z <= searchRadius; z++) {
                    net.minecraft.core.BlockPos testPos = striderPos.offset(x, y, z);

                    // Check if position is safe (solid ground, not water)
                    if (isSafeDismountPos(level, testPos)) {
                        return testPos;
                    }
                }
            }
        }

        // Fallback to current position
        return striderPos.above();
    }

    /**
     * Checks if a position is safe for dismounting.
     */
    private boolean isSafeDismountPos(Level level, net.minecraft.core.BlockPos pos) {
        // Check if position is solid ground
        if (!level.getBlockState(pos).isAir()) {
            return false;
        }

        // Check if there's solid ground below
        if (!level.getBlockState(pos.below()).isSolidRender(level, pos.below())) {
            return false;
        }

        // Check if position is not water
        if (level.getFluidState(pos).is(net.minecraft.world.level.material.Fluids.WATER)) {
            return false;
        }

        return true;
    }

    /**
     * Checks if the strider is being ridden.
     */
    public boolean isBeingRidden() {
        return isBeingRidden;
    }

    /**
     * Gets the current rider.
     */
    public Entity getRider() {
        return rider;
    }

    /**
     * Gets the current ride comfort level.
     */
    public double getRideComfort() {
        return rideComfort;
    }

    /**
     * Gets the breeding cooldown ticks remaining.
     */
    public int getBreedingCooldown() {
        return breedingCooldown;
    }

    /**
     * Gets the last rider input direction.
     */
    public Vec3d getLastRiderInput() {
        return lastRiderInput.copy();
    }
}
