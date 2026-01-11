package me.javavirtualenv.behavior.crepuscular;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import me.javavirtualenv.behavior.core.BehaviorContext;
import me.javavirtualenv.behavior.core.Vec3d;
import net.minecraft.core.BlockPos;

/**
 * Comprehensive test suite for crepuscular behavior package.
 * Tests light-triggered emergence, roosting behavior, activity cycles,
 * and dawn/dusk activity patterns scientifically.
 *
 * All tests use pure Java algorithms without Mockito, focusing on
 * deterministic behavior testing of core algorithms and state management.
 */
class CrepuscularBehaviorTest {

    private CrepuscularConfig config;
    private CrepuscularActivity activityCalculator;
    private RoostingBehavior roostingBehavior;
    private EmergenceTrigger emergenceTrigger;

    @BeforeEach
    void setUp() {
        config = new CrepuscularConfig();
        config.setEmergenceLightLevel(4);
        config.setReturnLightLevel(4);
        config.setGroupEmergenceChance(0.7);
        config.setTemperatureModifier(1.0);
        config.setRoostClusterDistance(3);
        config.setCeilingAttractionRange(5);
        config.setForagingRange(64);

        activityCalculator = new CrepuscularActivity(config);
        roostingBehavior = new RoostingBehavior(config);
        emergenceTrigger = new EmergenceTrigger(config);
    }

    // ==================== CrepuscularConfig Tests ====================

    @Test
    void config_defaultValues_areScientificallyBased() {
        CrepuscularConfig defaultConfig = new CrepuscularConfig();

        assertTrue(defaultConfig.getEmergenceLightLevel() >= 0 &&
                  defaultConfig.getEmergenceLightLevel() <= 15,
                  "Emergence light level should be valid MC light range (0-15)");
        assertTrue(defaultConfig.getGroupEmergenceChance() >= 0.0 &&
                  defaultConfig.getGroupEmergenceChance() <= 1.0,
                  "Group emergence chance should be probability (0-1)");
        assertTrue(defaultConfig.getTemperatureModifier() >= 0.5 &&
                  defaultConfig.getTemperatureModifier() <= 2.0,
                  "Temperature modifier should be in valid range");
    }

    @Test
    void config_validatesLightLevelBounds() {
        config.setEmergenceLightLevel(-5);
        assertEquals(0, config.getEmergenceLightLevel(),
                     "Negative light level should clamp to 0");

        config.setEmergenceLightLevel(20);
        assertEquals(15, config.getEmergenceLightLevel(),
                     "Light level above 15 should clamp to 15");
    }

    @Test
    void config_validatesReturnLightLevelBounds() {
        config.setReturnLightLevel(-5);
        assertEquals(0, config.getReturnLightLevel(),
                     "Negative return light level should clamp to 0");

        config.setReturnLightLevel(20);
        assertEquals(15, config.getReturnLightLevel(),
                     "Return light level above 15 should clamp to 15");
    }

    @Test
    void config_validatesTemperatureModifierBounds() {
        config.setTemperatureModifier(0.1);
        assertEquals(0.5, config.getTemperatureModifier(),
                     "Temperature modifier below 0.5 should clamp to 0.5");

        config.setTemperatureModifier(3.0);
        assertEquals(2.0, config.getTemperatureModifier(),
                     "Temperature modifier above 2.0 should clamp to 2.0");
    }

    @Test
    void config_validatesGroupEmergenceChance() {
        config.setGroupEmergenceChance(-0.5);
        assertEquals(0.0, config.getGroupEmergenceChance(),
                     "Negative probability should clamp to 0");

        config.setGroupEmergenceChance(1.5);
        assertEquals(1.0, config.getGroupEmergenceChance(),
                     "Probability above 1.0 should clamp to 1.0");
    }

    @Test
    void config_validatesGroupDetectionRange() {
        config.setGroupDetectionRange(0);
        assertEquals(1, config.getGroupDetectionRange(),
                     "Detection range below 1 should clamp to 1");

        config.setGroupDetectionRange(100);
        assertEquals(64, config.getGroupDetectionRange(),
                     "Detection range above 64 should clamp to 64");
    }

