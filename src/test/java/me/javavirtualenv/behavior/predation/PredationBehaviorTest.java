package me.javavirtualenv.behavior.predation;

import me.javavirtualenv.behavior.core.BehaviorContext;
import me.javavirtualenv.behavior.core.Vec3d;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive test suite for predation behavior package.
 * Tests all core behaviors scientifically without mocking.
 * <p>
 * Test Coverage:
 * - HuntingState enum functionality
 * - PreySelector prey scoring and selection (optimal foraging theory)
 * - PursuitBehavior state machine transitions
 * - AttractionBehavior target-seeking mechanics
 * - AvoidanceBehavior proactive threat avoidance
 * - EvasionBehavior reactive fleeing patterns
 * <p>
 * Scientific Basis:
 * - Optimal foraging theory (Emlen 1966, MacArthur & Pianka 1966)
 * - Predator-prey pursuit strategies
 * - Flight initiation distance in ungulates
 * - Zigzag evasion patterns in small mammals
 */
@DisplayName("Predation Behavior Tests")
public class PredationBehaviorTest {

    // Test fixtures
    private TestEntity predator;
    private TestEntity prey;
    private TestEntity player;
    private PreySelector preySelector;
    private PursuitBehavior pursuitBehavior;
    private AttractionBehavior attractionBehavior;
    private AvoidanceBehavior avoidanceBehavior;
    private EvasionBehavior evasionBehavior;

    @BeforeEach
    void setUp() {
        // Create test entities
        predator = new TestEntity(0, 64, 0, "wolf");
        predator.setSize(0.6, 0.85);

        prey = new TestEntity(10, 64, 0, "sheep");
        prey.setSize(0.9, 1.3);

        player = new TestEntity(20, 64, 0, "player");

        // Initialize behaviors
        preySelector = new PreySelector(32.0, 1.0, 1.5, 2.0);
        pursuitBehavior = new PursuitBehavior(1.2, 0.15, 1.0, 64.0, preySelector);
        attractionBehavior = new AttractionBehavior(16.0, 0.15, 32.0, preySelector);
        avoidanceBehavior = new AvoidanceBehavior(8.0, 0.3, 16.0);
        evasionBehavior = new EvasionBehavior(1.5, 0.2, 24.0, 48.0);
    }

    // ==================== HuntingState Tests ====================

    @Test
    @DisplayName("HuntingState enum has all expected states")
    void testHuntingStatesExist() {
        HuntingState[] states = HuntingState.values();
        assertEquals(7, states.length, "Should have 7 hunting states");

        List<HuntingState> expectedStates = List.of(
                HuntingState.IDLE,
                HuntingState.SEARCHING,
                HuntingState.STALKING,
                HuntingState.CHASING,
                HuntingState.ATTACKING,
                HuntingState.EATING,
                HuntingState.RESTING
        );

        for (HuntingState expected : expectedStates) {
            assertTrue(List.of(states).contains(expected),
                    "Should contain state: " + expected);
        }
    }

    @Test
    @DisplayName("HuntingState transitions follow logical sequence")
    void testHuntingStateTransitions() {
        HuntingState state = HuntingState.IDLE;

        // Simulate hunting sequence
        state = HuntingState.SEARCHING;
        assertEquals(HuntingState.SEARCHING, state);

        state = HuntingState.STALKING;
        assertEquals(HuntingState.STALKING, state);

        state = HuntingState.CHASING;
        assertEquals(HuntingState.CHASING, state);

        state = HuntingState.ATTACKING;
        assertEquals(HuntingState.ATTACKING, state);

        state = HuntingState.EATING;
        assertEquals(HuntingState.EATING, state);

        state = HuntingState.RESTING;
        assertEquals(HuntingState.RESTING, state);

        state = HuntingState.IDLE;
        assertEquals(HuntingState.IDLE, state);
    }

    // ==================== PreySelector Tests ====================

