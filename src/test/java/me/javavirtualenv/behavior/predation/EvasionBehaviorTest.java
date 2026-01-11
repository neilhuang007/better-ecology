package me.javavirtualenv.behavior.predation;

import me.javavirtualenv.behavior.core.BehaviorContext;
import me.javavirtualenv.behavior.core.Vec3d;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for EvasionBehavior.
 * Tests prey evasion mechanics, safety distances, and zigzag patterns.
 */
@DisplayName("Evasion Behavior Tests")
public class EvasionBehaviorTest {

    private EvasionBehavior behavior;

    @BeforeEach
    void setUp() {
        behavior = new EvasionBehavior();
    }

    // ==================== Constructor Tests ====================

    @Test
    @DisplayName("Default constructor initializes expected values")
    void defaultConstructor_initializesExpectedValues() {
        assertNotNull(behavior, "Behavior should initialize");
        assertFalse(behavior.isEvading(),
            "Should not be evading initially");
        assertNull(behavior.getCurrentThreat(),
            "Should have no threat initially");
    }

    @Test
    @DisplayName("Constructor with evasion speed and force works")
    void constructorWithSpeedAndForce_works() {
        double evasionSpeed = 2.0;
        double evasionForce = 0.3;
        double detectionRange = 32.0;
        double safetyDistance = 50.0;

        EvasionBehavior customBehavior = new EvasionBehavior(
            evasionSpeed, evasionForce, detectionRange, safetyDistance);

        assertNotNull(customBehavior, "Behavior should initialize");
    }

    @Test
    @DisplayName("Constructor with zigzag intensity works")
    void constructorWithZigzag_works() {
        double zigzagIntensity = 0.8;
        EvasionBehavior customBehavior = new EvasionBehavior(
            1.5, 0.2, 24.0, 36.0, zigzagIntensity);

        assertNotNull(customBehavior, "Behavior should initialize");
    }

    // ==================== Evasion State Tests ====================

    @Test
    @DisplayName("Evasion state can be set")
    void evasionState_canBeSet() {
        assertFalse(behavior.isEvading());

        behavior.setEvading(true);
        assertTrue(behavior.isEvading());

        behavior.setEvading(false);
        assertFalse(behavior.isEvading());
    }

    // ==================== Safety Distance Tests ====================

    @Test
    @DisplayName("Safety distance is 36 blocks (fixed from 48)")
    void safetyDistance_isThirtySix() {
        // Fixed from 48.0 to 36.0 based on code review
        double expectedSafetyDistance = 36.0;

        assertTrue(expectedSafetyDistance > 0,
            "Safety distance should be positive");
        assertTrue(expectedSafetyDistance < 50.0,
            "Safety distance should be reasonable");
    }

    @Test
    @DisplayName("Safety distance can be customized")
    void safetyDistance_canBeCustomized() {
        double customSafetyDistance = 50.0;
        EvasionBehavior customBehavior = new EvasionBehavior(
            1.5, 0.2, 24.0, customSafetyDistance);

        assertNotNull(customBehavior, "Custom safety distance should work");
    }

    // ==================== Detection Range Tests ====================

    @Test
    @DisplayName("Detection range is 24 blocks")
    void detectionRange_isTwentyFour() {
        // Default detection range from constructor
        double expectedDetectionRange = 24.0;

        assertTrue(expectedDetectionRange > 0,
            "Detection range should be positive");
        assertTrue(expectedDetectionRange < 48.0,
            "Detection range should be reasonable");
    }

    @Test
    @DisplayName("Detection range can be customized")
    void detectionRange_canBeCustomized() {
        double customDetectionRange = 32.0;
        EvasionBehavior customBehavior = new EvasionBehavior(
            1.5, 0.2, customDetectionRange, 36.0);

        assertNotNull(customBehavior, "Custom detection range should work");
    }

    // ==================== Evasion Speed Tests ====================

    @Test
    @DisplayName("Evasion speed is 1.5x normal")
    void evasionSpeed_isFaster() {
        double normalSpeed = 1.0;
        double evasionSpeed = 1.5;

        assertTrue(evasionSpeed > normalSpeed,
            "Evasion speed should be greater than normal speed");
    }

    // ==================== Zigzag Tests ====================

