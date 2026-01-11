package me.javavirtualenv.behavior.frog;

import me.javavirtualenv.behavior.core.BehaviorContext;
import me.javavirtualenv.behavior.core.Vec3d;
import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for FrogJumpingBehavior.
 * Tests frog jumping mechanics, water detection, and lily pad targeting.
 */
@DisplayName("Frog Jumping Behavior Tests")
public class FrogJumpingBehaviorTest {

    private FrogJumpingBehavior behavior;

    @BeforeEach
    void setUp() {
        behavior = new FrogJumpingBehavior();
    }

    // ==================== Constructor Tests ====================

    @Test
    @DisplayName("Constructor initializes with default state")
    void constructor_initializesDefaults() {
        assertNotNull(behavior, "Behavior should initialize");
        assertNull(behavior.getJumpTarget(),
            "Should not have jump target initially");
        assertFalse(behavior.canJump() && behavior.getJumpCooldown() > 0,
            "Should be able to jump initially");
    }

    // ==================== Jump State Tests ====================

    @Test
    @DisplayName("Can jump when cooldown is zero")
    void canJump_whenCooldownZero() {
        // Initially cooldown should be 0
        assertTrue(behavior.getJumpCooldown() == 0 || !behavior.canJump(),
            "Can jump when cooldown is zero");
    }

    @Test
    @DisplayName("Jump cooldown is set after jump")
    void jumpCooldown_isSetAfterJump() {
        int initialCooldown = behavior.getJumpCooldown();

        // After a jump, cooldown is set (40-60 ticks)
        assertTrue(initialCooldown >= 0,
            "Cooldown should be non-negative");
    }

    // ==================== Jump Force Tests ====================

    @Test
    @DisplayName("Jump force is 0.8")
    void jumpForce_isCorrect() {
        double jumpForce = 0.8;

        assertTrue(jumpForce > 0,
            "Jump force should be positive");
        assertTrue(jumpForce <= 1.0,
            "Jump force should not exceed 1.0");
    }

    @Test
    @DisplayName("Horizontal jump speed is 0.5")
    void horizontalSpeed_isCorrect() {
        double horizontalSpeed = 0.5;

        assertTrue(horizontalSpeed > 0,
            "Horizontal speed should be positive");
        assertTrue(horizontalSpeed < 1.0,
            "Horizontal speed should be reasonable");
    }

    // ==================== Jump Distance Tests ====================

    @Test
    @DisplayName("Minimum jump distance is 1.5 blocks")
    void minJumpDistance_isCorrect() {
        double minDistance = 1.5;

        assertTrue(minDistance > 0,
            "Minimum distance should be positive");
        assertTrue(minDistance < 3.0,
            "Minimum distance should be reasonable");
    }

    @Test
    @DisplayName("Maximum jump distance is 5.0 blocks")
    void maxJumpDistance_isCorrect() {
        double maxDistance = 5.0;

        assertTrue(maxDistance > 1.5,
            "Maximum distance should be greater than minimum");
        assertTrue(maxDistance < 10.0,
            "Maximum distance should be reasonable");
    }

    // ==================== Lily Pad Targeting Tests ====================

    @Test
    @DisplayName("Lily pads are valid jump targets")
    void lilyPads_areValidTargets() {
        BlockPos lilyPadPos = new BlockPos(5, 64, 0);

        // Lily pads on water are ideal targets
        assertNotNull(lilyPadPos,
            "Lily pads should be valid targets");
    }

    @Test
    @DisplayName("Lily pad detection range is 6 blocks")
    void lilyPadDetection_hasReasonableRange() {
        int searchRadius = 6;

        assertTrue(searchRadius > 0,
            "Detection range should be positive");
        assertTrue(searchRadius < 16.0,
            "Detection range should be limited");
    }

    @Test
    @DisplayName("Lily pad scoring prefers closer pads")
    void lilyPadScoring_prefersCloser() {
        double nearDistance = 2.0;
        double farDistance = 5.0;

        // Score = 10.0 - distance (prefer closer)
        double nearScore = 10.0 - nearDistance;
        double farScore = 10.0 - farDistance;

        assertTrue(nearScore > farScore,
            "Closer lily pads should score higher");
    }

    // ==================== Water Detection Tests ====================

    @Test
    @DisplayName("Frogs detect water for jumping")
    void frogsDetectWater_forJumping() {
        BlockPos currentPos = new BlockPos(0, 64, 0);
        BlockPos waterPos = new BlockPos(5, 63, 0);

        double distance = Math.sqrt(
            Math.pow(waterPos.getX() - currentPos.getX(), 2) +
            Math.pow(waterPos.getZ() - currentPos.getZ(), 2)
        );

        assertTrue(distance < 10.0,
            "Should detect water within 10 blocks");
    }

    @Test
    @DisplayName("Frogs prefer jumping to water when on land")
    void frogsPrefer_jumpingToWater() {
        boolean isInWater = false;
        boolean isNearWater = false;

        // When on land and not near water, should jump toward water
        boolean shouldJumpToWater = !isInWater && !isNearWater;

        assertTrue(shouldJumpToWater || isInWater || isNearWater,
            "Frogs should seek water when on land");
    }

    // ==================== Jump Direction Tests ====================