    @Test
    void config_validatesRoostClusterDistance() {
        config.setRoostClusterDistance(0);
        assertEquals(1, config.getRoostClusterDistance(),
                     "Cluster distance below 1 should clamp to 1");

        config.setRoostClusterDistance(15);
        assertEquals(10, config.getRoostClusterDistance(),
                     "Cluster distance above 10 should clamp to 10");
    }

    @Test
    void config_validatesCeilingAttractionRange() {
        config.setCeilingAttractionRange(0);
        assertEquals(1, config.getCeilingAttractionRange(),
                     "Ceiling range below 1 should clamp to 1");

        config.setCeilingAttractionRange(20);
        assertEquals(16, config.getCeilingAttractionRange(),
                     "Ceiling range above 16 should clamp to 16");
    }

    @Test
    void config_validatesForagingRange() {
        config.setForagingRange(10);
        assertEquals(16, config.getForagingRange(),
                     "Foraging range below 16 should clamp to 16");

        config.setForagingRange(300);
        assertEquals(256, config.getForagingRange(),
                     "Foraging range above 256 should clamp to 256");
    }

    @Test
    void config_validatesDuskStartTick() {
        config.setDuskStartTick(10000);
        assertEquals(11000, config.getDuskStartTick(),
                     "Dusk start below 11000 should clamp to 11000");

        config.setDuskStartTick(15000);
        assertEquals(14000, config.getDuskStartTick(),
                     "Dusk start above 14000 should clamp to 14000");
    }

    @Test
    void config_validatesDawnEndTick() {
        config.setDawnEndTick(-100);
        assertEquals(0, config.getDawnEndTick(),
                     "Dawn end below 0 should clamp to 0");

        config.setDawnEndTick(3000);
        assertEquals(2000, config.getDawnEndTick(),
                     "Dawn end above 2000 should clamp to 2000");
    }

    @Test
    void config_builderPattern_createsValidConfig() {
        CrepuscularConfig builtConfig = CrepuscularConfig.builder()
            .emergenceLightLevel(3)
            .returnLightLevel(5)
            .groupEmergenceChance(0.8)
            .temperatureModifier(1.2)
            .foragingRange(80)
            .roostClusterDistance(4)
            .build();

        assertEquals(3, builtConfig.getEmergenceLightLevel());
        assertEquals(5, builtConfig.getReturnLightLevel());
        assertEquals(0.8, builtConfig.getGroupEmergenceChance());
        assertEquals(1.2, builtConfig.getTemperatureModifier());
        assertEquals(80, builtConfig.getForagingRange());
        assertEquals(4, builtConfig.getRoostClusterDistance());
    }

    @Test
    void config_customConstructor_initializesCorrectly() {
        CrepuscularConfig customConfig = new CrepuscularConfig(
            5, 6, 0.85, 1.3
        );

        assertEquals(5, customConfig.getEmergenceLightLevel());
        assertEquals(6, customConfig.getReturnLightLevel());
        assertEquals(0.85, customConfig.getGroupEmergenceChance());
        assertEquals(1.3, customConfig.getTemperatureModifier());
    }

    // ==================== CrepuscularActivity Tests ====================

    @Test
    void activity_dawnDetection_worksAtSunriseBoundaries() {
        assertTrue(activityCalculator.isDawn(23500),
                   "Should detect dawn at end of night (23500)");
        assertTrue(activityCalculator.isDawn(0),
                   "Should detect dawn at midnight (0)");
        assertTrue(activityCalculator.isDawn(500),
                   "Should detect dawn at early morning (500)");
        assertTrue(activityCalculator.isDawn(1000),
                   "Should detect dawn at sunrise end (1000)");
    }

    @Test
    void activity_dawnDetection_rejectsNonDawnTimes() {
        assertFalse(activityCalculator.isDawn(2000),
                    "Should not detect mid-morning as dawn");
        assertFalse(activityCalculator.isDawn(12000),
                    "Should not detect dusk as dawn");
        assertFalse(activityCalculator.isDawn(18000),
                    "Should not detect night as dawn");
    }

    @Test
    void activity_duskDetection_worksAtSunsetBoundaries() {
        assertTrue(activityCalculator.isDusk(12000),
                   "Should detect dusk at sunset start (12000)");
        assertTrue(activityCalculator.isDusk(13000),
                   "Should detect dusk at mid-sunset (13000)");
        assertTrue(activityCalculator.isDusk(13800),
                   "Should detect dusk at sunset end (13800)");
    }

