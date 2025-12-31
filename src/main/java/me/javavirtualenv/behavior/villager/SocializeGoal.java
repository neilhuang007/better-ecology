package me.javavirtualenv.behavior.villager;

import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.npc.Villager;

import java.util.EnumSet;
import java.util.List;

/**
 * Goal for villager socialization at meeting point.
 * Share gossip and interact with other villagers.
 */
public class SocializeGoal extends Goal {
    private final Villager villager;
    private final GossipSystem gossipSystem;
    private final DailyRoutine dailyRoutine;

    private Villager socialPartner;
    private int socializeTicks = 0;
    private static final int SOCIALIZE_DURATION = 200; // 10 seconds

    public SocializeGoal(Villager villager, GossipSystem gossipSystem, DailyRoutine dailyRoutine) {
        this.villager = villager;
        this.gossipSystem = gossipSystem;
        this.dailyRoutine = dailyRoutine;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        // Only socialize during socialization time
        return dailyRoutine.getCurrentPhase() == DailyRoutine.RoutinePhase.SOCIALIZING &&
               socializeTicks == 0;
    }

    @Override
    public boolean canContinueToUse() {
        return socializeTicks < SOCIALIZE_DURATION &&
               socialPartner != null &&
               socialPartner.isAlive();
    }

    @Override
    public void start() {
        socialPartner = findSocialPartner();
        socializeTicks = 0;

        if (socialPartner != null) {
            // Look at partner
            villager.getLookControl().setLookAt(socialPartner);
        }
    }

    @Override
    public void stop() {
        // Share gossip before leaving
        if (socialPartner != null) {
            shareGossip(socialPartner);
        }
        socialPartner = null;
        socializeTicks = 0;
    }

    @Override
    public void tick() {
        if (socialPartner == null) {
            return;
        }

        socializeTicks++;

        // Look at partner
        villager.getLookControl().setLookAt(socialPartner);

        // Share gossip periodically
        if (socializeTicks % 60 == 0) {
            shareGossip(socialPartner);
        }

        // Move closer if too far
        double distance = villager.distanceTo(socialPartner);
        if (distance > 3.0) {
            villager.getNavigation().moveTo(
                socialPartner.getX(),
                socialPartner.getY(),
                socialPartner.getZ(),
                0.5
            );
        }
    }

    /**
     * Finds a nearby villager to socialize with.
     */
    private Villager findSocialPartner() {
        List<Villager> nearby = villager.level().getEntitiesOfClass(
            Villager.class,
            villager.getBoundingBox().inflate(8.0)
        );

        // Filter out self and dead villagers
        nearby.removeIf(v -> v == villager || !v.isAlive());

        if (nearby.isEmpty()) {
            return null;
        }

        // Return random nearby villager
        return nearby.get(villager.getRandom().nextInt(nearby.size()));
    }

    /**
     * Shares gossip with another villager.
     */
    private void shareGossip(Villager other) {
        GossipSystem otherGossip = VillagerMixin.getGossipSystem(other);
        if (otherGossip != null) {
            gossipSystem.spreadGossip(other);
        }
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return false;
    }
}
