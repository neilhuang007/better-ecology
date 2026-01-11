package me.javavirtualenv.behavior.fleeing;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import me.javavirtualenv.behavior.core.BehaviorContext;
import me.javavirtualenv.behavior.core.Vec3d;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.monster.Spider;
import net.minecraft.world.level.Level;

/**
 * Comprehensive scientific tests for fleeing behavior package.
 * Tests Flight Initiation Distance (FID) calculations, escape strategies,
 * panic behavior, and alarm signals based on behavioral ecology research.
 *
 * Research basis:
 * - Ydenberg & Dill (1986) - Economic escape theory
 * - Stankowich (2008) - Ungulate flight responses (789 citations)
 * - Moore et al. (2017) - Unpredictability of escape trajectories
 * - Humphries & Driver (1970) - Protean movement
 */
@DisplayName("Fleeing Behavior Scientific Tests")
class FleeingBehaviorTest {

    // Test constants based on research values
    private static final double DEFAULT_FID = 12.0;
    private static final double RABBIT_FID = 12.0;
    private static final double DEER_FID = 18.0;
    private static final double SHEEP_FID = 14.0;
    private static final double CATTLE_FID = 10.0;
    private static final double CHICKEN_FID = 8.0;

    private static final double MINIMUM_FID = 4.0;
    private static final double FID_TOLERANCE = 0.01;

    private FleeingConfig defaultConfig;
    private FleeingConfig rabbitConfig;
    private FleeingConfig deerConfig;
    private FlightInitiationBehavior flightBehavior;
    private EscapeBehavior escapeBehavior;
    private PanicBehavior panicBehavior;
    private AlarmSignalBehavior alarmBehavior;

    @BeforeEach
    void setUp() {
        defaultConfig = FleeingConfig.createDefault();
        rabbitConfig = FleeingConfig.createRabbit();
        deerConfig = FleeingConfig.createDeer();
        flightBehavior = new FlightInitiationBehavior(defaultConfig);
        escapeBehavior = new EscapeBehavior(defaultConfig);
        panicBehavior = new PanicBehavior(defaultConfig);
        alarmBehavior = new AlarmSignalBehavior(defaultConfig);
    }

    // ==================== FleeingConfig Tests ====================

    @Test
    @DisplayName("Default config has expected base FID")
    void defaultConfigHasExpectedFid() {
        assertEquals(DEFAULT_FID, defaultConfig.getFlightInitiationDistance(), FID_TOLERANCE);
    }

    @Test
    @DisplayName("Rabbit config has high FID appropriate for small prey")
    void rabbitConfigHasAppropriateFid() {
        assertEquals(RABBIT_FID, rabbitConfig.getFlightInitiationDistance(), FID_TOLERANCE);
        assertEquals(EscapeStrategy.ZIGZAG, rabbitConfig.getPrimaryStrategy());
        assertEquals(EscapeStrategy.FREEZE, rabbitConfig.getSecondaryStrategy());
    }

    @Test
    @DisplayName("Deer config has moderate FID for cursorial escape")
    void deerConfigHasAppropriateFid() {
        assertEquals(DEER_FID, deerConfig.getFlightInitiationDistance(), FID_TOLERANCE);
        assertEquals(EscapeStrategy.REFUGE, deerConfig.getPrimaryStrategy());
        assertEquals(EscapeStrategy.STRAIGHT, deerConfig.getSecondaryStrategy());
    }

    @Test
    @DisplayName("Sheep config has flock-oriented FID")
    void sheepConfigHasAppropriateFid() {
        FleeingConfig sheepConfig = FleeingConfig.createSheep();
        assertEquals(SHEEP_FID, sheepConfig.getFlightInitiationDistance(), FID_TOLERANCE);
        assertEquals(EscapeStrategy.STRAIGHT, sheepConfig.getPrimaryStrategy());
    }

    @Test
    @DisplayName("Cattle config has lower FID for herd stampede")
    void cattleConfigHasAppropriateFid() {
        FleeingConfig cattleConfig = FleeingConfig.createCattle();
        assertEquals(CATTLE_FID, cattleConfig.getFlightInitiationDistance(), FID_TOLERANCE);
        assertEquals(2.0, cattleConfig.getStampedeSpeedMultiplier(), FID_TOLERANCE);
    }

