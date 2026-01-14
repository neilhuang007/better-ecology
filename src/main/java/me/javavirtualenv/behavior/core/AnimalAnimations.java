package me.javavirtualenv.behavior.core;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Utility class for playing eating, drinking, and other animal animations.
 *
 * <p>This provides vanilla-style visual and audio feedback for animal behaviors,
 * making the mod feel more integrated with Minecraft's existing systems.
 *
 * <p>Animation patterns are based on:
 * <ul>
 *   <li>Player eating animations (particles + sound)</li>
 *   <li>Wolf shake animation (entity event)</li>
 *   <li>Sheep eating grass (head bob + block particles)</li>
 * </ul>
 */
public final class AnimalAnimations {

    private AnimalAnimations() {
        // Utility class
    }

    // ========== ENTITY EVENTS ==========
    // These trigger client-side animations via LivingEntity.handleEntityEvent

    /** Sheep eating grass - head lowering animation */
    public static final byte EVENT_EAT_GRASS = 10;

    /** Generic eating particles (used by some mobs) */
    public static final byte EVENT_EATING = 45;

    /** Wolf shake animation */
    public static final byte EVENT_WOLF_SHAKE = 8;

    // ========== EATING ANIMATIONS ==========

    /**
     * Plays eating animation for a mob consuming an item.
     * Spawns item particles around the mouth and plays eating sound.
     *
     * @param mob the mob that is eating
     * @param foodItem the item being consumed
     * @param tickCount current tick in the eating animation (for pacing)
     */
    public static void playEatingAnimation(Mob mob, ItemStack foodItem, int tickCount) {
        Level level = mob.level();

        // Play particles every 4 ticks for smooth animation
        if (tickCount % 4 == 0 && !foodItem.isEmpty()) {
            spawnEatingParticles(mob, foodItem);
        }

        // Play eating sound every 8 ticks
        if (tickCount % 8 == 0) {
            playEatingSound(mob);
        }

        // Make mob look down slightly while eating (head bob effect)
        if (tickCount % 2 == 0) {
            // Small random head movement for realism
            float yaw = mob.getYHeadRot();
            mob.getLookControl().setLookAt(
                mob.getX() + Math.sin(Math.toRadians(yaw)) * 0.5,
                mob.getY() + 0.2 + (tickCount % 8 < 4 ? -0.1 : 0.1),
                mob.getZ() + Math.cos(Math.toRadians(yaw)) * 0.5
            );
        }
    }

    /**
     * Spawns item particles around the mob's mouth position.
     *
     * @param mob the mob eating
     * @param item the item being eaten
     */
    public static void spawnEatingParticles(Mob mob, ItemStack item) {
        Level level = mob.level();
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }

        // Calculate mouth position (in front of and slightly below head)
        Vec3 lookVec = mob.getLookAngle();
        double mouthX = mob.getX() + lookVec.x * 0.5;
        double mouthY = mob.getY() + mob.getEyeHeight() - 0.2;
        double mouthZ = mob.getZ() + lookVec.z * 0.5;

