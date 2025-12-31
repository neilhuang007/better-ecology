package me.javavirtualenv.behavior.sniffer;

import me.javavirtualenv.behavior.core.BehaviorContext;
import me.javavirtualenv.behavior.core.Vec3d;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.animal.sniffer.Sniffer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;

import java.util.HashMap;
import java.util.Map;

/**
 * Digging behavior for sniffers to extract ancient seeds from dirt blocks.
 * Supports biome-specific seed drops and rare ancient variants.
 */
public class DiggingBehavior extends SnifferBehavior {
    private final double diggingSpeed;
    private final double slowingRadius;
    private final int diggingDuration;
    private final int dailyDigLimit;

    private DiggingState state;
    private BlockPos diggingTarget;
    private int diggingTicks;
    private int dailyDigs;
    private long lastDay;

    private static final Map<String, SeedType> BIOME_SEED_MAP = new HashMap<>();

    static {
        BIOME_SEED_MAP.put("minecraft:plains", SeedType.TORCHFLOWER);
        BIOME_SEED_MAP.put("minecraft:forest", SeedType.TORCHFLOWER);
        BIOME_SEED_MAP.put("minecraft:birch_forest", SeedType.TORCHFLOWER);
        BIOME_SEED_MAP.put("minecraft:dark_forest", SeedType.PITCHER_POD);
        BIOME_SEED_MAP.put("minecraft:swamp", SeedType.PITCHER_POD);
        BIOME_SEED_MAP.put("minecraft:jungle", SeedType.PITCHER_POD);
        BIOME_SEED_MAP.put("minecraft:savanna", SeedType.TORCHFLOWER);
        BIOME_SEED_MAP.put("minecraft:taiga", SeedType.TORCHFLOWER);
        BIOME_SEED_MAP.put("minecraft:meadow", SeedType.ANCIENT_MOSS);
        BIOME_SEED_MAP.put("minecraft:cherry_grove", SeedType.RARE_ANCIENT);
    }

    public DiggingBehavior(double smellRadius, int scentPersistenceTicks, int seedMemorySize,
                          double diggingSpeed, double slowingRadius, int diggingDuration, int dailyDigLimit) {
        super(smellRadius, scentPersistenceTicks, seedMemorySize);
        this.diggingSpeed = diggingSpeed;
        this.slowingRadius = slowingRadius;
        this.diggingDuration = diggingDuration;
        this.dailyDigLimit = dailyDigLimit;
        this.state = DiggingState.SEARCHING;
    }

    public DiggingBehavior() {
        this(16.0, 1200, 10, 0.2, 2.0, 80, 6);
    }

    @Override
    protected Vec3d calculateSnifferBehavior(BehaviorContext context) {
        if (!(context.getEntity() instanceof Sniffer sniffer)) {
            return new Vec3d();
        }

        updateDailyLimit(context);
        updateDiggingState(context, sniffer);

        switch (state) {
            case SEARCHING:
                return handleSearching(context);
            case APPROACHING:
                return handleApproaching(context);
            case DIGGING:
                return handleDigging(context, sniffer);
            default:
                return new Vec3d();
        }
    }

    private void updateDailyLimit(BehaviorContext context) {
        long currentDay = context.getLevel().getDayTime() / 24000L;

        if (currentDay != lastDay) {
            dailyDigs = 0;
            lastDay = currentDay;
        }
    }

    private void updateDiggingState(BehaviorContext context, Sniffer sniffer) {
        if (!sniffer.isAlive()) {
            state = DiggingState.SEARCHING;
            return;
        }

        if (dailyDigs >= dailyDigLimit) {
            state = DiggingState.EXHAUSTED;
            return;
        }

        if (state == DiggingState.DIGGING) {
            diggingTicks++;

            if (diggingTicks % 15 == 0) {
                spawnDiggingParticles(context, sniffer);
            }

            if (diggingTicks % 20 == 0) {
                playDiggingSound(context, sniffer);
            }

            if (diggingTicks >= diggingDuration) {
                completeDigging(context, sniffer);
            }
            return;
        }

        if (state == DiggingState.APPROACHING && diggingTarget != null) {
            double distance = context.getPosition().distanceTo(
                diggingTarget.getX() + 0.5,
                diggingTarget.getY(),
                diggingTarget.getZ() + 0.5
            );

            if (distance < 1.5) {
                state = DiggingState.DIGGING;
                diggingTicks = 0;
            }
        }

        if (state == DiggingState.SEARCHING) {
            diggingTarget = findBestDiggingSite(context);

            if (diggingTarget != null) {
                state = DiggingState.APPROACHING;
            }
        }
    }

    private Vec3d handleSearching(BehaviorContext context) {
        return new Vec3d();
    }

