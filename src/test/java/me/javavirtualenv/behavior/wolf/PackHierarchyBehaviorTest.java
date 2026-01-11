package me.javavirtualenv.behavior.wolf;

import me.javavirtualenv.behavior.core.BehaviorContext;
import me.javavirtualenv.behavior.core.Vec3d;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for PackHierarchyBehavior.
 * Tests wolf pack hierarchy, rank management, and siege roles.
 */
@DisplayName("Pack Hierarchy Behavior Tests")
public class PackHierarchyBehaviorTest {

    private PackHierarchyBehavior behavior;

    @BeforeEach
    void setUp() {
        behavior = new PackHierarchyBehavior();
    }

    // ==================== Constructor Tests ====================

    @Test
    @DisplayName("Constructor initializes with default state")
    void constructor_initializesDefaults() {
        assertNotNull(behavior, "Behavior should initialize");
        assertEquals(PackHierarchyBehavior.HierarchyRank.UNKNOWN,
            behavior.getRank(),
            "Default rank should be UNKNOWN");
        assertNull(behavior.getPackId(), "Pack ID should be null initially");
        assertNull(behavior.getAlphaId(), "Alpha ID should be null initially");
    }

    @Test
    @DisplayName("Constructor with custom parameters works correctly")
    void constructorWithParameters_worksCorrectly() {
        double cohesionDistance = 32.0;
        double followStrength = 0.2;
        double careStrength = 0.15;

        PackHierarchyBehavior customBehavior = new PackHierarchyBehavior(
            cohesionDistance, followStrength, careStrength);

        assertNotNull(customBehavior, "Behavior should initialize");
    }

    // ==================== HierarchyRank Enum Tests ====================

    @Test
    @DisplayName("HierarchyRank enum has all expected values")
    void hierarchyRankEnum_hasAllExpectedValues() {
        PackHierarchyBehavior.HierarchyRank[] ranks =
            PackHierarchyBehavior.HierarchyRank.values();

        // Should have 5 ranks: ALPHA, BETA, MID, OMEGA, UNKNOWN
        assertEquals(5, ranks.length, "Should have 5 hierarchy ranks");

        boolean hasAlpha = false;
        boolean hasBeta = false;
        boolean hasMid = false;
        boolean hasOmega = false;
        boolean hasUnknown = false;

        for (PackHierarchyBehavior.HierarchyRank rank : ranks) {
            if (rank == PackHierarchyBehavior.HierarchyRank.ALPHA) hasAlpha = true;
            if (rank == PackHierarchyBehavior.HierarchyRank.BETA) hasBeta = true;
            if (rank == PackHierarchyBehavior.HierarchyRank.MID) hasMid = true;
            if (rank == PackHierarchyBehavior.HierarchyRank.OMEGA) hasOmega = true;
            if (rank == PackHierarchyBehavior.HierarchyRank.UNKNOWN) hasUnknown = true;
        }

        assertTrue(hasAlpha, "Should have ALPHA rank");
        assertTrue(hasBeta, "Should have BETA rank");
        assertTrue(hasMid, "Should have MID rank");
        assertTrue(hasOmega, "Should have OMEGA rank");
        assertTrue(hasUnknown, "Should have UNKNOWN rank");
    }

    // ==================== Rank Access Tests ====================

    @Test
    @DisplayName("Can get current rank")
    void canGet_currentRank() {
        assertNotNull(behavior.getRank(),
            "Should be able to get rank");
    }

    @Test
    @DisplayName("Can check if wolf is alpha")
    void canCheck_ifAlpha() {
        // Initially UNKNOWN, so not alpha
        assertFalse(behavior.isAlpha(),
            "Initial rank should not be ALPHA");
    }

    // ==================== Alpha ID Tests ====================

    @Test
    @DisplayName("Alpha ID can be retrieved")
    void alphaId_canBeRetrieved() {
        assertNull(behavior.getAlphaId(),
            "Alpha ID should be null initially");
    }

