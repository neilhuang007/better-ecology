package me.javavirtualenv.behavior.core;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for SteeringBehavior.
 * Tests core steering behavior calculations including seek, flee, arrive,
 * weight management, and force limiting.
 */
class SteeringBehaviorTest {

    private TestSteeringBehavior steeringBehavior;
    private BehaviorContext testContext;
    private Vec3d testPosition;
    private Vec3d testVelocity;

    private static final double DEFAULT_MAX_SPEED = 1.0;
    private static final double DEFAULT_MAX_FORCE = 0.5;
    private static final double EPSILON = 0.0001;

    @BeforeEach
    void setUp() {
        steeringBehavior = new TestSteeringBehavior();
        testPosition = new Vec3d(0.0, 0.0, 0.0);
        testVelocity = new Vec3d(0.5, 0.0, 0.5);
        testContext = new BehaviorContext(testPosition, testVelocity, DEFAULT_MAX_SPEED, DEFAULT_MAX_FORCE);
    }

    @Test
    void calculateWeightedReturnsZeroVectorWhenDisabled() {
        steeringBehavior.setEnabled(false);
        steeringBehavior.setWeight(2.0);

        Vec3d result = steeringBehavior.calculateWeighted(testContext);

        assertEquals(0.0, result.x, EPSILON);
        assertEquals(0.0, result.y, EPSILON);
        assertEquals(0.0, result.z, EPSILON);
    }

    @Test
    void calculateWeightedReturnsZeroVectorWhenCalculateReturnsZero() {
        steeringBehavior.setEnabled(true);
        steeringBehavior.setTestResult(new Vec3d(0.0, 0.0, 0.0));

        Vec3d result = steeringBehavior.calculateWeighted(testContext);

        assertEquals(0.0, result.x, EPSILON);
        assertEquals(0.0, result.y, EPSILON);
        assertEquals(0.0, result.z, EPSILON);
    }

    @Test
    void calculateWeightedMultipliesResultByWeight() {
        Vec3d baseResult = new Vec3d(1.0, 2.0, 3.0);
        steeringBehavior.setTestResult(baseResult);
        steeringBehavior.setWeight(2.5);

        Vec3d result = steeringBehavior.calculateWeighted(testContext);

        assertEquals(2.5, result.x, EPSILON);
        assertEquals(5.0, result.y, EPSILON);
        assertEquals(7.5, result.z, EPSILON);
    }

    @Test
    void calculateWeightedUsesDefaultWeightWhenNotSet() {
        Vec3d baseResult = new Vec3d(1.0, 1.0, 1.0);
        steeringBehavior.setTestResult(baseResult);

        Vec3d result = steeringBehavior.calculateWeighted(testContext);

        assertEquals(1.0, result.x, EPSILON);
        assertEquals(1.0, result.y, EPSILON);
        assertEquals(1.0, result.z, EPSILON);
    }

    @Test
    void seekReturnsForceTowardsTarget() {
        Vec3d currentPosition = new Vec3d(0.0, 0.0, 0.0);
        Vec3d currentVelocity = new Vec3d(0.0, 0.0, 0.0);
        Vec3d targetPosition = new Vec3d(10.0, 0.0, 0.0);
        double maxSpeed = 1.0;

        Vec3d result = steeringBehavior.testSeek(currentPosition, currentVelocity, targetPosition, maxSpeed);

        assertEquals(1.0, result.x, EPSILON);
        assertEquals(0.0, result.y, EPSILON);
        assertEquals(0.0, result.z, EPSILON);
    }

    @Test
    void seekAccountsForCurrentVelocity() {
        Vec3d currentPosition = new Vec3d(0.0, 0.0, 0.0);
        Vec3d currentVelocity = new Vec3d(0.5, 0.0, 0.0);
        Vec3d targetPosition = new Vec3d(10.0, 0.0, 0.0);
        double maxSpeed = 1.0;

        Vec3d result = steeringBehavior.testSeek(currentPosition, currentVelocity, targetPosition, maxSpeed);

        assertEquals(0.5, result.x, EPSILON);
        assertEquals(0.0, result.y, EPSILON);
        assertEquals(0.0, result.z, EPSILON);
    }

