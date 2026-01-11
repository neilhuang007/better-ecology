package me.javavirtualenv.behavior;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for BehaviorWeights.
 * Tests default values, getters, setters, and copy functionality.
 */
class BehaviorWeightsTest {

    private BehaviorWeights weights;

    @BeforeEach
    void setUp() {
        weights = new BehaviorWeights();
    }

    // Flocking behavior tests

    @Test
    void defaultSeparationValue() {
        assertEquals(1.5, weights.getSeparation(), 0.001);
    }

    @Test
    void setSeparationValue() {
        weights.setSeparation(2.5);
        assertEquals(2.5, weights.getSeparation(), 0.001);
    }

    @Test
    void defaultAlignmentValue() {
        assertEquals(1.0, weights.getAlignment(), 0.001);
    }

    @Test
    void setAlignmentValue() {
        weights.setAlignment(1.8);
        assertEquals(1.8, weights.getAlignment(), 0.001);
    }

    @Test
    void defaultCohesionValue() {
        assertEquals(1.0, weights.getCohesion(), 0.001);
    }

    @Test
    void setCohesionValue() {
        weights.setCohesion(1.3);
        assertEquals(1.3, weights.getCohesion(), 0.001);
    }

    // Foraging behavior tests

    @Test
    void defaultFoodSeekValue() {
        assertEquals(2.0, weights.getFoodSeek(), 0.001);
    }

    @Test
    void setFoodSeekValue() {
        weights.setFoodSeek(3.0);
        assertEquals(3.0, weights.getFoodSeek(), 0.001);
    }

    @Test
    void defaultWaterSeekValue() {
        assertEquals(1.5, weights.getWaterSeek(), 0.001);
    }

    @Test
    void setWaterSeekValue() {
        weights.setWaterSeek(2.2);
        assertEquals(2.2, weights.getWaterSeek(), 0.001);
    }

    @Test
    void defaultShelterSeekValue() {
        assertEquals(0.8, weights.getShelterSeek(), 0.001);
    }

    @Test
    void setShelterSeekValue() {
        weights.setShelterSeek(1.0);
        assertEquals(1.0, weights.getShelterSeek(), 0.001);
    }

    // Predation behavior tests

    @Test
    void defaultPursuitValue() {
        assertEquals(2.0, weights.getPursuit(), 0.001);
    }

    @Test
    void setPursuitValue() {
        weights.setPursuit(2.5);
        assertEquals(2.5, weights.getPursuit(), 0.001);
    }

    @Test
    void defaultEvasionValue() {
        assertEquals(3.0, weights.getEvasion(), 0.001);
    }

    @Test
    void setEvasionValue() {
        weights.setEvasion(3.5);
        assertEquals(3.5, weights.getEvasion(), 0.001);
    }

    // Territorial behavior tests

    @Test
    void defaultTerritorialDefenseValue() {
        assertEquals(1.2, weights.getTerritorialDefense(), 0.001);
    }

    @Test
    void setTerritorialDefenseValue() {
        weights.setTerritorialDefense(1.8);
        assertEquals(1.8, weights.getTerritorialDefense(), 0.001);
    }

    @Test
    void defaultWanderValue() {
        assertEquals(0.5, weights.getWander(), 0.001);
    }

    @Test
    void setWanderValue() {
        weights.setWander(0.7);
        assertEquals(0.7, weights.getWander(), 0.001);
    }

    // Environmental behavior tests

    @Test
    void defaultObstacleAvoidanceValue() {
        assertEquals(2.5, weights.getObstacleAvoidance(), 0.001);
    }

    @Test
    void setObstacleAvoidanceValue() {
        weights.setObstacleAvoidance(3.0);
        assertEquals(3.0, weights.getObstacleAvoidance(), 0.001);
    }

    @Test
    void defaultLightPreferenceValue() {
        assertEquals(0.3, weights.getLightPreference(), 0.001);
    }

    @Test
    void setLightPreferenceValue() {
        weights.setLightPreference(0.5);
        assertEquals(0.5, weights.getLightPreference(), 0.001);
    }

    // Bee behavior tests

    @Test
    void defaultPollinationValue() {
        assertEquals(1.5, weights.getPollination(), 0.001);
    }

    @Test
    void setPollinationValue() {
        weights.setPollination(2.0);
        assertEquals(2.0, weights.getPollination(), 0.001);
    }

    @Test
    void defaultHiveReturnValue() {
        assertEquals(2.0, weights.getHiveReturn(), 0.001);
    }