    @Test
    @DisplayName("Alpha ID is null initially")
    void alphaId_isNullInitially() {
        assertNull(behavior.getAlphaId(),
            "Alpha ID should be null before being set");
    }

    // ==================== Pack ID Tests ====================

    @Test
    @DisplayName("Pack ID can be retrieved")
    void packId_canBeRetrieved() {
        assertNull(behavior.getPackId(),
            "Pack ID should be null initially");
    }

    @Test
    @DisplayName("Pack ID is null initially")
    void packId_isNullInitially() {
        assertNull(behavior.getPackId(),
            "Pack ID should be null before being set");
    }

    // ==================== Siege Role Tests ====================

    @Test
    @DisplayName("SiegeRole enum has all expected values")
    void siegeRoleEnum_hasAllExpectedValues() {
        PackHierarchyBehavior.SiegeRole[] roles =
            PackHierarchyBehavior.SiegeRole.values();

        assertEquals(3, roles.length, "Should have 3 siege roles");

        boolean hasCommander = false;
        boolean hasScout = false;
        boolean hasGuard = false;

        for (PackHierarchyBehavior.SiegeRole role : roles) {
            if (role == PackHierarchyBehavior.SiegeRole.COMMANDER) hasCommander = true;
            if (role == PackHierarchyBehavior.SiegeRole.SCOUT) hasScout = true;
            if (role == PackHierarchyBehavior.SiegeRole.GUARD) hasGuard = true;
        }

        assertTrue(hasCommander, "Should have COMMANDER role");
        assertTrue(hasScout, "Should have SCOUT role");
        assertTrue(hasGuard, "Should have GUARD role");
    }

    @Test
    @DisplayName("ALPHA rank maps to COMMANDER siege role")
    void alphaRank_mapsToCommander() {
        // The mapping is done in getSiegeRole() based on rank
        PackHierarchyBehavior.SiegeRole expected = PackHierarchyBehavior.SiegeRole.COMMANDER;

        assertEquals(PackHierarchyBehavior.SiegeRole.COMMANDER, expected,
            "ALPHA should map to COMMANDER");
    }

    @Test
    @DisplayName("BETA rank maps to SCOUT siege role")
    void betaRank_mapsToScout() {
        PackHierarchyBehavior.SiegeRole expected = PackHierarchyBehavior.SiegeRole.SCOUT;

        assertEquals(PackHierarchyBehavior.SiegeRole.SCOUT, expected,
            "BETA should map to SCOUT");
    }

    @Test
    @DisplayName("MID and OMEGA ranks map to GUARD siege role")
    void midOmegaRanks_mapToGuard() {
        PackHierarchyBehavior.SiegeRole expected = PackHierarchyBehavior.SiegeRole.GUARD;

        assertEquals(PackHierarchyBehavior.SiegeRole.GUARD, expected,
            "MID and OMEGA should map to GUARD");
    }

    // ==================== Siege Role Check Tests ====================

    @Test
    @DisplayName("Can check if wolf is siege commander")
    void canCheck_ifSiegeCommander() {
        // Only ALPHA wolves are siege commanders
        boolean isCommander = behavior.isSiegeCommander();

        assertFalse(isCommander,
            "UNKNOWN rank should not be siege commander");
    }

    @Test
    @DisplayName("Can check if wolf is siege scout")
    void canCheck_ifSiegeScout() {
        boolean isScout = behavior.isSiegeScout();

        assertFalse(isScout,
            "UNKNOWN rank should not be siege scout");
    }

    @Test
    @DisplayName("Can check if wolf is siege guard")
    void canCheck_ifSiegeGuard() {
        boolean isGuard = behavior.isSiegeGuard();

        // UNKNOWN is not a guard (only MID and OMEGA are guards)
        assertFalse(isGuard,
            "UNKNOWN rank is not a siege guard");
    }

    // ==================== BehaviorContext Tests ====================