    @Test
    void seekNormalizesDirection() {
        Vec3d currentPosition = new Vec3d(0.0, 0.0, 0.0);
        Vec3d currentVelocity = new Vec3d(0.0, 0.0, 0.0);
        Vec3d targetPosition = new Vec3d(3.0, 4.0, 0.0);
        double maxSpeed = 1.0;

        Vec3d result = steeringBehavior.testSeek(currentPosition, currentVelocity, targetPosition, maxSpeed);
        double magnitude = Math.sqrt(result.x * result.x + result.y * result.y + result.z * result.z);

        assertEquals(1.0, magnitude, EPSILON);
    }

    @Test
    void fleeReturnsForceAwayFromThreat() {
        Vec3d currentPosition = new Vec3d(10.0, 0.0, 0.0);
        Vec3d currentVelocity = new Vec3d(0.0, 0.0, 0.0);
        Vec3d threatPosition = new Vec3d(0.0, 0.0, 0.0);
        double maxSpeed = 1.0;

        Vec3d result = steeringBehavior.testFlee(currentPosition, currentVelocity, threatPosition, maxSpeed);

        assertEquals(1.0, result.x, EPSILON);
    }

    @Test
    void fleeAccountsForCurrentVelocity() {
        Vec3d currentPosition = new Vec3d(10.0, 0.0, 0.0);
        Vec3d currentVelocity = new Vec3d(0.3, 0.0, 0.0);
        Vec3d threatPosition = new Vec3d(0.0, 0.0, 0.0);
        double maxSpeed = 1.0;

        Vec3d result = steeringBehavior.testFlee(currentPosition, currentVelocity, threatPosition, maxSpeed);

        assertEquals(0.7, result.x, EPSILON);
    }

    @Test
    void fleeNormalizesDirection() {
        Vec3d currentPosition = new Vec3d(0.0, 0.0, 0.0);
        Vec3d currentVelocity = new Vec3d(0.0, 0.0, 0.0);
        Vec3d threatPosition = new Vec3d(3.0, 4.0, 0.0);
        double maxSpeed = 1.0;

        Vec3d result = steeringBehavior.testFlee(currentPosition, currentVelocity, threatPosition, maxSpeed);
        double magnitude = Math.sqrt(result.x * result.x + result.y * result.y + result.z * result.z);

        assertEquals(1.0, magnitude, EPSILON);
    }

    @Test
    void arriveSlowsDownWithinSlowingRadius() {
        Vec3d currentPosition = new Vec3d(0.0, 0.0, 0.0);
        Vec3d currentVelocity = new Vec3d(0.0, 0.0, 0.0);
        Vec3d targetPosition = new Vec3d(5.0, 0.0, 0.0);
        double maxSpeed = 1.0;
        double slowingRadius = 10.0;

        Vec3d result = steeringBehavior.testArrive(currentPosition, currentVelocity, targetPosition, maxSpeed, slowingRadius);

        assertEquals(0.5, result.x, EPSILON);
    }

    @Test
    void arriveReturnsZeroForceWhenVeryCloseToTarget() {
        Vec3d currentPosition = new Vec3d(0.0, 0.0, 0.0);
        Vec3d currentVelocity = new Vec3d(0.0, 0.0, 0.0);
        Vec3d targetPosition = new Vec3d(0.05, 0.0, 0.0);
        double maxSpeed = 1.0;
        double slowingRadius = 10.0;

        Vec3d result = steeringBehavior.testArrive(currentPosition, currentVelocity, targetPosition, maxSpeed, slowingRadius);

        assertEquals(0.0, result.x, EPSILON);
        assertEquals(0.0, result.y, EPSILON);
        assertEquals(0.0, result.z, EPSILON);
    }

    @Test
    void arriveUsesFullSpeedOutsideSlowingRadius() {
        Vec3d currentPosition = new Vec3d(0.0, 0.0, 0.0);
        Vec3d currentVelocity = new Vec3d(0.0, 0.0, 0.0);
        Vec3d targetPosition = new Vec3d(20.0, 0.0, 0.0);
        double maxSpeed = 1.0;
        double slowingRadius = 10.0;

        Vec3d result = steeringBehavior.testArrive(currentPosition, currentVelocity, targetPosition, maxSpeed, slowingRadius);

        assertEquals(1.0, result.x, EPSILON);
    }

