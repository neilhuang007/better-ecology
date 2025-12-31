package me.javavirtualenv.behavior.fleeing;

import me.javavirtualenv.behavior.core.BehaviorContext;
import me.javavirtualenv.behavior.core.Vec3d;
import me.javavirtualenv.ecology.EcologyComponent;
import me.javavirtualenv.ecology.EcologyHooks;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.*;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Implements alarm signal behavior for warning nearby herd members of threats.
 * Supports vocal alarm calls and visual signals based on research findings.
 * <p>
 * Based on research by:
 * - Suzuki (2018) - Alarm calls evoke visual search images (PNAS)
 * - Various studies on cross-species alarm eavesdropping
 * - Ungulate tail-flagging and visual anti-predator signals
 * <p>
 * Features:
 * - Vocal alarm calls with configurable range
 * - Visual signals (tail flagging for deer, etc.)
 * - Alert range configuration
 * - Cross-species warning capabilities
 * - Alarm cooldown to prevent spam
 */
public class AlarmSignalBehavior {

    private final FleeingConfig config;

    // Alarm state
    private int alarmCooldown = 0;
    private long lastAlarmTime = 0;
    private LivingEntity lastThreat = null;

    // Alarm statistics
    private int totalAlarmsRaised = 0;
    private int herdMembersAlerted = 0;

    public AlarmSignalBehavior(FleeingConfig config) {
        this.config = config;
    }

    public AlarmSignalBehavior() {
        this(FleeingConfig.createDefault());
    }

    /**
     * Checks if alarm should be raised and raises it if conditions are met.
     *
     * @param context Behavior context
     * @param threat  Detected threat entity
     * @return true if alarm was raised
     */
    public boolean raiseAlarmIfNeeded(BehaviorContext context, LivingEntity threat) {
        Mob entity = context.getEntity();
        long currentTime = entity.level().getGameTime();

        // Check cooldown
        if (alarmCooldown > 0) {
            alarmCooldown--;
            return false;
        }

        // Check if alarm is warranted
        if (!shouldRaiseAlarm(context, threat)) {
            return false;
        }

        // Check if any nearby animals haven't noticed the threat yet
        List<Mob> unawareHerdMembers = findUnawareHerdMembers(context, threat);
        if (unawareHerdMembers.isEmpty() && !config.isCrossSpeciesWarning()) {
            // Everyone already knows, no need to alarm
            return false;
        }

        // Raise the alarm
        raiseAlarm(context, threat, unawareHerdMembers);

        // Set cooldown
        alarmCooldown = config.getAlarmCooldown();
        lastAlarmTime = currentTime;
        lastThreat = threat;

        return true;
    }

    /**
     * Determines if alarm should be raised based on threat assessment.
     *
     * @param context Behavior context
     * @param threat  Potential threat
     * @return true if alarm should be raised
     */
    private boolean shouldRaiseAlarm(BehaviorContext context, LivingEntity threat) {
        // No alarm system configured
        if (config.getAlarmCallRange() <= 0) {
            return false;
        }

        // No threat detected
        if (threat == null) {
            return false;
        }

        // Check if threat is within alarming range
        Vec3d position = context.getPosition();
        Vec3d threatPos = new Vec3d(threat.getX(), threat.getY(), threat.getZ());
        double distance = position.distanceTo(threatPos);

        // Only alarm if threat is within FID range but not too close
        if (distance > config.getFlightInitiationDistance() * 1.5) {
            return false; // Too far
        }

        if (distance < config.getFlightInitiationDistance() * 0.3) {
            return false; // Too close, focus on fleeing
        }

        // Check if this is a significant threat type
        if (!isSignificantThreat(context, threat)) {
            return false;
        }

        return true;
    }

    /**
     * Raises the alarm through vocal and/or visual signals.
     *
     * @param context   Behavior context
     * @param threat    Threat entity
     * @param herdMembers List of unaware herd members
     */
    private void raiseAlarm(BehaviorContext context, LivingEntity threat, List<Mob> herdMembers) {
        Mob entity = context.getEntity();
        Vec3d position = context.getPosition();

        // Play vocal alarm sound
        playAlarmSound(context);

        // Emit visual alarm signal
        emitVisualSignal(context);

        // Alert nearby herd members
        alertHerdMembers(context, threat, herdMembers);

        // Alert other species if cross-species warning enabled
        if (config.isCrossSpeciesWarning()) {
            alertOtherSpecies(context, threat);
        }

        totalAlarmsRaised++;
        herdMembersAlerted += herdMembers.size();
    }

    /**
     * Plays species-appropriate alarm sound.
     *
     * @param context Behavior context
     */
    private void playAlarmSound(BehaviorContext context) {
        Mob entity = context.getEntity();
        SoundEvent alarmSound = getAlarmSound(entity);

        if (alarmSound != null && !entity.level().isClientSide()) {
            ServerLevel serverLevel = (ServerLevel) entity.level();
            Vec3d position = context.getPosition();

            // Play sound at entity location
            serverLevel.playSound(
                null,
                entity.getX(),
                entity.getY(),
                entity.getZ(),
                alarmSound,
                SoundSource.NEUTRAL,
                1.0F, // Volume
                1.2F  // Pitch (higher pitch for alarm)
            );
        }
    }

