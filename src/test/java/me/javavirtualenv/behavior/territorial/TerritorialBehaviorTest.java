package me.javavirtualenv.behavior.territorial;

import me.javavirtualenv.behavior.core.BehaviorContext;
import me.javavirtualenv.behavior.core.Vec3d;
import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive test suite for territorial behavior package.
 * Tests all core behaviors scientifically without using Mockito.
 * Focuses on pure algorithm testing for HomeRange, Territory, and HomeRangeBehavior.
 */
class TerritorialBehaviorTest {

    private BlockPos centerPos;
    private BlockPos edgePos;
    private BlockPos outsidePos;
    private BlockPos farOutsidePos;
    private UUID ownerId;
    private UUID otherOwnerId;

    @BeforeEach
    void setUp() {
        centerPos = new BlockPos(0, 64, 0);
        edgePos = new BlockPos(50, 64, 0);
        outsidePos = new BlockPos(60, 64, 0);
        farOutsidePos = new BlockPos(100, 64, 0);
        ownerId = UUID.randomUUID();
        otherOwnerId = UUID.randomUUID();
    }

    // ==================== HomeRange Tests ====================

    @Test
    void homeRange_initializesCorrectly() {
        double radius = 50.0;
        HomeRange homeRange = new HomeRange(centerPos, radius);

        assertNotNull(homeRange, "HomeRange should initialize");
        assertEquals(centerPos, homeRange.getCenter(), "Center should match input");
        assertEquals(radius, homeRange.getRadius(), 0.001, "Radius should match input");
        assertTrue(homeRange.getVisitedLocations().isEmpty(), "Should start with no visited locations");
    }

    @Test
    void homeRange_isWithinRange_atCenter() {
        HomeRange homeRange = new HomeRange(centerPos, 50.0);

        assertTrue(homeRange.isWithinRange(centerPos), "Center position should be within range");
    }

    @Test
    void homeRange_isWithinRange_atEdge() {
        HomeRange homeRange = new HomeRange(centerPos, 50.0);

        assertTrue(homeRange.isWithinRange(edgePos), "Edge position should be within range");
    }

    @Test
    void homeRange_isWithinRange_outside() {
        HomeRange homeRange = new HomeRange(centerPos, 50.0);

        assertFalse(homeRange.isWithinRange(outsidePos), "Outside position should not be within range");
    }

    @Test
    void homeRange_recordVisit_updatesVisitedLocations() {
        HomeRange homeRange = new HomeRange(centerPos, 50.0);

        homeRange.recordVisit(centerPos);
        homeRange.recordVisit(edgePos);

        assertEquals(2, homeRange.getVisitedLocations().size(), "Should record both visits");
        assertTrue(homeRange.getVisitedLocations().contains(centerPos), "Should contain center");
        assertTrue(homeRange.getVisitedLocations().contains(edgePos), "Should contain edge position");
    }

    @Test
    void homeRange_recordVisit_increasesFrequency() {
        HomeRange homeRange = new HomeRange(centerPos, 50.0);

        homeRange.recordVisit(centerPos);
        homeRange.recordVisit(centerPos);
        homeRange.recordVisit(centerPos);

        assertEquals(1, homeRange.getVisitedLocations().size(), "Should have one unique location");
        assertEquals(centerPos, homeRange.getMostFamiliarArea(), "Center should be most familiar");
    }

    @Test
    void homeRange_getMostFamiliarArea_returnsCenterWhenEmpty() {
        HomeRange homeRange = new HomeRange(centerPos, 50.0);

        assertEquals(centerPos, homeRange.getMostFamiliarArea(), "Should return center when no visits");
    }

    @Test
    void homeRange_getMostFamiliarArea_returnsMostVisited() {
        HomeRange homeRange = new HomeRange(centerPos, 50.0);
        BlockPos pos1 = new BlockPos(10, 64, 10);
        BlockPos pos2 = new BlockPos(20, 64, 20);

        homeRange.recordVisit(pos1);
        homeRange.recordVisit(pos1);
        homeRange.recordVisit(pos2);

        assertEquals(pos1, homeRange.getMostFamiliarArea(), "Should return most visited position");
    }

