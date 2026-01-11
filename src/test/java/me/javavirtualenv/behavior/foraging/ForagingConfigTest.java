package me.javavirtualenv.behavior.foraging;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for ForagingConfig.
 * Tests configuration parameters and builder pattern for different foraging strategies.
 */
class ForagingConfigTest {

    @Test
    void builderPatternWorksCorrectly() {
        ForagingConfig config = new ForagingConfig.Builder()
            .grazingStartTime(1000)
            .grazingEndTime(11000)
            .middayRestStart(5000)
            .middayRestEnd(7000)
            .patchSize(5)
            .givingUpDensity(0.3)
            .biteSize(1)
            .grazingSpeed(0.3)
            .socialDominanceFactor(0.5)
            .memoryDuration(6000)
            .searchRadius(20.0)
            .hungerRestore(2)
            .hydrationRestoreAmount(5)
            .build();

        assertNotNull(config);
        assertEquals(1000, config.getGrazingStartTime());
        assertEquals(11000, config.getGrazingEndTime());
        assertEquals(5000, config.getMiddayRestStart());
        assertEquals(7000, config.getMiddayRestEnd());
        assertEquals(5, config.getPatchSize());
        assertEquals(0.3, config.getGivingUpDensity(), 0.001);
        assertEquals(1, config.getBiteSize());
        assertEquals(0.3, config.getGrazingSpeed(), 0.001);
        assertEquals(0.5, config.getSocialDominanceFactor(), 0.001);
        assertEquals(6000, config.getMemoryDuration());
        assertEquals(20.0, config.getSearchRadius(), 0.001);
        assertEquals(2, config.getHungerRestore());
        assertEquals(5, config.getHydrationRestoreAmount());
    }

    @Test
    void createBimodalCreatesBimodalGrazingConfig() {
        ForagingConfig config = ForagingConfig.createBimodal();

        assertNotNull(config);
        assertEquals(1000, config.getGrazingStartTime());
        assertEquals(11000, config.getGrazingEndTime());
        assertEquals(5000, config.getMiddayRestStart());
        assertEquals(7000, config.getMiddayRestEnd());
        assertEquals(5, config.getPatchSize());
        assertEquals(0.3, config.getGivingUpDensity(), 0.001);
        assertEquals(1, config.getBiteSize());
        assertEquals(0.3, config.getGrazingSpeed(), 0.001);
        assertEquals(0.5, config.getSocialDominanceFactor(), 0.001);
        assertEquals(6000, config.getMemoryDuration());
        assertEquals(20.0, config.getSearchRadius(), 0.001);
        assertEquals(2, config.getHungerRestore());
        assertEquals(5, config.getHydrationRestoreAmount());
    }

    @Test
    void createContinuousCreatesContinuousGrazingConfig() {
        ForagingConfig config = ForagingConfig.createContinuous();

        assertNotNull(config);
        assertEquals(0, config.getGrazingStartTime());
        assertEquals(12000, config.getGrazingEndTime());
        assertEquals(0, config.getMiddayRestStart());
        assertEquals(0, config.getMiddayRestEnd());
        assertEquals(7, config.getPatchSize());
        assertEquals(0.25, config.getGivingUpDensity(), 0.001);
        assertEquals(2, config.getBiteSize());
        assertEquals(0.25, config.getGrazingSpeed(), 0.001);
        assertEquals(0.3, config.getSocialDominanceFactor(), 0.001);
        assertEquals(8000, config.getMemoryDuration());
        assertEquals(25.0, config.getSearchRadius(), 0.001);
        assertEquals(3, config.getHungerRestore());
        assertEquals(7, config.getHydrationRestoreAmount());
    }

    @Test
    void createSelectiveCreatesSelectiveBrowsingConfig() {
        ForagingConfig config = ForagingConfig.createSelective();

        assertNotNull(config);
        assertEquals(500, config.getGrazingStartTime());
        assertEquals(11500, config.getGrazingEndTime());
        assertEquals(5500, config.getMiddayRestStart());
        assertEquals(6500, config.getMiddayRestEnd());
        assertEquals(3, config.getPatchSize());
        assertEquals(0.5, config.getGivingUpDensity(), 0.001);
        assertEquals(1, config.getBiteSize());
        assertEquals(0.35, config.getGrazingSpeed(), 0.001);
        assertEquals(0.2, config.getSocialDominanceFactor(), 0.001);
        assertEquals(4000, config.getMemoryDuration());
        assertEquals(15.0, config.getSearchRadius(), 0.001);
        assertEquals(2, config.getHungerRestore());
        assertEquals(5, config.getHydrationRestoreAmount());
    }

