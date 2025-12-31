package me.javavirtualenv.behavior.aquatic;

import me.javavirtualenv.behavior.core.BehaviorContext;
import me.javavirtualenv.behavior.core.SteeringBehavior;
import me.javavirtualenv.behavior.core.Vec3d;
import me.javavirtualenv.behavior.predation.PreySelector;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Axolotl;
import net.minecraft.world.phys.AABB;

import java.util.List;
import java.util.UUID;

/**
 * Axolotl hunting behavior for fish and aquatic prey.
 * Axolotls are active predators that hunt tropical fish, cod, salmon, and squid.
 * <p>
 * Scientific basis: Axolotls are carnivorous ambush predators that suck in prey
 * with a rapid gulp. They hunt smaller aquatic animals using a sit-and-wait strategy
 * followed by a quick strike.
 */
public class AxolotlHuntingBehavior extends SteeringBehavior {
    private final AquaticConfig config;
    private final PreySelector preySelector;
    private Entity currentPrey;
    private int attackCooldown = 0;
    private boolean isAttacking = false;
    private static final UUID ATTACK_SPEED_BOOST_UUID = UUID.fromString("7f101912-8c23-11ee-b9d1-0242ac120003");

    public AxolotlHuntingBehavior(AquaticConfig config, PreySelector preySelector) {
        super(1.5, true);
        this.config = config;
        this.preySelector = preySelector != null ? preySelector : new PreySelector();
    }

    public AxolotlHuntingBehavior(AquaticConfig config) {
        this(config, new PreySelector(16.0, 1.0, 1.5, 2.0));
    }

    public AxolotlHuntingBehavior() {
        this(AquaticConfig.createDefault(), new PreySelector(16.0, 1.0, 1.5, 2.0));
    }

    @Override
    public Vec3d calculate(BehaviorContext context) {
        Entity self = context.getEntity();

        if (!(self instanceof Axolotl axolotl)) {
            return new Vec3d();
        }

        // Don't hunt if playing dead
        if (axolotl.isPlayingDead()) {
            currentPrey = null;
            return new Vec3d();
        }

        // Update attack cooldown
        if (attackCooldown > 0) {
            attackCooldown--;
        }

        // Find or validate prey
        Entity prey = findPrey(context);

        if (prey == null || !prey.isAlive()) {
            currentPrey = null;
            isAttacking = false;
            return new Vec3d();
        }

        currentPrey = prey;
        Vec3d position = context.getPosition();
        Vec3d preyPos = new Vec3d(prey.getX(), prey.getY(), prey.getZ());
        double distance = position.distanceTo(preyPos);

        // Attack if close enough and cooldown is ready
        if (distance < 1.5 && attackCooldown == 0) {
            performAttack(axolotl, prey);
            return new Vec3d();
        }

        // Pursue prey
        return calculatePursuit(context, preyPos);
    }

    private Vec3d calculatePursuit(BehaviorContext context, Vec3d preyPos) {
        Vec3d position = context.getPosition();
        Vec3d currentVelocity = context.getVelocity();

        // Predict prey position slightly
        Vec3d targetPos = preyPos.copy();

        // Add small prediction based on prey movement
        if (currentPrey != null) {
            Vec3d preyVel = new Vec3d(
                currentPrey.getDeltaMovement().x,
                currentPrey.getDeltaMovement().y,
                currentPrey.getDeltaMovement().z
            );
            Vec3d prediction = preyVel.copy();
            prediction.mult(5.0); // Predict 5 ticks ahead
            targetPos.add(prediction);
        }

        // Calculate desired velocity towards prey
        Vec3d desired = Vec3d.sub(targetPos, position);
        double distance = desired.magnitude();

        // Slow down when approaching prey
        double speed;
        if (distance < 3.0) {
            speed = config.getMaxSpeed() * (distance / 3.0);
            isAttacking = true;
        } else {
            speed = config.getMaxSpeed() * 1.2; // Axolotl sprint when chasing
            isAttacking = false;
        }

        desired.normalize();
        desired.mult(speed);

        // Calculate steering force
        Vec3d steering = Vec3d.sub(desired, currentVelocity);

        // Limit force
        if (steering.magnitude() > config.getMaxForce()) {
            steering.normalize();
            steering.mult(config.getMaxForce());
        }

        return steering;
    }

    private Entity findPrey(BehaviorContext context) {
        Entity self = context.getEntity();
        Vec3d position = context.getPosition();

        // If already tracking prey, validate it
        if (currentPrey != null && currentPrey.isAlive()) {
            double distance = position.distanceTo(
                new Vec3d(currentPrey.getX(), currentPrey.getY(), currentPrey.getZ())
            );
            if (distance < preySelector.getMaxPreyDistance() * 1.5) {
                return currentPrey;
            }
        }

        // Use PreySelector to find optimal prey
        if (!(self instanceof Axolotl axolotl)) {
            return null;
        }

        Entity selectedPrey = preySelector.selectPrey(axolotl);

        // Filter to axolotl-specific prey
        if (selectedPrey != null && isValidPrey(selectedPrey)) {
            return selectedPrey;
        }

        return null;
    }

    private boolean isValidPrey(Entity entity) {
        if (!entity.isAlive()) {
            return false;
        }

        String entityId = net.minecraft.core.Registry.ENTITY_TYPE.getKey(entity.getType()).toString();

        // Axolotl prey: fish, squid, tadpoles
        return entityId.equals("minecraft:tropical_fish") ||
               entityId.equals("minecraft:cod") ||
               entityId.equals("minecraft:salmon") ||
               entityId.equals("minecraft:pufferfish") ||
               entityId.equals("minecraft:squid") ||
               entityId.equals("minecraft:glow_squid") ||
               entityId.equals("minecraft:tadpole");
    }

    private void performAttack(Axolotl axolotl, Entity prey) {
        // Apply attack damage
        if (axolotl.getAttribute(Attributes.ATTACK_DAMAGE) != null) {
            double damage = axolotl.getAttribute(Attributes.ATTACK_DAMAGE).getValue();
            prey.hurt(axolotl.level().damageSources().mobAttack(axolotl), (float) damage);
        }

        // Set cooldown
        attackCooldown = 20; // 1 second between attacks

        // Play attack sound
        if (!axolotl.level().isClientSide) {
            axolotl.playSound(net.minecraft.sounds.SoundEvents.AXOLOTL_ATTACK, 1.0F, 1.0F);
        }
    }

    /**
     * Check if axolotl is currently hunting (for animation purposes).
     */
    public boolean isHunting() {
        return currentPrey != null && isAttacking;
    }

    /**
     * Get current prey entity.
     */
    public Entity getCurrentPrey() {
        return currentPrey;
    }

    /**
     * Check if axolotl can attack (cooldown is ready).
     */
    public boolean canAttack() {
        return attackCooldown == 0;
    }
}