    @Test
    void homeRange_getFamiliarityAt_unvisitedPosition() {
        HomeRange homeRange = new HomeRange(centerPos, 50.0);

        double familiarity = homeRange.getFamiliarityAt(new BlockPos(10, 64, 10));

        assertEquals(0.0, familiarity, 0.001, "Unvisited position should have 0 familiarity");
    }

    @Test
    void homeRange_getFamiliarityAt_visitedPosition() {
        HomeRange homeRange = new HomeRange(centerPos, 50.0);
        BlockPos pos1 = new BlockPos(10, 64, 10);

        homeRange.recordVisit(pos1);
        homeRange.recordVisit(pos1);

        double familiarity = homeRange.getFamiliarityAt(pos1);

        assertEquals(1.0, familiarity, 0.001, "Should have maximum familiarity for most visited");
    }

    @Test
    void homeRange_getFamiliarityAt_relativeFamiliarity() {
        HomeRange homeRange = new HomeRange(centerPos, 50.0);
        BlockPos pos1 = new BlockPos(10, 64, 10);
        BlockPos pos2 = new BlockPos(20, 64, 20);

        homeRange.recordVisit(pos1);
        homeRange.recordVisit(pos1);
        homeRange.recordVisit(pos2);

        double familiarity1 = homeRange.getFamiliarityAt(pos1);
        double familiarity2 = homeRange.getFamiliarityAt(pos2);

        assertTrue(familiarity1 > familiarity2, "More visited position should have higher familiarity");
        assertEquals(1.0, familiarity1, 0.001, "Most visited should have 1.0");
        assertEquals(0.5, familiarity2, 0.001, "Less visited should have relative familiarity");
    }

    @Test
    void homeRange_expandRange_increasesRadiusForOutsidePosition() {
        HomeRange homeRange = new HomeRange(centerPos, 50.0);
        double initialRadius = homeRange.getRadius();

        homeRange.expandRange(outsidePos);

        assertTrue(homeRange.getRadius() > initialRadius, "Radius should increase");
        assertTrue(homeRange.getRadius() < 60.0, "Should expand gradually (10% of excess)");
    }

    @Test
    void homeRange_expandRange_doesNotIncreaseForInsidePosition() {
        HomeRange homeRange = new HomeRange(centerPos, 50.0);
        double initialRadius = homeRange.getRadius();

        homeRange.expandRange(centerPos);

        assertEquals(initialRadius, homeRange.getRadius(), 0.001, "Radius should not increase");
    }

    @Test
    void homeRange_setCenter_updatesCenter() {
        HomeRange homeRange = new HomeRange(centerPos, 50.0);
        BlockPos newCenter = new BlockPos(100, 64, 100);

        homeRange.setCenter(newCenter);

        assertEquals(newCenter, homeRange.getCenter(), "Center should update");
    }

    @Test
    void homeRange_setRadius_updatesRadius() {
        HomeRange homeRange = new HomeRange(centerPos, 50.0);

        homeRange.setRadius(75.0);

        assertEquals(75.0, homeRange.getRadius(), 0.001, "Radius should update");
    }

    @Test
    void homeRange_setRadius_preventsNegativeRadius() {
        HomeRange homeRange = new HomeRange(centerPos, 50.0);

        homeRange.setRadius(-10.0);

        assertEquals(0.0, homeRange.getRadius(), 0.001, "Should clamp to 0");
    }

    @Test
    void homeRange_getVisitedLocations_returnsCopy() {
        HomeRange homeRange = new HomeRange(centerPos, 50.0);
        homeRange.recordVisit(centerPos);

        var visited = homeRange.getVisitedLocations();
        visited.add(new BlockPos(999, 64, 999));

        assertEquals(1, homeRange.getVisitedLocations().size(), "Should return defensive copy");
    }

