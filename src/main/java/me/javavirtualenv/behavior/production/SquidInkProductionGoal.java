package me.javavirtualenv.behavior.production;

import me.javavirtualenv.ecology.EcologyComponent;
import me.javavirtualenv.ecology.handles.production.ResourceProductionHandle;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.animal.Squid;
import net.minecraft.world.item.Items;

/**
 * Squid ink production goal.
 * <p>
 * Squids produce ink sacs periodically.
 * Glow squids produce glow ink sacs.
 * Production rate is influenced by health and environment.
 */
public class SquidInkProductionGoal extends ResourceGatheringGoal {

    private static final int INK_PRODUCTION_INTERVAL = 3000;
    private static final int INK_PRODUCTION_DURATION = 20;

    private final Squid squid;
    private final boolean isGlowSquid;

    public SquidInkProductionGoal(Squid squid) {
        super(squid, 0.0, INK_PRODUCTION_INTERVAL, INK_PRODUCTION_DURATION);
        this.squid = squid;
        this.isGlowSquid = squid.getClass().getSimpleName().contains("Glow");
    }

    @Override
    public boolean canUse() {
        if (!squid.isAlive()) {
            return false;
        }

        if (!squid.isInWater()) {
            return false;
        }

        long currentTime = squid.level().getGameTime();

        ticksSinceLastSearch++;

        if (ticksSinceLastSearch < searchInterval) {
            return false;
        }

        ticksSinceLastSearch = 0;

        return true;
    }

    @Override
    protected BlockPos findNearestResource() {
        return squid.blockPosition();
    }

    @Override
    protected boolean isValidResource(BlockPos pos) {
        return true;
    }

    @Override
    protected void gatherResource() {
        if (!squid.level().isClientSide) {
            ServerLevel serverLevel = (ServerLevel) squid.level();

            gatheringTicks++;

            if (gatheringTicks >= gatheringDuration) {
                produceInk();
                hasResource = true;
            }
        }
    }

    @Override
    protected void onResourceDelivered() {
        gatheringTicks = 0;

        EcologyComponent component = EcologyComponent.getFromEntity(squid);
        if (component != null) {
            CompoundTag productionData = component.getHandleTag("production");

            double currentAmount = productionData.getDouble("amount");
            double quality = productionData.getDouble("quality");

            double productionAmount = isGlowSquid ? 8.0 : 10.0;

            double newAmount = currentAmount + productionAmount;
            productionData.putDouble("amount", newAmount);
        }
    }

    private void produceInk() {
        if (squid.level().isClientSide) {
            return;
        }

        ServerLevel serverLevel = (ServerLevel) squid.level();

        if (squid.getRandom().nextFloat() < 0.2f) {
            net.minecraft.world.item.ItemStack inkStack = isGlowSquid
                ? new net.minecraft.world.item.ItemStack(Items.GLOW_INK_SAC)
                : new net.minecraft.world.item.ItemStack(Items.INK_SAC);

            net.minecraft.world.entity.item.ItemEntity itemEntity = new net.minecraft.world.entity.item.ItemEntity(
                serverLevel,
                squid.getX(),
                squid.getY(),
                squid.getZ(),
                inkStack
            );
            itemEntity.setDefaultPickUpDelay();
            serverLevel.addFreshEntity(itemEntity);
        }
    }

    @Override
    protected BlockPos getHomePosition() {
        return squid.blockPosition();
    }
}
