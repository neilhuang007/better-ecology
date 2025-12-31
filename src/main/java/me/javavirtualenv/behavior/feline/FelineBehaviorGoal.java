package me.javavirtualenv.behavior.feline;

import me.javavirtualenv.behavior.core.BehaviorContext;
import me.javavirtualenv.behavior.core.Vec3d;
import me.javavirtualenv.ecology.EcologyComponent;
import me.javavirtualenv.ecology.handles.FelineBehaviorHandle;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/**
 * AI goal that integrates all feline behaviors with Minecraft's goal system.
 * <p>
 * This goal manages the complete feline behavior suite:
 * - Hunting behaviors (stalk, pounce, creep)
 * - Stealth behaviors (quiet movement, squeeze gaps)
 * - Social behaviors (purr, hiss, rub)
 * - Gift giving
 * - Special abilities (creeper detection, phantom repel)
 * <p>
 * The goal coordinates these behaviors, prioritizing based on context and
 * entity state to create realistic, emergent feline behavior.
 */
public class FelineBehaviorGoal extends Goal {

    private final Mob mob;
    private final CatAffectionComponent affection;

    // Behavior instances
    private final StalkBehavior stalkBehavior;
    private final PounceBehavior pounceBehavior;
    private final CreepingBehavior creepingBehavior;
    private final QuietMovementBehavior quietMovementBehavior;
    private final SqueezeThroughGapsBehavior squeezeGapsBehavior;
    private final PurrBehavior purrBehavior;
    private final HissBehavior hissBehavior;
    private final RubAffectionBehavior rubAffectionBehavior;
    private final GiftGivingBehavior giftGivingBehavior;
    private final CreeperDetectionBehavior creeperDetectionBehavior;
    private final PhantomRepelBehavior phantomRepelBehavior;

    // State tracking
    private FelineState currentState = FelineState.IDLE;
    private int behaviorCooldown = 0;