    @Test
    void setHiveReturnValue() {
        weights.setHiveReturn(2.5);
        assertEquals(2.5, weights.getHiveReturn(), 0.001);
    }

    @Test
    void defaultWaggleDanceValue() {
        assertEquals(0.5, weights.getWaggleDance(), 0.001);
    }

    @Test
    void setWaggleDanceValue() {
        weights.setWaggleDance(0.8);
        assertEquals(0.8, weights.getWaggleDance(), 0.001);
    }

    @Test
    void defaultHiveDefenseValue() {
        assertEquals(2.5, weights.getHiveDefense(), 0.001);
    }

    @Test
    void setHiveDefenseValue() {
        weights.setHiveDefense(3.0);
        assertEquals(3.0, weights.getHiveDefense(), 0.001);
    }

    // Allay behavior tests

    @Test
    void defaultItemCollectingValue() {
        assertEquals(2.0, weights.getItem_collecting(), 0.001);
    }

    @Test
    void setItemCollectingValue() {
        weights.setItem_collecting(2.5);
        assertEquals(2.5, weights.getItem_collecting(), 0.001);
    }

    @Test
    void defaultSoundFollowingValue() {
        assertEquals(1.8, weights.getSound_following(), 0.001);
    }

    @Test
    void setSoundFollowingValue() {
        weights.setSound_following(2.2);
        assertEquals(2.2, weights.getSound_following(), 0.001);
    }

    // Strider behavior tests

    @Test
    void defaultLavaWalkingValue() {
        assertEquals(2.0, weights.getLava_walking(), 0.001);
    }

    @Test
    void setLavaWalkingValue() {
        weights.setLava_walking(2.5);
        assertEquals(2.5, weights.getLava_walking(), 0.001);
    }

    @Test
    void defaultTemperatureSeekingValue() {
        assertEquals(1.5, weights.getTemperature_seeking(), 0.001);
    }

    @Test
    void setTemperatureSeekingValue() {
        weights.setTemperature_seeking(1.8);
        assertEquals(1.8, weights.getTemperature_seeking(), 0.001);
    }

    @Test
    void defaultRidingValue() {
        assertEquals(1.0, weights.getRiding(), 0.001);
    }

    @Test
    void setRidingValue() {
        weights.setRiding(1.5);
        assertEquals(1.5, weights.getRiding(), 0.001);
    }

    // Sniffer behavior tests

    @Test
    void defaultSniffingValue() {
        assertEquals(1.5, weights.getSniffing(), 0.001);
    }

    @Test
    void setSniffingValue() {
        weights.setSniffing(2.0);
        assertEquals(2.0, weights.getSniffing(), 0.001);
    }

    @Test
    void defaultDiggingValue() {
        assertEquals(2.0, weights.getDigging(), 0.001);
    }

    @Test
    void setDiggingValue() {
        weights.setDigging(2.5);
        assertEquals(2.5, weights.getDigging(), 0.001);
    }

    @Test
    void defaultSnifferSocialValue() {
        assertEquals(1.2, weights.getSnifferSocial(), 0.001);
    }

    @Test
    void setSnifferSocialValue() {
        weights.setSnifferSocial(1.5);
        assertEquals(1.5, weights.getSnifferSocial(), 0.001);
    }

    // Armadillo behavior tests

    @Test
    void defaultPredatorAvoidanceValue() {
        assertEquals(2.5, weights.getPredatorAvoidance(), 0.001);
    }

    @Test
    void setPredatorAvoidanceValue() {
        weights.setPredatorAvoidance(3.0);
        assertEquals(3.0, weights.getPredatorAvoidance(), 0.001);
    }

    // Aquatic behavior tests

    @Test
    void defaultSchoolingValue() {
        assertEquals(1.5, weights.getSchooling(), 0.001);
    }

    @Test
    void setSchoolingValue() {
        weights.setSchooling(2.0);
        assertEquals(2.0, weights.getSchooling(), 0.001);
    }

    @Test
    void defaultCurrentRidingValue() {
        assertEquals(0.8, weights.getCurrentRiding(), 0.001);
    }

    @Test
    void setCurrentRidingValue() {
        weights.setCurrentRiding(1.0);
        assertEquals(1.0, weights.getCurrentRiding(), 0.001);
    }

    @Test
    void defaultEscapeValue() {
        assertEquals(2.0, weights.getEscape(), 0.001);
    }

    @Test
    void setEscapeValue() {
        weights.setEscape(2.5);
        assertEquals(2.5, weights.getEscape(), 0.001);
    }

