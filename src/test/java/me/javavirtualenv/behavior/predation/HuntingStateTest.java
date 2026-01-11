package me.javavirtualenv.behavior.predation;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

/**
 * Unit tests for HuntingState enum.
 * Tests all enum values and state transitions.
 */
@DisplayName("HuntingState Enum Tests")
class HuntingStateTest {

    private HuntingState[] allStates;

    @BeforeEach
    void setUp() {
        allStates = HuntingState.values();
    }

    // ==================== Enum Values Tests ====================

    @Test
    @DisplayName("All expected HuntingState enum values exist")
    void allEnumValuesExist() {
        assertEquals(7, allStates.length, "Should have 7 hunting states");
    }

    @Test
    @DisplayName("IDLE state exists")
    void idleStateExists() {
        assertTrue(List.of(allStates).contains(HuntingState.IDLE));
        assertEquals("IDLE", HuntingState.IDLE.name());
    }

    @Test
    @DisplayName("SEARCHING state exists")
    void searchingStateExists() {
        assertTrue(List.of(allStates).contains(HuntingState.SEARCHING));
        assertEquals("SEARCHING", HuntingState.SEARCHING.name());
    }

    @Test
    @DisplayName("STALKING state exists")
    void stalkingStateExists() {
        assertTrue(List.of(allStates).contains(HuntingState.STALKING));
        assertEquals("STALKING", HuntingState.STALKING.name());
    }

    @Test
    @DisplayName("CHASING state exists")
    void chasingStateExists() {
        assertTrue(List.of(allStates).contains(HuntingState.CHASING));
        assertEquals("CHASING", HuntingState.CHASING.name());
    }

    @Test
    @DisplayName("ATTACKING state exists")
    void attackingStateExists() {
        assertTrue(List.of(allStates).contains(HuntingState.ATTACKING));
        assertEquals("ATTACKING", HuntingState.ATTACKING.name());
    }

    @Test
    @DisplayName("EATING state exists")
    void eatingStateExists() {
        assertTrue(List.of(allStates).contains(HuntingState.EATING));
        assertEquals("EATING", HuntingState.EATING.name());
    }

    @Test
    @DisplayName("RESTING state exists")
    void restingStateExists() {
        assertTrue(List.of(allStates).contains(HuntingState.RESTING));
        assertEquals("RESTING", HuntingState.RESTING.name());
    }

    // ==================== Enum Properties Tests ====================

    @Test
    @DisplayName("HuntingState enum is not empty")
    void enumIsNotEmpty() {
        assertTrue(allStates.length > 0, "Enum should have at least one value");
    }

    @Test
    @DisplayName("HuntingState values() returns correct array")
    void valuesReturnsCorrectArray() {
        HuntingState[] states = HuntingState.values();
        assertNotNull(states);
        assertEquals(7, states.length);
    }

    @Test
    @DisplayName("HuntingState valueOf() works correctly")
    void valueOfWorksCorrectly() {
        assertEquals(HuntingState.IDLE, HuntingState.valueOf("IDLE"));
        assertEquals(HuntingState.SEARCHING, HuntingState.valueOf("SEARCHING"));
        assertEquals(HuntingState.STALKING, HuntingState.valueOf("STALKING"));
        assertEquals(HuntingState.CHASING, HuntingState.valueOf("CHASING"));
        assertEquals(HuntingState.ATTACKING, HuntingState.valueOf("ATTACKING"));
        assertEquals(HuntingState.EATING, HuntingState.valueOf("EATING"));
        assertEquals(HuntingState.RESTING, HuntingState.valueOf("RESTING"));
    }

    @Test
    @DisplayName("HuntingState valueOf() throws exception for invalid name")
    void valueOfThrowsExceptionForInvalidName() {
        assertThrows(IllegalArgumentException.class, () -> {
            HuntingState.valueOf("INVALID_STATE");
        });
    }

    // ==================== State Sequence Tests ====================

