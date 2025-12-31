package me.javavirtualenv.behavior.reproduction;

import me.javavirtualenv.ecology.EcologyComponent;
import me.javavirtualenv.ecology.handles.reproduction.NestBuildingHandle;
import me.javavirtualenv.ecology.handles.reproduction.NestData;
import me.javavirtualenv.ecology.handles.reproduction.NestBuildingConfig;
import me.javavirtualenv.ecology.handles.reproduction.NestBuildingHandle.NestBuildingConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.util.RandomPos;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;
import java.util.List;
import java.util.function.Predicate;

/**
 * AI goal for nest building behavior.
 * <p>
 * This goal handles:
 * - Finding suitable nest locations
 * - Gathering nest materials
 * - Building/constructing nests
 * - Defending nests from intruders
 * - Maintaining nest quality
 */
public class NestBuildingGoal extends Goal {
    private final Animal animal;
    private final NestBuildingConfig config;
    private final Level level;

    private NestData nestData;
    private BuildingPhase currentPhase;
    private BlockPos targetLocation;
    private Vec3 targetPosition;
    private int buildingCooldown;
    private int searchCooldown;

    public NestBuildingGoal(Animal animal, NestBuildingConfig config) {
        this.animal = animal;
        this.config = config;
        this.level = animal.level();
        this.currentPhase = BuildingPhase.FINDING_LOCATION;
        this.setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        if (!animal.isAlive()) {
            return false;
        }

        if (!animal.isBaby()) {
            return false;
        }

        // Load nest data
        EcologyComponent component = EcologyComponent.get(animal);
        if (component == null) {
            return false;
        }

        nestData = NestBuildingHandle.getOrCreateNestData(component, config);

        // Check if nest is complete and usable
        if (nestData.isComplete() && nestData.canUseNest(level)) {
            return false;
        }

        // Abandoned nest - need to find new location
        if (nestData.isAbandoned()) {
            nestData.reset();
            currentPhase = BuildingPhase.FINDING_LOCATION;
        }

        return true;
    }

    @Override
    public boolean canContinueToUse() {
        if (!animal.isAlive()) {
            return false;
        }

        if (nestData == null) {
            return false;
        }

        // Stop if nest is complete
        if (nestData.isComplete() && nestData.canUseNest(level)) {
            return false;
        }

        return true;
    }

    @Override
    public void start() {
        determinePhase();
    }

    @Override
    public void stop() {
        targetLocation = null;
        targetPosition = null;
    }

    @Override
    public void tick() {
        if (buildingCooldown > 0) {
            buildingCooldown--;
            return;
        }

        if (searchCooldown > 0) {
            searchCooldown--;
            return;
        }

        switch (currentPhase) {
            case FINDING_LOCATION -> findNestLocation();
            case GATHERING_MATERIALS -> gatherMaterials();
            case BUILDING_NEST -> buildNest();
            case DEFENDING_NEST -> defendNest();
            case MAINTENANCE -> performMaintenance();
        }

        buildingCooldown = (int) (20 / config.buildingSpeed());
    }

    private void determinePhase() {
        if (!nestData.hasNest()) {
            currentPhase = BuildingPhase.FINDING_LOCATION;
        } else if (nestData.getCollectedMaterials() < config.maxMaterials() &&
                   nestData.getBuildingProgress() < 100) {
            currentPhase = BuildingPhase.GATHERING_MATERIALS;
        } else if (nestData.getBuildingProgress() < 100) {
            currentPhase = BuildingPhase.BUILDING_NEST;
        } else if (nestData.isComplete() && !nestData.canUseNest(level)) {
            currentPhase = BuildingPhase.MAINTENANCE;
        } else {
            currentPhase = BuildingPhase.DEFENDING_NEST;
        }
    }

    private void findNestLocation() {
        if (targetLocation != null) {
            moveToTarget();
            return;
        }

        if (searchCooldown > 0) {
            return;
        }

        BlockPos bestLocation = findBestNestLocation();

        if (bestLocation != null) {
            nestData.setNestLocation(bestLocation);
            targetLocation = bestLocation;
            targetPosition = Vec3.atCenterOf(bestLocation);
            currentPhase = BuildingPhase.GATHERING_MATERIALS;
        }

        searchCooldown = 100; // 5 seconds
    }

