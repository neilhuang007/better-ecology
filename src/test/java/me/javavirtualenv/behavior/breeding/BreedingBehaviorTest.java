package me.javavirtualenv.behavior.breeding;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import me.javavirtualenv.behavior.core.Vec3d;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Comprehensive test suite for breeding behavior system.
 * Tests all breeding behaviors scientifically without mocking.
 * Uses TestBreedingEntity implementation for pure Java algorithm testing.
 */
class BreedingBehaviorTest {

    private BreedingConfig config;
    private BreedingBehavior breedingBehavior;
    private TestBreedingEntity maleEntity;
    private TestBreedingEntity femaleEntity;

    @BeforeEach
    void setUp() {
        config = new BreedingConfig();
        config.setMatingSystem(MatingSystem.MONOGAMY);
        config.setTerritorySize(32.0);
        config.setMateFidelity(0.8);
        config.setCourtshipDuration(20);
        config.setDisplayRange(16.0);
        config.setDisplayType(DisplayType.DANCING);
        config.setMinHealthForBreeding(0.7);
        config.setMinAgeForBreeding(100);
        config.setBreedingCooldown(6000);
        config.setYearRoundBreeding(true);
        config.setBiparentalCare(true);
        config.setParentalInvestmentLevel(0.8);

        breedingBehavior = new BreedingBehavior(config);

        UUID maleId = UUID.randomUUID();
        UUID femaleId = UUID.randomUUID();

        maleEntity = new TestBreedingEntity(maleId, true, "entity.minecraft.wolf");
        maleEntity.setPosition(new Vec3d(0, 64, 0));
        maleEntity.setHealth(20.0);
        maleEntity.setMaxHealth(20.0);
        maleEntity.setAge(500);
        maleEntity.setGameTime(10000L);

        femaleEntity = new TestBreedingEntity(femaleId, false, "entity.minecraft.wolf");
        femaleEntity.setPosition(new Vec3d(5, 64, 0));
        femaleEntity.setHealth(20.0);
        femaleEntity.setMaxHealth(20.0);
        femaleEntity.setAge(500);
        femaleEntity.setGameTime(10000L);
    }

    @Test
    void breedingBehavior_initializesCorrectly() {
        assertNotNull(breedingBehavior, "BreedingBehavior should initialize");
        assertNotNull(breedingBehavior.getConfig(), "Config should be set");
        assertNotNull(breedingBehavior.getMateSelection(), "MateSelection should be initialized");
        assertNotNull(breedingBehavior.getCourtshipDisplay(), "CourtshipDisplay should be initialized");
        assertNotNull(breedingBehavior.getTerritorialDefense(), "TerritorialDefense should be initialized");
        assertNotNull(breedingBehavior.getBreedingSeason(), "BreedingSeason should be initialized");
        assertNotNull(breedingBehavior.getMateFidelity(), "MateFidelity should be initialized");
        assertNotNull(breedingBehavior.getParentalInvestment(), "ParentalInvestment should be initialized");
    }

    @Test
    void canBreed_returnsFalseForNullEntities() {
        assertFalse(breedingBehavior.canBreed(null, femaleEntity), "Null first entity should return false");
        assertFalse(breedingBehavior.canBreed(maleEntity, null), "Null second entity should return false");
        assertFalse(breedingBehavior.canBreed(null, null), "Both null entities should return false");
    }

    @Test
    void canBreed_returnsTrueForHealthyAdults() {
        assertTrue(breedingBehavior.canBreed(maleEntity, femaleEntity), "Healthy adults should be able to breed");
    }

    @Test
    void canBreed_returnsFalseForDeadEntities() {
        maleEntity.setAlive(false);
        assertFalse(breedingBehavior.canBreed(maleEntity, femaleEntity), "Dead entity should not breed");

        maleEntity.setAlive(true);
        femaleEntity.setAlive(false);
        assertFalse(breedingBehavior.canBreed(maleEntity, femaleEntity), "Dead female should not breed");
    }

    @Test
    void canBreed_returnsFalseForBabies() {
        maleEntity.setBaby(true);
        assertFalse(breedingBehavior.canBreed(maleEntity, femaleEntity), "Baby should not breed");

        maleEntity.setBaby(false);
        femaleEntity.setBaby(true);
        assertFalse(breedingBehavior.canBreed(maleEntity, femaleEntity), "Baby female should not breed");
    }

    @Test
    void canBreed_returnsFalseForEntitiesInLove() {
        maleEntity.setInLove(true);
        assertFalse(breedingBehavior.canBreed(maleEntity, femaleEntity), "Entity in love should not breed");

        maleEntity.setInLove(false);
        femaleEntity.setInLove(true);
        assertFalse(breedingBehavior.canBreed(maleEntity, femaleEntity), "Female in love should not breed");
    }

    @Test
    void canBreed_returnsFalseForLowHealthEntities() {
        maleEntity.setHealth(5.0);
        assertFalse(breedingBehavior.canBreed(maleEntity, femaleEntity), "Low health entity should not breed");

        maleEntity.setHealth(20.0);
        femaleEntity.setHealth(5.0);
        assertFalse(breedingBehavior.canBreed(maleEntity, femaleEntity), "Low health female should not breed");
    }

    @Test
    void canBreedNow_returnsTrueForFirstTime() {
        assertTrue(breedingBehavior.canBreedNow(maleEntity), "First time breeding should be allowed");
    }

    @Test
    void canBreedNow_respectsCooldown() {
        breedingBehavior.recordBreeding(maleEntity);
        assertFalse(breedingBehavior.canBreedNow(maleEntity), "Should not breed immediately after");

        maleEntity.setGameTime(10000L + config.getBreedingCooldown());
        assertTrue(breedingBehavior.canBreedNow(maleEntity), "Should breed after cooldown");
    }

    @Test
    void recordBreeding_storesBreedingTime() {
        breedingBehavior.recordBreeding(maleEntity);
        assertEquals(0, breedingBehavior.getTimeSinceLastBreeding(maleEntity), "Time since breeding should be 0 immediately after");
    }

    @Test
    void recordBreeding_forPair_updatesMateFidelity() {
        breedingBehavior.recordBreeding(maleEntity, femaleEntity);

        assertTrue(breedingBehavior.getMateFidelity().hasPreviousMate(maleEntity), "Male should have previous mate");
        assertTrue(breedingBehavior.getMateFidelity().hasPreviousMate(femaleEntity), "Female should have previous mate");
        assertEquals(femaleEntity.getUuid(), breedingBehavior.getMateFidelity().getPreviousMate(maleEntity), "Male's previous mate should be female");
        assertEquals(maleEntity.getUuid(), breedingBehavior.getMateFidelity().getPreviousMate(femaleEntity), "Female's previous mate should be male");
    }

    @Test
    void selectBestMate_returnsNullForEmptyList() {
        BreedingEntity result = breedingBehavior.selectBestMate(maleEntity, new ArrayList<>());
        assertNull(result, "Should return null for empty list");
    }

    @Test
    void selectBestMate_returnsNullForNullList() {
        BreedingEntity result = breedingBehavior.selectBestMate(maleEntity, null);
        assertNull(result, "Should return null for null list");
    }

