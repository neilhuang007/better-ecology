package me.javavirtualenv.behavior.parent;

import java.util.HashMap;
import java.util.Map;

/**
 * Configuration for parent-offspring behaviors.
 * Provides species-specific settings for following, protection, separation distress, and hiding.
 */
public class ParentOffspringConfig {

    private final Map<String, SpeciesConfig> speciesConfigs;

    public ParentOffspringConfig() {
        this.speciesConfigs = new HashMap<>();
        initializeDefaultConfigs();
    }

    private void initializeDefaultConfigs() {
        addSpeciesConfig("cow", new SpeciesConfig(
                SpeciesType.FOLLOWER,
                4.0,
                10.0,
                1.0,
                2.0,
                -24000,
                16.0,
                24.0,
                1.5,
                1.0,
                0.0,
                60.0,
                false
        ));

        addSpeciesConfig("sheep", new SpeciesConfig(
                SpeciesType.FOLLOWER,
                3.5,
                8.0,
                0.9,
                1.8,
                -24000,
                14.0,
                20.0,
                1.4,
                1.0,
                0.0,
                60.0,
                false
        ));

        addSpeciesConfig("pig", new SpeciesConfig(
                SpeciesType.FOLLOWER,
                4.0,
                9.0,
                1.0,
                2.0,
                -24000,
                14.0,
                20.0,
                1.4,
                1.0,
                0.0,
                60.0,
                false
        ));

        addSpeciesConfig("chicken", new SpeciesConfig(
                SpeciesType.FOLLOWER,
                2.5,
                6.0,
                0.8,
                1.5,
                -24000,
                10.0,
                16.0,
                1.3,
                0.9,
                0.0,
                45.0,
                false
        ));

        addSpeciesConfig("wolf", new SpeciesConfig(
                SpeciesType.FOLLOWER,
                5.0,
                15.0,
                1.2,
                2.5,
                -24000,
                20.0,
                32.0,
                1.8,
                1.5,
                0.0,
                90.0,
                false
        ));

        addSpeciesConfig("cat", new SpeciesConfig(
                SpeciesType.FOLLOWER,
                3.0,
                8.0,
                1.1,
                2.0,
                -24000,
                12.0,
                18.0,
                1.4,
                1.2,
                0.0,
                50.0,
                false
        ));

        addSpeciesConfig("rabbit", new SpeciesConfig(
                SpeciesType.HIDER,
                0.0,
                0.0,
                0.0,
                0.0,
                -24000,
                8.0,
                16.0,
                1.2,
                0.8,
                8.0,
                30.0,
                true
        ));

        addSpeciesConfig("deer", new SpeciesConfig(
                SpeciesType.HIDER,
                0.0,
                0.0,
                0.0,
                0.0,
                -24000,
                12.0,
                20.0,
                1.5,
                1.0,
                10.0,
                6000.0,
                true
        ));

        addSpeciesConfig("horse", new SpeciesConfig(
                SpeciesType.FOLLOWER,
                6.0,
                18.0,
                1.3,
                3.0,
                -24000,
                18.0,
                28.0,
                1.6,
                1.2,
                0.0,
                80.0,
                false
        ));

        addSpeciesConfig("fox", new SpeciesConfig(
                SpeciesType.FOLLOWER,
                4.0,
                12.0,
                1.1,
                2.2,
                -24000,
                14.0,
                22.0,
                1.5,
                1.1,
                0.0,
                60.0,
                false
        ));

        addSpeciesConfig("panda", new SpeciesConfig(
                SpeciesType.FOLLOWER,
                3.0,
                8.0,
                0.7,
                1.5,
                -24000,
                12.0,
                18.0,
                1.2,
                0.8,
                0.0,
                60.0,
                false
        ));

        addSpeciesConfig("polar_bear", new SpeciesConfig(
                SpeciesType.FOLLOWER,
                6.0,
                16.0,
                1.1,
                2.5,
                -24000,
                20.0,
                30.0,
                1.6,
                1.3,
                0.0,
                90.0,
                false
        ));

        addSpeciesConfig("turtle", new SpeciesConfig(
                SpeciesType.FOLLOWER,
                2.0,
                5.0,
                0.6,
                1.2,
                -24000,
                8.0,
                12.0,
                1.0,
                0.7,
                0.0,
                30.0,
                false
        ));

        addSpeciesConfig("bee", new SpeciesConfig(
                SpeciesType.FOLLOWER,
                1.5,
                4.0,
                0.8,
                1.0,
                -24000,
                6.0,
                10.0,
                1.2,
                0.6,
                0.0,
                20.0,
                false
        ));

        addSpeciesConfig("axolotl", new SpeciesConfig(
                SpeciesType.FOLLOWER,
                3.0,
                8.0,
                0.9,
                1.8,
                -24000,
                10.0,
                16.0,
                1.3,
                0.9,
                0.0,
                40.0,
                false
        ));

        addSpeciesConfig("frog", new SpeciesConfig(
                SpeciesType.FOLLOWER,
                2.0,
                5.0,
                0.8,
                1.2,
                -24000,
                6.0,
                12.0,
                1.1,
                0.7,
                0.0,
                30.0,
                false
        ));

        addSpeciesConfig("goat", new SpeciesConfig(
                SpeciesType.FOLLOWER,
                5.0,
                14.0,
                1.1,
                2.5,
                -24000,
                16.0,
                24.0,
                1.5,
                1.1,
                0.0,
                70.0,
                false
        ));

        addSpeciesConfig("llama", new SpeciesConfig(
                SpeciesType.FOLLOWER,
                5.0,
                14.0,
                1.0,
                2.3,
                -24000,
                16.0,
                24.0,
                1.4,
                1.0,
                0.0,
                70.0,
                false
        ));

        addSpeciesConfig("mooshroom", new SpeciesConfig(
                SpeciesType.FOLLOWER,
                4.0,
                10.0,
                1.0,
                2.0,
                -24000,
                16.0,
                24.0,
                1.5,
                1.0,
                0.0,
                60.0,
                false
        ));

        addSpeciesConfig("ocelot", new SpeciesConfig(
                SpeciesType.FOLLOWER,
                3.0,
                8.0,
                1.1,
                2.0,
                -24000,
                12.0,
                18.0,
                1.4,
                1.2,
                0.0,
                50.0,
                false
        ));

        addSpeciesConfig("parrot", new SpeciesConfig(
                SpeciesType.FOLLOWER,
                3.0,
                8.0,
                1.0,
                2.0,
                -24000,
                12.0,
                18.0,
                1.3,
                1.0,
                0.0,
                50.0,
                false
        ));

        addSpeciesConfig("dolphin", new SpeciesConfig(
                SpeciesType.FOLLOWER,
                4.0,
                12.0,
                1.2,
                2.5,
                -24000,
                16.0,
                24.0,
                1.5,
                1.2,
                0.0,
                60.0,
                false
        ));

        addSpeciesConfig("squid", new SpeciesConfig(
                SpeciesType.FOLLOWER,
                3.0,
                8.0,
                0.8,
                1.8,
                -24000,
                10.0,
                16.0,
                1.2,
                0.8,
                0.0,
                40.0,
                false
        ));

        addSpeciesConfig("glow_squid", new SpeciesConfig(
                SpeciesType.FOLLOWER,
                3.0,
                8.0,
                0.8,
                1.8,
                -24000,
                10.0,
                16.0,
                1.2,
                0.8,
                0.0,
                40.0,
                false
        ));

        addSpeciesConfig("sniffer", new SpeciesConfig(
                SpeciesType.FOLLOWER,
                4.0,
                10.0,
                0.7,
                1.8,
                -24000,
                12.0,
                20.0,
                1.2,
                0.8,
                0.0,
                60.0,
                false
        ));

        addSpeciesConfig("armadillo", new SpeciesConfig(
                SpeciesType.FOLLOWER,
                3.0,
                8.0,
                0.8,
                1.5,
                -24000,
                10.0,
                18.0,
                1.2,
                0.9,
                0.0,
                50.0,
                false
        ));
    }

