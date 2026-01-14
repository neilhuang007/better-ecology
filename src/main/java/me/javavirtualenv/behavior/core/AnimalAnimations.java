package me.javavirtualenv.behavior.core;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
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
     *
     * @param mob the mob that is drinking
     * @param waterPos the position of the water block
     * @param tickCount current tick in the drinking animation
     */
    public static void playDrinkingAnimation(Mob mob, BlockPos waterPos, int tickCount) {
        Level level = mob.level();

        // Make mob look at water
        mob.getLookControl().setLookAt(
            waterPos.getX() + 0.5,
            waterPos.getY() + 0.5,
            waterPos.getZ() + 0.5
        );

        // Play splash particles every 5 ticks
        if (tickCount % 5 == 0) {
            spawnDrinkingParticles(mob, waterPos);
        }

        // Play drinking sound every 10 ticks
        if (tickCount % 10 == 0) {
            playDrinkingSound(mob);
        }

        // Head bobbing animation - dip head toward water
        if (tickCount % 4 == 0) {
            // Broadcast entity event for head movement
            level.broadcastEntityEvent(mob, EVENT_EAT_GRASS);
        }
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

        // Spawn splash particles at water surface
        double waterX = waterPos.getX() + 0.5;
        double waterY = waterPos.getY() + 1.0;  // Top of water block
        double waterZ = waterPos.getZ() + 0.5;

        // Create ripple effect with multiple particles
        for (int i = 0; i < 4; i++) {
            double offsetX = (mob.getRandom().nextDouble() - 0.5) * 0.4;
            double offsetZ = (mob.getRandom().nextDouble() - 0.5) * 0.4;

            serverLevel.sendParticles(
                ParticleTypes.SPLASH,
                waterX + offsetX,
                waterY,
                waterZ + offsetZ,
                1,
                0, 0.1, 0,
                0.1
            );
        }

        // Occasional drip particle near mob's mouth
        if (mob.getRandom().nextFloat() < 0.3f) {
            Vec3 lookVec = mob.getLookAngle();
            serverLevel.sendParticles(
                ParticleTypes.DRIPPING_WATER,
                mob.getX() + lookVec.x * 0.4,
                mob.getY() + mob.getEyeHeight() - 0.3,
                mob.getZ() + lookVec.z * 0.4,
                1,
                0, 0, 0,
                0
            );
        }
    }

    /**
     * Plays the drinking sound for a mob.
     *
     * @param mob the mob drinking
     */
    public static void playDrinkingSound(Mob mob) {
        mob.level().playSound(
            null,
            mob.getX(),
            mob.getY(),
            mob.getZ(),
            SoundEvents.GENERIC_DRINK,
            SoundSource.NEUTRAL,
            0.4F + mob.getRandom().nextFloat() * 0.2F,
            0.9F + mob.getRandom().nextFloat() * 0.2F
        );
    }

    // ========== GRAZING ANIMATIONS ==========

    /**
     * Plays grazing animation for herbivores eating grass.
     *
     * @param mob the mob that is grazing
     * @param grassPos the position of the grass being eaten
     * @param tickCount current tick in the grazing animation
     */
    public static void playGrazingAnimation(Mob mob, BlockPos grassPos, int tickCount) {
        Level level = mob.level();

        // Make mob look at grass
        mob.getLookControl().setLookAt(
            grassPos.getX() + 0.5,
            grassPos.getY() + 0.3,
            grassPos.getZ() + 0.5
        );

        // Trigger head-down animation (same as sheep eating)
        if (tickCount % 4 == 0) {
            level.broadcastEntityEvent(mob, EVENT_EAT_GRASS);
        }

        // Play grass rustling/eating sound
        if (tickCount % 6 == 0) {
            playGrazingSound(mob);
        }

        // Spawn grass particles
        if (tickCount % 8 == 0) {
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
}