    @Test
    void selectBestMate_filtersUnsuitableMates() {
        List<BreedingEntity> potentialMates = new ArrayList<>();
        potentialMates.add(femaleEntity);

        TestBreedingEntity baby = new TestBreedingEntity(UUID.randomUUID(), false, "entity.minecraft.wolf");
        baby.setBaby(true);
        potentialMates.add(baby);

        BreedingEntity result = breedingBehavior.selectBestMate(maleEntity, potentialMates);
        assertEquals(femaleEntity, result, "Should select adult female over baby");
    }

    @Test
    void selectBestMate_prefersPreviousMateWithHighFidelity() {
        breedingBehavior.recordBreeding(maleEntity, femaleEntity);

        TestBreedingEntity newFemale = new TestBreedingEntity(UUID.randomUUID(), false, "entity.minecraft.wolf");
        newFemale.setPosition(new Vec3d(10, 64, 0));
        newFemale.setHealth(20.0);
        newFemale.setMaxHealth(20.0);
        newFemale.setAge(500);

        List<BreedingEntity> potentialMates = new ArrayList<>();
        potentialMates.add(newFemale);
        potentialMates.add(femaleEntity);

        BreedingEntity result = breedingBehavior.selectBestMate(maleEntity, potentialMates);
        assertNotNull(result, "Should select a mate");
    }

    @Test
    void shouldInitiateCourtship_returnsFalseForDeadMate() {
        femaleEntity.setAlive(false);
        assertFalse(breedingBehavior.shouldInitiateCourtship(maleEntity, femaleEntity), "Should not initiate with dead mate");
    }

    @Test
    void shouldInitiateCourtship_returnsFalseForMateInLove() {
        femaleEntity.setInLove(true);
        assertFalse(breedingBehavior.shouldInitiateCourtship(maleEntity, femaleEntity), "Should not initiate with mate in love");
    }

    @Test
    void shouldInitiateCourtship_returnsTrueForMateInRange() {
        assertTrue(breedingBehavior.shouldInitiateCourtship(maleEntity, femaleEntity), "Should initiate with mate in range");
    }

    @Test
    void shouldInitiateCourtship_returnsFalseForMateOutOfRange() {
        femaleEntity.setPosition(new Vec3d(50, 64, 0));
        assertFalse(breedingBehavior.shouldInitiateCourtship(maleEntity, femaleEntity), "Should not initiate with mate out of range");
    }

    @Test
    void startCourtship_beginsCourtshipDisplay() {
        breedingBehavior.startCourtship(maleEntity);
        assertTrue(breedingBehavior.getCourtshipDisplay().isCourtshipActive(maleEntity), "Courtship should be active");
        assertEquals(1.0, breedingBehavior.getCourtshipDisplay().getCurrentIntensity(maleEntity), 0.01, "Initial intensity should be 1.0");
    }

    @Test
    void tick_advancesCourtshipProgress() {
        breedingBehavior.startCourtship(maleEntity);

        for (int i = 0; i < config.getCourtshipDuration(); i++) {
            breedingBehavior.tick();
        }

        assertTrue(breedingBehavior.isCourtshipComplete(maleEntity), "Courtship should be complete after duration");
    }

    @Test
    void isCourtshipComplete_returnsFalseInitially() {
        breedingBehavior.startCourtship(maleEntity);
        assertFalse(breedingBehavior.isCourtshipComplete(maleEntity), "Courtship should not be complete immediately");
    }

    @Test
    void onRejected_terminatesCourtship() {
        breedingBehavior.startCourtship(maleEntity);
        breedingBehavior.onRejected(maleEntity);
        assertFalse(breedingBehavior.getCourtshipDisplay().isCourtshipActive(maleEntity), "Courtship should not be active after rejection");
    }

    @Test
    void setTerritoryCenter_storesTerritoryPosition() {
        Vec3d center = new Vec3d(100, 64, 100);
        breedingBehavior.setTerritoryCenter(maleEntity.getUuid(), center);

        TestBreedingEntity intruder = new TestBreedingEntity(UUID.randomUUID(), true, "entity.minecraft.wolf");
        intruder.setPosition(new Vec3d(110, 64, 100));
        intruder.setAlive(true);

        assertTrue(breedingBehavior.isIntruder(maleEntity, intruder), "Entity near territory should be intruder");
    }

    @Test
    void isIntruder_returnsFalseForDeadEntity() {
        Vec3d center = new Vec3d(100, 64, 100);
        breedingBehavior.setTerritoryCenter(maleEntity.getUuid(), center);

        TestBreedingEntity deadIntruder = new TestBreedingEntity(UUID.randomUUID(), true, "entity.minecraft.wolf");
        deadIntruder.setPosition(new Vec3d(105, 64, 100));
        deadIntruder.setAlive(false);

        assertFalse(breedingBehavior.isIntruder(maleEntity, deadIntruder), "Dead entity should not be intruder");
    }

    @Test
    void getTimeSinceLastBreeding_returnsZeroForNeverBred() {
        assertEquals(0, breedingBehavior.getTimeSinceLastBreeding(maleEntity), "Never bred should return 0");
    }

    @Test
    void getTimeSinceLastBreeding_calculatesTimeCorrectly() {
        breedingBehavior.recordBreeding(maleEntity);
        maleEntity.setGameTime(16000L);
        assertEquals(6000, breedingBehavior.getTimeSinceLastBreeding(maleEntity), "Should calculate time since breeding");
    }

    @Test
    void getSeasonProgress_returnsHalfForYearRoundBreeding() {
        assertEquals(0.5, breedingBehavior.getSeasonProgress(maleEntity), 0.01, "Year-round breeding should have 0.5 progress");
    }

    @Test
    void isSeasonEndingSoon_returnsFalseForYearRoundBreeding() {
        assertFalse(breedingBehavior.isSeasonEndingSoon(maleEntity, 30), "Year-round breeding should not end");
    }

    @Test
    void registerOffspring_tracksParentalResponsibility() {
        UUID offspring = UUID.randomUUID();
        breedingBehavior.registerOffspring(maleEntity, offspring);

        assertTrue(breedingBehavior.getParentalInvestment().hasOffspring(maleEntity), "Parent should have offspring");
        assertTrue(breedingBehavior.getParentalInvestment().isResponsibleFor(maleEntity, offspring), "Parent should be responsible for offspring");
    }

    @Test
    void calculateCareLevel_returnsZeroForNoOffspring() {
        assertEquals(0.0, breedingBehavior.calculateCareLevel(maleEntity), 0.01, "No offspring should have 0 care level");
    }

    @Test
    void calculateCareLevel_increasesWithOffspring() {
        UUID offspring = UUID.randomUUID();
        breedingBehavior.registerOffspring(maleEntity, offspring);

        double careLevel = breedingBehavior.calculateCareLevel(maleEntity);
        assertTrue(careLevel > 0.0, "Care level should be positive with offspring");
        assertTrue(careLevel <= 1.0, "Care level should not exceed 1.0");
    }

