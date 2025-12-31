package me.javavirtualenv.behavior.feline;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.UUIDUtil;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Component for tracking cat affection and trust levels.
 * <p>
 * This component manages:
 * - Trust levels with players (0-100)
 * - Personality traits (playful, shy, aggressive, cuddly)
 * - Social bonds with specific players
 * - Gift giving eligibility
 * - Behavior modifiers based on affection
 */
public class CatAffectionComponent {

    private static final String TRUST_KEY = "Trust";
    private static final String PERSONALITY_KEY = "Personality";
    private static final String LAST_GIFT_TIME_KEY = "LastGiftTime";
    private static final String TOTAL_PETS_KEY = "TotalPets";
    private static final String PLAY_SESSIONS_KEY = "PlaySessions";

    private final Map<UUID, PlayerAffectionData> affectionData = new HashMap<>();
    private CatPersonality personality = CatPersonality.getRandom();
    private long lastGiftTime = 0;

    public CatAffectionComponent() {
    }

    /**
     * Get the trust level for a specific player.
     */
    public double getTrustLevel(Player player) {
        if (player == null) {
            return 0.0;
        }

        PlayerAffectionData data = affectionData.get(player.getUUID());
        return data != null ? data.trust : 0.0;
    }

    /**
     * Set the trust level for a specific player.
     */
    public void setTrustLevel(Player player, double trust) {
        if (player == null) {
            return;
        }

        PlayerAffectionData data = affectionData.computeIfAbsent(
            player.getUUID(),
            uuid -> new PlayerAffectionData()
        );
        data.trust = Math.max(0.0, Math.min(100.0, trust));
    }

    /**
     * Increase trust with a player.
     */
    public void increaseTrust(Player player, double amount) {
        double currentTrust = getTrustLevel(player);
        setTrustLevel(player, currentTrust + amount);
    }

    /**
     * Decrease trust with a player.
     */
    public void decreaseTrust(Player player, double amount) {
        double currentTrust = getTrustLevel(player);
        setTrustLevel(player, currentTrust - amount);
    }

    /**
     * Get the cat's personality type.
     */
    public CatPersonality getPersonality() {
        return personality;
    }

    /**
     * Set the cat's personality type.
     */
    public void setPersonality(CatPersonality personality) {
        this.personality = personality;
    }

    /**
     * Check if the cat will give gifts to a player.
     */
    public boolean willGiveGifts(Player player) {
        if (player == null) {
            return false;
        }

        double trust = getTrustLevel(player);
        return trust >= 70.0;
    }

    /**
     * Check if the cat will follow a player.
     */
    public boolean willFollow(Player player) {
        if (player == null) {
            return false;
        }

        double trust = getTrustLevel(player);
        return trust >= 50.0;
    }

    /**
     * Check if the cat allows being petted by a player.
     */
    public boolean allowsPetting(Player player) {
        if (player == null) {
            return false;
        }

        double trust = getTrustLevel(player);

        // Shy cats require more trust
        if (personality == CatPersonality.SHY) {
            return trust >= 40.0;
        }

        return trust >= 20.0;
    }

    /**
     * Record that the cat was petted.
     */
    public void recordPetting(Player player) {
        if (player == null) {
            return;
        }

        PlayerAffectionData data = affectionData.computeIfAbsent(
            player.getUUID(),
            uuid -> new PlayerAffectionData()
        );
        data.totalPets++;
        increaseTrust(player, personality.getPettingTrustGain());
    }

    /**
     * Record a play session with a player.
     */
    public void recordPlaySession(Player player) {
        if (player == null) {
            return;
        }

        PlayerAffectionData data = affectionData.computeIfAbsent(
            player.getUUID(),
            uuid -> new PlayerAffectionData()
        );
        data.playSessions++;
        increaseTrust(player, personality.getPlayTrustGain());
    }

    /**
     * Get the number of times the cat has been petted.
     */
    public int getTotalPets(Player player) {
        if (player == null) {
            return 0;
        }

        PlayerAffectionData data = affectionData.get(player.getUUID());
        return data != null ? data.totalPets : 0;
    }