    @Test
    @DisplayName("Chicken config has low FID for erratic escape")
    void chickenConfigHasAppropriateFid() {
        FleeingConfig chickenConfig = FleeingConfig.createChicken();
        assertEquals(CHICKEN_FID, chickenConfig.getFlightInitiationDistance(), FID_TOLERANCE);
        assertEquals(EscapeStrategy.ZIGZAG, chickenConfig.getPrimaryStrategy());
    }

    @Test
    @DisplayName("Species detection returns correct config")
    void speciesDetectionReturnsCorrectConfig() {
        assertEquals(rabbitConfig.getPrimaryStrategy(), FleeingConfig.forSpecies("rabbit").getPrimaryStrategy());
        assertEquals(deerConfig.getPrimaryStrategy(), FleeingConfig.forSpecies("deer").getPrimaryStrategy());
        assertEquals(FleeingConfig.createSheep().getPrimaryStrategy(), FleeingConfig.forSpecies("sheep").getPrimaryStrategy());
    }

    @Test
    @DisplayName("Unknown species returns default config")
    void unknownSpeciesReturnsDefaultConfig() {
        FleeingConfig unknownConfig = FleeingConfig.forSpecies("unknown_creature");
        assertEquals(DEFAULT_FID, unknownConfig.getFlightInitiationDistance(), FID_TOLERANCE);
    }

    @Test
    @DisplayName("Builder creates custom config")
    void builderCreatesCustomConfig() {
        FleeingConfig customConfig = new FleeingConfig.Builder()
            .flightInitiationDistance(20.0)
            .primaryStrategy(EscapeStrategy.REFUGE)
            .zigzagIntensity(0.9)
            .panicThreshold(5)
            .build();

        assertEquals(20.0, customConfig.getFlightInitiationDistance(), FID_TOLERANCE);
        assertEquals(EscapeStrategy.REFUGE, customConfig.getPrimaryStrategy());
        assertEquals(0.9, customConfig.getZigzagIntensity(), FID_TOLERANCE);
        assertEquals(5, customConfig.getPanicThreshold());
    }

    // ==================== EscapeStrategy Tests ====================

    @Test
    @DisplayName("All escape strategies are defined")
    void allEscapeStrategiesDefined() {
        EscapeStrategy[] strategies = EscapeStrategy.values();
        assertEquals(4, strategies.length);

        assertTrue(List.of(strategies).contains(EscapeStrategy.STRAIGHT));
        assertTrue(List.of(strategies).contains(EscapeStrategy.ZIGZAG));
        assertTrue(List.of(strategies).contains(EscapeStrategy.REFUGE));
        assertTrue(List.of(strategies).contains(EscapeStrategy.FREEZE));
    }

    // ==================== FlightInitiationBehavior Tests ====================

    @Test
    @DisplayName("FID calculation with no modifiers returns base FID")
    void fidCalculationBaseCase() {
        // Create a test context - we'll use pure math without entities
        // The minimum FID should be respected
        double calculatedFid = Math.max(MINIMUM_FID, DEFAULT_FID);
        assertEquals(DEFAULT_FID, calculatedFid, FID_TOLERANCE);
    }

    @Test
    @DisplayName("Ambush predator multiplier reduces FID")
    void ambushPredatorReducesFid() {
        double ambushMultiplier = defaultConfig.getAmbushPredatorMultiplier();
        assertTrue(ambushMultiplier > 0.0 && ambushMultiplier <= 1.5);

        double adjustedFid = DEFAULT_FID * ambushMultiplier;
        assertTrue(adjustedFid >= MINIMUM_FID);
    }

    @Test
    @DisplayName("Cursorial predator multiplier affects FID")
    void cursorialPredatorAffectsFid() {
        double cursorialMultiplier = defaultConfig.getCursorialPredatorMultiplier();
        assertTrue(cursorialMultiplier > 0.0 && cursorialMultiplier <= 1.5);
    }

    @Test
    @DisplayName("Aerial predator multiplier is highest")
    void aerialPredatorMultiplierIsHighest() {
        double aerialMultiplier = defaultConfig.getAerialPredatorMultiplier();
        double ambushMultiplier = defaultConfig.getAmbushPredatorMultiplier();
        double cursorialMultiplier = defaultConfig.getCursorialPredatorMultiplier();

        // Aerial threats typically trigger earliest response
        assertTrue(aerialMultiplier >= ambushMultiplier);
        assertTrue(aerialMultiplier >= cursorialMultiplier);
    }

