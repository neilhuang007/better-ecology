package me.javavirtualenv.behavior.fox;

import me.javavirtualenv.behavior.core.BehaviorContext;
import me.javavirtualenv.behavior.core.SteeringBehavior;
import me.javavirtualenv.behavior.core.Vec3d;
import me.javavirtualenv.behavior.predation.PreySelector;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/**
 * Fox pursuit behavior with stalking and pouncing mechanics.
 * <p>
 * Foxes hunt using a unique strategy:
 * 1. STALKING: Slow approach toward prey from downwind
 * 2. CROUCHING: Lower profile and prepare to pounce
 * 3. POUNCING: Rapid leap toward prey (up to 4 blocks)
 * 4. ATTACKING: Close-range attack when within reach
 * <p>
 * Scientific basis: Red foxes use a "pounce" hunting technique where they
 * leap high and pin prey to the ground. This is especially effective against
 * small animals like rabbits and rodents.
 */
public class FoxPursuitBehavior extends SteeringBehavior {

    private final double stalkSpeed;
    private final double pounceSpeed;
    private final double pounceHeight;
    private final double pounceRange;
    private final double maxHuntDistance;
    private final int stalkDuration;
    private final int crouchDuration;
    private final PreySelector preySelector;

    private Entity currentPrey;
    private HuntingState currentState = HuntingState.IDLE;
    private int stateTimer = 0;
    private boolean isPouncing = false;
    private Vec3 pounceTarget;

    public FoxPursuitBehavior(double stalkSpeed, double pounceSpeed,
                              double pounceHeight, double pounceRange,
                              double maxHuntDistance, int stalkDuration, int crouchDuration,
                              PreySelector preySelector) {
        this.stalkSpeed = stalkSpeed;
        this.pounceSpeed = pounceSpeed;
        this.pounceHeight = pounceHeight;
        this.pounceRange = pounceRange;
        this.maxHuntDistance = maxHuntDistance;
        this.stalkDuration = stalkDuration;
        this.crouchDuration = crouchDuration;
        this.preySelector = preySelector != null ? preySelector : new PreySelector();
    }

    public FoxPursuitBehavior(double stalkSpeed, double pounceSpeed,
                              double pounceHeight, double pounceRange,
                              double maxHuntDistance, int stalkDuration, int crouchDuration) {
        this(stalkSpeed, pounceSpeed, pounceHeight, pounceRange, maxHuntDistance,
             stalkDuration, crouchDuration, new PreySelector());
    }

    public FoxPursuitBehavior() {
        this(0.3, 1.2, 0.8, 5.0, 64.0, 100, 40, new PreySelector());
    }

    @Override
    public Vec3d calculate(BehaviorContext context) {
        Mob fox = (Mob) context.getEntity();
        Vec3d foxPos = context.getPosition();

        // Update state machine
        updateHuntingState(fox);

        // Find or validate prey
        Entity prey = findPrey(fox);
        if (prey == null || !prey.isAlive()) {
            resetHunt();
            return new Vec3d();
        }

        currentPrey = prey;
        Vec3d preyPos = new Vec3d(prey.getX(), prey.getY(), prey.getZ());
        double distance = foxPos.distanceTo(preyPos);

        // Check if prey is too far
        if (distance > maxHuntDistance) {
            resetHunt();
            return new Vec3d();
        }

        // State-based behavior
        return switch (currentState) {
            case IDLE -> beginStalking(fox, preyPos);
            case STALKING -> calculateStalk(fox, foxPos, prey, preyPos, distance);
            case CROUCHING -> calculateCrouch(fox, foxPos, preyPos, distance);
            case POUNCING -> calculatePounce(fox, foxPos, preyPos);
            case ATTACKING -> new Vec3d();
            default -> new Vec3d();
        };
    }

    private Vec3d beginStalking(Mob fox, Vec3d preyPos) {
        currentState = HuntingState.STALKING;
        stateTimer = 0;
        return calculateStalk(fox, new Vec3d(fox.getX(), fox.getY(), fox.getZ()), currentPrey, preyPos, 16.0);
    }

    private Vec3d calculateStalk(Mob fox, Vec3d foxPos, Entity prey, Vec3d preyPos, double distance) {
        stateTimer++;

        // Check if should crouch
        if (distance < 10.0 && stateTimer > stalkDuration) {
            currentState = HuntingState.CROUCHING;
            stateTimer = 0;
            playStalkSound(fox);
            return new Vec3d();
        }

        // Slow, careful approach
        Vec3d toPrey = Vec3d.sub(preyPos, foxPos);
        toPrey.normalize();
        toPrey.mult(stalkSpeed);

        // Reduce speed as getting closer
        if (distance < 8.0) {
            toPrey.mult(0.5);
        }

        // Try to stay downwind (avoid prey's line of sight)
        Vec3d stealthOffset = calculateStealthOffset(fox, prey);
        toPrey.add(stealthOffset);

        return toPrey;
    }

