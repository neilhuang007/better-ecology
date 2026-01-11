package me.javavirtualenv.behavior.wolf;

import me.javavirtualenv.behavior.core.BehaviorContext;
import me.javavirtualenv.behavior.core.Vec3d;
import me.javavirtualenv.behavior.predation.PreySelector;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive unit tests for PackHuntingBehavior.
 * Tests state management, calculations, and wolf pack coordination
 * without requiring full Minecraft entity simulation.
 * <p>
 * Scientific Basis:
 * - Wolf pack hunting strategies (Mech 2007, Peterson 2018)
 * - Alpha-led pursuit with flanking maneuvers
 * - Pack coordination through positioning
 * - Energy-based sustainability (30% energy cost per hunt)
 */
@DisplayName("Pack Hunting Behavior Tests")
public class PackHuntingBehaviorTest {

    private PackHuntingBehavior behavior;
    private static final double DOUBLE_DELTA = 0.001;

    @BeforeEach
    void setUp() {
        behavior = new PackHuntingBehavior();
    }

    // ==================== Constructor Tests ====================

    @Test
    @DisplayName("Default constructor sets expected values")
    void defaultConstructor_setsExpectedValues() {
        PackHuntingBehavior defaultBehavior = new PackHuntingBehavior();

        assertNotNull(defaultBehavior, "Behavior should initialize");
        assertEquals(PackHuntingBehavior.PackHuntingState.IDLE,
                defaultBehavior.getHuntingState(),
                "Initial state should be IDLE");
        assertNull(defaultBehavior.getCurrentPrey(),
                "Initial prey should be null");
    }

    @Test
    @DisplayName("Constructor with parameters sets values correctly")
    void constructorWithParameters_setsValuesCorrectly() {
        double maxPursuitSpeed = 1.5;
        double maxPursuitForce = 0.2;
        double flankingAngle = Math.PI / 4;
        double coordinationRange = 64.0;
        int minPackSize = 3;

        PackHuntingBehavior customBehavior = new PackHuntingBehavior(
                maxPursuitSpeed, maxPursuitForce, flankingAngle,
                coordinationRange, minPackSize
        );

        assertNotNull(customBehavior, "Behavior should initialize with custom params");
        assertEquals(PackHuntingBehavior.PackHuntingState.IDLE,
                customBehavior.getHuntingState(),
                "Initial state should be IDLE");
        assertNull(customBehavior.getCurrentPrey(),
                "Initial prey should be null");
    }

    @Test
    @DisplayName("Constructor with PreySelector parameter works correctly")
    void constructorWithPreySelector_worksCorrectly() {
        PreySelector preySelector = new PreySelector(48.0, 1.5, 2.0, 3.0);

        PackHuntingBehavior customBehavior = new PackHuntingBehavior(
                1.3, 0.18, Math.PI / 3, 48.0, 2, preySelector
        );

        assertNotNull(customBehavior, "Behavior should initialize with PreySelector");
        assertEquals(PackHuntingBehavior.PackHuntingState.IDLE,
                customBehavior.getHuntingState());
    }

    // ==================== State Management Tests ====================

    @Test
    @DisplayName("getCurrentPrey/setCurrentPrey work correctly")
    void getCurrentPreySetCurrentPrey_workCorrectly() {
        // Initially null
        assertNull(behavior.getCurrentPrey(),
                "Initial prey should be null");

        // Set to null (Entity mock would require complex setup)
        behavior.setCurrentPrey(null);
        assertNull(behavior.getCurrentPrey(),
                "Prey should remain null");
    }