    /**
     * Gets the appropriate alarm sound for the entity type.
     *
     * @param entity Entity that is alarming
     * @return Alarm sound event, or null if no alarm sound
     */
    private SoundEvent getAlarmSound(Mob entity) {
        // Return species-specific alarm sounds
        if (entity instanceof Pig) {
            return SoundEvents.PIG_HURT;
        } else if (entity instanceof Sheep) {
            return SoundEvents.SHEEP_AMBIENT; // Higher pitch handled by playAlarmSound
        } else if (entity instanceof Cow) {
            return SoundEvents.COW_AMBIENT;
        } else if (entity instanceof Chicken) {
            return SoundEvents.CHICKEN_HURT;
        } else if (entity instanceof Rabbit) {
            return SoundEvents.RABBIT_HURT;
        } else if (entity instanceof Wolf) {
            return SoundEvents.WOLF_HOWL;
        } else if (entity instanceof Cat) {
            return SoundEvents.CAT_HISS;
        } else if (entity instanceof Parrot) {
            return SoundEvents.PARROT_IMITATE_ZOMBIE; // Alarm mimicry
        } else if (entity instanceof Fox) {
            return SoundEvents.FOX_AGGRO; // Use AGGRO sound instead of SCREAM
        }

        // Default: return null - no sound for unknown types
        return null;
    }

    /**
     * Emits visual alarm signals (tail flagging, etc.).
     * In Minecraft, this could be represented by particle effects or animations.
     *
     * @param context Behavior context
     */
    private void emitVisualSignal(BehaviorContext context) {
        Mob entity = context.getEntity();

        // Visual alarm indicators (would be enhanced with custom rendering)
        // For now, we note the behavior - full implementation would include:
        // - Tail flagging particles for deer
        // - Ear position changes
        // - Body posture changes
        // - Staring behavior (looking at threat)

        // Make entity look at threat to alert others
        if (lastThreat != null) {
            entity.getLookControl().setLookAt(lastThreat, 30.0F, 30.0F);
        }
    }

    /**
     * Alerts nearby herd members of the threat.
     *
     * @param context   Behavior context
     * @param threat    Threat entity
     * @param herdMembers List of unaware herd members
     */
    private void alertHerdMembers(BehaviorContext context, LivingEntity threat, List<Mob> herdMembers) {
        for (Mob member : herdMembers) {
            // Set the threat as target for the herd member
            // This triggers their fleeing behavior
            alertEntity(member, threat);
        }
    }

    /**
     * Alerts other species in the area (cross-species alarm eavesdropping).
     * Many animals learn to recognize other species' alarm calls.
     *
     * @param context Behavior context
     * @param threat  Threat entity
     */
    private void alertOtherSpecies(BehaviorContext context, LivingEntity threat) {
        Mob entity = context.getEntity();
        double alarmRange = config.getAlarmCallRange();

        List<Mob> nearbyEntities = entity.level().getEntitiesOfClass(
            Mob.class,
            entity.getBoundingBox().inflate(alarmRange)
        );

        for (Mob nearby : nearbyEntities) {
            // Skip same species (already handled)
            if (nearby.getType() == entity.getType()) {
                continue;
            }

            // Skip the threat itself
            if (nearby == threat) {
                continue;
            }

            // Check if this species would respond to cross-species alarm
            if (respondsToCrossSpeciesAlarm(nearby)) {
                alertEntity(nearby, threat);
            }
        }
    }

    /**
     * Alerts a single entity to the presence of a threat.
     * Sets the panic state on the entity's EcologyComponent which triggers
     * fleeing behavior via PanicBehavior on the next tick.
     *
     * @param entity Entity to alert
     * @param threat Threat to alert about
     */
    private void alertEntity(Mob entity, LivingEntity threat) {
        // Make the entity look at the threat for visual awareness
        entity.getLookControl().setLookAt(threat, 30.0F, 30.0F);

        // Trigger panic state via EcologyComponent - this is what actually causes fleeing
        EcologyComponent component = EcologyHooks.getEcologyComponent(entity);
        if (component != null) {
            // Set panic state - PanicBehavior will detect this and trigger flee on next tick
            component.state().setIsPanicking(true);

            // Also set fleeing state for immediate effect
            component.state().setIsFleeing(true);
        }

        // For entities without EcologyComponent, try to trigger vanilla panic via hurt
        // This is a fallback that works for fish and other vanilla entities
        if (component == null && entity.getLastHurtByMob() == null) {
            // Set the threat as the last attacker to trigger vanilla PanicGoal
            entity.setLastHurtByMob(threat);
        }
    }

