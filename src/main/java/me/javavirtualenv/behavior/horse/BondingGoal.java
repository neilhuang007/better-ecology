package me.javavirtualenv.behavior.horse;

import me.javavirtualenv.BetterEcology;
import me.javavirtualenv.debug.BehaviorLogger;
import me.javavirtualenv.ecology.EcologyComponent;
import me.javavirtualenv.ecology.api.EcologyAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.pathfinder.Path;

import java.util.EnumSet;
import java.util.List;
import java.util.UUID;

/**
 * AI Goal for horse bonding behavior with players.
 * <p>
 * Horses form bonds with players through:
 * <ul>
 *   <li>Proximity - being near a player builds trust</li>
 *   <li>Interactions - feeding, petting increase bond</li>
 *   <li>Riding - being ridden strengthens the bond</li>
 * </ul>
 * <p>
 * Bonded horses show affection through nicker sounds and heart particles.
 */
public class BondingGoal extends Goal {

    // Configuration constants
    private static final String BONDING_KEY = "bonding";
    private static final String BONDS_TAG = "bonds";

    private static final double SEARCH_RADIUS = 12.0; // Search 12 blocks for players
    private static final double BONDING_DISTANCE = 3.0; // Distance to gain bond XP
    private static final double NICKER_MIN_DISTANCE = 6.0; // Minimum distance for nicker
    private static final double NICKER_MAX_DISTANCE = 10.0; // Maximum distance for nicker
    private static final int BOND_INTERVAL_TICKS = 100; // Gain bond XP every 100 ticks
    private static final double NICKER_CHANCE = 0.4; // Chance to nicker when bonded player approaches
    private static final int NICKER_COOLDOWN_TICKS = 200; // Cooldown between nickers
    private static final int HEART_CHANCE = 30; // Ticks interval for heart particles (30% chance)
    private static final int STRONG_BOND_THRESHOLD = 75; // Bond level for "strongly bonded"
    private static final int MAX_BOND_LEVEL = 100;

    // Instance fields
    private final AbstractHorse horse;
    private Player targetPlayer;
    private int bondTicks;
    private int nickerCooldownTicks;

    // Debug info
    private String lastDebugMessage = "";
    private boolean hadBondedPlayerLastCheck = false;

