package me.javavirtualenv.behavior.breeding;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Comprehensive test suite for BreedingConfig.
 * Tests builder pattern validation, parameter bounds checking, default values,
 * and species-specific configurations.
 */
class BreedingConfigTest {

    private BreedingConfig config;

    @BeforeEach
    void setUp() {
        config = new BreedingConfig();
    }

    // ==================== Default Values Tests ====================

    @Test
    void defaultValues_areCorrect() {
        assertEquals(MatingSystem.MONOGAMY, config.getMatingSystem());
        assertEquals(32.0, config.getTerritorySize(), 0.001);
        assertEquals(0.8, config.getMateFidelity(), 0.001);
        assertEquals(20, config.getCourtshipDuration());
        assertEquals(16.0, config.getDisplayRange(), 0.001);
        assertEquals(DisplayType.DANCING, config.getDisplayType());
        assertEquals(0.5, config.getDisplayTraitWeight(), 0.001);
        assertEquals(0.3, config.getAgePreference(), 0.001);
        assertEquals(3, config.getBreedingSeasonStart());
        assertEquals(6, config.getBreedingSeasonEnd());
        assertFalse(config.isYearRoundBreeding());
        assertFalse(config.isPhotoperiodTrigger());
        assertEquals(1000, config.getMinDayLength());
        assertEquals(0.7, config.getMinHealthForBreeding(), 0.001);
        assertEquals(0, config.getMinAgeForBreeding());
        assertEquals(6000, config.getBreedingCooldown());
        assertFalse(config.isBiparentalCare());
        assertEquals(0.5, config.getParentalInvestmentLevel(), 0.001);
    }

    // ==================== MatingSystem Tests ====================

    @Test
    void setMatingSystem_setsCorrectly() {
        config.setMatingSystem(MatingSystem.POLYGYNY);
        assertEquals(MatingSystem.POLYGYNY, config.getMatingSystem());

        config.setMatingSystem(MatingSystem.LEKKING);
        assertEquals(MatingSystem.LEKKING, config.getMatingSystem());

        config.setMatingSystem(MatingSystem.POLYANDRY);
        assertEquals(MatingSystem.POLYANDRY, config.getMatingSystem());

        config.setMatingSystem(MatingSystem.PROMISCUITY);
        assertEquals(MatingSystem.PROMISCUITY, config.getMatingSystem());
    }

    @Test
    void matingSystem_hasAllExpectedValues() {
        MatingSystem[] systems = MatingSystem.values();

        assertEquals(5, systems.length, "Should have 5 mating system types");
        assertTrue(java.util.Arrays.asList(systems).contains(MatingSystem.MONOGAMY));
        assertTrue(java.util.Arrays.asList(systems).contains(MatingSystem.POLYGYNY));
        assertTrue(java.util.Arrays.asList(systems).contains(MatingSystem.LEKKING));
        assertTrue(java.util.Arrays.asList(systems).contains(MatingSystem.POLYANDRY));
        assertTrue(java.util.Arrays.asList(systems).contains(MatingSystem.PROMISCUITY));
    }

    // ==================== DisplayType Tests ====================

    @Test
    void setDisplayType_setsCorrectly() {
        config.setDisplayType(DisplayType.VOCALIZATION);
        assertEquals(DisplayType.VOCALIZATION, config.getDisplayType());

        config.setDisplayType(DisplayType.POSTURING);
        assertEquals(DisplayType.POSTURING, config.getDisplayType());

        config.setDisplayType(DisplayType.COLORATION);
        assertEquals(DisplayType.COLORATION, config.getDisplayType());

        config.setDisplayType(DisplayType.GIFT_GIVING);
        assertEquals(DisplayType.GIFT_GIVING, config.getDisplayType());

        config.setDisplayType(DisplayType.SCENT_MARKING);
        assertEquals(DisplayType.SCENT_MARKING, config.getDisplayType());
    }

    @Test
    void displayType_hasAllExpectedValues() {
        DisplayType[] types = DisplayType.values();

        assertEquals(6, types.length, "Should have 6 display types");
        assertTrue(java.util.Arrays.asList(types).contains(DisplayType.DANCING));
        assertTrue(java.util.Arrays.asList(types).contains(DisplayType.VOCALIZATION));
        assertTrue(java.util.Arrays.asList(types).contains(DisplayType.POSTURING));
        assertTrue(java.util.Arrays.asList(types).contains(DisplayType.COLORATION));
        assertTrue(java.util.Arrays.asList(types).contains(DisplayType.GIFT_GIVING));
        assertTrue(java.util.Arrays.asList(types).contains(DisplayType.SCENT_MARKING));
    }

