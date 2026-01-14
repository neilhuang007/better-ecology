package me.javavirtualenv.behavior.core;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Utility class for hunting and predator attack animations.
 *
 * <p>Provides visual and audio feedback for predator behaviors including:
 * <ul>
 *   <li>Stalking/crouching animations before pouncing</li>
 *   <li>Lunging/pounce animations for attacks</li>
 *   <li>Tail wagging/raising for wolves when hunting</li>
 *   <li>Sound effects for hunting behaviors (growls, hisses, etc.)</li>
 * </ul>
 */
public final class HuntingAnimations {

    private HuntingAnimations() {
        // Utility class
    }

    // ========== STALKING ANIMATIONS ==========

    /**
     * Plays stalking/crouching animation for predators before pouncing.
     * Used by foxes and cats when sneaking up on prey.
     *
     * @param mob the predator that is stalking
     * @param tickCount current tick in the stalking animation
     */
    public static void playStalkingAnimation(Mob mob, int tickCount) {
        Level level = mob.level();

        // Ensure mob is crouched (sneaking pose)
        mob.setShiftKeyDown(true);

        // Subtle tail swish particles for cats/foxes every 15 ticks
        if (tickCount % 15 == 0 && level instanceof ServerLevel serverLevel) {
            spawnStalkingParticles(mob, serverLevel);
        }

        // Occasional low growl for wolves, hiss for cats
        if (tickCount % 40 == 0 && mob.getRandom().nextFloat() < 0.3f) {
            playStalkingSound(mob);
        }
    }

    /**
     * Spawns subtle particles during stalking to show focused predator.
     *
     * @param mob the stalking predator
     * @param serverLevel the server level
     */
    private static void spawnStalkingParticles(Mob mob, ServerLevel serverLevel) {
        // Small puff particles near feet to show careful movement
        Vec3 pos = mob.position();
        serverLevel.sendParticles(
            ParticleTypes.POOF,
            pos.x,
            pos.y + 0.1,
            pos.z,
            1,
            0.1, 0.05, 0.1,
            0.01
        );
    }

    /**
     * Plays stalking sound based on mob type.
     * Uses generic idle sounds instead of protected ambient sounds.
     *
     * @param mob the stalking predator
     */
    private static void playStalkingSound(Mob mob) {
        // Play a quiet idle/breathing sound to enhance stalking atmosphere
        // Using ENTITY_STEP as a subtle sound (alternative approach since getAmbientSound is protected)
        if (mob.getRandom().nextFloat() < 0.3f) {
            mob.playSound(
                SoundEvents.WOLF_AMBIENT,  // Generic predator sound
                0.15F,
                0.7F + mob.getRandom().nextFloat() * 0.2F
            );
        }
    }

    // ========== LUNGING / POUNCING ANIMATIONS ==========

    /**
     * Plays lunging/pounce animation for predator attacks.
     * Creates dramatic leap with particles and sound.
     *
     * @param mob the predator lunging
     * @param target the target being attacked
     */
    public static void playLungeAnimation(Mob mob, LivingEntity target) {
        Level level = mob.level();

        // Disable sneaking when lunging
        mob.setShiftKeyDown(false);

        // Play aggressive growl/hiss sound
        playLungeSound(mob);

        // Spawn attack particles
        if (level instanceof ServerLevel serverLevel) {
            spawnLungeParticles(mob, target, serverLevel);
        }

        // Apply lunge velocity toward target
        applyLungeVelocity(mob, target);
    }

    /**
     * Applies velocity to mob for dramatic lunge toward target.
     *
     * @param mob the lunging predator
     * @param target the attack target
     */
    private static void applyLungeVelocity(Mob mob, LivingEntity target) {
        Vec3 direction = new Vec3(
            target.getX() - mob.getX(),
            0,
            target.getZ() - mob.getZ()
        ).normalize();

        // Add horizontal velocity toward target and upward arc
        Vec3 lungeVelocity = direction.scale(0.8).add(0, 0.3, 0);
        mob.setDeltaMovement(mob.getDeltaMovement().add(lungeVelocity));
    }