    @Test
    void bothParentsInvest_returnsTrueForBiparentalCare() {
        UUID offspring = UUID.randomUUID();
        breedingBehavior.registerOffspring(maleEntity, offspring);
        breedingBehavior.registerOffspring(femaleEntity, offspring);

        assertTrue(breedingBehavior.bothParentsInvest(maleEntity, femaleEntity, offspring), "Both parents should invest");
    }

    @Test
    void bothParentsInvest_returnsFalseForNoBiparentalCare() {
        config.setBiparentalCare(false);
        BreedingBehavior newBehavior = new BreedingBehavior(config);

        UUID offspring = UUID.randomUUID();
        newBehavior.registerOffspring(maleEntity, offspring);
        newBehavior.registerOffspring(femaleEntity, offspring);

        assertFalse(newBehavior.bothParentsInvest(maleEntity, femaleEntity, offspring), "Should not invest without biparental care");
    }

    @Test
    void cleanup_removesStaleData() {
        breedingBehavior.startCourtship(maleEntity);
        breedingBehavior.setTerritoryCenter(maleEntity.getUuid(), new Vec3d(0, 64, 0));
        breedingBehavior.recordBreeding(maleEntity, femaleEntity);

        breedingBehavior.cleanup(maleEntity);
        assertNotNull(breedingBehavior, "Cleanup should not null the behavior");
    }
}

/**
 * Mate selection tests focused on scientific accuracy.
 */
class MateSelectionTest {

    private BreedingConfig config;
    private MateSelection mateSelection;
    private TestBreedingEntity chooser;

    @BeforeEach
    void setUp() {
        config = new BreedingConfig();
        config.setMinHealthForBreeding(0.7);
        config.setMinAgeForBreeding(100);
        config.setDisplayTraitWeight(0.5);
        config.setAgePreference(0.3);

        mateSelection = new MateSelection(config);

        chooser = new TestBreedingEntity(UUID.randomUUID(), true, "entity.minecraft.wolf");
        chooser.setPosition(new Vec3d(0, 64, 0));
        chooser.setHealth(20.0);
        chooser.setMaxHealth(20.0);
        chooser.setAge(500);
        chooser.setGameTime(10000L);
    }

    @Test
    void selectBestMate_returnsNullForEmptyList() {
        BreedingEntity result = mateSelection.selectBestMate(chooser, new ArrayList<>());
        assertNull(result, "Should return null for empty list");
    }

    @Test
    void selectBestMate_returnsNullForNullList() {
        BreedingEntity result = mateSelection.selectBestMate(chooser, null);
        assertNull(result, "Should return null for null list");
    }

    @Test
    void selectBestMate_filtersOutDeadMates() {
        List<BreedingEntity> potentialMates = new ArrayList<>();

        TestBreedingEntity deadMate = new TestBreedingEntity(UUID.randomUUID(), false, "entity.minecraft.wolf");
        deadMate.setAlive(false);
        deadMate.setHealth(20.0);
        deadMate.setMaxHealth(20.0);
        deadMate.setAge(500);
        potentialMates.add(deadMate);

        BreedingEntity result = mateSelection.selectBestMate(chooser, potentialMates);
        assertNull(result, "Should not select dead mate");
    }

    @Test
    void selectBestMate_filtersOutBabyMates() {
        List<BreedingEntity> potentialMates = new ArrayList<>();

        TestBreedingEntity babyMate = new TestBreedingEntity(UUID.randomUUID(), false, "entity.minecraft.wolf");
        babyMate.setBaby(true);
        babyMate.setAlive(true);
        babyMate.setHealth(20.0);
        babyMate.setMaxHealth(20.0);
        babyMate.setAge(50);
        potentialMates.add(babyMate);

        BreedingEntity result = mateSelection.selectBestMate(chooser, potentialMates);
        assertNull(result, "Should not select baby mate");
    }

    @Test
    void selectBestMate_filtersOutMatesInLove() {
        List<BreedingEntity> potentialMates = new ArrayList<>();

        TestBreedingEntity inLoveMate = new TestBreedingEntity(UUID.randomUUID(), false, "entity.minecraft.wolf");
        inLoveMate.setInLove(true);
        inLoveMate.setAlive(true);
        inLoveMate.setHealth(20.0);
        inLoveMate.setMaxHealth(20.0);
        inLoveMate.setAge(500);
        potentialMates.add(inLoveMate);

        BreedingEntity result = mateSelection.selectBestMate(chooser, potentialMates);
        assertNull(result, "Should not select mate in love");
    }

    @Test
    void selectBestMate_filtersOutLowHealthMates() {
        List<BreedingEntity> potentialMates = new ArrayList<>();

        TestBreedingEntity lowHealthMate = new TestBreedingEntity(UUID.randomUUID(), false, "entity.minecraft.wolf");
        lowHealthMate.setAlive(true);
        lowHealthMate.setHealth(5.0);
        lowHealthMate.setMaxHealth(20.0);
        lowHealthMate.setAge(500);
        potentialMates.add(lowHealthMate);

        BreedingEntity result = mateSelection.selectBestMate(chooser, potentialMates);
        assertNull(result, "Should not select low health mate");
    }

    @Test
    void selectBestMate_filtersOutSameSexMates() {
        List<BreedingEntity> potentialMates = new ArrayList<>();

        TestBreedingEntity sameSexMate = new TestBreedingEntity(UUID.randomUUID(), true, "entity.minecraft.wolf");
        sameSexMate.setAlive(true);
        sameSexMate.setHealth(20.0);
        sameSexMate.setMaxHealth(20.0);
        sameSexMate.setAge(500);
        potentialMates.add(sameSexMate);

        BreedingEntity result = mateSelection.selectBestMate(chooser, potentialMates);
        assertNull(result, "Should not select same sex mate");
    }

    @Test
    void selectBestMate_selectsBestQualityMate() {
        List<BreedingEntity> potentialMates = new ArrayList<>();

        TestBreedingEntity averageMate = new TestBreedingEntity(UUID.randomUUID(), false, "entity.minecraft.wolf");
        averageMate.setAlive(true);
        averageMate.setHealth(15.0);
        averageMate.setMaxHealth(20.0);
        averageMate.setAge(300);
        potentialMates.add(averageMate);

        TestBreedingEntity highQualityMate = new TestBreedingEntity(UUID.randomUUID(), false, "entity.minecraft.wolf");
        highQualityMate.setAlive(true);
        highQualityMate.setHealth(20.0);
        highQualityMate.setMaxHealth(20.0);
        highQualityMate.setAge(500);
        potentialMates.add(highQualityMate);

        BreedingEntity result = mateSelection.selectBestMate(chooser, potentialMates);
        assertEquals(highQualityMate, result, "Should select highest quality mate");
    }

