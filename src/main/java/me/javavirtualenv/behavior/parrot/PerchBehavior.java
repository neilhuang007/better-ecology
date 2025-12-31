package me.javavirtualenv.behavior.parrot;

import me.javavirtualenv.behavior.steering.SeekBehavior;
import me.javavirtualenv.ecology.EcologyComponent;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

/**
 * Perch behavior for parrots.
 * Parrots seek high perches, player shoulders, and perform gliding flights.
 */
public class PerchBehavior {
    private final Mob parrot;
    private final PerchConfig config;
    private final EcologyComponent component;
    private final Random random = new Random();

    private BlockPos currentPerch;
    private PerchType perchType;
    private boolean isPerched = false;
    private int perchTicks = 0;

    public PerchBehavior(Mob parrot, PerchConfig config, EcologyComponent component) {
        this.parrot = parrot;
        this.config = config;
        this.component = component;
    }

    /**
     * Finds a suitable perch position.
     * @return the best perch position, or null if none found
     */
    public BlockPos findPerch() {
        List<PerchCandidate> candidates = new ArrayList<>();
        BlockPos centerPos = parrot.blockPosition();
        int searchRadius = config.perchSearchRadius;

        // Search for potential perches
        for (int x = -searchRadius; x <= searchRadius; x++) {
            for (int y = searchRadius; y >= -searchRadius; y--) {
                for (int z = -searchRadius; z <= searchRadius; z++) {
                    BlockPos pos = centerPos.offset(x, y, z);

                    if (isValidPerch(pos)) {
                        double score = scorePerch(pos);
                        candidates.add(new PerchCandidate(pos, score));
                    }
                }
            }
        }

        if (candidates.isEmpty()) {
            return null;
        }

        // Sort by score and pick the best
        candidates.sort(Comparator.comparingDouble(PerchCandidate::score).reversed());
        return candidates.get(0).pos();
    }

    /**
     * Checks if a position is a valid perch.
     */
    private boolean isValidPerch(BlockPos pos) {
        // Must be within range
        double distance = parrot.distanceToSqr(
            pos.getX() + 0.5,
            pos.getY() + 0.5,
            pos.getZ() + 0.5
        );

        if (distance > config.perchSearchRadius * config.perchSearchRadius) {
            return false;
        }

        // Check if the block is solid
        BlockState blockState = parrot.level().getBlockState(pos);
        if (!blockState.isSolidRender(parrot.level(), pos)) {
            return false;
        }

        // Check if there's space above
        BlockPos abovePos = pos.above();
        BlockState aboveState = parrot.level().getBlockState(abovePos);
        if (aboveState.isSolidRender(parrot.level(), abovePos)) {
            return false;
        }

        // Check if we can path to it (simple line of sight check)
        if (!hasLineOfSight(pos)) {
            return false;
        }

        // Check height preference
        if (config.preferHighPerches) {
            int height = pos.getY() - parrot.level().getMinBuildHeight();
            if (height < config.minPerchHeight) {
                return false;
            }
        }

        return true;
    }

    /**
     * Checks if there's line of sight to a position.
     */
    private boolean hasLineOfSight(BlockPos pos) {
        Vec3 start = parrot.getEyePosition();
        Vec3 end = Vec3.atCenterOf(pos);

        ClipContext context = new ClipContext(
            start,
            end,
            ClipContext.Block.COLLIDER,
            ClipContext.Fluid.NONE,
            parrot
        );

        HitResult result = parrot.level().clip(context);
        return result.getType() == HitResult.Type.MISS ||
               result.getLocation().distanceTo(end) < 1.0;
    }

    /**
     * Scores a perch position based on desirability.
     */
    private double scorePerch(BlockPos pos) {
        double score = 0.0;

        // Height bonus
        if (config.preferHighPerches) {
            int height = pos.getY() - parrot.level().getMinBuildHeight();
            score += height * config.heightBonus;
        }

        // Distance penalty (prefer closer perches)
        double distance = parrot.distanceTo(pos);
        score -= distance * config.distancePenalty;

        // Block type bonus
        Block block = parrot.level().getBlockState(pos).getBlock();
        if (config.preferredBlocks.contains(block)) {
            score += config.preferredBlockBonus;
        }

        // Leaf bonus (parrots like trees)
        if (block == Blocks.OAK_LEAVES ||
            block == Blocks.BIRCH_LEAVES ||
            block == Blocks.JUNGLE_LEAVES ||
            block == Blocks.ACACIA_LEAVES ||
            block == Blocks.DARK_OAK_LEAVES ||
            block == Blocks.AZALEA_LEAVES ||
            block == Blocks.FLOWERING_AZALEA_LEAVES) {
            score += config.leafBonus;
        }

        // Isolation bonus (parrots prefer perches away from hostile mobs)
        if (config.preferIsolatedPerches) {
            score += scoreIsolation(pos);
        }

        return score;
    }