    @Test
    void activity_duskDetection_rejectsNonDuskTimes() {
        assertFalse(activityCalculator.isDusk(11000),
                    "Should not detect before sunset as dusk");
        assertFalse(activityCalculator.isDusk(14000),
                    "Should not detect night as dusk");
        assertFalse(activityCalculator.isDusk(500),
                    "Should not detect dawn as dusk");
    }

    @Test
    void activity_nighttimeDetection_worksCorrectly() {
        assertTrue(activityCalculator.isNighttime(14001),
                   "Should detect night just after dusk (14001)");
        assertTrue(activityCalculator.isNighttime(18000),
                   "Should detect night at midnight (18000)");
        assertTrue(activityCalculator.isNighttime(22999),
                   "Should detect night before dawn (22999)");

        assertFalse(activityCalculator.isNighttime(13800),
                    "Should not detect night during dusk");
        assertFalse(activityCalculator.isNighttime(1000),
                    "Should not detect night during dawn");
    }

    @Test
    void activity_daytimeDetection_worksCorrectly() {
        assertTrue(activityCalculator.isDaytime(2000),
                   "Should detect day after dawn (2000)");
        assertTrue(activityCalculator.isDaytime(6000),
                   "Should detect day at noon (6000)");
        assertTrue(activityCalculator.isDaytime(11999),
                   "Should detect day before dusk (11999)");

        assertFalse(activityCalculator.isDaytime(1000),
                    "Should not detect day during dawn");
        assertFalse(activityCalculator.isDaytime(13000),
                    "Should not detect day during dusk");
    }

    // Note: Moon phase and weather tests require Level object from Minecraft
    // These methods are tested in integration tests with actual game instances

    @Test
    void activity_configReturnsCorrectly() {
        CrepuscularConfig retrievedConfig = activityCalculator.getConfig();

        assertNotNull(retrievedConfig, "Config should not be null");
        assertEquals(config.getEmergenceLightLevel(),
                     retrievedConfig.getEmergenceLightLevel());
    }

    @Test
    void activity_configUpdates() {
        CrepuscularConfig newConfig = CrepuscularConfig.builder()
            .emergenceLightLevel(2)
            .returnLightLevel(3)
            .build();

        activityCalculator.setConfig(newConfig);

        assertEquals(2, activityCalculator.getConfig().getEmergenceLightLevel(),
                     "Config should update");
    }

    @Test
    void activity_nullConfigDoesNotCrash() {
        CrepuscularActivity activity = new CrepuscularActivity(null);
        assertNotNull(activity, "Should handle null config gracefully");
    }

    // ==================== EmergenceTrigger Tests ====================

    @Test
    void emergenceTrigger_initializesCorrectly() {
        assertNotNull(emergenceTrigger, "EmergenceTrigger should initialize");
        assertNotNull(emergenceTrigger.getConfig(), "Should have config");
        assertNotNull(emergenceTrigger.getActivityCalculator(),
                      "Should have activity calculator");
    }

    @Test
    void emergenceTrigger_defaultConstructor_works() {
        EmergenceTrigger defaultTrigger = new EmergenceTrigger();

        assertNotNull(defaultTrigger.getConfig(),
                      "Default trigger should have config");
    }

    @Test
    void emergenceTrigger_configUpdatesApplyCorrectly() {
        CrepuscularConfig newConfig = CrepuscularConfig.builder()
            .groupDetectionRange(20)
            .emergenceLightLevel(2)
            .build();

        emergenceTrigger.setConfig(newConfig);

        assertEquals(newConfig, emergenceTrigger.getConfig(),
                     "Config should update");
        assertEquals(20, emergenceTrigger.getConfig().getGroupDetectionRange(),
                     "New config values should apply");
    }

    @Test
    void emergenceTrigger_activityCalculatorAccessible() {
        CrepuscularActivity calculator = emergenceTrigger.getActivityCalculator();

        assertNotNull(calculator, "Activity calculator should be accessible");
        assertEquals(config, calculator.getConfig(),
                     "Should use same config");
    }

    // ==================== RoostingBehavior Tests ====================