    public void addSpeciesConfig(String speciesId, SpeciesConfig config) {
        speciesConfigs.put(speciesId.toLowerCase(), config);
    }

    public SpeciesConfig getSpeciesConfig(String speciesId) {
        return speciesConfigs.get(speciesId.toLowerCase());
    }

    public boolean hasConfig(String speciesId) {
        return speciesConfigs.containsKey(speciesId.toLowerCase());
    }

    public Map<String, SpeciesConfig> getAllConfigs() {
        return new HashMap<>(speciesConfigs);
    }

    public enum SpeciesType {
        FOLLOWER,
        HIDER
    }

    public static class SpeciesConfig {
        public final SpeciesType speciesType;
        public final double baseFollowDistance;
        public final double maxFollowDistance;
        public final double followSpeed;
        public final double slowingRadius;
        public final int adulthoodAge;
        public final double protectionRange;
        public final double threatDetectionRange;
        public final double attackSpeed;
        public final double aggressionLevel;
        public final double motherReturnThreshold;
        public final double separationDistressThreshold;
        public final boolean isHider;

        public SpeciesConfig(SpeciesType speciesType,
                           double baseFollowDistance,
                           double maxFollowDistance,
                           double followSpeed,
                           double slowingRadius,
                           int adulthoodAge,
                           double protectionRange,
                           double threatDetectionRange,
                           double attackSpeed,
                           double aggressionLevel,
                           double motherReturnThreshold,
                           double separationDistressThreshold,
                           boolean isHider) {
            this.speciesType = speciesType;
            this.baseFollowDistance = baseFollowDistance;
            this.maxFollowDistance = maxFollowDistance;
            this.followSpeed = followSpeed;
            this.slowingRadius = slowingRadius;
            this.adulthoodAge = adulthoodAge;
            this.protectionRange = protectionRange;
            this.threatDetectionRange = threatDetectionRange;
            this.attackSpeed = attackSpeed;
            this.aggressionLevel = aggressionLevel;
            this.motherReturnThreshold = motherReturnThreshold;
            this.separationDistressThreshold = separationDistressThreshold;
            this.isHider = isHider;
        }
    }
}