    // ==================== Parameter Bounds Tests ====================

    @Test
    void setTerritorySize_positiveValue_setsCorrectly() {
        config.setTerritorySize(50.0);
        assertEquals(50.0, config.getTerritorySize(), 0.001);
    }

    @Test
    void setTerritorySize_negativeValue_storesRawValue() {
        config.setTerritorySize(-10.0);
        assertEquals(-10.0, config.getTerritorySize(), 0.001, "Negative territory size is stored; validate() will catch it");
    }

    @Test
    void setTerritorySize_zero_setsCorrectly() {
        config.setTerritorySize(0.0);
        assertEquals(0.0, config.getTerritorySize(), 0.001);
    }

    @Test
    void setMateFidelity_withinRange_setsCorrectly() {
        config.setMateFidelity(0.5);
        assertEquals(0.5, config.getMateFidelity(), 0.001);

        config.setMateFidelity(1.0);
        assertEquals(1.0, config.getMateFidelity(), 0.001);

        config.setMateFidelity(0.0);
        assertEquals(0.0, config.getMateFidelity(), 0.001);
    }

    @Test
    void setMateFidelity_aboveRange_storesRawValue() {
        config.setMateFidelity(1.5);
        assertEquals(1.5, config.getMateFidelity(), 0.001, "Raw value stored; validate() will catch it");
    }

    @Test
    void setMateFidelity_belowRange_storesRawValue() {
        config.setMateFidelity(-0.5);
        assertEquals(-0.5, config.getMateFidelity(), 0.001, "Raw value stored; validate() will catch it");
    }

    @Test
    void setCourtshipDuration_positiveValue_setsCorrectly() {
        config.setCourtshipDuration(100);
        assertEquals(100, config.getCourtshipDuration());
    }

    @Test
    void setCourtshipDuration_zero_storesRawValue() {
        config.setCourtshipDuration(0);
        assertEquals(0, config.getCourtshipDuration(), "Raw value stored; validate() will catch it");
    }

    @Test
    void setCourtshipDuration_negativeValue_storesRawValue() {
        config.setCourtshipDuration(-50);
        assertEquals(-50, config.getCourtshipDuration(), "Raw value stored; validate() will catch it");
    }

    @Test
    void setDisplayRange_positiveValue_setsCorrectly() {
        config.setDisplayRange(32.0);
        assertEquals(32.0, config.getDisplayRange(), 0.001);
    }

    @Test
    void setDisplayRange_belowMinimum_storesRawValue() {
        config.setDisplayRange(0.0);
        assertEquals(0.0, config.getDisplayRange(), 0.001, "Raw value stored; validate() will catch it");

        config.setDisplayRange(0.5);
        assertEquals(0.5, config.getDisplayRange(), 0.001, "Raw value stored; validate() will catch it");
    }

    @Test
    void setDisplayTraitWeight_withinRange_setsCorrectly() {
        config.setDisplayTraitWeight(0.7);
        assertEquals(0.7, config.getDisplayTraitWeight(), 0.001);

        config.setDisplayTraitWeight(1.0);
        assertEquals(1.0, config.getDisplayTraitWeight(), 0.001);

        config.setDisplayTraitWeight(0.0);
        assertEquals(0.0, config.getDisplayTraitWeight(), 0.001);
    }

    @Test
    void setDisplayTraitWeight_aboveRange_storesRawValue() {
        config.setDisplayTraitWeight(1.5);
        assertEquals(1.5, config.getDisplayTraitWeight(), 0.001);
    }

    @Test
    void setDisplayTraitWeight_belowRange_storesRawValue() {
        config.setDisplayTraitWeight(-0.3);
        assertEquals(-0.3, config.getDisplayTraitWeight(), 0.001);
    }

    @Test
    void setAgePreference_withinRange_setsCorrectly() {
        config.setAgePreference(0.6);
        assertEquals(0.6, config.getAgePreference(), 0.001);
    }

    @Test
    void setAgePreference_storesRawValue() {
        config.setAgePreference(1.5);
        assertEquals(1.5, config.getAgePreference(), 0.001);

        config.setAgePreference(-0.5);
        assertEquals(-0.5, config.getAgePreference(), 0.001);
    }

    @Test
    void setBreedingSeasonStart_withinRange_setsCorrectly() {
        config.setBreedingSeasonStart(5);
        assertEquals(5, config.getBreedingSeasonStart());
    }

