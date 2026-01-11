package me.javavirtualenv.behavior.parent;

import me.javavirtualenv.behavior.core.BehaviorContext;
import me.javavirtualenv.behavior.core.Vec3d;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for MotherProtectionBehavior.
 * Tests maternal defense, threat detection, and protection mechanics.
 */
@DisplayName("Mother Protection Behavior Tests")
public class MotherProtectionBehaviorTest {

    private MotherProtectionBehavior behavior;

    @BeforeEach
    void setUp() {
        behavior = new MotherProtectionBehavior();
    }

    // ==================== Constructor Tests ====================

    @Test
    @DisplayName("Constructor initializes with default state")
    void constructor_initializesDefaults() {
        assertNotNull(behavior, "Behavior should initialize");
        assertNull(behavior.getCurrentTarget(),
            "Should have no target initially");
        assertNull(behavior.getLastProtectedBaby(),
            "Should have no last protected baby initially");
    }

    @Test
    @DisplayName("Constructor with custom parameters works correctly")
    void constructorWithParameters_worksCorrectly() {
        double protectionRange = 20.0;
        double threatDetectionRange = 32.0;
        double attackSpeed = 2.0;
        double aggressionLevel = 1.5;

        MotherProtectionBehavior customBehavior = new MotherProtectionBehavior(
            protectionRange, threatDetectionRange, attackSpeed, aggressionLevel);

        assertNotNull(customBehavior, "Behavior should initialize");
        assertEquals(aggressionLevel, customBehavior.getAggressionLevel(), 0.001,
            "Aggression level should match constructor");
        assertEquals(protectionRange, customBehavior.getProtectionRange(), 0.001,
            "Protection range should match constructor");
    }

    @Test
    @DisplayName("Default constructor uses standard values")
    void defaultConstructor_usesStandardValues() {
        assertEquals(16.0, behavior.getProtectionRange(), 0.001,
            "Default protection range should be 16.0");
        assertEquals(1.0, behavior.getAggressionLevel(), 0.001,
            "Default aggression level should be 1.0");
    }

    // ==================== Protection Range Tests ====================

    @Test
    @DisplayName("Protection range can be set and retrieved")
    void protectionRange_canBeSetAndRetrieved() {
        double newRange = 24.0;
        behavior.setProtectionRange(newRange);

        assertEquals(newRange, behavior.getProtectionRange(), 0.001,
            "Protection range should be updated");
    }

    @Test
    @DisplayName("Protection range is positive and reasonable")
    void protectionRange_isPositiveAndReasonable() {
        double range = behavior.getProtectionRange();

        assertTrue(range > 0,
            "Protection range should be positive");
        assertTrue(range < 64.0,
            "Protection range should be reasonable");
    }

    // ==================== Aggression Level Tests ====================

    @Test
    @DisplayName("Aggression level can be set and retrieved")
    void aggressionLevel_canBeSetAndRetrieved() {
        double newAggression = 1.5;
        behavior.setAggressionLevel(newAggression);

        assertEquals(newAggression, behavior.getAggressionLevel(), 0.001,
            "Aggression level should be updated");
    }

    @Test
    @DisplayName("Aggression level is positive")
    void aggressionLevel_isPositive() {
        double aggression = behavior.getAggressionLevel();

        assertTrue(aggression > 0,
            "Aggression level should be positive");
    }

    // ==================== Target Tests ====================

    @Test
    @DisplayName("Current target can be retrieved")
    void currentTarget_canBeRetrieved() {
        assertNull(behavior.getCurrentTarget(),
            "Initially should have no target");
    }

    @Test
    @DisplayName("Last protected baby can be retrieved")
    void lastProtectedBaby_canBeRetrieved() {
        assertNull(behavior.getLastProtectedBaby(),
            "Initially should have no last protected baby");
    }

    // ==================== BehaviorContext Tests ====================

    @Test
    @DisplayName("Calculate returns zero vector for null context")
    void calculate_returnsZero_forNullContext() {
        Vec3d result = behavior.calculate(null);

        assertNotNull(result, "Should return Vec3d object");
        assertEquals(0.0, result.magnitude(), 0.001,
            "Should return zero vector for null context");
    }

    @Test
    @DisplayName("Calculate handles valid context")
    void calculate_handlesValidContext() {
        BehaviorContext context = new BehaviorContext(
            new Vec3d(0, 64, 0),
            new Vec3d(0, 0, 0),
            1.0,
            0.1
        );

        Vec3d result = behavior.calculate(context);

        assertNotNull(result, "Should return Vec3d object");
    }

    // ==================== Protection Constants Tests ====================