    private Vec3d calculateCrouch(Mob fox, Vec3d foxPos, Vec3d preyPos, double distance) {
        stateTimer++;

        // Check if should pounce
        if (distance < pounceRange && stateTimer > crouchDuration) {
            return initiatePounce(fox, foxPos, preyPos);
        }

        // Very slow movement while crouching
        if (distance > 3.0) {
            Vec3d toPrey = Vec3d.sub(preyPos, foxPos);
            toPrey.normalize();
            toPrey.mult(stalkSpeed * 0.2);
            return toPrey;
        }

        return new Vec3d();
    }

    private Vec3d initiatePounce(Mob fox, Vec3d foxPos, Vec3d preyPos) {
        currentState = HuntingState.POUNCING;
        isPouncing = true;

        // Calculate pounce trajectory
        Vec3d toPrey = Vec3d.sub(preyPos, foxPos);
        pounceTarget = preyPos.copy();

        // Launch fox toward prey with leap
        Vec3 pounceVec = new Vec3(toPrey.x, pounceHeight, toPrey.z);
        pounceVec = pounceVec.normalize().scale(pounceSpeed);

        fox.setDeltaMovement(pounceVec);
        fox.hasImpulse = true;

        playPounceSound(fox);
        spawnPounceParticles(fox);

        return new Vec3d(pounceVec.x, pounceVec.y, pounceVec.z);
    }

    private Vec3d calculatePounce(Mob fox, Vec3d foxPos, Vec3d preyPos) {
        // Check if landed
        if (fox.isOnGround()) {
            currentState = HuntingState.ATTACKING;
            isPouncing = false;
            return new Vec3d();
        }

        // Continue pounce trajectory
        Vec3d currentVel = new Vec3d(
            fox.getDeltaMovement().x,
            fox.getDeltaMovement().y,
            fox.getDeltaMovement().z
        );
        return currentVel;
    }

    private Vec3d calculateStealthOffset(Mob fox, Entity prey) {
        // Calculate offset to stay out of prey's field of view
        Vec3 toFox = fox.position().subtract(prey.position());
        Vec3 preyLookDir = prey.getLookAngle();

        // Cross product gives perpendicular direction
        Vec3 cross = preyLookDir.cross(toFox);
        if (cross.length() < 0.01) {
            return new Vec3d();
        }

        cross = cross.normalize();
        double offsetStrength = 0.15;

        return new Vec3d(cross.x * offsetStrength, 0, cross.z * offsetStrength);
    }

    private void updateHuntingState(Mob fox) {
        // Check if pounce completed
        if (isPouncing && fox.isOnGround()) {
            currentState = HuntingState.ATTACKING;
            isPouncing = false;
            return;
        }

        // Reset if attacking and no target
        if (currentState == HuntingState.ATTACKING) {
            if (currentPrey == null || !currentPrey.isAlive()) {
                resetHunt();
            }
        }
    }

    private Entity findPrey(Mob fox) {
        if (currentPrey != null && currentPrey.isAlive()) {
            double distance = fox.position().distanceTo(currentPrey.position());
            if (distance < maxHuntDistance && isFoxPrey(currentPrey)) {
                return currentPrey;
            }
        }

        Entity selectedPrey = preySelector.selectPrey(fox);

        if (selectedPrey != null && isFoxPrey(selectedPrey)) {
            return selectedPrey;
        }

        return null;
    }

    private boolean isFoxPrey(Entity entity) {
        String entityId = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE
            .getKey(entity.getType()).toString();

        return entityId.equals("minecraft:rabbit") ||
               entityId.equals("minecraft:chicken") ||
               entityId.equals("minecraft:parrot") ||
               entityId.equals("minecraft:fish");
    }

    private void resetHunt() {
        currentPrey = null;
        currentState = HuntingState.IDLE;
        stateTimer = 0;
        isPouncing = false;
        pounceTarget = null;
    }

    private void playStalkSound(Mob fox) {
        fox.level().playSound(null, fox.blockPosition(), SoundEvents.FOX_SNIFF,
            SoundSource.NEUTRAL, 0.5f, 1.0f);
    }

    private void playPounceSound(Mob fox) {
        fox.level().playSound(null, fox.blockPosition(), SoundEvents.FOX_BITE,
            SoundSource.NEUTRAL, 0.8f, 1.2f);
    }

    private void spawnPounceParticles(Mob fox) {
        if (fox.level().isClientSide) {
            return;
        }

        Vec3 pos = fox.position();
        for (int i = 0; i < 10; i++) {
            fox.level().addParticle(
                net.minecraft.core.particles.ParticleTypes.CLOUD,
                pos.x, pos.y + 0.5, pos.z,
                (fox.getRandom().nextDouble() - 0.5) * 0.2,
                fox.getRandom().nextDouble() * 0.2,
                (fox.getRandom().nextDouble() - 0.5) * 0.2
            );
        }
    }

    public Entity getCurrentPrey() {
        return currentPrey;
    }

    public HuntingState getCurrentState() {
        return currentState;
    }

    public boolean isPouncing() {
        return isPouncing;
    }

    public void setCurrentState(HuntingState state) {
        this.currentState = state;
    }

    public enum HuntingState {
        IDLE,
        STALKING,
        CROUCHING,
        POUNCING,
        ATTACKING
    }
}