    /**
     * Spawns attack particles during lunge.
     *
     * @param mob the attacking predator
     * @param target the target
     * @param serverLevel the server level
     */
    private static void spawnLungeParticles(Mob mob, LivingEntity target, ServerLevel serverLevel) {
        Vec3 pos = mob.position();

        // Angry particle burst
        serverLevel.sendParticles(
            ParticleTypes.ANGRY_VILLAGER,
            pos.x,
            pos.y + mob.getEyeHeight(),
            pos.z,
            3,
            0.3, 0.2, 0.3,
            0.02
        );

        // Cloud trail during leap
        serverLevel.sendParticles(
            ParticleTypes.CLOUD,
            pos.x,
            pos.y + 0.2,
            pos.z,
            5,
            0.2, 0.1, 0.2,
            0.03
        );
    }

    /**
     * Plays lunge/attack sound for predators.
     *
     * @param mob the lunging predator
     */
    private static void playLungeSound(Mob mob) {
        Level level = mob.level();

        // Play appropriate attack sound based on mob type
        String mobType = mob.getType().toString().toLowerCase();

        if (mobType.contains("wolf")) {
            level.playSound(
                null,
                mob.getX(), mob.getY(), mob.getZ(),
                SoundEvents.WOLF_GROWL,
                SoundSource.HOSTILE,
                0.8F,
                0.8F + mob.getRandom().nextFloat() * 0.3F
            );
        } else if (mobType.contains("cat") || mobType.contains("ocelot")) {
            level.playSound(
                null,
                mob.getX(), mob.getY(), mob.getZ(),
                SoundEvents.CAT_HISS,
                SoundSource.HOSTILE,
                0.7F,
                0.9F + mob.getRandom().nextFloat() * 0.2F
            );
        } else if (mobType.contains("fox")) {
            level.playSound(
                null,
                mob.getX(), mob.getY(), mob.getZ(),
                SoundEvents.FOX_AGGRO,
                SoundSource.HOSTILE,
                0.7F,
                1.0F + mob.getRandom().nextFloat() * 0.2F
            );
        } else {
            // Generic aggressive sound
            level.playSound(
                null,
                mob.getX(), mob.getY(), mob.getZ(),
                SoundEvents.GENERIC_HURT,
                SoundSource.HOSTILE,
                0.5F,
                0.7F
            );
        }
    }

    // ========== HUNTING POSTURE ANIMATIONS ==========

    /**
     * Plays hunting posture animation for wolves in hunt mode.
     * Shows lowered head, raised tail, focused eyes.
     *
     * @param mob the wolf in hunting mode
     * @param target the prey being hunted
     * @param tickCount current tick count
     */
    public static void playHuntingPostureAnimation(Mob mob, LivingEntity target, int tickCount) {
        // Look at target with focused gaze
        mob.getLookControl().setLookAt(target, 30.0F, 30.0F);

        // Occasional growl when closing in
        if (tickCount % 30 == 0 && mob.distanceTo(target) < 10.0) {
            playHuntingGrowl(mob);
        }

        // Particle effect showing heightened focus every 20 ticks
        if (tickCount % 20 == 0 && mob.level() instanceof ServerLevel serverLevel) {
            spawnHuntingFocusParticles(mob, serverLevel);
        }
    }

    /**
     * Plays hunting growl sound.
     *
     * @param mob the hunting predator
     */
    private static void playHuntingGrowl(Mob mob) {
        String mobType = mob.getType().toString().toLowerCase();

        if (mobType.contains("wolf")) {
            mob.level().playSound(
                null,
                mob.getX(), mob.getY(), mob.getZ(),
                SoundEvents.WOLF_GROWL,
                SoundSource.NEUTRAL,
                0.4F,
                0.9F + mob.getRandom().nextFloat() * 0.2F
            );
        }
    }