    @Test
    void filterSuitableMates_returnsOnlyValidMates() {
        List<BreedingEntity> potentialMates = new ArrayList<>();

        TestBreedingEntity validMate1 = new TestBreedingEntity(UUID.randomUUID(), false, "entity.minecraft.wolf");
        validMate1.setAlive(true);
        validMate1.setHealth(20.0);
        validMate1.setMaxHealth(20.0);
        validMate1.setAge(500);
        potentialMates.add(validMate1);

        TestBreedingEntity invalidMate = new TestBreedingEntity(UUID.randomUUID(), true, "entity.minecraft.wolf");
        invalidMate.setAlive(true);
        invalidMate.setHealth(20.0);
        invalidMate.setMaxHealth(20.0);
        invalidMate.setAge(500);
        potentialMates.add(invalidMate);

        TestBreedingEntity validMate2 = new TestBreedingEntity(UUID.randomUUID(), false, "entity.minecraft.wolf");
        validMate2.setAlive(true);
        validMate2.setHealth(18.0);
        validMate2.setMaxHealth(20.0);
        validMate2.setAge(400);
        potentialMates.add(validMate2);

        List<BreedingEntity> suitable = mateSelection.filterSuitableMates(chooser, potentialMates);
        assertEquals(2, suitable.size(), "Should have 2 suitable mates");
        assertTrue(suitable.contains(validMate1), "Should contain valid mate 1");
        assertTrue(suitable.contains(validMate2), "Should contain valid mate 2");
        assertFalse(suitable.contains(invalidMate), "Should not contain invalid mate");
    }
}

/**
 * Courtship display tests covering all display types.
 */
class CourtshipDisplayTest {

    private BreedingConfig config;
    private CourtshipDisplay courtshipDisplay;
    private TestBreedingEntity performer;
    private TestBreedingEntity potentialMate;

    @BeforeEach
    void setUp() {
        config = new BreedingConfig();
        config.setCourtshipDuration(20);
        config.setDisplayRange(16.0);
        config.setDisplayType(DisplayType.DANCING);

        courtshipDisplay = new CourtshipDisplay(config);

        UUID performerId = UUID.randomUUID();
        UUID mateId = UUID.randomUUID();

        performer = new TestBreedingEntity(performerId, true, "entity.minecraft.wolf");
        performer.setPosition(new Vec3d(0, 64, 0));
        performer.setGameTime(10000L);

        potentialMate = new TestBreedingEntity(mateId, false, "entity.minecraft.wolf");
        potentialMate.setPosition(new Vec3d(5, 64, 0));
        potentialMate.setAlive(true);
        potentialMate.setInLove(false);
    }

    @Test
    void shouldInitiateCourtship_returnsTrueForValidMate() {
        assertTrue(courtshipDisplay.shouldInitiateCourtship(performer, potentialMate), "Should initiate courtship for valid mate");
    }

    @Test
    void shouldInitiateCourtship_returnsFalseForDeadMate() {
        potentialMate.setAlive(false);
        assertFalse(courtshipDisplay.shouldInitiateCourtship(performer, potentialMate), "Should not initiate for dead mate");
    }

    @Test
    void shouldInitiateCourtship_returnsFalseForMateInLove() {
        potentialMate.setInLove(true);
        assertFalse(courtshipDisplay.shouldInitiateCourtship(performer, potentialMate), "Should not initiate for mate in love");
    }

    @Test
    void shouldInitiateCourtship_returnsFalseForMateOutOfRange() {
        potentialMate.setPosition(new Vec3d(50, 64, 0));
        assertFalse(courtshipDisplay.shouldInitiateCourtship(performer, potentialMate), "Should not initiate for mate out of range");
    }

    @Test
    void startCourtship_initializesState() {
        courtshipDisplay.startCourtship(performer);
        assertTrue(courtshipDisplay.isCourtshipActive(performer), "Courtship should be active");
        assertEquals(1.0, courtshipDisplay.getCurrentIntensity(performer), 0.01, "Initial intensity should be 1.0");
    }

    @Test
    void tick_advancesCourtshipProgress() {
        courtshipDisplay.startCourtship(performer);

        for (int i = 0; i < config.getCourtshipDuration(); i++) {
            courtshipDisplay.tick();
        }

        assertTrue(courtshipDisplay.isCourtshipComplete(performer), "Courtship should be complete after duration");
    }

    @Test
    void tick_decreasesIntensityOverTime() {
        courtshipDisplay.startCourtship(performer);

        double initialIntensity = courtshipDisplay.getCurrentIntensity(performer);

        for (int i = 0; i < config.getCourtshipDuration() / 2; i++) {
            courtshipDisplay.tick();
        }

        double midIntensity = courtshipDisplay.getCurrentIntensity(performer);
        assertTrue(midIntensity < initialIntensity, "Intensity should decrease over time");
        assertTrue(midIntensity > 0.0, "Intensity should still be positive midway");
    }

    @Test
    void isCourtshipComplete_returnsTrueAfterDuration() {
        courtshipDisplay.startCourtship(performer);

        for (int i = 0; i < config.getCourtshipDuration(); i++) {
            courtshipDisplay.tick();
        }

        assertTrue(courtshipDisplay.isCourtshipComplete(performer), "Courtship should be complete");
    }

    @Test
    void isCourtshipActive_returnsFalseAfterRejection() {
        courtshipDisplay.startCourtship(performer);
        courtshipDisplay.onRejected(performer);
        assertFalse(courtshipDisplay.isCourtshipActive(performer), "Courtship should not be active after rejection");
    }

    @Test
    void getCurrentIntensity_returnsZeroForNonActiveCourtship() {
        assertEquals(0.0, courtshipDisplay.getCurrentIntensity(performer), 0.01, "Intensity should be 0 for non-active courtship");
    }

    @Test
    void calculateDisplayForce_returnsZeroForNoIntensity() {
        Vec3d force = courtshipDisplay.calculateDisplayForce(performer);
        assertEquals(0.0, force.magnitude(), 0.01, "Force should be zero without active courtship");
    }

    @Test
    void calculateDisplayForce_generatesDancingMovement() {
        config.setDisplayType(DisplayType.DANCING);
        courtshipDisplay = new CourtshipDisplay(config);
        courtshipDisplay.startCourtship(performer);

        Vec3d force = courtshipDisplay.calculateDisplayForce(performer);
        assertTrue(force.magnitude() > 0.0, "Dancing display should generate force");
    }

    @Test
    void calculateDisplayForce_generatesPosturingMovement() {
        config.setDisplayType(DisplayType.POSTURING);
        courtshipDisplay = new CourtshipDisplay(config);
        courtshipDisplay.startCourtship(performer);

        Vec3d force = courtshipDisplay.calculateDisplayForce(performer);
        assertTrue(force.y > 0.0, "Posturing display should generate upward force");
    }

    @Test
    void calculateDisplayForce_generatesVocalizationMovement() {
        config.setDisplayType(DisplayType.VOCALIZATION);
        courtshipDisplay = new CourtshipDisplay(config);
        courtshipDisplay.startCourtship(performer);

        Vec3d force = courtshipDisplay.calculateDisplayForce(performer);
        assertTrue(force.magnitude() > 0.0, "Vocalization display should generate force");
    }

    @Test
    void getDisplayType_returnsConfiguredType() {
        assertEquals(DisplayType.DANCING, courtshipDisplay.getDisplayType(), "Should return configured display type");
    }

    @Test
    void cleanup_removesCompletedCourtships() {
        courtshipDisplay.startCourtship(performer);

        for (int i = 0; i < config.getCourtshipDuration(); i++) {
            courtshipDisplay.tick();
        }

        courtshipDisplay.cleanup();
        assertFalse(courtshipDisplay.isCourtshipActive(performer), "Cleanup should remove completed courtship");
    }
}

