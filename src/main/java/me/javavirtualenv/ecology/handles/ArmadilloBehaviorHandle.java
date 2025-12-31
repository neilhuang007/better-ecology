package me.javavirtualenv.ecology.handles;

import me.javavirtualenv.behavior.armadillo.*;
import me.javavirtualenv.ecology.CodeBasedHandle;
import me.javavirtualenv.ecology.EcologyComponent;
import me.javavirtualenv.ecology.EcologyProfile;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.animal.Wolf;

/**
 * Handle for managing armadillo-specific behaviors.
 * <p>
 * Armadillos have unique behaviors:
 * - Rolling up into ball for defense
 * - Insect foraging with digging
 * - Burrowing for shelter
 * - Predator avoidance with flee-or-roll strategy
 * - Crepuscular/nocturnal activity cycle
 */
public class ArmadilloBehaviorHandle extends CodeBasedHandle {

    @Override
    public String id() {
        return "armadillo_behavior";
    }

    @Override
    public void registerGoals(Mob mob, EcologyComponent component, EcologyProfile profile) {
        ArmadilloComponent armadilloComponent = new ArmadilloComponent(component.getHandleTag("armadillo"));

        // Register roll/unroll goals
        int rollPriority = profile.getInt("behavior.roll_priority", 1);
        int unrollPriority = profile.getInt("behavior.unroll_priority", 2);
        int foragePriority = profile.getInt("behavior.forage_priority", 5);
        int burrowPriority = profile.getInt("behavior.burrow_priority", 6);

        // Roll up goal (high priority - defensive)
        RollUpGoal rollUpGoal = new RollUpGoal(mob, component, profile);
        getGoalSelector(mob).addGoal(rollPriority, rollUpGoal);

        // Unroll goal (lower priority - only when safe)
        UnrollGoal unrollGoal = new UnrollGoal(mob, component, profile);
        getGoalSelector(mob).addGoal(unrollPriority, unrollGoal);

        // Insect foraging goal
        InsectForagingGoal foragingGoal = new InsectForagingGoal(mob, component, profile);
        getGoalSelector(mob).addGoal(foragePriority, foragingGoal);

        // Burrow seeking goal
        SeekBurrowGoal burrowGoal = new SeekBurrowGoal(mob, component, profile);
        getGoalSelector(mob).addGoal(burrowPriority, burrowGoal);

        // Register predator avoidance
        getGoalSelector(mob).addGoal(3, new AvoidEntityGoal<>(
            mob,
            Wolf.class,
            12.0f,
            0.8,
            0.6
        ));

        // Basic survival goals
        getGoalSelector(mob).addGoal(0, new FloatGoal(mob));
        getGoalSelector(mob).addGoal(7, new RandomStrollGoal(mob, 0.5));
    }

    @Override
    public void tick(Mob mob, EcologyComponent component, EcologyProfile profile) {
        ArmadilloComponent armadilloComponent = new ArmadilloComponent(component.getHandleTag("armadillo"));

        // Update panic state
        if (armadilloComponent.isPanicking()) {
            int panicTicks = armadilloComponent.getPanicTicks();
            panicTicks++;

            // Panic lasts for 10 seconds (200 ticks)
            if (panicTicks >= 200) {
                armadilloComponent.setPanicking(false);
                armadilloComponent.setPanicTicks(0);
            } else {
                armadilloComponent.setPanicTicks(panicTicks);
            }
        }

        // Update rolled state damage reduction
        if (armadilloComponent.isRolled()) {
            // While rolled, damage is reduced by 80%
            // This is handled in the damage mixin
        }

        // Decay scent strength over time
        double currentScent = armadilloComponent.getScentStrength();
        if (currentScent > 0) {
            armadilloComponent.setScentStrength(currentScent * 0.98);
        }
    }

    @Override
    public void readNbt(Mob mob, EcologyComponent component, EcologyProfile profile, CompoundTag tag) {
        // NBT is automatically loaded via component.getHandleTag()
    }

    @Override
    public void writeNbt(Mob mob, EcologyComponent component, EcologyProfile profile, CompoundTag tag) {
        CompoundTag handleTag = component.getHandleTag(id());
        tag.put(id(), handleTag.copy());
    }

    /**
     * Helper method to get the goal selector from a mob.
     */
    private net.minecraft.world.entity.ai.goal.GoalSelector getGoalSelector(Mob mob) {
        me.javavirtualenv.mixin.MobAccessor accessor = (me.javavirtualenv.mixin.MobAccessor) mob;
        return accessor.betterEcology$getGoalSelector();
    }
}
