package me.javavirtualenv.behavior.aquatic;

import me.javavirtualenv.behavior.core.BehaviorContext;
import me.javavirtualenv.behavior.core.SteeringBehavior;
import me.javavirtualenv.behavior.core.Vec3d;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.Dolphin;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * Dolphin treasure hunting and player assistance behavior.
 * Dolphins lead players to buried treasure and underwater ruins.
 * <p>
 * Scientific basis: While real dolphins don't hunt treasure, they are highly
 * intelligent and can be trained. This behavior mimics the game mechanic where
 * dolphins help players find loot.
 */
public class DolphinTreasureHuntBehavior extends SteeringBehavior {
    private final AquaticConfig config;
    private BlockPos targetTreasure;
    private int searchCooldown = 0;
    private Entity assistedPlayer;
    private int assistanceTimer = 0;

    public DolphinTreasureHuntBehavior(AquaticConfig config) {
        super(1.0, true);
        this.config = config;
    }

    public DolphinTreasureHuntBehavior() {
        this(AquaticConfig.createDefault());
    }

    @Override
    public Vec3d calculate(BehaviorContext context) {
        Entity self = context.getEntity();

        if (!(self instanceof Dolphin dolphin)) {
            return new Vec3d();
        }

        Level level = context.getLevel();

        // Update cooldowns
        if (searchCooldown > 0) {
            searchCooldown--;
        }

        if (assistanceTimer > 0) {
            assistanceTimer--;
            if (assistanceTimer <= 0) {
                assistedPlayer = null;
            }
        }

        // Check if leading player to treasure
        if (targetTreasure != null && assistedPlayer != null) {
            return leadToTreasure(context, assistedPlayer, targetTreasure);
        }

        // Look for players to assist
        Entity player = findPlayerToAssist(context);

        if (player != null && searchCooldown == 0) {
            // Find nearby treasure
            BlockPos treasure = findNearestTreasure(context, player.blockPosition());

            if (treasure != null) {
                targetTreasure = treasure;
                assistedPlayer = player;
                assistanceTimer = 600; // Assist for 30 seconds

                // Play dolphin sound
                if (!level.isClientSide) {
                    dolphin.playSound(net.minecraft.sounds.SoundEvents.DOLPHIN_PLAY, 1.0F, 1.0F);
                }
            }

            searchCooldown = 200; // 10 seconds cooldown
        }

        return new Vec3d();
    }

    private Entity findPlayerToAssist(BehaviorContext context) {
        Entity self = context.getEntity();
        Vec3d position = context.getPosition();
        double searchRadius = 32.0;

        for (Entity entity : self.level().getEntitiesOfClass(
                net.minecraft.world.entity.player.Player.class,
                self.getBoundingBox().inflate(searchRadius))) {

            // Check if player is in water
            BlockPos playerPos = entity.blockPosition();
            BlockState playerBlock = context.getLevel().getBlockState(playerPos);

            if (playerBlock.is(Blocks.WATER)) {
                // Check if player fed the dolphin (raw fish or cod)
                if (entity instanceof net.minecraft.world.entity.player.Player player) {
                    // In vanilla, this is tracked by the dolphin entity itself
                    // For now, we'll assist any nearby player in water
                    return entity;
                }
            }
        }

        return null;
    }

    private BlockPos findNearestTreasure(BehaviorContext context, BlockPos centerPos) {
        Level level = context.getLevel();

        if (level.isClientSide) {
            return null;
        }

        ServerLevel serverLevel = (ServerLevel) level;

        // Search for treasure chests in nearby structures
        // This is a simplified search - in reality would use structure manager
        List<BlockPos> potentialTreasures = new ArrayList<>();

        int searchRadius = 64;
        int seaLevel = level.getSeaLevel();

        // Search for buried treasure (under sand/gravel)
        for (int x = -searchRadius; x <= searchRadius; x += 8) {
            for (int z = -searchRadius; z <= searchRadius; z += 8) {
                BlockPos checkPos = new BlockPos(
                    centerPos.getX() + x,
                    seaLevel,
                    centerPos.getZ() + z
                );

                // Check for treasure blocks (could be chest or buried treasure indicator)
                if (isLikelyTreasureLocation(serverLevel, checkPos)) {
                    potentialTreasures.add(checkPos);
                }
            }
        }

        // Find nearest treasure
        BlockPos nearest = null;
        double nearestDistance = Double.MAX_VALUE;

        for (BlockPos pos : potentialTreasures) {
            double distance = centerPos.distSqr(pos);
            if (distance < nearestDistance) {
                nearestDistance = distance;
                nearest = pos;
            }
        }

        return nearest;
    }