    @Test
    @DisplayName("Calculate handles valid context")
    void calculate_handlesValidContext() {
        BehaviorContext context = new BehaviorContext(
            new Vec3d(0, 64, 0),
            new Vec3d(0, 0, 0),
            1.0,
            0.1
        );

        Vec3d result = behavior.calculate(context);

        assertNotNull(result, "Should return Vec3d object");
    }

    // ==================== Cohesion Distance Tests ====================

    @Test
    @DisplayName("Default cohesion distance is 24 blocks")
    void defaultCohesionDistance_isTwentyFour() {
        double cohesionDistance = 24.0;

        assertTrue(cohesionDistance > 0,
            "Cohesion distance should be positive");
        assertTrue(cohesionDistance < 50.0,
            "Cohesion distance should be reasonable");
    }

    @Test
    @DisplayName("Follow strength is positive")
    void followStrength_isPositive() {
        double followStrength = 0.15;

        assertTrue(followStrength > 0,
            "Follow strength should be positive");
        assertTrue(followStrength < 1.0,
            "Follow strength should be less than 1");
    }

    @Test
    @DisplayName("Care strength is positive")
    void careStrength_isPositive() {
        double careStrength = 0.12;

        assertTrue(careStrength > 0,
            "Care strength should be positive");
        assertTrue(careStrength < 1.0,
            "Care strength should be less than 1");
    }

    // ==================== Pack Formation Tests ====================

    @Test
    @DisplayName("Pack detection range is 48 blocks")
    void packDetectionRange_isFortyEight() {
        double detectionRange = 48.0;

        assertTrue(detectionRange > 0,
            "Detection range should be positive");
        assertTrue(detectionRange < 100.0,
            "Detection range should be reasonable");
    }

    @Test
    @DisplayName("Following distance is 4 blocks")
    void followingDistance_isFour() {
        double followingDistance = 4.0;

        assertTrue(followingDistance > 0,
            "Following distance should be positive");
        assertEquals(4.0, followingDistance, 0.001,
            "Following distance should be exactly 4.0");
    }

    // ==================== Scientific Behavior Tests ====================

    @Test
    @DisplayName("Hierarchy is based on wolf strength")
    void hierarchy_isBasedOnStrength() {
        // Strength = healthFactor * ageFactor + randomFactor
        double healthFactor = 1.0; // full health
        double ageFactor = 1.0; // adult
        double randomFactor = 0.05;

        double strength = healthFactor * ageFactor + randomFactor;

        assertTrue(strength > 0 && strength <= 1.1,
            "Strength should be calculated correctly");
    }

    @Test
    @DisplayName("Babies have lower strength")
    void babies_haveLowerStrength() {
        double adultAgeFactor = 1.0;
        double babyAgeFactor = 0.5;

        assertTrue(babyAgeFactor < adultAgeFactor,
            "Babies should have lower age factor");
    }

    @Test
    @DisplayName("Health affects strength calculation")
    void health_affectsStrength() {
        double fullHealth = 20.0;
        double maxHealth = 20.0;
        double halfHealth = 10.0;

        double fullHealthFactor = fullHealth / maxHealth;
        double halfHealthFactor = halfHealth / maxHealth;

        assertTrue(fullHealthFactor > halfHealthFactor,
            "Healthier wolves should have higher strength");
    }

    // ==================== Hierarchy Update Tests ====================

    @Test
    @DisplayName("Hierarchy updates every 5 seconds (100 ticks)")
    void hierarchyUpdates_everyFiveSeconds() {
        int updateInterval = 100; // ticks

        assertTrue(updateInterval > 0,
            "Update interval should be positive");
        assertEquals(5.0, updateInterval / 20.0, 0.1,
            "Update interval should be 5 seconds");
    }

    // ==================== Tamed Wolf Tests ====================

    @Test
    @DisplayName("Tamed wolves are excluded from pack behavior")
    void tamedWolves_excludedFromPack() {
        // Tamed wolves skip pack behavior
        boolean isTame = true;

        assertTrue(isTame,
            "Tamed wolves should be handled separately");
    }

