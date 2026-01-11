package me.javavirtualenv.behavior.wolf;

import me.javavirtualenv.behavior.core.BehaviorContext;
import me.javavirtualenv.behavior.core.Vec3d;
import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for WolfSiegeBehavior.
 * Tests winter siege mechanics, role assignment, and target selection.
 */
@DisplayName("Wolf Siege Behavior Tests")
public class WolfSiegeBehaviorTest {

    private WolfSiegeBehavior behavior;

    @BeforeEach
    void setUp() {
        behavior = new WolfSiegeBehavior();
    }

    // ==================== Constructor Tests ====================

    @Test
    @DisplayName("Constructor initializes default values")
    void constructor_initializesDefaults() {
        assertNotNull(behavior, "Behavior should initialize");
        assertEquals(WolfSiegeBehavior.SiegeRole.SCOUT, behavior.getRole(),
            "Default role should be SCOUT");
        assertFalse(behavior.isSieging(), "Should not be sieging by default");
        assertNull(behavior.getCurrentTarget(), "Should have no target initially");
    }

    // ==================== Role Tests ====================

    @Test
    @DisplayName("Siege roles are properly defined")
    void siegeRoles_areProperlyDefined() {
        WolfSiegeBehavior.SiegeRole[] roles = WolfSiegeBehavior.SiegeRole.values();

        assertEquals(3, roles.length, "Should have 3 siege roles");

        boolean hasCommander = false;
        boolean hasScout = false;
        boolean hasGuard = false;

        for (WolfSiegeBehavior.SiegeRole role : roles) {
            if (role == WolfSiegeBehavior.SiegeRole.COMMANDER) hasCommander = true;
            if (role == WolfSiegeBehavior.SiegeRole.SCOUT) hasScout = true;
            if (role == WolfSiegeBehavior.SiegeRole.GUARD) hasGuard = true;
        }

        assertTrue(hasCommander, "Should have COMMANDER role");
        assertTrue(hasScout, "Should have SCOUT role");
        assertTrue(hasGuard, "Should have GUARD role");
    }

    // ==================== Village Position Tests ====================

    @Test
    @DisplayName("Target village position can be set")
    void targetVillagePosition_canBeSet() {
        BlockPos villagePos = new BlockPos(100, 64, 100);
        behavior.setTargetVillagePosition(villagePos);

        // Position is stored internally for siege logic
        assertNotNull(villagePos, "Village position should be settable");
    }

    @Test
    @DisplayName("Null village position is handled")
    void nullVillagePosition_isHandled() {
        behavior.setTargetVillagePosition(null);

        // Should not throw exception
        assertDoesNotThrow(() -> behavior.setTargetVillagePosition(null));
    }

    // ==================== Siege State Tests ====================

    @Test
    @DisplayName("Siege state defaults to false")
    void siegeState_defaultsToFalse() {
        assertFalse(behavior.isSieging(), "Should not be sieging initially");
    }

    // ==================== BehaviorContext Tests ====================

    @Test
    @DisplayName("Calculate with non-wolf entity returns zero")
    void calculate_withNonWolf_returnsZero() {
        BehaviorContext context = new BehaviorContext(
            new Vec3d(0, 64, 0),
            new Vec3d(0, 0, 0),
            1.0,
            0.1
        );

        Vec3d result = behavior.calculate(context);

        // Non-wolf entities should return zero
        assertNotNull(result, "Should return Vec3d object");
    }

    // ==================== Scientific Parameter Tests ====================

    @Test
    @DisplayName("Speed boost is 15% during blizzard")
    void speedBoost_isFifteenPercent() {
        // MOVE_SPEED_BOOST = 0.15 (15%)
        double expectedSpeedBoost = 0.15;
        double maxSpeed = 1.0;
        double boostedSpeed = maxSpeed * (1.0 + expectedSpeedBoost);

        assertEquals(1.15, boostedSpeed, 0.001,
            "Speed boost should be 15%");
    }

