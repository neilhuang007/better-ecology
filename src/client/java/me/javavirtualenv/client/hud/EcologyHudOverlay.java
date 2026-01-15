package me.javavirtualenv.client.hud;

import me.javavirtualenv.behavior.core.AnimalThresholds;
import me.javavirtualenv.client.network.ClientEcologyPacketHandler;
import me.javavirtualenv.mixin.MobAccessor;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.GoalSelector;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.lang.reflect.Field;
import java.util.Set;

/**
 * Client-side HUD overlay that displays entity stats when looking at an animal.
 * Shows health, hunger, thirst, current AI goals, and pathfinding status.
 */
public class EcologyHudOverlay implements HudRenderCallback {

    private static boolean enabled = false;

    /**
     * Toggles the overlay on/off.
     */
    public static void toggle() {
        enabled = !enabled;
    }

    /**
     * Sets the overlay enabled state.
     */
    public static void setEnabled(boolean value) {
        enabled = value;
    }

    /**
     * Gets the overlay enabled state.
     */
    public static boolean isEnabled() {
        return enabled;
    }

    @Override
    public void onHudRender(GuiGraphics guiGraphics, DeltaTracker deltaTracker) {
        if (!enabled) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null) {
            return;
        }

        HitResult hitResult = minecraft.hitResult;
        if (hitResult == null || hitResult.getType() != HitResult.Type.ENTITY) {
            return;
        }

        Entity targetEntity = ((EntityHitResult) hitResult).getEntity();
        if (!(targetEntity instanceof Mob mob)) {
            return;
        }