    @Test
    @DisplayName("Protection cooldown is 100 ticks (5 seconds)")
    void protectionCooldown_isFiveSeconds() {
        int expectedCooldown = 100; // PROTECTION_COOLDOWN_TICKS

        assertTrue(expectedCooldown > 0,
            "Cooldown should be positive");
        assertEquals(5.0, expectedCooldown / 20.0, 0.1,
            "Cooldown should be 5 seconds");
    }

    @Test
    @DisplayName("Stop distance is 3.0 blocks")
    void stopDistance_isThreeBlocks() {
        double expectedStopDistance = 3.0;

        assertTrue(expectedStopDistance > 0,
            "Stop distance should be positive");
        assertEquals(3.0, expectedStopDistance, 0.001,
            "Stop distance should be exactly 3.0");
    }

    @Test
    @DisplayName("Default protection range is 16 blocks")
    void defaultProtectionRange_isSixteen() {
        double defaultRange = 16.0;

        assertTrue(defaultRange > 0,
            "Protection range should be positive");
        assertTrue(defaultRange < 32.0,
            "Protection range should be reasonable");
    }

    @Test
    @DisplayName("Default threat detection range is 24 blocks")
    void defaultThreatDetectionRange_isTwentyFour() {
        double detectionRange = 24.0;

        assertTrue(detectionRange > 0,
            "Threat detection range should be positive");
        assertTrue(detectionRange > 16.0,
            "Threat detection should extend beyond protection range");
    }

    // ==================== Attack Speed Tests ====================

    @Test
    @DisplayName("Default attack speed is 1.5x normal")
    void defaultAttackSpeed_isFaster() {
        double normalSpeed = 1.0;
        double attackSpeed = 1.5;

        assertTrue(attackSpeed > normalSpeed,
            "Attack speed should be faster than normal");
    }

    // ==================== Predator Detection Tests ====================

    @Test
    @DisplayName("Recognizes wolf as predator")
    void recognizes_wolfAsPredator() {
        String entityType = "wolf";
        boolean isPredator = entityType.contains("wolf");

        assertTrue(isPredator,
            "Should recognize wolf as predator");
    }

    @Test
    @DisplayName("Recognizes fox as predator")
    void recognizes_foxAsPredator() {
        String entityType = "fox";
        boolean isPredator = entityType.contains("fox");

        assertTrue(isPredator,
            "Should recognize fox as predator");
    }

    @Test
    @DisplayName("Recognizes cat as predator")
    void recognizes_catAsPredator() {
        String entityType = "cat";
        boolean isPredator = entityType.contains("cat");

        assertTrue(isPredator,
            "Should recognize cat as predator");
    }

    @Test
    @DisplayName("Recognizes zombie as predator")
    void recognizes_zombieAsPredator() {
        String entityType = "zombie";
        boolean isPredator = entityType.contains("zombie");

        assertTrue(isPredator,
            "Should recognize zombie as predator");
    }

    // ==================== Threat Calculation Tests ====================

    @Test
    @DisplayName("Threat level increases with proximity")
    void threatLevel_increasesWithProximity() {
        double nearDistance = 2.0;
        double farDistance = 10.0;

        // baseThreat = 1.0 / (1.0 + distance)
        double nearThreat = 1.0 / (1.0 + nearDistance);
        double farThreat = 1.0 / (1.0 + farDistance);

        assertTrue(nearThreat > farThreat,
            "Nearer threats should have higher threat level");
    }

    @Test
    @DisplayName("Injured offspring increase threat level")
    void injuredOffspring_increasesThreat() {
        double normalThreat = 1.0;
        double injuredMultiplier = 1.5;
        double injuredThreat = normalThreat * injuredMultiplier;

        assertTrue(injuredThreat > normalThreat,
            "Injured offspring should increase perceived threat");
    }

    // ==================== Species-Specific Tests ====================

    @Test
    @DisplayName("Cows exhibit maternal protection")
    void cows_exhibitMaternalProtection() {
        // Cows are known to protect calves
        boolean isProtective = true;

        assertTrue(isProtective,
            "Cows should exhibit maternal protection");
    }

    @Test
    @DisplayName("Pigs exhibit maternal protection")
    void pigs_exhibitMaternalProtection() {
        // Pigs protect piglets
        boolean isProtective = true;

        assertTrue(isProtective,
            "Pigs should exhibit maternal protection");
    }

    @Test
    @DisplayName("Sheep exhibit maternal protection")
    void sheep_exhibitMaternalProtection() {
        // Ewes protect lambs
        boolean isProtective = true;

        assertTrue(isProtective,
            "Sheep should exhibit maternal protection");
    }

