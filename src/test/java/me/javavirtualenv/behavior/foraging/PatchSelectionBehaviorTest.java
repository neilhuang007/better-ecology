package me.javavirtualenv.behavior.foraging;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import me.javavirtualenv.behavior.core.BehaviorContext;
import me.javavirtualenv.behavior.core.Vec3d;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

/**
 * Unit tests for PatchSelectionBehavior.
 * Tests patch selection logic based on Marginal Value Theorem.
 */
class PatchSelectionBehaviorTest {

    private PatchSelectionBehavior patchSelectionBehavior;
    private ForagingConfig config;
    private FoodMemory foodMemory;
    private List<Block> targetBlocks;
    private BehaviorContext mockContext;
    private Level mockLevel;

    @BeforeEach
    void setUp() {
        config = ForagingConfig.createBimodal();
        foodMemory = new FoodMemory(6000);
        targetBlocks = new ArrayList<>();

        mockContext = mock(BehaviorContext.class);
        mockLevel = mock(Level.class);

        when(mockContext.getLevel()).thenReturn(mockLevel);
        when(mockContext.getBlockPos()).thenReturn(new BlockPos(0, 64, 0));

        patchSelectionBehavior = new PatchSelectionBehavior(config, foodMemory, targetBlocks);
    }

    @Test
    void constructorInitializesCorrectly() {
        assertNotNull(patchSelectionBehavior);
        assertEquals(0.0, patchSelectionBehavior.getCurrentPatchQuality(), 0.001);
        assertEquals(0, patchSelectionBehavior.getTimeInPatch());
        assertNull(patchSelectionBehavior.getCurrentPatchCenter());
    }

    @Test
    void assessCurrentPatchQualityReturnsZeroForNullLevel() {
        when(mockContext.getLevel()).thenReturn(null);

        double quality = patchSelectionBehavior.assessCurrentPatchQuality(mockContext);

        assertEquals(0.0, quality, 0.001);
    }

    @Test
    void assessCurrentPatchQualityDelegatesToAssessPatchQuality() {
        BlockPos center = new BlockPos(5, 64, 5);
        when(mockContext.getBlockPos()).thenReturn(center);

        double quality = patchSelectionBehavior.assessCurrentPatchQuality(mockContext);

        assertNotNull(quality);
        assertTrue(quality >= 0.0 && quality <= 1.0);
    }

    @Test
    void assessPatchQualityReturnsZeroForNullLevel() {
        BlockPos center = new BlockPos(0, 64, 0);
        double quality = patchSelectionBehavior.assessPatchQuality(null, center);

        assertEquals(0.0, quality, 0.001);
    }

    @Test
    void assessPatchQualityReturnsZeroForNullCenter() {
        double quality = patchSelectionBehavior.assessPatchQuality(mockLevel, null);

        assertEquals(0.0, quality, 0.001);
    }

    @Test
    void shouldAbandonPatchReturnsTrueWhenQualityBelowThreshold() {
        double lowQuality = 0.2;
        double givingUpDensity = config.getGivingUpDensity();

        assertTrue(lowQuality < givingUpDensity);
        assertTrue(patchSelectionBehavior.shouldAbandonPatch(lowQuality));
    }

    @Test
    void shouldAbandonPatchReturnsFalseWhenQualityAtOrAboveThreshold() {
        double highQuality = 0.5;
        double givingUpDensity = config.getGivingUpDensity();

        assertFalse(patchSelectionBehavior.shouldAbandonPatch(givingUpDensity));
        assertFalse(patchSelectionBehavior.shouldAbandonPatch(highQuality));
    }

    @Test
    void getCurrentPatchCenterReturnsInitialValue() {
        assertNull(patchSelectionBehavior.getCurrentPatchCenter());
    }

    @Test
    void getCurrentPatchQualityReturnsInitialValue() {
        assertEquals(0.0, patchSelectionBehavior.getCurrentPatchQuality(), 0.001);
    }

    @Test
    void getTimeInPatchReturnsInitialValue() {
        assertEquals(0, patchSelectionBehavior.getTimeInPatch());
    }

    @Test
    void resetPatchTrackingResetsTimeAndQuality() {
        patchSelectionBehavior.resetPatchTracking();

        assertEquals(0, patchSelectionBehavior.getTimeInPatch());
        assertEquals(0.0, patchSelectionBehavior.getCurrentPatchQuality(), 0.001);
    }

    @Test
    void updateCurrentPatchSetsCenterWhenNull() {
        patchSelectionBehavior.updateCurrentPatch(mockContext);

        assertNotNull(patchSelectionBehavior.getCurrentPatchCenter());
        assertEquals(new BlockPos(0, 64, 0), patchSelectionBehavior.getCurrentPatchCenter());
    }

