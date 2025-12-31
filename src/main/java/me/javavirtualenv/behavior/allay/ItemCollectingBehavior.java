package me.javavirtualenv.behavior.allay;

import me.javavirtualenv.behavior.core.BehaviorContext;
import me.javavirtualenv.behavior.core.Vec3d;
import me.javavirtualenv.behavior.steering.SteeringBehavior;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.allay.Allay;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.List;

/**
 * Behavior for allay item collecting mechanics.
 * <p>
 * This behavior:
 * - Seeks and collects specific item types within detection range
 * - Brings collected items to player or note block
 * - Maintains memory of item locations
 * - Triggers dance animations when delivering items
 * - Prioritizes items based on preferences and proximity
 */
public class ItemCollectingBehavior extends SteeringBehavior {

    private static final double ITEM_SEARCH_RADIUS = 32.0;
    private static final double COLLECTION_RANGE = 1.5;
    private static final int MAX_ITEM_MEMORY = 15;
    private static final int ITEM_MEMORY_DURATION = 1200;

    private final List<ItemMemory> itemMemory;
    private final List<ItemTypePreference> preferences;
    private ItemEntity targetItem;
    private BlockPos deliveryLocation;
    private boolean isDelivering;
    private int memoryCleanupTick;

    public ItemCollectingBehavior() {
        this(1.0);
    }

    public ItemCollectingBehavior(double weight) {
        super(weight);
        this.itemMemory = new ArrayList<>();
        this.preferences = new ArrayList<>();
        this.targetItem = null;
        this.deliveryLocation = null;
        this.isDelivering = false;
        this.memoryCleanupTick = 0;

        // Default item preferences
        addPreference(new ItemTypePreference("minecraft:apple", 1.0));
        addPreference(new ItemTypePreference("minecraft:diamond", 1.5));
        addPreference(new ItemTypePreference("minecraft:iron_ingot", 1.2));
        addPreference(new ItemTypePreference("minecraft:gold_ingot", 1.3));
    }

    @Override
    public Vec3d calculate(BehaviorContext context) {
        Entity self = context.getSelf();
        if (!(self instanceof Allay allay)) {
            return new Vec3d();
        }

        Level level = context.getWorld();
        Vec3d position = context.getPosition();

        // Periodic memory cleanup
        cleanupMemory(level.getGameTime());

        // Check if allay is holding an item
        boolean hasItem = !allay.getMainHandItem().isEmpty();

        if (hasItem) {
            // Deliver item to target location
            return calculateDeliveryForce(allay, position, context);
        } else {
            // Seek and collect items
            return calculateCollectionForce(allay, position, level, context);
        }
    }

    /**
     * Calculates steering force toward delivery location.
     */
    private Vec3d calculateDeliveryForce(Allay allay, Vec3d position, BehaviorContext context) {
        if (deliveryLocation == null) {
            deliveryLocation = findDeliveryLocation(allay);
        }

        if (deliveryLocation == null) {
            return new Vec3d();
        }

        Vec3d targetPos = new Vec3d(
            deliveryLocation.getX() + 0.5,
            deliveryLocation.getY(),
            deliveryLocation.getZ() + 0.5
        );

        double distance = position.distanceTo(targetPos);

        // Check if close enough to deliver
        if (distance <= 2.5) {
            onItemDelivered(allay);
            return new Vec3d();
        }

        // Calculate steering force toward delivery location
        Vec3d desired = Vec3d.sub(targetPos, position);
        desired.normalize();
        desired.mult(context.getMaxSpeed());

        Vec3d steer = Vec3d.sub(desired, context.getVelocity());
        return limitForce(steer, context.getMaxForce());
    }

    /**
     * Calculates steering force toward target item.
     */
    private Vec3d calculateCollectionForce(Allay allay, Vec3d position, Level level,
                                           BehaviorContext context) {
        // Find target item if we don't have one
        if (targetItem == null || !targetItem.isAlive()) {
            targetItem = findBestItem(allay, position, level);
        }

        if (targetItem == null) {
            return new Vec3d();
        }

        Vec3d itemPos = Vec3d.fromMinecraftVec3(targetItem.position());
        double distance = position.distanceTo(itemPos);

        // Check if close enough to collect
        if (distance <= COLLECTION_RANGE) {
            collectItem(allay, targetItem);
            targetItem = null;
            return new Vec3d();
        }

        // Calculate steering force toward item
        Vec3d desired = Vec3d.sub(itemPos, position);
        desired.normalize();
        desired.mult(context.getMaxSpeed());

        Vec3d steer = Vec3d.sub(desired, context.getVelocity());
        return limitForce(steer, context.getMaxForce());
    }