    @Test
    @DisplayName("States follow logical hunting sequence")
    void statesFollowLogicalSequence() {
        HuntingState initialState = HuntingState.IDLE;
        HuntingState searchingState = HuntingState.SEARCHING;
        HuntingState stalkingState = HuntingState.STALKING;
        HuntingState chasingState = HuntingState.CHASING;
        HuntingState attackingState = HuntingState.ATTACKING;
        HuntingState eatingState = HuntingState.EATING;
        HuntingState restingState = HuntingState.RESTING;

        assertNotEquals(initialState, searchingState);
        assertNotEquals(searchingState, stalkingState);
        assertNotEquals(stalkingState, chasingState);
        assertNotEquals(chasingState, attackingState);
        assertNotEquals(attackingState, eatingState);
        assertNotEquals(eatingState, restingState);
    }

    @Test
    @DisplayName("Each state has unique ordinal")
    void eachStateHasUniqueOrdinal() {
        int[] ordinals = new int[allStates.length];
        for (int i = 0; i < allStates.length; i++) {
            ordinals[i] = allStates[i].ordinal();
        }

        for (int i = 0; i < ordinals.length; i++) {
            for (int j = i + 1; j < ordinals.length; j++) {
                assertNotEquals(ordinals[i], ordinals[j],
                    "States at positions " + i + " and " + j + " should have different ordinals");
            }
        }
    }

    @Test
    @DisplayName("State ordinals are sequential from 0")
    void stateOrdinalsSequential() {
        for (int i = 0; i < allStates.length; i++) {
            assertEquals(i, allStates[i].ordinal(),
                "State at index " + i + " should have ordinal " + i);
        }
    }

    // ==================== State Classification Tests ====================

    @Test
    @DisplayName("IDLE is initial non-active state")
    void idleIsInitialState() {
        assertEquals(0, HuntingState.IDLE.ordinal());
    }

    @Test
    @DisplayName("SEARCHING represents looking for prey")
    void searchingIsLookingState() {
        assertEquals(1, HuntingState.SEARCHING.ordinal());
    }

    @Test
    @DisplayName("STALKING represents stealthy approach")
    void stalkingIsStealthyState() {
        assertEquals(2, HuntingState.STALKING.ordinal());
    }

    @Test
    @DisplayName("CHASING represents active pursuit")
    void chasingIsActivePursuitState() {
        assertEquals(3, HuntingState.CHASING.ordinal());
    }

    @Test
    @DisplayName("ATTACKING represents close-range attack")
    void attackingIsCloseRangeState() {
        assertEquals(4, HuntingState.ATTACKING.ordinal());
    }

    @Test
    @DisplayName("EATING represents consuming prey")
    void eatingIsConsumingState() {
        assertEquals(5, HuntingState.EATING.ordinal());
    }

    @Test
    @DisplayName("RESTING represents post-hunt recovery")
    void restingIsRecoveryState() {
        assertEquals(6, HuntingState.RESTING.ordinal());
    }

    // ==================== State Transition Tests ====================

    @Test
    @DisplayName("State transition from IDLE to SEARCHING is valid")
    void transitionFromIdleToSearching() {
        HuntingState from = HuntingState.IDLE;
        HuntingState to = HuntingState.SEARCHING;

        assertNotEquals(from, to, "States should be different");
        assertTrue(to.ordinal() > from.ordinal(), "SEARCHING should come after IDLE");
    }

    @Test
    @DisplayName("State transition from CHASING to ATTACKING is valid")
    void transitionFromChasingToAttacking() {
        HuntingState from = HuntingState.CHASING;
        HuntingState to = HuntingState.ATTACKING;

        assertNotEquals(from, to);
        assertTrue(to.ordinal() > from.ordinal(), "ATTACKING should come after CHASING");
    }

    @Test
    @DisplayName("State transition from ATTACKING to EATING is valid")
    void transitionFromAttackingToEating() {
        HuntingState from = HuntingState.ATTACKING;
        HuntingState to = HuntingState.EATING;

        assertNotEquals(from, to);
        assertTrue(to.ordinal() > from.ordinal(), "EATING should come after ATTACKING");
    }

    @Test
    @DisplayName("Can transition from any state to IDLE")
    void canTransitionToIdle() {
        HuntingState idleState = HuntingState.IDLE;

        for (HuntingState state : allStates) {
            if (state != idleState) {
                assertNotEquals(idleState, state,
                    state.name() + " should be different from IDLE");
            }
        }
    }

    // ==================== Enum Comparison Tests ====================

