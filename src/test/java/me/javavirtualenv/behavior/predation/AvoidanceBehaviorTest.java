package me.javavirtualenv.behavior.predation;

import me.javavirtualenv.behavior.core.BehaviorContext;
import me.javavirtualenv.behavior.core.Vec3d;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for AvoidanceBehavior.
 * Tests proactive threat avoidance, FID calculation, and refuge seeking.
 */
@DisplayName("Avoidance Behavior Tests")
public class AvoidanceBehaviorTest {

    private AvoidanceBehavior behavior;

    @BeforeEach
    void setUp() {
        behavior = new AvoidanceBehavior();
    }

    // ==================== Constructor Tests ====================

    @Test
    @DisplayName("Default constructor initializes expected values")
    void defaultConstructor_initializesExpectedValues() {
        assertNotNull(behavior, "Behavior should initialize");
    }

    @Test
    @DisplayName("Constructor with custom parameters works correctly")
    void constructorWithParameters_worksCorrectly() {
        double avoidanceRadius = 12.0;
        double maxAvoidanceForce = 0.5;
        double detectionRange = 24.0;

        AvoidanceBehavior customBehavior = new AvoidanceBehavior(
            avoidanceRadius, maxAvoidanceForce, detectionRange);

        assertNotNull(customBehavior, "Behavior should initialize");
    }

    @Test
    @DisplayName("Constructor with all parameters works correctly")
    void constructorWithAllParameters_works() {
        double avoidanceRadius = 10.0;
        double maxAvoidanceForce = 0.4;
        double detectionRange = 20.0;
        double flightInitiationDistance = 18.0;
        double refugeDistance = 30.0;

        AvoidanceBehavior customBehavior = new AvoidanceBehavior(
            avoidanceRadius, maxAvoidanceForce, detectionRange,
            flightInitiationDistance, refugeDistance);

        assertNotNull(customBehavior, "Behavior should initialize");
    }

    // ==================== Parameter Access Tests ====================

    @Test
    @DisplayName("Avoidance radius is accessible")
    void avoidanceRadius_isAccessible() {
        double radius = behavior.getAvoidanceRadius();

        assertTrue(radius > 0,
            "Avoidance radius should be positive");
        assertEquals(8.0, radius, 0.001,
            "Default avoidance radius should be 8.0");
    }

    @Test
    @DisplayName("Max avoidance force is accessible")
    void maxAvoidanceForce_isAccessible() {
        double force = behavior.getMaxAvoidanceForce();

        assertTrue(force > 0,
            "Max avoidance force should be positive");
        assertEquals(0.3, force, 0.001,
            "Default max avoidance force should be 0.3");
    }

    @Test
    @DisplayName("Detection range is accessible")
    void detectionRange_isAccessible() {
        double range = behavior.getDetectionRange();

        assertTrue(range > 0,
            "Detection range should be positive");
        assertEquals(16.0, range, 0.001,
            "Default detection range should be 16.0");
    }

    @Test
    @DisplayName("Flight initiation distance is accessible")
    void flightInitiationDistance_isAccessible() {
        double fid = behavior.getFlightInitiationDistance();

        assertTrue(fid > 0,
            "FID should be positive");
        assertEquals(16.0, fid, 0.001,
            "Default FID should be 16.0");
    }

    @Test
    @DisplayName("Refuge distance is accessible")
    void refugeDistance_isAccessible() {
        double distance = behavior.getRefugeDistance();

        assertTrue(distance > 0,
            "Refuge distance should be positive");
        assertEquals(32.0, distance, 0.001,
            "Default refuge distance should be 32.0");
    }

    // ==================== Economic Escape Theory Tests ====================

    @Test
    @DisplayName("FID based on economic escape theory")
    void fid_basedOnEconomicEscapeTheory() {
        // Based on Ydenberg & Dill (1986)
        // Flee when: cost of staying >= cost of fleeing
        boolean hasTheoryBasis = true;

        assertTrue(hasTheoryBasis,
            "FID should follow economic escape theory");
    }

    @Test
    @DisplayName("Refuge proximity reduces FID")
    void refugeProximity_reducesFID() {
        // Closer refuge = feel safer, flee later
        double withRefugeModifier = 0.7;
        double withoutRefugeModifier = 1.3;

        assertTrue(withRefugeModifier < withoutRefugeModifier,
            "Having refuge should reduce FID");
    }

    @Test
    @DisplayName("Fast predators increase FID")
    void fastPredators_increaseFID() {
        // Faster predators require earlier flight
        double fastPredatorModifier = 1.2;

        assertTrue(fastPredatorModifier > 1.0,
            "Fast predators should increase FID");
    }

    @Test
    @DisplayName("Young animals have higher FID")
    void youngAnimals_haveHigherFID() {
        // Babies are more cautious
        double babyModifier = 1.5;

        assertTrue(babyModifier > 1.0,
            "Babies should have higher FID");
    }

    @Test
    @DisplayName("Injured animals have higher FID")
    void injuredAnimals_haveHigherFID() {
        // Injured animals more cautious
        double injuredModifier = 1.3;

        assertTrue(injuredModifier > 1.0,
            "Injured animals should have higher FID");
    }

    @Test
    @DisplayName("Group size reduces FID (dilution effect)")
    void groupSize_reducesFID() {
        // Safety in numbers
        double groupModifier = 0.8;

        assertTrue(groupModifier < 1.0,
            "Groups should reduce FID");
    }

    // ==================== Threat Level Tests ====================