    /**
     * Get the number of play sessions with a player.
     */
    public int getPlaySessions(Player player) {
        if (player == null) {
            return 0;
        }

        PlayerAffectionData data = affectionData.get(player.getUUID());
        return data != null ? data.playSessions : 0;
    }

    /**
     * Serialize this component to NBT.
     */
    public CompoundTag toNbt() {
        CompoundTag tag = new CompoundTag();

        // Save personality
        tag.putString(PERSONALITY_KEY, personality.name());

        // Save last gift time
        tag.putLong(LAST_GIFT_TIME_KEY, lastGiftTime);

        // Save affection data for each player
        CompoundTag affectionTag = new CompoundTag();
        for (Map.Entry<UUID, PlayerAffectionData> entry : affectionData.entrySet()) {
            CompoundTag playerTag = new CompoundTag();
            playerTag.putDouble(TRUST_KEY, entry.getValue().trust);
            playerTag.putInt(TOTAL_PETS_KEY, entry.getValue().totalPets);
            playerTag.putInt(PLAY_SESSIONS_KEY, entry.getValue().playSessions);

            affectionTag.put(entry.getKey().toString(), playerTag);
        }
        tag.put("AffectionData", affectionTag);

        return tag;
    }

    /**
     * Deserialize this component from NBT.
     */
    public void fromNbt(CompoundTag tag) {
        // Load personality
        if (tag.contains(PERSONALITY_KEY)) {
            try {
                personality = CatPersonality.valueOf(tag.getString(PERSONALITY_KEY));
            } catch (IllegalArgumentException e) {
                personality = CatPersonality.getRandom();
            }
        }

        // Load last gift time
        if (tag.contains(LAST_GIFT_TIME_KEY)) {
            lastGiftTime = tag.getLong(LAST_GIFT_TIME_KEY);
        }

        // Load affection data
        if (tag.contains("AffectionData")) {
            CompoundTag affectionTag = tag.getCompound("AffectionData");

            for (String uuidString : affectionTag.getAllKeys()) {
                try {
                    UUID uuid = UUID.fromString(uuidString);
                    CompoundTag playerTag = affectionTag.getCompound(uuidString);

                    PlayerAffectionData data = new PlayerAffectionData();
                    data.trust = playerTag.getDouble(TRUST_KEY);
                    data.totalPets = playerTag.getInt(TOTAL_PETS_KEY);
                    data.playSessions = playerTag.getInt(PLAY_SESSIONS_KEY);

                    affectionData.put(uuid, data);
                } catch (IllegalArgumentException e) {
                    // Invalid UUID, skip
                }
            }
        }
    }

    /**
     * Data class for per-player affection.
     */
    private static class PlayerAffectionData {
        private double trust = 0.0;
        private int totalPets = 0;
        private int playSessions = 0;
    }

    /**
     * Personality types for cats.
     */
    public enum CatPersonality {
        PLAYFUL("Playful", 1.5, 2.0),
        SHY("Shy", 0.5, 1.0),
        AGGRESSIVE("Aggressive", 0.3, 0.5),
        CUDDLY("Cuddly", 2.0, 1.5),
        LAZY("Lazy", 1.0, 0.8),
        HUNTER("Hunter", 1.2, 1.5),
        INDEPENDENT("Independent", 0.8, 1.0);

        private final String displayName;
        private final double pettingTrustGain;
        private final double playTrustGain;

        CatPersonality(String displayName, double pettingTrustGain, double playTrustGain) {
            this.displayName = displayName;
            this.pettingTrustGain = pettingTrustGain;
            this.playTrustGain = playTrustGain;
        }

        public String getDisplayName() {
            return displayName;
        }

        public double getPettingTrustGain() {
            return pettingTrustGain;
        }

        public double getPlayTrustGain() {
            return playTrustGain;
        }

        public static CatPersonality getRandom() {
            CatPersonality[] values = values();
            return values[(int) (Math.random() * values.length)];
        }
    }
}