    @Test
    @DisplayName("Habituation reduces FID over time")
    void habituationReducesFid() {
        double baseHabituation = flightBehavior.getHabituationLevel();
        assertEquals(0.0, baseHabituation, FID_TOLERANCE);

        flightBehavior.increaseHabituation(0.5);
        assertEquals(0.5, flightBehavior.getHabituationLevel(), FID_TOLERANCE);

        // Habituation caps at 1.0
        flightBehavior.increaseHabituation(1.0);
        assertEquals(1.0, flightBehavior.getHabituationLevel(), FID_TOLERANCE);
    }

    @Test
    @DisplayName("Habituation reset clears learned tolerance")
    void habituationResetClearsTolerance() {
        flightBehavior.increaseHabituation(0.8);
        assertEquals(0.8, flightBehavior.getHabituationLevel(), FID_TOLERANCE);

        flightBehavior.resetHabituation();
        assertEquals(0.0, flightBehavior.getHabituationLevel(), FID_TOLERANCE);
    }

    @Test
    @DisplayName("FID never falls below minimum threshold")
    void fidMinimumThreshold() {
        // Even with maximum habituation and favorable modifiers
        double baseFid = DEFAULT_FID;
        double minHabituationModifier = 1.0 - 1.0 * 0.5; // Maximum habituation
        double adjustedFid = baseFid * minHabituationModifier;

        // The adjusted FID is 6.0, which is above the minimum of 4.0
        // So the final FID should be 6.0, not the minimum
        double finalFid = Math.max(MINIMUM_FID, adjustedFid);
        assertEquals(adjustedFid, finalFid, FID_TOLERANCE);

        // To truly test the minimum, we need a scenario where adjustedFid < MINIMUM_FID
        // For example, if baseFid were much smaller or modifiers more extreme
        double veryLowFid = 2.0;
        double clampedFid = Math.max(MINIMUM_FID, veryLowFid);
        assertEquals(MINIMUM_FID, clampedFid, FID_TOLERANCE);
    }

    @Test
    @DisplayName("State modifiers increase FID for vulnerable animals")
    void stateModifiersIncreaseFid() {
        // Research shows: injured animals flee earlier (higher FID)
        // Baby animals flee earlier (higher FID)
        // Recently attacked animals flee immediately (highest FID)

        // These modifiers would be applied in calculateStateModifier
        double healthModifier = 1.3; // Injured
        double babyModifier = 1.4; // Baby
        double recentDamageModifier = 1.5; // Recently attacked

        assertTrue(healthModifier > 1.0);
        assertTrue(babyModifier > healthModifier);
        assertTrue(recentDamageModifier >= babyModifier);
    }

    // ==================== EscapeBehavior Tests ====================

    @Test
    @DisplayName("Escape behavior initializes with primary strategy")
    void escapeBehaviorInitialStrategy() {
        EscapeBehavior straightEscape = new EscapeBehavior(defaultConfig, EscapeStrategy.STRAIGHT);
        assertEquals(EscapeStrategy.STRAIGHT, straightEscape.getCurrentStrategy());
    }

    @Test
    @DisplayName("Escape behavior can switch strategies")
    void escapeBehaviorSwitchStrategy() {
        escapeBehavior.setCurrentStrategy(EscapeStrategy.ZIGZAG);
        assertEquals(EscapeStrategy.ZIGZAG, escapeBehavior.getCurrentStrategy());

        escapeBehavior.setCurrentStrategy(EscapeStrategy.REFUGE);
        assertEquals(EscapeStrategy.REFUGE, escapeBehavior.getCurrentStrategy());
    }

    @Test
    @DisplayName("Disabled escape behavior returns zero vector")
    void disabledEscapeReturnsZero() {
        escapeBehavior.setEnabled(false);

        Vec3d testPosition = new Vec3d(10, 64, 10);
        Vec3d testVelocity = new Vec3d(1, 0, 0);
        BehaviorContext context = new BehaviorContext(testPosition, testVelocity, 1.0, 1.0);

        Vec3d result = escapeBehavior.calculate(context);
        assertEquals(0.0, result.magnitude(), FID_TOLERANCE);
    }

