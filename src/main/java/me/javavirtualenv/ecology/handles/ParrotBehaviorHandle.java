package me.javavirtualenv.ecology.handles;

import me.javavirtualenv.behavior.parrot.*;
import me.javavirtualenv.ecology.EcologyComponent;
import me.javavirtualenv.ecology.EcologyHandle;
import me.javavirtualenv.ecology.EcologyProfile;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Handle for parrot-specific behaviors.
 * Manages mimicking, dancing, music detection, and perching.
 */
public class ParrotBehaviorHandle implements EcologyHandle {
    private MimicBehavior mimicBehavior;
    private MusicDetectionBehavior musicDetectionBehavior;
    private DanceBehavior danceBehavior;
    private PerchBehavior perchBehavior;

    private Goal mimicGoal;
    private Goal musicGoal;
    private Goal perchGoal;
    private Goal behaviorGoal;

    @Override
    public String id() {
        return "parrot_behavior";
    }

    @Override
    public void initialize(Mob mob, EcologyComponent component, @Nullable EcologyProfile profile) {
        // Get configuration from profile or use defaults
        MimicBehavior.MimicConfig mimicConfig = loadMimicConfig(profile);
        MusicDetectionBehavior.MusicConfig musicConfig = loadMusicConfig(profile);
        DanceBehavior.DanceConfig danceConfig = loadDanceConfig(profile);
        PerchBehavior.PerchConfig perchConfig = loadPerchConfig(profile);

        // Initialize behaviors
        this.mimicBehavior = new MimicBehavior(mob, mimicConfig, component);
        this.musicDetectionBehavior = new MusicDetectionBehavior(mob, musicConfig, component);
        this.danceBehavior = new DanceBehavior(mob, danceConfig, component);
        this.perchBehavior = new PerchBehavior(mob, perchConfig, component);

        // Initialize goals
        if (mob instanceof PathfinderMob pathfinderMob) {
            this.mimicGoal = new ParrotMimicGoal(pathfinderMob, mimicBehavior, mimicConfig);
            this.musicGoal = new ParrotMusicGoal(pathfinderMob, musicDetectionBehavior, danceBehavior, musicConfig);
            this.perchGoal = new ParrotPerchGoal(pathfinderMob, perchBehavior, perchConfig);

            ParrotBehaviorGoal.ParrotBehaviorConfig behaviorConfig = loadBehaviorConfig(profile);
            this.behaviorGoal = new ParrotBehaviorGoal(
                pathfinderMob,
                mimicBehavior,
                musicDetectionBehavior,
                danceBehavior,
                perchBehavior,
                behaviorConfig
            );
        }

        // Initialize component data
        initializeComponentData(component);
    }

    private void initializeComponentData(EcologyComponent component) {
        // Initialize mimic data
        CompoundTag mimicData = component.getHandleTag("mimic");
        if (!mimicData.contains("accuracy")) {
            mimicData.putDouble("accuracy", 0.75);
        }
        component.setHandleTag("mimic", mimicData);

        // Initialize dance data
        CompoundTag danceData = component.getHandleTag("dance");
        if (!danceData.contains("is_dancing")) {
            danceData.putBoolean("is_dancing", false);
        }
        component.setHandleTag("dance", danceData);

        // Initialize perch data
        CompoundTag perchData = component.getHandleTag("perch");
        if (!perchData.contains("is_perched")) {
            perchData.putBoolean("is_perched", false);
        }
        component.setHandleTag("perch", perchData);

        // Initialize note block tracking
        CompoundTag noteData = component.getHandleTag("note_blocks");
        component.setHandleTag("note_blocks", noteData);
    }

    private MimicBehavior.MimicConfig loadMimicConfig(@Nullable EcologyProfile profile) {
        MimicBehavior.MimicConfig config = new MimicBehavior.MimicConfig();

        if (profile != null) {
            // Load from profile if available
            Double baseAccuracy = profile.getDouble("mimic.base_accuracy");
            if (baseAccuracy != null) {
                config.baseMimicAccuracy = baseAccuracy;
            }

            Double mimicChance = profile.getDouble("mimic.chance");
            if (mimicChance != null) {
                config.mimicChance = mimicChance;
            }

            Double warningRange = profile.getDouble("mimic.warning_range");
            if (warningRange != null) {
                config.warningRange = warningRange;
            }
        }

        return config;
    }

