package me.javavirtualenv.behavior.sniffer;

import me.javavirtualenv.behavior.core.BehaviorContext;
import me.javavirtualenv.behavior.core.Vec3d;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.sniffer.Sniffer;

import java.util.List;
import java.util.UUID;

/**
 * Social behavior for sniffers including parent teaching and group communication.
 * Parents teach babies to dig, and sniffers share seed discoveries.
 */
public class SnifferSocialBehavior extends SnifferBehavior {
    private final double teachingRange;
    private final double communicationRange;
    private final double followSpeed;
    private final int teachingDuration;

    private boolean isTeaching;
    private int teachingTicks;
    private UUID currentStudent;
    private BlockPos sharedDiscovery;

    public SnifferSocialBehavior(double teachingRange, double communicationRange,
                                double followSpeed, int teachingDuration) {
        super(24.0, 1200, 10);
        this.teachingRange = teachingRange;
        this.communicationRange = communicationRange;
        this.followSpeed = followSpeed;
        this.teachingDuration = teachingDuration;
    }

    public SnifferSocialBehavior() {
        this(16.0, 32.0, 0.25, 200);
    }

    @Override
    protected Vec3d calculateSnifferBehavior(BehaviorContext context) {
        if (!(context.getEntity() instanceof Sniffer sniffer)) {
            return new Vec3d();
        }

        if (!sniffer.isAlive()) {
            isTeaching = false;
            return new Vec3d();
        }

        if (sniffer.isBaby()) {
            return handleBabyBehavior(context, sniffer);
        }

        return handleAdultBehavior(context, sniffer);
    }

    private Vec3d handleBabyBehavior(BehaviorContext context, Sniffer baby) {
        Sniffer mother = findMother(baby);

        if (mother == null || !mother.isAlive()) {
            return followNearestAdult(context, baby);
        }

        double distanceToMother = context.getPosition().distanceTo(
            mother.getX(), mother.getY(), mother.getZ()
        );

        if (distanceToMother > 8.0) {
            Vec3d motherPos = new Vec3d(mother.getX(), mother.getY(), mother.getZ());
            return arrive(context.getPosition(), context.getVelocity(), motherPos, followSpeed, 2.0);
        }

        if (isMotherTeaching(mother) && distanceToMother < teachingRange) {
            return watchMotherDig(context, baby);
        }

        return new Vec3d();
    }

    private Sniffer findMother(Sniffer baby) {
        List<Sniffer> nearbySniffers = baby.level().getEntitiesOfClass(
            Sniffer.class,
            baby.getBoundingBox().inflate(32.0)
        );

        for (Sniffer sniffer : nearbySniffers) {
            if (!sniffer.isBaby() && sniffer.isAlive()) {
                return sniffer;
            }
        }

        return null;
    }

    private Vec3d followNearestAdult(BehaviorContext context, Sniffer baby) {
        List<Sniffer> nearbyAdults = baby.level().getEntitiesOfClass(
            Sniffer.class,
            baby.getBoundingBox().inflate(32.0)
        );

        Sniffer nearestAdult = null;
        double nearestDistance = Double.MAX_VALUE;

        for (Sniffer adult : nearbyAdults) {
            if (!adult.isBaby() && adult.isAlive()) {
                double distance = context.getPosition().distanceTo(
                    adult.getX(), adult.getY(), adult.getZ()
                );

                if (distance < nearestDistance) {
                    nearestDistance = distance;
                    nearestAdult = adult;
                }
            }
        }

        if (nearestAdult != null && nearestDistance < 16.0) {
            Vec3d adultPos = new Vec3d(nearestAdult.getX(), nearestAdult.getY(), nearestAdult.getZ());
            return arrive(context.getPosition(), context.getVelocity(), adultPos, followSpeed, 2.0);
        }

        return new Vec3d();
    }

    private boolean isMotherTeaching(Sniffer mother) {
        return false;
    }

    private Vec3d watchMotherDig(BehaviorContext context, Sniffer baby) {
        return new Vec3d();
    }

    private Vec3d handleAdultBehavior(BehaviorContext context, Sniffer adult) {
        if (isTeaching) {
            teachingTicks++;

            if (teachingTicks >= teachingDuration) {
                isTeaching = false;
                teachingTicks = 0;
                currentStudent = null;
            }

            return new Vec3d();
        }

        List<Sniffer> nearbyBabies = context.getLevel().getEntitiesOfClass(
            Sniffer.class,
            adult.getBoundingBox().inflate(teachingRange)
        );

        for (Sniffer baby : nearbyBabies) {
            if (baby.isBaby() && baby.isAlive() && shouldTeachBaby(adult, baby)) {
                startTeaching(context, adult, baby);
                break;
            }
        }

        return new Vec3d();
    }

    private boolean shouldTeachBaby(Sniffer adult, Sniffer baby) {
        return adult.level().random.nextDouble() < 0.02;
    }

    private void startTeaching(BehaviorContext context, Sniffer adult, Sniffer baby) {
        isTeaching = true;
        teachingTicks = 0;
        currentStudent = baby.getUUID();
    }

    public void shareDiscoveryWithGroup(BehaviorContext context, BlockPos discovery) {
        if (!(context.getEntity() instanceof Sniffer sniffer)) {
            return;
        }

        this.sharedDiscovery = discovery;

        context.getLevel().getEntitiesOfClass(
            Sniffer.class,
            sniffer.getBoundingBox().inflate(communicationRange)
        ).forEach(otherSniffer -> {
            if (otherSniffer != sniffer && otherSniffer.isAlive()) {
                notifyOfDiscovery(context, otherSniffer, discovery);
            }
        });
    }

    private void notifyOfDiscovery(BehaviorContext context, Sniffer sniffer, BlockPos discovery) {
        Vec3d discoveryPos = new Vec3d(
            discovery.getX() + 0.5,
            discovery.getY(),
            discovery.getZ() + 0.5
        );

        detectedScents.add(new ScentMarker(discoveryPos, discovery, 0));
    }

    public boolean isTeaching() {
        return isTeaching;
    }

    public void setTeaching(boolean teaching) {
        this.isTeaching = teaching;
    }

    public UUID getCurrentStudent() {
        return currentStudent;
    }

    public BlockPos getSharedDiscovery() {
        return sharedDiscovery;
    }

    public void setSharedDiscovery(BlockPos pos) {
        this.sharedDiscovery = pos;
    }
}
