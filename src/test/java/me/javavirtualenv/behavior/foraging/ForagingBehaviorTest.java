package me.javavirtualenv.behavior.foraging;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import net.minecraft.core.BlockPos;

/**
 * Unit tests for ForagingBehavior and FoodMemory.
 * Tests core logic without requiring Minecraft registry initialization.
 */
class ForagingBehaviorTest {

    private ForagingBehavior foragingBehavior;
    private FoodMemory foodMemory;

    private static final double SEARCH_RADIUS = 10.0;
    private static final int SEARCH_INTERVAL = 20;
    private static final int MEMORY_DURATION = 1000;
    private static final int HUNGER_RESTORE = 2;

    @BeforeEach
    void setUp() {
        foragingBehavior = new ForagingBehavior(
            SEARCH_RADIUS,
            SEARCH_INTERVAL,
            null, // Use null to avoid Block registry dependency in tests
            MEMORY_DURATION,
            HUNGER_RESTORE
        );

        foodMemory = foragingBehavior.getFoodMemory();
    }

    @Test
    void initialStateIsSearching() {
        assertEquals(ForagingBehavior.ForagingState.SEARCHING, foragingBehavior.getState());
        assertNull(foragingBehavior.getCurrentTarget());
    }

    @Test
    void foodMemoryRemembersPatches() {
        BlockPos patchLocation = new BlockPos(10, 64, 10);
        int foodAmount = 15;

        foodMemory.rememberPatch(patchLocation, foodAmount);

        assertEquals(foodAmount, foodMemory.getEstimatedFood(patchLocation));
        assertTrue(foodMemory.hasViablePatch(patchLocation));
    }

    @Test
    void foodMemoryForgetsOldPatches() {
        BlockPos patchLocation = new BlockPos(10, 64, 10);

        FoodMemory shortMemory = new FoodMemory(-1); // Negative duration causes immediate forgetting
        shortMemory.rememberPatch(patchLocation, 10);
        shortMemory.forgetOldPatches();

        assertFalse(shortMemory.hasViablePatch(patchLocation));
    }

    @Test
    void foodMemoryConsumesFromPatch() {
        BlockPos patchLocation = new BlockPos(10, 64, 10);
        int initialAmount = 10;
        foodMemory.rememberPatch(patchLocation, initialAmount);

        foodMemory.consumeFromPatch(patchLocation, 3);

        assertEquals(initialAmount - 3, foodMemory.getEstimatedFood(patchLocation));
    }

    @Test
    void foodMemoryMarksDepletedPatches() {
        BlockPos patchLocation = new BlockPos(10, 64, 10);
        foodMemory.rememberPatch(patchLocation, 5);

        foodMemory.consumeFromPatch(patchLocation, 5);

        assertFalse(foodMemory.hasViablePatch(patchLocation));
        assertEquals(0, foodMemory.getEstimatedFood(patchLocation));
    }

    @Test
    void foodMemoryGetBestPatchReturnsNullWhenEmpty() {
        assertNull(foodMemory.getBestPatch());
    }

    @Test
    void foodMemoryGetBestPatchReturnsHighestQuality() {
        BlockPos lowQualityPatch = new BlockPos(10, 64, 10);
        BlockPos highQualityPatch = new BlockPos(20, 64, 20);

        foodMemory.rememberPatch(lowQualityPatch, 5);
        foodMemory.rememberPatch(highQualityPatch, 15);

        BlockPos bestPatch = foodMemory.getBestPatch();

        assertEquals(highQualityPatch, bestPatch);
    }

    @Test
    void foodMemoryClearRemovesAllPatches() {
        foodMemory.rememberPatch(new BlockPos(10, 64, 10), 10);
        foodMemory.rememberPatch(new BlockPos(20, 64, 20), 15);

        foodMemory.clear();

        assertNull(foodMemory.getBestPatch());
    }

    @Test
    void foragingBehaviorHasCorrectConfiguration() {
        assertEquals(SEARCH_RADIUS, foragingBehavior.getSearchRadius(), 0.001);
        assertEquals(SEARCH_INTERVAL, foragingBehavior.getSearchInterval());
        assertEquals(HUNGER_RESTORE, foragingBehavior.getHungerRestore());
    }

    @Test
    void foragingBehaviorHandlesNullTargetBlocks() {
        ForagingBehavior nullBlockBehavior = new ForagingBehavior(
            SEARCH_RADIUS,
            SEARCH_INTERVAL,
            null,
            MEMORY_DURATION,
            HUNGER_RESTORE
        );

        assertNotNull(nullBlockBehavior.getFoodMemory());
        assertFalse(nullBlockBehavior.isTargetBlock(null));
    }

    @Test
    void foragingBehaviorHandlesEmptyTargetBlocks() {
        List<net.minecraft.world.level.block.Block> emptyBlocks = new ArrayList<>();
        ForagingBehavior emptyBlockBehavior = new ForagingBehavior(
            SEARCH_RADIUS,
            SEARCH_INTERVAL,
            emptyBlocks,
            MEMORY_DURATION,
            HUNGER_RESTORE
        );

        assertNotNull(emptyBlockBehavior.getFoodMemory());
    }

    @Test
    void foodMemoryUpdatesExistingPatch() {
        BlockPos patchLocation = new BlockPos(10, 64, 10);
        foodMemory.rememberPatch(patchLocation, 10);

        // Update the same patch with new amount
        foodMemory.rememberPatch(patchLocation, 20);

        assertEquals(20, foodMemory.getEstimatedFood(patchLocation));
        assertTrue(foodMemory.hasViablePatch(patchLocation));
    }

    @Test
    void foodMemoryMarksPatchesAsDepleted() {
        BlockPos patchLocation = new BlockPos(10, 64, 10);
        foodMemory.rememberPatch(patchLocation, 10);

        foodMemory.markDepleted(patchLocation);

        assertFalse(foodMemory.hasViablePatch(patchLocation));
        assertEquals(0, foodMemory.getEstimatedFood(patchLocation));
    }

    @Test
    void foodMemoryGetNearbyPatchesFiltersByDistance() {
        BlockPos nearPatch = new BlockPos(10, 64, 10);
        BlockPos farPatch = new BlockPos(50, 64, 50);
        BlockPos center = new BlockPos(10, 64, 10);

        foodMemory.rememberPatch(nearPatch, 10);
        foodMemory.rememberPatch(farPatch, 15);

        var nearbyPatches = foodMemory.getNearbyRememberedPatches(center, 5.0);

        assertEquals(1, nearbyPatches.size());
        assertEquals(nearPatch, nearbyPatches.get(0).location);
    }
}