    private BlockPos findBestNestLocation() {
        BlockPos entityPos = animal.blockPosition();
        BlockPos bestPos = null;
        double bestScore = Double.NEGATIVE_INFINITY;

        for (int i = 0; i < 20; i++) {
            BlockPos testPos = getRandomPositionInRange(entityPos, config.searchRadius());
            if (testPos == null) {
                continue;
            }

            double score = scoreNestLocation(testPos);
            if (score > bestScore) {
                bestScore = score;
                bestPos = testPos;
            }
        }

        return bestPos;
    }

    private BlockPos getRandomPositionInRange(BlockPos center, int radius) {
        Vec3 randomVec = RandomPos.getPos(animal, radius, radius / 2);
        if (randomVec == null) {
            return null;
        }
        return BlockPos.containing(randomVec);
    }

    private double scoreNestLocation(BlockPos pos) {
        double score = 0.0;

        // Check if position is valid
        if (!isValidNestPosition(pos)) {
            return Double.NEGATIVE_INFINITY;
        }

        // Distance from current position (prefer closer)
        double distance = animal.blockPosition().distSqr(pos);
        score -= distance * 0.01;

        // Shelter preference
        if (config.preferShelter()) {
            score += scoreShelter(pos);
        }

        // Nest type-specific scoring
        if (config.isTreeNest()) {
            score += scoreTreeNest(pos);
        } else if (config.isGroundNest()) {
            score += scoreGroundNest(pos);
        } else if (config.isBurrowNest()) {
            score += scoreBurrowNest(pos);
        } else if (config.isSandNest()) {
            score += scoreSandNest(pos);
        }

        return score;
    }

    private boolean isValidNestPosition(BlockPos pos) {
        if (!level.hasChunkAt(pos)) {
            return false;
        }

        BlockState blockState = level.getBlockState(pos);

        if (config.isTreeNest()) {
            // Check for leaves or wood
            return isTreeBlock(level.getBlockState(pos)) ||
                   isTreeBlock(level.getBlockState(pos.above()));
        } else if (config.isGroundNest()) {
            // Check for solid ground
            return level.getBlockState(pos.below()).isSolid() &&
                   level.getBlockState(pos).isAir();
        } else if (config.isBurrowNest()) {
            // Check for diggable ground
            BlockState below = level.getBlockState(pos.below());
            return below.isSolid() && below.getBlock() != Blocks.BEDROCK;
        } else if (config.isSandNest()) {
            // Check for sand
            BlockState below = level.getBlockState(pos.below());
            return below.is(Blocks.SAND);
        }

        return true;
    }

    private boolean isTreeBlock(BlockState state) {
        return state.is(Blocks.OAK_LOG) ||
               state.is(Blocks.BIRCH_LOG) ||
               state.is(Blocks.SPRUCE_LOG) ||
               state.is(Blocks.JUNGLE_LOG) ||
               state.is(Blocks.ACACIA_LOG) ||
               state.is(Blocks.DARK_OAK_LOG) ||
               state.is(Blocks.OAK_LEAVES) ||
               state.is(Blocks.BIRCH_LEAVES) ||
               state.is(Blocks.SPRUCE_LEAVES) ||
               state.is(Blocks.JUNGLE_LEAVES) ||
               state.is(Blocks.ACACIA_LEAVES) ||
               state.is(Blocks.DARK_OAK_LEAVES);
    }

    private double scoreShelter(BlockPos pos) {
        double score = 0.0;

        // Check for blocks above (shelter)
        int shelterBlocks = 0;
        for (int i = 1; i <= 3; i++) {
            if (!level.getBlockState(pos.above(i)).isAir()) {
                shelterBlocks++;
            }
        }
        score += shelterBlocks * 2.0;

        // Check for surrounding blocks (windbreak)
        int surroundBlocks = 0;
        for (BlockPos checkPos : BlockPos.betweenClosed(pos.offset(-1, 0, -1),
                                                          pos.offset(1, 0, 1))) {
            if (!level.getBlockState(checkPos).isAir()) {
                surroundBlocks++;
            }
        }
        score += surroundBlocks * 0.5;

        return score;
    }

