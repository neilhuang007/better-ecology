package me.javavirtualenv.behavior.feline;

import me.javavirtualenv.behavior.core.BehaviorContext;
import me.javavirtualenv.behavior.core.Vec3d;
import me.javavirtualenv.behavior.steering.SteeringBehavior;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

import java.util.Random;

/**
 * Play behavior for curious felines.
 * <p>
 * Cats and kittens play when:
 * - Young (kittens play more)
 * - Bored/low activity
 * - See interesting items
 * - Interacting with players
 * <p>
 * Play behavior includes:
 * - Batting at items
 * - Chasing moving objects
 * - Pouncing playfully
 * - Following players
 */
public class PlayBehavior extends SteeringBehavior {

    private final double playRange;
    private final int playDuration;
    private final double boredomThreshold;

    private boolean isPlaying = false;
    private int playTicks = 0;
    private Entity playTarget;
    private PlayType currentPlayType;

    private static final Random RANDOM = new Random();

    public PlayBehavior(double playRange, int playDuration, double boredomThreshold) {
        super(0.7);
        this.playRange = playRange;
        this.playDuration = playDuration;
        this.boredomThreshold = boredomThreshold;
    }

    public PlayBehavior() {
        this(8.0, 200, 0.3);
    }

    @Override
    public Vec3d calculate(BehaviorContext context) {
        Mob mob = context.getEntity();

        // Check if should play
        if (!shouldPlay(mob, context)) {
            if (isPlaying) {
                playTicks--;
                if (playTicks <= 0) {
                    stopPlaying();
                }
            }
            return new Vec3d();
        }

        // Find or validate play target
        if (playTarget == null || !playTarget.isAlive()) {
            playTarget = findPlayTarget(mob);
            if (playTarget == null) {
                return new Vec3d();
            }
            currentPlayType = selectPlayType(mob, playTarget);
        }

        isPlaying = true;
        playTicks = playDuration;

        // Execute play behavior
        return executePlay(context, playTarget);
    }

    private boolean shouldPlay(Mob mob, BehaviorContext context) {
        // Kittens always want to play
        if (mob.isBaby()) {
            return RANDOM.nextDouble() < 0.4;
        }

        // Adults play when bored
        double energyPercent = getEnergyPercent(mob);
        return energyPercent > boredomThreshold && RANDOM.nextDouble() < 0.1;
    }

    private Entity findPlayTarget(Mob mob) {
        Level level = mob.level();

        // Look for item entities nearby (to bat at)
        for (Entity entity : level.getEntitiesOfClass(
                ItemEntity.class,
                mob.getBoundingBox().inflate(playRange))) {
            if (entity.isAlive() && RANDOM.nextDouble() < 0.5) {
                return entity;
            }
        }

        // Look for players to follow/play with
        for (Player player : level.players()) {
            double distance = mob.position().distanceTo(player.position());
            if (distance < playRange && RANDOM.nextDouble() < 0.3) {
                return player;
            }
        }

        return null;
    }

    private PlayType selectPlayType(Mob mob, Entity target) {
        if (target instanceof ItemEntity) {
            return PlayType.BAT_AT_ITEM;
        }
        if (target instanceof Player) {
            // Tamed cats follow players, untamed cats stalk them
            if (mob instanceof net.minecraft.world.entity.animal.Cat cat && cat.isTame()) {
                return PlayType.FOLLOW_PLAYER;
            }
            return PlayType.STALK_PLAYER;
        }
        return PlayType.CHASE;
    }

    private Vec3d executePlay(BehaviorContext context, Entity target) {
        Mob mob = context.getEntity();
        Vec3d mobPos = context.getPosition();
        Vec3d targetPos = new Vec3d(target.getX(), target.getY(), target.getZ());

        Vec3d toTarget = Vec3d.sub(targetPos, mobPos);
        double distance = toTarget.magnitude();

        switch (currentPlayType) {
            case BAT_AT_ITEM:
                return batAtItem(mob, target, distance, toTarget);
            case FOLLOW_PLAYER:
                return followPlayer(mob, target, distance, toTarget);
            case STALK_PLAYER:
                return stalkPlayfully(mob, target, distance, toTarget);
            case CHASE:
                return chaseTarget(mob, target, distance, toTarget);
            default:
                return new Vec3d();
        }
    }

    private Vec3d batAtItem(Mob mob, Entity target, double distance, Vec3d toTarget) {
        if (distance < 1.0) {
            // Bat at the item
            Vec3d batDirection = toTarget.copy();
            batDirection.normalize();
            batDirection.mult(0.3);

            // Play meow sound occasionally
            if (RANDOM.nextInt(60) == 0) {
                mob.level().playSound(null, mob.blockPosition(), SoundEvents.CAT_AMBIENT,
                    SoundSource.NEUTRAL, 0.5f, 1.2f);
            }

            return batDirection;
        }

        // Approach the item playfully
        toTarget.normalize();
        toTarget.mult(0.4);
        return toTarget;
    }

    private Vec3d followPlayer(Mob mob, Entity target, double distance, Vec3d toTarget) {
        // Follow at a comfortable distance
        if (distance < 2.0) {
            return new Vec3d(); // Close enough
        }

        toTarget.normalize();
        toTarget.mult(0.3);
        return toTarget;
    }

    private Vec3d stalkPlayfully(Mob mob, Entity target, double distance, Vec3d toTarget) {
        // Creep toward player, stopping when they look
        if (distance < 3.0) {
            return new Vec3d(); // Close enough to pounce
        }

        // Move slowly and cautiously
        toTarget.normalize();
        toTarget.mult(0.15);
        return toTarget;
    }

    private Vec3d chaseTarget(Mob mob, Entity target, double distance, Vec3d toTarget) {
        // Energetic chasing
        toTarget.normalize();
        toTarget.mult(0.5);
        return toTarget;
    }

    private double getEnergyPercent(Mob mob) {
        // This would normally come from EnergyHandle
        // For now, use a simple heuristic
        return mob.getHealth() / mob.getMaxHealth();
    }

    private void stopPlaying() {
        isPlaying = false;
        playTicks = 0;
        playTarget = null;
        currentPlayType = null;
    }

    public boolean isPlaying() {
        return isPlaying;
    }

    public Entity getPlayTarget() {
        return playTarget;
    }

    public PlayType getCurrentPlayType() {
        return currentPlayType;
    }

    public void startPlaying(Entity target) {
        this.playTarget = target;
        this.isPlaying = true;
        this.playTicks = playDuration;
        this.currentPlayType = selectPlayType(null, target);
    }

    public enum PlayType {
        BAT_AT_ITEM,
        FOLLOW_PLAYER,
        STALK_PLAYER,
        CHASE
    }
}