    @Test
    void defaultPanicValue() {
        assertEquals(2.5, weights.getPanic(), 0.001);
    }

    @Test
    void setPanicValue() {
        weights.setPanic(3.0);
        assertEquals(3.0, weights.getPanic(), 0.001);
    }

    @Test
    void defaultUpstreamValue() {
        assertEquals(1.0, weights.getUpstream(), 0.001);
    }

    @Test
    void setUpstreamValue() {
        weights.setUpstream(1.5);
        assertEquals(1.5, weights.getUpstream(), 0.001);
    }

    @Test
    void defaultInkCloudValue() {
        assertEquals(1.5, weights.getInkCloud(), 0.001);
    }

    @Test
    void setInkCloudValue() {
        weights.setInkCloud(2.0);
        assertEquals(2.0, weights.getInkCloud(), 0.001);
    }

    @Test
    void defaultVerticalMigrationValue() {
        assertEquals(0.8, weights.getVerticalMigration(), 0.001);
    }

    @Test
    void setVerticalMigrationValue() {
        weights.setVerticalMigration(1.0);
        assertEquals(1.0, weights.getVerticalMigration(), 0.001);
    }

    @Test
    void defaultPreyAttractionValue() {
        assertEquals(1.0, weights.getPreyAttraction(), 0.001);
    }

    @Test
    void setPreyAttractionValue() {
        weights.setPreyAttraction(1.5);
        assertEquals(1.5, weights.getPreyAttraction(), 0.001);
    }

    @Test
    void defaultInflateValue() {
        assertEquals(2.0, weights.getInflate(), 0.001);
    }

    @Test
    void setInflateValue() {
        weights.setInflate(2.5);
        assertEquals(2.5, weights.getInflate(), 0.001);
    }

    @Test
    void defaultHuntingValue() {
        assertEquals(1.5, weights.getHunting(), 0.001);
    }

    @Test
    void setHuntingValue() {
        weights.setHunting(2.0);
        assertEquals(2.0, weights.getHunting(), 0.001);
    }

    @Test
    void defaultPlayDeadValue() {
        assertEquals(2.0, weights.getPlayDead(), 0.001);
    }

    @Test
    void setPlayDeadValue() {
        weights.setPlayDead(2.5);
        assertEquals(2.5, weights.getPlayDead(), 0.001);
    }

    @Test
    void defaultWaveRidingValue() {
        assertEquals(1.2, weights.getWaveRiding(), 0.001);
    }

    @Test
    void setWaveRidingValue() {
        weights.setWaveRiding(1.5);
        assertEquals(1.5, weights.getWaveRiding(), 0.001);
    }

    @Test
    void defaultTreasureHuntValue() {
        assertEquals(1.0, weights.getTreasureHunt(), 0.001);
    }

    @Test
    void setTreasureHuntValue() {
        weights.setTreasureHunt(1.5);
        assertEquals(1.5, weights.getTreasureHunt(), 0.001);
    }

    @Test
    void defaultMetamorphosisValue() {
        assertEquals(0.5, weights.getMetamorphosis(), 0.001);
    }

    @Test
    void setMetamorphosisValue() {
        weights.setMetamorphosis(0.8);
        assertEquals(0.8, weights.getMetamorphosis(), 0.001);
    }

    @Test
    void defaultHomeRangeValue() {
        assertEquals(1.0, weights.getHomeRange(), 0.001);
    }

    @Test
    void setHomeRangeValue() {
        weights.setHomeRange(1.5);
        assertEquals(1.5, weights.getHomeRange(), 0.001);
    }

    @Test
    void defaultRoostingValue() {
        assertEquals(1.0, weights.getRoosting(), 0.001);
    }

    @Test
    void setRoostingValue() {
        weights.setRoosting(1.3);
        assertEquals(1.3, weights.getRoosting(), 0.001);
    }

    // Copy method tests

    @Test
    void copyCreatesNewInstance() {
        BehaviorWeights copy = weights.copy();
        assertNotNull(copy);
        assertNotSame(weights, copy);
    }

    @Test
    void copyHasSameFlockingWeights() {
        weights.setSeparation(2.0);
        weights.setAlignment(1.5);
        weights.setCohesion(1.3);

        BehaviorWeights copy = weights.copy();

        assertEquals(weights.getSeparation(), copy.getSeparation(), 0.001);
        assertEquals(weights.getAlignment(), copy.getAlignment(), 0.001);
        assertEquals(weights.getCohesion(), copy.getCohesion(), 0.001);
    }

