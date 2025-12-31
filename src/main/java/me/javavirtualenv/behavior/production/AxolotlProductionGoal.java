package me.javavirtualenv.behavior.production;

import me.javavirtualenv.ecology.EcologyComponent;
import me.javavirtualenv.ecology.handles.production.ResourceProductionHandle;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.animal.axolotl.Axolotl;
import net.minecraft.world.item.Items;

/**
 * Axolotl slime production goal.
 * <p>
 * Axolotls produce slime balls through grooming behavior.
 * Playful behavior increases production rate.
 * Can be "milked" with a bottle for slime balls.
 */
public class AxolotlProductionGoal extends ResourceGatheringGoal {

    private static final int GROOMING_INTERVAL = 6000;
    private static final int GROOMING_DURATION = 40;

    private final Axolotl axolotl;
    private long lastGroomingTime;
    private boolean isPlaying;
    private int playTicks;

    public AxolotlProductionGoal(Axolotl axolotl) {
        super(axolotl, 8.0, GROOMING_INTERVAL, GROOMING_DURATION);
        this.axolotl = axolotl;
        this.lastGroomingTime = 0;
        this.isPlaying = false;
    }

    @Override
    public boolean canUse() {
        if (!axolotl.isAlive()) {
            return false;
        }

        if (!axolotl.isInWater()) {
            return false;
        }

        long currentTime = axolotl.level().getGameTime();

        if (axolotl.isDeadOrDying()) {
            return false;
        }

        ticksSinceLastSearch++;

        if (ticksSinceLastSearch < searchInterval) {
            return false;
        }

        ticksSinceLastSearch = 0;

        return true;
    }

    @Override
    protected BlockPos findNearestResource() {
        return axolotl.blockPosition();
    }

    @Override
    protected boolean isValidResource(BlockPos pos) {
        return true;
    }

    @Override
    protected void gatherResource() {
        if (!axolotl.level().isClientSide) {
            ServerLevel serverLevel = (ServerLevel) axolotl.level();

            if (axolotl.getRandom().nextFloat() < 0.1f) {
                isPlaying = true;
                playTicks = 0;
            }

            gatheringTicks++;

            if (isPlaying) {
                playTicks++;

                if (playTicks % 20 == 0) {
                    axolotl.level().playSound(
                        null,
                        axolotl.blockPosition(),
                        net.minecraft.sounds.SoundEvents.AXOLOTL_SPLASH,
                        net.minecraft.sounds.SoundSource.BLOCKS,
                        0.3f,
                        1.0f + axolotl.getRandom().nextFloat() * 0.2f
                    );
                }

                if (playTicks >= 60) {
                    isPlaying = false;
                    playTicks = 0;
                }
            }
        }

        if (gatheringTicks >= gatheringDuration) {
            produceSlime();
            hasResource = true;
            lastGroomingTime = axolotl.level().getGameTime();
        }
    }

    @Override
    protected void onResourceDelivered() {
        gatheringTicks = 0;

        EcologyComponent component = EcologyComponent.getFromEntity(axolotl);
        if (component != null) {
            CompoundTag productionData = component.getHandleTag("production");

            double currentAmount = productionData.getDouble("amount");
            double quality = productionData.getDouble("quality");

            double productionAmount = 5.0;

            if (isPlaying) {
                productionAmount *= 1.5;
            }

            double newAmount = currentAmount + productionAmount;
            productionData.putDouble("amount", newAmount);
        }
    }

    private void produceSlime() {
        if (axolotl.level().isClientSide) {
            return;
        }

        ServerLevel serverLevel = (ServerLevel) axolotl.level();

        if (axolotl.getRandom().nextFloat() < 0.15f) {
            net.minecraft.world.entity.item.ItemEntity itemEntity = new net.minecraft.world.entity.item.ItemEntity(
                serverLevel,
                axolotl.getX(),
                axolotl.getY(),
                axolotl.getZ(),
                new net.minecraft.world.item.ItemStack(Items.SLIME_BALL)
            );
            itemEntity.setDefaultPickUpDelay();
            serverLevel.addFreshEntity(itemEntity);
        }
    }

    @Override
    protected BlockPos getHomePosition() {
        return axolotl.blockPosition();
    }

    public boolean isPlaying() {
        return isPlaying;
    }
}
