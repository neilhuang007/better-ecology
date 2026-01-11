package me.javavirtualenv.behavior.grazing;

import me.javavirtualenv.behavior.core.BehaviorContext;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;

import java.util.ArrayList;
import java.util.List;

/**
 * Grazing behavior for grass-eating herbivores.
 * Implements bimodal grazing patterns, midday rest, and patch selection based on research.
 */
public class GrazingBehavior {

    private final int grazingStartTime;
    private final int grazingEndTime;
    private final int middayRestStart;
    private final int middayRestEnd;
    private final int patchSize;
    private final double givingUpDensity;
    private final int biteSize;
    private final double grazingSpeed;
    private Season season = Season.SPRING;
    private double personality = 1.0; // 1.0 = moderate, <1 = aggressive/bold, >1 = selective/shy
    private double socialDominance = 0.5;
    private int accumulatedGrazingTime = 0;
    private boolean isContinuousGrazing = false;

    public GrazingBehavior(int grazingStartTime, int grazingEndTime,
                           int middayRestStart, int middayRestEnd,
                           int patchSize, double givingUpDensity,
                           int biteSize, double grazingSpeed) {
        this.grazingStartTime = grazingStartTime;
        this.grazingEndTime = grazingEndTime;
        this.middayRestStart = middayRestStart;
        this.middayRestEnd = middayRestEnd;
        this.patchSize = patchSize;
        this.givingUpDensity = givingUpDensity;
        this.biteSize = biteSize;
        this.grazingSpeed = grazingSpeed;
    }

    public boolean isGrazingTime(long dayTime) {
        // Morning grazing: grazingStartTime to middayRestStart
        if (dayTime >= grazingStartTime && dayTime < middayRestStart) {
            return true;
        }
        // Afternoon grazing: middayRestEnd to grazingEndTime
        if (dayTime >= middayRestEnd && dayTime < grazingEndTime) {
            return true;
        }
        return false;
    }

    public boolean isMiddayRest(long dayTime) {
        return dayTime >= middayRestStart && dayTime < middayRestEnd;
    }

    public boolean shouldLeavePatch(double patchQuality) {
        double adjustedGUD = givingUpDensity * personality;
        return patchQuality < adjustedGUD;
    }

    public double calculatePatchQuality(BlockPos patchCenter, Level level) {
        int grassBlocks = 0;
        int totalBlocks = 0;

        for (int x = -patchSize; x <= patchSize; x++) {
            for (int z = -patchSize; z <= patchSize; z++) {
                BlockPos pos = patchCenter.offset(x, 0, z);
                totalBlocks++;
                if (level.getBlockState(pos).isAir()) {
                    pos = pos.below();
                    if (!level.getBlockState(pos).isAir()) {
                        grassBlocks++;
                    }
                }
            }
        }

        return totalBlocks > 0 ? (double) grassBlocks / totalBlocks : 0.0;
    }

    public double calculatePatchQuality(Level level, BlockPos patchCenter, int searchRadius) {
        int grassBlocks = 0;
        int totalBlocks = 0;

        for (int x = -searchRadius; x <= searchRadius; x++) {
            for (int z = -searchRadius; z <= searchRadius; z++) {
                BlockPos pos = patchCenter.offset(x, 0, z);
                totalBlocks++;
                if (level.getBlockState(pos).is(Blocks.GRASS_BLOCK) ||
                    level.getBlockState(pos).is(Blocks.SHORT_GRASS) ||
                    level.getBlockState(pos).is(Blocks.TALL_GRASS)) {
                    grassBlocks++;
                }
            }
        }

        return totalBlocks > 0 ? (double) grassBlocks / totalBlocks : 0.0;
    }

    public double calculateResidenceTime(double travelTime, double patchQuality) {
        // Marginal Value Theorem: residence time decreases with longer travel times
        // and increases with patch quality
        double m = 1.0;
        return m * (1.0 - patchQuality) * travelTime + (patchQuality / givingUpDensity) * 100.0;
    }

    public long calculateOptimalResidenceTime(double travelTime, double patchQuality, double handlingTime) {
        // Marginal Value Theorem formula
        // Residence time = sqrt((2 * travelTime * handlingTime) / patchQuality) - handlingTime
        if (patchQuality <= 0) return 0;
        double residenceTime = Math.sqrt((2 * travelTime * handlingTime) / patchQuality) - handlingTime;
        return Math.max(0, (long) residenceTime);
    }

    public List<BlockPos> findGrassPatches(Level level, BlockPos centerPos, int radius) {
        List<BlockPos> patches = new ArrayList<>();

        for (int x = -radius; x <= radius; x += 4) {
            for (int z = -radius; z <= radius; z += 4) {
                BlockPos pos = centerPos.offset(x, 0, z);
                if (calculatePatchQuality(level, pos, 2) > 0.1) {
                    patches.add(pos);
                }
            }
        }

        return patches;
    }

    public void setSeason(Season season) {
        this.season = season;
    }

    public Season getSeason() {
        return season;
    }

    public void setGrazingPersonality(double personality) {
        this.personality = Math.max(0.5, Math.min(1.5, personality));
    }

