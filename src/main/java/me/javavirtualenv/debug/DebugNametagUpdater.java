package me.javavirtualenv.debug;

import me.javavirtualenv.ecology.EcologyComponent;
import me.javavirtualenv.ecology.EcologyHooks;
import me.javavirtualenv.ecology.state.EntityState;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;

/**
 * Updates entity nametags with debug information when debug mode is enabled.
 */
public final class DebugNametagUpdater {

    private static final String DEBUG_PREFIX = "BE: "; // Tag to identify our custom names

    private DebugNametagUpdater() {
    }

    /**
     * Start the debug updater loop.
     */
    public static void init() {
        ServerTickEvents.END_SERVER_TICK.register(DebugNametagUpdater::onServerTick);
    }

    private static void onServerTick(MinecraftServer server) {
        if (!DebugModeManager.isAnyDebugActive()) {
            return;
        }

        for (ServerLevel level : server.getAllLevels()) {
            updateLevel(level);
        }
    }

    private static void updateLevel(ServerLevel level) {
        // Iterate only loaded entities to avoid lag
        for (Entity entity : level.getAllEntities()) {
            if (!(entity instanceof Mob mob)) {
                continue;
            }

            // Only update if debug is needed near this entity
            if (!shouldUpdateEntity(mob)) {
                // If it had a debug name, clear it
                if (hasDebugName(mob)) {
                    clearDebugName(mob);
                }
                continue;
            }

            updateEntityNametag(mob);
        }
    }

    private static boolean shouldUpdateEntity(Mob mob) {
        if (DebugModeManager.isGlobalEnabled()) {
            return true;
        }

        // Optimisation: Only update if near a player with debug enabled
        // Check within 32 blocks
        return mob.level().players().stream()
            .anyMatch(player -> DebugModeManager.isEnabledForPlayer(player.getUUID()) &&
                               player.distanceToSqr(mob) < 32 * 32);
    }

    private static boolean hasDebugName(Mob mob) {
        Component name = mob.getCustomName();
        return name != null && name.getString().startsWith(DEBUG_PREFIX);
    }

    private static void clearDebugName(Mob mob) {
        // Only clear if we set it (check prefix)
        if (hasDebugName(mob)) {
            mob.setCustomName(null);
            mob.setCustomNameVisible(false);
        }
    }

    private static void updateEntityNametag(Mob mob) {
        EcologyComponent component = EcologyHooks.getEcologyComponent(mob);

        // Only show debug if it has ecology data
        if (!component.hasProfile() && component.handles().isEmpty()) {
            return;
        }

        // Get states
        EntityState state = component.state();
        CompoundTag hungerTag = component.getHandleTag("hunger");
        CompoundTag thirstTag = component.getHandleTag("thirst");

        int hunger = hungerTag.contains("hunger") ? hungerTag.getInt("hunger") : -1;
        int thirst = thirstTag.contains("thirst") ? thirstTag.getInt("thirst") : -1;

        // Health info
        float health = mob.getHealth();
        float maxHealth = mob.getMaxHealth();
        int healthPercent = maxHealth > 0 ? Math.round((health / maxHealth) * 100) : 0;

        // Format string: "HP:80% H:20 T:100 [Hungry][Thirsty][Panic]"
        StringBuilder sb = new StringBuilder(DEBUG_PREFIX);

        sb.append("HP:").append(healthPercent).append("% ");
        if (hunger != -1) sb.append("H:").append(hunger).append(" ");
        if (thirst != -1) sb.append("T:").append(thirst).append(" ");

        // State flags
        appendStateFlags(sb, state);

        Component debugText = Component.literal(sb.toString().trim())
            .withStyle(ChatFormatting.YELLOW);

        mob.setCustomName(debugText);
        mob.setCustomNameVisible(true);
    }

    private static void appendStateFlags(StringBuilder sb, EntityState state) {
        if (state.isPanicking()) sb.append("[Pnc]");
        if (state.isFleeing()) sb.append("[Fle]");
        if (state.isHunting()) sb.append("[Hun]");
        if (state.isHungry()) sb.append("[Hg]");
        if (state.isThirsty()) sb.append("[Th]");
        if (state.isStarving()) sb.append("[Stv]");
        if (state.isDehydrated()) sb.append("[Deh]");
        if (state.isInjured()) sb.append("[Inj]");
        if (state.isBaby()) sb.append("[Baby]");
        if (state.isElderly()) sb.append("[Old]");
        if (state.isLonely()) sb.append("[Lon]");
        if (state.isInCombat()) sb.append("[Cbt]");
        if (state.isRetreating()) sb.append("[Ret]");
        if (state.isCarryingOffspring()) sb.append("[Carry]");
        if (state.isInWater()) sb.append("[Water]");
        if (state.isTamed()) sb.append("[Tamed]");
    }
}