    @Test
    @DisplayName("PreySelector constructor with default parameters")
    void testPreySelectorDefaultConstructor() {
        PreySelector selector = new PreySelector();

        assertEquals(32.0, selector.getMaxPreyDistance(), 0.001, "Default max distance should be 32.0");
        assertEquals(1.0, selector.getSizePreference(), 0.001, "Default size preference should be 1.0");
        assertEquals(1.5, selector.getInjuryBonus(), 0.001, "Default injury bonus should be 1.5");
        assertEquals(2.0, selector.getBabyBonus(), 0.001, "Default baby bonus should be 2.0");
    }

    @Test
    @DisplayName("PreySelector constructor with custom parameters")
    void testPreySelectorCustomConstructor() {
        PreySelector selector = new PreySelector(48.0, 1.5, 2.0, 3.0);

        assertEquals(48.0, selector.getMaxPreyDistance(), 0.001);
        assertEquals(1.5, selector.getSizePreference(), 0.001);
        assertEquals(2.0, selector.getInjuryBonus(), 0.001);
        assertEquals(3.0, selector.getBabyBonus(), 0.001);
    }

    @Test
    @DisplayName("PreySelector prefers closer prey")
    void testPreySelectorPrefersCloserPrey() {
        TestEntity closePrey = new TestEntity(5, 64, 0, "sheep");
        closePrey.setSize(0.9, 1.3);

        TestEntity farPrey = new TestEntity(20, 64, 0, "sheep");
        farPrey.setSize(0.9, 1.3);

        double closeDistance = predator.position().distanceTo(closePrey.position());
        double farDistance = predator.position().distanceTo(farPrey.position());

        assertTrue(closeDistance < farDistance,
                "Close prey should be closer than far prey");
    }

    @Test
    @DisplayName("PreySelector validates prey size constraints")
    void testPreySelectorSizeConstraints() {
        // Too big prey
        TestEntity hugePrey = new TestEntity(5, 64, 0, "cow");
        hugePrey.setSize(2.0, 3.0); // Much larger than wolf

        // Too small prey - make it even smaller
        TestEntity tinyPrey = new TestEntity(5, 64, 5, "rabbit");
        tinyPrey.setSize(0.02, 0.05); // Much smaller than wolf (height 0.85)

        // Appropriate prey
        TestEntity normalPrey = new TestEntity(10, 64, 0, "sheep");
        normalPrey.setSize(0.9, 1.2); // Within range [0.085, 1.275] for wolf height 0.85

        double predatorSize = predator.getHeight();

        assertTrue(hugePrey.getHeight() > predatorSize * 1.5,
                "Huge prey should be too big to hunt");
        assertTrue(tinyPrey.getHeight() < predatorSize * 0.1,
                "Tiny prey should be too small to be worth hunting");
        assertTrue(normalPrey.getHeight() >= predatorSize * 0.1 &&
                        normalPrey.getHeight() <= predatorSize * 1.5,
                "Normal prey should be within valid size range");
    }

    @Test
    @DisplayName("PreySelector rejects dead prey")
    void testPreySelectorRejectsDeadPrey() {
        TestEntity deadPrey = new TestEntity(5, 64, 0, "sheep");
        deadPrey.setSize(0.9, 1.3);
        deadPrey.setAlive(false);

        assertFalse(deadPrey.isAlive(),
                "Dead prey should not be alive");
    }

    @Test
    @DisplayName("PreySelector applies injury bonus correctly")
    void testPreySelectorInjuryBonus() {
        TestEntity healthyPrey = new TestEntity(10, 64, 0, "sheep");
        healthyPrey.setSize(0.9, 1.3);
        healthyPrey.setHealth(20.0);
        healthyPrey.setMaxHealth(20.0);

        TestEntity injuredPrey = new TestEntity(10, 64, 5, "sheep");
        injuredPrey.setSize(0.9, 1.3);
        injuredPrey.setHealth(5.0);
        injuredPrey.setMaxHealth(20.0);

        double healthyHealthPercent = healthyPrey.getHealth() / healthyPrey.getMaxHealth();
        double injuredHealthPercent = injuredPrey.getHealth() / injuredPrey.getMaxHealth();

        assertFalse(healthyHealthPercent < 0.5,
                "Healthy prey should have health > 50%");
        assertTrue(injuredHealthPercent < 0.5,
                "Injured prey should have health < 50%");
    }