    /**
     * Scores a position based on isolation from threats.
     */
    private double scoreIsolation(BlockPos pos) {
        double isolationScore = 0.0;

        List<Mob> nearbyMobs = parrot.level().getEntitiesOfClass(
            Mob.class,
            parrot.getBoundingBox().inflate(config.threatDetectionRadius)
        );

        for (Mob mob : nearbyMobs) {
            if (isThreat(mob)) {
                double threatDistance = mob.distanceToSqr(
                    pos.getX() + 0.5,
                    pos.getY() + 0.5,
                    pos.getZ() + 0.5
                );

                // Penalize perches near threats
                isolationScore -= config.threatPenalty / (threatDistance + 1.0);
            }
        }

        return isolationScore;
    }

    /**
     * Checks if a mob is a threat to parrots.
     */
    private boolean isThreat(Mob mob) {
        // Common predators
        String mobType = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE
            .getKey(mob.getType()).toString();

        return mobType.equals("minecraft:cat") ||
               mobType.equals("minecraft:fox") ||
               mobType.equals("minecraft:wolf") ||
               mobType.equals("minecraft:ocelot");
    }

    /**
     * Attempts to perch on a nearby player's shoulder.
     * @return true if successfully perched on a shoulder
     */
    public boolean tryPerchOnShoulder() {
        List<Player> nearbyPlayers = parrot.level().getEntitiesOfClass(
            Player.class,
            parrot.getBoundingBox().inflate(config.shoulderPerchRange)
        );

        for (Player player : nearbyPlayers) {
            if (canPerchOnShoulder(player)) {
                perchOnShoulder(player);
                return true;
            }
        }

        return false;
    }

    /**
     * Checks if a parrot can perch on a player's shoulder.
     */
    private boolean canPerchOnShoulder(Player player) {
        // Check if player has a shoulder available
        if (!player.getShoulderEntityLeft().isEmpty() && !player.getShoulderEntityRight().isEmpty()) {
            return false;
        }

        // Check if parrot is tamed (vanilla parrot behavior)
        if (parrot instanceof net.minecraft.world.entity.animal.Parrot vanillaParrot) {
            return vanillaParrot.isTame();
        }

        return false;
    }

    /**
     * Perches the parrot on a player's shoulder.
     */
    private void perchOnShoulder(Player player) {
        if (parrot instanceof net.minecraft.world.entity.animal.Parrot vanillaParrot) {
            // Use vanilla shoulder perching
            vanillaParrot.startRiding(player, true);

            // Update component
            CompoundTag perchData = component.getHandleTag("perch");
            perchData.putBoolean("is_perched", true);
            perchData.putString("perch_type", PerchType.SHOULDER.name());
            perchData.putUUID("shoulder_owner", player.getUUID());
            component.setHandleTag("perch", perchData);

            this.isPerched = true;
            this.perchType = PerchType.SHOULDER;
        }
    }

    /**
     * Starts perching at a position.
     */
    public void startPerching(BlockPos pos, PerchType type) {
        this.currentPerch = pos;
        this.perchType = type;
        this.isPerched = true;
        this.perchTicks = 0;

        // Update component
        CompoundTag perchData = component.getHandleTag("perch");
        perchData.putBoolean("is_perched", true);
        perchData.putString("perch_type", type.name());
        perchData.putInt("perch_x", pos.getX());
        perchData.putInt("perch_y", pos.getY());
        perchData.putInt("perch_z", pos.getZ());
        component.setHandleTag("perch", perchData);
    }

    /**
     * Stops perching.
     */
    public void stopPerching() {
        this.currentPerch = null;
        this.isPerched = false;
        this.perchTicks = 0;

        // Clear component
        CompoundTag perchData = component.getHandleTag("perch");
        perchData.putBoolean("is_perched", false);
        perchData.remove("perch_type");
        component.setHandleTag("perch", perchData);
    }

    /**
     * Updates the perch behavior.
     */
    public void tick() {
        if (!isPerched) {
            return;
        }

        perchTicks++;

        // Check if we should leave the perch
        if (shouldLeavePerch()) {
            stopPerching();
            return;
        }

        // Random movements while perched
        if (perchTicks % config.perchAdjustInterval == 0) {
            adjustPerchPosition();
        }
    }