    public FelineBehaviorGoal(Mob mob, CatAffectionComponent affection) {
        this.mob = mob;
        this.affection = affection;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));

        // Initialize behaviors with default parameters
        this.stalkBehavior = new StalkBehavior();
        this.pounceBehavior = new PounceBehavior();
        this.creepingBehavior = new CreepingBehavior();
        this.quietMovementBehavior = new QuietMovementBehavior();
        this.squeezeGapsBehavior = new SqueezeThroughGapsBehavior();
        this.purrBehavior = new PurrBehavior();
        this.hissBehavior = new HissBehavior();
        this.rubAffectionBehavior = new RubAffectionBehavior();
        this.giftGivingBehavior = new GiftGivingBehavior();
        this.creeperDetectionBehavior = new CreeperDetectionBehavior();
        this.phantomRepelBehavior = new PhantomRepelBehavior();
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
    public void tick() {
        if (behaviorCooldown > 0) {
            behaviorCooldown--;
            return;
        }

        BehaviorContext context = new BehaviorContext(mob);

        // Priority-based behavior selection
        Vec3d movement = updateBehaviors(context);

        // Apply movement if any
        if (movement != null && movement.magnitude() > 0.001) {
            applyMovement(movement);
        }

        // Update social behaviors
        updateSocialBehaviors(context);

        // Update special abilities
        updateSpecialAbilities(context);
    }

    /**
     * Update all behaviors and return the movement vector.
     */
    private Vec3d updateBehaviors(BehaviorContext context) {
        // Priority 1: Detection of threats (creepers, phantoms)
        if (shouldDetectCreepers(context)) {
            currentState = FelineState.DETECTING_THREAT;
            return creeperDetectionBehavior.calculate(context);
        }

        // Priority 2: Gift giving (tamed cats only)
        if (shouldGiveGift(context)) {
            currentState = FelineState.BRINGING_GIFT;
            return giftGivingBehavior.calculate(context);
        }

        // Priority 3: Hunting (pouncing)
        if (pounceBehavior.canPounce()) {
            Vec3d pounce = pounceBehavior.calculate(context);
            if (pounce.magnitude() > 0) {
                currentState = FelineState.POUNCING;
                return pounce;
            }
        }

        // Priority 4: Stalking prey
        if (stalkBehavior.isStalking()) {
            currentState = FelineState.STALKING;
            return stalkBehavior.calculate(context);
        }

        // Priority 5: Social behaviors (rubbing affection)
        if (rubAffectionBehavior.isRubbing()) {
            currentState = FelineState.SHOWING_AFFECTION;
            return rubAffectionBehavior.calculate(context);
        }

        // Priority 6: Creeping (cautious movement)
        if (creepingBehavior.isCreeping()) {
            currentState = FelineState.CREEPING;
            return creepingBehavior.calculate(context);
        }

        currentState = FelineState.IDLE;
        return new Vec3d();
    }

    /**
     * Update social behaviors that don't affect movement.
     */
    private void updateSocialBehaviors(BehaviorContext context) {
        // Purring
        purrBehavior.calculate(context);

        // Hissing at threats
        hissBehavior.calculate(context);
    }

    /**
     * Update special abilities.
     */
    private void updateSpecialAbilities(BehaviorContext context) {
        // Phantom repelling (sleeping cats)
        if (mob instanceof net.minecraft.world.entity.animal.Cat) {
            phantomRepelBehavior.calculate(context);
        }

        // Apply quiet movement modifier
        if (quietMovementBehavior.isQuiet()) {
            quietMovementBehavior.calculate(context);
        }
    }

    private boolean shouldDetectCreepers(BehaviorContext context) {
        if (!creeperDetectionBehavior.hasDetectedCreeper()) {
            return false;
        }

        // Always detect creepers for tamed cats
        if (mob instanceof net.minecraft.world.entity.animal.Cat cat && cat.isTame()) {
            return true;
        }

        // Ocelots also detect creepers
        return mob instanceof net.minecraft.world.entity.animal.Ocelot;
    }

    private boolean shouldGiveGift(BehaviorContext context) {
        if (!(mob instanceof net.minecraft.world.entity.animal.Cat cat)) {
            return false;
        }

        if (!cat.isTame()) {
            return false;
        }

        if (!giftGivingBehavior.isBringingGift()) {
            return false;
        }

        // Check trust level with owner
        if (affection != null && cat.getOwner() != null) {
            Player owner = mob.getLevel().getPlayerByUUID(cat.getOwnerUUID());
            return affection.willGiveGifts(owner);
        }

        return false;
    }

    private void applyMovement(Vec3d movement) {
        Vec3 mcMovement = movement.toMinecraftVec3();
        Vec3 currentMovement = mob.getDeltaMovement();
        Vec3 newMovement = currentMovement.add(mcMovement);

        // Clamp speed
        double speed = newMovement.length();
        double maxSpeed = 0.5;

        if (speed > maxSpeed) {
            newMovement = newMovement.normalize().multiply(maxSpeed, maxSpeed, maxSpeed);
        }

        mob.setDeltaMovement(newMovement);

        // Face movement direction
        if (movement.magnitude() > 0.01) {
            float yaw = (float) Math.toDegrees(Math.atan2(movement.x, movement.z));
            mob.setYRot(yaw);
            mob.yRotO = yaw;
        }
    }

    // Public getters for behavior state

    public FelineState getCurrentState() {
        return currentState;
    }

    public boolean isStalking() {
        return stalkBehavior.isStalking();
    }

    public boolean isPouncing() {
        return pounceBehavior.isPouncing();
    }

    public boolean isPurring() {
        return purrBehavior.isPurring();
    }

    public boolean isHissing() {
        return hissBehavior.isHissing();
    }

    public boolean isBringingGift() {
        return giftGivingBehavior.isBringingGift();
    }

    public CatAffectionComponent getAffection() {
        return affection;
    }

    /**
     * Feline behavior states for tracking current activity.
     */
    public enum FelineState {
        IDLE,
        STALKING,
        POUNCING,
        CREEPING,
        DETECTING_THREAT,
        SHOWING_AFFECTION,
        BRINGING_GIFT,
        SLEEPING
    }
}
