package me.javavirtualenv.behavior.sniffer;

import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.animal.sniffer.Sniffer;

import java.util.EnumSet;

/**
 * AI goal for sniffer sniffing behavior.
 * Integrates SniffingBehavior with Minecraft's goal system.
 */
public class SniffingGoal extends Goal {
    protected final Sniffer sniffer;
    protected final SniffingBehavior behavior;

    public SniffingGoal(Sniffer sniffer) {
        this.sniffer = sniffer;
        this.behavior = new SniffingBehavior();
        this.setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        if (!sniffer.isAlive()) {
            return false;
        }

        if (sniffer.isInWater()) {
            return false;
        }

        return true;
    }

    @Override
    public boolean canContinueToUse() {
        return sniffer.isAlive() && !sniffer.isInWater();
    }

    @Override
    public void tick() {
        behavior.calculate(new me.javavirtualenv.behavior.core.BehaviorContext(sniffer));
    }

    @Override
    public void stop() {
        behavior.setCurrentScentTarget(null);
    }

    public SniffingBehavior getBehavior() {
        return behavior;
    }

    public SniffingBehavior.SniffingState getState() {
        return behavior.getState();
    }

    public boolean hasDiscovery() {
        return behavior.hasDiscovery();
    }
}
