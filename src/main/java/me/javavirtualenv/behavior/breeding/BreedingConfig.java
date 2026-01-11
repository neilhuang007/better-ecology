package me.javavirtualenv.behavior.breeding;

import java.util.HashMap;
import java.util.Map;

/**
 * Configuration for breeding and courtship behaviors.
 * Provides species-specific settings for mate selection, courtship displays,
 * territorial defense, breeding seasons, and parental investment.
 *
 * Research parameters from animal behavior studies:
 * - Territory sizes: 16-64 blocks (species-dependent)
 * - Courtship duration: 5-60 seconds (100-1200 ticks)
 * - Display range: 8-32 blocks
 * - Mate fidelity: 0.0-1.0 (chance of re-mating same partner)
 */
public class BreedingConfig {

    // Mating system and partner selection
    private MatingSystem matingSystem = MatingSystem.MONOGAMY;
    private double territorySize = 32.0;
    private double mateFidelity = 0.8;

    // Courtship display parameters
    private int courtshipDuration = 20;
    private double displayRange = 16.0;
    private DisplayType displayType = DisplayType.DANCING;
    private double displayTraitWeight = 0.5;
    private double agePreference = 0.3;

    // Breeding season timing
    private int breedingSeasonStart = 3;
    private int breedingSeasonEnd = 6;
    private boolean yearRoundBreeding = false;
    private boolean photoperiodTrigger = false;
    private long minDayLength = 1000;

    // Breeding requirements
    private double minHealthForBreeding = 0.7;
    private int minAgeForBreeding = 0;
    private long breedingCooldown = 6000;

    // Parental investment
    private boolean biparentalCare = false;
    private double parentalInvestmentLevel = 0.5;

    private static final Map<String, BreedingConfig> SPECIES_CONFIGS = new HashMap<>();

    static {
        initializeWolfConfig();
        initializeParrotConfig();
        initializeDeerConfig();
        initializeCowConfig();
    }

    private static void initializeWolfConfig() {
        BreedingConfig config = new BreedingConfig();
        config.matingSystem = MatingSystem.MONOGAMY;
        config.mateFidelity = 0.95;
        config.territorySize = 64.0;
        config.biparentalCare = true;
        config.parentalInvestmentLevel = 0.9;
        config.breedingCooldown = 12000;
        config.minHealthForBreeding = 0.8;
        SPECIES_CONFIGS.put("entity.minecraft.wolf", config);
    }

    private static void initializeParrotConfig() {
        BreedingConfig config = new BreedingConfig();
        config.matingSystem = MatingSystem.LEKKING;
        config.courtshipDuration = 60;
        config.displayRange = 32.0;
        config.displayType = DisplayType.DANCING;
        config.displayTraitWeight = 0.8;
        config.mateFidelity = 0.3;
        config.yearRoundBreeding = true;
        SPECIES_CONFIGS.put("entity.minecraft.parrot", config);
    }

    private static void initializeDeerConfig() {
        BreedingConfig config = new BreedingConfig();
        config.matingSystem = MatingSystem.POLYGYNY;
        config.territorySize = 48.0;
        config.displayType = DisplayType.POSTURING;
        config.breedingSeasonStart = 9;
        config.breedingSeasonEnd = 11;
        config.mateFidelity = 0.2;
        config.biparentalCare = false;
        SPECIES_CONFIGS.put("entity.minecraft.deer", config);
    }

    private static void initializeCowConfig() {
        BreedingConfig config = new BreedingConfig();
        config.matingSystem = MatingSystem.POLYGYNY;
        config.mateFidelity = 0.2;
        config.minHealthForBreeding = 0.7;
        config.yearRoundBreeding = true;
        config.biparentalCare = false;
        config.parentalInvestmentLevel = 0.6;
        SPECIES_CONFIGS.put("entity.minecraft.cow", config);
    }

    public BreedingConfig() {
    }

    /**
     * Creates a configuration optimized for a specific species.
     *
     * @param speciesId The entity ID (e.g., "entity.minecraft.wolf")
     * @return Species-specific configuration, or default if not found
     */
    public static BreedingConfig forSpecies(String speciesId) {
        BreedingConfig config = SPECIES_CONFIGS.get(speciesId);
        return config != null ? config : new BreedingConfig();
    }

    public MatingSystem getMatingSystem() {
        return matingSystem;
    }

    public void setMatingSystem(MatingSystem matingSystem) {
        this.matingSystem = matingSystem;
    }

