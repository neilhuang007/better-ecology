package me.javavirtualenv.behavior.villager;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.Villager;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Gossip system for villager information sharing.
 * Villagers share information about players, threats, and opportunities.
 */
public class GossipSystem {
    private final Villager villager;
    private final Map<GossipType, List<GossipEntry>> gossipByType = new ConcurrentHashMap<>();
    private static final int MAX_GOSSIP_PER_TYPE = 20;
    private static final long GOSSIP_DECAY_TIME = 24000; // 1 day in ticks
    private static final double SPREAD_CHANCE = 0.3;

    public GossipSystem(Villager villager) {
        this.villager = villager;
        for (GossipType type : GossipType.values()) {
            gossipByType.put(type, new ArrayList<>());
        }
    }

    /**
     * Adds a new gossip piece to this villager's knowledge.
     */
    public void addGossip(GossipType type, UUID target, int strength) {
        List<GossipEntry> gossipList = gossipByType.get(type);
        if (gossipList == null) {
            return;
        }

        // Check if gossip about this target already exists
        Optional<GossipEntry> existing = gossipList.stream()
            .filter(g -> g.targetId.equals(target))
            .findFirst();

        long currentTime = villager.level().getGameTime();

        if (existing.isPresent()) {
            // Strengthen existing gossip
            GossipEntry entry = existing.get();
            entry.strength = Math.min(type.getMaxStrength(), entry.strength + strength);
            entry.timestamp = currentTime;
        } else {
            // Add new gossip
            if (gossipList.size() >= MAX_GOSSIP_PER_TYPE) {
                // Remove oldest gossip
                gossipList.sort(Comparator.comparingLong(g -> g.timestamp));
                gossipList.remove(0);
            }
            gossipList.add(new GossipEntry(target, strength, currentTime));
        }
    }

    /**
     * Gets the total gossip strength for a target and type.
     */
    public int getGossipStrength(GossipType type, UUID target) {
        List<GossipEntry> gossipList = gossipByType.get(type);
        if (gossipList == null) {
            return 0;
        }

        return gossipList.stream()
            .filter(g -> g.targetId.equals(target))
            .mapToInt(g -> {
                long age = villager.level().getGameTime() - g.timestamp;
                int decay = (int) (age / GOSSIP_DECAY_TIME);
                return Math.max(0, g.strength - decay);
            })
            .sum();
    }

    /**
     * Gets all gossip of a specific type.
     */
    public List<GossipEntry> getGossipByType(GossipType type) {
        List<GossipEntry> gossipList = gossipByType.get(type);
        if (gossipList == null) {
            return List.of();
        }

        // Filter out expired gossip
        long currentTime = villager.level().getGameTime();
        List<GossipEntry> valid = new ArrayList<>();
        for (GossipEntry entry : gossipList) {
            long age = currentTime - entry.timestamp;
            if (age < GOSSIP_DECAY_TIME * 5) { // Keep gossip for 5 days
                int decay = (int) (age / GOSSIP_DECAY_TIME);
                if (entry.strength - decay > 0) {
                    valid.add(entry);
                }
            }
        }

        return valid;
    }

    /**
     * Spreads gossip to nearby villagers.
     * Should be called when villagers socialize.
     */
    public void spreadGossip(Villager other) {
        GossipSystem otherGossip = VillagerMixin.getGossipSystem(other);
        if (otherGossip == null) {
            return;
        }

        // Spread each type of gossip
        for (GossipType type : GossipType.values()) {
            List<GossipEntry> myGossip = getGossipByType(type);
            List<GossipEntry> theirGossip = otherGossip.getGossipByType(type);

            // Share my gossip with them
            for (GossipEntry entry : myGossip) {
                if (villager.getRandom().nextDouble() < SPREAD_CHANCE) {
                    otherGossip.addGossip(type, entry.targetId, Math.max(1, entry.strength / 2));
                }
            }

            // Receive their gossip
            for (GossipEntry entry : theirGossip) {
                if (villager.getRandom().nextDouble() < SPREAD_CHANCE) {
                    addGossip(type, entry.targetId, Math.max(1, entry.strength / 2));
                }
            }
        }
    }

    /**
     * Serializes gossip data to NBT.
     */
    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        for (GossipType type : GossipType.values()) {
            List<GossipEntry> gossipList = gossipByType.get(type);
            if (!gossipList.isEmpty()) {
                CompoundTag typeTag = new CompoundTag();
                for (int i = 0; i < gossipList.size(); i++) {
                    typeTag.put("gossip_" + i, gossipList.get(i).save());
                }
                tag.put(type.name(), typeTag);
            }
        }
        tag.putInt("Size", gossipByType.size());
        return tag;
    }

    /**
     * Loads gossip data from NBT.
     */
    public void load(CompoundTag tag) {
        for (GossipType type : GossipType.values()) {
            String typeName = type.name();
            if (tag.contains(typeName)) {
                CompoundTag typeTag = tag.getCompound(typeName);
                List<GossipEntry> gossipList = gossipByType.get(type);
                gossipList.clear();

                for (String key : typeTag.getAllKeys()) {
                    if (key.startsWith("gossip_")) {
                        GossipEntry entry = new GossipEntry();
                        entry.load(typeTag.getCompound(key));
                        gossipList.add(entry);
                    }
                }
            }
        }
    }

    /**
     * Removes old gossip to prevent memory bloat.
     */
    public void decayGossip() {
        long currentTime = villager.level().getGameTime();

        for (GossipType type : GossipType.values()) {
            List<GossipEntry> gossipList = gossipByType.get(type);
            gossipList.removeIf(entry -> {
                long age = currentTime - entry.timestamp;
                int decay = (int) (age / GOSSIP_DECAY_TIME);
                return entry.strength - decay <= 0;
            });
        }
    }

    /**
     * Represents a single piece of gossip.
     */
    public static class GossipEntry {
        private UUID targetId;
        private int strength;
        private long timestamp;

        public GossipEntry() {
        }

        public GossipEntry(UUID targetId, int strength, long timestamp) {
            this.targetId = targetId;
            this.strength = strength;
            this.timestamp = timestamp;
        }

        public UUID getTargetId() {
            return targetId;
        }

        public int getStrength() {
            return strength;
        }

        public long getTimestamp() {
            return timestamp;
        }

        public CompoundTag save() {
            CompoundTag tag = new CompoundTag();
            tag.putUUID("TargetId", targetId);
            tag.putInt("Strength", strength);
            tag.putLong("Timestamp", timestamp);
            return tag;
        }

        public void load(CompoundTag tag) {
            targetId = tag.getUUID("TargetId");
            strength = tag.getInt("Strength");
            timestamp = tag.getLong("Timestamp");
        }
    }

    /**
     * Types of gossip that can be shared.
     */
    public enum GossipType {
        MAJOR_POSITIVE("major_positive", 100),
        MINOR_POSITIVE("minor_positive", 50),
        MAJOR_NEGATIVE("major_negative", 100),
        MINOR_NEGATIVE("minor_negative", 50),
        TRADING("trading", 20),
        THREAT("threat", 50),
        EMERGENCY("emergency", 100);

        private final String name;
        private final int maxStrength;

        GossipType(String name, int maxStrength) {
            this.name = name;
            this.maxStrength = maxStrength;
        }

        public String getName() {
            return name;
        }

        public int getMaxStrength() {
            return maxStrength;
        }
    }
}