    /**
     * Checks if the parrot should leave its perch.
     */
    private boolean shouldLeavePerch() {
        // Leave if spooked
        if (wasSpooked()) {
            return true;
        }

        // Leave after some time
        if (perchTicks > config.maxPerchTicks) {
            return true;
        }

        // Leave if perch is no longer valid
        if (currentPerch != null && !isValidPerch(currentPerch)) {
            return true;
        }

        return false;
    }

    /**
     * Checks if the parrot was spooked.
     */
    private boolean wasSpooked() {
        List<Mob> nearbyMobs = parrot.level().getEntitiesOfClass(
            Mob.class,
            parrot.getBoundingBox().inflate(config.spookRadius)
        );

        for (Mob mob : nearbyMobs) {
            if (isThreat(mob)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Makes small adjustments to perch position.
     */
    private void adjustPerchPosition() {
        if (currentPerch == null) {
            return;
        }

        // Small hop or head movement
        if (random.nextDouble() < 0.3) {
            parrot.getJumpControl().jump();
        }

        // Look around
        float lookChange = random.nextFloat() * 60 - 30;
        parrot.setYRot(parrot.getYRot() + lookChange);
    }

    /**
     * Performs a glide flight to the ground.
     */
    public void glideToGround() {
        BlockPos groundPos = findGroundPosition();
        if (groundPos != null) {
            flyToPosition(groundPos, config.glideSpeed);
        }
    }

    /**
     * Finds a suitable ground position to glide to.
     */
    private BlockPos findGroundPosition() {
        BlockPos currentPos = parrot.blockPosition();

        // Look for ground directly below
        for (int y = currentPos.getY(); y >= parrot.level().getMinBuildHeight(); y--) {
            BlockPos checkPos = new BlockPos(currentPos.getX(), y, currentPos.getZ());
            BlockState blockState = parrot.level().getBlockState(checkPos);

            if (blockState.isSolidRender(parrot.level(), checkPos)) {
                // Found ground, return position above it
                return checkPos.above();
            }
        }

        return null;
    }

    /**
     * Flies to a specific position.
     */
    private void flyToPosition(BlockPos pos, double speed) {
        if (parrot.getNavigation().isDone()) {
            parrot.getNavigation().moveTo(
                pos.getX() + 0.5,
                pos.getY() + 0.5,
                pos.getZ() + 0.5,
                speed
            );
        }
    }

    public boolean isPerched() {
        return isPerched;
    }

    public BlockPos getCurrentPerch() {
        return currentPerch;
    }

    public PerchType getPerchType() {
        return perchType;
    }

    /**
     * Types of perches.
     */
    public enum PerchType {
        SHOULDER,
        TREE_BRANCH,
        FENCE,
        BUILDING,
        GROUND
    }

    /**
     * Configuration for perch behavior.
     */
    public static class PerchConfig {
        public int perchSearchRadius = 32;
        public boolean preferHighPerches = true;
        public int minPerchHeight = 5;
        public double heightBonus = 1.0;
        public double distancePenalty = 0.1;
        public double leafBonus = 10.0;
        public double preferredBlockBonus = 5.0;
        public boolean preferIsolatedPerches = true;
        public double threatDetectionRadius = 16.0;
        public double threatPenalty = 20.0;

        public java.util.Set<Block> preferredBlocks = new java.util.HashSet<>();

        public double shoulderPerchRange = 3.0;
        public double spookRadius = 8.0;
        public int maxPerchTicks = 1200; // 60 seconds
        public int perchAdjustInterval = 40;

        public double glideSpeed = 0.8;

        public PerchConfig() {
            // Default preferred blocks
            preferredBlocks.add(Blocks.OAK_FENCE);
            preferredBlocks.add(Blocks.BIRCH_FENCE);
            preferredBlocks.add(Blocks.JUNGLE_FENCE);
            preferredBlocks.add(Blocks.ACACIA_FENCE);
            preferredBlocks.add(Blocks.DARK_OAK_FENCE);
            preferredBlocks.add(Blocks.OAK_LOG);
            preferredBlocks.add(Blocks.BIRCH_LOG);
            preferredBlocks.add(Blocks.JUNGLE_LOG);
            preferredBlocks.add(Blocks.ACACIA_LOG);
            preferredBlocks.add(Blocks.DARK_OAK_LOG);
        }
    }

    private record PerchCandidate(BlockPos pos, double score) {
    }
}