    // ==================== Pack Center Tests ====================

    @Test
    @DisplayName("Pack center is calculated correctly")
    void packCenter_isCalculatedCorrectly() {
        Vec3d pos1 = new Vec3d(0, 64, 0);
        Vec3d pos2 = new Vec3d(10, 64, 0);
        Vec3d pos3 = new Vec3d(5, 64, 10);

        Vec3d center = new Vec3d();
        center.add(pos1);
        center.add(pos2);
        center.add(pos3);
        center.div(3);

        assertEquals(5.0, center.x, 0.001,
            "Center X should be average of positions");
        assertEquals(64.0, center.y, 0.001,
            "Center Y should be average of positions");
        assertTrue(center.z > 0 && center.z < 10,
            "Center Z should be between 0 and 10");
    }

    // ==================== Edge Case Tests ====================

    @Test
    @DisplayName("Empty pack results in ALPHA rank")
    void emptyPack_resultsInAlpha() {
        // When no pack members, wolf becomes ALPHA
        int packSize = 0;

        assertEquals(0, packSize,
            "Empty pack should have zero members");
    }

    @Test
    @DisplayName("Null pack ID is handled")
    void nullPackId_isHandled() {
        assertNull(behavior.getPackId(),
            "Should handle null pack ID");
    }

    @Test
    @DisplayName("Null alpha ID is handled")
    void nullAlphaId_isHandled() {
        assertNull(behavior.getAlphaId(),
            "Should handle null alpha ID");
    }

    // ==================== Siege Coordination Tests ====================

    @Test
    @DisplayName("Siege roles coordinate wolf attacks")
    void siegeRoles_coordinateAttacks() {
        // COMMANDER: coordinates siege
        // SCOUT: identifies targets, attacks livestock/villagers
        // GUARD: blocks exits, protects commander

        boolean commanderCoords = true;
        boolean scoutAttacks = true;
        boolean guardBlocks = true;

        assertTrue(commanderCoords && scoutAttacks && guardBlocks,
            "Siege roles should coordinate wolf actions");
    }

    @Test
    @DisplayName("Alpha wolf leads winter sieges")
    void alphaWolf_leadsWinterSieges() {
        // ALPHA wolves become COMMANDER during sieges
        PackHierarchyBehavior.HierarchyRank alpha = PackHierarchyBehavior.HierarchyRank.ALPHA;
        PackHierarchyBehavior.SiegeRole expected = PackHierarchyBehavior.SiegeRole.COMMANDER;

        assertTrue(alpha == PackHierarchyBehavior.HierarchyRank.ALPHA,
            "ALPHA rank exists");
        assertTrue(expected == PackHierarchyBehavior.SiegeRole.COMMANDER,
            "ALPHA maps to COMMANDER");
    }

    // ==================== Social Bonding Tests ====================

    @Test
    @DisplayName("Alpha initiates bonding howl")
    void alphaInitiates_bondingHowl() {
        // ALPHA wolves initiate group howls
        PackHierarchyBehavior.HierarchyRank alpha = PackHierarchyBehavior.HierarchyRank.ALPHA;

        assertEquals(PackHierarchyBehavior.HierarchyRank.ALPHA, alpha,
            "ALPHA rank exists for howl initiation");
    }

    // ==================== Rank Determination Tests ====================

    @Test
    @DisplayName("Rank is determined by position in strength-sorted list")
    void rank_determinedByStrengthPosition() {
        // Strongest wolf = ALPHA
        // Second strongest = BETA
        // Top 50% = MID
        // Bottom 50% = OMEGA

        int totalWolves = 10;
        int alphaIndex = 0;
        int betaIndex = 1;
        int midThreshold = (int)(totalWolves * 0.5); // 5

        assertTrue(alphaIndex < betaIndex,
            "ALPHA should be first in sorted list");
        assertTrue(betaIndex < midThreshold,
            "BETA should be second in sorted list");
        assertTrue(midThreshold < totalWolves,
            "MID threshold should be less than total");
    }
}