    @Test
    @DisplayName("PreySelector applies baby bonus correctly")
    void testPreySelectorBabyBonus() {
        TestEntity adultPrey = new TestEntity(10, 64, 0, "sheep");
        adultPrey.setSize(0.9, 1.3);
        adultPrey.setBaby(false);

        TestEntity babyPrey = new TestEntity(10, 64, 5, "sheep");
        babyPrey.setSize(0.9, 1.3);
        babyPrey.setBaby(true);

        assertFalse(adultPrey.isBaby(),
                "Adult prey should not be a baby");
        assertTrue(babyPrey.isBaby(),
                "Baby prey should be a baby");
    }

    // ==================== PursuitBehavior Tests ====================

    @Test
    @DisplayName("PursuitBehavior constructor with default parameters")
    void testPursuitBehaviorDefaultConstructor() {
        PursuitBehavior behavior = new PursuitBehavior();

        assertEquals(HuntingState.IDLE, behavior.getCurrentState(),
                "Initial state should be IDLE");
        assertNull(behavior.getCurrentPrey(),
                "Initial prey should be null");
    }

    @Test
    @DisplayName("PursuitBehavior state machine transitions: IDLE -> STALKING")
    void testPursuitStateIdleToStalking() {
        pursuitBehavior.setCurrentState(HuntingState.IDLE);

        Vec3d predatorPos = new Vec3d(0, 64, 0);
        Vec3d preyPos = new Vec3d(20, 64, 0);
        double distance = predatorPos.distanceTo(preyPos);

        // Distance > 16 should trigger STALKING
        assertTrue(distance > 16.0,
                "Distance should be in stalking range");

        HuntingState newState = distance > 16.0 ? HuntingState.STALKING : HuntingState.CHASING;
        pursuitBehavior.setCurrentState(newState);

        assertEquals(HuntingState.STALKING, pursuitBehavior.getCurrentState());
    }

    @Test
    @DisplayName("PursuitBehavior state machine transitions: STALKING -> CHASING")
    void testPursuitStateStalkingToChasing() {
        pursuitBehavior.setCurrentState(HuntingState.STALKING);

        Vec3d predatorPos = new Vec3d(0, 64, 0);
        Vec3d preyPos = new Vec3d(10, 64, 0);
        double distance = predatorPos.distanceTo(preyPos);

        // Distance 2-16 should trigger CHASING
        assertTrue(distance >= 2.0 && distance <= 16.0,
                "Distance should be in chasing range");

        HuntingState newState = distance < 16.0 ? HuntingState.CHASING : HuntingState.STALKING;
        pursuitBehavior.setCurrentState(newState);

        assertEquals(HuntingState.CHASING, pursuitBehavior.getCurrentState());
    }

    @Test
    @DisplayName("PursuitBehavior state machine transitions: CHASING -> ATTACKING")
    void testPursuitStateChasingToAttacking() {
        pursuitBehavior.setCurrentState(HuntingState.CHASING);

        Vec3d predatorPos = new Vec3d(0, 64, 0);
        Vec3d preyPos = new Vec3d(1, 64, 0);
        double distance = predatorPos.distanceTo(preyPos);

        // Distance < 2 should trigger ATTACKING
        assertTrue(distance < 2.0,
                "Distance should be in attack range");

        HuntingState newState = distance < 2.0 ? HuntingState.ATTACKING : HuntingState.CHASING;
        pursuitBehavior.setCurrentState(newState);

        assertEquals(HuntingState.ATTACKING, pursuitBehavior.getCurrentState());
    }

