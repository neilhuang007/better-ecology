package me.javavirtualenv.behavior.feline;

import me.javavirtualenv.behavior.core.BehaviorContext;
import me.javavirtualenv.behavior.steering.SteeringBehavior;
import me.javavirtualenv.behavior.core.Vec3d;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;

/**
 * Hiss behavior for threatened felines.
 * <p>
 * Cats hiss when:
 * - Detecting a creeper
 * - Feeling threatened by larger mobs
 * - Defending territory
 * - Warning before attacking
 * <p>
 * Hissing acts as a warning signal and can deter creepers.
 */
public class HissBehavior extends SteeringBehavior {

    private boolean isHissing = false;
    private int hissTicks = 0;
    private final int hissDuration;
    private final double threatRange;
    private Entity currentThreat;

    public HissBehavior(int hissDuration, double threatRange) {
        super(0.0);
        this.hissDuration = hissDuration;
        this.threatRange = threatRange;
    }

    public HissBehavior() {
        this(40, 8.0);
    }

    @Override
    public Vec3d calculate(BehaviorContext context) {
        Mob mob = context.getEntity();

        // Check for threats
        Entity threat = detectThreat(mob, context);

        if (threat != null) {
            currentThreat = threat;

            if (!isHissing) {
                startHissing(mob);
            }

            hissTicks++;

            // Deter creepers with hissing
            if (threat instanceof Creeper creeper) {
                deterCreeper(creeper, mob);
            }

            if (hissTicks > hissDuration) {
                stopHissing();
            }
        } else {
            if (isHissing) {
                stopHissing();
            }
        }

        return new Vec3d();
    }

    private Entity detectThreat(Mob mob, BehaviorContext context) {
        // Check for creepers first (highest priority)
        for (Entity entity : mob.level().getEntitiesOfClass(
                Creeper.class,
                mob.getBoundingBox().inflate(threatRange))) {
            if (entity.isAlive()) {
                return entity;
            }
        }

        // Check for other threatening entities
        for (Entity entity : mob.level().getEntitiesOfClass(
                Mob.class,
                mob.getBoundingBox().inflate(threatRange))) {
            if (entity.equals(mob)) {
                continue;
            }

            if (!entity.isAlive()) {
                continue;
            }

            // Larger entities are threats
            if (entity.getBbWidth() > mob.getBbWidth() * 1.3) {
                return entity;
            }
        }

        return null;
    }

    private void startHissing(Mob mob) {
        isHissing = true;
        hissTicks = 0;
        playHissSound(mob);
    }

    private void stopHissing() {
        isHissing = false;
        hissTicks = 0;
        currentThreat = null;
    }

    private void playHissSound(Mob mob) {
        Level level = mob.level();
        if (!level.isClientSide) {
            level.playSound(null, mob.blockPosition(), SoundEvents.CAT_HISS,
                SoundSource.NEUTRAL, 0.8f, 1.0f);
        }
    }

    private void deterCreeper(Creeper creeper, Mob cat) {
        // Hissing deters creepers - makes them flee
        // This is handled by creeper's explosion being cancelled
        // when a cat/ocelot is nearby (vanilla behavior)
    }

    public boolean isHissing() {
        return isHissing;
    }

    public Entity getCurrentThreat() {
        return currentThreat;
    }

    public void hissAt(Entity threat) {
        currentThreat = threat;
        startHissing(null);
    }
}
