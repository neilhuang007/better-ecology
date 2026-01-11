package me.javavirtualenv.behavior.sleeping;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class SleepingConfigTest {

    @Test
    void builder_createsConfigWithDefaults() {
        SleepingConfig config = SleepingConfig.builder().build();
        assertNotNull(config);
        assertEquals(SleepingConfig.ActivityCycle.DIURNAL, config.getActivityCycle());
        assertEquals(13000, config.getSleepStart());
        assertEquals(1000, config.getSleepEnd());
        assertEquals(6000, config.getSleepDuration());
        assertEquals(0.5, config.getVigilanceThreshold(), 0.001);
        assertEquals(0.3, config.getSocialSleepBonus(), 0.001);
        assertTrue(config.isSentinelDuty());
        assertEquals(1200, config.getSentinelRotationInterval());
        assertEquals(32, config.getMaxSleepSiteDistance());
        assertTrue(config.isPreferCoveredSites());
        assertFalse(config.isPreferElevatedSites());
        assertEquals(8.0, config.getGroupSleepRadius(), 0.001);
        assertEquals(SleepingConfig.SleepPosture.LYING_DOWN, config.getPosture());
        assertEquals(5, config.getRapidAwakeningResponseTicks());
    }

    @Test
    void forSpecies_cat_returnsLongSleepDuration() {
        SleepingConfig config = SleepingConfig.forSpecies("cat");
        assertEquals(SleepingConfig.ActivityCycle.CREPUSCULAR, config.getActivityCycle());
        assertEquals(12000, config.getSleepStart());
        assertEquals(2000, config.getSleepEnd());
        assertEquals(10000, config.getSleepDuration());
        assertFalse(config.isSentinelDuty());
        assertEquals(SleepingConfig.SleepPosture.CURLED_UP, config.getPosture());
    }

    @Test
    void forSpecies_ocelot_returnsLongSleepDuration() {
        SleepingConfig config = SleepingConfig.forSpecies("ocelot");
        assertEquals(SleepingConfig.ActivityCycle.CREPUSCULAR, config.getActivityCycle());
        assertEquals(10000, config.getSleepDuration());
        assertEquals(SleepingConfig.SleepPosture.CURLED_UP, config.getPosture());
    }

    @Test
    void sleepPosture_hasCorrectValues() {
        SleepingConfig.SleepPosture[] postures = SleepingConfig.SleepPosture.values();
        assertEquals(7, postures.length);
    }

    @Test
    void scientific_catConfig_matchesFelineBehavior() {
        SleepingConfig config = SleepingConfig.forSpecies("cat");
        assertEquals(10000, config.getSleepDuration());
        assertEquals(SleepingConfig.SleepPosture.CURLED_UP, config.getPosture());
        assertEquals(SleepingConfig.ActivityCycle.CREPUSCULAR, config.getActivityCycle());
    }
}
