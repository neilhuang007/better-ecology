package me.javavirtualenv.behavior.flocking;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for FlockingConfig class.
 * Tests configuration presets, validation, getters, and setters.
 * <p>
 * Test Coverage:
 * - Default constructor values
 * - Preset configurations (murmurations, V-formation, small flocks)
 * - Validation of parameter ranges
 * - Getter and setter functionality
 * - Scientific accuracy of default values
 * <p>
 * Scientific Basis:
 * - Ballerini et al. (2008) starling murmuration research
 * - Reynolds (1986) boids algorithm parameters
 * - Biological flocking behavior studies
 */
@DisplayName("FlockingConfig Tests")
public class FlockingConfigTest {

    private FlockingConfig defaultConfig;
    private FlockingConfig murmurationConfig;
    private FlockingConfig vFormationConfig;
    private FlockingConfig smallFlockConfig;

    @BeforeEach
    void setUp() {
        defaultConfig = new FlockingConfig();
        murmurationConfig = FlockingConfig.forMurmurations();
        vFormationConfig = FlockingConfig.forVFormation();
        smallFlockConfig = FlockingConfig.forSmallFlocks();
    }

    // ==================== Default Constructor Tests ====================

    @Test
    @DisplayName("Default constructor sets expected values")
    void testDefaultConstructor() {
        assertEquals(2.5, defaultConfig.getSeparationWeight(), "Default separation weight");
        assertEquals(1.5, defaultConfig.getAlignmentWeight(), "Default alignment weight");
        assertEquals(1.3, defaultConfig.getCohesionWeight(), "Default cohesion weight");
        assertEquals(7, defaultConfig.getTopologicalNeighborCount(), "Default neighbor count");
        assertEquals(2.0, defaultConfig.getSeparationDistance(), "Default separation distance");
        assertEquals(16.0, defaultConfig.getPerceptionRadius(), "Default perception radius");
        assertEquals(0.8, defaultConfig.getMaxSpeed(), "Default max speed");
        assertEquals(0.15, defaultConfig.getMaxForce(), "Default max force");
        assertEquals(Math.toRadians(270), defaultConfig.getPerceptionAngle(), 0.001,
            "Default perception angle should be 270 degrees");
        assertEquals(0.3, defaultConfig.getNoiseWeight(), "Default noise weight");
    }

    @Test
    @DisplayName("Default values are scientifically based")
    void testDefaultValuesScientific() {
        // Topological neighbor count should match Ballerini et al. (2008)
        assertTrue(defaultConfig.getTopologicalNeighborCount() >= 6 &&
                   defaultConfig.getTopologicalNeighborCount() <= 8,
            "Neighbor count should be in research-based range (6-8)");

        // Separation should be weighted highest to prevent overlap
        assertTrue(defaultConfig.getSeparationWeight() > defaultConfig.getAlignmentWeight(),
            "Separation should be weighted higher than alignment");
        assertTrue(defaultConfig.getSeparationWeight() > defaultConfig.getCohesionWeight(),
            "Separation should be weighted higher than cohesion");

        // Perception angle should be wide (close to 360)
        assertTrue(defaultConfig.getPerceptionAngle() > Math.toRadians(200),
            "Perception angle should be wide for natural flocking");
    }

    // ==================== Preset Configuration Tests ====================

    @Test
    @DisplayName("forMurmurations() sets murmeration-specific values")
    void testForMurmurations() {
        assertEquals(7, murmurationConfig.getTopologicalNeighborCount(),
            "Murmurations should use 7 neighbors per research");
        assertEquals(2.0, murmurationConfig.getSeparationWeight(),
            "Murmurations have moderate separation");
        assertEquals(1.5, murmurationConfig.getAlignmentWeight(),
            "Murmurations have high alignment for coordinated movement");
        assertEquals(1.2, murmurationConfig.getCohesionWeight(),
            "Murmurations have moderate cohesion");
        assertEquals(20.0, murmurationConfig.getPerceptionRadius(),
            "Murmurations have larger perception radius");
        assertEquals(1.2, murmurationConfig.getMaxSpeed(),
            "Murmurations have higher max speed");
        assertEquals(0.2, murmurationConfig.getMaxForce(),
            "Murmurations have higher max force");
        assertEquals(0.2, murmurationConfig.getNoiseWeight(),
            "Murmurations have lower noise for more coordinated movement");
    }