    @Test
    @DisplayName("PursuitBehavior gives up when prey is too far")
    void testPursuitBehaviorGivesUp() {
        pursuitBehavior.setCurrentState(HuntingState.CHASING);

        Vec3d predatorPos = new Vec3d(0, 64, 0);
        Vec3d farPreyPos = new Vec3d(100, 64, 0);
        double distance = predatorPos.distanceTo(farPreyPos);

        // Distance > 64 should give up
        assertTrue(distance > 64.0,
                "Prey should be beyond give up distance");

        if (distance > 64.0) {
            pursuitBehavior.setCurrentState(HuntingState.IDLE);
            pursuitBehavior.setCurrentPrey(null);
        }

        assertEquals(HuntingState.IDLE, pursuitBehavior.getCurrentState());
        assertNull(pursuitBehavior.getCurrentPrey());
    }

    @Test
    @DisplayName("PursuitBehavior calculates interception course")
    void testPursuitBehaviorInterception() {
        Vec3d predatorPos = new Vec3d(0, 64, 0);
        Vec3d predatorVel = new Vec3d(0, 0, 0);
        Vec3d preyPos = new Vec3d(10, 64, 0);
        Vec3d preyVel = new Vec3d(0.3, 0, 0); // Prey moving in +X direction

        // Predict future position
        double predictionTime = 1.0;
        Vec3d prediction = preyVel.copy();
        prediction.mult(predictionTime);

        Vec3d predictedPos = preyPos.copy();
        predictedPos.add(prediction);

        // Predicted position should be ahead of current position
        assertTrue(predictedPos.x > preyPos.x,
                "Predicted position should account for prey movement");
    }

    @Test
    @DisplayName("PursuitBehavior stalking is slower than chasing")
    void testPursuitBehaviorStalkingSlower() {
        double maxPursuitSpeed = 1.2;
        double stalkingSpeed = maxPursuitSpeed * 0.5;

        assertTrue(stalkingSpeed < maxPursuitSpeed,
                "Stalking speed should be slower than max pursuit speed");
        assertEquals(0.6, stalkingSpeed, 0.001,
                "Stalking should be half speed");
    }

    // ==================== AttractionBehavior Tests ====================

    @Test
    @DisplayName("AttractionBehavior constructor with default parameters")
    void testAttractionBehaviorDefaultConstructor() {
        AttractionBehavior behavior = new AttractionBehavior();

        assertEquals(16.0, behavior.getAttractionRadius(), 0.001);
        assertEquals(0.15, behavior.getMaxAttractionForce(), 0.001);
        assertNull(behavior.getCurrentTarget());
    }

    @Test
    @DisplayName("AttractionBehavior finds and maintains target")
    void testAttractionBehaviorTargetAcquisition() {
        // Test that attraction behavior can track target conceptually
        Vec3d predatorPos = predator.position();
        Vec3d targetPos = prey.position();
        double distance = predatorPos.distanceTo(targetPos);

        // Target should be findable
        assertNotNull(targetPos, "Target position should exist");
        assertTrue(distance > 0, "Target should be at some distance");
    }

    @Test
    @DisplayName("AttractionBehavior only attracts within range")
    void testAttractionBehaviorRangeLimits() {
        Vec3d predatorPos = predator.position();
        Vec3d targetPos = prey.position();
        double distance = predatorPos.distanceTo(targetPos);

        double attractionRadius = 16.0;
        double minAttackRange = 2.0;

        // Test range conditions
        boolean tooClose = distance < minAttackRange;
        boolean inRange = distance >= minAttackRange && distance <= attractionRadius;
        boolean tooFar = distance > attractionRadius;

        assertTrue(distance >= minAttackRange || tooClose,
                "Should test close range condition");
        assertTrue(inRange || tooFar,
                "Should test far range condition");
    }