    public double getTerritorySize() {
        return territorySize;
    }

    public void setTerritorySize(double territorySize) {
        this.territorySize = territorySize;
    }

    public double getMateFidelity() {
        return mateFidelity;
    }

    public void setMateFidelity(double mateFidelity) {
        this.mateFidelity = mateFidelity;
    }

    public int getCourtshipDuration() {
        return courtshipDuration;
    }

    public void setCourtshipDuration(int courtshipDuration) {
        this.courtshipDuration = courtshipDuration;
    }

    public double getDisplayRange() {
        return displayRange;
    }

    public void setDisplayRange(double displayRange) {
        this.displayRange = displayRange;
    }

    public DisplayType getDisplayType() {
        return displayType;
    }

    public void setDisplayType(DisplayType displayType) {
        this.displayType = displayType;
    }

    public double getDisplayTraitWeight() {
        return displayTraitWeight;
    }

    public void setDisplayTraitWeight(double displayTraitWeight) {
        this.displayTraitWeight = displayTraitWeight;
    }

    public double getAgePreference() {
        return agePreference;
    }

    public void setAgePreference(double agePreference) {
        this.agePreference = agePreference;
    }

    public int getBreedingSeasonStart() {
        return breedingSeasonStart;
    }

    public void setBreedingSeasonStart(int breedingSeasonStart) {
        this.breedingSeasonStart = Math.max(0, Math.min(11, breedingSeasonStart));
    }

    public int getBreedingSeasonEnd() {
        return breedingSeasonEnd;
    }

    public void setBreedingSeasonEnd(int breedingSeasonEnd) {
        this.breedingSeasonEnd = Math.max(0, Math.min(11, breedingSeasonEnd));
    }

    public boolean isYearRoundBreeding() {
        return yearRoundBreeding;
    }

    public void setYearRoundBreeding(boolean yearRoundBreeding) {
        this.yearRoundBreeding = yearRoundBreeding;
    }

    public boolean isPhotoperiodTrigger() {
        return photoperiodTrigger;
    }

    public void setPhotoperiodTrigger(boolean photoperiodTrigger) {
        this.photoperiodTrigger = photoperiodTrigger;
    }

    public long getMinDayLength() {
        return minDayLength;
    }

    public void setMinDayLength(long minDayLength) {
        this.minDayLength = Math.max(0, minDayLength);
    }

    public double getMinHealthForBreeding() {
        return minHealthForBreeding;
    }

    public void setMinHealthForBreeding(double minHealthForBreeding) {
        this.minHealthForBreeding = minHealthForBreeding;
    }

    public int getMinAgeForBreeding() {
        return minAgeForBreeding;
    }

    public void setMinAgeForBreeding(int minAgeForBreeding) {
        this.minAgeForBreeding = Math.max(0, minAgeForBreeding);
    }

    public long getBreedingCooldown() {
        return breedingCooldown;
    }

    public void setBreedingCooldown(long breedingCooldown) {
        this.breedingCooldown = breedingCooldown;
    }

    public boolean isBiparentalCare() {
        return biparentalCare;
    }

    public void setBiparentalCare(boolean biparentalCare) {
        this.biparentalCare = biparentalCare;
    }

    public double getParentalInvestmentLevel() {
        return parentalInvestmentLevel;
    }

    public void setParentalInvestmentLevel(double parentalInvestmentLevel) {
        this.parentalInvestmentLevel = parentalInvestmentLevel;
    }

    /**
     * Validates that all parameters are within acceptable ranges.
     */
    public void validate() {
        if (territorySize < 0) {
            throw new IllegalStateException("Territory size must be non-negative");
        }
        if (mateFidelity < 0 || mateFidelity > 1) {
            throw new IllegalStateException("Mate fidelity must be between 0 and 1");
        }
        if (courtshipDuration < 1) {
            throw new IllegalStateException("Courtship duration must be positive");
        }
        if (displayRange < 1) {
            throw new IllegalStateException("Display range must be at least 1");
        }
        if (minHealthForBreeding < 0 || minHealthForBreeding > 1) {
            throw new IllegalStateException("Min health for breeding must be between 0 and 1");
        }
        if (breedingCooldown < 0) {
            throw new IllegalStateException("Breeding cooldown must be non-negative");
        }
        if (parentalInvestmentLevel < 0 || parentalInvestmentLevel > 1) {
            throw new IllegalStateException("Parental investment level must be between 0 and 1");
        }
    }
}