    @Test
    @DisplayName("forVFormation() sets V-formation specific values")
    void testForVFormation() {
        assertEquals(6, vFormationConfig.getTopologicalNeighborCount(),
            "V-formation uses 6 neighbors");
        assertEquals(2.0, vFormationConfig.getSeparationWeight(),
            "V-formation has moderate separation");
        assertEquals(2.0, vFormationConfig.getAlignmentWeight(),
            "V-formation has highest alignment for precise formation");
        assertEquals(1.0, vFormationConfig.getCohesionWeight(),
            "V-formation has lower cohesion to maintain spacing");
        assertEquals(12.0, vFormationConfig.getPerceptionRadius(),
            "V-formation has moderate perception radius");
        assertEquals(1.0, vFormationConfig.getMaxSpeed(),
            "V-formation has moderate speed");
        assertEquals(0.1, vFormationConfig.getMaxForce(),
            "V-formation has lower max force for smooth movement");
        assertEquals(0.1, vFormationConfig.getNoiseWeight(),
            "V-formation has minimal noise for precise formation");
    }

    @Test
    @DisplayName("forSmallFlocks() sets small flock values")
    void testForSmallFlocks() {
        assertEquals(5, smallFlockConfig.getTopologicalNeighborCount(),
            "Small flocks track fewer neighbors");
        assertEquals(3.0, smallFlockConfig.getSeparationWeight(),
            "Small flocks have high separation to prevent crowding");
        assertEquals(1.5, smallFlockConfig.getAlignmentWeight(),
            "Small flocks have moderate alignment");
        assertEquals(1.5, smallFlockConfig.getCohesionWeight(),
            "Small flocks have high cohesion to stay together");
        assertEquals(1.5, smallFlockConfig.getSeparationDistance(),
            "Small flocks have smaller separation distance");
        assertEquals(8.0, smallFlockConfig.getPerceptionRadius(),
            "Small flocks have smaller perception radius");
        assertEquals(0.6, smallFlockConfig.getMaxSpeed(),
            "Small flocks have lower speed");
        assertEquals(0.12, smallFlockConfig.getMaxForce(),
            "Small flocks have lower max force");
    }

    @Test
    @DisplayName("Preset configurations are distinct from default")
    void testPresetsDistinct() {
        // Murmuration should differ from default
        assertNotEquals(defaultConfig.getPerceptionRadius(), murmurationConfig.getPerceptionRadius());
        assertNotEquals(defaultConfig.getMaxSpeed(), murmurationConfig.getMaxSpeed());

        // V-formation should differ from default
        assertNotEquals(defaultConfig.getAlignmentWeight(), vFormationConfig.getAlignmentWeight());
        assertNotEquals(defaultConfig.getCohesionWeight(), vFormationConfig.getCohesionWeight());

        // Small flock should differ from default
        assertNotEquals(defaultConfig.getTopologicalNeighborCount(), smallFlockConfig.getTopologicalNeighborCount());
        assertNotEquals(defaultConfig.getSeparationWeight(), smallFlockConfig.getSeparationWeight());
    }

    // ==================== Validation Tests ====================

    @Test
    @DisplayName("validate() throws exception for negative weights")
    void testValidateNegativeWeights() {
        FlockingConfig invalidConfig = new FlockingConfig();
        invalidConfig.setSeparationWeight(-1.0);

        Exception exception = assertThrows(IllegalStateException.class, invalidConfig::validate);
        assertTrue(exception.getMessage().contains("Behavior weights must be non-negative"));
    }

    @Test
    @DisplayName("setters clamp invalid values and validate passes for clamped values")
    void testValidateWithClampedValues() {
        FlockingConfig config = new FlockingConfig();
        // These setters clamp to minimum values
        config.setTopologicalNeighborCount(0);
        config.setMaxSpeed(0);
        config.setMaxForce(0);

        // After clamping, validate should pass since values are now valid
        assertDoesNotThrow(() -> config.validate());
        // Verify clamping occurred
        assertEquals(1, config.getTopologicalNeighborCount());
        assertEquals(0.01, config.getMaxSpeed());
        assertEquals(0.01, config.getMaxForce());
    }

    @Test
    @DisplayName("validate() passes for valid configuration")
    void testValidateValidConfig() {
        assertDoesNotThrow(() -> defaultConfig.validate());
        assertDoesNotThrow(() -> murmurationConfig.validate());
        assertDoesNotThrow(() -> vFormationConfig.validate());
        assertDoesNotThrow(() -> smallFlockConfig.validate());
    }