    @Test
    void setBreedingSeasonStart_belowRange_clampsToMinimum() {
        config.setBreedingSeasonStart(-5);
        assertEquals(0, config.getBreedingSeasonStart(), "Should clamp to 0");

        config.setBreedingSeasonStart(0);
        assertEquals(0, config.getBreedingSeasonStart());
    }

    @Test
    void setBreedingSeasonStart_aboveRange_clampsToMaximum() {
        config.setBreedingSeasonStart(15);
        assertEquals(11, config.getBreedingSeasonStart(), "Should clamp to 11");

        config.setBreedingSeasonStart(11);
        assertEquals(11, config.getBreedingSeasonStart());
    }

    @Test
    void setBreedingSeasonEnd_withinRange_setsCorrectly() {
        config.setBreedingSeasonEnd(8);
        assertEquals(8, config.getBreedingSeasonEnd());
    }

    @Test
    void setBreedingSeasonEnd_clampsToRange() {
        config.setBreedingSeasonEnd(-3);
        assertEquals(0, config.getBreedingSeasonEnd());

        config.setBreedingSeasonEnd(20);
        assertEquals(11, config.getBreedingSeasonEnd());
    }

    @Test
    void setYearRoundBreeding_setsCorrectly() {
        config.setYearRoundBreeding(true);
        assertTrue(config.isYearRoundBreeding());

        config.setYearRoundBreeding(false);
        assertFalse(config.isYearRoundBreeding());
    }

    @Test
    void setPhotoperiodTrigger_setsCorrectly() {
        config.setPhotoperiodTrigger(true);
        assertTrue(config.isPhotoperiodTrigger());

        config.setPhotoperiodTrigger(false);
        assertFalse(config.isPhotoperiodTrigger());
    }

    @Test
    void setMinDayLength_nonNegative_setsCorrectly() {
        config.setMinDayLength(2000);
        assertEquals(2000, config.getMinDayLength());
    }

    @Test
    void setMinDayLength_negative_clampsToZero() {
        config.setMinDayLength(-500);
        assertEquals(0, config.getMinDayLength(), "Negative values should be clamped to 0");
    }

    @Test
    void setMinDayLength_zero_setsCorrectly() {
        config.setMinDayLength(0);
        assertEquals(0, config.getMinDayLength());
    }

    @Test
    void setMinHealthForBreeding_withinRange_setsCorrectly() {
        config.setMinHealthForBreeding(0.8);
        assertEquals(0.8, config.getMinHealthForBreeding(), 0.001);

        config.setMinHealthForBreeding(1.0);
        assertEquals(1.0, config.getMinHealthForBreeding(), 0.001);

        config.setMinHealthForBreeding(0.0);
        assertEquals(0.0, config.getMinHealthForBreeding(), 0.001);
    }

    @Test
    void setMinHealthForBreeding_storesRawValue() {
        config.setMinHealthForBreeding(1.5);
        assertEquals(1.5, config.getMinHealthForBreeding(), 0.001);

        config.setMinHealthForBreeding(-0.3);
        assertEquals(-0.3, config.getMinHealthForBreeding(), 0.001);
    }

    @Test
    void setMinAgeForBreeding_nonNegative_setsCorrectly() {
        config.setMinAgeForBreeding(100);
        assertEquals(100, config.getMinAgeForBreeding());
    }

    @Test
    void setMinAgeForBreeding_negative_clampsToZero() {
        config.setMinAgeForBreeding(-50);
        assertEquals(0, config.getMinAgeForBreeding(), "Negative values should be clamped to 0");
    }

    @Test
    void setBreedingCooldown_nonNegative_setsCorrectly() {
        config.setBreedingCooldown(12000);
        assertEquals(12000, config.getBreedingCooldown());
    }

    @Test
    void setBreedingCooldown_negative_storesRawValue() {
        config.setBreedingCooldown(-1000);
        assertEquals(-1000, config.getBreedingCooldown(), "Raw value stored; validate() will catch it");
    }

    @Test
    void setBiparentalCare_setsCorrectly() {
        config.setBiparentalCare(true);
        assertTrue(config.isBiparentalCare());

        config.setBiparentalCare(false);
        assertFalse(config.isBiparentalCare());
    }

