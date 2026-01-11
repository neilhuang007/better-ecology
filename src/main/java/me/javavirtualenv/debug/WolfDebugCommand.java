package me.javavirtualenv.debug;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import me.javavirtualenv.ecology.EcologyComponent;
import me.javavirtualenv.ecology.api.EcologyAccess;
import me.javavirtualenv.mixin.MobAccessor;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.GoalSelector;
import net.minecraft.world.entity.animal.Wolf;

import java.util.Map;

/**
 * Debug command for inspecting wolf AI goals and internal state.
 * Usage:
 * - /wolfdebug - Shows info about the wolf you're looking at
 * - /wolfdebug <wolf> - Shows info about a specific wolf
 * - /wolfdebug goals - Shows all registered goals
 * - /wolfdebug state - Shows hunger/thirst/condition state
 */
public final class WolfDebugCommand {

    private WolfDebugCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            Commands.literal("wolfdebug")
                // Show info about looked-at wolf
                .executes(WolfDebugCommand::showLookedAtWolf)
                // Show info about specific wolf
                .then(Commands.argument("wolf", EntityArgument.entity())
                    .executes(WolfDebugCommand::showSpecificWolf))
                // Show goals subcommand
                .then(Commands.literal("goals")
                    .executes(WolfDebugCommand::showGoalsOfLookedAt)
                    .then(Commands.argument("wolf", EntityArgument.entity())
                        .executes(WolfDebugCommand::showGoalsOfSpecific)))
                // Show state subcommand
                .then(Commands.literal("state")
                    .executes(WolfDebugCommand::showStateOfLookedAt)
                    .then(Commands.argument("wolf", EntityArgument.entity())
                        .executes(WolfDebugCommand::showStateOfSpecific)))
                // Force trigger behaviors
                .then(Commands.literal("trigger")
                    .then(Commands.literal("thirsty")
                        .executes(WolfDebugCommand::makeThirsty))
                    .then(Commands.literal("hungry")
                        .executes(WolfDebugCommand::makeHungry)))
        );
    }

    private static int showLookedAtWolf(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        Wolf wolf = getLookedAtWolf(source);
        if (wolf == null) {
            source.sendFailure(Component.literal("No wolf found. Look at a wolf or specify one."));
            return 0;
        }
        showWolfInfo(source, wolf);
        return 1;
    }

    private static int showSpecificWolf(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        try {
            Entity entity = EntityArgument.getEntity(context, "wolf");
            if (!(entity instanceof Wolf wolf)) {
                source.sendFailure(Component.literal("Target is not a wolf!"));
                return 0;
            }
            showWolfInfo(source, wolf);
            return 1;
        } catch (Exception e) {
            source.sendFailure(Component.literal("Error: " + e.getMessage()));
            return 0;
        }
    }

    private static int showGoalsOfLookedAt(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        Wolf wolf = getLookedAtWolf(source);
        if (wolf == null) {
            source.sendFailure(Component.literal("No wolf found. Look at a wolf or specify one."));
            return 0;
        }
        showGoalsInfo(source, wolf);
        return 1;
    }

    private static int showGoalsOfSpecific(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        try {
            Entity entity = EntityArgument.getEntity(context, "wolf");
            if (!(entity instanceof Wolf wolf)) {
                source.sendFailure(Component.literal("Target is not a wolf!"));
                return 0;
            }
            showGoalsInfo(source, wolf);
            return 1;
        } catch (Exception e) {
            source.sendFailure(Component.literal("Error: " + e.getMessage()));
            return 0;
        }
    }

    private static int showStateOfLookedAt(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        Wolf wolf = getLookedAtWolf(source);
        if (wolf == null) {
            source.sendFailure(Component.literal("No wolf found. Look at a wolf or specify one."));
            return 0;
        }
        showStateInfo(source, wolf);
        return 1;
    }

    private static int showStateOfSpecific(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        try {
            Entity entity = EntityArgument.getEntity(context, "wolf");
            if (!(entity instanceof Wolf wolf)) {
                source.sendFailure(Component.literal("Target is not a wolf!"));
                return 0;
            }
            showStateInfo(source, wolf);
            return 1;
        } catch (Exception e) {
            source.sendFailure(Component.literal("Error: " + e.getMessage()));
            return 0;
        }
    }

    private static int makeThirsty(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        Wolf wolf = getLookedAtWolf(source);
        if (wolf == null) {
            source.sendFailure(Component.literal("No wolf found. Look at a wolf."));
            return 0;
        }
        setThirst(wolf, 10);
        source.sendSuccess(() -> Component.literal("Set wolf thirst to 10 (very thirsty)"), true);
        return 1;
    }

    private static int makeHungry(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        Wolf wolf = getLookedAtWolf(source);
        if (wolf == null) {
            source.sendFailure(Component.literal("No wolf found. Look at a wolf."));
            return 0;
        }
        setHunger(wolf, 30);
        source.sendSuccess(() -> Component.literal("Set wolf hunger to 30 (very hungry)"), true);
        return 1;
    }

    private static Wolf getLookedAtWolf(CommandSourceStack source) {
        if (!(source.getEntity() instanceof Mob mob)) {
            return null;
        }
        // Find nearest wolf within 6 blocks
        return mob.level().getEntitiesOfClass(
            Wolf.class,
            mob.getBoundingBox().inflate(6.0)
        ).stream().findFirst().orElse(null);
    }

    private static void showWolfInfo(CommandSourceStack source, Wolf wolf) {
        source.sendSuccess(() -> Component.literal("=== Wolf Debug Info ==="), false);
        source.sendSuccess(() -> Component.literal("ID: " + wolf.getId()), false);
        source.sendSuccess(() -> Component.literal("Tamed: " + wolf.isTame()), false);
        source.sendSuccess(() -> Component.literal("Health: " + wolf.getHealth() + "/" + wolf.getMaxHealth()), false);
        source.sendSuccess(() -> Component.literal("Position: " + wolf.blockPosition()), false);

        EcologyComponent component = getComponent(wolf);
        if (component == null) {
            source.sendFailure(Component.literal("No EcologyComponent found! Mixin may have failed."));
            return;
        }

        showStateInfo(source, wolf);
    }

    private static void showGoalsInfo(CommandSourceStack source, Wolf wolf) {
        source.sendSuccess(() -> Component.literal("=== Wolf Goals ==="), false);

        MobAccessor accessor = (MobAccessor) wolf;
        GoalSelector goalSelector = accessor.betterEcology$getGoalSelector();

        source.sendSuccess(() -> Component.literal("Available goals: " + countGoals(goalSelector)), false);

        // Try to get goals via reflection
        try {
            Object availableGoals = getAvailableGoals(goalSelector);
            if (availableGoals instanceof Map<?, ?> goalMap) {
                source.sendSuccess(() -> Component.literal("--- Registered Goals ---"), false);
                for (var entry : goalMap.entrySet()) {
                    Object priority = entry.getKey();
                    var goalWrapper = entry.getValue();
                    Goal goal = getGoalFromWrapper(goalWrapper);
                    if (goal != null) {
                        String goalName = goal.getClass().getSimpleName();
                        String isRunning = isGoalRunning(goalWrapper) ? " [RUNNING]" : "";
                        source.sendSuccess(() -> Component.literal("  Priority " + priority + ": " + goalName + isRunning), false);
                    }
                }
            } else {
                source.sendFailure(Component.literal("Could not access goals list (reflection failed)"));
            }
        } catch (Exception e) {
            source.sendFailure(Component.literal("Error reading goals: " + e.getMessage()));
        }
    }

    private static void showStateInfo(CommandSourceStack source, Wolf wolf) {
        EcologyComponent component = getComponent(wolf);
        if (component == null) {
            source.sendFailure(Component.literal("No EcologyComponent found! Mixin may have failed."));
            return;
        }

        source.sendSuccess(() -> Component.literal("=== Internal State ==="), false);

        // Hunger
        var hungerTag = component.getHandleTag("hunger");
        int hunger = hungerTag.contains("hunger") ? hungerTag.getInt("hunger") : 70;
        source.sendSuccess(() -> Component.literal("Hunger: " + hunger + "/100 " + getStateEmoji(hunger, 75, 50, 25)), false);

        // Thirst
        var thirstTag = component.getHandleTag("thirst");
        int thirst = thirstTag.contains("thirst") ? thirstTag.getInt("thirst") : 100;
        source.sendSuccess(() -> Component.literal("Thirst: " + thirst + "/100 " + getStateEmoji(thirst, 80, 50, 20)), false);

        // Condition
        var conditionTag = component.getHandleTag("condition");
        int condition = conditionTag.contains("condition") ? conditionTag.getInt("condition") : 80;
        source.sendSuccess(() -> Component.literal("Condition: " + condition + "/100"), false);

        // Energy
        var energyTag = component.getHandleTag("energy");
        int energy = energyTag.contains("energy") ? energyTag.getInt("energy") : 100;
        source.sendSuccess(() -> Component.literal("Energy: " + energy + "/100"), false);

        // Social
        var socialTag = component.getHandleTag("social");
        int social = socialTag.contains("social") ? socialTag.getInt("social") : 70;
        source.sendSuccess(() -> Component.literal("Social: " + social + "/100"), false);

        // Entity state flags
        var state = component.state();
        source.sendSuccess(() -> Component.literal("Flags: " +
            "Hungry=" + state.isHungry() + " " +
            "Thirsty=" + state.isThirsty() + " " +
            "Starving=" + state.isStarving() + " " +
            "Lonely=" + state.isLonely()), false);
    }

    private static EcologyComponent getComponent(Wolf wolf) {
        if (!(wolf instanceof EcologyAccess access)) {
            return null;
        }
        return access.betterEcology$getEcologyComponent();
    }

    private static void setHunger(Wolf wolf, int value) {
        EcologyComponent component = getComponent(wolf);
        if (component == null) return;
        component.getHandleTag("hunger").putInt("hunger", value);
    }

    private static void setThirst(Wolf wolf, int value) {
        EcologyComponent component = getComponent(wolf);
        if (component == null) return;
        component.getHandleTag("thirst").putInt("thirst", value);
        component.state().setIsThirsty(value < 30);
    }

    private static String getStateEmoji(int value, int good, int warning, int critical) {
        if (value >= good) return "[GREEN]";
        if (value >= warning) return "[YELLOW]";
        if (value >= critical) return "[ORANGE]";
        return "[RED]";
    }

    // Reflection helpers for GoalSelector (vanilla class has private fields)
    private static Object getAvailableGoals(GoalSelector selector) {
        try {
            var field = GoalSelector.class.getDeclaredField("availableGoals");
            field.setAccessible(true);
            return field.get(selector);
        } catch (Exception e) {
            return null;
        }
    }

    private static int countGoals(GoalSelector selector) {
        var goals = getAvailableGoals(selector);
        if (goals instanceof Map map) {
            return map.size();
        }
        return -1;
    }

    private static Goal getGoalFromWrapper(Object wrapper) {
        try {
            // Goal stores the actual Goal in a field
            var field = wrapper.getClass().getDeclaredField("goal");
            field.setAccessible(true);
            return (Goal) field.get(wrapper);
        } catch (Exception e) {
            return null;
        }
    }

    private static boolean isGoalRunning(Object wrapper) {
        try {
            var field = wrapper.getClass().getDeclaredField("running");
            field.setAccessible(true);
            return field.getBoolean(wrapper);
        } catch (Exception e) {
            return false;
        }
    }
}