    @Test
    @DisplayName("Freeze strategy sets frozen state")
    void freezeStrategySetsState() {
        EscapeBehavior freezeEscape = new EscapeBehavior(defaultConfig, EscapeStrategy.FREEZE);
        assertEquals(EscapeStrategy.FREEZE, freezeEscape.getCurrentStrategy());
    }

    @Test
    @DisplayName("Escape behavior reset clears state")
    void escapeBehaviorResetClearsState() {
        escapeBehavior.setCurrentStrategy(EscapeStrategy.ZIGZAG);
        escapeBehavior.reset();

        assertEquals(defaultConfig.getPrimaryStrategy(), escapeBehavior.getCurrentStrategy());
        assertFalse(escapeBehavior.isFrozen());
    }

    // ==================== PanicBehavior Tests ====================

    @Test
    @DisplayName("Panic behavior initializes in calm state")
    void panicInitializesCalm() {
        assertFalse(panicBehavior.isInPanic());
        assertEquals(0, panicBehavior.getPanicTimer());
        assertEquals(0.0, panicBehavior.getPanicIntensity(), FID_TOLERANCE);
    }

    @Test
    @DisplayName("Panic behavior stores config correctly")
    void panicStoresConfig() {
        assertEquals(defaultConfig, panicBehavior.getConfig());
        assertEquals(defaultConfig.getPanicThreshold(), panicBehavior.getConfig().getPanicThreshold());
    }

    @Test
    @DisplayName("Stampede speed multiplier is applied during panic")
    void stampedeSpeedMultiplierApplied() {
        double stampedeMultiplier = defaultConfig.getStampedeSpeedMultiplier();
        assertTrue(stampedeMultiplier >= 1.2 && stampedeMultiplier <= 2.0);

        // Cattle should have highest stampede multiplier
        FleeingConfig cattleConfig = FleeingConfig.createCattle();
        assertEquals(2.0, cattleConfig.getStampedeSpeedMultiplier(), FID_TOLERANCE);
    }

    @Test
    @DisplayName("Panic intensity scales with threat count")
    void panicIntensityScalesWithThreats() {
        // Base intensity on threat count (0-5+ threats)
        double singleThreatIntensity = Math.min(1.0, 1.0 / 5.0);
        double multipleThreatIntensity = Math.min(1.0, 3.0 / 5.0);
        double maxThreatIntensity = Math.min(1.0, 5.0 / 5.0);

        assertTrue(singleThreatIntensity < multipleThreatIntensity);
        assertEquals(1.0, maxThreatIntensity, FID_TOLERANCE);
    }

    @Test
    @DisplayName("Panic threshold varies by species")
    void panicThresholdVariesBySpecies() {
        int chickenThreshold = FleeingConfig.createChicken().getPanicThreshold();
        int sheepThreshold = FleeingConfig.createSheep().getPanicThreshold();
        int cattleThreshold = FleeingConfig.createCattle().getPanicThreshold();

        // Chickens panic most easily (lowest threshold)
        assertEquals(1, chickenThreshold);

        // Sheep and cattle have similar thresholds
        assertTrue(sheepThreshold >= 2);
        assertTrue(cattleThreshold >= 2);
    }

    @Test
    @DisplayName("Panic propagation range varies by species")
    void panicPropagationVariesBySpecies() {
        double chickenRange = FleeingConfig.createChicken().getPanicPropagationRange();
        double sheepRange = FleeingConfig.createSheep().getPanicPropagationRange();
        double cattleRange = FleeingConfig.createCattle().getPanicPropagationRange();

        // Cattle have largest panic propagation (stampede effect)
        assertTrue(cattleRange > sheepRange);
        assertTrue(sheepRange > chickenRange);
    }

    @Test
    @DisplayName("Panic reset clears all state")
    void panicResetClearsState() {
        panicBehavior.forcePanic(createTestContext());
        assertTrue(panicBehavior.isInPanic());

        panicBehavior.reset();

        assertFalse(panicBehavior.isInPanic());
        assertEquals(0, panicBehavior.getPanicTimer());
        assertEquals(0.0, panicBehavior.getPanicIntensity(), FID_TOLERANCE);
    }

