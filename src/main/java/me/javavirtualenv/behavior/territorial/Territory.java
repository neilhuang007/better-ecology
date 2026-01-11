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
    private final double boundaryOverlapThreshold;

    public Territory(UUID ownerId, BlockPos center, double radius) {
        this(ownerId, center, radius, 0.3);
    }

    public Territory(UUID ownerId, BlockPos center, double radius, double boundaryOverlapThreshold) {
        this.ownerId = ownerId;
        this.center = center;
        this.radius = radius;
        this.markers = new ArrayList<>();
        this.establishedTime = System.currentTimeMillis();
        this.boundaryOverlapThreshold = Math.max(0.0, Math.min(1.0, boundaryOverlapThreshold));
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
     * Calculates the percentage of this territory that overlaps with another.
     * Returns 0.0 if no overlap, 1.0 if completely contained within other.
     * <p>
     * Scientific note: Territories often overlap at boundaries (buffer zones)
     * where conspecifics may have reduced aggression.
     */
    public double getOverlapPercentage(Territory other) {
        double overlapDistance = getOverlapAmount(other);

        if (overlapDistance <= 0) {
            return 0.0;
        }

        // Estimate overlap volume as percentage of this territory's volume
        // This is a simplified 3D sphere intersection calculation
        double myVolume = (4.0 / 3.0) * Math.PI * Math.pow(radius, 3);
        double otherVolume = (4.0 / 3.0) * Math.PI * Math.pow(other.radius, 3);

        // Approximate overlap volume using simplified formula
        double overlapVolume = Math.min(myVolume, otherVolume) *
                (overlapDistance / (this.radius + other.radius));

        return overlapVolume / myVolume;
    }

    /**
     * Checks if this territory has a boundary overlap (buffer zone) with another.
     * Boundary overlaps are common in territorial systems as buffer zones.
     */
    public boolean hasBoundaryOverlap(Territory other) {
        if (!overlaps(other)) {
            return false;
        }

        double overlapPercentage = getOverlapPercentage(other);
        // Boundary overlap is typically less than configured threshold of territory area
        return overlapPercentage > 0.0 && overlapPercentage < boundaryOverlapThreshold;
    }

    /**
     * Gets the boundary overlap threshold used for determining buffer zones.
     */
    public double getBoundaryOverlapThreshold() {
        return boundaryOverlapThreshold;
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