        renderEntityStats(guiGraphics, minecraft, mob);
    }

    private void renderEntityStats(GuiGraphics guiGraphics, Minecraft minecraft, Mob mob) {
        Font font = minecraft.font;
        int screenWidth = minecraft.getWindow().getGuiScaledWidth();
        int screenHeight = minecraft.getWindow().getGuiScaledHeight();

        // Position overlay on right side of screen
        int x = screenWidth - 220;
        int y = 10;
        int lineHeight = 10;

        // Get synced needs data
        ClientEcologyPacketHandler.AnimalNeedsData needsData =
            ClientEcologyPacketHandler.getNeedsData(mob.getId());

        // Build overlay text
        int currentY = y;

        // Title
        String title = "§6§l" + mob.getType().getDescription().getString();
        guiGraphics.drawString(font, title, x, currentY, 0xFFFFFF);
        currentY += lineHeight + 2;

        // Entity ID
        String idText = "§7ID: §f" + mob.getId();
        guiGraphics.drawString(font, idText, x, currentY, 0xFFFFFF);
        currentY += lineHeight;

        // Health
        float health = mob.getHealth();
        float maxHealth = mob.getMaxHealth();
        String healthColor = getHealthColor(health, maxHealth);
        String healthText = String.format("§7Health: %s%.1f§7/§f%.1f", healthColor, health, maxHealth);
        guiGraphics.drawString(font, healthText, x, currentY, 0xFFFFFF);
        currentY += lineHeight;

        // Age
        String ageText = "§7Age: §f" + (mob.isBaby() ? "Baby" : "Adult");
        guiGraphics.drawString(font, ageText, x, currentY, 0xFFFFFF);
        currentY += lineHeight;

        // Hunger and Thirst (if data available)
        if (needsData != null) {
            float hunger = needsData.hunger();
            float thirst = needsData.thirst();

            String hungerColor = getNeedsColor(hunger);
            String hungerStatus = getHungerStatus(hunger);
            String hungerText = String.format("§7Hunger: %s%.1f §7(%s§7)", hungerColor, hunger, hungerStatus);
            guiGraphics.drawString(font, hungerText, x, currentY, 0xFFFFFF);
            currentY += lineHeight;

            String thirstColor = getNeedsColor(thirst);
            String thirstStatus = getThirstStatus(thirst);
            String thirstText = String.format("§7Thirst: %s%.1f §7(%s§7)", thirstColor, thirst, thirstStatus);
            guiGraphics.drawString(font, thirstText, x, currentY, 0xFFFFFF);
            currentY += lineHeight;
        } else {
            guiGraphics.drawString(font, "§7Hunger: §8No data", x, currentY, 0xFFFFFF);
            currentY += lineHeight;
            guiGraphics.drawString(font, "§7Thirst: §8No data", x, currentY, 0xFFFFFF);
            currentY += lineHeight;
        }

        // Current goal
        currentY += 2;
        guiGraphics.drawString(font, "§7Current Goals:", x, currentY, 0xFFFFFF);
        currentY += lineHeight;

        String currentGoal = getCurrentGoal(mob);
        if (currentGoal != null && !currentGoal.isEmpty()) {
            // Split long goal names into multiple lines
            String[] lines = wrapText(currentGoal, 30);
            for (String line : lines) {
                guiGraphics.drawString(font, "  §e" + line, x, currentY, 0xFFFFFF);
                currentY += lineHeight;
            }
        } else {
            guiGraphics.drawString(font, "  §8None", x, currentY, 0xFFFFFF);
            currentY += lineHeight;
        }

        // Target entity
        if (mob.getTarget() != null) {
            currentY += 2;
            String targetText = "§7Target: §c" + mob.getTarget().getName().getString();
            guiGraphics.drawString(font, targetText, x, currentY, 0xFFFFFF);
            currentY += lineHeight;
        }

        // Pathfinding status
        currentY += 2;
        guiGraphics.drawString(font, "§7Pathfinding:", x, currentY, 0xFFFFFF);
        currentY += lineHeight;

        PathNavigation navigation = mob.getNavigation();
        Path currentPath = navigation.getPath();

        if (currentPath != null && !navigation.isDone()) {
            Vec3 target = Vec3.atBottomCenterOf(currentPath.getTarget());
            String pathText = String.format("  §bTo: §f(%.1f, %.1f, %.1f)",
                target.x, target.y, target.z);
            guiGraphics.drawString(font, pathText, x, currentY, 0xFFFFFF);
            currentY += lineHeight;

            // Distance to target
            double distance = mob.position().distanceTo(target);
            String distText = String.format("  §7Distance: §f%.1f blocks", distance);
            guiGraphics.drawString(font, distText, x, currentY, 0xFFFFFF);
            currentY += lineHeight;

            // Check if stuck
            if (navigation.isStuck()) {
                guiGraphics.drawString(font, "  §cStatus: Stuck", x, currentY, 0xFFFFFF);
                currentY += lineHeight;
            } else {
                guiGraphics.drawString(font, "  §aStatus: Following", x, currentY, 0xFFFFFF);
                currentY += lineHeight;
            }
        } else {
            guiGraphics.drawString(font, "  §8No active path", x, currentY, 0xFFFFFF);
            currentY += lineHeight;
        }
    }

    private String getHealthColor(float health, float maxHealth) {
        float percent = health / maxHealth;
        if (percent >= 0.7f) return "§a";
        if (percent >= 0.4f) return "§e";
        return "§c";
    }

    private String getNeedsColor(float value) {
        if (value >= 60f) return "§a";
        if (value >= 30f) return "§e";
        return "§c";
    }

    private String getHungerStatus(float hunger) {
        if (hunger < AnimalThresholds.STARVING) return "§4Starving";
        if (hunger < AnimalThresholds.HUNGRY) return "§eHungry";
        if (hunger >= AnimalThresholds.SATISFIED) return "§aSatisfied";
        return "§fNormal";
    }

    private String getThirstStatus(float thirst) {
        if (thirst < AnimalThresholds.DEHYDRATED) return "§4Dehydrated";
        if (thirst < AnimalThresholds.THIRSTY) return "§eThirsty";
        if (thirst >= AnimalThresholds.HYDRATED) return "§aHydrated";
        return "§fNormal";
    }

    private String getCurrentGoal(Mob mob) {
        try {
            // Use MobAccessor to get goal selector (cleaner than reflection)
            GoalSelector goalSelector = ((MobAccessor) mob).getGoalSelector();

            // Still need reflection for availableGoals field
            Field availableGoalsField = GoalSelector.class.getDeclaredField("availableGoals");
            availableGoalsField.setAccessible(true);

            @SuppressWarnings("unchecked")
            Set<Object> availableGoals = (Set<Object>) availableGoalsField.get(goalSelector);

            StringBuilder goals = new StringBuilder();
            int count = 0;

            for (Object wrappedGoal : availableGoals) {
                if (count >= 3) break; // Limit to 3 goals

                try {
                    Field goalField = wrappedGoal.getClass().getDeclaredField("goal");
                    goalField.setAccessible(true);
                    Goal goal = (Goal) goalField.get(wrappedGoal);

                    Field isRunningField = wrappedGoal.getClass().getDeclaredField("isRunning");
                    isRunningField.setAccessible(true);
                    boolean isRunning = isRunningField.getBoolean(wrappedGoal);

                    if (isRunning) {
                        if (count > 0) goals.append(", ");
                        String goalName = goal.getClass().getSimpleName()
                            .replace("Goal", "")
                            .replaceAll("([A-Z])", " $1")
                            .trim();
                        goals.append(goalName);
                        count++;
                    }
                } catch (Exception e) {
                    // Ignore individual goal errors
                }
            }

            return goals.toString();
        } catch (Exception e) {
            return "Unknown";
        }
    }

    private String[] wrapText(String text, int maxLength) {
        if (text.length() <= maxLength) {
            return new String[]{text};
        }

        String[] words = text.split(" ");
        StringBuilder current = new StringBuilder();
        int lineCount = 0;

        // Count how many lines we need
        for (String word : words) {
            if (current.length() + word.length() + 1 > maxLength) {
                lineCount++;
                current = new StringBuilder(word);
            } else {
                if (current.length() > 0) current.append(" ");
                current.append(word);
            }
        }
        lineCount++; // Last line

        // Build the lines
        String[] lines = new String[lineCount];
        current = new StringBuilder();
        int lineIndex = 0;

        for (String word : words) {
            if (current.length() + word.length() + 1 > maxLength) {
                lines[lineIndex++] = current.toString();
                current = new StringBuilder(word);
            } else {
                if (current.length() > 0) current.append(" ");
                current.append(word);
            }
        }
        lines[lineIndex] = current.toString();

        return lines;
    }
}
