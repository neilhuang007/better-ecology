package me.javavirtualenv.behavior.core;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import net.minecraft.core.BlockPos;

import java.util.ArrayList;
import java.util.List;

/**
 * Comprehensive test suite for BehaviorContext.
 * Tests constructors, getters, setters, and builder pattern.
 *
 * Note: Tests use the test-only constructor BehaviorContext(Vec3d, Vec3d, double, double)
 * which doesn't require Minecraft Entity/Mob instances.
 */
class BehaviorContextTest {

    private Vec3d testPosition;
    private Vec3d testVelocity;
    private double testMaxSpeed;
    private double testMaxForce;

    @BeforeEach
    void setUp() {
        testPosition = new Vec3d(10.0, 64.0, 20.0);
        testVelocity = new Vec3d(0.1, 0.0, 0.2);
        testMaxSpeed = 1.0;
        testMaxForce = 0.5;
    }

    // ==================== Constructor Tests ====================

    @Test
    void constructor_withVec3d_initializesCorrectly() {
        BehaviorContext context = new BehaviorContext(testPosition, testVelocity, testMaxSpeed, testMaxForce);

        assertNotNull(context, "Context should initialize");
        assertEquals(testPosition, context.getPosition(), "Position should match input");
        assertEquals(testVelocity, context.getVelocity(), "Velocity should match input");
        assertEquals(testMaxSpeed, context.getMaxSpeed(), 0.001, "Max speed should match input");
        assertEquals(testMaxForce, context.getMaxForce(), 0.001, "Max force should match input");
        assertNull(context.getEntity(), "Entity should be null");
        assertNull(context.getLevel(), "Level should be null");
        assertTrue(context.getNeighbors().isEmpty(), "Neighbors should be empty initially");
    }

    @Test
    void constructor_withNullPosition_usesDefault() {
        BehaviorContext context = new BehaviorContext(null, testVelocity, testMaxSpeed, testMaxForce);

        assertNotNull(context.getPosition(), "Position should not be null");
        assertEquals(0.0, context.getPosition().x, 0.001);
        assertEquals(0.0, context.getPosition().y, 0.001);
        assertEquals(0.0, context.getPosition().z, 0.001);
    }

    @Test
    void constructor_withNullVelocity_usesDefault() {
        BehaviorContext context = new BehaviorContext(testPosition, null, testMaxSpeed, testMaxForce);

        assertNotNull(context.getVelocity(), "Velocity should not be null");
        assertEquals(0.0, context.getVelocity().x, 0.001);
        assertEquals(0.0, context.getVelocity().y, 0.001);
        assertEquals(0.0, context.getVelocity().z, 0.001);
    }

    @Test
    void constructor_withAllNull_usesDefaults() {
        BehaviorContext context = new BehaviorContext(null, null, 0.5, 0.1);

        assertNotNull(context.getPosition());
        assertNotNull(context.getVelocity());
        assertEquals(0.5, context.getMaxSpeed(), 0.001);
        assertEquals(0.1, context.getMaxForce(), 0.001);
    }

    @Test
    void constructor_withNegativeSpeed_storesNegativeValue() {
        BehaviorContext context = new BehaviorContext(testPosition, testVelocity, -1.0, testMaxForce);

        assertEquals(-1.0, context.getMaxSpeed(), 0.001, "Max speed stores negative value directly");
    }

    @Test
    void constructor_withZeroValues_worksCorrectly() {
        BehaviorContext context = new BehaviorContext(
                new Vec3d(0, 0, 0),
                new Vec3d(0, 0, 0),
                0.0,
                0.0
        );

        assertEquals(0.0, context.getPosition().magnitude(), 0.001);
        assertEquals(0.0, context.getVelocity().magnitude(), 0.001);
        assertEquals(0.0, context.getMaxSpeed(), 0.001);
        assertEquals(0.0, context.getMaxForce(), 0.001);
    }

    // ==================== Getter Tests ====================

    @Test
    void getPosition_returnsCorrectValue() {
        BehaviorContext context = new BehaviorContext(testPosition, testVelocity, testMaxSpeed, testMaxForce);

        Vec3d position = context.getPosition();

        assertEquals(10.0, position.x, 0.001);
        assertEquals(64.0, position.y, 0.001);
        assertEquals(20.0, position.z, 0.001);
    }

    @Test
    void getPosition_returnsSameInstance() {
        BehaviorContext context = new BehaviorContext(testPosition, testVelocity, testMaxSpeed, testMaxForce);

        Vec3d position1 = context.getPosition();
        Vec3d position2 = context.getPosition();

        assertSame(position1, position2, "Should return same instance");
    }

    @Test
    void getVelocity_returnsCorrectValue() {
        BehaviorContext context = new BehaviorContext(testPosition, testVelocity, testMaxSpeed, testMaxForce);

        Vec3d velocity = context.getVelocity();

        assertEquals(0.1, velocity.x, 0.001);
        assertEquals(0.0, velocity.y, 0.001);
        assertEquals(0.2, velocity.z, 0.001);
    }

