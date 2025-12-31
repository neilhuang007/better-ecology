package me.javavirtualenv.behavior.sniffer;

import me.javavirtualenv.behavior.core.BehaviorContext;
import me.javavirtualenv.behavior.core.Vec3d;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.animal.sniffer.Sniffer;

import java.util.UUID;

/**
 * Sniffing behavior for enhanced smell detection and trail following.
 * Sniffers use their nose to detect seed locations and share discoveries with other sniffers.
 */
public class SniffingBehavior extends SnifferBehavior {
    private final double sniffingSpeed;
    private final double slowingRadius;
    private final int sniffingDuration;

    private SniffingState state;
    private BlockPos sniffingTarget;
    private int sniffingTicks;
    private UUID sharedFrom;
    private boolean hasDiscovery;

    public SniffingBehavior(double smellRadius, int scentPersistenceTicks, int seedMemorySize,
                           double sniffingSpeed, double slowingRadius, int sniffingDuration) {
        super(smellRadius, scentPersistenceTicks, seedMemorySize);
        this.sniffingSpeed = sniffingSpeed;
        this.slowingRadius = slowingRadius;
        this.sniffingDuration = sniffingDuration;
        this.state = SniffingState.SEARCHING;
    }

    public SniffingBehavior() {
        this(24.0, 1200, 10, 0.3, 3.0, 60);
    }

    @Override
    protected Vec3d calculateSnifferBehavior(BehaviorContext context) {
        Sniffer sniffer = (Sniffer) context.getEntity();

        updateSniffingState(context, sniffer);

        switch (state) {
            case SEARCHING:
                return handleSearching(context);
            case APPROACHING:
                return handleApproaching(context);
            case SNIFFING:
                return handleSniffing(context, sniffer);
            default:
                return new Vec3d();
        }
    }

    private void updateSniffingState(BehaviorContext context, Sniffer sniffer) {
        if (!sniffer.isAlive()) {
            state = SniffingState.SEARCHING;
            return;
        }

        if (state == SniffingState.SNIFFING) {
            sniffingTicks++;

            if (sniffingTicks % 10 == 0) {
                spawnSniffingParticles(context, sniffer);
            }

            if (sniffingTicks >= sniffingDuration) {
                completeSniffing(context, sniffer);
            }
            return;
        }

        if (state == SniffingState.APPROACHING && sniffingTarget != null) {
            double distance = context.getPosition().distanceTo(
                sniffingTarget.getX() + 0.5,
                sniffingTarget.getY(),
                sniffingTarget.getZ() + 0.5
            );

            if (distance < 2.0) {
                state = SniffingState.SNIFFING;
                sniffingTicks = 0;
                playSniffingSound(context, sniffer);
            }
        }

        if (state == SniffingState.SEARCHING) {
            sniffingTarget = findNearestScent(context);

            if (sniffingTarget != null) {
                state = SniffingState.APPROACHING;
            }
        }
    }

    private Vec3d handleSearching(BehaviorContext context) {
        Vec3d randomWander = getRandomWanderForce(context);
        randomWander.mult(0.3);
        return randomWander;
    }

    private Vec3d handleApproaching(BehaviorContext context) {
        if (sniffingTarget == null) {
            state = SniffingState.SEARCHING;
            return new Vec3d();
        }

        Vec3d targetPos = new Vec3d(
            sniffingTarget.getX() + 0.5,
            sniffingTarget.getY(),
            sniffingTarget.getZ() + 0.5
        );

        return arrive(context.getPosition(), context.getVelocity(), targetPos, sniffingSpeed, slowingRadius);
    }

    private Vec3d handleSniffing(BehaviorContext context, Sniffer sniffer) {
        return new Vec3d();
    }

    private void completeSniffing(BehaviorContext context, Sniffer sniffer) {
        if (sniffingTarget != null) {
            addToDiggingMemory(sniffingTarget);
            shareDiscovery(context, sniffer, sniffingTarget);
            hasDiscovery = true;
        }

        sniffingTicks = 0;
        sniffingTarget = null;
        state = SniffingState.SEARCHING;
    }

    private void spawnSniffingParticles(BehaviorContext context, Sniffer sniffer) {
        if (context.getLevel().isClientSide) {
            return;
        }

        Vec3d pos = context.getPosition();
        double offsetX = (context.getLevel().random.nextDouble() - 0.5) * 0.5;
        double offsetZ = (context.getLevel().random.nextDouble() - 0.5) * 0.5;

        context.getLevel().addParticle(
            ParticleTypes.SNIFFER_SNIFFING,
            pos.x + offsetX,
            pos.y + 1.2,
            pos.z + offsetZ,
            0.0, 0.1, 0.0
        );
    }

    private void playSniffingSound(BehaviorContext context, Sniffer sniffer) {
        context.getLevel().playSound(
            null,
            sniffer.blockPosition(),
            SoundEvents.SNIFFER_SNIFFING,
            SoundSource.NEUTRAL,
            0.8f,
            1.0f
        );
    }

    private void shareDiscovery(BehaviorContext context, Sniffer sniffer, BlockPos discovery) {
        if (context.getLevel().isClientSide) {
            return;
        }

        context.getLevel().getEntitiesOfClass(
            Sniffer.class,
            sniffer.getBoundingBox().inflate(32.0)
        ).forEach(otherSniffer -> {
            if (otherSniffer != sniffer && otherSniffer.isAlive()) {
                otherSniffer.getBrain().setMemory(
                    net.minecraft.world.entity.ai.memory.MemoryModuleType.NEAREST_VISIBLE_DESIRED_BLOCK,
                    net.minecraft.core.GlobalPos.of(context.getLevel().dimension(), discovery)
                );
            }
        });
    }

    private Vec3d getRandomWanderForce(BehaviorContext context) {
        Vec3d wander = new Vec3d(
            (context.getLevel().random.nextDouble() - 0.5) * 2.0,
            0.0,
            (context.getLevel().random.nextDouble() - 0.5) * 2.0
        );
        wander.normalize();
        wander.mult(sniffingSpeed * 0.5);
        return wander;
    }

    public void setSharedDiscovery(BlockPos pos, UUID fromSniffer) {
        this.sniffingTarget = pos;
        this.sharedFrom = fromSniffer;
        this.state = SniffingState.APPROACHING;
    }

    public SniffingState getState() {
        return state;
    }

    public BlockPos getSniffingTarget() {
        return sniffingTarget;
    }

    public boolean hasDiscovery() {
        return hasDiscovery;
    }

    public void setHasDiscovery(boolean hasDiscovery) {
        this.hasDiscovery = hasDiscovery;
    }

    public enum SniffingState {
        SEARCHING,
        APPROACHING,
        SNIFFING
    }
}