    private double scoreTreeNest(BlockPos pos) {
        double score = 0.0;

        // Prefer higher positions
        int height = pos.getY() - level.getMinBuildHeight();
        score += height * 0.1;

        // Prefer leaves
        if (level.getBlockState(pos).getBlock() instanceof LeavesBlock) {
            score += 5.0;
        }

        // Prefer wood support below
        if (isTreeBlock(level.getBlockState(pos.below()))) {
            score += 3.0;
        }

        return score;
    }

    private double scoreGroundNest(BlockPos pos) {
        double score = 0.0;

        // Prefer grass or dirt below
        BlockState below = level.getBlockState(pos.below());
        if (below.is(Blocks.GRASS_BLOCK) || below.is(Blocks.DIRT)) {
            score += 3.0;
        }

        // Prefer hay bales
        if (below.is(Blocks.HAY_BLOCK)) {
            score += 10.0;
        }

        return score;
    }

    private double scoreBurrowNest(BlockPos pos) {
        double score = 0.0;

        // Prefer dirt or grass
        BlockState below = level.getBlockState(pos.below());
        if (below.is(Blocks.DIRT) || below.is(Blocks.GRASS_BLOCK)) {
            score += 3.0;
        }

        // Prefer areas near bushes (hidden entrance)
        if (hasNearbyBush(pos)) {
            score += 5.0;
        }

        return score;
    }

    private double scoreSandNest(BlockPos pos) {
        double score = 0.0;

        // Must have sand below
        BlockState below = level.getBlockState(pos.below());
        if (below.is(Blocks.SAND)) {
            score += 10.0;
        }

        // Prefer near water (turtles)
        if (isNearWater(pos)) {
            score += 5.0;
        }

        return score;
    }