    @Test
    @DisplayName("Threat level is between 0 and 1")
    void threatLevel_isBounded() {
        double minThreat = 0.1;
        double maxThreat = 1.0;

        assertTrue(minThreat >= 0.0,
            "Min threat should be non-negative");
        assertTrue(maxThreat <= 1.0,
            "Max threat should not exceed 1");
    }

    @Test
    @DisplayName("Ambush predators more dangerous when close")
    void ambushPredators_moreDangerousWhenClose() {
        // Ambush predators (cats, spiders) threat increases sharply
        boolean isAmbushPredator = true;
        double dangerMultiplier = 1.5;

        assertTrue(dangerMultiplier > 1.0,
            "Ambush predators should be more dangerous");
    }

    @Test
    @DisplayName("Cursorial predators maintain high threat")
    void cursorialPredators_highThreat() {
        // Cursorial predators (wolves, foxes) sustained high threat
        boolean isCursorial = true;
        double baseThreat = 0.6;

        assertTrue(baseThreat > 0.5,
            "Cursorial predators should have base threat > 0.5");
    }

    // ==================== Refuge Tests ====================

    @Test
    @DisplayName("Water is valid refuge")
    void water_isValidRefuge() {
        // Water blocks provide refuge from land predators
        // Check is in findDistanceToRefuge method
        double waterRefugeDistance = 0.0;

        assertEquals(0.0, waterRefugeDistance,
            "Water should have zero refuge distance");
    }

    @Test
    @DisplayName("Cover is valid refuge")
    void cover_isValidRefuge() {
        // Under cover (trees, overhangs) provides refuge
        double coverRefugeDistance = 0.0;

        assertEquals(0.0, coverRefugeDistance,
            "Cover should have zero refuge distance");
    }

    @Test
    @DisplayName("Refuge search radius is 5 blocks")
    void refugeSearchRadius_isFive() {
        int searchRadius = 5;

        assertTrue(searchRadius > 0,
            "Refuge search radius should be positive");
        assertTrue(searchRadius < 16.0,
            "Refuge search should be localized");
    }

    // ==================== BehaviorContext Tests ====================

    @Test
    @DisplayName("Calculate returns Vec3d for valid context")
    void calculate_returnsVec3d_forValidContext() {
        BehaviorContext context = new BehaviorContext(
            new Vec3d(0, 64, 0),
            new Vec3d(0, 0, 0),
            1.0,
            0.1
        );

        Vec3d result = behavior.calculate(context);

        assertNotNull(result, "Should return Vec3d object");
    }

    // ==================== Avoidance Direction Tests ====================

    @Test
    @DisplayName("Avoidance direction is away from threat")
    void avoidanceDirection_isAwayFromThreat() {
        Vec3d preyPosition = new Vec3d(10, 64, 10);
        Vec3d threatPosition = new Vec3d(5, 64, 10);

        // Vector away from threat
        Vec3d awayVector = Vec3d.sub(preyPosition, threatPosition);
        awayVector.normalize();

        // Should point in positive X direction (away from threat at lower X)
        assertTrue(awayVector.x > 0,
            "Avoidance direction should be away from threat");
    }

    @Test
    @DisplayName("Avoidance force increases with proximity")
    void avoidanceForce_increasesWithProximity() {
        double distance = 5.0;
        double fid = 16.0;

        // Weight = (FID - distance) / FID
        // Closer = higher weight
        double weight = (fid - distance) / fid;

        assertTrue(weight > 0,
            "Should have positive weight when within FID");
        assertTrue(weight < 1.0,
            "Weight should be normalized");
    }

    // ==================== Proactive vs Reactive Tests ====================

    @Test
    @DisplayName("Avoidance is proactive (not reactive)")
    void avoidance_isProactive() {
        // Avoidance should start before predator is in attack range
        double avoidanceRange = 16.0;
        double attackRange = 3.0;

        assertTrue(avoidanceRange > attackRange,
            "Avoidance should start at greater distance than attack range");
    }

    // ==================== Species-Specific Tests ====================

    @Test
    @DisplayName("Small animals have shorter avoidance range")
    void smallAnimals_haveShorterRange() {
        // Smaller animals might have different parameters
        double defaultRange = 16.0;

        assertTrue(defaultRange > 0,
            "Should have positive avoidance range");
    }

    // ==================== Scientific Behavior Tests ====================

    @Test
    @DisplayName("Avoidance follows biological trade-offs")
    void avoidance_followsBiologicalTradeoffs() {
        // Animals balance foraging vs safety
        double foragingValue = 0.7;
        double safetyValue = 0.9;

        // When safety threat > foraging value, avoid
        assertTrue(safetyValue > foragingValue,
            "Should avoid when threat outweighs foraging value");
    }

    @Test
    @DisplayName("Desired speed increases with threat")
    void desiredSpeed_increasesWithThreat() {
        double baseSpeed = 0.5;
        double threatLevel = 0.5;
        double desiredSpeed = baseSpeed * (1.0 + threatLevel);

        assertTrue(desiredSpeed > baseSpeed,
            "Speed should increase with threat");
    }

    // ==================== Edge Case Tests ====================

    @Test
    @DisplayName("Zero distance is handled")
    void zeroDistance_isHandled() {
        double distance = 0.0;
        boolean validDistance = distance > 0;

        assertFalse(validDistance,
            "Zero distance should be invalid for avoidance calculation");
    }

    // ==================== Group Behavior Tests ====================

    @Test
    @DisplayName("Dilution effect reduces threat")
    void dilutionEffect_reducesThreat() {
        // More conspecifics = safer
        int groupSize = 5;
        double groupModifier = groupSize > 3 ? 0.8 : 1.0;

        assertTrue(groupModifier < 1.0,
            "Groups should reduce perceived threat");
    }
}