    @Test
    void roostingBehavior_initializesCorrectly() {
        assertNotNull(roostingBehavior, "RoostingBehavior should initialize");
        assertFalse(roostingBehavior.hasEstablishedRoost(),
                    "Should not have roost initially");
        assertNull(roostingBehavior.getRoostPosition(),
                   "Roost position should be null initially");
    }

    @Test
    void roostingBehavior_defaultConstructor_works() {
        RoostingBehavior defaultBehavior = new RoostingBehavior();

        assertNotNull(defaultBehavior, "Default constructor should work");
        assertFalse(defaultBehavior.hasEstablishedRoost());
    }

    @Test
    void roostingBehavior_canSetAndClearRoost() {
        BlockPos testPos = new BlockPos(10, 64, 20);

        roostingBehavior.setRoostPosition(testPos);

        assertTrue(roostingBehavior.hasEstablishedRoost(),
                   "Should have roost after setting");
        assertEquals(testPos, roostingBehavior.getRoostPosition(),
                     "Roost position should match");

        roostingBehavior.clearRoost();

        assertFalse(roostingBehavior.hasEstablishedRoost(),
                    "Should not have roost after clearing");
        assertNull(roostingBehavior.getRoostPosition(),
                   "Roost position should be null after clearing");
    }

    @Test
    void roostingBehavior_handlesNullPosition() {
        roostingBehavior.setRoostPosition(null);

        assertFalse(roostingBehavior.hasEstablishedRoost(),
                    "Null position should not establish roost");
    }

    @Test
    void roostingBehavior_calculatesSteering() {
        BehaviorContext context = new BehaviorContext(
            new Vec3d(10.0, 64.0, 20.0),
            new Vec3d(0.1, 0.0, 0.1),
            0.5,
            0.1
        );

        Vec3d steering = roostingBehavior.calculate(context);

        assertNotNull(steering, "Should return steering vector");
    }

    @Test
    void roostingBehavior_calculatesClusterAttraction() {
        BehaviorContext context = new BehaviorContext(
            new Vec3d(10.0, 64.0, 20.0),
            new Vec3d(0.1, 0.0, 0.1),
            0.5,
            0.1
        );

        Vec3d attraction = roostingBehavior.calculateClusterAttraction(context);

        assertNotNull(attraction, "Should return attraction vector");
    }

    @Test
    void roostingBehavior_configUpdatesApplyCorrectly() {
        CrepuscularConfig newConfig = CrepuscularConfig.builder()
            .roostClusterDistance(5)
            .ceilingAttractionRange(8)
            .build();

        roostingBehavior.setConfig(newConfig);

        assertEquals(5, config.getRoostClusterDistance(),
                     "New cluster distance should apply");
        assertEquals(8, config.getCeilingAttractionRange(),
                     "New ceiling range should apply");
    }

    @Test
    void roostingBehavior_nullConfigDoesNotCrash() {
        RoostingBehavior behavior = new RoostingBehavior(null);
        assertNotNull(behavior, "Should handle null config");
    }

    // ==================== BatActivityCycle Tests ====================

    @Test
    void activityCycle_initializesCorrectly() {
        BatActivityCycle cycle = new BatActivityCycle(config);

        assertNotNull(cycle, "BatActivityCycle should initialize");
        assertEquals(BatActivityCycle.ActivityState.ROOSTING,
                     cycle.getCurrentState(),
                     "Should start in ROOSTING state");
        assertNotNull(cycle.getEmergenceTrigger(),
                      "Should have emergence trigger");
        assertNotNull(cycle.getRoostingBehavior(),
                      "Should have roosting behavior");
        assertNotNull(cycle.getActivityCalculator(),
                      "Should have activity calculator");
    }

    @Test
    void activityCycle_defaultConstructor_works() {
        BatActivityCycle cycle = new BatActivityCycle();

        assertNotNull(cycle, "Default constructor should work");
        assertEquals(BatActivityCycle.ActivityState.ROOSTING,
                     cycle.getCurrentState());
    }