    @Test
    void setParentalInvestmentLevel_withinRange_setsCorrectly() {
        config.setParentalInvestmentLevel(0.7);
        assertEquals(0.7, config.getParentalInvestmentLevel(), 0.001);

        config.setParentalInvestmentLevel(1.0);
        assertEquals(1.0, config.getParentalInvestmentLevel(), 0.001);

        config.setParentalInvestmentLevel(0.0);
        assertEquals(0.0, config.getParentalInvestmentLevel(), 0.001);
    }

    @Test
    void setParentalInvestmentLevel_storesRawValue() {
        config.setParentalInvestmentLevel(1.5);
        assertEquals(1.5, config.getParentalInvestmentLevel(), 0.001);

        config.setParentalInvestmentLevel(-0.5);
        assertEquals(-0.5, config.getParentalInvestmentLevel(), 0.001);
    }

    // ==================== Validation Tests ====================

    @Test
    void validate_withValidParameters_doesNotThrow() {
        config.setTerritorySize(50.0);
        config.setMateFidelity(0.8);
        config.setCourtshipDuration(30);
        config.setDisplayRange(20.0);
        config.setMinHealthForBreeding(0.7);
        config.setBreedingCooldown(6000);
        config.setParentalInvestmentLevel(0.6);

        assertDoesNotThrow(() -> config.validate());
    }

    @Test
    void validate_withNegativeTerritorySize_throwsException() {
        config.setTerritorySize(-10.0);

        assertThrows(IllegalStateException.class, () -> config.validate());
    }

    @Test
    void validate_withMateFidelityAboveRange_throwsException() {
        config.setMateFidelity(1.5);

        assertThrows(IllegalStateException.class, () -> config.validate());
    }

    @Test
    void validate_withMateFidelityBelowRange_throwsException() {
        config.setMateFidelity(-0.5);

        assertThrows(IllegalStateException.class, () -> config.validate());
    }

    @Test
    void validate_withInvalidCourtshipDuration_throwsException() {
        config.setCourtshipDuration(0);

        assertThrows(IllegalStateException.class, () -> config.validate());
    }

    @Test
    void validate_withDisplayRangeBelowMinimum_throwsException() {
        config.setDisplayRange(0.5);

        assertThrows(IllegalStateException.class, () -> config.validate());
    }

    @Test
    void validate_withMinHealthAboveRange_throwsException() {
        config.setMinHealthForBreeding(1.5);

        assertThrows(IllegalStateException.class, () -> config.validate());
    }

    @Test
    void validate_withMinHealthBelowRange_throwsException() {
        config.setMinHealthForBreeding(-0.3);

        assertThrows(IllegalStateException.class, () -> config.validate());
    }

    @Test
    void validate_withNegativeBreedingCooldown_throwsException() {
        config.setBreedingCooldown(-100);

        assertThrows(IllegalStateException.class, () -> config.validate());
    }

    @Test
    void validate_withParentalInvestmentAboveRange_throwsException() {
        config.setParentalInvestmentLevel(1.3);

        assertThrows(IllegalStateException.class, () -> config.validate());
    }

    @Test
    void validate_withParentalInvestmentBelowRange_throwsException() {
        config.setParentalInvestmentLevel(-0.2);

        assertThrows(IllegalStateException.class, () -> config.validate());
    }

    // ==================== Species Configuration Tests ====================

    @Test
    void forSpecies_wolf_returnsCorrectConfiguration() {
        BreedingConfig wolfConfig = BreedingConfig.forSpecies("entity.minecraft.wolf");

        assertEquals(MatingSystem.MONOGAMY, wolfConfig.getMatingSystem());
        assertEquals(0.95, wolfConfig.getMateFidelity(), 0.001);
        assertEquals(64.0, wolfConfig.getTerritorySize(), 0.001);
        assertTrue(wolfConfig.isBiparentalCare());
        assertEquals(0.9, wolfConfig.getParentalInvestmentLevel(), 0.001);
        assertEquals(12000, wolfConfig.getBreedingCooldown());
        assertEquals(0.8, wolfConfig.getMinHealthForBreeding(), 0.001);
    }

    @Test
    void forSpecies_parrot_returnsCorrectConfiguration() {
        BreedingConfig parrotConfig = BreedingConfig.forSpecies("entity.minecraft.parrot");

        assertEquals(MatingSystem.LEKKING, parrotConfig.getMatingSystem());
        assertEquals(60, parrotConfig.getCourtshipDuration());
        assertEquals(32.0, parrotConfig.getDisplayRange(), 0.001);
        assertEquals(DisplayType.DANCING, parrotConfig.getDisplayType());
        assertEquals(0.8, parrotConfig.getDisplayTraitWeight(), 0.001);
        assertEquals(0.3, parrotConfig.getMateFidelity(), 0.001);
        assertTrue(parrotConfig.isYearRoundBreeding());
    }