    @Test
    void getVelocity_returnsSameInstance() {
        BehaviorContext context = new BehaviorContext(testPosition, testVelocity, testMaxSpeed, testMaxForce);

        Vec3d velocity1 = context.getVelocity();
        Vec3d velocity2 = context.getVelocity();

        assertSame(velocity1, velocity2, "Should return same instance");
    }

    @Test
    void getMaxSpeed_returnsCorrectValue() {
        BehaviorContext context = new BehaviorContext(testPosition, testVelocity, 2.5, testMaxForce);

        assertEquals(2.5, context.getMaxSpeed(), 0.001);
    }

    @Test
    void getMaxForce_returnsCorrectValue() {
        BehaviorContext context = new BehaviorContext(testPosition, testVelocity, testMaxSpeed, 0.75);

        assertEquals(0.75, context.getMaxForce(), 0.001);
    }

    @Test
    void getEntity_returnsNullForTestConstructor() {
        BehaviorContext context = new BehaviorContext(testPosition, testVelocity, testMaxSpeed, testMaxForce);

        assertNull(context.getEntity());
    }

    @Test
    void getMob_returnsNullForTestConstructor() {
        BehaviorContext context = new BehaviorContext(testPosition, testVelocity, testMaxSpeed, testMaxForce);

        assertNull(context.getMob());
    }

    @Test
    void getLevel_returnsNullForTestConstructor() {
        BehaviorContext context = new BehaviorContext(testPosition, testVelocity, testMaxSpeed, testMaxForce);

        assertNull(context.getLevel());
    }

    @Test
    void getNeighbors_returnsEmptyListInitially() {
        BehaviorContext context = new BehaviorContext(testPosition, testVelocity, testMaxSpeed, testMaxForce);

        assertTrue(context.getNeighbors().isEmpty());
    }

    // ==================== Setter Tests ====================

    @Test
    void setNeighbors_withValidList_setsCorrectly() {
        BehaviorContext context = new BehaviorContext(testPosition, testVelocity, testMaxSpeed, testMaxForce);

        List<net.minecraft.world.entity.Entity> neighbors = createMockEntityList(3);
        context.setNeighbors(neighbors);

        assertEquals(3, context.getNeighbors().size());
    }

    @Test
    void setNeighbors_withNull_createsEmptyList() {
        BehaviorContext context = new BehaviorContext(testPosition, testVelocity, testMaxSpeed, testMaxForce);

        context.setNeighbors(null);

        assertTrue(context.getNeighbors().isEmpty());
    }

    @Test
    void setNeighbors_withEmptyList_worksCorrectly() {
        BehaviorContext context = new BehaviorContext(testPosition, testVelocity, testMaxSpeed, testMaxForce);

        List<net.minecraft.world.entity.Entity> neighbors = new ArrayList<>();
        context.setNeighbors(neighbors);

        assertTrue(context.getNeighbors().isEmpty());
    }

    @Test
    void setNeighbors_replacesExistingList() {
        BehaviorContext context = new BehaviorContext(testPosition, testVelocity, testMaxSpeed, testMaxForce);

        List<net.minecraft.world.entity.Entity> neighbors1 = createMockEntityList(2);
        context.setNeighbors(neighbors1);
        assertEquals(2, context.getNeighbors().size());

        List<net.minecraft.world.entity.Entity> neighbors2 = createMockEntityList(5);
        context.setNeighbors(neighbors2);
        assertEquals(5, context.getNeighbors().size());
    }

    @Test
    void getNeighbors_returnsModifiableList() {
        BehaviorContext context = new BehaviorContext(testPosition, testVelocity, testMaxSpeed, testMaxForce);

        List<net.minecraft.world.entity.Entity> neighbors = context.getNeighbors();
        neighbors.add(null);

        assertEquals(1, context.getNeighbors().size());
    }

    // ==================== Builder Pattern Tests ====================

    @Test
    void builder_withRequiredParameters_throwsExceptionWithoutEntity() {
        BehaviorContext.Builder builder = new BehaviorContext.Builder();

        assertThrows(IllegalStateException.class, builder::build,
                     "Should throw exception when entity is not set");
    }

    // ==================== Backwards Compatibility Tests ====================

    @Test
    void getSelf_returnsEntity() {
        BehaviorContext context = new BehaviorContext(testPosition, testVelocity, testMaxSpeed, testMaxForce);

        assertNull(context.getSelf());
    }

    @Test
    void getWorld_returnsLevel() {
        BehaviorContext context = new BehaviorContext(testPosition, testVelocity, testMaxSpeed, testMaxForce);

        assertNull(context.getWorld());
    }

    @Test
    void getNearbyEntities_returnsNeighbors() {
        BehaviorContext context = new BehaviorContext(testPosition, testVelocity, testMaxSpeed, testMaxForce);

        assertSame(context.getNeighbors(), context.getNearbyEntities(),
                    "Should return same list as getNeighbors");
    }