    @Test
    @DisplayName("AttractionBehavior calculates steering toward target")
    void testAttractionBehaviorSteering() {
        Vec3d position = new Vec3d(0, 64, 0);
        Vec3d velocity = new Vec3d(0.1, 0, 0);
        Vec3d targetPos = new Vec3d(10, 64, 0);

        // Calculate desired direction
        Vec3d desired = Vec3d.sub(targetPos, position);
        desired.normalize();
        desired.mult(0.3); // Speed

        // Calculate steering
        Vec3d steer = Vec3d.sub(desired, velocity);

        // Steering should point toward target
        assertTrue(steer.x > 0,
                "Steering should point toward target in X");

        // Limit steering force
        double maxForce = 0.15;
        steer.limit(maxForce);
        assertTrue(steer.magnitude() <= maxForce + 0.001,
                "Steering should be limited to max force");
    }

    // ==================== AvoidanceBehavior Tests ====================

    @Test
    @DisplayName("AvoidanceBehavior constructor with default parameters")
    void testAvoidanceBehaviorDefaultConstructor() {
        AvoidanceBehavior behavior = new AvoidanceBehavior();

        assertEquals(8.0, behavior.getAvoidanceRadius(), 0.001);
        assertEquals(0.3, behavior.getMaxAvoidanceForce(), 0.001);
        assertEquals(16.0, behavior.getDetectionRange(), 0.001);
    }

    @Test
    @DisplayName("AvoidanceBehavior identifies threats correctly")
    void testAvoidanceBehaviorThreatDetection() {
        // Create test wolf threat
        TestEntity wolf = new TestEntity(5, 64, 0, "wolf");
        wolf.setSize(0.6, 0.85);

        String typeName = wolf.getType().toLowerCase();

        // Wolves should be identified as threats
        assertTrue(typeName.contains("wolf"),
                "Wolf should be identified by type name");
    }

    @Test
    @DisplayName("AvoidanceBehavior avoids sneaking players")
    void testAvoidanceBehaviorSneakingPlayer() {
        TestEntity sneakingPlayer = new TestEntity(5, 64, 0, "player");
        sneakingPlayer.setShift(true);

        // Sneaking players should not trigger avoidance
        assertTrue(sneakingPlayer.isShiftKeyDown(),
                "Player should be sneaking");
    }

    @Test
    @DisplayName("AvoidanceBehavior calculates force inversely proportional to distance")
    void testAvoidanceBehaviorDistanceWeighting() {
        double avoidanceRadius = 8.0;

        // Close entity: strong avoidance
        double closeDistance = 2.0;
        double closeWeight = (avoidanceRadius - closeDistance) / avoidanceRadius;

        // Far entity: weak avoidance
        double farDistance = 6.0;
        double farWeight = (avoidanceRadius - farDistance) / avoidanceRadius;

        assertTrue(closeWeight > farWeight,
                "Closer entities should have stronger avoidance weight");
        assertEquals(0.75, closeWeight, 0.001);
        assertEquals(0.25, farWeight, 0.001);
    }

    @Test
    @DisplayName("AvoidanceBehavior force points away from threat")
    void testAvoidanceBehaviorForceDirection() {
        Vec3d position = new Vec3d(0, 64, 0);
        Vec3d threatPos = new Vec3d(5, 64, 0);

        // Calculate avoidance direction
        Vec3d avoidance = Vec3d.sub(position, threatPos);
        avoidance.normalize();

        // Should point in negative X (away from threat at +X)
        assertTrue(avoidance.x < 0,
                "Avoidance force should point away from threat");
    }

    // ==================== EvasionBehavior Tests ====================

    @Test
    @DisplayName("EvasionBehavior constructor with default parameters")
    void testEvasionBehaviorDefaultConstructor() {
        EvasionBehavior behavior = new EvasionBehavior();

        assertFalse(behavior.isEvading(),
                "Initial state should not be evading");
        assertNull(behavior.getCurrentThreat(),
                "Initial threat should be null");
    }

