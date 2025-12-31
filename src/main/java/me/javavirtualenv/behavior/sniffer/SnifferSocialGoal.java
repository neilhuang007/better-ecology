package me.javavirtualenv.behavior.sniffer;

import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.animal.sniffer.Sniffer;

import java.util.EnumSet;

/**
 * AI goal for sniffer social behaviors including teaching and communication.
 * Integrates SnifferSocialBehavior with Minecraft's goal system.
 */
public class SnifferSocialGoal extends Goal {
    protected final Sniffer sniffer;
    protected final SnifferSocialBehavior behavior;

    public SnifferSocialGoal(Sniffer sniffer) {
        this.sniffer = sniffer;
        this.behavior = new SnifferSocialBehavior();
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (!sniffer.isAlive()) {
            return false;
        }

        return true;
    }

    @Override
    public boolean canContinueToUse() {
        return sniffer.isAlive();
    }

    @Override
    public void tick() {
        behavior.calculate(new me.javavirtualenv.behavior.core.BehaviorContext(sniffer));
    }

    @Override
    public void stop() {
    }

    public SnifferSocialBehavior getBehavior() {
        return behavior;
    }

    public boolean isTeaching() {
        return behavior.isTeaching();
    }

    public void shareDiscovery(net.minecraft.core.BlockPos discovery) {
        behavior.shareDiscoveryWithGroup(
            new me.javavirtualenv.behavior.core.BehaviorContext(sniffer),
            discovery
        );
    }
}