    private MusicDetectionBehavior.MusicConfig loadMusicConfig(@Nullable EcologyProfile profile) {
        MusicDetectionBehavior.MusicConfig config = new MusicDetectionBehavior.MusicConfig();

        if (profile != null) {
            Integer detectionRadius = profile.getInt("music.detection_radius");
            if (detectionRadius != null) {
                config.detectionRadius = detectionRadius;
            }

            Double flightSpeed = profile.getDouble("music.flight_speed");
            if (flightSpeed != null) {
                config.flightSpeed = flightSpeed;
            }
        }

        return config;
    }

    private DanceBehavior.DanceConfig loadDanceConfig(@Nullable EcologyProfile profile) {
        DanceBehavior.DanceConfig config = new DanceBehavior.DanceConfig();

        if (profile != null) {
            Boolean showParticles = profile.getBoolean("dance.show_particles");
            if (showParticles != null) {
                config.showParticles = showParticles;
            }

            Boolean enableParty = profile.getBoolean("dance.enable_party_effect");
            if (enableParty != null) {
                config.enablePartyEffect = enableParty;
            }

            Double partyRadius = profile.getDouble("dance.party_radius");
            if (partyRadius != null) {
                config.partyRadius = partyRadius;
            }
        }

        return config;
    }

    private PerchBehavior.PerchConfig loadPerchConfig(@Nullable EcologyProfile profile) {
        PerchBehavior.PerchConfig config = new PerchBehavior.PerchConfig();

        if (profile != null) {
            Integer searchRadius = profile.getInt("perch.search_radius");
            if (searchRadius != null) {
                config.perchSearchRadius = searchRadius;
            }

            Boolean preferHigh = profile.getBoolean("perch.prefer_high");
            if (preferHigh != null) {
                config.preferHighPerches = preferHigh;
            }

            Double shoulderRange = profile.getDouble("perch.shoulder_range");
            if (shoulderRange != null) {
                config.shoulderPerchRange = shoulderRange;
            }
        }

        return config;
    }

    private ParrotBehaviorGoal.ParrotBehaviorConfig loadBehaviorConfig(@Nullable EcologyProfile profile) {
        ParrotBehaviorGoal.ParrotBehaviorConfig config = new ParrotBehaviorGoal.ParrotBehaviorConfig();

        if (profile != null) {
            Boolean enableMusic = profile.getBoolean("behavior.enable_music");
            if (enableMusic != null) {
                config.enableMusicBehavior = enableMusic;
            }

            Boolean enablePerch = profile.getBoolean("behavior.enable_perch");
            if (enablePerch != null) {
                config.enablePerchBehavior = enablePerch;
            }

            Boolean enableMimic = profile.getBoolean("behavior.enable_mimic");
            if (enableMimic != null) {
                config.enableMimicBehavior = enableMimic;
            }

            Double perchChance = profile.getDouble("behavior.perch_seek_chance");
            if (perchChance != null) {
                config.perchSeekChance = perchChance;
            }
        }

        return config;
    }

    @Override
    public void registerGoals(Mob mob, EcologyComponent component, List<Goal> goals) {
        if (mob instanceof PathfinderMob) {
            // Register parrot-specific goals
            if (behaviorGoal != null) {
                goals.add(behaviorGoal);
            }
            if (mimicGoal != null) {
                goals.add(mimicGoal);
            }
            if (musicGoal != null) {
                goals.add(musicGoal);
            }
            if (perchGoal != null) {
                goals.add(perchGoal);
            }
        }
    }

    @Override
    public void serverTick(Mob mob, EcologyComponent component) {
        // Check for dance invitations from other parrots
        checkDanceInvitations(component);
    }

    private void checkDanceInvitations(EcologyComponent component) {
        CompoundTag danceData = component.getHandleTag("dance");

        if (danceData.getBoolean("should_start_dancing")) {
            String styleName = danceData.getString("invited_style");
            DanceBehavior.DanceStyle style = DanceBehavior.DanceStyle.valueOf(styleName);

            if (style != null && danceBehavior != null) {
                danceBehavior.startDancing(style, mob.blockPosition());
            }

            // Clear the invitation
            danceData.remove("should_start_dancing");
            danceData.remove("invited_style");
            component.setHandleTag("dance", danceData);
        }
    }

    public MimicBehavior getMimicBehavior() {
        return mimicBehavior;
    }

    public MusicDetectionBehavior getMusicDetectionBehavior() {
        return musicDetectionBehavior;
    }

    public DanceBehavior getDanceBehavior() {
        return danceBehavior;
    }

    public PerchBehavior getPerchBehavior() {
        return perchBehavior;
    }
}