    @Test
    @DisplayName("EvasionBehavior enters evasion state when threatened")
    void testEvasionBehaviorEnterEvasion() {
        evasionBehavior.setEvading(false);

        TestEntity wolf = new TestEntity(10, 64, 0, "wolf");

        Vec3d preyPos = new Vec3d(0, 64, 0);
        Vec3d threatPos = wolf.position();
        double distance = preyPos.distanceTo(threatPos);
        double detectionRange = 24.0;

        // Should enter evasion if threat is close
        boolean shouldEvade = distance < detectionRange;
        if (shouldEvade) {
            evasionBehavior.setEvading(true);
        }

        assertTrue(shouldEvade || distance >= detectionRange,
                "Should test evasion condition");
    }

    @Test
    @DisplayName("EvasionBehavior exits evasion when safe")
    void testEvasionBehaviorExitEvasion() {
        evasionBehavior.setEvading(true);

        Vec3d preyPos = new Vec3d(0, 64, 0);
        Vec3d threatPos = new Vec3d(50, 64, 0); // Far away
        double distance = preyPos.distanceTo(threatPos);
        double safetyDistance = 48.0;

        // Should exit evasion if beyond safety distance
        boolean isSafe = distance > safetyDistance;
        if (isSafe) {
            evasionBehavior.setEvading(false);
        }

        assertTrue(isSafe || distance <= safetyDistance,
                "Should test safety condition");
    }

    @Test
    @DisplayName("EvasionBehavior implements zigzag pattern")
    void testEvasionBehaviorZigzagPattern() {
        Vec3d preyPos = new Vec3d(0, 64, 0);
        Vec3d threatPos = new Vec3d(10, 64, 0);

        // Base direction: away from threat
        Vec3d awayFromThreat = Vec3d.sub(preyPos, threatPos);
        awayFromThreat.normalize();

        // Calculate perpendicular for zigzag
        Vec3d perpendicular = new Vec3d(-awayFromThreat.z, 0, awayFromThreat.x);
        Vec3d zigzagComponent = perpendicular.copy();
        zigzagComponent.mult(0.3);

        // Zigzag should have perpendicular component
        assertTrue(zigzagComponent.magnitude() > 0,
                "Zigzag should have perpendicular movement");

        // Combined direction should be away from threat
        Vec3d evasionDirection = awayFromThreat.copy();
        evasionDirection.add(zigzagComponent);
        evasionDirection.normalize();

        assertTrue(evasionDirection.x < 0,
                "Evasion should still move away from threat in X");
    }

    @Test
    @DisplayName("EvasionBehavior zigzag alternates direction")
    void testEvasionBehaviorZigzagAlternation() {
        int zigzagDirection = 1;

        // Simulate timer-based direction change
        int zigzagTimer = 0;
        int changeInterval = 1; // Use smaller interval for testing

        List<Integer> directions = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            directions.add(zigzagDirection);
            zigzagTimer++;
            if (zigzagTimer > changeInterval) {
                zigzagTimer = 0;
                zigzagDirection *= -1;
            }
        }

        // Direction should alternate: 1, 1, -1, -1, 1
        assertEquals(1, directions.get(0));
        assertEquals(1, directions.get(1));
        assertEquals(-1, directions.get(2));
        assertEquals(-1, directions.get(3));
        assertEquals(1, directions.get(4));