    @Test
    void arriveAccountsForCurrentVelocity() {
        Vec3d currentPosition = new Vec3d(0.0, 0.0, 0.0);
        Vec3d currentVelocity = new Vec3d(0.3, 0.0, 0.0);
        Vec3d targetPosition = new Vec3d(20.0, 0.0, 0.0);
        double maxSpeed = 1.0;
        double slowingRadius = 10.0;

        Vec3d result = steeringBehavior.testArrive(currentPosition, currentVelocity, targetPosition, maxSpeed, slowingRadius);

        assertEquals(0.7, result.x, EPSILON);
    }

    @Test
    void arriveProportionalSlowdown() {
        Vec3d currentPosition = new Vec3d(0.0, 0.0, 0.0);
        Vec3d currentVelocity = new Vec3d(0.0, 0.0, 0.0);
        Vec3d targetPosition = new Vec3d(5.0, 0.0, 0.0);
        double maxSpeed = 1.0;
        double slowingRadius = 10.0;

        Vec3d result = steeringBehavior.testArrive(currentPosition, currentVelocity, targetPosition, maxSpeed, slowingRadius);
        double distance = currentPosition.distanceTo(targetPosition);
        double expectedSpeed = maxSpeed * (distance / slowingRadius);

        assertEquals(expectedSpeed, result.x, EPSILON);
    }

    @Test
    void limitForceReturnsUnchangedForceWhenBelowMax() {
        Vec3d force = new Vec3d(0.3, 0.3, 0.0);
        double maxForce = 0.5;

        Vec3d result = steeringBehavior.limitForce(force, maxForce);
        double magnitude = Math.sqrt(result.x * result.x + result.y * result.y + result.z * result.z);

        assertTrue(magnitude <= maxForce);
        assertEquals(0.3, result.x, EPSILON);
        assertEquals(0.3, result.y, EPSILON);
    }

    @Test
    void limitForceClampsForceWhenAboveMax() {
        Vec3d force = new Vec3d(1.0, 0.0, 0.0);
        double maxForce = 0.5;

        Vec3d result = steeringBehavior.limitForce(force, maxForce);
        double magnitude = Math.sqrt(result.x * result.x + result.y * result.y + result.z * result.z);

        assertEquals(0.5, magnitude, EPSILON);
    }

    @Test
    void limitForcePreservesDirection() {
        Vec3d force = new Vec3d(3.0, 4.0, 0.0);
        double maxForce = 0.5;

        Vec3d result = steeringBehavior.limitForce(force, maxForce);

        double originalMagnitude = Math.sqrt(force.x * force.x + force.y * force.y + force.z * force.z);
        double resultMagnitude = Math.sqrt(result.x * result.x + result.y * result.y + result.z * result.z);

        assertEquals(force.x / originalMagnitude, result.x / resultMagnitude, EPSILON);
        assertEquals(force.y / originalMagnitude, result.y / resultMagnitude, EPSILON);
        assertEquals(force.z / originalMagnitude, result.z / resultMagnitude, EPSILON);
    }

    @Test
    void limitForceHandlesZeroVector() {
        Vec3d force = new Vec3d(0.0, 0.0, 0.0);
        double maxForce = 0.5;

        Vec3d result = steeringBehavior.limitForce(force, maxForce);

        assertEquals(0.0, result.x, EPSILON);
        assertEquals(0.0, result.y, EPSILON);
        assertEquals(0.0, result.z, EPSILON);
    }

    @Test
    void setWeightUpdatesWeightValue() {
        steeringBehavior.setWeight(3.5);

        assertEquals(3.5, steeringBehavior.getWeight(), EPSILON);
    }

    @Test
    void getWeightReturnsDefaultWeight() {
        assertEquals(1.0, steeringBehavior.getWeight(), EPSILON);
    }