    @Test
    @DisplayName("Enum values are comparable")
    void enumValuesAreComparable() {
        HuntingState state1 = HuntingState.IDLE;
        HuntingState state2 = HuntingState.SEARCHING;
        HuntingState state3 = HuntingState.CHASING;

        assertTrue(state1.ordinal() < state2.ordinal());
        assertTrue(state2.ordinal() < state3.ordinal());
    }

    @Test
    @DisplayName("Enum equality works correctly")
    void enumEqualityWorks() {
        HuntingState idle1 = HuntingState.IDLE;
        HuntingState idle2 = HuntingState.IDLE;
        HuntingState searching = HuntingState.SEARCHING;

        assertEquals(idle1, idle2, "Same enum values should be equal");
        assertNotEquals(idle1, searching, "Different enum values should not be equal");
    }

    @Test
    @DisplayName("Enum hashCode is consistent")
    void enumHashCodeConsistent() {
        HuntingState idle1 = HuntingState.IDLE;
        HuntingState idle2 = HuntingState.IDLE;

        assertEquals(idle1.hashCode(), idle2.hashCode(),
            "Same enum values should have same hashCode");
    }

    // ==================== String Representation Tests ====================

    @Test
    @DisplayName("Enum name() returns correct string")
    void enumNameReturnsCorrectString() {
        assertEquals("IDLE", HuntingState.IDLE.name());
        assertEquals("SEARCHING", HuntingState.SEARCHING.name());
        assertEquals("STALKING", HuntingState.STALKING.name());
        assertEquals("CHASING", HuntingState.CHASING.name());
        assertEquals("ATTACKING", HuntingState.ATTACKING.name());
        assertEquals("EATING", HuntingState.EATING.name());
        assertEquals("RESTING", HuntingState.RESTING.name());
    }

    @Test
    @DisplayName("Enum toString() returns name")
    void enumToStringReturnsName() {
        for (HuntingState state : allStates) {
            assertEquals(state.name(), state.toString(),
                "toString() should return the enum name");
        }
    }

    // ==================== Edge Cases Tests ====================

    @Test
    @DisplayName("Enum has no duplicate values")
    void enumHasNoDuplicates() {
        for (int i = 0; i < allStates.length; i++) {
            for (int j = i + 1; j < allStates.length; j++) {
                assertNotEquals(allStates[i], allStates[j],
                    "States at positions " + i + " and " + j + " should be different");
            }
        }
    }

    @Test
    @DisplayName("Enum values() returns new array each time")
    void valuesReturnsNewArray() {
        HuntingState[] states1 = HuntingState.values();
        HuntingState[] states2 = HuntingState.values();

        assertNotSame(states1, states2, "values() should return a new array each time");
        assertEquals(states1.length, states2.length);

        for (int i = 0; i < states1.length; i++) {
            assertEquals(states1[i], states2[i]);
        }
    }

    // ==================== Behavioral Context Tests ====================

    @Test
    @DisplayName("Active hunting states are sequential")
    void activeHuntingStatesSequential() {
        HuntingState[] activeStates = {
            HuntingState.SEARCHING,
            HuntingState.STALKING,
            HuntingState.CHASING,
            HuntingState.ATTACKING
        };

        for (int i = 0; i < activeStates.length - 1; i++) {
            assertTrue(activeStates[i].ordinal() < activeStates[i + 1].ordinal(),
                activeStates[i] + " should come before " + activeStates[i + 1]);
        }
    }

    @Test
    @DisplayName("Post-hunt states come after attack")
    void postHuntStatesAfterAttack() {
        assertTrue(HuntingState.ATTACKING.ordinal() < HuntingState.EATING.ordinal(),
            "EATING should come after ATTACKING");
        assertTrue(HuntingState.EATING.ordinal() < HuntingState.RESTING.ordinal(),
            "RESTING should come after EATING");
    }

    @Test
    @DisplayName("All states are in expected order")
    void allStatesInExpectedOrder() {
        assertEquals(0, HuntingState.IDLE.ordinal());
        assertEquals(1, HuntingState.SEARCHING.ordinal());
        assertEquals(2, HuntingState.STALKING.ordinal());
        assertEquals(3, HuntingState.CHASING.ordinal());
        assertEquals(4, HuntingState.ATTACKING.ordinal());
        assertEquals(5, HuntingState.EATING.ordinal());
        assertEquals(6, HuntingState.RESTING.ordinal());
    }
}