    @Test
    void createSheepCreatesSheepSpecificConfig() {
        ForagingConfig config = ForagingConfig.createSheep();

        assertNotNull(config);
        assertEquals(1000, config.getGrazingStartTime());
        assertEquals(11000, config.getGrazingEndTime());
        assertEquals(5000, config.getMiddayRestStart());
        assertEquals(7000, config.getMiddayRestEnd());
        assertEquals(3, config.getPatchSize());
        assertEquals(0.4, config.getGivingUpDensity(), 0.001);
        assertEquals(1, config.getBiteSize());
        assertEquals(0.35, config.getGrazingSpeed(), 0.001);
        assertEquals(0.3, config.getSocialDominanceFactor(), 0.001);
        assertEquals(7000, config.getMemoryDuration());
        assertEquals(25.0, config.getSearchRadius(), 0.001);
        assertEquals(2, config.getHungerRestore());
        assertEquals(5, config.getHydrationRestoreAmount());
    }

    @Test
    void createCattleCreatesCattleSpecificConfig() {
        ForagingConfig config = ForagingConfig.createCattle();

        assertNotNull(config);
        assertEquals(1000, config.getGrazingStartTime());
        assertEquals(11000, config.getGrazingEndTime());
        assertEquals(5000, config.getMiddayRestStart());
        assertEquals(7000, config.getMiddayRestEnd());
        assertEquals(7, config.getPatchSize());
        assertEquals(0.25, config.getGivingUpDensity(), 0.001);
        assertEquals(2, config.getBiteSize());
        assertEquals(0.25, config.getGrazingSpeed(), 0.001);
        assertEquals(0.6, config.getSocialDominanceFactor(), 0.001);
        assertEquals(5000, config.getMemoryDuration());
        assertEquals(20.0, config.getSearchRadius(), 0.001);
        assertEquals(3, config.getHungerRestore());
        assertEquals(7, config.getHydrationRestoreAmount());
    }

    @Test
    void allGettersReturnExpectedValues() {
        ForagingConfig config = ForagingConfig.createBimodal();

        assertEquals(1000, config.getGrazingStartTime());
        assertEquals(11000, config.getGrazingEndTime());
        assertEquals(5000, config.getMiddayRestStart());
        assertEquals(7000, config.getMiddayRestEnd());
        assertEquals(5, config.getPatchSize());
        assertEquals(0.3, config.getGivingUpDensity(), 0.001);
        assertEquals(1, config.getBiteSize());
        assertEquals(0.3, config.getGrazingSpeed(), 0.001);
        assertEquals(0.5, config.getSocialDominanceFactor(), 0.001);
        assertEquals(6000, config.getMemoryDuration());
        assertEquals(20.0, config.getSearchRadius(), 0.001);
        assertEquals(2, config.getHungerRestore());
        assertEquals(5, config.getHydrationRestoreAmount());
    }

    @Test
    void builderHasDefaultValues() {
        ForagingConfig config = new ForagingConfig.Builder().build();

        assertNotNull(config);
        assertEquals(1000, config.getGrazingStartTime());
        assertEquals(11000, config.getGrazingEndTime());
        assertEquals(5000, config.getMiddayRestStart());
        assertEquals(7000, config.getMiddayRestEnd());
        assertEquals(5, config.getPatchSize());
        assertEquals(0.3, config.getGivingUpDensity(), 0.001);
        assertEquals(1, config.getBiteSize());
        assertEquals(0.3, config.getGrazingSpeed(), 0.001);
        assertEquals(0.5, config.getSocialDominanceFactor(), 0.001);
        assertEquals(6000, config.getMemoryDuration());
        assertEquals(20.0, config.getSearchRadius(), 0.001);
        assertEquals(2, config.getHungerRestore());
        assertEquals(5, config.getHydrationRestoreAmount());
    }

    @Test
    void builderSupportsChaining() {
        ForagingConfig config = new ForagingConfig.Builder()
            .grazingStartTime(500)
            .grazingEndTime(12000)
            .patchSize(10)
            .build();

        assertEquals(500, config.getGrazingStartTime());
        assertEquals(12000, config.getGrazingEndTime());
        assertEquals(10, config.getPatchSize());
    }

    @Test
    void sheepHasSmallerPatchSizeThanCattle() {
        ForagingConfig sheepConfig = ForagingConfig.createSheep();
        ForagingConfig cattleConfig = ForagingConfig.createCattle();

        assertTrue(sheepConfig.getPatchSize() < cattleConfig.getPatchSize());
    }

    @Test
    void sheepHasHigherGivingUpDensityThanCattle() {
        ForagingConfig sheepConfig = ForagingConfig.createSheep();
        ForagingConfig cattleConfig = ForagingConfig.createCattle();

        assertTrue(sheepConfig.getGivingUpDensity() > cattleConfig.getGivingUpDensity());
    }

