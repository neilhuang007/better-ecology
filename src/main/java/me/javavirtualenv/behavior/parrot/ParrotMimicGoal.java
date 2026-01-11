package me.javavirtualenv.behavior.parrot;

import me.javavirtualenv.BetterEcology;
import me.javavirtualenv.debug.BehaviorLogger;
import me.javavirtualenv.ecology.EcologyComponent;
import me.javavirtualenv.ecology.api.EcologyAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.monster.Monster;

import java.util.EnumSet;
import java.util.List;

/**
 * AI goal for parrot mimicking behavior.
 * Parrots mimic hostile mob sounds randomly and as warnings.
 */
public class ParrotMimicGoal extends Goal {

    private static final int WARNING_CHECK_INTERVAL = 40;
    private static final int MAX_COOLDOWN_TICKS = 200;
    private static final int WARNING_COOLDOWN_TICKS = 100;

    private final PathfinderMob parrot;
    private final MimicBehavior mimicBehavior;
    private final MimicBehavior.MimicConfig config;
    private final EcologyComponent component;

    private int warningCheckTimer;
    private int mimicCooldown;
    private int mimicsPerformed;
    private int warningsGiven;

    private String lastDebugMessage = "";
    private boolean wasOnCooldownLastCheck = false;

    public ParrotMimicGoal(PathfinderMob parrot,
                          MimicBehavior mimicBehavior,
                          MimicBehavior.MimicConfig config) {
        this.parrot = parrot;
        this.mimicBehavior = mimicBehavior;
        this.config = config;
        this.component = getComponent();
        this.setFlags(EnumSet.of(Goal.Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (!parrot.isAlive()) {
            return false;
        }

        if (parrot.level().isClientSide) {
            return false;
        }

        return true;
    }

    @Override
    public boolean canContinueToUse() {
        return canUse();
    }

    @Override
    public void start() {
        debug("goal started");
        warningCheckTimer = 0;
        mimicCooldown = 0;
        mimicsPerformed = 0;
        warningsGiven = 0;
    }

    @Override
    public void stop() {
        debug("goal stopped, mimics=" + mimicsPerformed + ", warnings=" + warningsGiven);
    }

    @Override
    public void tick() {
        boolean isOnCooldown = mimicCooldown > 0;

        if (isOnCooldown != wasOnCooldownLastCheck) {
            debug("cooldown state changed: " + wasOnCooldownLastCheck + " -> " + isOnCooldown);
            wasOnCooldownLastCheck = isOnCooldown;
        }

        if (mimicCooldown > 0) {
            mimicCooldown--;
        }

        if (tryRandomMimic()) {
            mimicsPerformed++;
            return;
        }

        warningCheckTimer++;
        if (warningCheckTimer >= WARNING_CHECK_INTERVAL) {
            warningCheckTimer = 0;
            checkForWarnings();
        }
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    private boolean tryRandomMimic() {
        if (mimicCooldown > 0) {
            return false;
        }

        CompoundTag mimicTag = component.getHandleTag("mimic");
        double mimicAccuracy = mimicTag.getDouble("accuracy");

        if (parrot.getRandom().nextDouble() > config.mimicChance) {
            return false;
        }

        MimicBehavior.MimicType mimic = selectRandomMimic();
        if (mimic == null) {
            return false;
        }

        performMimic(mimic, mimicAccuracy);
        mimicCooldown = MAX_COOLDOWN_TICKS;

        return true;
    }

    private void checkForWarnings() {
        List<Monster> nearbyHostiles = parrot.level().getNearbyEntities(
            Monster.class,
            TargetingConditions.forNonCombat(),
            parrot,
            parrot.getBoundingBox().inflate(config.warningRange)
        );

        if (nearbyHostiles.isEmpty()) {
            return;
        }

        Monster closest = findClosestHostile(nearbyHostiles);
        if (closest == null) {
            return;
        }

        double distance = parrot.distanceTo(closest);
        if (distance > config.warningRange) {
            return;
        }

        if (mimicCooldown > 0) {
            return;
        }

        if (parrot.getRandom().nextDouble() > config.warningChance) {
            return;
        }

        MimicBehavior.MimicType mimic = selectDangerousMimic();
        if (mimic == null) {
            return;
        }

        double warningBonus = (1.0 - distance / config.warningRange) * config.warningAccuracyBonus;

        CompoundTag mimicTag = component.getHandleTag("mimic");
        double baseAccuracy = mimicTag.getDouble("accuracy");
        double enhancedAccuracy = Math.min(1.0, baseAccuracy + warningBonus);

        performMimic(mimic, enhancedAccuracy);
        mimicCooldown = WARNING_COOLDOWN_TICKS;
        warningsGiven++;

        debug("warning mimic for " + closest.getType().toShortString() +
              " at distance " + String.format("%.1f", distance));
    }

    private Monster findClosestHostile(List<Monster> hostiles) {
        Monster closest = null;
        double closestDistance = Double.MAX_VALUE;

        for (Monster hostile : hostiles) {
            double distance = parrot.distanceToSqr(hostile);
            if (distance < closestDistance) {
                closestDistance = distance;
                closest = hostile;
            }
        }

        return closest;
    }

    private MimicBehavior.MimicType selectRandomMimic() {
        MimicBehavior.MimicType[] mimics = MimicBehavior.MimicType.values();
        return mimics[parrot.getRandom().nextInt(mimics.length)];
    }

    private MimicBehavior.MimicType selectDangerousMimic() {
        MimicBehavior.MimicType[] dangerousMimics = {
            MimicBehavior.MimicType.CREEPER,
            MimicBehavior.MimicType.ENDERMAN,
            MimicBehavior.MimicType.WITCH,
            MimicBehavior.MimicType.RAVAGER
        };

        return dangerousMimics[parrot.getRandom().nextInt(dangerousMimics.length)];
    }

    private void performMimic(MimicBehavior.MimicType mimic, double accuracy) {
        boolean isAccurate = parrot.getRandom().nextDouble() < accuracy;

        net.minecraft.sounds.SoundEvent sound = isAccurate
            ? mimic.getAccurateSound()
            : mimic.getImpreciseSound();

        parrot.level().playSound(
            null,
            parrot.blockPosition(),
            sound,
            parrot.getSoundSource(),
            (float) config.mimicVolume,
            config.mimicPitch
        );

        CompoundTag mimicTag = component.getHandleTag("mimic");
        mimicTag.putString("last_mimic", mimic.name());
        mimicTag.putBoolean("was_accurate", isAccurate);
        mimicTag.putLong("mimic_time", parrot.level().getGameTime());
        mimicTag.putDouble("accuracy", accuracy);
        component.setHandleTag("mimic", mimicTag);

        debug("mimic " + mimic.name() + " (" + (isAccurate ? "accurate" : "imprecise") + ")");
    }

    private EcologyComponent getComponent() {
        if (!(parrot instanceof EcologyAccess access)) {
            return null;
        }
        return access.betterEcology$getEcologyComponent();
    }

    private void debug(String message) {
        lastDebugMessage = message;
        if (BehaviorLogger.isMinimal() || BetterEcology.DEBUG_MODE) {
            String prefix = "[ParrotMimic] Parrot #" + parrot.getId() + " ";
            BehaviorLogger.info(prefix + message);
        }
    }

    public String getLastDebugMessage() {
        return lastDebugMessage;
    }

    public String getDebugState() {
        CompoundTag mimicTag = component.getHandleTag("mimic");
        String lastMimic = mimicTag.getString("last_mimic");
        double accuracy = mimicTag.getDouble("accuracy");

        return String.format(
            "cooldown=%d, mimics=%d, warnings=%d, last_mimic=%s, accuracy=%.2f",
            mimicCooldown,
            mimicsPerformed,
            warningsGiven,
            lastMimic.isEmpty() ? "none" : lastMimic,
            accuracy
        );
    }

    public int getMimicsPerformed() {
        return mimicsPerformed;
    }

    public int getWarningsGiven() {
        return warningsGiven;
    }

    public boolean isOnCooldown() {
        return mimicCooldown > 0;
    }
}