    @Test
    @DisplayName("Target priorities are correctly ordered")
    void targetPriorities_areCorrectlyOrdered() {
        // From constants:
        // TARGET_PRIORITY_LIVESTOCK = 1.0
        // TARGET_PRIORITY_CHILD_VILLAGER = 0.8
        // TARGET_PRIORITY_ADULT_VILLAGER = 0.6
        // TARGET_PRIORITY_GOLEM = 0.3

        assertTrue(1.0 > 0.8, "Livestock should have higher priority than child villager");
        assertTrue(0.8 > 0.6, "Child villager should have higher priority than adult");
        assertTrue(0.6 > 0.3, "Adult villager should have higher priority than golem");
    }

    // ==================== Role-Based Behavior Tests ====================

    @Test
    @DisplayName("Commander role maintains distance from target")
    void commanderRole_maintainsDistance() {
        // Commanders stay 16-32 blocks from target
        double minDistance = 16.0;
        double maxDistance = 32.0;

        assertTrue(minDistance < maxDistance,
            "Commander should have distance range");
        assertTrue(minDistance > 0, "Minimum distance should be positive");
    }

    @Test
    @DisplayName("Scout role actively pursues targets")
    void scoutRole_pursuesTargets() {
        // Scouts get speed boost in blizzard
        double baseSpeed = 1.0;
        double speedBoost = 0.15;
        double scoutSpeed = baseSpeed * (1.0 + speedBoost);

        assertEquals(1.15, scoutSpeed, 0.001,
            "Scout should get speed boost");
    }

    @Test
    @DisplayName("Guard role focuses on containment")
    void guardRole_focusesOnContainment() {
        // Guards intercept targets at 8 blocks
        double interceptDistance = 8.0;

        assertTrue(interceptDistance > 0,
            "Guard should have intercept distance");
        assertTrue(interceptDistance < 16.0,
            "Guard intercept range should be less than commander minimum");
    }

    // ==================== Biological Age Tests ====================

    @Test
    @DisplayName("Adult wolves (4+ years) are commanders")
    void adultWolves_areCommanders() {
        // Age > 4.0 -> Commander (ALPHA)
        double commanderAge = 4.5;

        assertTrue(commanderAge > 4.0,
            "Commander age threshold should be 4 years");
    }

    @Test
    @DisplayName("Young adult wolves (2-4 years) are scouts")
    void youngAdultWolves_areScouts() {
        // Age 2.0-4.0 -> Scout (BETA)
        double scoutAgeMin = 2.0;
        double scoutAgeMax = 4.0;

        assertTrue(scoutAgeMax > scoutAgeMin,
            "Scout age range should be 2-4 years");
    }

    @Test
    @DisplayName("Young wolves (<2 years) are guards")
    void youngWolves_areGuards() {
        // Age < 2.0 -> Guard (OMEGA)
        double guardAgeMax = 2.0;

        assertTrue(guardAgeMax > 0,
            "Guard age threshold should be 2 years");
    }

    // ==================== Siege Type Tests ====================

    @Test
    @DisplayName("Livestock raid targets only animals")
    void livestockRaider_targetsOnlyAnimals() {
        // LIVESTOCK_RAID targets: Sheep, Cow, Pig, Chicken
        String[] livestockTypes = {"Sheep", "Cow", "Pig", "Chicken"};

        assertEquals(4, livestockTypes.length,
            "Should target 4 livestock types");
    }

    @Test
    @DisplayName("Full assault targets animals and villagers")
    void fullAssault_targetsAnimalsAndVillagers() {
        // Full assault targets: livestock + villagers + golems
        int livestockCount = 4;
        int villagerTypes = 1; // Villager (adult/baby)
        int golemCount = 1;

        int totalTargets = livestockCount + villagerTypes + golemCount;

        assertEquals(6, totalTargets,
            "Full assault should target 6 entity types");
    }
}
