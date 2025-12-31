package me.javavirtualenv.behavior.fox;

import me.javavirtualenv.behavior.core.BehaviorContext;
import me.javavirtualenv.behavior.core.SteeringBehavior;
import me.javavirtualenv.behavior.core.Vec3d;
import me.javavirtualenv.behavior.fox.FoxItemStorage;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

/**
 * Fox item carrying behavior.
 * <p>
 * Foxes can pick up, carry, and drop items:
 * - Prioritize food items (berries, meat, fish)
 * - Pick up items from the ground
 * - Can give items to trusted players as "gifts"
 * - May steal food from chests or containers
 * - Drop items when threatened or during combat
 * <p>
 * Scientific basis: Foxes are known to cache excess food and carry prey
 * back to their den. This behavior is implemented here as item carrying.
 */
public class FoxItemCarryBehavior extends SteeringBehavior {

    private final double pickupRange;
    private final double searchRange;
    private final double trustThreshold;

    public FoxItemCarryBehavior(double pickupRange, double searchRange, double trustThreshold) {
        this.pickupRange = pickupRange;
        this.searchRange = searchRange;
        this.trustThreshold = trustThreshold;
    }

    public FoxItemCarryBehavior() {
        this(2.0, 16.0, 0.6);
    }

    @Override
    public Vec3d calculate(BehaviorContext context) {
        Mob fox = (Mob) context.getEntity();
        FoxItemStorage storage = FoxItemStorage.get(fox);

        if (storage == null) {
            return new Vec3d();
        }

        // If already carrying an item, no need to look for more
        if (storage.hasItem()) {
            return new Vec3d();
        }

        // Look for items to pick up
        ItemEntity nearestItem = findNearestItem(fox);
        if (nearestItem == null) {
            return new Vec3d();
        }

        Vec3d foxPos = context.getPosition();
        Vec3d itemPos = new Vec3d(nearestItem.getX(), nearestItem.getY(), nearestItem.getZ());
        double distance = foxPos.distanceTo(itemPos);

        // If close enough, pick up the item
        if (distance < pickupRange) {
            pickupItem(fox, nearestItem, storage);
            return new Vec3d();
        }

        // Move toward the item
        return moveTowardItem(foxPos, itemPos, nearestItem.getItem());
    }

    private Vec3d moveTowardItem(Vec3d foxPos, Vec3d itemPos, ItemStack itemStack) {
        Vec3d toItem = Vec3d.sub(itemPos, foxPos);
        toItem.normalize();

        // Adjust speed based on item desirability
        double priority = getItemPriority(itemStack);
        toItem.mult(0.3 + (priority * 0.4));

        return toItem;
    }

    private ItemEntity findNearestItem(Mob fox) {
        ItemEntity nearestItem = null;
        double nearestDistance = Double.MAX_VALUE;

        for (ItemEntity itemEntity : fox.level().getEntitiesOfClass(
                ItemEntity.class,
                fox.getBoundingBox().inflate(searchRange))) {

            // Skip if item is not on ground
            if (!itemEntity.isOnGround()) {
                continue;
            }

            ItemStack itemStack = itemEntity.getItem();
            double priority = getItemPriority(itemStack);

            // Only pick up items with some priority
            if (priority <= 0) {
                continue;
            }

            double distance = fox.position().distanceTo(itemEntity.position());

            // Adjust distance by priority (will go further for better items)
            double adjustedDistance = distance - (priority * 5.0);

            if (adjustedDistance < nearestDistance) {
                nearestDistance = adjustedDistance;
                nearestItem = itemEntity;
            }
        }

        return nearestItem;
    }

    private void pickupItem(Mob fox, ItemEntity itemEntity, FoxItemStorage storage) {
        ItemStack itemStack = itemEntity.getItem().copy();

        // Store item
        storage.setItem(itemStack);

        // Remove from world
        itemEntity.discard();

        // Play pickup sound
        playPickupSound(fox);

        // Spawn particles
        spawnPickupParticles(fox);
    }