    public double getGrazingPersonality() {
        return personality;
    }

    public double getAdjustedGivingUpDensity() {
        return givingUpDensity * personality;
    }

    public void setSocialDominance(double dominance) {
        this.socialDominance = Math.max(0.0, Math.min(1.0, dominance));
    }

    public double getSocialDominance() {
        return socialDominance;
    }

    public BlockPos selectPatchByDominance(List<BlockPos> availablePatches) {
        if (availablePatches.isEmpty()) return null;
        // Dominant animals get first choice - return best patch
        return availablePatches.get(0);
    }

    public int getPreferredPatchSize() {
        // Selective grazers (personality > 1) prefer larger patches
        return personality > 1.0 ? patchSize * 2 : patchSize;
    }

    public void setContinuousGrazing(boolean continuous) {
        this.isContinuousGrazing = continuous;
    }

    public boolean isContinuousGrazing() {
        return isContinuousGrazing;
    }

    public void addGrazingTime(int ticks) {
        this.accumulatedGrazingTime += ticks;
    }

    public int getAccumulatedGrazingTime() {
        return accumulatedGrazingTime;
    }

    public void resetGrazingTime() {
        this.accumulatedGrazingTime = 0;
    }

    public int getPatchSize() {
        return patchSize;
    }

    public double getGivingUpDensity() {
        return givingUpDensity;
    }

    public int getBiteSize() {
        return biteSize;
    }

    public double getGrazingSpeed() {
        return grazingSpeed;
    }

    public double getSociallyAdjustedGUD() {
        // Social dominance affects GUD - dominant animals have lower GUD
        double dominanceBonus = (1.0 - socialDominance) * 0.2;
        return givingUpDensity * personality - dominanceBonus;
    }

    public me.javavirtualenv.behavior.core.Vec3d getSeasonallyAdjustedSpeed(me.javavirtualenv.behavior.core.Vec3d baseVelocity) {
        double speedMultiplier = 1.0;
        switch (season) {
            case WINTER:
            case DRY:
                speedMultiplier = 0.8; // Slower in harsh conditions
                break;
            case SPRING:
            case SUMMER:
            case WET:
                speedMultiplier = 1.2; // Faster in good conditions
                break;
            default:
                speedMultiplier = 1.0;
        }
        return new me.javavirtualenv.behavior.core.Vec3d(
            baseVelocity.x * speedMultiplier,
            baseVelocity.y,
            baseVelocity.z * speedMultiplier
        );
    }

    public double getSeasonallyAdjustedGUD() {
        double seasonModifier = 1.0;
        switch (season) {
            case WINTER:
            case DRY:
                seasonModifier = 0.7; // Lower GUD in harsh conditions (less picky)
                break;
            case SPRING:
            case SUMMER:
            case WET:
                seasonModifier = 1.3; // Higher GUD in good conditions (more picky)
                break;
        }
        return givingUpDensity * seasonModifier;
    }

    public double getBaseGivingUpDensity() {
        return givingUpDensity;
    }

    // Static factory methods for species-specific configurations
    public static GrazingBehavior forCattle() {
        return new GrazingBehavior(1000, 11000, 5000, 7000, 8, 0.3, 3, 0.8);
    }

    public static GrazingBehavior forSheep() {
        return new GrazingBehavior(1000, 11000, 5000, 7000, 5, 0.4, 2, 0.7);
    }

    public static GrazingBehavior forHorses() {
        return new GrazingBehavior(1000, 13000, 0, 0, 10, 0.2, 4, 1.2); // Continuous grazing
    }

    /**
     * Calculates steering force for grazing behavior.
     */
    public me.javavirtualenv.behavior.core.Vec3d calculate(BehaviorContext context) {
        if (!isGrazingTime(context.getDayTime())) {
            // Return wander vector when not grazing
            double angle = Math.random() * 2 * Math.PI;
            return new me.javavirtualenv.behavior.core.Vec3d(
                Math.cos(angle) * 0.1, 0, Math.sin(angle) * 0.1);
        }
        // Return zero vector when contentedly grazing
        return new me.javavirtualenv.behavior.core.Vec3d(0, 0, 0);
    }

    /**
     * Gets the daily travel distance for this grazing configuration (in blocks).
     * Based on research: sheep travel ~2.85 km/day.
     */
    public double getDailyTravelDistance() {
        // Adjust based on patch size - larger patches = less travel
        double baseDistance = 2850.0; // sheep baseline in blocks (~2.85 km)
        return baseDistance * (8.0 / patchSize); // Inverse relationship with patch size
    }

    /**
     * Consumes grass at the specified position.
     */
    public void consumeGrass(BehaviorContext context, BlockPos pos) {
        if (pos == null) return;
        // Stub implementation - would actually consume grass in real behavior
        Object levelObj = context.getLevel();

        if (!(levelObj instanceof Level level)) {

            return;

        }
        // Grass consumption logic would go here
    }

    public enum Season {
        SPRING, SUMMER, AUTUMN, WINTER, DRY, WET
    }
}