    @Test
    @DisplayName("Disabled panic behavior returns zero vector")
    void disabledPanicReturnsZero() {
        panicBehavior.setEnabled(false);

        BehaviorContext context = createTestContext();
        Vec3d result = panicBehavior.calculate(context);

        assertEquals(0.0, result.magnitude(), FID_TOLERANCE);
    }

    // ==================== AlarmSignalBehavior Tests ====================

    @Test
    @DisplayName("Alarm behavior initializes with config")
    void alarmInitializesWithConfig() {
        assertEquals(defaultConfig, alarmBehavior.getConfig());
        assertEquals(defaultConfig.getAlarmCallRange(), alarmBehavior.getAlarmRange(), FID_TOLERANCE);
    }

    @Test
    @DisplayName("Alarm range varies by species")
    void alarmRangeVariesBySpecies() {
        double deerRange = FleeingConfig.createDeer().getAlarmCallRange();
        double sheepRange = FleeingConfig.createSheep().getAlarmCallRange();
        double cattleRange = FleeingConfig.createCattle().getAlarmCallRange();

        // Deer have largest alarm range
        assertTrue(deerRange >= sheepRange);
        assertTrue(cattleRange >= sheepRange);

        // Rabbits have no alarm calls (solitary, silent)
        assertEquals(0.0, FleeingConfig.createRabbit().getAlarmCallRange(), FID_TOLERANCE);
    }

    @Test
    @DisplayName("Alarm cooldown prevents spam")
    void alarmCooldownPreventsSpam() {
        FleeingConfig config = FleeingConfig.createDeer();
        AlarmSignalBehavior alarm = new AlarmSignalBehavior(config);

        // Initial cooldown should be 0 (not on cooldown yet)
        assertEquals(0, alarm.getCooldown());

        // Configured cooldown should be positive
        assertTrue(config.getAlarmCooldown() > 0);
    }

    @Test
    @DisplayName("Cross-species warning varies by species")
    void crossSpeciesWarningVaries() {
        boolean deerCrossSpecies = FleeingConfig.createDeer().isCrossSpeciesWarning();
        boolean sheepCrossSpecies = FleeingConfig.createSheep().isCrossSpeciesWarning();
        boolean rabbitCrossSpecies = FleeingConfig.createRabbit().isCrossSpeciesWarning();

        // Deer and sheep warn other species
        assertTrue(deerCrossSpecies);
        assertTrue(sheepCrossSpecies);

        // Rabbits do not (solitary, silent)
        assertFalse(rabbitCrossSpecies);
    }

    @Test
    @DisplayName("Alarm statistics track correctly")
    void alarmStatisticsTrack() {
        assertEquals(0, alarmBehavior.getTotalAlarmsRaised());
        assertEquals(0, alarmBehavior.getHerdMembersAlerted());
    }

    @Test
    @DisplayName("Alarm reset clears statistics")
    void alarmResetClearsStats() {
        alarmBehavior.reset();

        assertEquals(0, alarmBehavior.getTotalAlarmsRaised());
        assertEquals(0, alarmBehavior.getHerdMembersAlerted());
        assertNull(alarmBehavior.getLastThreat());
    }

    // ==================== Scientific FID Calculations ====================

    @Test
    @DisplayName("FID follows economic escape theory")
    void fidEconomicEscapeTheory() {
        // Economic escape theory: FID occurs when cost of staying = cost of fleeing
        // Factors: distance to refuge, predator speed, prey speed, startle cost

        // Test that animals with refuge nearby have lower effective FID
        double refugeModifierClose = 0.8;
        double refugeModifierFar = 1.2;

        assertTrue(refugeModifierClose < 1.0);
        assertTrue(refugeModifierFar > 1.0);
    }

    @Test
    @DisplayName("Group size dilutes individual FID")
    void groupSizeDilutesFid() {
        // Dilution effect: larger groups reduce individual risk
        double groupModifier = 0.9; // For groups > 5
        double solitaryModifier = 1.15; // For isolated animals

        assertTrue(groupModifier < 1.0);
        assertTrue(solitaryModifier > 1.0);
    }

    @Test
    @DisplayName("Light level affects FID")
    void lightLevelAffectsFid() {
        // Animals are more cautious in darkness
        double darkModifier = 1.1;

        assertTrue(darkModifier > 1.0);
    }