    @Test
    void cattleHasHigherSocialDominanceThanSheep() {
        ForagingConfig sheepConfig = ForagingConfig.createSheep();
        ForagingConfig cattleConfig = ForagingConfig.createCattle();

        assertTrue(cattleConfig.getSocialDominanceFactor() > sheepConfig.getSocialDominanceFactor());
    }

    @Test
    void continuousHasLongerGrazingWindowThanBimodal() {
        ForagingConfig bimodalConfig = ForagingConfig.createBimodal();
        ForagingConfig continuousConfig = ForagingConfig.createContinuous();

        long bimodalWindow = bimodalConfig.getGrazingEndTime() - bimodalConfig.getGrazingStartTime();
        long continuousWindow = continuousConfig.getGrazingEndTime() - continuousConfig.getGrazingStartTime();

        assertTrue(continuousWindow >= bimodalWindow);
    }

    @Test
    void continuousHasNoMiddayRest() {
        ForagingConfig continuousConfig = ForagingConfig.createContinuous();

        assertEquals(0, continuousConfig.getMiddayRestStart());
        assertEquals(0, continuousConfig.getMiddayRestEnd());
    }

    @Test
    void selectiveHasHigherGivingUpDensityThanGrazers() {
        ForagingConfig selectiveConfig = ForagingConfig.createSelective();
        ForagingConfig bimodalConfig = ForagingConfig.createBimodal();

        assertTrue(selectiveConfig.getGivingUpDensity() > bimodalConfig.getGivingUpDensity());
    }

    @Test
    void selectiveHasSmallerPatchSizeThanGrazers() {
        ForagingConfig selectiveConfig = ForagingConfig.createSelective();
        ForagingConfig bimodalConfig = ForagingConfig.createBimodal();

        assertTrue(selectiveConfig.getPatchSize() < bimodalConfig.getPatchSize());
    }

    @Test
    void selectiveHasLowerSocialDominanceThanGrazers() {
        ForagingConfig selectiveConfig = ForagingConfig.createSelective();
        ForagingConfig bimodalConfig = ForagingConfig.createBimodal();

        assertTrue(selectiveConfig.getSocialDominanceFactor() < bimodalConfig.getSocialDominanceFactor());
    }

    @Test
    void builderAllowsCustomGivingUpDensity() {
        double customDensity = 0.7;
        ForagingConfig config = new ForagingConfig.Builder()
            .givingUpDensity(customDensity)
            .build();

        assertEquals(customDensity, config.getGivingUpDensity(), 0.001);
    }

    @Test
    void builderAllowsCustomPatchSize() {
        int customPatchSize = 15;
        ForagingConfig config = new ForagingConfig.Builder()
            .patchSize(customPatchSize)
            .build();

        assertEquals(customPatchSize, config.getPatchSize());
    }

    @Test
    void builderAllowsCustomSearchRadius() {
        double customRadius = 35.0;
        ForagingConfig config = new ForagingConfig.Builder()
            .searchRadius(customRadius)
            .build();

        assertEquals(customRadius, config.getSearchRadius(), 0.001);
    }

    @Test
    void builderAllowsCustomMemoryDuration() {
        int customDuration = 10000;
        ForagingConfig config = new ForagingConfig.Builder()
            .memoryDuration(customDuration)
            .build();

        assertEquals(customDuration, config.getMemoryDuration());
    }

    @Test
    void builderAllowsCustomHungerRestore() {
        int customHungerRestore = 5;
        ForagingConfig config = new ForagingConfig.Builder()
            .hungerRestore(customHungerRestore)
            .build();

        assertEquals(customHungerRestore, config.getHungerRestore());
    }

    @Test
    void builderAllowsCustomHydrationRestore() {
        int customHydrationRestore = 10;
        ForagingConfig config = new ForagingConfig.Builder()
            .hydrationRestoreAmount(customHydrationRestore)
            .build();

        assertEquals(customHydrationRestore, config.getHydrationRestoreAmount());
    }

    @Test
    void builderAllowsCustomGrazingSpeed() {
        double customSpeed = 0.5;
        ForagingConfig config = new ForagingConfig.Builder()
            .grazingSpeed(customSpeed)
            .build();

        assertEquals(customSpeed, config.getGrazingSpeed(), 0.001);
    }

    @Test
    void builderAllowsCustomBiteSize() {
        int customBiteSize = 3;
        ForagingConfig config = new ForagingConfig.Builder()
            .biteSize(customBiteSize)
            .build();

        assertEquals(customBiteSize, config.getBiteSize());
    }
}
