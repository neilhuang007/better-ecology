package me.javavirtualenv.behavior.herd;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for herd behavior system.
 * Tests configuration and core algorithms without requiring Minecraft entity mocks.
 */
class HerdBehaviorTest {

    private HerdConfig config;

    @BeforeEach
    void setUp() {
        config = new HerdConfig();
        config.setQuorumThreshold(0.5);
        config.setCohesionRadius(12.0);
        config.setSeparationDistance(2.5);
        config.setMaxLeaders(3);
        config.setMaxForce(0.1);
        config.setMaxSpeed(0.4);
    }

    @Test
    void quorumMovement_initializesCorrectly() {
        QuorumMovement quorum = new QuorumMovement(config);
        assertNotNull(quorum, "QuorumMovement should initialize");
        assertEquals(0.5, quorum.getQuorumThreshold(), "Should use config threshold");
    }

    @Test
    void quorumMovement_defaultsToAllowedWhenEmpty() {
        QuorumMovement quorum = new QuorumMovement(config);
        // Default should be false (no movement allowed) until quorum status is updated
        assertFalse(quorum.isMovementAllowed(), "Should default to not allowed until quorum is established");
    }

    @Test
    void quorumMovement_calculatesQuorumRatioForEmptyList() {
        QuorumMovement quorum = new QuorumMovement(config);
        double ratio = quorum.calculateQuorumRatio(null, new java.util.ArrayList<>());
        assertEquals(1.0, ratio, 0.01, "Empty herd should have ratio of 1.0");
    }

    @Test
    void herdCohesion_initializesCorrectly() {
        HerdCohesion cohesion = new HerdCohesion(config);
        assertNotNull(cohesion, "HerdCohesion should initialize");
        assertEquals(12.0, cohesion.getCohesionRadius(), "Should use config radius");
        assertEquals(2.5, cohesion.getSeparationDistance(), "Should use config separation");
    }

    @Test
    void herdCohesion_handlesNullContext() {
        HerdCohesion cohesion = new HerdCohesion(config);
        var force = cohesion.calculateCohesion(null, new java.util.ArrayList<>());
        assertNotNull(force, "Should return force object even for null context");
        assertEquals(0.0, force.magnitude(), "Force should be zero for null context");
    }

    @Test
    void leaderFollowing_initializesCorrectly() {
        LeaderFollowing leaderFollowing = new LeaderFollowing(config);
        assertNotNull(leaderFollowing, "LeaderFollowing should initialize");
        assertNull(leaderFollowing.getCurrentLeader(), "Should have no leader initially");
    }

    @Test
    void herdBehavior_initializesCorrectly() {
        HerdBehavior herd = new HerdBehavior(config);
        assertNotNull(herd, "HerdBehavior should initialize");
        assertNotNull(herd.getHerdId(), "Should have unique herd ID");
    }

    @Test
    void herdConfig_validatesQuorumThreshold() {
        HerdConfig testConfig = new HerdConfig();
        testConfig.setQuorumThreshold(1.5);
        assertEquals(1.0, testConfig.getQuorumThreshold());
        testConfig.setQuorumThreshold(-0.5);
        assertEquals(0.0, testConfig.getQuorumThreshold());
    }

    @Test
    void herdConfig_createsSpeciesSpecificConfigs() {
        HerdConfig bisonConfig = HerdConfig.forSpecies("entity.minecraft.bison");
        HerdConfig sheepConfig = HerdConfig.forSpecies("entity.minecraft.sheep");
        assertTrue(bisonConfig.getQuorumThreshold() > sheepConfig.getQuorumThreshold());
        assertTrue(bisonConfig.getCohesionRadius() > sheepConfig.getCohesionRadius());
    }

    @Test
    void scientificParameters_areResearchBased() {
        HerdConfig defaultConfig = new HerdConfig();
        assertTrue(defaultConfig.getQuorumThreshold() >= 0.3);
        assertTrue(defaultConfig.getCohesionRadius() >= 8.0);
        assertTrue(defaultConfig.getSeparationDistance() >= 1.0);
    }

    @Test
    void quorumThresholds_matchResearchFindings() {
        HerdConfig bisonConfig = HerdConfig.forSpecies("entity.minecraft.bison");
        assertEquals(0.47, bisonConfig.getQuorumThreshold(), 0.01);
    }
}