    @Test
    void copyHasSameForagingWeights() {
        weights.setFoodSeek(2.5);
        weights.setWaterSeek(2.0);
        weights.setShelterSeek(1.0);

        BehaviorWeights copy = weights.copy();

        assertEquals(weights.getFoodSeek(), copy.getFoodSeek(), 0.001);
        assertEquals(weights.getWaterSeek(), copy.getWaterSeek(), 0.001);
        assertEquals(weights.getShelterSeek(), copy.getShelterSeek(), 0.001);
    }

    @Test
    void copyHasSamePredationWeights() {
        weights.setPursuit(2.5);
        weights.setEvasion(3.5);

        BehaviorWeights copy = weights.copy();

        assertEquals(weights.getPursuit(), copy.getPursuit(), 0.001);
        assertEquals(weights.getEvasion(), copy.getEvasion(), 0.001);
    }

    @Test
    void copyHasSameTerritorialWeights() {
        weights.setTerritorialDefense(1.8);
        weights.setWander(0.7);

        BehaviorWeights copy = weights.copy();

        assertEquals(weights.getTerritorialDefense(), copy.getTerritorialDefense(), 0.001);
        assertEquals(weights.getWander(), copy.getWander(), 0.001);
    }

    @Test
    void copyHasSameEnvironmentalWeights() {
        weights.setObstacleAvoidance(3.0);
        weights.setLightPreference(0.5);

        BehaviorWeights copy = weights.copy();

        assertEquals(weights.getObstacleAvoidance(), copy.getObstacleAvoidance(), 0.001);
        assertEquals(weights.getLightPreference(), copy.getLightPreference(), 0.001);
    }

    @Test
    void copyHasSameBeeWeights() {
        weights.setPollination(2.0);
        weights.setHiveReturn(2.5);
        weights.setWaggleDance(0.8);
        weights.setHiveDefense(3.0);

        BehaviorWeights copy = weights.copy();

        assertEquals(weights.getPollination(), copy.getPollination(), 0.001);
        assertEquals(weights.getHiveReturn(), copy.getHiveReturn(), 0.001);
        assertEquals(weights.getWaggleDance(), copy.getWaggleDance(), 0.001);
        assertEquals(weights.getHiveDefense(), copy.getHiveDefense(), 0.001);
    }

    @Test
    void copyHasSameAllayWeights() {
        weights.setItem_collecting(2.5);
        weights.setSound_following(2.2);

        BehaviorWeights copy = weights.copy();

        assertEquals(weights.getItem_collecting(), copy.getItem_collecting(), 0.001);
        assertEquals(weights.getSound_following(), copy.getSound_following(), 0.001);
    }

    @Test
    void copyHasSameStriderWeights() {
        weights.setLava_walking(2.5);
        weights.setTemperature_seeking(1.8);
        weights.setRiding(1.5);

        BehaviorWeights copy = weights.copy();

        assertEquals(weights.getLava_walking(), copy.getLava_walking(), 0.001);
        assertEquals(weights.getTemperature_seeking(), copy.getTemperature_seeking(), 0.001);
        assertEquals(weights.getRiding(), copy.getRiding(), 0.001);
    }

    @Test
    void copyHasSameSnifferWeights() {
        weights.setSniffing(2.0);
        weights.setDigging(2.5);
        weights.setSnifferSocial(1.5);

        BehaviorWeights copy = weights.copy();

        assertEquals(weights.getSniffing(), copy.getSniffing(), 0.001);
        assertEquals(weights.getDigging(), copy.getDigging(), 0.001);
        assertEquals(weights.getSnifferSocial(), copy.getSnifferSocial(), 0.001);
    }

    @Test
    void copyHasSameArmadilloWeights() {
        weights.setPredatorAvoidance(3.0);

        BehaviorWeights copy = weights.copy();

        assertEquals(weights.getPredatorAvoidance(), copy.getPredatorAvoidance(), 0.001);
    }

