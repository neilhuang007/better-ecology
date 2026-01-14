package me.javavirtualenv.mixin;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.GoalSelector;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Accessor mixin to access protected fields in Mob class.
 */
@Mixin(Mob.class)
public interface MobAccessor {

    /**
     * Gets the goal selector for this mob.
     */
    @Accessor("goalSelector")
    GoalSelector getGoalSelector();

    /**
     * Gets the target selector for this mob.
     */
    @Accessor("targetSelector")
    GoalSelector getTargetSelector();
}
