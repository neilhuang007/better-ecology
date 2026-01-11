package me.javavirtualenv.behavior.core;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import net.minecraft.world.phys.Vec3;

/**
 * Comprehensive unit tests for Vec3d vector class.
 * Tests all vector operations including constructors, arithmetic operations,
 * normalization, distance calculations, and edge cases.
 */
class Vec3dTest {

    private static final double DELTA = 0.0001;

    @Test
    void defaultConstructorCreatesZeroVector() {
        Vec3d vec = new Vec3d();

        assertEquals(0.0, vec.x, DELTA);
        assertEquals(0.0, vec.y, DELTA);
        assertEquals(0.0, vec.z, DELTA);
    }

    @Test
    void componentConstructorCreatesVector() {
        Vec3d vec = new Vec3d(1.5, 2.5, 3.5);

        assertEquals(1.5, vec.x, DELTA);
        assertEquals(2.5, vec.y, DELTA);
        assertEquals(3.5, vec.z, DELTA);
    }

    @Test
    void copyConstructorCreatesIndependentCopy() {
        Vec3d original = new Vec3d(1.0, 2.0, 3.0);
        Vec3d copy = new Vec3d(original);

        assertEquals(original.x, copy.x, DELTA);
        assertEquals(original.y, copy.y, DELTA);
        assertEquals(original.z, copy.z, DELTA);

        // Verify independence
        copy.x = 10.0;
        assertNotEquals(original.x, copy.x);
        assertEquals(1.0, original.x, DELTA);
    }

    @Test
    void minecraftVecConstructorCreatesVector() {
        Vec3 minecraftVec = new Vec3(1.5, 2.5, 3.5);
        Vec3d vec = new Vec3d(minecraftVec);

        assertEquals(minecraftVec.x, vec.x, DELTA);
        assertEquals(minecraftVec.y, vec.y, DELTA);
        assertEquals(minecraftVec.z, vec.z, DELTA);
    }

    @Test
    void addScalarAddsToAllComponents() {
        Vec3d vec = new Vec3d(1.0, 2.0, 3.0);
        vec.add(5.0);

        assertEquals(6.0, vec.x, DELTA);
        assertEquals(7.0, vec.y, DELTA);
        assertEquals(8.0, vec.z, DELTA);
    }

    @Test
    void addVectorAddsComponentWise() {
        Vec3d vec1 = new Vec3d(1.0, 2.0, 3.0);
        Vec3d vec2 = new Vec3d(4.0, 5.0, 6.0);
        vec1.add(vec2);

        assertEquals(5.0, vec1.x, DELTA);
        assertEquals(7.0, vec1.y, DELTA);
        assertEquals(9.0, vec1.z, DELTA);
    }

    @Test
    void subVectorSubtractsComponentWise() {
        Vec3d vec1 = new Vec3d(5.0, 7.0, 9.0);
        Vec3d vec2 = new Vec3d(1.0, 2.0, 3.0);
        vec1.sub(vec2);

        assertEquals(4.0, vec1.x, DELTA);
        assertEquals(5.0, vec1.y, DELTA);
        assertEquals(6.0, vec1.z, DELTA);
    }

    @Test
    void staticSubReturnsNewVector() {
        Vec3d vec1 = new Vec3d(5.0, 7.0, 9.0);
        Vec3d vec2 = new Vec3d(1.0, 2.0, 3.0);
        Vec3d result = Vec3d.sub(vec1, vec2);

        assertEquals(4.0, result.x, DELTA);
        assertEquals(5.0, result.y, DELTA);
        assertEquals(6.0, result.z, DELTA);

        // Verify original vectors unchanged
        assertEquals(5.0, vec1.x, DELTA);
        assertEquals(1.0, vec2.x, DELTA);
    }

    @Test
    void staticAddReturnsNewVector() {
        Vec3d vec1 = new Vec3d(1.0, 2.0, 3.0);
        Vec3d vec2 = new Vec3d(4.0, 5.0, 6.0);
        Vec3d result = Vec3d.add(vec1, vec2);

        assertEquals(5.0, result.x, DELTA);
        assertEquals(7.0, result.y, DELTA);
        assertEquals(9.0, result.z, DELTA);

        // Verify original vectors unchanged
        assertEquals(1.0, vec1.x, DELTA);
        assertEquals(4.0, vec2.x, DELTA);
    }

