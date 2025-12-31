package me.javavirtualenv.behavior.production;

import me.javavirtualenv.ecology.EcologyComponent;
import me.javavirtualenv.ecology.handles.production.ResourceProductionHandle;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.UUID;

/**
 * Allay item collection bonus goal.
 * <p>
 * Allays provide enhanced item duplication for note block interactions.
 * They can improve item quality and provide special bonuses when dancing.
 * <p>
 * Production features:
 * - Enhanced item duplication chance
 * - Dance celebrations when delivering items
 * - Item quality improvement based on happiness
 */
public class AllayCollectionGoal extends ResourceGatheringGoal {

    private final net.minecraft.world.entity.animal.allay.Allay allay;
    private UUID likedPlayerUuid;
    private boolean isDancing;
    private int danceTicks;

    public AllayCollectionGoal(net.minecraft.world.entity.animal.allay.Allay allay) {
        super(allay, 32.0, 50, 20);
        this.allay = allay;
        this.isDancing = false;
    }

    @Override
    public boolean canUse() {
        if (!allay.isAlive()) {
            return false;
        }

        return true;
    }

    @Override
    protected BlockPos findNearestResource() {
        if (likedPlayerUuid == null) {
            return null;
        }

        Player player = allay.level().getPlayerByUUID(likedPlayerUuid);
        if (player == null) {
            return null;
        }

        return player.blockPosition();
    }

    @Override
    protected boolean isValidResource(BlockPos pos) {
        return true;
    }

    @Override
    protected void gatherResource() {
        ItemStack heldItem = allay.getMainHandItem();

        if (!heldItem.isEmpty()) {
            hasResource = true;
            gatheringTicks++;

            if (gatheringTicks >= 10 && !isDancing) {
                startDancing();
            }
        }
    }

    @Override
    protected void returnToHome() {
        if (likedPlayerUuid == null) {
            return;
        }

        Player player = allay.level().getPlayerByUUID(likedPlayerUuid);
        if (player == null) {
            return;
        }

        double distance = allay.position().distanceTo(player.position());

        if (distance > 2.0) {
            allay.getNavigation().moveTo(player, 1.0);
        }
    }

    @Override
    protected boolean isNearHome() {
        if (likedPlayerUuid == null) {
            return true;
        }

        Player player = allay.level().getPlayerByUUID(likedPlayerUuid);
        if (player == null) {
            return true;
        }

        double distance = allay.position().distanceTo(player.position());
        return distance < 2.5;
    }

    @Override
    protected void onResourceDelivered() {
        if (isDancing) {
            performDance();
        }

        ItemStack heldItem = allay.getMainHandItem();

        if (!heldItem.isEmpty()) {
            ItemStack duplicatedItem = tryDuplicateItem(heldItem);

            if (!duplicatedItem.isEmpty() && !allay.level().isClientSide) {
                ServerLevel serverLevel = (ServerLevel) allay.level();

                ItemEntity itemEntity = new ItemEntity(
                    serverLevel,
                    allay.getX(),
                    allay.getY(),
                    allay.getZ(),
                    duplicatedItem
                );
                itemEntity.setDefaultPickUpDelay();
                serverLevel.addFreshEntity(itemEntity);
            }

            allay.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        }

        EcologyComponent component = EcologyComponent.getFromEntity(allay);
        if (component != null) {
            CompoundTag productionData = component.getHandleTag("production");

            double currentAmount = productionData.getDouble("amount");
            double quality = productionData.getDouble("quality");

            double bonusAmount = isDancing ? 15.0 : 10.0;
            double newAmount = currentAmount + bonusAmount;
            productionData.putDouble("amount", newAmount);
            productionData.putDouble("quality", Math.min(2.0, quality + 0.05));
        }

        gatheringTicks = 0;
        isDancing = false;
        danceTicks = 0;
    }

    /**
     * Starts dancing behavior.
     */
    private void startDancing() {
        isDancing = true;
        danceTicks = 0;

        if (!allay.level().isClientSide) {
            allay.level().playSound(
                null,
                allay.blockPosition(),
                net.minecraft.sounds.SoundEvents.ALLAY_ITEM_TAKEN,
                net.minecraft.sounds.SoundSource.NEUTRAL,
                1.0f,
                1.0f
            );
        }
    }

    /**
     * Performs dance animation and effects.
     */
    private void performDance() {
        if (allay.level().isClientSide) {
            return;
        }

        ServerLevel serverLevel = (ServerLevel) allay.level();

        for (int i = 0; i < 5; i++) {
            double offsetX = serverLevel.random.nextDouble() * 0.5 - 0.25;
            double offsetY = serverLevel.random.nextDouble() * 0.5;
            double offsetZ = serverLevel.random.nextDouble() * 0.5 - 0.25;

            serverLevel.addParticle(
                net.minecraft.core.particles.ParticleTypes.HAPPY_VILLAGER,
                allay.getX() + offsetX,
                allay.getY() + offsetY,
                allay.getZ() + offsetZ,
                0.0, 0.1, 0.0
            );
        }
    }

    /**
     * Tries to duplicate an item with a chance based on allay's happiness.
     */
    private ItemStack tryDuplicateItem(ItemStack original) {
        EcologyComponent component = EcologyComponent.getFromEntity(allay);
        if (component == null) {
            return ItemStack.EMPTY;
        }

        CompoundTag socialData = component.getHandleTag("social");
        double socialValue = socialData.getDouble("value");

        double duplicationChance = 0.1 + (socialValue / 100.0) * 0.15;

        if (allay.getRandom().nextDouble() < duplicationChance) {
            ItemStack copy = original.copy();
            copy.setCount(1);
            return copy;
        }

        return ItemStack.EMPTY;
    }

    /**
     * Sets the liked player for this allay.
     */
    public void setLikedPlayer(UUID playerUuid) {
        this.likedPlayerUuid = playerUuid;
    }

    /**
     * Gets whether the allay is currently dancing.
     */
    public boolean isDancing() {
        return isDancing;
    }
}