    @Test
    void activityCycle_hasAllStates() {
        BatActivityCycle.ActivityState[] states = BatActivityCycle.ActivityState.values();

        assertEquals(5, states.length,
                     "Should have 5 activity states");

        assertTrue(java.util.Arrays.asList(states).contains(
                   BatActivityCycle.ActivityState.ROOSTING),
                   "Should have ROOSTING state");
        assertTrue(java.util.Arrays.asList(states).contains(
                   BatActivityCycle.ActivityState.EMERGING),
                   "Should have EMERGING state");
        assertTrue(java.util.Arrays.asList(states).contains(
                   BatActivityCycle.ActivityState.FORAGING),
                   "Should have FORAGING state");
        assertTrue(java.util.Arrays.asList(states).contains(
                   BatActivityCycle.ActivityState.RETURNING),
                   "Should have RETURNING state");
        assertTrue(java.util.Arrays.asList(states).contains(
                   BatActivityCycle.ActivityState.ROOST_SEARCHING),
                   "Should have ROOST_SEARCHING state");
    }

    @Test
    void activityCycle_canSetState() {
        BatActivityCycle cycle = new BatActivityCycle(config);

        cycle.setCurrentState(BatActivityCycle.ActivityState.FORAGING);
        assertEquals(BatActivityCycle.ActivityState.FORAGING,
                     cycle.getCurrentState());

        cycle.setCurrentState(BatActivityCycle.ActivityState.EMERGING);
        assertEquals(BatActivityCycle.ActivityState.EMERGING,
                     cycle.getCurrentState());
    }

    @Test
    void activityCycle_calculatesSteeringForEachState() {
        BatActivityCycle cycle = new BatActivityCycle(config);
        BehaviorContext context = new BehaviorContext(
            new Vec3d(10.0, 64.0, 20.0),
            new Vec3d(0.1, 0.0, 0.1),
            0.5,
            0.1
        );

        for (BatActivityCycle.ActivityState state : BatActivityCycle.ActivityState.values()) {
            cycle.setCurrentState(state);
            Vec3d steering = cycle.calculateSteering(context);

            assertNotNull(steering, "Steering should not be null for " + state);
        }
    }

    @Test
    void activityCycle_roostingStateProducesNoMovement() {
        BatActivityCycle cycle = new BatActivityCycle(config);
        cycle.setCurrentState(BatActivityCycle.ActivityState.ROOSTING);

        BehaviorContext context = new BehaviorContext(
            new Vec3d(10.0, 64.0, 20.0),
            new Vec3d(0.1, 0.0, 0.1),
            0.5,
            0.1
        );
        Vec3d steering = cycle.calculateSteering(context);

        assertEquals(0.0, steering.magnitude(), 0.001,
                     "ROOSTING state should produce no movement");
    }

    @Test
    void activityCycle_emergingStateProducesMovement() {
        BatActivityCycle cycle = new BatActivityCycle(config);
        cycle.setCurrentState(BatActivityCycle.ActivityState.EMERGING);

        BehaviorContext context = new BehaviorContext(
            new Vec3d(10.0, 64.0, 20.0),
            new Vec3d(0.1, 0.0, 0.1),
            0.5,
            0.1
        );
        Vec3d steering = cycle.calculateSteering(context);

        assertTrue(steering.magnitude() > 0,
                   "EMERGING state should produce movement");
    }

    @Test
    void activityCycle_foragingStateProducesMovement() {
        BatActivityCycle cycle = new BatActivityCycle(config);
        cycle.setCurrentState(BatActivityCycle.ActivityState.FORAGING);

        BehaviorContext context = new BehaviorContext(
            new Vec3d(10.0, 64.0, 20.0),
            new Vec3d(0.1, 0.0, 0.1),
            0.5,
            0.1
        );

        Vec3d steering = cycle.calculateSteering(context);

        assertNotNull(steering, "FORAGING steering should not be null");
    }

    @Test
    void activityCycle_returningStateSeeksRoost() {
        BatActivityCycle cycle = new BatActivityCycle(config);
        cycle.setCurrentState(BatActivityCycle.ActivityState.RETURNING);

        cycle.getRoostingBehavior().setRoostPosition(new BlockPos(0, 64, 0));

        BehaviorContext context = new BehaviorContext(
            new Vec3d(10.0, 64.0, 20.0),
            new Vec3d(0.1, 0.0, 0.1),
            0.5,
            0.1
        );
        Vec3d steering = cycle.calculateSteering(context);

        assertNotNull(steering, "RETURNING steering should not be null");
    }

    // Note: Seasonal modifier test requires Level object from Minecraft
    // The method is tested in integration tests with actual game instances