    /**
     * Finds herd members that haven't noticed the threat yet.
     *
     * @param context Behavior context
     * @param threat  Threat entity
     * @return List of unaware herd members
     */
    private List<Mob> findUnawareHerdMembers(BehaviorContext context, LivingEntity threat) {
        Mob entity = context.getEntity();
        Vec3d threatPos = new Vec3d(threat.getX(), threat.getY(), threat.getZ());
        double alarmRange = config.getAlarmCallRange();

        List<Mob> nearbyEntities = entity.level().getEntitiesOfClass(
            Mob.class,
            entity.getBoundingBox().inflate(alarmRange)
        );

        List<Mob> unawareMembers = new java.util.ArrayList<>();

        for (Mob nearby : nearbyEntities) {
            // Only include same species
            if (nearby.getType() != entity.getType()) {
                continue;
            }

            // Skip self
            if (nearby == entity) {
                continue;
            }

            // Check if this herd member already knows about the threat
            if (isAwareOfThreat(nearby, threatPos)) {
                continue;
            }

            unawareMembers.add(nearby);
        }

        return unawareMembers;
    }

    /**
     * Checks if an entity is already aware of a threat.
     *
     * @param entity    Entity to check
     * @param threatPos Position of threat
     * @return true if entity appears aware
     */
    private boolean isAwareOfThreat(Mob entity, Vec3d threatPos) {
        // Check if entity is looking toward threat
        Vec3d entityPos = new Vec3d(entity.getX(), entity.getY(), entity.getZ());
        Vec3 lookDir = entity.getLookAngle();
        Vec3d lookDirVec = new Vec3d(lookDir.x, lookDir.y, lookDir.z);
        Vec3d toThreat = Vec3d.sub(threatPos, entityPos);

        // Normalize vectors
        lookDirVec.normalize();
        toThreat.normalize();

        // Check if looking toward threat (dot product)
        double dotProduct = lookDirVec.x * toThreat.x + lookDirVec.y * toThreat.y + lookDirVec.z * toThreat.z;

        // If looking somewhat toward threat, likely aware
        return dotProduct > 0.5;
    }

    /**
     * Determines if a species responds to cross-species alarm calls.
     * Many animals, especially prey species, eavesdrop on other species' alarms.
     *
     * @param entity Entity to check
     * @return true if entity responds to cross-species alarms
     */
    private boolean respondsToCrossSpeciesAlarm(Mob entity) {
        // Prey animals typically respond
        if (entity instanceof Animal) {
            return true;
        }

        // Some predators also pay attention to alarms (to locate prey or avoid danger)
        String typeName = entity.getType().toString().toLowerCase();
        if (typeName.contains("wolf") || typeName.contains("fox") || typeName.contains("cat")) {
            return true;
        }

        return false;
    }

    /**
     * Determines if a threat is significant enough to warrant an alarm.
     *
     * @param context Behavior context
     * @param threat  Threat entity
     * @return true if threat is significant
     */
    private boolean isSignificantThreat(BehaviorContext context, LivingEntity threat) {
        // Players are always significant
        if (threat instanceof net.minecraft.world.entity.player.Player) {
            return true;
        }

        // Predators are significant
        String typeName = threat.getType().toString().toLowerCase();
        if (typeName.contains("wolf") || typeName.contains("fox") ||
            typeName.contains("spider") || typeName.contains("phantom") ||
            typeName.contains("cat")) {
            return true;
        }

        // Aggressive mobs are significant
        if (threat instanceof Mob mob && mob.isAggressive()) {
            return true;
        }

        return false;
    }

    /**
     * Gets the alarm range configuration.
     *
     * @return Alarm call range in blocks
     */
    public double getAlarmRange() {
        return config.getAlarmCallRange();
    }

    /**
     * Gets the current cooldown remaining.
     *
     * @return Cooldown ticks
     */
    public int getCooldown() {
        return alarmCooldown;
    }

    /**
     * Gets the last threat that triggered an alarm.
     *
     * @return Last threat entity
     */
    public LivingEntity getLastThreat() {
        return lastThreat;
    }

    /**
     * Gets the time of last alarm.
     *
     * @return Game time of last alarm
     */
    public long getLastAlarmTime() {
        return lastAlarmTime;
    }

    /**
     * Gets alarm statistics.
     *
     * @return Total alarms raised
     */
    public int getTotalAlarmsRaised() {
        return totalAlarmsRaised;
    }

    /**
     * Gets total herd members alerted.
     *
     * @return Total members alerted across all alarms
     */
    public int getHerdMembersAlerted() {
        return herdMembersAlerted;
    }

    /**
     * Gets the config.
     *
     * @return Fleeing config
     */
    public FleeingConfig getConfig() {
        return config;
    }

    /**
     * Resets alarm state.
     */
    public void reset() {
        alarmCooldown = 0;
        lastAlarmTime = 0;
        lastThreat = null;
        totalAlarmsRaised = 0;
        herdMembersAlerted = 0;
    }

    /**
     * Forces an alarm to be raised immediately (useful for testing).
     *
     * @param context Behavior context
     * @param threat  Threat to alarm about
     * @return true if alarm was raised
     */
    public boolean forceAlarm(BehaviorContext context, LivingEntity threat) {
        raiseAlarm(context, threat, findUnawareHerdMembers(context, threat));
        return true;
    }
}