    @Test
    void multScalarMultipliesAllComponents() {
        Vec3d vec = new Vec3d(1.0, 2.0, 3.0);
        vec.mult(2.0);

        assertEquals(2.0, vec.x, DELTA);
        assertEquals(4.0, vec.y, DELTA);
        assertEquals(6.0, vec.z, DELTA);
    }

    @Test
    void staticMultReturnsNewVector() {
        Vec3d vec = new Vec3d(1.0, 2.0, 3.0);
        Vec3d result = Vec3d.mult(vec, 2.0);

        assertEquals(2.0, result.x, DELTA);
        assertEquals(4.0, result.y, DELTA);
        assertEquals(6.0, result.z, DELTA);

        // Verify original vector unchanged
        assertEquals(1.0, vec.x, DELTA);
    }

    @Test
    void divScalarDividesAllComponents() {
        Vec3d vec = new Vec3d(6.0, 8.0, 10.0);
        vec.div(2.0);

        assertEquals(3.0, vec.x, DELTA);
        assertEquals(4.0, vec.y, DELTA);
        assertEquals(5.0, vec.z, DELTA);
    }

    @Test
    void divByZeroDoesNothing() {
        Vec3d vec = new Vec3d(1.0, 2.0, 3.0);
        vec.div(0.0);

        assertEquals(1.0, vec.x, DELTA);
        assertEquals(2.0, vec.y, DELTA);
        assertEquals(3.0, vec.z, DELTA);
    }

    @Test
    void magnitudeCalculatesLength() {
        Vec3d vec = new Vec3d(3.0, 4.0, 0.0);

        assertEquals(5.0, vec.magnitude(), DELTA);
    }

    @Test
    void magnitudeForZeroVector() {
        Vec3d vec = new Vec3d();

        assertEquals(0.0, vec.magnitude(), DELTA);
    }

    @Test
    void magnitudeForThreeDimensionalVector() {
        Vec3d vec = new Vec3d(1.0, 2.0, 2.0);

        assertEquals(3.0, vec.magnitude(), DELTA);
    }

    @Test
    void normalizeCreatesUnitVector() {
        Vec3d vec = new Vec3d(3.0, 4.0, 0.0);
        vec.normalize();

        assertEquals(1.0, vec.magnitude(), DELTA);
        assertEquals(0.6, vec.x, DELTA);
        assertEquals(0.8, vec.y, DELTA);
        assertEquals(0.0, vec.z, DELTA);
    }

    @Test
    void normalizeZeroVectorRemainsUnchanged() {
        Vec3d vec = new Vec3d();
        vec.normalize();

        assertEquals(0.0, vec.x, DELTA);
        assertEquals(0.0, vec.y, DELTA);
        assertEquals(0.0, vec.z, DELTA);
    }

    @Test
    void normalizeUnitVectorRemainsUnchanged() {
        Vec3d vec = new Vec3d(1.0, 0.0, 0.0);
        vec.normalize();

        assertEquals(1.0, vec.x, DELTA);
        assertEquals(0.0, vec.y, DELTA);
        assertEquals(0.0, vec.z, DELTA);
    }

    @Test
    void normalizedReturnsNewNormalizedVector() {
        Vec3d vec = new Vec3d(3.0, 4.0, 0.0);
        Vec3d normalized = vec.normalized();

        assertEquals(1.0, normalized.magnitude(), DELTA);
        assertEquals(0.6, normalized.x, DELTA);
        assertEquals(0.8, normalized.y, DELTA);

        // Verify original vector unchanged
        assertEquals(3.0, vec.x, DELTA);
        assertEquals(4.0, vec.y, DELTA);
    }

    @Test
    void limitRestrictsMagnitude() {
        Vec3d vec = new Vec3d(10.0, 0.0, 0.0);
        vec.limit(5.0);

        assertEquals(5.0, vec.magnitude(), DELTA);
    }