    private double getItemPriority(ItemStack itemStack) {
        if (itemStack.isEmpty()) {
            return 0.0;
        }

        // High priority: berries
        if (itemStack.is(Items.SWEET_BERRIES) ||
            itemStack.is(Items.GLOW_BERRIES)) {
            return 1.0;
        }

        // High priority: meat
        if (itemStack.is(Items.RABBIT) ||
            itemStack.is(Items.CHICKEN) ||
            itemStack.is(Items.COD) ||
            itemStack.is(Items.SALMON) ||
            itemStack.is(Items.MUTTON) ||
            itemStack.is(Items.BEEF) ||
            itemStack.is(Items.PORKCHOP)) {
            return 0.9;
        }

        // Medium priority: other food
        if (itemStack.isEdible()) {
            return 0.5;
        }

        // Low priority: other items
        return 0.1;
    }

    /**
     * Check if fox should give item to player as gift.
     */
    public boolean shouldGiftItem(Mob fox, Player player, FoxItemStorage storage) {
        if (!storage.hasItem()) {
            return false;
        }

        // Check if player is trusted
        double trustLevel = getTrustLevel(fox, player);
        if (trustLevel < trustThreshold) {
            return false;
        }

        // Random chance based on trust
        return fox.getRandom().nextDouble() < (trustLevel * 0.1);
    }

    /**
     * Give item to player.
     */
    public void giftItem(Mob fox, Player player, FoxItemStorage storage) {
        ItemStack itemStack = storage.getItem();
        if (itemStack.isEmpty()) {
            return;
        }

        // Drop item near player
        if (!player.level().isClientSide) {
            ItemEntity itemEntity = new ItemEntity(
                player.level(),
                player.getX(),
                player.getY(),
                player.getZ(),
                itemStack.copy()
            );
            itemEntity.setPickUpDelay(10);
            player.level().addFreshEntity(itemEntity);
        }

        // Clear from storage
        storage.setItem(ItemStack.EMPTY);

        // Play sound
        playGiftSound(fox);
    }

    /**
     * Drop carried item.
     */
    public void dropItem(Mob fox, FoxItemStorage storage) {
        ItemStack itemStack = storage.getItem();
        if (itemStack.isEmpty()) {
            return;
        }

        // Drop item
        if (!fox.level().isClientSide) {
            ItemEntity itemEntity = new ItemEntity(
                fox.level(),
                fox.getX(),
                fox.getY(),
                fox.getZ(),
                itemStack.copy()
            );
            itemEntity.setPickUpDelay(20);
            fox.level().addFreshEntity(itemEntity);
        }

        // Clear from storage
        storage.setItem(ItemStack.EMPTY);
    }

    private double getTrustLevel(Mob fox, Player player) {
        // Check if player has tamed this fox
        if (fox instanceof net.minecraft.world.entity.animal.Fox minecraftFox) {
            if (minecraftFox.isTrusting(player)) {
                return 1.0;
            }
        }

        // Base trust on distance and player behavior
        double distance = fox.position().distanceTo(player.position());
        if (distance > 16.0) {
            return 0.0;
        }

        return 1.0 - (distance / 16.0);
    }

    private void playPickupSound(Mob fox) {
        fox.level().playSound(null, fox.blockPosition(), SoundEvents.FOX_SNIFF,
            SoundSource.NEUTRAL, 0.5f, 1.0f);
    }

    private void playGiftSound(Mob fox) {
        fox.level().playSound(null, fox.blockPosition(), SoundEvents.FOX_AMBIENT,
            SoundSource.NEUTRAL, 1.0f, 1.3f);
    }

    private void spawnPickupParticles(Mob fox) {
        if (fox.level().isClientSide) {
            return;
        }

        Vec3 pos = fox.position();
        for (int i = 0; i < 3; i++) {
            fox.level().addParticle(
                net.minecraft.core.particles.ParticleTypes.ITEM,
                pos.x, pos.y + 0.5, pos.z,
                0, 0.1, 0
            );
        }
    }
}