    /**
     * Finds the best item to collect based on preferences and proximity.
     */
    private ItemEntity findBestItem(Allay allay, Vec3d position, Level level) {
        // Check memory first for known items
        ItemEntity bestFromMemory = findBestFromMemory(allay, position, level);
        if (bestFromMemory != null) {
            return bestFromMemory;
        }

        // Search for nearby items
        AABB searchBox = new AABB(
            allay.getX() - ITEM_SEARCH_RADIUS,
            allay.getY() - ITEM_SEARCH_RADIUS,
            allay.getZ() - ITEM_SEARCH_RADIUS,
            allay.getX() + ITEM_SEARCH_RADIUS,
            allay.getY() + ITEM_SEARCH_RADIUS,
            allay.getZ() + ITEM_SEARCH_RADIUS
        );

        List<ItemEntity> nearbyItems = level.getEntitiesOfClass(ItemEntity.class, searchBox);

        if (nearbyItems.isEmpty()) {
            return null;
        }

        // Score each item based on preferences and distance
        ItemEntity bestItem = null;
        double bestScore = Double.NEGATIVE_INFINITY;

        for (ItemEntity item : nearbyItems) {
            if (!item.isAlive() || item.hasPickUpDelay()) {
                continue;
            }

            double score = scoreItem(item, position);
            if (score > bestScore) {
                bestScore = score;
                bestItem = item;
            }
        }

        if (bestItem != null) {
            addToMemory(bestItem);
        }

        return bestItem;
    }

    /**
     * Finds the best item from memory.
     */
    private ItemEntity findBestFromMemory(Allay allay, Vec3d position, Level level) {
        ItemEntity bestItem = null;
        double bestScore = Double.NEGATIVE_INFINITY;

        for (ItemMemory memory : itemMemory) {
            if (!memory.isValid(level.getGameTime())) {
                continue;
            }

            ItemEntity item = level.getEntity(memory.itemId);
            if (!(item instanceof ItemEntity itemEntity) || !itemEntity.isAlive()) {
                continue;
            }

            double score = scoreItem(itemEntity, position);
            if (score > bestScore) {
                bestScore = score;
                bestItem = itemEntity;
            }
        }

        return bestItem;
    }

    /**
     * Scores an item based on preferences and distance.
     */
    private double scoreItem(ItemEntity item, Vec3d position) {
        Vec3d itemPos = Vec3d.fromMinecraftVec3(item.position());
        double distance = position.distanceTo(itemPos);

        // Base score inversely proportional to distance
        double distanceScore = 100.0 / (distance + 1.0);

        // Preference bonus
        double preferenceBonus = 0.0;
        String itemId = item.getItem().getItem().toString();
        for (ItemTypePreference pref : preferences) {
            if (itemId.contains(pref.itemId)) {
                preferenceBonus = pref.multiplier * 50.0;
                break;
            }
        }

        return distanceScore + preferenceBonus;
    }

    /**
     * Attempts to collect the target item.
     */
    private void collectItem(Allay allay, ItemEntity item) {
        if (!allay.level().isClientSide) {
            // Pickup item
            allay.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, item.getItem().copy());
            item.discard();

            // Play collection sound
            allay.level().playSound(
                null,
                allay.blockPosition(),
                net.minecraft.sounds.SoundEvents.ALLAY_ITEM_TAKEN,
                net.minecraft.sounds.SoundSource.NEUTRAL,
                1.0f,
                1.0f
            );
        }