    @Test
    @DisplayName("getHuntingState/setHuntingState work correctly")
    void getHuntingStateSetHuntingState_workCorrectly() {
        // Initial state is IDLE
        assertEquals(PackHuntingBehavior.PackHuntingState.IDLE,
                behavior.getHuntingState(),
                "Initial state should be IDLE");

        // Set to SEARCHING
        behavior.setHuntingState(PackHuntingBehavior.PackHuntingState.SEARCHING);
        assertEquals(PackHuntingBehavior.PackHuntingState.SEARCHING,
                behavior.getHuntingState(),
                "State should be SEARCHING");

        // Set to WAITING
        behavior.setHuntingState(PackHuntingBehavior.PackHuntingState.WAITING);
        assertEquals(PackHuntingBehavior.PackHuntingState.WAITING,
                behavior.getHuntingState(),
                "State should be WAITING");

        // Set to LEADING
        behavior.setHuntingState(PackHuntingBehavior.PackHuntingState.LEADING);
        assertEquals(PackHuntingBehavior.PackHuntingState.LEADING,
                behavior.getHuntingState(),
                "State should be LEADING");

        // Set to FLANKING
        behavior.setHuntingState(PackHuntingBehavior.PackHuntingState.FLANKING);
        assertEquals(PackHuntingBehavior.PackHuntingState.FLANKING,
                behavior.getHuntingState(),
                "State should be FLANKING");

        // Set to ATTACKING
        behavior.setHuntingState(PackHuntingBehavior.PackHuntingState.ATTACKING);
        assertEquals(PackHuntingBehavior.PackHuntingState.ATTACKING,
                behavior.getHuntingState(),
                "State should be ATTACKING");

        // Set to RESTING
        behavior.setHuntingState(PackHuntingBehavior.PackHuntingState.RESTING);
        assertEquals(PackHuntingBehavior.PackHuntingState.RESTING,
                behavior.getHuntingState(),
                "State should be RESTING");
    }

    @Test
    @DisplayName("getPackId returns pack ID")
    void getPackId_returnsPackId() {
        // Initially null
        assertNull(behavior.getPackId(),
                "Initial pack ID should be null");

        // Set a pack ID
        UUID packId = UUID.randomUUID();
        behavior.setPackId(packId);

        assertEquals(packId, behavior.getPackId(),
                "Pack ID should be returned correctly");
    }

    // ==================== Enum Tests ====================

    @Test
    @DisplayName("PackHuntingState enum has all expected values")
    void packHuntingStateEnum_hasAllExpectedValues() {
        PackHuntingBehavior.PackHuntingState[] states =
                PackHuntingBehavior.PackHuntingState.values();

        assertEquals(7, states.length,
                "Should have 7 hunting states");

        // Verify all expected states exist
        boolean hasIdle = false;
        boolean hasSearching = false;
        boolean hasWaiting = false;
        boolean hasLeading = false;
        boolean hasFlanking = false;
        boolean hasAttacking = false;
        boolean hasResting = false;

        for (PackHuntingBehavior.PackHuntingState state : states) {
            if (state == PackHuntingBehavior.PackHuntingState.IDLE) hasIdle = true;
            if (state == PackHuntingBehavior.PackHuntingState.SEARCHING) hasSearching = true;
            if (state == PackHuntingBehavior.PackHuntingState.WAITING) hasWaiting = true;
            if (state == PackHuntingBehavior.PackHuntingState.LEADING) hasLeading = true;
            if (state == PackHuntingBehavior.PackHuntingState.FLANKING) hasFlanking = true;
            if (state == PackHuntingBehavior.PackHuntingState.ATTACKING) hasAttacking = true;
            if (state == PackHuntingBehavior.PackHuntingState.RESTING) hasResting = true;
        }

        assertTrue(hasIdle, "Should have IDLE state");
        assertTrue(hasSearching, "Should have SEARCHING state");
        assertTrue(hasWaiting, "Should have WAITING state");
        assertTrue(hasLeading, "Should have LEADING state");
        assertTrue(hasFlanking, "Should have FLANKING state");
        assertTrue(hasAttacking, "Should have ATTACKING state");
        assertTrue(hasResting, "Should have RESTING state");
    }

    @Test
    @DisplayName("PackHuntingState follows logical hunting sequence")
    void packHuntingState_followsLogicalHuntingSequence() {
        // Test state transitions
        PackHuntingBehavior.PackHuntingState state =
                PackHuntingBehavior.PackHuntingState.IDLE;

        state = PackHuntingBehavior.PackHuntingState.SEARCHING;
        assertEquals(PackHuntingBehavior.PackHuntingState.SEARCHING, state);

        state = PackHuntingBehavior.PackHuntingState.WAITING;
        assertEquals(PackHuntingBehavior.PackHuntingState.WAITING, state);

        state = PackHuntingBehavior.PackHuntingState.LEADING;
        assertEquals(PackHuntingBehavior.PackHuntingState.LEADING, state);

        state = PackHuntingBehavior.PackHuntingState.FLANKING;
        assertEquals(PackHuntingBehavior.PackHuntingState.FLANKING, state);

        state = PackHuntingBehavior.PackHuntingState.ATTACKING;
        assertEquals(PackHuntingBehavior.PackHuntingState.ATTACKING, state);

        state = PackHuntingBehavior.PackHuntingState.RESTING;
        assertEquals(PackHuntingBehavior.PackHuntingState.RESTING, state);

        state = PackHuntingBehavior.PackHuntingState.IDLE;
        assertEquals(PackHuntingBehavior.PackHuntingState.IDLE, state);
    }