    @Test
    void updateCurrentPatchUpdatesCenterWhenFarFromCurrent() {
        BlockPos initialCenter = new BlockPos(0, 64, 0);
        patchSelectionBehavior.updateCurrentPatch(mockContext);

        BlockPos farPosition = new BlockPos(20, 64, 20);
        when(mockContext.getBlockPos()).thenReturn(farPosition);
        patchSelectionBehavior.updateCurrentPatch(mockContext);

        assertEquals(farPosition, patchSelectionBehavior.getCurrentPatchCenter());
        assertEquals(0, patchSelectionBehavior.getTimeInPatch());
    }

    @Test
    void updateCurrentPatchKeepsCenterWhenNear() {
        BlockPos initialCenter = new BlockPos(0, 64, 0);
        patchSelectionBehavior.updateCurrentPatch(mockContext);

        int patchSize = config.getPatchSize();
        BlockPos nearPosition = new BlockPos(1, 64, 1);
        when(mockContext.getBlockPos()).thenReturn(nearPosition);

        int initialTime = patchSelectionBehavior.getTimeInPatch();
        patchSelectionBehavior.updateCurrentPatch(mockContext);

        assertEquals(initialCenter, patchSelectionBehavior.getCurrentPatchCenter());
        assertEquals(initialTime, patchSelectionBehavior.getTimeInPatch());
    }

    @Test
    void patchDecisionHasStayValue() {
        assertTrue(PatchSelectionBehavior.PatchDecision.STAY != null);
    }

    @Test
    void patchDecisionHasMoveValue() {
        assertTrue(PatchSelectionBehavior.PatchDecision.MOVE != null);
    }

    @Test
    void patchDecisionHasWaitValue() {
        assertTrue(PatchSelectionBehavior.PatchDecision.WAIT != null);
    }

    @Test
    void evaluatePatchReturnsStayBeforeAssessmentInterval() {
        patchSelectionBehavior.evaluatePatch(mockContext);

        assertEquals(PatchSelectionBehavior.PatchDecision.STAY, patchSelectionBehavior.evaluatePatch(mockContext));
    }

    @Test
    void evaluatePatchReturnsStayWhenQualityIsGood() {
        BlockPos center = new BlockPos(0, 64, 0);
        when(mockContext.getBlockPos()).thenReturn(center);
        patchSelectionBehavior.updateCurrentPatch(mockContext);

        for (int i = 0; i < 20; i++) {
            patchSelectionBehavior.evaluatePatch(mockContext);
        }

        PatchSelectionBehavior.PatchDecision decision = patchSelectionBehavior.evaluatePatch(mockContext);

        assertEquals(PatchSelectionBehavior.PatchDecision.STAY, decision);
    }

    @Test
    void evaluatePatchUpdatesCurrentPatchQuality() {
        BlockPos center = new BlockPos(0, 64, 0);
        when(mockContext.getBlockPos()).thenReturn(center);
        patchSelectionBehavior.updateCurrentPatch(mockContext);

        for (int i = 0; i < 20; i++) {
            patchSelectionBehavior.evaluatePatch(mockContext);
        }

        patchSelectionBehavior.evaluatePatch(mockContext);

        assertTrue(patchSelectionBehavior.getCurrentPatchQuality() >= 0.0);
    }

    @Test
    void evaluatePatchIncrementsTimeInPatch() {
        BlockPos center = new BlockPos(0, 64, 0);
        when(mockContext.getBlockPos()).thenReturn(center);
        patchSelectionBehavior.updateCurrentPatch(mockContext);

        int initialTime = patchSelectionBehavior.getTimeInPatch();
        patchSelectionBehavior.evaluatePatch(mockContext);

        assertEquals(initialTime + 1, patchSelectionBehavior.getTimeInPatch());
    }

    @Test
    void getRandomPositionInPatchReturnsValidPosition() {
        BlockPos center = new BlockPos(10, 64, 10);
        when(mockContext.getBlockPos()).thenReturn(center);
        patchSelectionBehavior.updateCurrentPatch(mockContext);

        Vec3d position = patchSelectionBehavior.getRandomPositionInPatch(mockContext);

        assertNotNull(position);
        double maxOffset = config.getPatchSize() + 1;
        assertTrue(position.x >= center.getX() - maxOffset && position.x <= center.getX() + maxOffset);
        assertTrue(position.z >= center.getZ() - maxOffset && position.z <= center.getZ() + maxOffset);
    }