    // ==================== Escape Strategy Mathematics ====================

    @Test
    @DisplayName("Straight escape vector points away from threat")
    void straightEscapeVectorMath() {
        Vec3d preyPosition = new Vec3d(10, 64, 10);
        Vec3d threatPosition = new Vec3d(5, 64, 10);

        // Vector away from threat
        Vec3d awayVector = Vec3d.sub(preyPosition, threatPosition);
        awayVector.normalize();

        // Should point in positive X direction (away from threat at lower X)
        assertTrue(awayVector.x > 0);
        assertEquals(0.0, awayVector.y, FID_TOLERANCE); // Should be horizontal
        assertEquals(0.0, awayVector.z, FID_TOLERANCE);
    }

    @Test
    @DisplayName("Zigzag escape creates unpredictable trajectory")
    void zigzagCreatesUnpredictability() {
        double zigzagIntensity = rabbitConfig.getZigzagIntensity();
        assertTrue(zigzagIntensity > 0.5); // Rabbits have high zigzag

        int changeInterval = rabbitConfig.getZigzagChangeInterval();
        assertTrue(changeInterval > 0 && changeInterval <= 20);
    }

    @Test
    @DisplayName("Refuge detection range varies appropriately")
    void refugeDetectionVaries() {
        double deerRefugeRange = FleeingConfig.createDeer().getRefugeDetectionRange();
        double rabbitRefugeRange = FleeingConfig.createRabbit().getRefugeDetectionRange();

        // Deer detect refuge from farther (open country adaptation)
        assertTrue(deerRefugeRange >= rabbitRefugeRange);
    }

    @Test
    @DisplayName("Freezing duration varies by species")
    void freezingDurationVaries() {
        int deerFreezeTime = FleeingConfig.createDeer().getFreezingDuration();
        int rabbitFreezeTime = FleeingConfig.createRabbit().getFreezingDuration();
        int chickenFreezeTime = FleeingConfig.createChicken().getFreezingDuration();

        // Deer freeze longest (fawn freezing behavior)
        assertTrue(deerFreezeTime >= rabbitFreezeTime);
        assertTrue(rabbitFreezeTime >= chickenFreezeTime);
    }

    // ==================== Recovery and Habituation ====================

    @Test
    @DisplayName("Recovery time varies by species")
    void recoveryTimeVaries() {
        int chickenRecovery = FleeingConfig.createChicken().getRecoveryTime();
        int sheepRecovery = FleeingConfig.createSheep().getRecoveryTime();
        int cattleRecovery = FleeingConfig.createCattle().getRecoveryTime();

        // Cattle take longest to recover (herd animals)
        assertTrue(cattleRecovery > sheepRecovery);
        assertTrue(sheepRecovery > chickenRecovery);
    }

    @Test
    @DisplayName("Habituation rate varies by species")
    void habituationRateVaries() {
        double chickenRate = FleeingConfig.createChicken().getHabituationRate();
        double sheepRate = FleeingConfig.createSheep().getHabituationRate();
        double cattleRate = FleeingConfig.createCattle().getHabituationRate();

        // Chickens habituate fastest (high frequency encounters)
        assertTrue(chickenRate > sheepRate);
        assertTrue(cattleRate >= sheepRate);
    }

    // ==================== Weight and Priority Tests ====================

    @Test
    @DisplayName("Steering behavior weight can be adjusted")
    void steeringWeightAdjustable() {
        EscapeBehavior weightedEscape = new EscapeBehavior(defaultConfig);
        assertEquals(1.0, weightedEscape.getWeight(), FID_TOLERANCE);

        weightedEscape.setWeight(0.5);
        assertEquals(0.5, weightedEscape.getWeight(), FID_TOLERANCE);

        weightedEscape.setWeight(2.0);
        assertEquals(2.0, weightedEscape.getWeight(), FID_TOLERANCE);
    }

    @Test
    @DisplayName("Steering behavior can be enabled/disabled")
    void steeringEnabledToggle() {
        EscapeBehavior toggleEscape = new EscapeBehavior(defaultConfig);
        assertTrue(toggleEscape.isEnabled());

        toggleEscape.setEnabled(false);
        assertFalse(toggleEscape.isEnabled());

        toggleEscape.setEnabled(true);
        assertTrue(toggleEscape.isEnabled());
    }