    // ==================== Territory Tests ====================

    @Test
    void territory_initializesCorrectly() {
        double radius = 50.0;
        Territory territory = new Territory(ownerId, centerPos, radius);

        assertNotNull(territory, "Territory should initialize");
        assertEquals(ownerId, territory.getOwnerId(), "Owner ID should match");
        assertEquals(centerPos, territory.getCenter(), "Center should match");
        assertEquals(radius, territory.getRadius(), 0.001, "Radius should match");
        assertTrue(territory.getMarkers().isEmpty(), "Should start with no markers");
        assertTrue(territory.getEstablishedTime() > 0, "Should have establishment time");
    }

    @Test
    void territory_contains_positionAtCenter() {
        Territory territory = new Territory(ownerId, centerPos, 50.0);

        assertTrue(territory.contains(centerPos), "Should contain center position");
    }

    @Test
    void territory_contains_positionAtEdge() {
        Territory territory = new Territory(ownerId, centerPos, 50.0);

        assertTrue(territory.contains(edgePos), "Should contain edge position");
    }

    @Test
    void territory_contains_positionOutside() {
        Territory territory = new Territory(ownerId, centerPos, 50.0);

        assertFalse(territory.contains(outsidePos), "Should not contain outside position");
    }

    @Test
    void territory_isOwnedBy_correctOwner() {
        Territory territory = new Territory(ownerId, centerPos, 50.0);

        assertTrue(territory.isOwnedBy(ownerId), "Should identify correct owner");
    }

    @Test
    void territory_isOwnedBy_incorrectOwner() {
        Territory territory = new Territory(ownerId, centerPos, 50.0);

        assertFalse(territory.isOwnedBy(otherOwnerId), "Should not identify incorrect owner");
    }

    @Test
    void territory_addMarker_addsMarker() {
        Territory territory = new Territory(ownerId, centerPos, 50.0);
        BlockPos markerPos = new BlockPos(25, 64, 25);

        territory.addMarker(markerPos);

        assertEquals(1, territory.getMarkers().size(), "Should have one marker");
        assertTrue(territory.getMarkers().contains(markerPos), "Should contain marker");
    }

    @Test
    void territory_addMarker_preventsDuplicates() {
        Territory territory = new Territory(ownerId, centerPos, 50.0);
        BlockPos markerPos = new BlockPos(25, 64, 25);

        territory.addMarker(markerPos);
        territory.addMarker(markerPos);

        assertEquals(1, territory.getMarkers().size(), "Should not add duplicate marker");
    }

    @Test
    void territory_removeMarker_removesMarker() {
        Territory territory = new Territory(ownerId, centerPos, 50.0);
        BlockPos markerPos = new BlockPos(25, 64, 25);
        territory.addMarker(markerPos);

        territory.removeMarker(markerPos);

        assertTrue(territory.getMarkers().isEmpty(), "Should remove marker");
    }

    @Test
    void territory_overlaps_identicalTerritories() {
        Territory territory1 = new Territory(ownerId, centerPos, 50.0);
        Territory territory2 = new Territory(otherOwnerId, centerPos, 50.0);

        assertTrue(territory1.overlaps(territory2), "Identical territories should overlap");
    }

    @Test
    void territory_overlaps_adjacentTerritories() {
        Territory territory1 = new Territory(ownerId, centerPos, 50.0);
        Territory territory2 = new Territory(otherOwnerId, new BlockPos(100, 64, 0), 50.0);

        assertFalse(territory1.overlaps(territory2), "Adjacent territories touching at edges should not overlap");
    }

    @Test
    void territory_overlaps_distantTerritories() {
        Territory territory1 = new Territory(ownerId, centerPos, 50.0);
        Territory territory2 = new Territory(otherOwnerId, new BlockPos(200, 64, 0), 50.0);

        assertFalse(territory1.overlaps(territory2), "Distant territories should not overlap");
    }

