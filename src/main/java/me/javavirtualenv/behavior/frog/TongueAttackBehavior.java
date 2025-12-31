package me.javavirtualenv.behavior.frog;

import me.javavirtualenv.behavior.core.Vec3d;
import me.javavirtualenv.behavior.steering.BehaviorContext;
import me.javavirtualenv.behavior.steering.SeekBehavior;
import me.javavirtualenv.behavior.predation.PreySelector;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.frog.Frog;
import net.minecraft.world.entity.monster.Slime;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.UUID;

/**
 * Tongue attack behavior for frogs.
 * <p>
 * Enables frogs to use their long tongue to catch prey from a distance.
 * Based on real frog predation where frogs rapidly extend their tongue to capture insects and small prey.
 * <p>
 * Key behaviors:
 * - Detect small prey within tongue range
 * - Calculate tongue trajectory
 * - Pull prey to frog on successful hit
 * - Cooldown between attacks
 * - Different ranges for different prey types
 */
public class TongueAttackBehavior extends SeekBehavior {

    private static final double DEFAULT_TONGUE_RANGE = 3.0;
    private static final int DEFAULT_COOLDOWN_TICKS = 60; // 3 seconds
    private static final double TONGUE_SPEED = 0.5;

    private final double tongueRange;
    private final int cooldownTicks;
    private final double tongueSpeed;
    private final PreySelector preySelector;

    private Entity currentTarget;
    private int cooldownTimer = 0;
    private TongueState state = TongueState.IDLE;
    private UUID lastTargetId;

    public TongueAttackBehavior() {
        this(DEFAULT_TONGUE_RANGE, DEFAULT_COOLDOWN_TICKS, TONGUE_SPEED);
    }

    public TongueAttackBehavior(double tongueRange, int cooldownTicks, double tongueSpeed) {
        this(tongueRange, cooldownTicks, tongueSpeed, null);
    }

    public TongueAttackBehavior(double tongueRange, int cooldownTicks, double tongueSpeed,
                                PreySelector preySelector) {
        super(tongueSpeed);
        this.tongueRange = tongueRange;
        this.cooldownTicks = cooldownTicks;
        this.tongueSpeed = tongueSpeed;
        this.preySelector = preySelector != null ? preySelector : new PreySelector(tongueRange * 2, 1.0, 1.5, 2.0);
    }

    @Override
    public Vec3d calculate(BehaviorContext context) {
        Entity entity = context.getEntity();
        if (!(entity instanceof Frog frog)) {
            return new Vec3d();
        }

        // Update cooldown
        if (cooldownTimer > 0) {
            cooldownTimer--;
            if (cooldownTimer == 0) {
                state = TongueState.IDLE;
            }
        }

        // Don't attack if cooling down or tongue is extended
        if (cooldownTimer > 0 && state != TongueState.IDLE) {
            return new Vec3d();
        }

        // Find or validate target
        Entity target = findTarget(frog);
        if (target == null) {
            currentTarget = null;
            state = TongueState.IDLE;
            return new Vec3d();
        }

        currentTarget = target;
        Vec3d frogPos = context.getPosition();
        Vec3d targetPos = new Vec3d(target.getX(), target.getY(), target.getZ());
        double distance = frogPos.distanceTo(targetPos);

        // Check if target is in range
        if (distance > tongueRange) {
            return new Vec3d();
        }

        // Check if we can attack
        if (canAttack(frog, target)) {
            performAttack(frog, target);
            return new Vec3d();
        }

        // Seek target if in range but not attacking yet
        return seek(frogPos, context.getVelocity(), targetPos, getMaxSpeed());
    }

    /**
     * Finds a valid prey target within range.
     * Uses PreySelector for optimal target selection, then filters for frog-specific prey.
     */
    private Entity findTarget(Frog frog) {
        Level level = frog.level();

        // Check current target
        if (currentTarget != null && currentTarget.isAlive()) {
            double distance = frog.distanceTo(currentTarget);
            if (distance <= tongueRange && isValidPrey(frog, currentTarget)) {
                return currentTarget;
            }
        }

        // Use PreySelector to find optimal prey
        Entity selectedTarget = preySelector.selectPrey(frog);

        // Filter to frog-specific prey (small insects and slimes)
        if (selectedTarget != null && isValidPrey(frog, selectedTarget) && isValidPreyBasic(selectedTarget)) {
            return selectedTarget;
        }

        return null;
    }

