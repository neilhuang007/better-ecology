package me.javavirtualenv.behavior.production;

import me.javavirtualenv.ecology.EcologyComponent;
import me.javavirtualenv.ecology.handles.production.ResourceProductionHandle;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.animal.Bee;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Bee-specific pollination goal.
 * <p>
 * Bees search for flowers within range, pollinate them, and return to the hive.
 * Different flowers produce different honey types:
 * - Regular flowers: Regular honey
 * - Sunflower: Bright yellow honey, extra saturation
 * - Cornflower: Blue honey, speed boost
 * - Wither rose: Poison honey (dangerous)
 * <p>
 * Pollination also boosts crop growth in nearby farmland.
 */
public class BeePollinationGoal extends ResourceGatheringGoal {

    private static final int POLLINATION_TICKS_NEEDED = 60;
    private static final int CROP_GROWTH_RADIUS = 8;

    private final Bee bee;
    private String currentFlowerType;
    private int pollinationTicks;

    public BeePollinationGoal(Bee bee) {
        super(bee, 22.0, 100, POLLINATION_TICKS_NEEDED);
        this.bee = bee;
    }

    @Override
    public boolean canUse() {
        if (!bee.isAlive()) {
            return false;
        }

        if (bee.isAngry()) {
            return false;
        }

        if (bee.hasNectar()) {
            returningHome = true;
            hasResource = true;
            return true;
        }

        long dayTime = bee.level().getDayTime() % 24000;
        boolean isNight = dayTime >= 13000 && dayTime < 23000;
        boolean isBadWeather = bee.level().isRaining() || bee.level().isThundering();

        if (isNight || isBadWeather) {
            return false;
        }

        return super.canUse();
    }

    @Override
    protected boolean isValidResource(BlockPos pos) {
        if (!bee.level().isLoaded(pos)) {
            return false;
        }

        BlockState state = bee.level().getBlockState(pos);

        if (state.is(BlockTags.FLOWERS)) {
            return true;
        }

        return state.getBlock() == Blocks.SPORE_BLOSSOM ||
               state.getBlock() == Blocks.MANGROVE_PROPAGULE ||
               state.getBlock() == Blocks.AZALEA ||
               state.getBlock() == Blocks.FLOWERING_AZALEA_LEAVES ||
               state.getBlock() == Blocks.PINK_PETALS;
    }

    @Override
    protected void gatherResource() {
        if (targetResourcePos == null) {
            return;
        }

        BlockState flowerState = bee.level().getBlockState(targetResourcePos);
        currentFlowerType = getFlowerType(flowerState);

        pollinationTicks++;

        if (pollinationTicks >= 10 && pollinationTicks % 10 == 0) {
            pollinateNearbyCrops();
        }

        if (pollinationTicks >= gatheringDuration) {
            bee.setHasNectar(true);
            hasResource = true;

            EcologyComponent component = EcologyComponent.getFromEntity(bee);
            if (component != null) {
                ResourceProductionHandle productionHandle = new ResourceProductionHandle();
                productionHandle.setPollinationTarget(bee, component, currentFlowerType);
            }
        }
    }

    @Override
    protected void returnToHome() {
        BlockPos hivePos = bee.getHivePos();
        if (hivePos != null) {
            double distance = bee.position().distanceTo(
                hivePos.getX() + 0.5,
                hivePos.getY(),
                hivePos.getZ() + 0.5
            );

            if (distance > 1.5) {
                bee.getNavigation().moveTo(hivePos.getX(), hivePos.getY(), hivePos.getZ(), 1.0);
            }
        }
    }

    @Override
    protected boolean isNearHome() {
        BlockPos hivePos = bee.getHivePos();
        if (hivePos == null) {
            return true;
        }

        double distance = bee.position().distanceTo(
            hivePos.getX() + 0.5,
            hivePos.getY(),
            hivePos.getZ() + 0.5
        );

        return distance < 2.0;
    }