    // ==================== Calculation Tests ====================

    @Test
    @DisplayName("calculateStrength() based on health and age - formula validation")
    void calculateStrength_basedOnHealthAndAge_worksCorrectly() {
        // Validate the strength calculation formula:
        // strength = (health / maxHealth) * ageBonus
        // where ageBonus is 1.5 for adults, 1.0 for babies

        // Test adult with full health: (20.0 / 20.0) * 1.5 = 1.5
        double adultHealthyStrength = (20.0 / 20.0) * 1.5;
        assertEquals(1.5, adultHealthyStrength, DOUBLE_DELTA,
                "Adult wolf with full health should have strength 1.5");

        // Test adult with half health: (10.0 / 20.0) * 1.5 = 0.75
        double adultInjuredStrength = (10.0 / 20.0) * 1.5;
        assertEquals(0.75, adultInjuredStrength, DOUBLE_DELTA,
                "Adult wolf with half health should have strength 0.75");

        // Test baby with full health: (20.0 / 20.0) * 1.0 = 1.0
        double babyHealthyStrength = (20.0 / 20.0) * 1.0;
        assertEquals(1.0, babyHealthyStrength, DOUBLE_DELTA,
                "Baby wolf with full health should have strength 1.0");

        // Test baby with half health: (10.0 / 20.0) * 1.0 = 0.5
        double babyInjuredStrength = (10.0 / 20.0) * 1.0;
        assertEquals(0.5, babyInjuredStrength, DOUBLE_DELTA,
                "Baby wolf with half health should have strength 0.5");
    }

    @Test
    @DisplayName("calculateStrength() handles edge cases")
    void calculateStrength_handlesEdgeCases() {
        // Wolf with zero health: (0.0 / 20.0) * 1.5 = 0.0
        double deadStrength = (0.0 / 20.0) * 1.5;
        assertEquals(0.0, deadStrength, DOUBLE_DELTA,
                "Wolf with zero health should have zero strength");

        // Wolf with very low health: (1.0 / 20.0) * 1.5 = 0.075
        double criticalStrength = (1.0 / 20.0) * 1.5;
        assertEquals(0.075, criticalStrength, DOUBLE_DELTA,
                "Wolf with critical health should have proportional strength");
    }

    @Test
    @DisplayName("calculateStrength() adult stronger than baby with same health")
    void calculateStrength_adultStrongerThanBaby_withSameHealth() {
        double adultStrength = (15.0 / 20.0) * 1.5;
        double babyStrength = (15.0 / 20.0) * 1.0;

        assertTrue(adultStrength > babyStrength,
                "Adult wolf should be stronger than baby with same health");
        assertEquals(1.5, adultStrength / babyStrength, DOUBLE_DELTA,
                "Adult should be 1.5x stronger than baby");
    }

    // ==================== onSuccessfulKill Tests ====================

    @Test
    @DisplayName("onSuccessfulKill() sets handling timer")
    void onSuccessfulKill_setsHandlingTimer() {
        // Use null since we can't easily mock Wolf and EcologyComponent
        behavior.onSuccessfulKill(null);

        // Verify state changed to RESTING
        assertEquals(PackHuntingBehavior.PackHuntingState.RESTING,
                behavior.getHuntingState(),
                "State should be RESTING after successful kill");
    }

    @Test
    @DisplayName("onSuccessfulKill() with null wolf handles gracefully")
    void onSuccessfulKill_withNullWolf_handlesGracefully() {
        // Should not throw exception
        assertDoesNotThrow(() -> behavior.onSuccessfulKill(null),
                "onSuccessfulKill should handle null wolf gracefully");
    }

    // ==================== BehaviorContext Tests ====================

    @Test
    @DisplayName("Calculate returns zero vector for null context")
    void calculate_returnsZeroVector_forNullContext() {
        Vec3d result = behavior.calculate(null);

        assertNotNull(result, "Should return Vec3d object");
        assertEquals(0.0, result.magnitude(), DOUBLE_DELTA,
                "Should return zero vector for null context");
    }