    private boolean isLikelyTreasureLocation(ServerLevel level, BlockPos pos) {
        // Look for suspicious terrain patterns
        // Buried treasure is typically under sand or gravel
        BlockState surfaceBlock = level.getBlockState(pos);
        BlockState belowBlock = level.getBlockState(pos.below());

        // Check for sand/gravel beach terrain
        if (surfaceBlock.is(Blocks.SAND) ||
            surfaceBlock.is(Blocks.GRAVEL) ||
            surfaceBlock.is(Blocks.WATER)) {

            // Check below for treasure blocks
            for (int y = 0; y < 5; y++) {
                BlockPos checkPos = pos.below(y);
                BlockState checkBlock = level.getBlockState(checkPos);

                if (checkBlock.is(Blocks.CHEST) ||
                    checkBlock.is(Blocks.BURIED_TREASURE) ||
                    checkBlock.is(Blocks.GRAVEL) && y > 2) {
                    return true;
                }
            }
        }

        return false;
    }

    private Vec3d leadToTreasure(BehaviorContext context, Entity player, BlockPos treasure) {
        Entity self = context.getEntity();
        Vec3d position = context.getPosition();
        Vec3d playerPos = new Vec3d(player.getX(), player.getY(), player.getZ());

        // Calculate midpoint between dolphin and treasure
        Vec3d treasurePos = new Vec3d(
            treasure.getX() + 0.5,
            treasure.getY() + 0.5,
            treasure.getZ() + 0.5
        );

        // Lead position - slightly ahead of player towards treasure
        Vec3d toTreasure = Vec3d.sub(treasurePos, playerPos);
        toTreasure.normalize();

        Vec3d leadPos = playerPos.copy();
        Vec3d leadOffset = toTreasure.copy();
        leadOffset.mult(8.0); // Lead 8 blocks ahead
        leadPos.add(leadOffset);
        leadPos.y = Math.min(playerPos.y, treasurePos.y); // Stay at appropriate depth

        // Check if reached treasure
        double distanceToTreasure = position.distanceTo(treasurePos);
        if (distanceToTreasure < 5.0) {
            // Reached treasure - clear target
            targetTreasure = null;
            assistedPlayer = null;

            // Play success sound
            if (!self.level().isClientSide) {
                self.playSound(net.minecraft.sounds.SoundEvents.DOLPHIN_PLAY, 1.2F, 1.0F);
            }

            return new Vec3d();
        }

        // Calculate steering to lead position
        Vec3d desired = Vec3d.sub(leadPos, position);
        desired.normalize();
        desired.mult(config.getMaxSpeed() * 1.2);

        Vec3d steering = Vec3d.sub(desired, context.getVelocity());

        // Limit force
        if (steering.magnitude() > config.getMaxForce()) {
            steering.normalize();
            steering.mult(config.getMaxForce());
        }

        return steering;
    }

    /**
     * Check if dolphin is currently leading a player.
     */
    public boolean isLeadingPlayer() {
        return assistedPlayer != null && targetTreasure != null;
    }

    /**
     * Get the player being assisted.
     */
    public Entity getAssistedPlayer() {
        return assistedPlayer;
    }

    /**
     * Get the target treasure location.
     */
    public BlockPos getTargetTreasure() {
        return targetTreasure;
    }

    /**
     * Clear the current treasure hunt.
     */
    public void clearTreasureHunt() {
        this.targetTreasure = null;
        this.assistedPlayer = null;
        this.assistanceTimer = 0;
    }

    /**
     * Set a specific treasure target (for manual control).
     */
    public void setTreasureTarget(BlockPos treasure, Entity player) {
        this.targetTreasure = treasure;
        this.assistedPlayer = player;
        this.assistanceTimer = 600;
    }
}