    /**
     * Basic prey validation (entity type only).
     */
    private boolean isValidPreyBasic(Entity entity) {
        // Small slimes
        if (entity instanceof Slime slime) {
            return slime.getSize() <= 1;
        }

        // Small magma cubes
        if (entity.getType().getDescription().getString().contains("magma_cube")) {
            return true; // Assume small if we can detect size
        }

        // Insects (modded entities with "insect" type)
        if (entity.getType().getDescription().getString().toLowerCase().contains("insect")
                || entity.getType().getDescription().getString().toLowerCase().contains("bee")
                || entity.getType().getDescription().getString().toLowerCase().contains("butterfly")
                || entity.getType().getDescription().getString().toLowerCase().contains("firefly")) {
            return true;
        }

        return false;
    }

    /**
     * Full prey validation including distance and conditions.
     */
    private boolean isValidPrey(Frog frog, Entity entity) {
        if (!entity.isAlive()) {
            return false;
        }

        if (entity.equals(frog)) {
            return false;
        }

        // Don't attack players
        if (entity instanceof net.minecraft.world.entity.player.Player) {
            return false;
        }

        // Check if too large
        if (entity.getBbWidth() > 1.0 || entity.getBbHeight() > 1.0) {
            return false;
        }

        // Check line of sight
        Vec3 eyePos = frog.getEyePosition(1.0F);
        Vec3 targetPos = entity.getEyePosition(1.0F);
        if (!frog.level().clip(new net.minecraft.world.level.ClipContext(eyePos, targetPos,
                net.minecraft.world.level.ClipContext.Block.COLLIDER,
                net.minecraft.world.level.ClipContext.Fluid.NONE,
                entity)).getType().equals(net.minecraft.world.phys.HitResult.Type.MISS)) {
            return false; // Blocked
        }

        return true;
    }

    /**
     * Check if frog can attack the target.
     */
    private boolean canAttack(Frog frog, Entity target) {
        if (cooldownTimer > 0) {
            return false;
        }

        // Check if target is reachable (not in water for frog, etc.)
        if (frog.isInWater()) {
            return false; // Can't use tongue while swimming
        }

        double distance = frog.distanceTo(target);
        return distance <= tongueRange && distance > 1.0;
    }

    /**
     * Perform the tongue attack.
     */
    private void performAttack(Frog frog, Entity target) {
        state = TongueState.EXTENDING;
        cooldownTimer = cooldownTicks;

        // Play tongue sound
        frog.level().playSound(null, frog, net.minecraft.sounds.SoundEvents.FROG_TONGUE,
                net.minecraft.sounds.SoundSource.NEUTRAL, 1.0F, 1.0F);

        // Set tongue target for animation
        if (frog instanceof me.javavirtualenv.ecology.api.EcologyAccess access) {
            var component = access.betterEcology$getEcologyComponent();
            if (component != null) {
                var tag = component.getHandleTag("frog");
                tag.putInt("tongue_target_id", target.getId());
                tag.putInt("tongue_state", state.ordinal());
            }
        }

        // Pull target to frog
        pullTargetToFrog(frog, target);

        // Damage the target
        if (target instanceof LivingEntity living) {
            living.hurt(frog.level().damageSources().mobAttack(frog), 1.0F);
        }

        // Restore some hunger on successful eat
        if (!target.isAlive()) {
            me.javavirtualenv.ecology.handles.HungerHandle.restoreHunger(frog, 5);
            state = TongueState.EATING;
        } else {
            state = TongueState.RETRACTING;
        }
    }

    /**
     * Pull the target towards the frog.
     */
    private void pullTargetToFrog(Frog frog, Entity target) {
        Vec3 frogPos = frog.position();
        Vec3 targetPos = target.position();
        Vec3 direction = frogPos.subtract(targetPos).normalize();
        double pullStrength = 0.75;

        Vec3 newVelocity = direction.scale(pullStrength);
        target.setDeltaMovement(newVelocity);
        target.hurtMarked = true;
    }

    public Entity getCurrentTarget() {
        return currentTarget;
    }

    public TongueState getState() {
        return state;
    }

    public void setState(TongueState state) {
        this.state = state;
    }

    public int getCooldownTimer() {
        return cooldownTimer;
    }

    public void setCooldownTimer(int cooldownTimer) {
        this.cooldownTimer = cooldownTimer;
    }

    public double getTongueRange() {
        return tongueRange;
    }

    /**
     * States of the tongue attack animation.
     */
    public enum TongueState {
        IDLE,
        EXTENDING,
        RETRACTING,
        EATING
    }
}
