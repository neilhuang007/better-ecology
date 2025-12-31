package me.javavirtualenv.behavior.territorial;

import net.minecraft.core.BlockPos;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Represents a defended territory owned by an entity.
 * Territories have defined boundaries and can be marked with scent or visual markers.
 */
public class Territory {
    private final UUID ownerId;
    private final BlockPos center;
    private final double radius;
    private final List<BlockPos> markers;
    private final long establishedTime;

    public Territory(UUID ownerId, BlockPos center, double radius) {
        this.ownerId = ownerId;
        this.center = center;
        this.radius = radius;
        this.markers = new ArrayList<>();
        this.establishedTime = System.currentTimeMillis();
    }

    /**
     * Checks if a position is within this territory
     */
    public boolean contains(BlockPos position) {
        double dx = position.getX() - center.getX();
        double dy = position.getY() - center.getY();
        double dz = position.getZ() - center.getZ();
        double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
        return distance <= radius;
    }

    /**
     * Checks if this territory is owned by the specified entity
     */
    public boolean isOwnedBy(UUID entityId) {
        return ownerId.equals(entityId);
    }

    /**
     * Adds a marker position to the territory
     */
    public void addMarker(BlockPos markerPos) {
        if (!markers.contains(markerPos)) {
            markers.add(markerPos);
        }
    }

    /**
     * Gets all marker positions in this territory
     */
    public List<BlockPos> getMarkers() {
        return new ArrayList<>(markers);
    }

    /**
     * Removes a marker at the specified position
     */
    public void removeMarker(BlockPos markerPos) {
        markers.remove(markerPos);
    }

    /**
     * Checks if this territory overlaps with another territory
     */
    public boolean overlaps(Territory other) {
        double dx = this.center.getX() - other.center.getX();
        double dy = this.center.getY() - other.center.getY();
        double dz = this.center.getZ() - other.center.getZ();
        double distanceBetweenCenters = Math.sqrt(dx * dx + dy * dy + dz * dz);

        return distanceBetweenCenters < (this.radius + other.radius);
    }

    /**
     * Calculates the amount of overlap between this territory and another
     * Returns 0 if no overlap, positive value indicating overlap distance
     */
    public double getOverlapAmount(Territory other) {
        double dx = this.center.getX() - other.center.getX();
        double dy = this.center.getY() - other.center.getY();
        double dz = this.center.getZ() - other.center.getZ();
        double distanceBetweenCenters = Math.sqrt(dx * dx + dy * dy + dz * dz);

        double combinedRadius = this.radius + other.radius;
        double overlap = combinedRadius - distanceBetweenCenters;

        return Math.max(0, overlap);
    }

    /**
     * Gets the owner's UUID
     */
    public UUID getOwnerId() {
        return ownerId;
    }

    /**
     * Gets the center position of the territory
     */
    public BlockPos getCenter() {
        return center;
    }

    /**
     * Gets the radius of the territory
     */
    public double getRadius() {
        return radius;
    }

    /**
     * Gets the time when this territory was established
     */
    public long getEstablishedTime() {
        return establishedTime;
    }

    /**
     * Gets the age of the territory in milliseconds
     */
    public long getAge() {
        return System.currentTimeMillis() - establishedTime;
    }

    /**
     * Calculates distance from a position to the territory center
     */
    public double distanceToCenter(BlockPos position) {
        double dx = position.getX() - center.getX();
        double dy = position.getY() - center.getY();
        double dz = position.getZ() - center.getZ();
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    /**
     * Calculates distance from a position to the territory edge
     * Positive values = outside territory, negative = inside
     */
    public double distanceToEdge(BlockPos position) {
        return distanceToCenter(position) - radius;
    }
}