    @Test
    void forSpecies_deer_returnsCorrectConfiguration() {
        BreedingConfig deerConfig = BreedingConfig.forSpecies("entity.minecraft.deer");

        assertEquals(MatingSystem.POLYGYNY, deerConfig.getMatingSystem());
        assertEquals(48.0, deerConfig.getTerritorySize(), 0.001);
        assertEquals(DisplayType.POSTURING, deerConfig.getDisplayType());
        assertEquals(9, deerConfig.getBreedingSeasonStart());
        assertEquals(11, deerConfig.getBreedingSeasonEnd());
        assertEquals(0.2, deerConfig.getMateFidelity(), 0.001);
        assertFalse(deerConfig.isBiparentalCare());
    }

    @Test
    void forSpecies_cow_returnsCorrectConfiguration() {
        BreedingConfig cowConfig = BreedingConfig.forSpecies("entity.minecraft.cow");

        assertEquals(MatingSystem.POLYGYNY, cowConfig.getMatingSystem());
        assertEquals(0.2, cowConfig.getMateFidelity(), 0.001);
        assertEquals(0.7, cowConfig.getMinHealthForBreeding(), 0.001);
        assertTrue(cowConfig.isYearRoundBreeding());
        assertFalse(cowConfig.isBiparentalCare());
        assertEquals(0.6, cowConfig.getParentalInvestmentLevel(), 0.001);
    }

    @Test
    void forSpecies_unknown_returnsDefaultConfiguration() {
        BreedingConfig unknownConfig = BreedingConfig.forSpecies("entity.minecraft.unknown");

        assertNotNull(unknownConfig);
        assertEquals(MatingSystem.MONOGAMY, unknownConfig.getMatingSystem());
        assertEquals(0.8, unknownConfig.getMateFidelity(), 0.001);
    }

    @Test
    void forSpecies_null_returnsDefaultConfiguration() {
        BreedingConfig nullConfig = BreedingConfig.forSpecies(null);

        assertNotNull(nullConfig);
        assertEquals(MatingSystem.MONOGAMY, nullConfig.getMatingSystem());
    }

    // ==================== Scientific Validation Tests ====================

    @Test
    void scientific_wolfConfig_reflectsPackBehavior() {
        BreedingConfig wolfConfig = BreedingConfig.forSpecies("entity.minecraft.wolf");

        assertTrue(wolfConfig.isBiparentalCare(), "Wolves provide biparental care");
        assertEquals(0.95, wolfConfig.getMateFidelity(), 0.001, "Wolves have high mate fidelity");
        assertEquals(MatingSystem.MONOGAMY, wolfConfig.getMatingSystem(), "Wolves are monogamous");
    }

    @Test
    void scientific_parrotConfig_reflectsLekkingBehavior() {
        BreedingConfig parrotConfig = BreedingConfig.forSpecies("entity.minecraft.parrot");

        assertEquals(MatingSystem.LEKKING, parrotConfig.getMatingSystem());
        assertEquals(60, parrotConfig.getCourtshipDuration(), "Parrots have extended courtship");
        assertEquals(0.8, parrotConfig.getDisplayTraitWeight(), 0.001, "Display traits are heavily weighted");
    }

    @Test
    void scientific_deerConfig_reflectsPolygynyBehavior() {
        BreedingConfig deerConfig = BreedingConfig.forSpecies("entity.minecraft.deer");

        assertEquals(MatingSystem.POLYGYNY, deerConfig.getMatingSystem());
        assertEquals(9, deerConfig.getBreedingSeasonStart(), "Deer breed in fall");
        assertEquals(11, deerConfig.getBreedingSeasonEnd());
        assertFalse(deerConfig.isBiparentalCare(), "Deer lack biparental care");
    }

    @Test
    void scientific_cowConfig_reflectsHerdBehavior() {
        BreedingConfig cowConfig = BreedingConfig.forSpecies("entity.minecraft.cow");

        assertEquals(MatingSystem.POLYGYNY, cowConfig.getMatingSystem());
        assertEquals(0.2, cowConfig.getMateFidelity(), 0.001, "Low mate fidelity in polygynous systems");
        assertTrue(cowConfig.isYearRoundBreeding(), "Cattle can breed year-round");
    }