        // Determine delivery location
        deliveryLocation = findDeliveryLocation(allay);
        isDelivering = true;
    }

    /**
     * Handles item delivery completion.
     */
    private void onItemDelivered(Allay allay) {
        if (!allay.level().isClientSide) {
            // Play delivery sound
            allay.level().playSound(
                null,
                allay.blockPosition(),
                net.minecraft.sounds.SoundEvents.ALLAY_ITEM_GIVEN,
                net.minecraft.sounds.SoundSource.NEUTRAL,
                1.0f,
                1.0f
            );

            // Spawn heart particles
            spawnHeartParticles(allay);
        }

        // Clear delivery state
        allay.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, net.minecraft.world.item.ItemStack.EMPTY);
        deliveryLocation = null;
        isDelivering = false;
    }

    /**
     * Finds the appropriate delivery location (player or note block).
     */
    private BlockPos findDeliveryLocation(Allay allay) {
        // Check if allay has a liked player
        if (allay.getOwnerUUID() != null) {
            var player = allay.level().getPlayerByUUID(allay.getOwnerUUID());
            if (player != null && player.isAlive()) {
                return player.blockPosition();
            }
        }

        // Fall back to nearest note block
        return findNearestNoteBlock(allay);
    }

    /**
     * Finds the nearest note block within range.
     */
    private BlockPos findNearestNoteBlock(Allay allay) {
        BlockPos allayPos = allay.blockPosition();
        int searchRadius = 32;

        BlockPos nearest = null;
        double nearestDist = Double.MAX_VALUE;

        for (int x = -searchRadius; x <= searchRadius; x++) {
            for (int y = -8; y <= 8; y++) {
                for (int z = -searchRadius; z <= searchRadius; z++) {
                    BlockPos testPos = allayPos.offset(x, y, z);
                    if (allay.level().getBlockState(testPos).is(net.minecraft.world.level.block.Blocks.NOTE_BLOCK)) {
                        double dist = allay.position().distanceTo(net.minecraft.world.phys.Vec3.atCenterOf(testPos));
                        if (dist < nearestDist) {
                            nearestDist = dist;
                            nearest = testPos;
                        }
                    }
                }
            }
        }

        return nearest;
    }

    /**
     * Spawns heart particles around the allay.
     */
    private void spawnHeartParticles(Allay allay) {
        if (allay.level().isClientSide) {
            return;
        }

        for (int i = 0; i < 5; i++) {
            double offsetX = allay.level().random.nextDouble() * 0.5 - 0.25;
            double offsetY = allay.level().random.nextDouble() * 0.5;
            double offsetZ = allay.level().random.nextDouble() * 0.5 - 0.25;

            allay.level().addParticle(
                net.minecraft.core.particles.ParticleTypes.HEART,
                allay.getX() + offsetX,
                allay.getY() + 1.0 + offsetY,
                allay.getZ() + offsetZ,
                0.0, 0.1, 0.0
            );
        }
    }

    /**
     * Adds an item to memory.
     */
    private void addToMemory(ItemEntity item) {
        if (itemMemory.size() >= MAX_ITEM_MEMORY) {
            itemMemory.remove(0);
        }

        itemMemory.add(new ItemMemory(item.getId(), item.blockPosition(),
            item.level().getGameTime() + ITEM_MEMORY_DURATION));
    }

    /**
     * Cleans up expired memory entries.
     */
    private void cleanupMemory(long currentGameTime) {
        memoryCleanupTick++;
        if (memoryCleanupTick < 100) {
            return;
        }
        memoryCleanupTick = 0;

        itemMemory.removeIf(memory -> !memory.isValid(currentGameTime));
    }

    /**
     * Adds an item preference.
     */
    public void addPreference(ItemTypePreference preference) {
        preferences.add(preference);
    }

    /**
     * Clears all item preferences.
     */
    public void clearPreferences() {
        preferences.clear();
    }

    /**
     * Gets the current target item.
     */
    public ItemEntity getTargetItem() {
        return targetItem;
    }

    /**
     * Gets the delivery location.
     */
    public BlockPos getDeliveryLocation() {
        return deliveryLocation;
    }

    /**
     * Checks if currently delivering an item.
     */
    public boolean isDelivering() {
        return isDelivering;
    }

    /**
     * Memory entry for an item.
     */
    private static class ItemMemory {
        final int itemId;
        final BlockPos position;
        final long expireTime;

        ItemMemory(int itemId, BlockPos position, long expireTime) {
            this.itemId = itemId;
            this.position = position;
            this.expireTime = expireTime;
        }

        boolean isValid(long currentTime) {
            return currentTime < expireTime;
        }
    }

    /**
     * Preference for a specific item type.
     */
    public static class ItemTypePreference {
        final String itemId;
        final double multiplier;

        public ItemTypePreference(String itemId, double multiplier) {
            this.itemId = itemId;
            this.multiplier = multiplier;
        }
    }
}