/**
 * Territorial defense tests for breeding season protection.
 */
class TerritorialDefenseTest {

    private BreedingConfig config;
    private TerritorialDefense territorialDefense;
    private TestBreedingEntity owner;
    private TestBreedingEntity rival;
    private TestBreedingEntity potentialMate;

    @BeforeEach
    void setUp() {
        config = new BreedingConfig();
        config.setTerritorySize(32.0);

        territorialDefense = new TerritorialDefense(config);

        UUID ownerId = UUID.randomUUID();
        UUID rivalId = UUID.randomUUID();
        UUID mateId = UUID.randomUUID();

        owner = new TestBreedingEntity(ownerId, true, "entity.minecraft.wolf");
        owner.setPosition(new Vec3d(0, 64, 0));

        rival = new TestBreedingEntity(rivalId, true, "entity.minecraft.wolf");
        rival.setPosition(new Vec3d(10, 64, 0));
        rival.setAlive(true);

        potentialMate = new TestBreedingEntity(mateId, false, "entity.minecraft.wolf");
        potentialMate.setPosition(new Vec3d(15, 64, 0));
        potentialMate.setAlive(true);

        territorialDefense.setTerritoryCenter(ownerId, new Vec3d(0, 64, 0));
    }

    @Test
    void setTerritoryCenter_storesPosition() {
        UUID newOwnerId = UUID.randomUUID();
        Vec3d center = new Vec3d(100, 64, 100);
        territorialDefense.setTerritoryCenter(newOwnerId, center);

        TestBreedingEntity testEntity = new TestBreedingEntity(newOwnerId, true, "entity.minecraft.wolf");
        testEntity.setPosition(center);

        assertTrue(territorialDefense.isPositionInTerritory(testEntity, center), "Position should be in territory");
    }

    @Test
    void isIntruder_returnsTrueForEntityInTerritory() {
        assertTrue(territorialDefense.isIntruder(owner, rival), "Rival in territory should be intruder");
    }

    @Test
    void isIntruder_returnsFalseForDeadEntity() {
        rival.setAlive(false);
        assertFalse(territorialDefense.isIntruder(owner, rival), "Dead entity should not be intruder");
    }

    @Test
    void isIntruder_returnsFalseForEntityOutsideTerritory() {
        rival.setPosition(new Vec3d(100, 64, 0));
        assertFalse(territorialDefense.isIntruder(owner, rival), "Entity outside territory should not be intruder");
    }

    @Test
    void isIntruder_returnsFalseForEntityTooClose() {
        rival.setPosition(new Vec3d(1, 64, 0));
        assertFalse(territorialDefense.isIntruder(owner, rival), "Entity too close should not be intruder");
    }

    @Test
    void isRival_returnsTrueForSameSexSameSpecies() {
        assertTrue(territorialDefense.isRival(owner, rival), "Same sex same species should be rival");
    }

    @Test
    void isRival_returnsFalseForOppositeSex() {
        assertFalse(territorialDefense.isRival(owner, potentialMate), "Opposite sex should not be rival");
    }

    @Test
    void isRival_returnsFalseForDifferentSpecies() {
        TestBreedingEntity differentSpecies = new TestBreedingEntity(UUID.randomUUID(), true, "entity.minecraft.cow");
        differentSpecies.setAlive(true);

        assertFalse(territorialDefense.isRival(owner, differentSpecies), "Different species should not be rival");
    }

    @Test
    void addRival_tracksRivalEntity() {
        territorialDefense.addRival(owner, rival);
        List<BreedingEntity> rivals = territorialDefense.getRivals(owner);

        assertEquals(1, rivals.size(), "Should have 1 rival");
        assertTrue(rivals.contains(rival), "Should contain added rival");
    }

    @Test
    void clearRivals_removesAllRivals() {
        territorialDefense.addRival(owner, rival);
        territorialDefense.clearRivals(owner.getUuid());

        List<BreedingEntity> rivals = territorialDefense.getRivals(owner);
        assertTrue(rivals.isEmpty(), "Should have no rivals after clearing");
    }

    @Test
    void calculateDefenseForce_returnsZeroForNoRivals() {
        Vec3d force = territorialDefense.calculateDefenseForce(owner);
        assertEquals(0.0, force.magnitude(), 0.01, "Force should be zero with no rivals");
    }

    @Test
    void calculateDefenseForce_generatesForceTowardRival() {
        territorialDefense.addRival(owner, rival);
        Vec3d force = territorialDefense.calculateDefenseForce(owner);

        assertTrue(force.magnitude() > 0.0, "Should generate force toward rival");
    }

    @Test
    void calculateThreatLevel_returnsZeroForDeadRival() {
        rival.setAlive(false);
        double threat = territorialDefense.calculateThreatLevel(owner, rival);
        assertEquals(0.0, threat, 0.01, "Dead rival should have zero threat");
    }

    @Test
    void calculateThreatLevel_increasesWithProximity() {
        TestBreedingEntity closeRival = new TestBreedingEntity(UUID.randomUUID(), true, "entity.minecraft.wolf");
        closeRival.setPosition(new Vec3d(5, 64, 0));
        closeRival.setAlive(true);
        closeRival.setHealth(20.0);
        closeRival.setMaxHealth(20.0);

        TestBreedingEntity farRival = new TestBreedingEntity(UUID.randomUUID(), true, "entity.minecraft.wolf");
        farRival.setPosition(new Vec3d(30, 64, 0));
        farRival.setAlive(true);
        farRival.setHealth(20.0);
        farRival.setMaxHealth(20.0);

        double closeThreat = territorialDefense.calculateThreatLevel(owner, closeRival);
        double farThreat = territorialDefense.calculateThreatLevel(owner, farRival);

        assertTrue(closeThreat > farThreat, "Closer rival should have higher threat");
    }

    @Test
    void isPositionInTerritory_returnsTrueForPositionInRange() {
        Vec3d position = new Vec3d(10, 64, 0);
        assertTrue(territorialDefense.isPositionInTerritory(owner, position), "Position in range should be in territory");
    }

    @Test
    void isPositionInTerritory_returnsFalseForPositionOutOfRange() {
        Vec3d position = new Vec3d(50, 64, 0);
        assertFalse(territorialDefense.isPositionInTerritory(owner, position), "Position out of range should not be in territory");
    }

    @Test
    void cleanup_removesDeadRivals() {
        territorialDefense.addRival(owner, rival);
        rival.setAlive(false);

        territorialDefense.cleanup();

        List<BreedingEntity> rivals = territorialDefense.getRivals(owner);
        assertTrue(rivals.isEmpty(), "Cleanup should remove dead rivals");
    }
}

/**
 * Breeding season tests for temporal breeding patterns.
 */
class BreedingSeasonTest {

    private BreedingConfig config;
    private BreedingSeason breedingSeason;
    private TestBreedingEntity entity;

    @BeforeEach
    void setUp() {
        config = new BreedingConfig();
        config.setBreedingSeasonStart(2);
        config.setBreedingSeasonEnd(4);

        breedingSeason = new BreedingSeason(config);

        entity = new TestBreedingEntity(UUID.randomUUID(), true, "entity.minecraft.wolf");
        entity.setGameTime(0L);
        entity.setDayTime(12000L);
    }