    private Vec3d handleApproaching(BehaviorContext context) {
        if (diggingTarget == null) {
            state = DiggingState.SEARCHING;
            return new Vec3d();
        }

        Vec3d targetPos = new Vec3d(
            diggingTarget.getX() + 0.5,
            diggingTarget.getY(),
            diggingTarget.getZ() + 0.5
        );

        return arrive(context.getPosition(), context.getVelocity(), targetPos, diggingSpeed, slowingRadius);
    }

    private Vec3d handleDigging(BehaviorContext context, Sniffer sniffer) {
        return new Vec3d();
    }

    private void completeDigging(BehaviorContext context, Sniffer sniffer) {
        if (diggingTarget != null && !context.getLevel().isClientSide) {
            ServerLevel serverLevel = (ServerLevel) context.getLevel();
            SeedType seedType = determineSeedType(context, diggingTarget);
            spawnSeedItem(serverLevel, diggingTarget, seedType);
            convertDirtToPath(context, diggingTarget);
            addToDiggingMemory(diggingTarget);
            dailyDigs++;
        }

        diggingTicks = 0;
        diggingTarget = null;
        state = DiggingState.SEARCHING;
    }

    private SeedType determineSeedType(BehaviorContext context, BlockPos pos) {
        String biomeId = context.getLevel().getBiome(pos).unwrap().key().location().toString();
        SeedType biomeSeed = BIOME_SEED_MAP.getOrDefault(biomeId, SeedType.TORCHFLOWER);

        double roll = context.getLevel().random.nextDouble();

        if (roll < 0.05) {
            return SeedType.RARE_ANCIENT;
        } else if (roll < 0.15) {
            return SeedType.ANCIENT_MOSS;
        }

        return biomeSeed;
    }

    private void spawnSeedItem(ServerLevel level, BlockPos pos, SeedType seedType) {
        ItemStack stack = switch (seedType) {
            case TORCHFLOWER -> new ItemStack(Items.TORCHFLOWER_SEEDS);
            case PITCHER_POD -> new ItemStack(Items.PITCHER_POD);
            case ANCIENT_MOSS -> new ItemStack(Blocks.MOSS_BLOCK);
            case RARE_ANCIENT -> createRareAncientSeed(level);
        };

        net.minecraft.world.entity.item.ItemEntity itemEntity = new net.minecraft.world.entity.item.ItemEntity(
            level,
            pos.getX() + 0.5,
            pos.getY() + 1.0,
            pos.getZ() + 0.5,
            stack
        );
        itemEntity.setDefaultPickUpDelay();
        level.addFreshEntity(itemEntity);
    }

    private ItemStack createRareAncientSeed(ServerLevel level) {
        ItemStack stack = new ItemStack(Items.ENCHANTED_BOOK);
        stack.setHoverName(net.minecraft.network.chat.Component.literal("Ancient Seeds"));
        return stack;
    }

    private void convertDirtToPath(BehaviorContext context, BlockPos pos) {
        context.getLevel().setBlock(
            pos,
            Blocks.DIRT_PATH.defaultBlockState(),
            3
        );

        context.getLevel().levelEvent(
            2001,
            pos,
            net.minecraft.world.level.block.Block.getId(Blocks.DIRT.defaultBlockState())
        );
    }

    private void spawnDiggingParticles(BehaviorContext context, Sniffer sniffer) {
        if (context.getLevel().isClientSide) {
            return;
        }

        Vec3d pos = context.getPosition();
        for (int i = 0; i < 5; i++) {
            double offsetX = (context.getLevel().random.nextDouble() - 0.5) * 0.5;
            double offsetZ = (context.getLevel().random.nextDouble() - 0.5) * 0.5;

            context.getLevel().addParticle(
                ParticleTypes.BLOCK,
                pos.x + offsetX,
                pos.y + 0.2,
                pos.z + offsetZ,
                0.0, 0.2, 0.0,
                net.minecraft.world.level.block.Block.getId(Blocks.DIRT.defaultBlockState())
            );
        }
    }

    private void playDiggingSound(BehaviorContext context, Sniffer sniffer) {
        context.getLevel().playSound(
            null,
            sniffer.blockPosition(),
            SoundEvents.SNIFFER_DIGGING,
            SoundSource.BLOCKS,
            0.6f,
            1.0f
        );
    }

    public DiggingState getState() {
        return state;
    }

    public BlockPos getDiggingTarget() {
        return diggingTarget;
    }

    public int getDailyDigs() {
        return dailyDigs;
    }

    public int getDailyDigLimit() {
        return dailyDigLimit;
    }

    public boolean isExhausted() {
        return state == DiggingState.EXHAUSTED;
    }

    public void resetExhaustion() {
        if (dailyDigs < dailyDigLimit) {
            state = DiggingState.SEARCHING;
        }
    }

    public enum DiggingState {
        SEARCHING,
        APPROACHING,
        DIGGING,
        EXHAUSTED
    }

    public enum SeedType {
        TORCHFLOWER,
        PITCHER_POD,
        ANCIENT_MOSS,
        RARE_ANCIENT
    }
}