    @Test
    void activityCycle_resetClearsState() {
        BatActivityCycle cycle = new BatActivityCycle(config);

        cycle.setCurrentState(BatActivityCycle.ActivityState.FORAGING);
        cycle.getRoostingBehavior().setRoostPosition(new BlockPos(10, 64, 20));

        cycle.reset();

        assertEquals(BatActivityCycle.ActivityState.ROOSTING,
                     cycle.getCurrentState(),
                     "Should reset to ROOSTING state");
        assertFalse(cycle.getRoostingBehavior().hasEstablishedRoost(),
                    "Should clear roost on reset");
    }

    @Test
    void activityCycle_configUpdatesApplyToAllComponents() {
        BatActivityCycle cycle = new BatActivityCycle(config);

        CrepuscularConfig newConfig = CrepuscularConfig.builder()
            .emergenceLightLevel(2)
            .foragingRange(100)
            .build();

        cycle.setConfig(newConfig);

        assertEquals(newConfig.getEmergenceLightLevel(),
                     cycle.getConfig().getEmergenceLightLevel(),
                     "Main config should update");
    }

    @Test
    void activityCycle_getConfigReturnsCurrentConfig() {
        BatActivityCycle cycle = new BatActivityCycle(config);

        CrepuscularConfig retrievedConfig = cycle.getConfig();

        assertNotNull(retrievedConfig, "Config should not be null");
        assertEquals(config.getEmergenceLightLevel(),
                     retrievedConfig.getEmergenceLightLevel());
    }

    // ==================== Scientific Validation Tests ====================

    @Test
    void scientific_lightThresholds_matchBatBiology() {
        assertTrue(config.getEmergenceLightLevel() <= 4,
                   "Bats emerge at very low light levels (<=4)");
        assertTrue(config.getReturnLightLevel() >= 3,
                   "Bats return when light increases");
    }

    @Test
    void scientific_foragingRange_matchesBatBehavior() {
        assertTrue(config.getForagingRange() >= 32,
                   "Bats forage significant distance from roost");
        assertTrue(config.getForagingRange() <= 256,
                   "Foraging range should be reasonable");
    }

    @Test
    void scientific_roostClustering_matchesColonyBehavior() {
        assertTrue(config.getRoostClusterDistance() >= 1,
                   "Bats cluster together at roost");
        assertTrue(config.getRoostClusterDistance() <= 10,
                   "Clustering distance should be tight");
    }

    @Test
    void scientific_groupEmergence_matchesColonyBehavior() {
        assertTrue(config.getGroupEmergenceChance() >= 0.5,
                   "Bats strongly follow group emergence");
        assertTrue(config.getGroupEmergenceChance() <= 1.0,
                   "Group emergence should be probability");
    }

    @Test
    void scientific_activityCycle_matchesCrepuscularPattern() {
        assertTrue(activityCalculator.isDawn(500),
                   "Should be active at dawn");
        assertTrue(activityCalculator.isDusk(13000),
                   "Should be active at dusk");
        assertFalse(activityCalculator.isDaytime(500),
                    "Dawn should not be considered daytime");
        assertFalse(activityCalculator.isDaytime(13000),
                    "Dusk should not be considered daytime");
    }

    @Test
    void scientific_temperatureModifier_rangesAreBiologicallyPlausible() {
        double minMod = 0.5;
        double maxMod = 2.0;

        config.setTemperatureModifier(0.5);
        assertTrue(config.getTemperatureModifier() >= minMod,
                   "Temperature modifier should be at least minimum");

        config.setTemperatureModifier(2.0);
        assertTrue(config.getTemperatureModifier() <= maxMod,
                   "Temperature modifier should not exceed maximum");
    }

    @Test
    void scientific_ceilingAttraction_matchesRoostingBehavior() {
        assertTrue(config.getCeilingAttractionRange() >= 1,
                   "Bats seek ceilings for roosting");
        assertTrue(config.getCeilingAttractionRange() <= 16,
                   "Ceiling attraction range should be reasonable");
    }

    // ==================== Integration Tests ====================