    // ==================== Getter and Setter Tests ====================

    @Test
    @DisplayName("getSeparationWeight/setSeparationWeight work correctly")
    void testSeparationWeightGetterSetter() {
        defaultConfig.setSeparationWeight(3.5);
        assertEquals(3.5, defaultConfig.getSeparationWeight());
    }

    @Test
    @DisplayName("getAlignmentWeight/setAlignmentWeight work correctly")
    void testAlignmentWeightGetterSetter() {
        defaultConfig.setAlignmentWeight(2.5);
        assertEquals(2.5, defaultConfig.getAlignmentWeight());
    }

    @Test
    @DisplayName("getCohesionWeight/setCohesionWeight work correctly")
    void testCohesionWeightGetterSetter() {
        defaultConfig.setCohesionWeight(1.8);
        assertEquals(1.8, defaultConfig.getCohesionWeight());
    }

    @Test
    @DisplayName("getTopologicalNeighborCount/setTopologicalNeighborCount work correctly")
    void testTopologicalNeighborCountGetterSetter() {
        defaultConfig.setTopologicalNeighborCount(10);
        assertEquals(10, defaultConfig.getTopologicalNeighborCount());
    }

    @Test
    @DisplayName("setTopologicalNeighborCount enforces minimum value")
    void testTopologicalNeighborCountMinimum() {
        defaultConfig.setTopologicalNeighborCount(-5);
        assertEquals(1, defaultConfig.getTopologicalNeighborCount(),
            "Should clamp to minimum value of 1");

        defaultConfig.setTopologicalNeighborCount(0);
        assertEquals(1, defaultConfig.getTopologicalNeighborCount(),
            "Should clamp to minimum value of 1");
    }

    @Test
    @DisplayName("getSeparationDistance/setSeparationDistance work correctly")
    void testSeparationDistanceGetterSetter() {
        defaultConfig.setSeparationDistance(3.5);
        assertEquals(3.5, defaultConfig.getSeparationDistance());
    }

    @Test
    @DisplayName("setSeparationDistance enforces minimum value")
    void testSeparationDistanceMinimum() {
        defaultConfig.setSeparationDistance(-1.0);
        assertEquals(0.1, defaultConfig.getSeparationDistance(),
            "Should clamp to minimum value of 0.1");

        defaultConfig.setSeparationDistance(0);
        assertEquals(0.1, defaultConfig.getSeparationDistance(),
            "Should clamp to minimum value of 0.1");
    }

    @Test
    @DisplayName("getPerceptionRadius/setPerceptionRadius work correctly")
    void testPerceptionRadiusGetterSetter() {
        defaultConfig.setPerceptionRadius(25.0);
        assertEquals(25.0, defaultConfig.getPerceptionRadius());
    }

    @Test
    @DisplayName("setPerceptionRadius enforces minimum value")
    void testPerceptionRadiusMinimum() {
        defaultConfig.setPerceptionRadius(-5.0);
        assertEquals(1.0, defaultConfig.getPerceptionRadius(),
            "Should clamp to minimum value of 1.0");

        defaultConfig.setPerceptionRadius(0);
        assertEquals(1.0, defaultConfig.getPerceptionRadius(),
            "Should clamp to minimum value of 1.0");
    }

    @Test
    @DisplayName("getMaxSpeed/setMaxSpeed work correctly")
    void testMaxSpeedGetterSetter() {
        defaultConfig.setMaxSpeed(1.5);
        assertEquals(1.5, defaultConfig.getMaxSpeed());
    }

    @Test
    @DisplayName("setMaxSpeed enforces minimum value")
    void testMaxSpeedMinimum() {
        defaultConfig.setMaxSpeed(-0.5);
        assertEquals(0.01, defaultConfig.getMaxSpeed(),
            "Should clamp to minimum value of 0.01");

        defaultConfig.setMaxSpeed(0);
        assertEquals(0.01, defaultConfig.getMaxSpeed(),
            "Should clamp to minimum value of 0.01");
    }

    @Test
    @DisplayName("getMaxForce/setMaxForce work correctly")
    void testMaxForceGetterSetter() {
        defaultConfig.setMaxForce(0.25);
        assertEquals(0.25, defaultConfig.getMaxForce());
    }