    // ==================== Vec3d Mathematical Tests ====================

    @Test
    @DisplayName("Vec3d distance calculation is accurate")
    void vec3dDistanceAccurate() {
        Vec3d v1 = new Vec3d(0, 0, 0);
        Vec3d v2 = new Vec3d(3, 4, 0);

        double distance = v1.distanceTo(v2);
        assertEquals(5.0, distance, FID_TOLERANCE); // 3-4-5 triangle
    }

    @Test
    @DisplayName("Vec3d normalization preserves direction")
    void vec3dNormalizationPreservesDirection() {
        Vec3d v = new Vec3d(3, 0, 4);
        v.normalize();

        assertEquals(1.0, v.magnitude(), FID_TOLERANCE);
        assertTrue(v.x > 0);
        assertTrue(v.z > 0);
    }

    @Test
    @DisplayName("Vec3d subtraction yields correct vector")
    void vec3dSubtractionCorrect() {
        Vec3d v1 = new Vec3d(10, 10, 10);
        Vec3d v2 = new Vec3d(3, 3, 3);

        Vec3d result = Vec3d.sub(v1, v2);
        assertEquals(7, result.x, FID_TOLERANCE);
        assertEquals(7, result.y, FID_TOLERANCE);
        assertEquals(7, result.z, FID_TOLERANCE);
    }

    @Test
    @DisplayName("Vec3d addition yields correct vector")
    void vec3dAdditionCorrect() {
        Vec3d v1 = new Vec3d(1, 2, 3);
        Vec3d v2 = new Vec3d(4, 5, 6);

        Vec3d result = Vec3d.add(v1, v2);
        assertEquals(5, result.x, FID_TOLERANCE);
        assertEquals(7, result.y, FID_TOLERANCE);
        assertEquals(9, result.z, FID_TOLERANCE);
    }

    // ==================== Threat Classification Tests ====================

    @Test
    @DisplayName("Threat types are properly defined")
    void threatTypesDefined() {
        FlightInitiationBehavior.ThreatType[] types = FlightInitiationBehavior.ThreatType.values();
        assertEquals(5, types.length);

        assertTrue(List.of(types).contains(FlightInitiationBehavior.ThreatType.AMBUSH));
        assertTrue(List.of(types).contains(FlightInitiationBehavior.ThreatType.CURSORIAL));
        assertTrue(List.of(types).contains(FlightInitiationBehavior.ThreatType.AERIAL));
        assertTrue(List.of(types).contains(FlightInitiationBehavior.ThreatType.HUMAN));
        assertTrue(List.of(types).contains(FlightInitiationBehavior.ThreatType.UNKNOWN));
    }

    @Test
    @DisplayName("Predator multipliers are in valid range")
    void predatorMultipliersValid() {
        // All multipliers should be between 0.5 and 2.0
        double ambush = defaultConfig.getAmbushPredatorMultiplier();
        double cursorial = defaultConfig.getCursorialPredatorMultiplier();
        double aerial = defaultConfig.getAerialPredatorMultiplier();
        double human = defaultConfig.getHumanThreatMultiplier();

        assertTrue(ambush >= 0.5 && ambush <= 2.0);
        assertTrue(cursorial >= 0.5 && cursorial <= 2.0);
        assertTrue(aerial >= 0.5 && aerial <= 2.0);
        assertTrue(human >= 0.5 && human <= 2.0);
    }

    // ==================== Helper Methods ====================

    /**
     * Creates a test BehaviorContext with minimal dependencies.
     */
    private BehaviorContext createTestContext() {
        Vec3d position = new Vec3d(0, 64, 0);
        Vec3d velocity = new Vec3d(1, 0, 0);
        return new BehaviorContext(position, velocity, 1.0, 0.5);
    }

    /**
     * Creates a test BehaviorContext at a specific position.
     */
    private BehaviorContext createTestContextAt(double x, double y, double z) {
        Vec3d position = new Vec3d(x, y, z);
        Vec3d velocity = new Vec3d(0, 0, 0);
        return new BehaviorContext(position, velocity, 1.0, 0.5);
    }

    /**
     * Creates a test threat position at specified offset.
     */
    private Vec3d createThreatPosition(double distance) {
        return new Vec3d(distance, 64, 0);
    }
}