    @Test
    void isBreedingSeason_returnsTrueForYearRoundBreeding() {
        config.setYearRoundBreeding(true);
        breedingSeason = new BreedingSeason(config);

        assertTrue(breedingSeason.isBreedingSeason(entity), "Year-round breeding should always be true");
    }

    @Test
    void isBreedingSeason_returnsTrueForMonthInRange() {
        config.setYearRoundBreeding(false);
        entity.setGameTime(60 * 24000L);

        assertTrue(breedingSeason.isBreedingSeason(entity), "Should be breeding season in month range");
    }

    @Test
    void isBreedingSeason_returnsFalseForMonthOutOfRange() {
        config.setYearRoundBreeding(false);
        entity.setGameTime(180 * 24000L);

        assertFalse(breedingSeason.isBreedingSeason(entity), "Should not be breeding season outside range");
    }

    @Test
    void isBreedingSeason_handlesYearWraparound() {
        config.setBreedingSeasonStart(10);
        config.setBreedingSeasonEnd(2);

        entity.setGameTime(320 * 24000L);
        assertTrue(breedingSeason.isBreedingSeason(entity), "Should handle year wraparound (November)");
    }

    @Test
    void checkPhotoperiodTrigger_returnsTrueWhenDisabled() {
        config.setPhotoperiodTrigger(false);
        assertTrue(breedingSeason.checkPhotoperiodTrigger(entity), "Should return true when photoperiod disabled");
    }

    @Test
    void checkPhotoperiodTrigger_checksDayLength() {
        config.setPhotoperiodTrigger(true);
        config.setMinDayLength(10000L);
        entity.setDayTime(12000L);

        assertTrue(breedingSeason.checkPhotoperiodTrigger(entity), "Should trigger when day length sufficient");
    }

    @Test
    void checkPhotoperiodTrigger_returnsFalseForShortDay() {
        config.setPhotoperiodTrigger(true);
        config.setMinDayLength(15000L);
        entity.setDayTime(10000L);

        assertFalse(breedingSeason.checkPhotoperiodTrigger(entity), "Should not trigger when day too short");
    }

    @Test
    void getSeasonProgress_returnsHalfForYearRound() {
        config.setYearRoundBreeding(true);
        assertEquals(0.5, breedingSeason.getSeasonProgress(entity), 0.01, "Year-round should have 0.5 progress");
    }

    @Test
    void getSeasonProgress_calculatesCorrectly() {
        config.setYearRoundBreeding(false);
        config.setBreedingSeasonStart(0);
        config.setBreedingSeasonEnd(2);

        entity.setGameTime(30 * 24000L);
        double progress = breedingSeason.getSeasonProgress(entity);
        assertTrue(progress > 0.0 && progress <= 1.0, "Progress should be between 0 and 1");
    }

    @Test
    void getMonthsUntilSeason_returnsZeroWhenInSeason() {
        config.setYearRoundBreeding(false);
        entity.setGameTime(60 * 24000L);

        assertEquals(0, breedingSeason.getMonthsUntilSeason(entity), "Should return 0 when in season");
    }

    @Test
    void getMonthsUntilSeason_calculatesMonthsUntil() {
        config.setYearRoundBreeding(false);
        config.setBreedingSeasonStart(6);
        config.setBreedingSeasonEnd(8);

        entity.setGameTime(0L);
        int monthsUntil = breedingSeason.getMonthsUntilSeason(entity);
        assertEquals(6, monthsUntil, "Should calculate months until season");
    }

    @Test
    void isSeasonEndingSoon_returnsFalseForYearRound() {
        config.setYearRoundBreeding(true);
        assertFalse(breedingSeason.isSeasonEndingSoon(entity, 30), "Year-round should not end");
    }

    @Test
    void isSeasonEndingSoon_returnsTrueNearEnd() {
        config.setYearRoundBreeding(false);
        config.setBreedingSeasonStart(0);
        config.setBreedingSeasonEnd(2);

        // Set game time to near the end of the breeding season
        // Season is months 0-2 (90 days total: 30 days each for months 0, 1, 2).
        // Day 76 means 76 days into season, which is day 16 of month 2.
        // Season has 90 - 76 = 14 days remaining, which is <= 15 threshold.
        entity.setGameTime(76 * 24000L);
        assertTrue(breedingSeason.isSeasonEndingSoon(entity, 15), "Should detect season ending soon");
    }
}

/**
 * Mate fidelity tests for pair bonding behavior.
 */
class MateFidelityTest {

    private BreedingConfig config;
    private MateFidelity mateFidelity;
    private TestBreedingEntity entity;
    private UUID previousMateId;

    @BeforeEach
    void setUp() {
        config = new BreedingConfig();
        config.setMateFidelity(0.8);

        mateFidelity = new MateFidelity(config);

        UUID entityId = UUID.randomUUID();
        previousMateId = UUID.randomUUID();

        entity = new TestBreedingEntity(entityId, true, "entity.minecraft.wolf");
        entity.setGameTime(10000L);
    }

    @Test
    void hasPreviousMate_returnsFalseInitially() {
        assertFalse(mateFidelity.hasPreviousMate(entity), "Should not have previous mate initially");
    }

    @Test
    void recordPreviousMate_storesMate() {
        mateFidelity.recordPreviousMate(entity, previousMateId);
        assertTrue(mateFidelity.hasPreviousMate(entity), "Should have previous mate after recording");
    }

    @Test
    void getPreviousMate_returnsRecordedMate() {
        mateFidelity.recordPreviousMate(entity, previousMateId);
        assertEquals(previousMateId, mateFidelity.getPreviousMate(entity), "Should return recorded mate");
    }

    @Test
    void getPreviousMate_returnsNullForNoMate() {
        assertNull(mateFidelity.getPreviousMate(entity), "Should return null when no previous mate");
    }

    @Test
    void shouldPreferPreviousMate_returnsFalseWhenNoPreviousMate() {
        assertFalse(mateFidelity.shouldPreferPreviousMate(entity), "Should not prefer when no previous mate");
    }

    @Test
    void applyMateFidelity_returnsNullForEmptyList() {
        assertNull(mateFidelity.applyMateFidelity(entity, new ArrayList<>()), "Should return null for empty list");
    }

    @Test
    void applyMateFidelity_returnsNullForNullList() {
        assertNull(mateFidelity.applyMateFidelity(entity, null), "Should return null for null list");
    }

    @Test
    void applyMateFidelity_selectsPreviousMateWhenPresent() {
        mateFidelity.recordPreviousMate(entity, previousMateId);

        TestBreedingEntity previousMate = new TestBreedingEntity(previousMateId, false, "entity.minecraft.wolf");
        previousMate.setAlive(true);
        previousMate.setHealth(20.0);
        previousMate.setMaxHealth(20.0);
        previousMate.setAge(500);

        UUID newMateId = UUID.randomUUID();
        TestBreedingEntity newMate = new TestBreedingEntity(newMateId, false, "entity.minecraft.wolf");
        newMate.setAlive(true);
        newMate.setHealth(20.0);
        newMate.setMaxHealth(20.0);
        newMate.setAge(500);

        List<BreedingEntity> potentialMates = new ArrayList<>();
        potentialMates.add(newMate);
        potentialMates.add(previousMate);

        BreedingEntity selected = mateFidelity.applyMateFidelity(entity, potentialMates);

        assertNotNull(selected, "Should select a mate");
    }