    /**
     * Spawns focus particles around predator's head during hunting.
     *
     * @param mob the hunting predator
     * @param serverLevel the server level
     */
    private static void spawnHuntingFocusParticles(Mob mob, ServerLevel serverLevel) {
        double eyeX = mob.getX();
        double eyeY = mob.getEyeY();
        double eyeZ = mob.getZ();

        // Small sparkle particles near eyes showing focus
        serverLevel.sendParticles(
            ParticleTypes.INSTANT_EFFECT,
            eyeX,
            eyeY,
            eyeZ,
            2,
            0.1, 0.1, 0.1,
            0.01
        );
    }

    // ========== ATTACK ANIMATIONS ==========

    /**
     * Plays attack swing animation with sound effects.
     * Used when predator successfully strikes prey.
     *
     * @param attacker the attacking predator
     * @param victim the attacked entity
     */
    public static void playAttackSwingAnimation(Mob attacker, LivingEntity victim) {
        Level level = attacker.level();

        // Swing hand/paw animation
        attacker.swing(attacker.getRandom().nextBoolean() ?
            InteractionHand.MAIN_HAND :
            InteractionHand.OFF_HAND);

        // Play attack sound
        playAttackSound(attacker);

        // Spawn hit particles at impact point
        if (level instanceof ServerLevel serverLevel) {
            spawnAttackHitParticles(victim, serverLevel);
        }
    }

    /**
     * Plays attack sound for predators.
     *
     * @param attacker the attacking mob
     */
    private static void playAttackSound(Mob attacker) {
        attacker.level().playSound(
            null,
            attacker.getX(),
            attacker.getY(),
            attacker.getZ(),
            SoundEvents.PLAYER_ATTACK_CRIT,
            SoundSource.HOSTILE,
            0.6F,
            1.0F + attacker.getRandom().nextFloat() * 0.2F
        );
    }

    /**
     * Spawns hit particles when attack lands.
     *
     * @param victim the entity being hit
     * @param serverLevel the server level
     */
    private static void spawnAttackHitParticles(LivingEntity victim, ServerLevel serverLevel) {
        Vec3 pos = victim.position();

        // Damage particles
        serverLevel.sendParticles(
            ParticleTypes.DAMAGE_INDICATOR,
            pos.x,
            pos.y + victim.getEyeHeight() / 2,
            pos.z,
            4,
            0.2, 0.3, 0.2,
            0.1
        );

        // Impact particles
        serverLevel.sendParticles(
            ParticleTypes.CRIT,
            pos.x,
            pos.y + 0.5,
            pos.z,
            3,
            0.15, 0.15, 0.15,
            0.05
        );
    }

    // ========== TAIL ANIMATIONS ==========

    /**
     * Plays tail wagging animation for wolves when hunting successfully.
     * This is a visual cue that the wolf is excited about the hunt.
     *
     * @param mob the wolf
     * @param tickCount current tick count
     */
    public static void playTailWagAnimation(Mob mob, int tickCount) {
        // Tail wagging is handled by the model, but we can add particle effects
        if (tickCount % 10 == 0 && mob.level() instanceof ServerLevel serverLevel) {
            // Small happy particles near tail position
            Vec3 tailPos = calculateTailPosition(mob);
            serverLevel.sendParticles(
                ParticleTypes.HEART,
                tailPos.x,
                tailPos.y,
                tailPos.z,
                1,
                0.1, 0.1, 0.1,
                0.02
            );
        }
    }

    /**
     * Calculates approximate tail position for a mob.
     *
     * @param mob the mob
     * @return position at rear of mob (approximate tail location)
     */
    private static Vec3 calculateTailPosition(Mob mob) {
        Vec3 lookVec = mob.getLookAngle();
        // Position behind the mob
        return mob.position().add(lookVec.scale(-0.5)).add(0, 0.5, 0);
    }
}