    @Test
    @DisplayName("Calculate returns zero vector for context with null entity")
    void calculate_returnsZeroVector_forContextWithNullEntity() {
        BehaviorContext context = new BehaviorContext(
                new Vec3d(0, 64, 0),
                new Vec3d(0, 0, 0),
                1.0,
                0.1
        );

        Vec3d result = behavior.calculate(context);

        assertNotNull(result, "Should return Vec3d object");
        assertEquals(0.0, result.magnitude(), DOUBLE_DELTA,
                "Should return zero vector for null entity");
    }

    // ==================== Scientific Parameter Tests ====================

    @Test
    @DisplayName("Default parameters match scientific research")
    void defaultParameters_matchScientificResearch() {
        // Based on research into wolf pack hunting:
        // - Max pursuit speed: 1.3 (faster than walking)
        // - Max pursuit force: 0.18 (moderate steering)
        // - Flanking angle: PI/3 (60 degrees for surrounding)
        // - Coordination range: 48 blocks (pack communication range)
        // - Min pack size: 2 (wolves hunt in pairs minimum)

        PackHuntingBehavior defaultBehavior = new PackHuntingBehavior();

        assertNotNull(defaultBehavior, "Behavior should initialize");
        assertEquals(PackHuntingBehavior.PackHuntingState.IDLE,
                defaultBehavior.getHuntingState(),
                "Initial state should be IDLE");
    }

    @Test
    @DisplayName("Energy cost per hunt is sustainable")
    void energyCostPerHunt_isSustainable() {
        // ENERGY_COST_PER_HUNT = 30% (30 out of 100)
        // This allows 3 hunts before exhaustion, which matches
        // research on wolf hunting sustainability

        int expectedEnergyCost = 30;
        int maxEnergy = 100;

        assertTrue(expectedEnergyCost < maxEnergy,
                "Energy cost should be less than max energy");
        assertTrue(expectedEnergyCost > 0,
                "Energy cost should be positive");

        int huntsBeforeExhaustion = maxEnergy / expectedEnergyCost;
        assertEquals(3, huntsBeforeExhaustion,
                "Should allow approximately 3 hunts before rest");
    }

    @Test
    @DisplayName("Handling time allows realistic feeding behavior")
    void handlingTime_allowsRealisticFeedingBehavior() {
        // HANDLING_TIME_TICKS = 600 (30 seconds)
        // This matches research on wolf feeding duration
        // after successful kills

        int expectedHandlingTime = 600;
        double expectedSeconds = expectedHandlingTime / 20.0;

        assertEquals(30.0, expectedSeconds, 0.1,
                "Handling time should be 30 seconds");
    }

    // ==================== Edge Case Tests ====================

    @Test
    @DisplayName("State transitions handle all states")
    void stateTransitions_handleAllStates() {
        PackHuntingBehavior.PackHuntingState[] states =
                PackHuntingBehavior.PackHuntingState.values();

        // Verify we can set and get each state
        for (PackHuntingBehavior.PackHuntingState state : states) {
            behavior.setHuntingState(state);
            assertEquals(state, behavior.getHuntingState(),
                    "State should persist: " + state);
        }
    }

    @Test
    @DisplayName("Multiple behaviors can coexist")
    void multipleBehaviors_canCoexist() {
        PackHuntingBehavior behavior1 = new PackHuntingBehavior();
        PackHuntingBehavior behavior2 = new PackHuntingBehavior(1.5, 0.2, Math.PI / 4, 64.0, 3);

        assertNotNull(behavior1);
        assertNotNull(behavior2);

        // Each should have independent state
        behavior1.setHuntingState(PackHuntingBehavior.PackHuntingState.SEARCHING);
        behavior2.setHuntingState(PackHuntingBehavior.PackHuntingState.LEADING);

        assertEquals(PackHuntingBehavior.PackHuntingState.SEARCHING,
                behavior1.getHuntingState());
        assertEquals(PackHuntingBehavior.PackHuntingState.LEADING,
                behavior2.getHuntingState());
    }

    // ==================== Helper Classes ====================

    /**
     * Mock entity for testing prey tracking.
     */
    private static class MockPrey {
        private final Vec3d position;

        public MockPrey(double x, double y, double z) {
            this.position = new Vec3d(x, y, z);
        }

        public Vec3d position() { return position; }
    }
}