    @Test
    void setWeightWithNegativeValue() {
        steeringBehavior.setWeight(-2.0);

        assertEquals(-2.0, steeringBehavior.getWeight(), EPSILON);
    }

    @Test
    void setEnabledToFalseDisablesBehavior() {
        steeringBehavior.setEnabled(false);

        assertFalse(steeringBehavior.isEnabled());
    }

    @Test
    void setEnabledToTrueEnablesBehavior() {
        steeringBehavior.setEnabled(false);
        steeringBehavior.setEnabled(true);

        assertTrue(steeringBehavior.isEnabled());
    }

    @Test
    void isEnabledReturnsTrueByDefault() {
        assertTrue(steeringBehavior.isEnabled());
    }

    @Test
    void calculateWeightedDoesNotMutateOriginalResult() {
        Vec3d baseResult = new Vec3d(1.0, 2.0, 3.0);
        steeringBehavior.setTestResult(baseResult);
        steeringBehavior.setWeight(2.0);

        Vec3d beforeCopy = baseResult.copy();
        steeringBehavior.calculateWeighted(testContext);

        assertEquals(beforeCopy.x, baseResult.x, EPSILON);
        assertEquals(beforeCopy.y, baseResult.y, EPSILON);
        assertEquals(beforeCopy.z, baseResult.z, EPSILON);
    }

    @Test
    void seekHandlesZeroCurrentVelocity() {
        Vec3d currentPosition = new Vec3d(0.0, 0.0, 0.0);
        Vec3d currentVelocity = new Vec3d(0.0, 0.0, 0.0);
        Vec3d targetPosition = new Vec3d(1.0, 2.0, 3.0);
        double maxSpeed = 2.0;

        Vec3d result = steeringBehavior.testSeek(currentPosition, currentVelocity, targetPosition, maxSpeed);
        double magnitude = Math.sqrt(result.x * result.x + result.y * result.y + result.z * result.z);

        assertEquals(2.0, magnitude, EPSILON);
    }

    @Test
    void fleeHandlesZeroCurrentVelocity() {
        Vec3d currentPosition = new Vec3d(5.0, 0.0, 0.0);
        Vec3d currentVelocity = new Vec3d(0.0, 0.0, 0.0);
        Vec3d threatPosition = new Vec3d(0.0, 0.0, 0.0);
        double maxSpeed = 1.5;

        Vec3d result = steeringBehavior.testFlee(currentPosition, currentVelocity, threatPosition, maxSpeed);
        double magnitude = Math.sqrt(result.x * result.x + result.y * result.y + result.z * result.z);

        assertEquals(1.5, magnitude, EPSILON);
    }

    @Test
    void limitForceWithZeroMaxForce() {
        Vec3d force = new Vec3d(1.0, 2.0, 3.0);
        double maxForce = 0.0;

        Vec3d result = steeringBehavior.limitForce(force, maxForce);

        assertEquals(0.0, result.x, EPSILON);
        assertEquals(0.0, result.y, EPSILON);
        assertEquals(0.0, result.z, EPSILON);
    }

    /**
     * Concrete test implementation of SteeringBehavior for testing protected methods.
     */
    private static class TestSteeringBehavior extends SteeringBehavior {
        private Vec3d testResult;

        public void setTestResult(Vec3d result) {
            this.testResult = result;
        }

        @Override
        public Vec3d calculate(BehaviorContext context) {
            return testResult != null ? testResult : new Vec3d();
        }

        public Vec3d testSeek(Vec3d currentPosition, Vec3d currentVelocity, Vec3d targetPosition, double maxSpeed) {
            return seek(currentPosition, currentVelocity, targetPosition, maxSpeed);
        }

        public Vec3d testFlee(Vec3d currentPosition, Vec3d currentVelocity, Vec3d threatPosition, double maxSpeed) {
            return flee(currentPosition, currentVelocity, threatPosition, maxSpeed);
        }

        public Vec3d testArrive(Vec3d currentPosition, Vec3d currentVelocity, Vec3d targetPosition, double maxSpeed, double slowingRadius) {
            return arrive(currentPosition, currentVelocity, targetPosition, maxSpeed, slowingRadius);
        }
    }
}