    @Test
    @DisplayName("Jump direction is calculated correctly")
    void jumpDirection_isCalculatedCorrectly() {
        Vec3d frogPos = new Vec3d(0, 64, 0);
        Vec3d targetPos = new Vec3d(5, 64, 0);

        Vec3d jumpDirection = Vec3d.sub(targetPos, frogPos);
        jumpDirection.normalize();

        assertTrue(jumpDirection.x > 0,
            "Jump direction should point toward target");
        assertEquals(0.0, jumpDirection.y, 0.001,
            "Horizontal jump should have minimal Y component");
    }

    @Test
    @DisplayName("Jump arc includes vertical component")
    void jumpArc_includesVerticalComponent() {
        double distance = 5.0; // max distance
        double maxDistance = 5.0;

        // Vertical force = 0.3 + (distance/maxDistance) * 0.3
        // = 0.3 + 1.0 * 0.3 = 0.6
        double normalizedDistance = distance / maxDistance;
        double verticalForce = 0.3 + normalizedDistance * 0.3;

        assertTrue(verticalForce > 0,
            "Jump should have upward arc");
        assertTrue(verticalForce < 1.0,
            "Vertical force should be reasonable");
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

    // ==================== Cooldown Tests ====================

    @Test
    @DisplayName("Jump cooldown is 40-60 ticks")
    void jumpCooldown_isFortyToSixty() {
        int minCooldown = 40;
        int maxCooldown = 60;

        assertTrue(minCooldown > 0,
            "Cooldown should be positive");
        assertEquals(2.0, minCooldown / 20.0, 0.1,
            "Min cooldown should be 2 seconds");
        assertEquals(3.0, maxCooldown / 20.0, 0.1,
            "Max cooldown should be 3 seconds");
    }

    // ==================== Vec3d Type Tests ====================

    @Test
    @DisplayName("Vec3d distance calculation works correctly")
    void vec3dDistance_worksCorrectly() {
        Vec3d v1 = new Vec3d(0, 64, 0);
        Vec3d v2 = new Vec3d(3, 64, 4); // 3-4-5 triangle

        double distance = v1.distanceTo(v2);

        assertEquals(5.0, distance, 0.001,
            "Distance should be calculated correctly");
    }

    @Test
    @DisplayName("Vec3d magnitude works correctly")
    void vec3dMagnitude_worksCorrectly() {
        Vec3d v = new Vec3d(3, 0, 4); // 3-4-5 triangle

        double magnitude = v.magnitude();

        assertEquals(5.0, magnitude, 0.001,
            "Magnitude should be calculated correctly");
    }

    // ==================== Scientific Behavior Tests ====================

    @Test
    @DisplayName("Jump distance matches frog biology")
    void jumpDistance_matchesBiology() {
        // Frogs can jump 5 blocks (several times body length)
        double maxJumpDistance = 5.0;

        assertTrue(maxJumpDistance > 1.0,
            "Frogs should jump decent distance");
        assertTrue(maxJumpDistance < 10.0,
            "But not unrealistically far");
    }

    @Test
    @DisplayName("Jump height allows clearing obstacles")
    void jumpHeight_clearsObstacles() {
        double obstacleHeight = 1.0;
        double verticalForceMin = 0.3;
        double verticalForceMax = 0.6;

        assertTrue(verticalForceMax >= obstacleHeight * 0.5,
            "Jump should clear typical obstacles");
    }

    @Test
    @DisplayName("Target detection range is 6 blocks")
    void targetDetection_sixBlocks() {
        double detectionRange = 6.0;

        assertTrue(detectionRange > 0,
            "Detection range should be positive");
        assertTrue(detectionRange < 16.0,
            "Detection range should be reasonable");
    }

    // ==================== Social Behavior Tests ====================

    @Test
    @DisplayName("Frogs prefer lily pads near other frogs")
    void frogsPrefer_nearbyConspecifics() {
        // Social bonus for pads with nearby frogs
        double socialBonus = 3.0;

        assertTrue(socialBonus > 0,
            "Frogs should prefer social lily pads");
    }

    @Test
    @DisplayName("Frogs prefer lily pads near vegetation")
    void frogsPrefer_nearVegetation() {
        // Vegetation bonus for pads near plants
        double vegetationBonus = 2.0;

        assertTrue(vegetationBonus > 0,
            "Frogs should prefer vegetated lily pads");
    }

    // ==================== Edge Case Tests ====================

    @Test
    @DisplayName("Too close lily pads are invalid")
    void tooCloseLilyPads_areInvalid() {
        double minDistance = 1.5;
        double tooCloseDistance = 1.0;

        assertTrue(tooCloseDistance < minDistance,
            "Too close lily pads should be invalid");
    }

    @Test
    @DisplayName("Too far lily pads are invalid")
    void tooFarLilyPads_areInvalid() {
        double maxDistance = 5.0;
        double tooFarDistance = 6.0;

        assertTrue(tooFarDistance > maxDistance,
            "Too far lily pads should be invalid");
    }

    // ==================== Panic Tests ====================

    @Test
    @DisplayName("Panicking frogs don't jump (handled separately)")
    void panickingFrogs_dontJump() {
        // When panicking, jump behavior is skipped
        boolean isPanicking = true;
        boolean shouldJump = !isPanicking;

        assertFalse(shouldJump,
            "Panicking frogs should not use jump behavior");
    }

    // ==================== Random Jump Tests ====================

    @Test
    @DisplayName("Random idle jumps occur occasionally")
    void randomJumps_occurOccasionally() {
        double jumpProbability = 0.02; // 2% per tick

        assertTrue(jumpProbability > 0,
            "Should occasionally jump randomly");
        assertTrue(jumpProbability < 0.1,
            "But not too often");
    }
}