    @Test
    void limitDoesNotChangeSmallerVector() {
        Vec3d vec = new Vec3d(3.0, 0.0, 0.0);
        vec.limit(5.0);

        assertEquals(3.0, vec.magnitude(), DELTA);
        assertEquals(3.0, vec.x, DELTA);
    }

    @Test
    void limitZeroVectorRemainsUnchanged() {
        Vec3d vec = new Vec3d();
        vec.limit(5.0);

        assertEquals(0.0, vec.magnitude(), DELTA);
    }

    @Test
    void dotProductCalculatesCorrectly() {
        Vec3d vec1 = new Vec3d(1.0, 2.0, 3.0);
        Vec3d vec2 = new Vec3d(4.0, 5.0, 6.0);

        assertEquals(32.0, vec1.dot(vec2), DELTA);
    }

    @Test
    void dotProductWithOrthogonalVectors() {
        Vec3d vec1 = new Vec3d(1.0, 0.0, 0.0);
        Vec3d vec2 = new Vec3d(0.0, 1.0, 0.0);

        assertEquals(0.0, vec1.dot(vec2), DELTA);
    }

    @Test
    void dotProductWithParallelVectors() {
        Vec3d vec1 = new Vec3d(1.0, 0.0, 0.0);
        Vec3d vec2 = new Vec3d(2.0, 0.0, 0.0);

        assertEquals(2.0, vec1.dot(vec2), DELTA);
    }

    @Test
    void crossProductCalculatesCorrectly() {
        Vec3d vec1 = new Vec3d(1.0, 0.0, 0.0);
        Vec3d vec2 = new Vec3d(0.0, 1.0, 0.0);
        Vec3d result = vec1.cross(vec2);

        assertEquals(0.0, result.x, DELTA);
        assertEquals(0.0, result.y, DELTA);
        assertEquals(1.0, result.z, DELTA);
    }

    @Test
    void crossProductIsAntiCommutative() {
        Vec3d vec1 = new Vec3d(1.0, 0.0, 0.0);
        Vec3d vec2 = new Vec3d(0.0, 1.0, 0.0);

        Vec3d cross1 = vec1.cross(vec2);
        Vec3d cross2 = vec2.cross(vec1);

        assertEquals(-1.0 * cross1.x, cross2.x, DELTA);
        assertEquals(-1.0 * cross1.y, cross2.y, DELTA);
        assertEquals(-1.0 * cross1.z, cross2.z, DELTA);
    }

    @Test
    void distanceToCalculatesCorrectly() {
        Vec3d vec1 = new Vec3d(0.0, 0.0, 0.0);
        Vec3d vec2 = new Vec3d(3.0, 4.0, 0.0);

        assertEquals(5.0, vec1.distanceTo(vec2), DELTA);
    }

    @Test
    void distanceToWithCoordinatesCalculatesCorrectly() {
        Vec3d vec = new Vec3d(0.0, 0.0, 0.0);

        assertEquals(5.0, vec.distanceTo(3.0, 4.0, 0.0), DELTA);
    }

    @Test
    void staticDistCalculatesCorrectly() {
        Vec3d vec1 = new Vec3d(0.0, 0.0, 0.0);
        Vec3d vec2 = new Vec3d(3.0, 4.0, 0.0);

        assertEquals(5.0, Vec3d.dist(vec1, vec2), DELTA);
    }

    @Test
    void distanceToSamePointIsZero() {
        Vec3d vec = new Vec3d(1.0, 2.0, 3.0);

        assertEquals(0.0, vec.distanceTo(vec), DELTA);
        assertEquals(0.0, vec.distanceTo(1.0, 2.0, 3.0), DELTA);
    }

    @Test
    void angleBetweenParallelVectors() {
        Vec3d vec1 = new Vec3d(1.0, 0.0, 0.0);
        Vec3d vec2 = new Vec3d(2.0, 0.0, 0.0);

        assertEquals(0.0, vec1.angleBetween(vec2), DELTA);
    }