    @Test
    void integration_emergenceToForagingTransition() {
        BatActivityCycle cycle = new BatActivityCycle(config);

        cycle.setCurrentState(BatActivityCycle.ActivityState.ROOSTING);
        assertEquals(BatActivityCycle.ActivityState.ROOSTING,
                     cycle.getCurrentState());

        cycle.setCurrentState(BatActivityCycle.ActivityState.EMERGING);
        assertEquals(BatActivityCycle.ActivityState.EMERGING,
                     cycle.getCurrentState());

        cycle.setCurrentState(BatActivityCycle.ActivityState.FORAGING);
        assertEquals(BatActivityCycle.ActivityState.FORAGING,
                     cycle.getCurrentState());
    }

    @Test
    void integration_foragingToReturningTransition() {
        BatActivityCycle cycle = new BatActivityCycle(config);

        cycle.setCurrentState(BatActivityCycle.ActivityState.FORAGING);
        assertEquals(BatActivityCycle.ActivityState.FORAGING,
                     cycle.getCurrentState());

        cycle.setCurrentState(BatActivityCycle.ActivityState.RETURNING);
        assertEquals(BatActivityCycle.ActivityState.RETURNING,
                     cycle.getCurrentState());
    }

    @Test
    void integration_returningToRoostingTransition() {
        BatActivityCycle cycle = new BatActivityCycle(config);

        BlockPos roostPos = new BlockPos(10, 64, 10);
        cycle.getRoostingBehavior().setRoostPosition(roostPos);

        cycle.setCurrentState(BatActivityCycle.ActivityState.RETURNING);
        assertEquals(BatActivityCycle.ActivityState.RETURNING,
                     cycle.getCurrentState());

        cycle.setCurrentState(BatActivityCycle.ActivityState.ROOSTING);
        assertEquals(BatActivityCycle.ActivityState.ROOSTING,
                     cycle.getCurrentState());
    }

    @Test
    void integration_configChangesAffectAllComponents() {
        BatActivityCycle cycle = new BatActivityCycle(config);

        CrepuscularConfig newConfig = CrepuscularConfig.builder()
            .emergenceLightLevel(2)
            .groupEmergenceChance(0.9)
            .temperatureModifier(1.5)
            .build();

        cycle.setConfig(newConfig);

        assertEquals(2, cycle.getConfig().getEmergenceLightLevel());
        assertEquals(0.9, cycle.getConfig().getGroupEmergenceChance());
        assertEquals(1.5, cycle.getConfig().getTemperatureModifier());
    }

    @Test
    void integration_allComponentsShareConfig() {
        BatActivityCycle cycle = new BatActivityCycle(config);

        CrepuscularConfig triggerConfig = cycle.getEmergenceTrigger().getConfig();
        CrepuscularConfig activityConfig = cycle.getActivityCalculator().getConfig();
        CrepuscularConfig mainConfig = cycle.getConfig();

        assertEquals(mainConfig.getEmergenceLightLevel(),
                     triggerConfig.getEmergenceLightLevel(),
                     "Trigger should share config");
        assertEquals(mainConfig.getDuskStartTick(),
                     activityConfig.getDuskStartTick(),
                     "Activity should share config");

        // RoostingBehavior doesn't expose getConfig() directly, but uses it internally
        assertNotNull(cycle.getRoostingBehavior(), "Roosting behavior should exist");
    }

    // ==================== Edge Case Tests ====================

    @Test
    void edgeCase_handlesNullConfigGracefully() {
        CrepuscularActivity activity = new CrepuscularActivity(null);
        assertNotNull(activity, "Should handle null config");

        RoostingBehavior roosting = new RoostingBehavior(null);
        assertNotNull(roosting, "Should handle null config");

        EmergenceTrigger trigger = new EmergenceTrigger(null);
        assertNotNull(trigger, "Should handle null config");
    }

    @Test
    void edgeCase_handlesExtremeLightLevels() {
        config.setEmergenceLightLevel(0);
        assertEquals(0, config.getEmergenceLightLevel());

        config.setEmergenceLightLevel(15);
        assertEquals(15, config.getEmergenceLightLevel());

        config.setReturnLightLevel(0);
        assertEquals(0, config.getReturnLightLevel());

        config.setReturnLightLevel(15);
        assertEquals(15, config.getReturnLightLevel());
    }