    @Test
    void territory_getOverlapAmount_adjacentTerritories() {
        Territory territory1 = new Territory(ownerId, centerPos, 50.0);
        Territory territory2 = new Territory(otherOwnerId, new BlockPos(100, 64, 0), 50.0);

        double overlap = territory1.getOverlapAmount(territory2);

        assertEquals(0.0, overlap, 0.001, "Adjacent territories should have zero overlap");
    }

    @Test
    void territory_getOverlapAmount_overlappingTerritories() {
        Territory territory1 = new Territory(ownerId, centerPos, 50.0);
        Territory territory2 = new Territory(otherOwnerId, new BlockPos(75, 64, 0), 50.0);

        double overlap = territory1.getOverlapAmount(territory2);

        assertTrue(overlap > 0, "Should have positive overlap");
        assertEquals(25.0, overlap, 0.1, "Overlap should be approximately correct");
    }

    @Test
    void territory_getOverlapAmount_nonOverlappingTerritories() {
        Territory territory1 = new Territory(ownerId, centerPos, 50.0);
        Territory territory2 = new Territory(otherOwnerId, new BlockPos(200, 64, 0), 50.0);

        double overlap = territory1.getOverlapAmount(territory2);

        assertEquals(0.0, overlap, 0.001, "Should have zero overlap");
    }

    @Test
    void territory_getAge_increasesOverTime() {
        Territory territory = new Territory(ownerId, centerPos, 50.0);

        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            fail("Sleep interrupted");
        }