    @Test
    void angleBetweenOrthogonalVectors() {
        Vec3d vec1 = new Vec3d(1.0, 0.0, 0.0);
        Vec3d vec2 = new Vec3d(0.0, 1.0, 0.0);

        assertEquals(Math.PI / 2, vec1.angleBetween(vec2), DELTA);
    }

    @Test
    void angleBetweenOppositeVectors() {
        Vec3d vec1 = new Vec3d(1.0, 0.0, 0.0);
        Vec3d vec2 = new Vec3d(-1.0, 0.0, 0.0);

        assertEquals(Math.PI, vec1.angleBetween(vec2), DELTA);
    }

    @Test
    void angleBetweenWithZeroVector() {
        Vec3d vec1 = new Vec3d(1.0, 0.0, 0.0);
        Vec3d vec2 = new Vec3d();

        assertEquals(0.0, vec1.angleBetween(vec2), DELTA);
    }

    @Test
    void angleBetweenTwoZeroVectors() {
        Vec3d vec1 = new Vec3d();
        Vec3d vec2 = new Vec3d();

        assertEquals(0.0, vec1.angleBetween(vec2), DELTA);
    }

    @Test
    void headingCalculatesCorrectly() {
        Vec3d vec = new Vec3d(1.0, 0.0, 1.0);

        assertEquals(Math.PI / 4, vec.heading(), DELTA);
    }

    @Test
    void headingForForwardVector() {
        Vec3d vec = new Vec3d(0.0, 0.0, 1.0);

        assertEquals(0.0, vec.heading(), DELTA);
    }

    @Test
    void headingForRightVector() {
        Vec3d vec = new Vec3d(1.0, 0.0, 0.0);

        assertEquals(Math.PI / 2, vec.heading(), DELTA);
    }

    @Test
    void copyCreatesIndependentCopy() {
        Vec3d original = new Vec3d(1.0, 2.0, 3.0);
        Vec3d copy = original.copy();

        assertEquals(original.x, copy.x, DELTA);
        assertEquals(original.y, copy.y, DELTA);
        assertEquals(original.z, copy.z, DELTA);

        // Verify independence
        copy.x = 10.0;
        assertNotEquals(original.x, copy.x);
        assertEquals(1.0, original.x, DELTA);
    }

    @Test
    void toStringFormatsCorrectly() {
        Vec3d vec = new Vec3d(1.234, 2.567, 3.890);

        String result = vec.toString();

        assertTrue(result.contains("1.23"));
        assertTrue(result.contains("2.57"));
        assertTrue(result.contains("3.89"));
    }

    @Test
    void equalsWithSameInstance() {
        Vec3d vec = new Vec3d(1.0, 2.0, 3.0);

        assertTrue(vec.equals(vec));
    }

    @Test
    void equalsWithEqualComponents() {
        Vec3d vec1 = new Vec3d(1.0, 2.0, 3.0);
        Vec3d vec2 = new Vec3d(1.0, 2.0, 3.0);

        assertTrue(vec1.equals(vec2));
    }

    @Test
    void equalsWithDifferentComponents() {
        Vec3d vec1 = new Vec3d(1.0, 2.0, 3.0);
        Vec3d vec2 = new Vec3d(1.0, 2.0, 4.0);

        assertFalse(vec1.equals(vec2));
    }

    @Test
    void equalsWithNull() {
        Vec3d vec = new Vec3d(1.0, 2.0, 3.0);

        assertFalse(vec.equals(null));
    }

    @Test
    void equalsWithDifferentType() {
        Vec3d vec = new Vec3d(1.0, 2.0, 3.0);

        assertFalse(vec.equals("not a vector"));
    }

    @Test
    void hashCodeConsistentWithEquals() {
        Vec3d vec1 = new Vec3d(1.0, 2.0, 3.0);
        Vec3d vec2 = new Vec3d(1.0, 2.0, 3.0);
        Vec3d vec3 = new Vec3d(1.0, 2.0, 4.0);

        assertEquals(vec1.hashCode(), vec2.hashCode());
        assertNotEquals(vec1.hashCode(), vec3.hashCode());
    }