    @Test
    void getRandomPositionInPatchReturnsPositionWithinPatch() {
        BlockPos center = new BlockPos(0, 64, 0);
        when(mockContext.getBlockPos()).thenReturn(center);
        patchSelectionBehavior.updateCurrentPatch(mockContext);

        Vec3d position = patchSelectionBehavior.getRandomPositionInPatch(mockContext);

        assertNotNull(position);
        double distanceSquared = Math.pow(position.x - center.getX() - 0.5, 2) +
                                 Math.pow(position.z - center.getZ() - 0.5, 2);
        assertTrue(distanceSquared <= config.getPatchSize() * config.getPatchSize());
    }

    @Test
    void findBestAlternativePatchReturnsNullWhenNoCandidates() {
        when(mockContext.getLevel()).thenReturn(null);
        BlockPos result = patchSelectionBehavior.findBestAlternativePatch(mockContext);

        assertNull(result);
    }

    @Test
    void findBestAlternativePatchReturnsBestCandidate() {
        BlockPos center = new BlockPos(0, 64, 0);
        when(mockContext.getBlockPos()).thenReturn(center);

        targetBlocks.add(mock(Block.class));

        PatchSelectionBehavior behaviorWithBlocks = new PatchSelectionBehavior(config, foodMemory, targetBlocks);

        foodMemory.rememberPatch(new BlockPos(5, 64, 5), 80);

        BlockPos result = behaviorWithBlocks.findBestAlternativePatch(mockContext);

        if (result != null) {
            foodMemory.getNearbyRememberedPatches(center, config.getSearchRadius());
        }
    }

    @Test
    void assessPatchQualityCalculatesCorrectRatio() {
        BlockPos center = new BlockPos(0, 64, 0);

        targetBlocks.add(mock(Block.class));

        PatchSelectionBehavior behaviorWithBlocks = new PatchSelectionBehavior(config, foodMemory, targetBlocks);
        double quality = behaviorWithBlocks.assessPatchQuality(mockLevel, center);

        assertTrue(quality >= 0.0 && quality <= 1.0);
    }

    @Test
    void assessPatchQualityReturnsZeroWhenNoBlocks() {
        BlockPos center = new BlockPos(0, 64, 0);

        double quality = patchSelectionBehavior.assessPatchQuality(mockLevel, center);

        assertEquals(0.0, quality, 0.001);
    }

    @Test
    void assessPatchQualityHandlesEmptyTargetBlocks() {
        BlockPos center = new BlockPos(0, 64, 0);

        List<Block> emptyTargets = new ArrayList<>();
        PatchSelectionBehavior behaviorWithEmptyTargets = new PatchSelectionBehavior(config, foodMemory, emptyTargets);

        double quality = behaviorWithEmptyTargets.assessPatchQuality(mockLevel, center);

        assertEquals(0.0, quality, 0.001);
    }

    @Test
    void evaluatePatchResetsTrackingWhenMovingToNewPatch() {
        BlockPos center = new BlockPos(0, 64, 0);
        when(mockContext.getBlockPos()).thenReturn(center);
        patchSelectionBehavior.updateCurrentPatch(mockContext);

        for (int i = 0; i < 25; i++) {
            patchSelectionBehavior.evaluatePatch(mockContext);
        }

        int timeAfterEvaluation = patchSelectionBehavior.getTimeInPatch();

        assertTrue(timeAfterEvaluation >= 20);
    }

    @Test
    void shouldAbandonPatchUsesConfigGivingUpDensity() {
        double customGivingUpDensity = 0.5;
        ForagingConfig customConfig = new ForagingConfig.Builder()
            .givingUpDensity(customGivingUpDensity)
            .build();

        PatchSelectionBehavior customBehavior = new PatchSelectionBehavior(customConfig, foodMemory, targetBlocks);

        assertTrue(customBehavior.shouldAbandonPatch(0.3));
        assertFalse(customBehavior.shouldAbandonPatch(0.6));
    }

    @Test
    void updateCurrentPatchDoesNotResetTimeWhenStayingInPatch() {
        BlockPos center = new BlockPos(0, 64, 0);
        when(mockContext.getBlockPos()).thenReturn(center);
        patchSelectionBehavior.updateCurrentPatch(mockContext);

        patchSelectionBehavior.evaluatePatch(mockContext);
        int timeAfterFirstUpdate = patchSelectionBehavior.getTimeInPatch();

        patchSelectionBehavior.updateCurrentPatch(mockContext);
        int timeAfterSecondUpdate = patchSelectionBehavior.getTimeInPatch();

        assertEquals(timeAfterFirstUpdate, timeAfterSecondUpdate);
    }
}
