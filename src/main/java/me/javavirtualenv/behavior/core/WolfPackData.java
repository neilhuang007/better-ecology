package me.javavirtualenv.behavior.core;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.core.UUIDUtil;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.animal.Wolf;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.world.phys.AABB;

/**
 * Data class for wolf pack membership and hierarchy.
 * Wolves track their pack via a pack UUID and their rank within the pack.
 */
public record WolfPackData(
    UUID packId,
    PackRank rank,
    int sharesCooldown
) {
    /**
     * Wolf pack ranks, from highest to lowest priority.
     */
    public enum PackRank {
        ALPHA(3),    // Pack leader, highest priority for food
        BETA(2),     // Second in command
        OMEGA(1);    // Lowest rank

        private final int priority;

        PackRank(int priority) {
            this.priority = priority;
        }

        public int getPriority() {
            return priority;
        }
    }

    // Codec for serialization
    public static final Codec<WolfPackData> CODEC = RecordCodecBuilder.create(instance ->
        instance.group(
            UUIDUtil.CODEC.fieldOf("pack_id").forGetter(WolfPackData::packId),
            Codec.STRING.xmap(PackRank::valueOf, PackRank::name).fieldOf("rank").forGetter(WolfPackData::rank),
            Codec.INT.fieldOf("shares_cooldown").forGetter(WolfPackData::sharesCooldown)
        ).apply(instance, WolfPackData::new)
    );

    // Default pack data - creates a new pack with this wolf as alpha
    public static WolfPackData createNewPack() {
        return new WolfPackData(UUID.randomUUID(), PackRank.ALPHA, 0);
    }

    // Join an existing pack as omega
    public static WolfPackData joinPack(UUID packId) {
        return new WolfPackData(packId, PackRank.OMEGA, 0);
    }

    // Attachment type for wolves
    public static final AttachmentType<WolfPackData> PACK_DATA_ATTACHMENT = AttachmentRegistry.create(
        ResourceLocation.fromNamespaceAndPath("better-ecology", "wolf_pack"),
        builder -> builder
            .initializer(WolfPackData::createNewPack)
            .persistent(CODEC)
            .copyOnDeath()
    );

    // ========== UTILITY METHODS ==========

    /**
     * Gets the pack data for a wolf, creating new pack if needed.
     */
    public static WolfPackData getPackData(Wolf wolf) {
        return wolf.getAttachedOrCreate(PACK_DATA_ATTACHMENT);
    }

    /**
     * Sets pack data for a wolf.
     */
    public static void setPackData(Wolf wolf, WolfPackData data) {
        wolf.setAttached(PACK_DATA_ATTACHMENT, data);
    }

    /**
     * Checks if two wolves are in the same pack.
     */
    public static boolean arePackmates(Wolf wolf1, Wolf wolf2) {
        if (wolf1 == wolf2) return true;
        WolfPackData data1 = getPackData(wolf1);
        WolfPackData data2 = getPackData(wolf2);
        return data1.packId().equals(data2.packId());
    }

    /**
     * Makes wolf2 join wolf1's pack as omega.
     */
    public static void joinPackOf(Wolf newMember, Wolf packLeader) {
        WolfPackData leaderData = getPackData(packLeader);
        setPackData(newMember, joinPack(leaderData.packId()));
    }

    /**
     * Promotes a wolf to a higher rank.
     */
    public static void promote(Wolf wolf) {
        WolfPackData data = getPackData(wolf);
        PackRank newRank = switch (data.rank()) {
            case OMEGA -> PackRank.BETA;
            case BETA -> PackRank.ALPHA;
            case ALPHA -> PackRank.ALPHA; // Already highest
        };
        setPackData(wolf, new WolfPackData(data.packId(), newRank, data.sharesCooldown()));
    }

    /**
     * Updates the share cooldown for a wolf.
     */
    public static void setSharesCooldown(Wolf wolf, int cooldown) {
        WolfPackData data = getPackData(wolf);
        setPackData(wolf, new WolfPackData(data.packId(), data.rank(), cooldown));
    }

    /**
     * Decrements share cooldown by 1 if > 0.
     */
    public static void tickSharesCooldown(Wolf wolf) {
        WolfPackData data = getPackData(wolf);
        if (data.sharesCooldown() > 0) {
            setPackData(wolf, new WolfPackData(data.packId(), data.rank(), data.sharesCooldown() - 1));
        }
    }

    /**
     * Checks if the wolf can share food (cooldown is 0).
     */
    public static boolean canShare(Wolf wolf) {
        return getPackData(wolf).sharesCooldown() <= 0;
    }

    /**
     * Checks if any nearby pack member is hungry.
     * Used to determine if a wolf should hunt for food to share.
     *
     * @param wolf The wolf to check around
     * @param range The range to search for pack members
     * @return true if any nearby pack member is hungry
     */
    public static boolean hasHungryPackMember(Wolf wolf, double range) {
        AABB searchBox = wolf.getBoundingBox().inflate(range);
        WolfPackData myData = getPackData(wolf);

        List<Wolf> nearbyWolves = wolf.level().getEntitiesOfClass(Wolf.class, searchBox,
            other -> other != wolf && other.isAlive());

        for (Wolf other : nearbyWolves) {
            WolfPackData otherData = getPackData(other);
            if (myData.packId().equals(otherData.packId()) && AnimalNeeds.isHungry(other)) {
                return true;
            }
        }

        return false;
    }
}