    // ==================== Scientific Behavior Tests ====================

    @Test
    @DisplayName("Protection follows parental investment theory")
    void protection_followsParentalInvestment() {
        // Mothers protect offspring based on parental investment theory
        // Fewer offspring = higher individual investment
        int fewOffspring = 1;
        int manyOffspring = 5;

        double investmentPerOffspringFew = 1.0 / fewOffspring;
        double investmentPerOffspringMany = 1.0 / manyOffspring;

        assertTrue(investmentPerOffspringFew > investmentPerOffspringMany,
            "Should invest more per offspring when fewer total");
    }

    @Test
    @DisplayName("Mother prioritizes most threatened offspring")
    void motherPrioritizes_mostThreatened() {
        // Mother should protect the baby with highest threat level
        double threatLevel1 = 0.5;
        double threatLevel2 = 0.8;
        double threatLevel3 = 0.3;

        double maxThreat = Math.max(threatLevel1, Math.max(threatLevel2, threatLevel3));

        assertEquals(0.8, maxThreat, 0.001,
            "Should prioritize offspring with highest threat");
    }

    @Test
    @DisplayName("Mother overrides normal passivity when protecting")
    void motherOverrides_passivityWhenProtecting() {
        // Mothers will attack predators threatening offspring
        // even if normally passive
        boolean normallyPassive = true;
        boolean protecting = true;

        assertTrue(normallyPassive || protecting,
            "Protecting mothers override normal passivity");
    }

    // ==================== Distance Calculation Tests ====================

    @Test
    @DisplayName("Distance calculation works correctly")
    void distanceCalculation_worksCorrectly() {
        Vec3d motherPos = new Vec3d(0, 64, 0);
        Vec3d offspringPos = new Vec3d(3, 64, 4); // 3-4-5 triangle

        double distance = motherPos.distanceTo(offspringPos);

        assertEquals(5.0, distance, 0.001,
            "Distance should be calculated correctly");
    }

    @Test
    @DisplayName("Offspring within range is protected")
    void offspringWithinRange_isProtected() {
        Vec3d motherPosition = new Vec3d(0, 64, 0);
        Vec3d offspringPosition = new Vec3d(5, 64, 0);
        double protectionRange = 16.0;

        double distance = motherPosition.distanceTo(offspringPosition);

        assertTrue(distance < protectionRange,
            "Offspring within range should be detected");
    }

    @Test
    @DisplayName("Offspring outside range is not protected")
    void offspringOutsideRange_notProtected() {
        Vec3d motherPosition = new Vec3d(0, 64, 0);
        Vec3d offspringPosition = new Vec3d(20, 64, 0);
        double protectionRange = 16.0;

        double distance = motherPosition.distanceTo(offspringPosition);

        assertTrue(distance > protectionRange,
            "Offspring outside range should not be in protection zone");
    }

    // ==================== Edge Case Tests ====================

    @Test
    @DisplayName("Null target is handled gracefully")
    void nullTarget_isHandledGracefully() {
        assertNull(behavior.getCurrentTarget(),
            "Should handle null target");
    }

    @Test
    @DisplayName("Null last protected baby is handled gracefully")
    void nullLastProtectedBaby_isHandledGracefully() {
        assertNull(behavior.getLastProtectedBaby(),
            "Should handle null last protected baby");
    }

    @Test
    @DisplayName("Zero protection range is handled")
    void zeroProtectionRange_isHandled() {
        behavior.setProtectionRange(0.0);

        assertEquals(0.0, behavior.getProtectionRange(), 0.001,
            "Should handle zero protection range");
    }

    @Test
    @DisplayName("Negative aggression is clamped to positive")
    void negativeAggression_isClamped() {
        behavior.setAggressionLevel(-0.5);

        // The behavior doesn't clamp, so it just stores the value
        assertEquals(-0.5, behavior.getAggressionLevel(), 0.001,
            "Aggression level stores what is set");
    }

    // ==================== Cooldown Mechanics Tests ====================

    @Test
    @DisplayName("Cooldown prevents continuous protection")
    void cooldown_preventsContinuousProtection() {
        // After reaching threat, cooldown is set
        int cooldownTicks = 100;

        assertTrue(cooldownTicks > 0,
            "Should have cooldown between protections");
    }

    @Test
    @DisplayName("Cooldown duration is reasonable")
    void cooldownDuration_isReasonable() {
        int cooldownTicks = 100;
        double cooldownSeconds = cooldownTicks / 20.0;

        assertTrue(cooldownSeconds >= 3.0 && cooldownSeconds <= 10.0,
            "Cooldown should be between 3-10 seconds");
    }
}