        assertTrue(territory.getAge() > 0, "Age should be positive");
        assertTrue(territory.getAge() >= 10, "Age should reflect elapsed time");
    }

    @Test
    void territory_distanceToCenter_calculatesCorrectly() {
        Territory territory = new Territory(ownerId, centerPos, 50.0);

        double distance = territory.distanceToCenter(outsidePos);

        assertEquals(60.0, distance, 0.1, "Should calculate correct distance");
    }

    @Test
    void territory_distanceToEdge_insideTerritory() {
        Territory territory = new Territory(ownerId, centerPos, 50.0);

        double distance = territory.distanceToEdge(centerPos);

        assertEquals(-50.0, distance, 0.1, "Should be negative when inside");
    }

    @Test
    void territory_distanceToEdge_atBoundary() {
        Territory territory = new Territory(ownerId, centerPos, 50.0);

        double distance = territory.distanceToEdge(edgePos);

        assertEquals(0.0, distance, 0.1, "Should be zero at boundary");
    }

    @Test
    void territory_distanceToEdge_outsideTerritory() {
        Territory territory = new Territory(ownerId, centerPos, 50.0);

        double distance = territory.distanceToEdge(outsidePos);

        assertEquals(10.0, distance, 0.1, "Should be positive when outside");
    }

    @Test
    void territory_getMarkers_returnsCopy() {
        Territory territory = new Territory(ownerId, centerPos, 50.0);
        BlockPos markerPos = new BlockPos(25, 64, 25);
        territory.addMarker(markerPos);

        var markers = territory.getMarkers();
        markers.add(new BlockPos(999, 64, 999));

        assertEquals(1, territory.getMarkers().size(), "Should return defensive copy");
    }

    // ==================== HomeRangeBehavior Tests ====================

    @Test
    void homeRangeBehavior_initializesCorrectly() {
        HomeRange homeRange = new HomeRange(centerPos, 50.0);
        HomeRangeBehavior behavior = new HomeRangeBehavior(homeRange, 1.0, true, 0.5);

        assertNotNull(behavior, "HomeRangeBehavior should initialize");
        assertEquals(homeRange, behavior.getHomeRange(), "HomeRange should match");
        assertEquals(1.0, behavior.getReturnStrength(), 0.001, "Return strength should match");
        assertTrue(behavior.isSoftBoundary(), "Should use soft boundary");
    }

    @Test
    void homeRangeBehavior_initializesWithDefaults() {
        HomeRange homeRange = new HomeRange(centerPos, 50.0);
        HomeRangeBehavior behavior = new HomeRangeBehavior(homeRange, 1.0, false);

        assertNotNull(behavior, "HomeRangeBehavior should initialize");
        assertFalse(behavior.isSoftBoundary(), "Should use hard boundary by default");
    }

    @Test
    void homeRangeBehavior_calculate_withinRange_returnsZeroForce() {
        HomeRange homeRange = new HomeRange(centerPos, 50.0);
        HomeRangeBehavior behavior = new HomeRangeBehavior(homeRange, 1.0, true);
        BehaviorContext context = createContext(centerPos);

        Vec3d force = behavior.calculate(context);

        assertEquals(0.0, force.x, 0.001, "Should have zero X force within range");
        assertEquals(0.0, force.y, 0.001, "Should have zero Y force within range");
        assertEquals(0.0, force.z, 0.001, "Should have zero Z force within range");
    }

    @Test
    void homeRangeBehavior_calculate_recordsVisit() {
        HomeRange homeRange = new HomeRange(centerPos, 50.0);
        HomeRangeBehavior behavior = new HomeRangeBehavior(homeRange, 1.0, true);
        BehaviorContext context = createContext(centerPos);

        behavior.calculate(context);

        assertTrue(homeRange.getVisitedLocations().contains(centerPos), "Should record visit");
    }

    @Test
    void homeRangeBehavior_softBoundary_outsideRange() {
        HomeRange homeRange = new HomeRange(centerPos, 50.0);
        HomeRangeBehavior behavior = new HomeRangeBehavior(homeRange, 0.5, true);
        BehaviorContext context = createContext(outsidePos);

        Vec3d force = behavior.calculate(context);

        assertTrue(force.magnitude() > 0, "Should have non-zero force outside range");
        assertTrue(force.x < 0, "Force should point toward center (negative X)");
    }

    @Test
    void homeRangeBehavior_softBoundary_forceProportionalToDistance() {
        HomeRange homeRange = new HomeRange(centerPos, 50.0);
        HomeRangeBehavior behavior = new HomeRangeBehavior(homeRange, 1.0, true);
        BehaviorContext contextNear = createContext(outsidePos);
        BehaviorContext contextFar = createContext(farOutsidePos);

        Vec3d forceNear = behavior.calculate(contextNear);
        Vec3d forceFar = behavior.calculate(contextFar);

        assertTrue(forceFar.magnitude() > forceNear.magnitude(), "Force should increase with distance");
    }

    @Test
    void homeRangeBehavior_hardBoundary_outsideRange() {
        HomeRange homeRange = new HomeRange(centerPos, 50.0);
        HomeRangeBehavior behavior = new HomeRangeBehavior(homeRange, 1.0, false);
        BehaviorContext context = createContext(outsidePos);

        Vec3d force = behavior.calculate(context);

        assertTrue(force.magnitude() > 0, "Should have non-zero force outside range");
        assertTrue(force.x < 0, "Force should point toward center (negative X)");
    }

    @Test
    void homeRangeBehavior_softVsHardBoundary_differentForces() {
        HomeRange homeRange1 = new HomeRange(centerPos, 50.0);
        HomeRange homeRange2 = new HomeRange(centerPos, 50.0);

        HomeRangeBehavior softBehavior = new HomeRangeBehavior(homeRange1, 1.0, true);
        HomeRangeBehavior hardBehavior = new HomeRangeBehavior(homeRange2, 1.0, false);

        BehaviorContext context = createContext(outsidePos);

        Vec3d softForce = softBehavior.calculate(context);
        Vec3d hardForce = hardBehavior.calculate(context);

        assertTrue(softForce.magnitude() != hardForce.magnitude(), "Soft and hard should produce different forces");
    }

    @Test
    void homeRangeBehavior_returnStrength_affectsForce() {
        HomeRange homeRange = new HomeRange(centerPos, 50.0);
        HomeRangeBehavior weakBehavior = new HomeRangeBehavior(homeRange, 0.1, true);
        HomeRangeBehavior strongBehavior = new HomeRangeBehavior(homeRange, 2.0, true);

        BehaviorContext context = createContext(outsidePos);

        Vec3d weakForce = weakBehavior.calculate(context);
        Vec3d strongForce = strongBehavior.calculate(context);

        assertTrue(strongForce.magnitude() > weakForce.magnitude(), "Stronger return strength should produce larger force");
    }

    @Test
    void homeRangeBehavior_directionTowardsCenter() {
        HomeRange homeRange = new HomeRange(centerPos, 50.0);
        HomeRangeBehavior behavior = new HomeRangeBehavior(homeRange, 1.0, true);
        BehaviorContext context = createContext(new BlockPos(60, 64, 0));

        Vec3d force = behavior.calculate(context);

        Vec3d toCenter = new Vec3d(centerPos.getX() - 60, centerPos.getY() - 64, centerPos.getZ() - 0);
        toCenter.normalize();

        double dotProduct = force.normalized().dot(toCenter);

        assertTrue(dotProduct > 0.9, "Force should point toward center");
    }

    // ==================== Scientific Behavior Tests ====================

    @Test
    void scientific_homeRangeExpansion_matchesBiologicalBehavior() {
        HomeRange homeRange = new HomeRange(centerPos, 50.0);
        double initialRadius = homeRange.getRadius();

        // Simulate animal exploring beyond current range
        BlockPos explorationPoint = new BlockPos(70, 64, 0);
        homeRange.expandRange(explorationPoint);

        double expansion = homeRange.getRadius() - initialRadius;
        double expectedExpansion = (70.0 - 50.0) * 0.1; // 10% of excess distance

        assertEquals(expectedExpansion, expansion, 0.01, "Should expand by 10% of excess distance");
    }

    @Test
    void scientific_territoryOverlap_matchesBiologicalTerritorialBehavior() {
        Territory territory1 = new Territory(ownerId, centerPos, 40.0);
        Territory territory2 = new Territory(otherOwnerId, new BlockPos(70, 64, 0), 40.0);

        boolean overlaps = territory1.overlaps(territory2);

        assertTrue(overlaps, "Territories should overlap at close distances");
    }

    @Test
    void scientific_familiarityTracking_matchesSpatialMemory() {
        HomeRange homeRange = new HomeRange(centerPos, 50.0);
        BlockPos frequentPos = new BlockPos(10, 64, 10);
        BlockPos rarePos = new BlockPos(20, 64, 20);

        // Simulate repeated visits to one location
        for (int i = 0; i < 10; i++) {
            homeRange.recordVisit(frequentPos);
        }
        homeRange.recordVisit(rarePos);

        double frequentFamiliarity = homeRange.getFamiliarityAt(frequentPos);
        double rareFamiliarity = homeRange.getFamiliarityAt(rarePos);

        assertTrue(frequentFamiliarity > rareFamiliarity, "Frequently visited areas should have higher familiarity");
        assertEquals(1.0, frequentFamiliarity, 0.001, "Most visited should have max familiarity");
        assertEquals(0.1, rareFamiliarity, 0.001, "Rarely visited should have proportional familiarity");
    }

    @Test
    void scientific_territorialExclusion_matchesNaturalBehavior() {
        Territory alphaTerritory = new Territory(ownerId, centerPos, 60.0);
        Territory betaTerritory = new Territory(otherOwnerId, new BlockPos(150, 64, 0), 60.0);

        boolean overlaps = alphaTerritory.overlaps(betaTerritory);

        assertFalse(overlaps, "Separate territories should not overlap when sufficiently distant");
    }

    @Test
    void scientific_homeReturnBehavior_matchesHomingInstinct() {
        HomeRange homeRange = new HomeRange(centerPos, 50.0);
        HomeRangeBehavior behavior = new HomeRangeBehavior(homeRange, 1.5, true);

        BehaviorContext contextFar = createContext(new BlockPos(100, 64, 0));
        BehaviorContext contextNear = createContext(new BlockPos(60, 64, 0));

        Vec3d forceFar = behavior.calculate(contextFar);
        Vec3d forceNear = behavior.calculate(contextNear);

        assertTrue(forceFar.magnitude() > forceNear.magnitude(), "Return force should increase with distance from home");
    }

    // ==================== Edge Case Tests ====================

    @Test
    void edgeCase_homeRange_zeroRadius() {
        HomeRange homeRange = new HomeRange(centerPos, 0.0);

        assertTrue(homeRange.isWithinRange(centerPos), "Only center should be in zero-radius range");
        assertFalse(homeRange.isWithinRange(edgePos), "Any other position should be outside");
    }

    @Test
    void edgeCase_territory_zeroRadius() {
        Territory territory = new Territory(ownerId, centerPos, 0.0);

        assertTrue(territory.contains(centerPos), "Only center should be in zero-radius territory");
        assertFalse(territory.contains(new BlockPos(1, 64, 0)), "Adjacent position should be outside");
    }

    @Test
    void edgeCase_homeRangeBehavior_atExactBoundary() {
        HomeRange homeRange = new HomeRange(centerPos, 50.0);
        HomeRangeBehavior behavior = new HomeRangeBehavior(homeRange, 1.0, true);

        // Position at exactly 50.0 distance
        BlockPos boundaryPos = new BlockPos(50, 64, 0);
        BehaviorContext context = createContext(boundaryPos);

        Vec3d force = behavior.calculate(context);

        assertEquals(0.0, force.magnitude(), 0.001, "Should have zero force at exact boundary");
    }

    @Test
    void edgeCase_territoryOverlap_identicalCenters() {
        Territory territory1 = new Territory(ownerId, centerPos, 50.0);
        Territory territory2 = new Territory(otherOwnerId, centerPos, 50.0);

        assertTrue(territory1.overlaps(territory2), "Territories with same center should overlap");
        assertEquals(100.0, territory1.getOverlapAmount(territory2), 0.001, "Overlap amount should equal combined radius");
    }

    @Test
    void edgeCase_homeRange_expansionFromZero() {
        HomeRange homeRange = new HomeRange(centerPos, 0.0);

        homeRange.expandRange(new BlockPos(10, 64, 0));

        assertTrue(homeRange.getRadius() > 0, "Should expand from zero radius");
        assertEquals(1.0, homeRange.getRadius(), 0.1, "Should expand by 10% of distance");
    }

    @Test
    void edgeCase_behaviorContext_nullEntity() {
        HomeRange homeRange = new HomeRange(centerPos, 50.0);
        HomeRangeBehavior behavior = new HomeRangeBehavior(homeRange, 1.0, true);

        BehaviorContext context = new BehaviorContext(new Vec3d(60, 64, 0), new Vec3d(0, 0, 0), 1.0, 0.1);

        Vec3d force = behavior.calculate(context);

        assertNotNull(force, "Should handle null entity gracefully");
        assertTrue(force.magnitude() > 0, "Should still produce force outside range");
    }

    // ==================== Helper Methods ====================

    /**
     * Creates a BehaviorContext for testing without requiring actual Minecraft entities.
     */
    private BehaviorContext createContext(BlockPos position) {
        Vec3d posVec = new Vec3d(position.getX() + 0.5, position.getY(), position.getZ() + 0.5);
        Vec3d velocity = new Vec3d(0, 0, 0);
        return new BehaviorContext(posVec, velocity, 1.0, 0.1);
    }
}