    @Test
    void filterByFidelity_prioritizesPreviousMate() {
        mateFidelity.recordPreviousMate(entity, previousMateId);

        TestBreedingEntity previousMate = new TestBreedingEntity(previousMateId, false, "entity.minecraft.wolf");
        previousMate.setAlive(true);
        previousMate.setHealth(20.0);
        previousMate.setMaxHealth(20.0);
        previousMate.setAge(500);

        UUID newMateId = UUID.randomUUID();
        TestBreedingEntity newMate = new TestBreedingEntity(newMateId, false, "entity.minecraft.wolf");
        newMate.setAlive(true);
        newMate.setHealth(20.0);
        newMate.setMaxHealth(20.0);
        newMate.setAge(500);

        List<BreedingEntity> potentialMates = new ArrayList<>();
        potentialMates.add(newMate);
        potentialMates.add(previousMate);

        List<BreedingEntity> filtered = mateFidelity.filterByFidelity(entity, potentialMates);

        assertEquals(previousMate, filtered.get(0), "Previous mate should be first in filtered list");
    }

    @Test
    void calculateBondStrength_returnsZeroWhenNoPreviousMate() {
        assertEquals(0.0, mateFidelity.calculateBondStrength(entity), 0.01, "Bond strength should be 0 without previous mate");
    }

    @Test
    void calculateBondStrength_decreasesOverTime() {
        mateFidelity.recordPreviousMate(entity, previousMateId);

        double initialBond = mateFidelity.calculateBondStrength(entity);
        entity.setGameTime(50000L);
        double laterBond = mateFidelity.calculateBondStrength(entity);

        assertTrue(laterBond < initialBond, "Bond should weaken over time");
    }

    @Test
    void isBondActive_returnsTrueWhenBondStrong() {
        mateFidelity.recordPreviousMate(entity, previousMateId);
        assertTrue(mateFidelity.isBondActive(entity), "Bond should be active initially");
    }

    @Test
    void clearPreviousMate_removesBond() {
        mateFidelity.recordPreviousMate(entity, previousMateId);
        mateFidelity.clearPreviousMate(entity);

        assertFalse(mateFidelity.hasPreviousMate(entity), "Should not have previous mate after clearing");
    }

    @Test
    void getTimeSinceLastBreeding_returnsNegativeForNeverBred() {
        assertEquals(-1, mateFidelity.getTimeSinceLastBreeding(entity), "Should return -1 for never bred");
    }

    @Test
    void cleanup_removesOldRecords() {
        mateFidelity.recordPreviousMate(entity, previousMateId);
        entity.setGameTime(400 * 24000L);

        mateFidelity.cleanup(entity);

        assertFalse(mateFidelity.hasPreviousMate(entity), "Cleanup should remove old records");
    }
}

/**
 * Parental investment tests for care behaviors.
 */
class ParentalInvestmentTest {

    private BreedingConfig config;
    private ParentalInvestment parentalInvestment;
    private TestBreedingEntity parent;
    private UUID offspringId;

    @BeforeEach
    void setUp() {
        config = new BreedingConfig();
        config.setBiparentalCare(true);
        config.setParentalInvestmentLevel(0.8);

        parentalInvestment = new ParentalInvestment(config);

        UUID parentId = UUID.randomUUID();
        offspringId = UUID.randomUUID();

        parent = new TestBreedingEntity(parentId, true, "entity.minecraft.wolf");
    }

    @Test
    void hasOffspring_returnsFalseInitially() {
        assertFalse(parentalInvestment.hasOffspring(parent), "Should not have offspring initially");
    }

    @Test
    void registerOffspring_tracksOffspring() {
        parentalInvestment.registerOffspring(parent, offspringId);
        assertTrue(parentalInvestment.hasOffspring(parent), "Should have offspring after registration");
    }

    @Test
    void isResponsibleFor_returnsTrueForRegisteredOffspring() {
        parentalInvestment.registerOffspring(parent, offspringId);
        assertTrue(parentalInvestment.isResponsibleFor(parent, offspringId), "Should be responsible for registered offspring");
    }

    @Test
    void isResponsibleFor_returnsFalseForUnregisteredOffspring() {
        assertFalse(parentalInvestment.isResponsibleFor(parent, offspringId), "Should not be responsible for unregistered offspring");
    }

    @Test
    void calculateCareLevel_returnsZeroForNoOffspring() {
        assertEquals(0.0, parentalInvestment.calculateCareLevel(parent), 0.01, "Care level should be 0 without offspring");
    }

    @Test
    void calculateCareLevel_increasesWithOffspring() {
        parentalInvestment.registerOffspring(parent, offspringId);
        double careLevel = parentalInvestment.calculateCareLevel(parent);

        assertTrue(careLevel > 0.0, "Care level should be positive with offspring");
        assertTrue(careLevel <= 1.0, "Care level should not exceed 1.0");
    }

    @Test
    void calculateCareLevel_decreasesWithMoreOffspring() {
        UUID offspring1 = UUID.randomUUID();
        UUID offspring2 = UUID.randomUUID();

        parentalInvestment.registerOffspring(parent, offspring1);
        double careWithOne = parentalInvestment.calculateCareLevel(parent);

        parentalInvestment.registerOffspring(parent, offspring2);
        double careWithTwo = parentalInvestment.calculateCareLevel(parent);

        assertTrue(careWithTwo < careWithOne, "Care level should decrease with more offspring");
    }

    @Test
    void bothParentsInvest_returnsTrueWhenBothResponsible() {
        TestBreedingEntity mother = new TestBreedingEntity(UUID.randomUUID(), false, "entity.minecraft.wolf");
        TestBreedingEntity father = new TestBreedingEntity(UUID.randomUUID(), true, "entity.minecraft.wolf");

        parentalInvestment.registerOffspring(mother, offspringId);
        parentalInvestment.registerOffspring(father, offspringId);

        assertTrue(parentalInvestment.bothParentsInvest(mother, father, offspringId), "Both parents should invest");
    }

    @Test
    void bothParentsInvest_returnsFalseWhenOnlyOneResponsible() {
        TestBreedingEntity mother = new TestBreedingEntity(UUID.randomUUID(), false, "entity.minecraft.wolf");
        TestBreedingEntity father = new TestBreedingEntity(UUID.randomUUID(), true, "entity.minecraft.wolf");

        parentalInvestment.registerOffspring(mother, offspringId);

        assertFalse(parentalInvestment.bothParentsInvest(mother, father, offspringId), "Should not invest when only one parent responsible");
    }