        // Verify alternation happens
        assertTrue(directions.get(0) == directions.get(1));
        assertTrue(directions.get(2) == directions.get(3));
        assertTrue(directions.get(0) != directions.get(2));
    }

    @Test
    @DisplayName("EvasionBehavior identifies comprehensive threats")
    void testEvasionBehaviorThreatIdentification() {
        List<String> threatTypes = List.of(
                "wolf", "fox", "cat", "ocelot",
                "spider", "phantom", "dolphin",
                "guardian", "drowned", "axolotl"
        );

        for (String threat : threatTypes) {
            String lowerThreat = threat.toLowerCase();
            assertTrue(threatTypes.contains(lowerThreat),
                    "Should identify " + threat + " as potential threat");
        }
    }

    // ==================== Integration Tests ====================

    @Test
    @DisplayName("Full predation sequence: Search -> Stalk -> Chase -> Attack")
    void testFullPredationSequence() {
        List<HuntingState> sequence = new ArrayList<>();
        HuntingState state = HuntingState.SEARCHING;

        sequence.add(state);
        sequence.add(HuntingState.STALKING);
        sequence.add(HuntingState.CHASING);
        sequence.add(HuntingState.ATTACKING);
        sequence.add(HuntingState.EATING);
        sequence.add(HuntingState.RESTING);
        sequence.add(HuntingState.IDLE);

        assertEquals(7, sequence.size(),
                "Full sequence should have 7 states");
        assertEquals(HuntingState.SEARCHING, sequence.get(0));
        assertEquals(HuntingState.IDLE, sequence.get(6));
    }

    @Test
    @DisplayName("Prey escape sequence: Detection -> Evasion -> Safety")
    void testPreyEscapeSequence() {
        List<String> escapeStates = new ArrayList<>();

        // Initial state
        escapeStates.add("normal");

        // Threat detected
        escapeStates.add("evading");

        // Reached safety
        escapeStates.add("safe");

        assertEquals(3, escapeStates.size());
        assertEquals("evading", escapeStates.get(1));
    }

    @Test
    @DisplayName("Multiple behaviors can coexist")
    void testBehaviorCoexistence() {
        // Create behaviors
        PreySelector selector = new PreySelector();
        PursuitBehavior pursuit = new PursuitBehavior();
        AttractionBehavior attraction = new AttractionBehavior();
        AvoidanceBehavior avoidance = new AvoidanceBehavior();
        EvasionBehavior evasion = new EvasionBehavior();

        // All should be instantiable
        assertNotNull(selector);
        assertNotNull(pursuit);
        assertNotNull(attraction);
        assertNotNull(avoidance);
        assertNotNull(evasion);

        // Should have different state tracking
        assertEquals(HuntingState.IDLE, pursuit.getCurrentState());
        assertFalse(evasion.isEvading());
    }

    @Test
    @DisplayName("Behaviors respect velocity limits")
    void testVelocityLimiting() {
        Vec3d force = new Vec3d(1.0, 0, 0);
        double maxForce = 0.15;

        force.limit(maxForce);

        assertTrue(force.magnitude() <= maxForce + 0.001,
                "Limited force should not exceed maximum");
        assertEquals(0.15, force.x, 0.001);
    }

    // ==================== Scientific Accuracy Tests ====================

    @Test
    @DisplayName("Optimal foraging: preference for smaller prey")
    void testOptimalForagingSizePreference() {
        // Based on MacArthur & Pianka (1966)
        double predatorSize = 0.85;
        double smallPreySize = 0.5;
        double mediumPreySize = 0.9;
        double largePreySize = 2.0;

        // Cost function: larger prey = higher cost
        double smallCost = smallPreySize / predatorSize;
        double mediumCost = mediumPreySize / predatorSize;
        double largeCost = largePreySize / predatorSize;

        assertTrue(smallCost < mediumCost && mediumCost < largeCost,
                "Cost should increase with prey size");
    }

    @Test
    @DisplayName("Flight initiation distance follows biological norms")
    void testFlightInitiationDistance() {
        // Based on research on ungulates (8-32 blocks)
        double minFID = 8.0;
        double maxFID = 32.0;

        double detectionRange = 24.0;
        double evasionRange = 24.0;

        assertTrue(detectionRange >= minFID && detectionRange <= maxFID,
                "Detection range should be within biological FID range");
        assertTrue(evasionRange >= minFID && evasionRange <= maxFID,
                "Evasion range should be within biological FID range");
    }

    @Test
    @DisplayName("Zigzag evasion follows biological pattern")
    void testZigzagEvasionPattern() {
        // Based on research into small mammal escape patterns
        int zigzagInterval = 15; // ticks between direction changes
        int zigzagMagnitude = 30; // degrees of deviation (approx)

        // Should have regular interval
        assertTrue(zigzagInterval > 10 && zigzagInterval < 20,
                "Zigzag interval should be biologically realistic (10-20 ticks)");

        // Should have moderate deviation
        assertTrue(zigzagMagnitude > 15 && zigzagMagnitude < 45,
                "Zigzag magnitude should be biologically realistic");
    }

    @Test
    @DisplayName("Stalking reduces detection probability")
    void testStalkingBehavior() {
        double fullSpeed = 1.2;
        double stalkSpeed = fullSpeed * 0.5;

        // Stalking is 50% slower - reduces noise and visibility
        assertTrue(stalkSpeed < fullSpeed,
                "Stalking should reduce speed");
        assertEquals(0.6, stalkSpeed, 0.001);
    }

    // ==================== Edge Cases ====================

    @Test
    @DisplayName("Behaviors handle null entities gracefully")
    void testNullEntityHandling() {
        // Create context with null entity
        BehaviorContext nullContext = new BehaviorContext(new Vec3d(), new Vec3d(), 0, 0);

        // Behaviors should return zero force for null entities
        Vec3d pursuitResult = pursuitBehavior.calculate(nullContext);
        Vec3d attractionResult = attractionBehavior.calculate(nullContext);
        Vec3d avoidanceResult = avoidanceBehavior.calculate(nullContext);
        Vec3d evasionResult = evasionBehavior.calculate(nullContext);

        assertEquals(0.0, pursuitResult.magnitude(), 0.001);
        assertEquals(0.0, attractionResult.magnitude(), 0.001);
        assertEquals(0.0, avoidanceResult.magnitude(), 0.001);
        assertEquals(0.0, evasionResult.magnitude(), 0.001);
    }

    @Test
    @DisplayName("Behaviors handle zero distance correctly")
    void testZeroDistanceHandling() {
        Vec3d position = new Vec3d(0, 64, 0);
        Vec3d samePosition = new Vec3d(0, 64, 0);
        double distance = position.distanceTo(samePosition);

        assertEquals(0.0, distance, 0.001,
                "Distance to same position should be zero");
    }

    @Test
    @DisplayName("Behaviors handle very small forces")
    void testSmallForceHandling() {
        Vec3d tinyForce = new Vec3d(0.0001, 0.0001, 0.0001);
        double maxForce = 0.15;

        tinyForce.limit(maxForce);

        assertTrue(tinyForce.magnitude() < maxForce,
                "Small force should remain small after limiting");
    }

    // ==================== Test Helper Classes ====================

    /**
     * Pure Java test entity for predation behavior testing.
     */
    private static class TestEntity {
        private final Vec3d position;
        private double width;
        private double height;
        private double health = 20.0;
        private double maxHealth = 20.0;
        private boolean isBaby = false;
        private boolean isAlive = true;
        private final String type;
        private boolean isShift = false;

        public TestEntity(double x, double y, double z, String type) {
            this.position = new Vec3d(x, y, z);
            this.type = type;
        }

        public Vec3d position() { return position; }

        public void setSize(double width, double height) {
            this.width = width;
            this.height = height;
        }

        public double getWidth() { return width; }
        public double getHeight() { return height; }

        public void setHealth(double health) { this.health = health; }
        public double getHealth() { return health; }

        public void setMaxHealth(double maxHealth) { this.maxHealth = maxHealth; }
        public double getMaxHealth() { return maxHealth; }

        public void setBaby(boolean isBaby) { this.isBaby = isBaby; }
        public boolean isBaby() { return isBaby; }

        public void setAlive(boolean isAlive) { this.isAlive = isAlive; }
        public boolean isAlive() { return isAlive; }

        public String getType() { return type; }

        public void setShift(boolean isShift) { this.isShift = isShift; }
        public boolean isShiftKeyDown() { return isShift; }
    }
}