    private boolean hasNearbyBush(BlockPos pos) {
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                BlockState state = level.getBlockState(pos.offset(dx, 0, dz));
                if (state.is(Blocks.SWEET_BERRY_BUSH) ||
                    state.getBlock() instanceof LeavesBlock) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean isNearWater(BlockPos pos) {
        for (int dx = -5; dx <= 5; dx++) {
            for (int dz = -5; dz <= 5; dz++) {
                BlockState state = level.getBlockState(pos.offset(dx, 0, dz));
                if (state.is(Blocks.WATER)) {
                    return true;
                }
            }
        }
        return false;
    }

    private void gatherMaterials() {
        if (nestData.getCollectedMaterials() >= config.maxMaterials()) {
            currentPhase = BuildingPhase.BUILDING_NEST;
            return;
        }

        // Look for nearby materials
        Vec3 materialPos = findNearbyMaterials();

        if (materialPos != null) {
            targetPosition = materialPos;
            moveToTarget();

            if (animal.position().distanceTo(materialPos) < 2.0) {
                collectMaterial(materialPos);
            }
        } else {
            // Random wander to find materials
            if (animal.getRandom().nextFloat() < 0.02) {
                Vec3 randomPos = RandomPos.getPos(animal, 16, 8);
                if (randomPos != null) {
                    targetPosition = randomPos;
                }
            }
            moveToTarget();
        }
    }

    private Vec3 findNearbyMaterials() {
        BlockPos pos = animal.blockPosition();
        int searchRadius = 16;

        // Search for items that could be nest materials
        for (int dx = -searchRadius; dx <= searchRadius; dx++) {
            for (int dy = -3; dy <= 3; dy++) {
                for (int dz = -searchRadius; dz <= searchRadius; dz++) {
                    BlockPos checkPos = pos.offset(dx, dy, dz);
                    BlockState state = level.getBlockState(checkPos);

                    if (isUsableMaterial(state)) {
                        return Vec3.atCenterOf(checkPos);
                    }
                }
            }
        }

        return null;
    }

    private boolean isUsableMaterial(BlockState state) {
        Block block = state.getBlock();
        return block == Blocks.GRASS ||
               block == Blocks.SHORT_GRASS ||
               block == Blocks.FERN ||
               block == Blocks.LARGE_FERN ||
               block == Blocks.HAY_BLOCK ||
               block == Blocks.WHITE_WOOL ||
               block == Blocks.STRING;
    }

    private void collectMaterial(Vec3 pos) {
        BlockPos blockPos = BlockPos.containing(pos);
        BlockState state = level.getBlockState(blockPos);

        if (isUsableMaterial(state)) {
            String materialType = getMaterialType(state);
            nestData.addMaterial(materialType, 1);
            buildingCooldown = 20;
        }
    }

    private String getMaterialType(BlockState state) {
        Block block = state.getBlock();
        if (block == Blocks.GRASS || block == Blocks.SHORT_GRASS) {
            return "grass";
        } else if (block == Blocks.FERN || block == Blocks.LARGE_FERN) {
            return "leaves";
        } else if (block == Blocks.HAY_BLOCK) {
            return "hay";
        } else if (block == Blocks.WHITE_WOOL) {
            return "wool";
        } else if (block == Blocks.STRING) {
            return "fiber";
        }
        return "unknown";
    }

    private void buildNest() {
        if (targetLocation == null) {
            targetLocation = nestData.getNestLocation();
        }

        if (targetLocation == null) {
            currentPhase = BuildingPhase.FINDING_LOCATION;
            return;
        }

        targetPosition = Vec3.atCenterOf(targetLocation);
        moveToTarget();

        if (animal.position().distanceTo(targetPosition) < 3.0) {
            // Build the nest
            constructNest();
        }
    }

    private void constructNest() {
        // Add building progress based on materials
        int materials = nestData.getCollectedMaterials();
        int requiredMaterials = config.maxMaterials() / 2;

        if (materials >= requiredMaterials) {
            int progressToAdd = (int) ((materials / (double) requiredMaterials) * 10);
            nestData.addProgress(progressToAdd);

            // Place nest block if complete
            if (nestData.isComplete() && nestData.getBuildingProgress() >= 100) {
                placeNestBlock();
            }
        }
    }

    private void placeNestBlock() {
        BlockPos nestPos = nestData.getNestLocation();
        if (nestPos == null) {
            return;
        }

        // Place nest block based on type
        if (config.isGroundNest() || config.isSandNest()) {
            // For ground nests, we might not place a block but mark the area
            level.setBlockAndUpdate(nestPos, Blocks.AIR.defaultBlockState());
        } else if (config.isTreeNest()) {
            // Bird nest in tree - use a hay block as visual
            if (level.getBlockState(nestPos).isAir()) {
                level.setBlockAndUpdate(nestPos, Blocks.HAY_BLOCK.defaultBlockState());
            }
        }
        // Burrow nests don't have visible blocks
    }

    private void defendNest() {
        if (!config.territorialDefense()) {
            return;
        }

        if (!nestData.hasNest()) {
            currentPhase = BuildingPhase.FINDING_LOCATION;
            return;
        }

        BlockPos nestPos = nestData.getNestLocation();

        // Check for nearby intruders
        List<Animal> nearbyAnimals = level.getEntitiesOfClass(
            Animal.class,
            animal.getBoundingBox().inflate(8.0)
        );

        for (Animal other : nearbyAnimals) {
            if (other != animal && other.getType() == animal.getType()) {
                // Same species - may be threat to nest
                double dist = other.position().distanceTo(Vec3.atCenterOf(nestPos));
                if (dist < 5.0) {
                    nestData.recordDisturbance(level);
                    // Flee or chase based on size
                    if (other.getBoundingBox().getSize() > animal.getBoundingBox().getSize()) {
                        // Flee
                        Vec3 fleePos = RandomPos.getPosAway(animal, 16, 8, other.position());
                        if (fleePos != null) {
                            targetPosition = fleePos;
                            moveToTarget();
                        }
                    }
                }
            }
        }
    }

    private void performMaintenance() {
        if (nestData.getTimeSinceLastDisturbance(level) > 1200) { // 1 minute
            // Reduce disturbance count over time
            nestData.recordDisturbance(level);
            if (nestData.isAbandoned()) {
                nestData.reset();
                currentPhase = BuildingPhase.FINDING_LOCATION;
            }
        }

        // Improve nest quality
        if (nestData.getNestQuality() < 0.8 && nestData.getCollectedMaterials() > 0) {
            double qualityImprovement = 0.01 * config.buildingSpeed();
            nestData.setNestQuality(nestData.getNestQuality() + qualityImprovement);
        }
    }

    private void moveToTarget() {
        if (targetPosition != null) {
            animal.getNavigation().moveTo(targetPosition.x, targetPosition.y, targetPosition.z, 1.0);
        }
    }

    enum BuildingPhase {
        FINDING_LOCATION,
        GATHERING_MATERIALS,
        BUILDING_NEST,
        DEFENDING_NEST,
        MAINTENANCE
    }
}