    public BondingGoal(AbstractHorse horse) {
        this.horse = horse;
        this.setFlags(EnumSet.of(Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        // Client-side only runs visual logic
        if (horse.level().isClientSide) {
            return false;
        }

        // Update cooldown
        if (nickerCooldownTicks > 0) {
            nickerCooldownTicks--;
        }

        // Find nearby player to bond with
        targetPlayer = findNearbyPlayer();

        if (targetPlayer == null) {
            // Log state change
            if (hadBondedPlayerLastCheck) {
                debug("no players nearby");
                hadBondedPlayerLastCheck = false;
            }
            return false;
        }

        hadBondedPlayerLastCheck = true;
        return true;
    }

    @Override
    public boolean canContinueToUse() {
        if (targetPlayer == null || !targetPlayer.isAlive()) {
            return false;
        }

        double distance = horse.distanceToSqr(targetPlayer);
        return distance <= SEARCH_RADIUS * SEARCH_RADIUS;
    }

    @Override
    public void start() {
        bondTicks = 0;
        int bondLevel = getBondLevel(targetPlayer);
        debug("STARTING: bonding with " + targetPlayer.getName().getString() +
              " (bond=" + bondLevel + ")");
    }

    @Override
    public void stop() {
        debug("bonding stopped");
        targetPlayer = null;
        bondTicks = 0;
    }

    @Override
    public void tick() {
        if (targetPlayer == null) {
            return;
        }

        // Look at the player
        horse.getLookControl().setLookAt(targetPlayer, 30.0f, 30.0f);

        bondTicks++;

        // Give bond experience for being near
        if (bondTicks % BOND_INTERVAL_TICKS == 0) {
            double distance = horse.distanceTo(targetPlayer);
            if (distance < BONDING_DISTANCE) {
                addBondExperience(targetPlayer, 1);
                int newBondLevel = getBondLevel(targetPlayer);
                debug("bond increased to " + newBondLevel);
            }
        }

        // Play nicker sound for bonded players
        if (nickerCooldownTicks <= 0 && isBonded(targetPlayer)) {
            double distance = horse.distanceTo(targetPlayer);
            if (distance > NICKER_MIN_DISTANCE && distance < NICKER_MAX_DISTANCE) {
                if (horse.getRandom().nextFloat() < NICKER_CHANCE) {
                    playNickerSound();
                    nickerCooldownTicks = NICKER_COOLDOWN_TICKS;
                    debug("nickered at bonded player");
                }
            }
        }

        // Show heart particles for strong bonds
        if (isStronglyBonded(targetPlayer)) {
            if (bondTicks % HEART_CHANCE == 0 && horse.getRandom().nextFloat() < 0.3) {
                spawnHeartParticles();
            }
        }
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    /**
     * Record a ride session with a player.
     */
    public void recordRide(Player player) {
        addBondExperience(player, 10);
        debug("recorded ride with " + player.getName().getString());
    }

    /**
     * Record an interaction with a player.
     */
    public void recordInteraction(Player player) {
        addBondExperience(player, 5);
    }

    /**
     * Find the nearest player to bond with.
     */
    private Player findNearbyPlayer() {
        List<Player> nearbyPlayers = horse.level().getEntitiesOfClass(
            Player.class,
            horse.getBoundingBox().inflate(SEARCH_RADIUS)
        );

        Player closestPlayer = null;
        double closestDistance = Double.MAX_VALUE;

        for (Player player : nearbyPlayers) {
            if (!player.isAlive()) {
                continue;
            }

            double distance = horse.distanceToSqr(player);
            if (distance > SEARCH_RADIUS * SEARCH_RADIUS) {
                continue;
            }

            // Prefer bonded players
            int bondLevel = getBondLevel(player);
            boolean isBonded = bondLevel > 0;

            if (isBonded && distance < closestDistance) {
                closestDistance = distance;
                closestPlayer = player;
            } else if (closestPlayer == null && distance < 16.0 * 16.0) {
                // Also consider unbonded players at close range
                if (distance < closestDistance) {
                    closestDistance = distance;
                    closestPlayer = player;
                }
            }
        }

        return closestPlayer;
    }

    /**
     * Add bond experience to a player.
     */
    private void addBondExperience(Player player, int amount) {
        if (player == null) {
            return;
        }

        EcologyComponent component = getComponent();
        if (component == null) {
            return;
        }

        CompoundTag bondingTag = component.getHandleTag(BONDING_KEY);
        CompoundTag bondsTag = bondingTag.getCompound(BONDS_TAG);
        String playerId = player.getUUID().toString();

        CompoundTag playerBond = bondsTag.getCompound(playerId);
        int currentLevel = playerBond.contains("level") ? playerBond.getInt("level") : 0;
        int newLevel = Math.min(MAX_BOND_LEVEL, currentLevel + amount);

        playerBond.putInt("level", newLevel);
        playerBond.putInt("interactions", playerBond.contains("interactions") ?
            playerBond.getInt("interactions") + 1 : 1);
        bondsTag.put(playerId, playerBond);
        bondingTag.put(BONDS_TAG, bondsTag);
    }

    /**
     * Get the bond level with a player.
     */
    private int getBondLevel(Player player) {
        if (player == null) {
            return 0;
        }

        EcologyComponent component = getComponent();
        if (component == null) {
            return 0;
        }

        CompoundTag bondingTag = component.getHandleTag(BONDING_KEY);
        CompoundTag bondsTag = bondingTag.getCompound(BONDS_TAG);
        String playerId = player.getUUID().toString();

        return bondsTag.getCompound(playerId).getInt("level");
    }

    /**
     * Check if a player is bonded with this horse.
     */
    private boolean isBonded(Player player) {
        return getBondLevel(player) > 0;
    }

    /**
     * Check if a player has a strong bond with this horse.
     */
    private boolean isStronglyBonded(Player player) {
        return getBondLevel(player) >= STRONG_BOND_THRESHOLD;
    }

    /**
     * Play the nicker sound for this horse type.
     */
    private void playNickerSound() {
        if (horse.level().isClientSide) {
            return;
        }

        var sound = getNickerSound();
        horse.level().playSound(null, horse.blockPosition(), sound,
            net.minecraft.sounds.SoundSource.NEUTRAL, 0.8f, 1.0f);
    }

    /**
     * Get the appropriate nicker sound for this horse type.
     */
    private net.minecraft.sounds.SoundEvent getNickerSound() {
        var type = horse.getType();

        if (type == net.minecraft.world.entity.EntityType.DONKEY) {
            return net.minecraft.sounds.SoundEvents.DONKEY_AMBIENT;
        } else if (type == net.minecraft.world.entity.EntityType.MULE) {
            return net.minecraft.sounds.SoundEvents.DONKEY_AMBIENT;
        } else {
            return net.minecraft.sounds.SoundEvents.HORSE_AMBIENT;
        }
    }

    /**
     * Spawn heart particles to show affection.
     */
    private void spawnHeartParticles() {
        if (horse.level().isClientSide) {
            return;
        }

        double x = horse.getX();
        double y = horse.getY() + horse.getBbHeight() + 0.5;
        double z = horse.getZ();

        for (int i = 0; i < 3; i++) {
            double offsetX = (horse.getRandom().nextDouble() - 0.5) * 0.5;
            double offsetZ = (horse.getRandom().nextDouble() - 0.5) * 0.5;
            ((net.minecraft.server.level.ServerLevel) horse.level()).sendParticles(
                net.minecraft.core.particles.ParticleTypes.HEART,
                x + offsetX, y, z + offsetZ,
                1, 0, 0.1, 0, 0
            );
        }
    }

    /**
     * Get the ecology component for this horse.
     */
    private EcologyComponent getComponent() {
        if (!(horse instanceof EcologyAccess access)) {
            return null;
        }
        return access.betterEcology$getEcologyComponent();
    }

    /**
     * Debug logging with consistent prefix.
     */
    private void debug(String message) {
        lastDebugMessage = message;
        if (BehaviorLogger.isMinimal() || BetterEcology.DEBUG_MODE) {
            String prefix = "[Bonding] Horse #" + horse.getId() + " ";
            BehaviorLogger.info(prefix + message);
        }
    }

    /**
     * Get last debug message for external display.
     */
    public String getLastDebugMessage() {
        return lastDebugMessage;
    }

    /**
     * Get current state info for debug display.
     */
    public String getDebugState() {
        int bondLevel = targetPlayer != null ? getBondLevel(targetPlayer) : 0;
        String playerName = targetPlayer != null ? targetPlayer.getName().getString() : "none";
        return String.format("player=%s, bond=%d, ticks=%d, cooldown=%d",
            playerName, bondLevel, bondTicks, nickerCooldownTicks);
    }
}
