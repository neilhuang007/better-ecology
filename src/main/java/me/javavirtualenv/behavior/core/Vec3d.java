package me.javavirtualenv.behavior.core;

import net.minecraft.world.phys.Vec3;

import static java.lang.Math.*;

/**
 * Lightweight 3D vector class optimized for Minecraft behavior algorithms.
 * Provides mutable vector operations for efficient boids and steering behaviors.
 */
public class Vec3d {

    public double x;
    public double y;
    public double z;

    /**
     * Default constructor initializing to (0, 0, 0).
     */
    public Vec3d() {
        this(0.0, 0.0, 0.0);
    }

    /**
     * Constructs a vector from individual components.
     */
    public Vec3d(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    /**
     * Copy constructor.
     */
    public Vec3d(Vec3d other) {
        this.x = other.x;
        this.y = other.y;
        this.z = other.z;
    }

    /**
     * Constructs from Minecraft's Vec3.
     */
    public Vec3d(Vec3 minecraftVec) {
        this.x = minecraftVec.x;
        this.y = minecraftVec.y;
        this.z = minecraftVec.z;
    }

    /**
     * Static factory method from Minecraft's Vec3.
     */
    public static Vec3d fromMinecraftVec3(Vec3 minecraftVec) {
        return new Vec3d(minecraftVec);
    }

    /**
     * Converts this vector to Minecraft's Vec3 type.
     */
    public Vec3 toMinecraftVec3() {
        return new Vec3(this.x, this.y, this.z);
    }

    /**
     * Adds a scalar value to all components (mutates this vector).
     */
    public void add(double scalar) {
        this.x += scalar;
        this.y += scalar;
        this.z += scalar;
    }

    /**
     * Adds another vector to this vector (mutates this vector).
     */
    public void add(Vec3d other) {
        this.x += other.x;
        this.y += other.y;
        this.z += other.z;
    }

    /**
     * Subtracts another vector from this vector (mutates this vector).
     */
    public void sub(Vec3d other) {
        this.x -= other.x;
        this.y -= other.y;
        this.z -= other.z;
    }

    /**
     * Static subtraction returning a new vector.
     */
    public static Vec3d sub(Vec3d a, Vec3d b) {
        return new Vec3d(a.x - b.x, a.y - b.y, a.z - b.z);
    }

    /**
     * Multiplies all components by a scalar (mutates this vector).
     */
    public void mult(double scalar) {
        this.x *= scalar;
        this.y *= scalar;
        this.z *= scalar;
    }

    /**
     * Divides all components by a scalar (mutates this vector).
     */
    public void div(double scalar) {
        if (scalar != 0) {
            this.x /= scalar;
            this.y /= scalar;
            this.z /= scalar;
        }
    }

    /**
     * Returns the magnitude (length) of this vector.
     */
    public double magnitude() {
        return sqrt(x * x + y * y + z * z);
    }

    /**
     * Normalizes this vector to unit length (mutates this vector).
     * If magnitude is zero, vector remains unchanged.
     */
    public void normalize() {
        double mag = magnitude();
        if (mag != 0 && mag != 1.0) {
            div(mag);
        }
    }

    /**
     * Limits the magnitude of this vector to a maximum value (mutates this vector).
     */
    public void limit(double max) {
        double mag = magnitude();
        if (mag > max) {
            normalize();
            mult(max);
        }
    }

    /**
     * Returns the horizontal heading angle (yaw) in radians.
     * Uses atan2(x, z) for Minecraft's coordinate system where Z is forward.
     */
    public double heading() {
        return atan2(this.x, this.z);
    }

    /**
     * Computes the dot product with another vector.
     */
    public double dot(Vec3d other) {
        return this.x * other.x + this.y * other.y + this.z * other.z;
    }

    /**
     * Computes the cross product with another vector, returning a new vector.
     */
    public Vec3d cross(Vec3d other) {
        double crossX = this.y * other.z - this.z * other.y;
        double crossY = this.z * other.x - this.x * other.z;
        double crossZ = this.x * other.y - this.y * other.x;
        return new Vec3d(crossX, crossY, crossZ);
    }

    /**
     * Computes the Euclidean distance to another vector.
     */
    public double distanceTo(Vec3d other) {
        return dist(this, other);
    }

    /**
     * Static method to compute distance between two vectors.
     */
    public static double dist(Vec3d a, Vec3d b) {
        double dx = a.x - b.x;
        double dy = a.y - b.y;
        double dz = a.z - b.z;
        return sqrt(dx * dx + dy * dy + dz * dz);
    }

    /**
     * Computes the angle between this vector and another in radians.
     * Returns 0 if either vector has zero magnitude.
     */
    public double angleBetween(Vec3d other) {
        double mag1 = magnitude();
        double mag2 = other.magnitude();

        if (mag1 == 0 || mag2 == 0) {
            return 0;
        }

        double dotProduct = dot(other);
        double cosAngle = dotProduct / (mag1 * mag2);

        // Clamp to [-1, 1] to handle floating point errors
        cosAngle = max(-1.0, min(1.0, cosAngle));

        return acos(cosAngle);
    }

    /**
     * Creates and returns a copy of this vector.
     */
    public Vec3d copy() {
        return new Vec3d(this.x, this.y, this.z);
    }

    @Override
    public String toString() {
        return String.format("Vec3d(%.2f, %.2f, %.2f)", x, y, z);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        Vec3d other = (Vec3d) obj;
        return Double.compare(other.x, x) == 0 &&
               Double.compare(other.y, y) == 0 &&
               Double.compare(other.z, z) == 0;
    }

    @Override
    public int hashCode() {
        long bits = 1L;
        bits = 31L * bits + Double.doubleToLongBits(x);
        bits = 31L * bits + Double.doubleToLongBits(y);
        bits = 31L * bits + Double.doubleToLongBits(z);
        return (int)(bits ^ (bits >>> 32));
    }
}