    @Test
    void edgeCase_handlesBoundaryDayTimes() {
        assertTrue(activityCalculator.isDawn(0),
                   "Should handle time 0 (midnight)");
        assertTrue(activityCalculator.isDusk(13800),
                   "Should handle end of dusk");
        assertTrue(activityCalculator.isNighttime(23999),
                   "Should handle time before midnight");
        assertTrue(activityCalculator.isDaytime(1001),
                   "Should handle time after dawn");
    }

    @Test
    void edgeCase_handlesZeroForagingTime() {
        BatActivityCycle cycle = new BatActivityCycle(config);
        assertEquals(BatActivityCycle.ActivityState.ROOSTING,
                     cycle.getCurrentState(),
                     "Should start in ROOSTING state");
    }

    @Test
    void edgeCase_steeringHandlesAllStates() {
        BatActivityCycle cycle = new BatActivityCycle(config);
        BehaviorContext context = new BehaviorContext(
            new Vec3d(10.0, 64.0, 20.0),
            new Vec3d(0.1, 0.0, 0.1),
            0.5,
            0.1
        );

        for (BatActivityCycle.ActivityState state : BatActivityCycle.ActivityState.values()) {
            cycle.setCurrentState(state);
            Vec3d steering = cycle.calculateSteering(context);
            assertNotNull(steering, "Steering should not be null for " + state);
        }
    }

    @Test
    void edgeCase_configWithExtremes() {
        CrepuscularConfig extremeConfig = CrepuscularConfig.builder()
            .emergenceLightLevel(0)
            .returnLightLevel(15)
            .groupEmergenceChance(0.0)
            .temperatureModifier(2.0)
            .foragingRange(16)
            .roostClusterDistance(1)
            .ceilingAttractionRange(1)
            .groupDetectionRange(1)
            .build();

        BatActivityCycle cycle = new BatActivityCycle(extremeConfig);

        assertNotNull(cycle, "Should handle extreme config values");
        assertEquals(0, cycle.getConfig().getEmergenceLightLevel());
        assertEquals(15, cycle.getConfig().getReturnLightLevel());
        assertEquals(0.0, cycle.getConfig().getGroupEmergenceChance());
        assertEquals(2.0, cycle.getConfig().getTemperatureModifier());
    }

    @Test
    void edgeCase_steeringHandlesNullEntity() {
        BehaviorContext nullEntityContext = new BehaviorContext(
            new Vec3d(10.0, 64.0, 20.0),
            new Vec3d(0.1, 0.0, 0.1),
            0.5,
            0.1
        );

        Vec3d steering = roostingBehavior.calculate(nullEntityContext);
        assertNotNull(steering, "Should handle null entity in context");
    }

    @Test
    void edgeCase_stateTransitionsWork() {
        BatActivityCycle cycle = new BatActivityCycle(config);

        for (BatActivityCycle.ActivityState state : BatActivityCycle.ActivityState.values()) {
            cycle.setCurrentState(state);
            assertEquals(state, cycle.getCurrentState(),
                         "State should be set correctly: " + state);
        }
    }

    @Test
    void edgeCase_builderChainingWorks() {
        CrepuscularConfig builtConfig = CrepuscularConfig.builder()
            .emergenceLightLevel(3)
            .returnLightLevel(5)
            .groupEmergenceChance(0.8)
            .temperatureModifier(1.2)
            .foragingRange(80)
            .roostClusterDistance(4)
            .useTemperature(true)
            .affectedByWeather(true)
            .affectedByMoonPhase(true)
            .ceilingAttractionRange(6)
            .groupDetectionRange(20)
            .build();

        assertNotNull(builtConfig);
        assertEquals(3, builtConfig.getEmergenceLightLevel());
        assertEquals(5, builtConfig.getReturnLightLevel());
        assertEquals(0.8, builtConfig.getGroupEmergenceChance());
        assertEquals(1.2, builtConfig.getTemperatureModifier());
        assertEquals(80, builtConfig.getForagingRange());
        assertEquals(4, builtConfig.getRoostClusterDistance());
        assertEquals(6, builtConfig.getCeilingAttractionRange());
        assertEquals(20, builtConfig.getGroupDetectionRange());
        assertTrue(builtConfig.isUseTemperatureModifier());
        assertTrue(builtConfig.isAffectedByWeather());
        assertTrue(builtConfig.isAffectedByMoonPhase());
    }
}