    @Test
    @DisplayName("setMaxForce enforces minimum value")
    void testMaxForceMinimum() {
        defaultConfig.setMaxForce(-0.1);
        assertEquals(0.01, defaultConfig.getMaxForce(),
            "Should clamp to minimum value of 0.01");

        defaultConfig.setMaxForce(0);
        assertEquals(0.01, defaultConfig.getMaxForce(),
            "Should clamp to minimum value of 0.01");
    }

    @Test
    @DisplayName("getPerceptionAngle/setPerceptionAngle work correctly")
    void testPerceptionAngleGetterSetter() {
        double newAngle = Math.toRadians(180);
        defaultConfig.setPerceptionAngle(newAngle);
        assertEquals(newAngle, defaultConfig.getPerceptionAngle(), 0.001);
    }

    @Test
    @DisplayName("setPerceptionAngle enforces valid range")
    void testPerceptionAngleRange() {
        // Test upper bound (2 * PI)
        defaultConfig.setPerceptionAngle(Math.PI * 3);
        assertEquals(Math.PI * 2, defaultConfig.getPerceptionAngle(), 0.001,
            "Should clamp to maximum of 2*PI");

        // Test lower bound (0)
        defaultConfig.setPerceptionAngle(-1.0);
        assertEquals(0.0, defaultConfig.getPerceptionAngle(), 0.001,
            "Should clamp to minimum of 0");
    }

    @Test
    @DisplayName("getNoiseWeight/setNoiseWeight work correctly")
    void testNoiseWeightGetterSetter() {
        defaultConfig.setNoiseWeight(0.5);
        assertEquals(0.5, defaultConfig.getNoiseWeight());
    }

    @Test
    @DisplayName("setNoiseWeight enforces minimum value")
    void testNoiseWeightMinimum() {
        defaultConfig.setNoiseWeight(-0.5);
        assertEquals(0.0, defaultConfig.getNoiseWeight(),
            "Should clamp to minimum value of 0");
    }

    // ==================== toString Test ====================

    @Test
    @DisplayName("toString returns formatted string")
    void testToString() {
        String result = defaultConfig.toString();
        assertNotNull(result);
        assertTrue(result.contains("FlockingConfig{"));
        assertTrue(result.contains("separation="));
        assertTrue(result.contains("alignment="));
        assertTrue(result.contains("cohesion="));
        assertTrue(result.contains("neighbors="));
        assertTrue(result.contains("radius="));
    }

    // ==================== Comparative Tests ====================

    @Test
    @DisplayName("Murmuration config has highest perception radius")
    void testMurmurationHighestPerception() {
        assertTrue(murmurationConfig.getPerceptionRadius() > defaultConfig.getPerceptionRadius(),
            "Murmurations should have larger perception than default");
        assertTrue(murmurationConfig.getPerceptionRadius() > vFormationConfig.getPerceptionRadius(),
            "Murmurations should have larger perception than V-formation");
    }

    @Test
    @DisplayName("V-formation config has highest alignment weight")
    void testVFormationHighestAlignment() {
        assertTrue(vFormationConfig.getAlignmentWeight() > defaultConfig.getAlignmentWeight(),
            "V-formation should have higher alignment than default");
        assertTrue(vFormationConfig.getAlignmentWeight() > murmurationConfig.getAlignmentWeight(),
            "V-formation should have higher alignment than murmurations");
        assertTrue(vFormationConfig.getAlignmentWeight() > smallFlockConfig.getAlignmentWeight(),
            "V-formation should have higher alignment than small flocks");
    }

    @Test
    @DisplayName("Small flock config has highest separation weight")
    void testSmallFlockHighestSeparation() {
        assertTrue(smallFlockConfig.getSeparationWeight() > defaultConfig.getSeparationWeight(),
            "Small flocks should have higher separation than default");
        assertTrue(smallFlockConfig.getSeparationWeight() > murmurationConfig.getSeparationWeight(),
            "Small flocks should have higher separation than murmurations");
    }

    @Test
    @DisplayName("Small flock config has smallest perception radius")
    void testSmallFlockSmallestPerception() {
        assertTrue(smallFlockConfig.getPerceptionRadius() < defaultConfig.getPerceptionRadius(),
            "Small flocks should have smaller perception than default");
        assertTrue(smallFlockConfig.getPerceptionRadius() < murmurationConfig.getPerceptionRadius(),
            "Small flocks should have smaller perception than murmurations");
    }