    @Test
    void copyHasSameAquaticWeights() {
        weights.setSchooling(2.0);
        weights.setCurrentRiding(1.0);
        weights.setEscape(2.5);
        weights.setPanic(3.0);
        weights.setUpstream(1.5);
        weights.setInkCloud(2.0);
        weights.setVerticalMigration(1.0);
        weights.setPreyAttraction(1.5);
        weights.setInflate(2.5);
        weights.setHunting(2.0);
        weights.setPlayDead(2.5);
        weights.setWaveRiding(1.5);
        weights.setTreasureHunt(1.5);
        weights.setMetamorphosis(0.8);
        weights.setHomeRange(1.5);
        weights.setRoosting(1.3);

        BehaviorWeights copy = weights.copy();

        assertEquals(weights.getSchooling(), copy.getSchooling(), 0.001);
        assertEquals(weights.getCurrentRiding(), copy.getCurrentRiding(), 0.001);
        assertEquals(weights.getEscape(), copy.getEscape(), 0.001);
        assertEquals(weights.getPanic(), copy.getPanic(), 0.001);
        assertEquals(weights.getUpstream(), copy.getUpstream(), 0.001);
        assertEquals(weights.getInkCloud(), copy.getInkCloud(), 0.001);
        assertEquals(weights.getVerticalMigration(), copy.getVerticalMigration(), 0.001);
        assertEquals(weights.getPreyAttraction(), copy.getPreyAttraction(), 0.001);
        assertEquals(weights.getInflate(), copy.getInflate(), 0.001);
        assertEquals(weights.getHunting(), copy.getHunting(), 0.001);
        assertEquals(weights.getPlayDead(), copy.getPlayDead(), 0.001);
        assertEquals(weights.getWaveRiding(), copy.getWaveRiding(), 0.001);
        assertEquals(weights.getTreasureHunt(), copy.getTreasureHunt(), 0.001);
        assertEquals(weights.getMetamorphosis(), copy.getMetamorphosis(), 0.001);
        assertEquals(weights.getHomeRange(), copy.getHomeRange(), 0.001);
        assertEquals(weights.getRoosting(), copy.getRoosting(), 0.001);
    }

    @Test
    void modifyingCopyDoesNotAffectOriginal() {
        weights.setSeparation(1.5);
        weights.setAlignment(1.0);
        weights.setCohesion(1.0);

        BehaviorWeights copy = weights.copy();
        copy.setSeparation(3.0);
        copy.setAlignment(2.0);
        copy.setCohesion(2.0);

        assertEquals(1.5, weights.getSeparation(), 0.001);
        assertEquals(1.0, weights.getAlignment(), 0.001);
        assertEquals(1.0, weights.getCohesion(), 0.001);
        assertEquals(3.0, copy.getSeparation(), 0.001);
        assertEquals(2.0, copy.getAlignment(), 0.001);
        assertEquals(2.0, copy.getCohesion(), 0.001);
    }

    @Test
    void modifyingOriginalDoesNotAffectCopy() {
        weights.setSeparation(1.5);

        BehaviorWeights copy = weights.copy();
        weights.setSeparation(3.0);

        assertEquals(3.0, weights.getSeparation(), 0.001);
        assertEquals(1.5, copy.getSeparation(), 0.001);
    }

    @Test
    void copyOfModifiedWeightsHasModifiedValues() {
        weights.setFoodSeek(5.0);
        weights.setEvasion(10.0);
        weights.setHiveDefense(15.0);

        BehaviorWeights copy = weights.copy();

        assertEquals(5.0, copy.getFoodSeek(), 0.001);
        assertEquals(10.0, copy.getEvasion(), 0.001);
        assertEquals(15.0, copy.getHiveDefense(), 0.001);
    }

    @Test
    void canSetZeroWeights() {
        weights.setSeparation(0.0);
        weights.setAlignment(0.0);
        weights.setCohesion(0.0);
        weights.setFoodSeek(0.0);
        weights.setEvasion(0.0);

        assertEquals(0.0, weights.getSeparation(), 0.001);
        assertEquals(0.0, weights.getAlignment(), 0.001);
        assertEquals(0.0, weights.getCohesion(), 0.001);
        assertEquals(0.0, weights.getFoodSeek(), 0.001);
        assertEquals(0.0, weights.getEvasion(), 0.001);
    }

    @Test
    void canSetNegativeWeights() {
        weights.setSeparation(-1.0);
        weights.setAlignment(-2.0);
        weights.setWander(-0.5);

        assertEquals(-1.0, weights.getSeparation(), 0.001);
        assertEquals(-2.0, weights.getAlignment(), 0.001);
        assertEquals(-0.5, weights.getWander(), 0.001);
    }

    @Test
    void canSetVeryLargeWeights() {
        double largeValue = 1000.0;
        weights.setPursuit(largeValue);
        weights.setEvasion(largeValue);
        weights.setHiveDefense(largeValue);

        assertEquals(largeValue, weights.getPursuit(), 0.001);
        assertEquals(largeValue, weights.getEvasion(), 0.001);
        assertEquals(largeValue, weights.getHiveDefense(), 0.001);
    }
}