    // ==================== Utility Method Tests ====================

    @Test
    void getSpeed_returnsVelocityMagnitude() {
        Vec3d velocity = new Vec3d(3.0, 4.0, 0.0);
        BehaviorContext context = new BehaviorContext(testPosition, velocity, testMaxSpeed, testMaxForce);

        double speed = context.getSpeed();

        assertEquals(5.0, speed, 0.001, "Speed should be magnitude of velocity");
    }

    @Test
    void getSpeed_withZeroVelocity_returnsZero() {
        Vec3d velocity = new Vec3d(0.0, 0.0, 0.0);
        BehaviorContext context = new BehaviorContext(testPosition, velocity, testMaxSpeed, testMaxForce);

        assertEquals(0.0, context.getSpeed(), 0.001);
    }

    @Test
    void getBlockPos_withNullEntity_returnsPositionAsBlockPos() {
        BehaviorContext context = new BehaviorContext(testPosition, testVelocity, testMaxSpeed, testMaxForce);

        BlockPos blockPos = context.getBlockPos();

        assertEquals(10, blockPos.getX());
        assertEquals(64, blockPos.getY());
        assertEquals(20, blockPos.getZ());
    }

    @Test
    void getBlockPos_withFractionalPosition_truncatesToIntegers() {
        Vec3d position = new Vec3d(10.7, 64.3, 20.9);
        BehaviorContext context = new BehaviorContext(position, testVelocity, testMaxSpeed, testMaxForce);

        BlockPos blockPos = context.getBlockPos();

        assertEquals(10, blockPos.getX());
        assertEquals(64, blockPos.getY());
        assertEquals(20, blockPos.getZ());
    }

    // ==================== Edge Case Tests ====================

    @Test
    void edgeCase_veryLargeSpeed_worksCorrectly() {
        BehaviorContext context = new BehaviorContext(testPosition, testVelocity, 1000.0, testMaxForce);

        assertEquals(1000.0, context.getMaxSpeed(), 0.001);
    }

    @Test
    void edgeCase_veryLargeForce_worksCorrectly() {
        BehaviorContext context = new BehaviorContext(testPosition, testVelocity, testMaxSpeed, 500.0);

        assertEquals(500.0, context.getMaxForce(), 0.001);
    }

    @Test
    void edgeCase_negativeVelocity_componentsPreserved() {
        Vec3d negativeVelocity = new Vec3d(-0.5, -0.3, -0.7);
        BehaviorContext context = new BehaviorContext(testPosition, negativeVelocity, testMaxSpeed, testMaxForce);

        assertEquals(-0.5, context.getVelocity().x, 0.001);
        assertEquals(-0.3, context.getVelocity().y, 0.001);
        assertEquals(-0.7, context.getVelocity().z, 0.001);
    }

    @Test
    void edgeCase_negativePosition_componentsPreserved() {
        Vec3d negativePosition = new Vec3d(-10.0, -64.0, -20.0);
        BehaviorContext context = new BehaviorContext(negativePosition, testVelocity, testMaxSpeed, testMaxForce);

        assertEquals(-10.0, context.getPosition().x, 0.001);
        assertEquals(-64.0, context.getPosition().y, 0.001);
        assertEquals(-20.0, context.getPosition().z, 0.001);
    }

    @Test
    void edgeCase_positionAndVelocityAreMutable_sharedReference() {
        BehaviorContext context = new BehaviorContext(testPosition, testVelocity, testMaxSpeed, testMaxForce);

        Vec3d position = context.getPosition();
        Vec3d velocity = context.getVelocity();

        position.x = 999.0;
        velocity.x = 888.0;

        assertEquals(999.0, context.getPosition().x, 0.001,
                     "Vec3d is mutable - modifications affect the context");
        assertEquals(888.0, context.getVelocity().x, 0.001,
                     "Vec3d is mutable - modifications affect the context");
    }

    @Test
    void edgeCase_creatingIndependentCopy_isolationWorks() {
        BehaviorContext context = new BehaviorContext(testPosition, testVelocity, testMaxSpeed, testMaxForce);

        Vec3d positionCopy = context.getPosition().copy();
        Vec3d velocityCopy = context.getVelocity().copy();

        positionCopy.x = 999.0;
        velocityCopy.x = 888.0;

        assertEquals(10.0, context.getPosition().x, 0.001,
                     "Modifying copy should not affect context");
        assertEquals(0.1, context.getVelocity().x, 0.001,
                     "Modifying copy should not affect context");
    }

    // ==================== Helper Methods ====================

    /**
     * Creates a list of mock entities using null placeholders.
     * This avoids the complexity of creating actual Entity instances in tests.
     */
    private List<net.minecraft.world.entity.Entity> createMockEntityList(int count) {
        List<net.minecraft.world.entity.Entity> entities = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            entities.add(null);
        }
        return entities;
    }
}
