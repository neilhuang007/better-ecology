package me.javavirtualenv.behavior.rabbit;

import me.javavirtualenv.behavior.core.BehaviorContext;
import me.javavirtualenv.behavior.core.Vec3d;
import me.javavirtualenv.ecology.EcologyComponent;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.pathfinder.Path;

/**
 * Integrated goal for all rabbit behaviors.
 * <p>
 * This goal manages:
 * - Evasion from predators with zigzag patterns
 * - Thumping warnings to other rabbits
 * - Burrow seeking and digging
 * - Foraging for food
 * - Freezing behavior to avoid detection
 * <p>
 * The behaviors are prioritized as:
 * 1. Escape immediate danger (highest priority)
 * 2. Thump warnings
 * 3. Seek burrow when threatened
 * 4. Forage when safe
 */
public class RabbitBehaviorGoal extends Goal {

    private final Mob mob;
    private final RabbitEvasionBehavior evasion;
    private final ThumpBehavior thump;
    private final RabbitForagingBehavior foraging;
    private final BurrowSystem burrowSystem;

    private final EcologyComponent component;

    // State
    private boolean isMovingToBurrow = false;
    private BlockPos targetBurrowPos;

    // Cooldowns
    private int burrowSearchCooldown = 0;

    public RabbitBehaviorGoal(Mob mob, EcologyComponent component,
                             RabbitEvasionConfig evasionConfig,
                             RabbitThumpConfig thumpConfig,
                             RabbitForagingConfig foragingConfig) {
        this.mob = mob;
        this.component = component;
        this.evasion = new RabbitEvasionBehavior(evasionConfig);
        this.thump = new ThumpBehavior(thumpConfig);
        this.foraging = new RabbitForagingBehavior(foragingConfig);
        this.burrowSystem = BurrowSystem.get(mob.level());
    }

    public RabbitBehaviorGoal(Mob mob, EcologyComponent component) {
        this(mob, component,
             RabbitEvasionConfig.createDefault(),
             RabbitThumpConfig.createDefault(),
             RabbitForagingConfig.createDefault());
    }

    @Override
    public boolean canUse() {
        return mob.isAlive();
    }

    @Override
    public boolean canContinueToUse() {
        return canUse();
    }

    @Override
    public void start() {
        isMovingToBurrow = false;
        targetBurrowPos = null;
    }

    @Override
    public void stop() {
        evasion.reset();
        isMovingToBurrow = false;
        targetBurrowPos = null;
    }

    @Override
    public void tick() {
        // Update burrow system reference (in case level changed)
        BurrowSystem currentBurrowSystem = BurrowSystem.get(mob.level());

        // Create behavior context
        BehaviorContext context = new BehaviorContext(mob);

        // Priority 1: Check for threats and evade
        Vec3d evasionForce = evasion.calculate(context);

        if (evasion.isEvading()) {
            // Currently evading - apply evasion movement
            applyEvasionMovement(evasionForce);

            // Thump to warn others if appropriate
            if (evasion.getCurrentThreat() instanceof Player) {
                thump.thumpIfNeeded(context, (Player) evasion.getCurrentThreat());
            }

            // Try to seek burrow if escaping
            if (burrowSearchCooldown <= 0) {
                trySeekBurrow(context, currentBurrowSystem);
                burrowSearchCooldown = 40; // Check every 2 seconds
            }
        } else {
            // Not immediately threatened
            burrowSearchCooldown--;

            // Priority 2: Thump if distant threat detected
            checkAndThump(context);

            // Priority 3: Forage when safe
            if (!evasion.isFrozen() && !isMovingToBurrow) {
                foraging.tick(context);

                // Occasionally stand on hind legs
                foraging.tryStand(context);
            }

            // Priority 4: Return to burrow to rest
            if (shouldReturnToBurrow(context)) {
                trySeekBurrow(context, currentBurrowSystem);
            }
        }
    }

    /**
     * Applies evasion movement forces to the mob.
     */
    private void applyEvasionMovement(Vec3d evasionForce) {
        if (evasionForce.magnitude() < 0.01) {
            return;
        }

        // Apply steering force to movement
        // Convert Vec3d to Minecraft Vec3
        net.minecraft.world.phys.Vec3 currentMovement = mob.getDeltaMovement();

        net.minecraft.world.phys.Vec3 newMovement = new net.minecraft.world.phys.Vec3(
            currentMovement.x + evasionForce.x * 0.1,
            currentMovement.y + evasionForce.y * 0.1,
            currentMovement.z + evasionForce.z * 0.1
        );

        // Limit maximum speed
        double maxSpeed = evasion.getConfig().getEvasionSpeed();
        double currentSpeed = newMovement.length();

        if (currentSpeed > maxSpeed) {
            newMovement = newMovement.normalize().scale(maxSpeed);
        }

        mob.setDeltaMovement(newMovement);

        // Face movement direction
        if (newMovement.length() > 0.01) {
            mob.setYRot((float) Math.toDegrees(Math.atan2(newMovement.x, newMovement.z)));
        }
    }

    /**
     * Checks for threats and thumps if appropriate.
     */
    private void checkAndThump(BehaviorContext context) {
        // Find nearby threats
        Player nearestPlayer = context.getLevel().getNearestPlayer(
            mob,
            thump.getConfig().getThumpDetectionRange()
        );

        if (nearestPlayer != null && !nearestPlayer.isShiftKeyDown()) {
            thump.thumpIfNeeded(context, nearestPlayer);
        }
    }

    /**
     * Attempts to seek and move to a burrow.
     */
    private void trySeekBurrow(BehaviorContext context, BurrowSystem burrowSystem) {
        BlockPos currentPos = mob.blockPosition();
        RabbitBurrow nearestBurrow = burrowSystem.findAvailableBurrow(currentPos, 32.0);

        if (nearestBurrow == null) {
            // No burrow nearby, try to dig one
            if (mob.getRandom().nextDouble() < 0.05) { // 5% chance per check
                burrowSystem.tryDigBurrow(context);
            }
            return;
        }

        BlockPos burrowPos = nearestBurrow.getPosition();
        double distance = nearestBurrow.distanceTo(currentPos);

        if (distance < 3.0) {
            // Arrived at burrow
            isMovingToBurrow = false;
            targetBurrowPos = null;
            burrowSystem.enterBurrow(mob, nearestBurrow);
            return;
        }

        // Move toward burrow
        isMovingToBurrow = true;
        targetBurrowPos = burrowPos;

        PathNavigation navigation = mob.getNavigation();
        Path path = navigation.createPath(burrowPos, 0);

        if (path != null) {
            navigation.moveTo(path, 1.0);
        }
    }

    /**
     * Determines if rabbit should return to burrow.
     */
    private boolean shouldReturnToBurrow(BehaviorContext context) {
        // Return to burrow at night
        long dayTime = context.getLevel().getDayTime() % 24000;
        boolean isNight = dayTime > 13000 && dayTime < 23000;

        // Return when injured
        boolean isInjured = mob.getHealth() < mob.getMaxHealth() * 0.5;

        // Return during rain
        boolean isRaining = context.getLevel().isRaining();

        return isNight || isInjured || isRaining;
    }

    // Getters for external access

    public RabbitEvasionBehavior getEvasion() {
        return evasion;
    }

    public ThumpBehavior getThump() {
        return thump;
    }

    public RabbitForagingBehavior getForaging() {
        return foraging;
    }

    public boolean isMovingToBurrow() {
        return isMovingToBurrow;
    }

    public BlockPos getTargetBurrowPos() {
        return targetBurrowPos;
    }
}