    @Test
    void fromMinecraftVec3CreatesVector() {
        Vec3 minecraftVec = new Vec3(1.5, 2.5, 3.5);
        Vec3d vec = Vec3d.fromMinecraftVec3(minecraftVec);

        assertEquals(minecraftVec.x, vec.x, DELTA);
        assertEquals(minecraftVec.y, vec.y, DELTA);
        assertEquals(minecraftVec.z, vec.z, DELTA);
    }

    @Test
    void toMinecraftVec3ConvertsCorrectly() {
        Vec3d vec = new Vec3d(1.5, 2.5, 3.5);
        Vec3 minecraftVec = vec.toMinecraftVec3();

        assertEquals(vec.x, minecraftVec.x, DELTA);
        assertEquals(vec.y, minecraftVec.y, DELTA);
        assertEquals(vec.z, minecraftVec.z, DELTA);
    }

    @Test
    void minecraftVec3RoundTripConversion() {
        Vec3d original = new Vec3d(1.5, 2.5, 3.5);
        Vec3 minecraftVec = original.toMinecraftVec3();
        Vec3d converted = Vec3d.fromMinecraftVec3(minecraftVec);

        assertEquals(original.x, converted.x, DELTA);
        assertEquals(original.y, converted.y, DELTA);
        assertEquals(original.z, converted.z, DELTA);
    }

    @Test
    void negativeComponentsHandledCorrectly() {
        Vec3d vec = new Vec3d(-1.0, -2.0, -3.0);

        assertEquals(-1.0, vec.x, DELTA);
        assertEquals(-2.0, vec.y, DELTA);
        assertEquals(-3.0, vec.z, DELTA);
        assertEquals(Math.sqrt(14), vec.magnitude(), DELTA);
    }

    @Test
    void veryLargeNumbersHandledCorrectly() {
        Vec3d vec = new Vec3d(1e10, 1e10, 1e10);

        assertEquals(1e10, vec.x, DELTA);
        assertEquals(1e10, vec.y, DELTA);
        assertEquals(1e10, vec.z, DELTA);
    }

    @Test
    void verySmallNumbersHandledCorrectly() {
        Vec3d vec = new Vec3d(1e-10, 1e-10, 1e-10);

        assertEquals(1e-10, vec.x, DELTA);
        assertEquals(1e-10, vec.y, DELTA);
        assertEquals(1e-10, vec.z, DELTA);
    }

    @Test
    void normalizeAlreadyNormalizedVector() {
        Vec3d vec = new Vec3d(1.0, 0.0, 0.0);
        double originalX = vec.x;

        vec.normalize();

        assertEquals(originalX, vec.x, DELTA);
    }

    @Test
    void negativeMagnitudeHandledCorrectly() {
        Vec3d vec = new Vec3d(-1.0, 0.0, 0.0);

        assertEquals(1.0, vec.magnitude(), DELTA);
    }

    @Test
    void crossProductWithParallelVectors() {
        Vec3d vec1 = new Vec3d(1.0, 0.0, 0.0);
        Vec3d vec2 = new Vec3d(2.0, 0.0, 0.0);
        Vec3d result = vec1.cross(vec2);

        assertEquals(0.0, result.x, DELTA);
        assertEquals(0.0, result.y, DELTA);
        assertEquals(0.0, result.z, DELTA);
    }

    @Test
    void angleBetweenHandlesFloatingPointErrors() {
        Vec3d vec1 = new Vec3d(1.0, 0.0, 0.0);
        Vec3d vec2 = new Vec3d(Math.cos(0.1), Math.sin(0.1), 0.0);

        double angle = vec1.angleBetween(vec2);

        assertTrue(angle >= 0.0 && angle <= Math.PI);
        assertEquals(0.1, angle, 0.001);
    }

    @Test
    void threeDimensionalCrossProduct() {
        Vec3d vec1 = new Vec3d(2.0, 3.0, 4.0);
        Vec3d vec2 = new Vec3d(5.0, 6.0, 7.0);
        Vec3d result = vec1.cross(vec2);

        assertEquals(-3.0, result.x, DELTA);
        assertEquals(6.0, result.y, DELTA);
        assertEquals(-3.0, result.z, DELTA);
    }
}