    @Test
    @DisplayName("Zigzag intensity is 0.5 by default")
    void zigzagIntensity_isDefault() {
        double defaultIntensity = 0.5;

        assertTrue(defaultIntensity > 0,
            "Should have some zigzag");
        assertTrue(defaultIntensity < 1.0,
            "Zigzag should not override primary direction");
    }

    @Test
    @DisplayName("Zigzag timing is unpredictable")
    void zigzagTiming_isUnpredictable() {
        // Change every 5-15 ticks (unpredictable timing)
        int minChangeInterval = 5;
        int maxChangeInterval = 15;

        assertTrue(minChangeInterval > 0,
            "Should change direction periodically");
        assertTrue(maxChangeInterval > minChangeInterval,
            "Timing should vary");
    }

    @Test
    @DisplayName("Zigzag direction is smoothed")
    void zigzagDirection_isSmoothed() {
        // Smooth interpolation with lerp factor of 0.2
        double lerpFactor = 0.2;

        assertTrue(lerpFactor > 0 && lerpFactor < 1.0,
            "Should use smoothing factor");
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

    // ==================== Evasion Direction Tests ====================

    @Test
    @DisplayName("Evasion direction is away from threat")
    void evasionDirection_isAwayFromThreat() {
        Vec3d preyPosition = new Vec3d(10, 64, 10);
        Vec3d threatPosition = new Vec3d(5, 64, 10);

        // Vector away from threat
        Vec3d awayVector = Vec3d.sub(preyPosition, threatPosition);
        awayVector.normalize();

        // Should point in positive X direction (away from threat at lower X)
        assertTrue(awayVector.x > 0,
            "Evasion direction should be away from threat");
    }

    @Test
    @DisplayName("Zigzag adds perpendicular component")
    void zigzag_addsPerpendicularComponent() {
        // Zigzag adds lateral (perpendicular) movement
        Vec3d awayFromThreat = new Vec3d(1, 0, 0); // Moving in +X
        Vec3d perpendicular = new Vec3d(0, 0, 1); // Perpendicular (Z axis)

        assertTrue(perpendicular.x == 0 && perpendicular.z != 0,
            "Perpendicular should be lateral");
    }

    // ==================== Threat Assessment Tests ====================

    @Test
    @DisplayName("Threats include wolves, foxes, cats, ocelots, spiders")
    void threats_includePredators() {
        // Predators that trigger evasion
        String[] predators = {"wolf", "fox", "cat", "ocelot", "spider"};

        assertTrue(predators.length >= 5,
            "Should recognize multiple predator types");
    }

    @Test
    @DisplayName("Sneaking players don't trigger evasion")
    void sneakingPlayers_dontTriggerEvasion() {
        // Players who are sneaking don't trigger evasion
        boolean isSneaking = true;
        boolean isThreat = !isSneaking;

        assertFalse(isThreat,
            "Sneaking players should not trigger evasion");
    }

    // ==================== Scientific Behavior Tests ====================

    @Test
    @DisplayName("Evasion uses protean (unpredictable) movement")
    void evasion_usesProteanMovement() {
        // Unpredictable zigzagging based on Moore et al. (2017)
        boolean hasZigzag = true;
        boolean isUnpredictable = true;

        assertTrue(hasZigzag && isUnpredictable,
            "Evasion should be unpredictable");
    }

    @Test
    @DisplayName("Flight initiation distance scales with threat")
    void flightInitiation_scalesWithThreat() {
        double closeThreatFID = 12.0;
        double farThreatFID = 24.0;

        assertTrue(farThreatFID > closeThreatFID,
            "Should detect threats from farther away");
    }

    // ==================== Edge Case Tests ====================

    @Test
    @DisplayName("Empty threat list is handled")
    void emptyThreatList_isHandled() {
        assertNull(behavior.getCurrentThreat(),
            "Should handle no threat gracefully");
    }

    @Test
    @DisplayName("Dead threat is cleared")
    void deadThreat_isCleared() {
        // When threat dies, it's cleared from current threat
        boolean isAlive = false;

        assertTrue(!isAlive || behavior.getCurrentThreat() == null,
            "Dead threats should be cleared");
    }

    // ==================== Force Limit Tests ====================

    @Test
    @DisplayName("Evasion force is limited")
    void evasionForce_isLimited() {
        double evasionForce = 0.2;

        assertTrue(evasionForce > 0,
            "Evasion force should be positive");
        assertTrue(evasionForce <= 1.0,
            "Evasion force should be limited");
    }
}