        // Spawn 3-5 item particles
        ItemParticleOption particleOption = new ItemParticleOption(ParticleTypes.ITEM, item);
        for (int i = 0; i < 3 + mob.getRandom().nextInt(3); i++) {
            double offsetX = (mob.getRandom().nextDouble() - 0.5) * 0.3;
            double offsetY = (mob.getRandom().nextDouble() - 0.5) * 0.2;
            double offsetZ = (mob.getRandom().nextDouble() - 0.5) * 0.3;

            serverLevel.sendParticles(
                particleOption,
                mouthX + offsetX,
                mouthY + offsetY,
                mouthZ + offsetZ,
                1,  // count
                0, -0.1, 0,  // velocity (slight downward)
                0.05  // speed
            );
        }
    }

    /**
     * Plays the eating sound for a mob.
     *
     * @param mob the mob eating
     */
    public static void playEatingSound(Mob mob) {
        mob.level().playSound(
            null,
            mob.getX(),
            mob.getY(),
            mob.getZ(),
            SoundEvents.GENERIC_EAT,
            SoundSource.NEUTRAL,
            0.5F + mob.getRandom().nextFloat() * 0.2F,
            0.9F + mob.getRandom().nextFloat() * 0.2F
        );
    }

    /**
     * Plays the final eating sound when food is consumed.
     *
     * @param mob the mob that finished eating
     */
    public static void playEatingFinishSound(Mob mob) {
        mob.level().playSound(
            null,
            mob.getX(),
            mob.getY(),
            mob.getZ(),
            SoundEvents.PLAYER_BURP,
            SoundSource.NEUTRAL,
            0.3F,
            0.8F + mob.getRandom().nextFloat() * 0.4F
        );
    }

    // ========== DRINKING ANIMATIONS ==========

    /**
     * Plays drinking animation for a mob at a water source.
     * Creates water splash particles and plays drinking sound.
     * Implements realistic drinking behavior with periodic head raising for vigilance.
     *
     * @param mob the mob that is drinking
     * @param waterPos the position of the water block
     * @param tickCount current tick in the drinking animation
     */
    public static void playDrinkingAnimation(Mob mob, BlockPos waterPos, int tickCount) {
        Level level = mob.level();

        // Vigilance cycle: drink for ~1 second, raise head for ~0.5 seconds
        // This creates a natural drinking rhythm where animals periodically check for danger
        int vigilanceCycle = tickCount % 30; // 1.5 second cycle
        boolean isRaisingHead = vigilanceCycle >= 20; // Last 0.5 seconds of cycle

        if (isRaisingHead) {
            // Raise head to look around (vigilance behavior)
            applyVigilanceLook(mob);
        } else {
            // Lower head to water level for drinking
            applyDrinkingHeadPosition(mob, waterPos);

            // Play splash particles every 5 ticks while head is down
            if (tickCount % 5 == 0) {
                spawnDrinkingParticles(mob, waterPos);
            }

            // Play drinking sound every 10 ticks while drinking
            if (tickCount % 10 == 0) {
                playDrinkingSound(mob);
            }

            // Head bobbing animation - subtle dip toward water
            if (tickCount % 4 == 0) {
                level.broadcastEntityEvent(mob, EVENT_EAT_GRASS);
            }
        }
    }

    /**
     * Positions the mob's head to look at the water source.
     * Creates a lowered head position for realistic drinking.
     *
     * @param mob the mob drinking
     * @param waterPos position of the water being drunk from
     */
    private static void applyDrinkingHeadPosition(Mob mob, BlockPos waterPos) {
        // Calculate water surface position (slightly above the block)
        double waterX = waterPos.getX() + 0.5;
        double waterY = waterPos.getY() + 0.8; // Water surface height
        double waterZ = waterPos.getZ() + 0.5;

        // Make mob look at water surface with head lowered
        mob.getLookControl().setLookAt(waterX, waterY, waterZ);
    }

    /**
     * Makes the mob raise its head and look around for danger.
     * Animals exhibit vigilance behavior while drinking.
     *
     * @param mob the mob exhibiting vigilance
     */
    private static void applyVigilanceLook(Mob mob) {
        // Look up and scan the environment (alternating directions)
        double scanAngle = (mob.tickCount % 60) * 6.0; // Sweep 360 degrees over 3 seconds
        double scanRadius = 8.0;

        double lookX = mob.getX() + Math.cos(Math.toRadians(scanAngle)) * scanRadius;
        double lookY = mob.getY() + mob.getEyeHeight() + 0.5; // Look slightly upward
        double lookZ = mob.getZ() + Math.sin(Math.toRadians(scanAngle)) * scanRadius;

        mob.getLookControl().setLookAt(lookX, lookY, lookZ);
    }

    /**
     * Spawns water splash particles at the water surface.
     *
     * @param mob the mob drinking
     * @param waterPos position of the water block
     */
    public static void spawnDrinkingParticles(Mob mob, BlockPos waterPos) {
        Level level = mob.level();
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }

        // Calculate positions
        Vec3 lookVec = mob.getLookAngle();
        double waterX = waterPos.getX() + 0.5;
        double waterY = waterPos.getY() + 1.0;  // Top of water block
        double waterZ = waterPos.getZ() + 0.5;

        // Calculate mouth position
        double mouthX = mob.getX() + lookVec.x * 0.5;
        double mouthY = mob.getY() + mob.getEyeHeight() * 0.6; // Lower than eyes
        double mouthZ = mob.getZ() + lookVec.z * 0.5;

        // Create ripple effect with splash particles at water surface
        for (int i = 0; i < 5; i++) {
            double offsetX = (mob.getRandom().nextDouble() - 0.5) * 0.5;
            double offsetZ = (mob.getRandom().nextDouble() - 0.5) * 0.5;

            serverLevel.sendParticles(
                ParticleTypes.SPLASH,
                waterX + offsetX,
                waterY,
                waterZ + offsetZ,
                1,
                0, 0.15, 0,
                0.1
            );
        }

        // Water drips from mouth while drinking (more frequent)
        if (mob.getRandom().nextFloat() < 0.5f) {
            serverLevel.sendParticles(
                ParticleTypes.DRIPPING_WATER,
                mouthX,
                mouthY,
                mouthZ,
                2,
                0.05, 0, 0.05,
                0
            );
        }

        // Occasional falling water particles (simulate water being scooped)
        if (mob.getRandom().nextFloat() < 0.2f) {
            serverLevel.sendParticles(
                ParticleTypes.FALLING_WATER,
                mouthX,
                mouthY + 0.1,
                mouthZ,
                1,
                0, -0.1, 0,
                0.05
            );
        }

        // Bubble particles in water (creates drinking effect)
        if (mob.getRandom().nextFloat() < 0.3f) {
            serverLevel.sendParticles(
                ParticleTypes.BUBBLE,
                waterX + (mob.getRandom().nextDouble() - 0.5) * 0.3,
                waterY - 0.2,
                waterZ + (mob.getRandom().nextDouble() - 0.5) * 0.3,
                1,
                0, 0.1, 0,
                0.02
            );
        }
    }

    /**
     * Plays the drinking sound for a mob.
     * Uses varied pitch and volume for natural drinking sounds.
     *
     * @param mob the mob drinking
     */
    public static void playDrinkingSound(Mob mob) {
        // Main drinking sound
        mob.level().playSound(
            null,
            mob.getX(),
            mob.getY(),
            mob.getZ(),
            SoundEvents.GENERIC_DRINK,
            SoundSource.NEUTRAL,
            0.5F + mob.getRandom().nextFloat() * 0.3F,  // Volume: 0.5-0.8
            0.8F + mob.getRandom().nextFloat() * 0.4F   // Pitch: 0.8-1.2
        );

        // Occasional water splash sound for realism
        if (mob.getRandom().nextFloat() < 0.3f) {
            mob.level().playSound(
                null,
                mob.getX(),
                mob.getY(),
                mob.getZ(),
                SoundEvents.GENERIC_SPLASH,
                SoundSource.NEUTRAL,
                0.3F + mob.getRandom().nextFloat() * 0.2F,
                1.0F + mob.getRandom().nextFloat() * 0.3F
            );
        }
    }

    // ========== GRAZING ANIMATIONS ==========

    /**
     * Plays grazing animation for herbivores eating grass.
     * Includes vigilance behavior - animals periodically raise their head to check for predators.
     *
     * @param mob the mob that is grazing
     * @param grassPos the position of the grass being eaten
     * @param tickCount current tick in the grazing animation
     */
    public static void playGrazingAnimation(Mob mob, BlockPos grassPos, int tickCount) {
        Level level = mob.level();

        // Vigilance behavior: raise head every 20-30 ticks to check for predators
        // Grazing animals in nature periodically scan their environment for threats
        int vigilanceCycle = tickCount % 30;  // 1.5 second cycle
        boolean isVigilant = vigilanceCycle >= 22;  // Look up for last 8 ticks (0.4 seconds)

        if (isVigilant) {
            // Look around while being vigilant (scan for predators)
            applyVigilanceLook(mob);
        } else {
            // Make mob look at grass while eating
            mob.getLookControl().setLookAt(
                grassPos.getX() + 0.5,
                grassPos.getY() + 0.3,
                grassPos.getZ() + 0.5
            );

            // Trigger head-down animation (same as sheep eating)
            if (tickCount % 4 == 0) {
                level.broadcastEntityEvent(mob, EVENT_EAT_GRASS);
            }

            // Chewing animation - subtle head bob while eating
            if (tickCount % 3 == 0) {
                applyChewingAnimation(mob, grassPos);
            }
        }

        // Play grass rustling/eating sound (only when head is down)
        if (!isVigilant && tickCount % 6 == 0) {
            playGrazingSound(mob);
        }

        // Spawn grass particles (only when actively chewing)
        if (!isVigilant && tickCount % 8 == 0) {
            spawnGrazingParticles(mob, grassPos);
        }
    }

    /**
     * Spawns grass/plant particles when grazing.
     *
     * @param mob the mob grazing
     * @param grassPos position of the grass
     */
    public static void spawnGrazingParticles(Mob mob, BlockPos grassPos) {
        Level level = mob.level();
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }

        // Use block destruction particles for grass
        serverLevel.sendParticles(
            ParticleTypes.COMPOSTER,  // Green plant-like particles
            grassPos.getX() + 0.5,
            grassPos.getY() + 0.5,
            grassPos.getZ() + 0.5,
            5,
            0.2, 0.1, 0.2,
            0.02
        );
    }

    /**
     * Plays the grazing/grass eating sound.
     *
     * @param mob the mob grazing
     */
    public static void playGrazingSound(Mob mob) {
        mob.level().playSound(
            null,
            mob.getX(),
            mob.getY(),
            mob.getZ(),
            SoundEvents.GRASS_BREAK,
            SoundSource.NEUTRAL,
            0.3F,
            1.0F + mob.getRandom().nextFloat() * 0.2F
        );
    }

    /**
     * Applies subtle chewing animation while grazing.
     * Creates a realistic head bobbing motion as the animal chews grass.
     *
     * @param mob the mob chewing
     * @param grassPos position of the grass being eaten
     */
    private static void applyChewingAnimation(Mob mob, BlockPos grassPos) {
        // Create a subtle bobbing motion by slightly varying the look target height
        // This simulates the up-and-down jaw movement of chewing
        double bobOffset = Math.sin(mob.tickCount * 0.5) * 0.15;  // Small vertical offset

        mob.getLookControl().setLookAt(
            grassPos.getX() + 0.5,
            grassPos.getY() + 0.3 + bobOffset,  // Slight vertical variation
            grassPos.getZ() + 0.5
        );
    }

    // ========== FLEEING ANIMATIONS ==========

    /**
     * Triggers a startled jump reaction when a prey animal first detects a predator.
     * Provides immediate visual feedback that the predator has been spotted.
     *
     * @param mob the prey mob that is startled
     */
    public static void playStartledJump(Mob mob) {
        if (!mob.onGround()) {
            return;
        }

        Vec3 currentMotion = mob.getDeltaMovement();
        double jumpStrength = 0.42;
        mob.setDeltaMovement(
            currentMotion.x * 0.5,
            jumpStrength,
            currentMotion.z * 0.5
        );

        playStartledSound(mob);
    }

    /**
     * Spawns dust particles from a fleeing animal's feet as it runs.
     * Creates a visual trail showing panic movement.
     *
     * @param mob the fleeing mob
     * @param tickCount current tick in the flee animation (for pacing)
     */
    public static void spawnFleeingDustParticles(Mob mob, int tickCount) {
        Level level = mob.level();
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }

        if (!mob.onGround()) {
            return;
        }

        if (tickCount % 3 != 0) {
            return;
        }

        Vec3 velocity = mob.getDeltaMovement();
        double speed = velocity.horizontalDistance();

        if (speed < 0.1) {
            return;
        }

        int particleCount = 2 + mob.getRandom().nextInt(3);
        for (int i = 0; i < particleCount; i++) {
            double offsetX = (mob.getRandom().nextDouble() - 0.5) * mob.getBbWidth();
            double offsetZ = (mob.getRandom().nextDouble() - 0.5) * mob.getBbWidth();

            serverLevel.sendParticles(
                ParticleTypes.POOF,
                mob.getX() + offsetX,
                mob.getY() + 0.1,
                mob.getZ() + offsetZ,
                1,
                0, 0, 0,
                0.02
            );
        }
    }

    /**
     * Plays distress vocalization when a prey animal is fleeing.
     * Different animal types have characteristic distress calls.
     *
     * @param mob the fleeing mob
     * @param tickCount current tick in the flee animation (for pacing)
     */
    public static void playDistressSound(Mob mob, int tickCount) {
        if (tickCount % 40 != 0) {
            return;
        }

        mob.playAmbientSound();
    }

    /**
     * Makes a fleeing mob occasionally look back at its pursuing predator.
     * Creates realistic vigilance behavior during escape.
     *
     * @param mob the fleeing mob
     * @param predator the pursuing predator
     * @param tickCount current tick in the flee animation
     */
    public static void applyFleeingLookBack(Mob mob, LivingEntity predator, int tickCount) {
        int lookCycle = tickCount % 40;

        if (lookCycle >= 35 && lookCycle <= 40) {
            mob.getLookControl().setLookAt(predator, 30.0F, 30.0F);
        }
    }

    /**
     * Plays a startled/alarm sound when predator is first detected.
     *
     * @param mob the startled mob
     */
    private static void playStartledSound(Mob mob) {
        mob.level().playSound(
            null,
            mob.getX(),
            mob.getY(),
            mob.getZ(),
            SoundEvents.RABBIT_JUMP,
            SoundSource.NEUTRAL,
            0.6F,
            1.2F + mob.getRandom().nextFloat() * 0.3F
        );
    }
}