    @Override
    protected void onResourceDelivered() {
        if (!bee.level().isClientSide) {
            ServerLevel serverLevel = (ServerLevel) bee.level();

            BlockPos hivePos = bee.getHivePos();
            if (hivePos != null) {
                BlockState hiveState = bee.level().getBlockState(hivePos);

                int honeyLevel = 0;
                if (hiveState.hasProperty(net.minecraft.world.level.block.BeehiveBlock.HONEY_LEVEL)) {
                    honeyLevel = hiveState.getValue(net.minecraft.world.level.block.BeehiveBlock.HONEY_LEVEL);
                }

                if (honeyLevel < 5) {
                    serverLevel.setBlock(
                        hivePos,
                        hiveState.setValue(net.minecraft.world.level.block.BeehiveBlock.HONEY_LEVEL, honeyLevel + 1),
                        3
                    );
                }
            }
        }

        bee.setHasNectar(false);
        currentFlowerType = null;
        pollinationTicks = 0;
    }

    /**
     * Gets the flower type for honey production.
     */
    private String getFlowerType(BlockState flowerState) {
        if (flowerState.is(Blocks.SUNFLOWER)) {
            return "sunflower";
        } else if (flowerState.is(Blocks.CORNFLOWER)) {
            return "cornflower";
        } else if (flowerState.is(Blocks.WITHER_ROSE)) {
            return "wither_rose";
        } else if (flowerState.is(Blocks.LILAC)) {
            return "lilac";
        } else if (flowerState.is(Blocks.PEONY)) {
            return "peony";
        } else if (flowerState.is(Blocks.ROSE_BUSH)) {
            return "rose";
        } else if (flowerState.is(Blocks.POPPY)) {
            return "poppy";
        } else if (flowerState.is(Blocks.DANDELION)) {
            return "dandelion";
        } else if (flowerState.is(Blocks.BLUE_ORCHID)) {
            return "orchid";
        } else if (flowerState.is(Blocks.ALLIUM)) {
            return "allium";
        } else if (flowerState.is(Blocks.AZURE_BLUET)) {
            return "azure_bluet";
        } else if (flowerState.is(Blocks.RED_TULIP) ||
                   flowerState.is(Blocks.ORANGE_TULIP) ||
                   flowerState.is(Blocks.WHITE_TULIP) ||
                   flowerState.is(Blocks.PINK_TULIP)) {
            return "tulip";
        } else if (flowerState.is(Blocks.OXEYE_DAISY)) {
            return "daisy";
        } else if (flowerState.is(Blocks.LILY_OF_THE_VALLEY)) {
            return "lily_of_the_valley";
        } else if (flowerState.is(BlockTags.TALL_FLOWERS)) {
            return "tall_flower";
        }

        return "regular";
    }

    /**
     * Pollinates nearby crops, boosting their growth.
     */
    private void pollinateNearbyCrops() {
        if (bee.level().isClientSide || targetResourcePos == null) {
            return;
        }

        ServerLevel serverLevel = (ServerLevel) bee.level();

        for (BlockPos pos : BlockPos.betweenClosed(
            targetResourcePos.getX() - CROP_GROWTH_RADIUS,
            targetResourcePos.getY() - 4,
            targetResourcePos.getZ() - CROP_GROWTH_RADIUS,
            targetResourcePos.getX() + CROP_GROWTH_RADIUS,
            targetResourcePos.getY() + 4,
            targetResourcePos.getZ() + CROP_GROWTH_RADIUS
        )) {
            BlockState state = serverLevel.getBlockState(pos);

            if (isGrowableCrop(state)) {
                if (serverLevel.random.nextFloat() < 0.15f) {
                    net.minecraft.world.level.block.Block block = state.getBlock();
                    block.randomTick(state, serverLevel, pos, serverLevel.random);
                }
            }
        }
    }

    /**
     * Checks if a block is a growable crop.
     */
    private boolean isGrowableCrop(BlockState state) {
        return state.is(net.minecraft.tags.BlockTags.CROPS) ||
               state.is(Blocks.MANGROVE_PROPAGULE) ||
               state.is(Blocks.SWEET_BERRY_BUSH) ||
               state.is(Blocks.CACTUS) ||
               state.is(Blocks.SUGAR_CANE) ||
               state.is(Blocks.BAMBOO) ||
               state.is(Blocks.PUMPKIN_STEM) ||
               state.is(Blocks.MELON_STEM);
    }
}