    @Test
    @DisplayName("V-formation config has lowest noise weight")
    void testVFormationLowestNoise() {
        assertTrue(vFormationConfig.getNoiseWeight() < defaultConfig.getNoiseWeight(),
            "V-formation should have lower noise than default");
        assertTrue(vFormationConfig.getNoiseWeight() <= murmurationConfig.getNoiseWeight(),
            "V-formation should have lower or equal noise than murmurations");
    }

    // ==================== Edge Cases ====================

    @Test
    @DisplayName("Config handles extreme values with clamping")
    void testExtremeValueClamping() {
        FlockingConfig extremeConfig = new FlockingConfig();

        // Test extreme high values
        extremeConfig.setTopologicalNeighborCount(Integer.MAX_VALUE);
        assertTrue(extremeConfig.getTopologicalNeighborCount() > 0);

        extremeConfig.setPerceptionRadius(Double.MAX_VALUE);
        assertTrue(extremeConfig.getPerceptionRadius() > 0);

        extremeConfig.setMaxSpeed(Double.MAX_VALUE);
        assertTrue(extremeConfig.getMaxSpeed() > 0);

        // Test extreme low values
        extremeConfig.setTopologicalNeighborCount(Integer.MIN_VALUE);
        assertEquals(1, extremeConfig.getTopologicalNeighborCount());

        extremeConfig.setPerceptionRadius(Double.MIN_VALUE);
        assertEquals(1.0, extremeConfig.getPerceptionRadius());
    }

    @Test
    @DisplayName("Multiple setters work independently")
    void testMultipleSetters() {
        FlockingConfig config = new FlockingConfig();

        config.setSeparationWeight(1.0);
        config.setAlignmentWeight(2.0);
        config.setCohesionWeight(3.0);
        config.setPerceptionRadius(20.0);
        config.setMaxSpeed(1.0);
        config.setMaxForce(0.2);

        assertEquals(1.0, config.getSeparationWeight());
        assertEquals(2.0, config.getAlignmentWeight());
        assertEquals(3.0, config.getCohesionWeight());
        assertEquals(20.0, config.getPerceptionRadius());
        assertEquals(1.0, config.getMaxSpeed());
        assertEquals(0.2, config.getMaxForce());
    }

    // ==================== Scientific Accuracy Tests ====================

    @Test
    @DisplayName("Default neighbor count matches Ballerini et al. (2008)")
    void testScientificNeighborCount() {
        assertTrue(defaultConfig.getTopologicalNeighborCount() >= 6 &&
                   defaultConfig.getTopologicalNeighborCount() <= 8,
            "Research shows starlings track 6-7 nearest neighbors");
    }

    @Test
    @DisplayName("Perception angle matches biological field of view")
    void testScientificPerceptionAngle() {
        assertTrue(defaultConfig.getPerceptionAngle() >= Math.toRadians(240),
            "Birds have wide field of view, typically 270+ degrees");
    }

    @Test
    @DisplayName("Weight ratios promote natural flocking")
    void testScientificWeightRatios() {
        // Separation > Alignment > Cohesion promotes natural, non-clumping flocks
        assertTrue(defaultConfig.getSeparationWeight() > defaultConfig.getAlignmentWeight(),
            "Separation should be strongest to prevent overlap");
        assertTrue(defaultConfig.getAlignmentWeight() >= defaultConfig.getCohesionWeight(),
            "Alignment should be at least as strong as cohesion");
    }

    @Test
    @DisplayName("Force limits prevent unnatural movement")
    void testScientificForceLimits() {
        assertTrue(defaultConfig.getMaxForce() < defaultConfig.getMaxSpeed(),
            "Max force should be less than max speed for smooth turning");

        assertTrue(defaultConfig.getMaxForce() > 0 && defaultConfig.getMaxForce() < 0.5,
            "Force should be small enough for gradual steering");
    }

    @Test
    @DisplayName("Perception radius matches biological sensing range")
    void testScientificPerceptionRadius() {
        assertTrue(defaultConfig.getPerceptionRadius() >= 8.0 &&
                   defaultConfig.getPerceptionRadius() <= 32.0,
            "Perception radius should be in biological range (8-32 blocks)");
    }

    @Test
    @DisplayName("Separation distance prevents overlap")
    void testScientificSeparationDistance() {
        assertTrue(defaultConfig.getSeparationDistance() > 0.5,
            "Separation distance should prevent entity overlap");
        assertTrue(defaultConfig.getSeparationDistance() < defaultConfig.getPerceptionRadius(),
            "Separation distance should be less than perception radius");
    }
}