    @Test
    void scientific_territorySizes_areBiologicallyPlausible() {
        BreedingConfig wolfConfig = BreedingConfig.forSpecies("entity.minecraft.wolf");
        BreedingConfig deerConfig = BreedingConfig.forSpecies("entity.minecraft.deer");
        BreedingConfig parrotConfig = BreedingConfig.forSpecies("entity.minecraft.parrot");

        assertTrue(wolfConfig.getTerritorySize() >= 16.0 && wolfConfig.getTerritorySize() <= 128.0,
                   "Wolf territory should be within biological range");
        assertTrue(deerConfig.getTerritorySize() >= 16.0 && deerConfig.getTerritorySize() <= 128.0,
                   "Deer territory should be within biological range");
        assertTrue(parrotConfig.getDisplayRange() >= 8.0 && parrotConfig.getDisplayRange() <= 64.0,
                   "Parrot display range should be within biological range");
    }

    // ==================== Edge Case Tests ====================

    @Test
    void edgeCase_maximumValues_allAtMaximum() {
        config.setTerritorySize(Double.MAX_VALUE);
        config.setMateFidelity(1.0);
        config.setCourtshipDuration(Integer.MAX_VALUE);
        config.setDisplayRange(Double.MAX_VALUE);
        config.setDisplayTraitWeight(1.0);
        config.setAgePreference(1.0);
        config.setBreedingSeasonStart(11);
        config.setBreedingSeasonEnd(11);
        config.setMinDayLength(Long.MAX_VALUE);
        config.setMinHealthForBreeding(1.0);
        config.setMinAgeForBreeding(Integer.MAX_VALUE);
        config.setBreedingCooldown(Long.MAX_VALUE);
        config.setParentalInvestmentLevel(1.0);

        assertDoesNotThrow(() -> config.validate());
    }

    @Test
    void edgeCase_minimumValues_allAtMinimum() {
        config.setTerritorySize(0.0);
        config.setMateFidelity(0.0);
        config.setCourtshipDuration(1);
        config.setDisplayRange(1.0);
        config.setDisplayTraitWeight(0.0);
        config.setAgePreference(0.0);
        config.setBreedingSeasonStart(0);
        config.setBreedingSeasonEnd(0);
        config.setMinDayLength(0);
        config.setMinHealthForBreeding(0.0);
        config.setMinAgeForBreeding(0);
        config.setBreedingCooldown(0);
        config.setParentalInvestmentLevel(0.0);

        assertDoesNotThrow(() -> config.validate());
    }

    @Test
    void edgeCase_boundaryValues_exactlyAtBoundaries() {
        config.setMateFidelity(0.0);
        assertEquals(0.0, config.getMateFidelity(), 0.001);

        config.setMateFidelity(1.0);
        assertEquals(1.0, config.getMateFidelity(), 0.001);

        config.setDisplayTraitWeight(0.0);
        assertEquals(0.0, config.getDisplayTraitWeight(), 0.001);

        config.setDisplayTraitWeight(1.0);
        assertEquals(1.0, config.getDisplayTraitWeight(), 0.001);

        config.setBreedingSeasonStart(0);
        assertEquals(0, config.getBreedingSeasonStart());

        config.setBreedingSeasonStart(11);
        assertEquals(11, config.getBreedingSeasonStart());

        config.setBreedingSeasonEnd(0);
        assertEquals(0, config.getBreedingSeasonEnd());

        config.setBreedingSeasonEnd(11);
        assertEquals(11, config.getBreedingSeasonEnd());
    }

    @Test
    void edgeCase_floatingPointPrecision_handlesSmallValues() {
        config.setMateFidelity(0.001);
        assertEquals(0.001, config.getMateFidelity(), 0.0001);

        config.setDisplayTraitWeight(0.001);
        assertEquals(0.001, config.getDisplayTraitWeight(), 0.0001);

        config.setAgePreference(0.001);
        assertEquals(0.001, config.getAgePreference(), 0.0001);

        config.setParentalInvestmentLevel(0.001);
        assertEquals(0.001, config.getParentalInvestmentLevel(), 0.0001);
    }

    @Test
    void edgeCase_independentConfigs_noSharedState() {
        BreedingConfig config1 = new BreedingConfig();
        BreedingConfig config2 = new BreedingConfig();

        config1.setMatingSystem(MatingSystem.POLYGYNY);
        config1.setTerritorySize(100.0);

        assertEquals(MatingSystem.MONOGAMY, config2.getMatingSystem());
        assertEquals(32.0, config2.getTerritorySize(), 0.001);
    }
}