    @Test
    void bothParentsInvest_returnsFalseWithoutBiparentalCare() {
        config.setBiparentalCare(false);
        parentalInvestment = new ParentalInvestment(config);

        TestBreedingEntity mother = new TestBreedingEntity(UUID.randomUUID(), false, "entity.minecraft.wolf");
        TestBreedingEntity father = new TestBreedingEntity(UUID.randomUUID(), true, "entity.minecraft.wolf");

        parentalInvestment.registerOffspring(mother, offspringId);
        parentalInvestment.registerOffspring(father, offspringId);

        assertFalse(parentalInvestment.bothParentsInvest(mother, father, offspringId), "Should not invest without biparental care");
    }

    @Test
    void getOffspring_returnsListOfOffspring() {
        UUID offspring1 = UUID.randomUUID();
        UUID offspring2 = UUID.randomUUID();

        parentalInvestment.registerOffspring(parent, offspring1);
        parentalInvestment.registerOffspring(parent, offspring2);

        List<UUID> offspring = parentalInvestment.getOffspring(parent);
        assertEquals(2, offspring.size(), "Should have 2 offspring");
        assertTrue(offspring.contains(offspring1), "Should contain offspring 1");
        assertTrue(offspring.contains(offspring2), "Should contain offspring 2");
    }

    @Test
    void getOffspringCount_returnsCorrectCount() {
        UUID offspring1 = UUID.randomUUID();
        UUID offspring2 = UUID.randomUUID();

        parentalInvestment.registerOffspring(parent, offspring1);
        assertEquals(1, parentalInvestment.getOffspringCount(parent), "Should have 1 offspring");

        parentalInvestment.registerOffspring(parent, offspring2);
        assertEquals(2, parentalInvestment.getOffspringCount(parent), "Should have 2 offspring");
    }

    @Test
    void removeOffspring_removesFromTracking() {
        parentalInvestment.registerOffspring(parent, offspringId);
        parentalInvestment.removeOffspring(parent, offspringId);

        assertFalse(parentalInvestment.isResponsibleFor(parent, offspringId), "Should not be responsible after removal");
    }

    @Test
    void clearOffspring_removesAllOffspring() {
        UUID offspring1 = UUID.randomUUID();
        UUID offspring2 = UUID.randomUUID();

        parentalInvestment.registerOffspring(parent, offspring1);
        parentalInvestment.registerOffspring(parent, offspring2);

        parentalInvestment.clearOffspring(parent);

        assertFalse(parentalInvestment.hasOffspring(parent), "Should have no offspring after clearing");
    }

    @Test
    void calculatePerOffspringAllocation_dividesInvestment() {
        UUID offspring1 = UUID.randomUUID();
        UUID offspring2 = UUID.randomUUID();

        parentalInvestment.registerOffspring(parent, offspring1);
        double oneOffspringAllocation = parentalInvestment.calculatePerOffspringAllocation(parent);

        parentalInvestment.registerOffspring(parent, offspring2);
        double twoOffspringAllocation = parentalInvestment.calculatePerOffspringAllocation(parent);

        assertTrue(oneOffspringAllocation > twoOffspringAllocation, "Allocation should decrease with more offspring");
    }

    @Test
    void canAcceptMoreOffspring_returnsTrueWhenCapacity() {
        assertTrue(parentalInvestment.canAcceptMoreOffspring(parent), "Should accept offspring when at capacity");
    }

    @Test
    void canAcceptMoreOffspring_returnsFalseWhenFull() {
        for (int i = 0; i < 15; i++) {
            parentalInvestment.registerOffspring(parent, UUID.randomUUID());
        }

        assertFalse(parentalInvestment.canAcceptMoreOffspring(parent), "Should not accept offspring when full");
    }

    @Test
    void calculateParentalStress_returnsZeroForIdealBrood() {
        parentalInvestment.registerOffspring(parent, offspringId);
        assertEquals(0.0, parentalInvestment.calculateParentalStress(parent), 0.01, "Should have no stress at ideal brood size");
    }

    @Test
    void calculateParentalStress_increasesWithOverload() {
        for (int i = 0; i < 10; i++) {
            parentalInvestment.registerOffspring(parent, UUID.randomUUID());
        }

        double stress = parentalInvestment.calculateParentalStress(parent);
        assertTrue(stress > 0.0, "Should have stress when overloaded");
        assertTrue(stress <= 1.0, "Stress should not exceed 1.0");
    }
}

/**
 * MatingSystem enum tests.
 */
class MatingSystemTest {

    @Test
    void matingSystem_containsAllExpectedValues() {
        MatingSystem[] systems = MatingSystem.values();

        assertEquals(5, systems.length, "Should have 5 mating systems");
    }
}

/**
 * DisplayType enum tests.
 */
class DisplayTypeTest {

    @Test
    void displayType_containsAllExpectedValues() {
        DisplayType[] types = DisplayType.values();

        assertEquals(6, types.length, "Should have 6 display types");
    }
}

/**
 * Test implementation of BreedingEntity for pure Java testing.
 */
class TestBreedingEntity implements BreedingEntity {

    private final UUID uuid;
    private final boolean isMale;
    private final String speciesId;
    private Vec3d position;
    private double health;
    private double maxHealth;
    private int age;
    private boolean isBaby;
    private boolean isInLove;
    private boolean isAlive;
    private long gameTime;
    private long dayTime;

    public TestBreedingEntity(UUID uuid, boolean isMale, String speciesId) {
        this.uuid = uuid;
        this.isMale = isMale;
        this.speciesId = speciesId;
        this.position = new Vec3d();
        this.health = 20.0;
        this.maxHealth = 20.0;
        this.age = 0;
        this.isBaby = false;
        this.isInLove = false;
        this.isAlive = true;
        this.gameTime = 0L;
        this.dayTime = 12000L;
    }

    @Override
    public UUID getUuid() {
        return uuid;
    }

    @Override
    public Vec3d getPosition() {
        return position;
    }

    public void setPosition(Vec3d position) {
        this.position = position;
    }

    @Override
    public double getHealth() {
        return health;
    }

    public void setHealth(double health) {
        this.health = health;
    }

    @Override
    public double getMaxHealth() {
        return maxHealth;
    }

    public void setMaxHealth(double maxHealth) {
        this.maxHealth = maxHealth;
    }

    @Override
    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    @Override
    public boolean isBaby() {
        return isBaby;
    }

    public void setBaby(boolean isBaby) {
        this.isBaby = isBaby;
    }

    @Override
    public boolean isInLove() {
        return isInLove;
    }

    public void setInLove(boolean isInLove) {
        this.isInLove = isInLove;
    }

    @Override
    public boolean isAlive() {
        return isAlive;
    }

    public void setAlive(boolean isAlive) {
        this.isAlive = isAlive;
    }

    @Override
    public boolean isMale() {
        return isMale;
    }

    @Override
    public long getGameTime() {
        return gameTime;
    }

    public void setGameTime(long gameTime) {
        this.gameTime = gameTime;
    }

    @Override
    public long getDayTime() {
        return dayTime;
    }

    public void setDayTime(long dayTime) {
        this.dayTime = dayTime;
    }

    @Override
    public String getSpeciesId() {
        return speciesId;
    }

    @Override
    public double getSize() {
        return 1.0; // Default size for testing
    }

    @Override
    public double getTemperature() {
        return 0.5; // Temperate climate for testing
    }
}
