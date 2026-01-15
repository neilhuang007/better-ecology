package me.javavirtualenv.network;

import me.javavirtualenv.BetterEcology;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

/**
 * Handles all client-server networking for Better Ecology.
 * Manages syncing animal needs data from server to client for the debug overlay.
 */
public final class EcologyPackets {

    private EcologyPackets() {
        // Utility class
    }

    // Packet ID for syncing animal needs
    public static final ResourceLocation SYNC_ANIMAL_NEEDS_ID =
        ResourceLocation.fromNamespaceAndPath(BetterEcology.MOD_ID, "sync_animal_needs");

    /**
     * Registers all packet types with the networking system.
     * Must be called during mod initialization.
     */
    public static void register() {
        PayloadTypeRegistry.playS2C().register(
            SyncAnimalNeedsPayload.TYPE,
            SyncAnimalNeedsPayload.CODEC
        );

        BetterEcology.LOGGER.debug("Registered ecology network packets");
    }

    /**
     * Sends animal needs data to a specific player.
     * Called when needs change or periodically for tracked entities.
     */
    public static void sendAnimalNeedsToPlayer(ServerPlayer player, int entityId, float hunger, float thirst) {
        SyncAnimalNeedsPayload payload = new SyncAnimalNeedsPayload(entityId, hunger, thirst);
        ServerPlayNetworking.send(player, payload);
    }

    /**
     * Sends animal needs data to all players tracking an entity.
     */
    public static void sendAnimalNeedsToTracking(Entity entity, float hunger, float thirst) {
        if (entity.level().isClientSide()) {
            return; // Only send from server
        }

        SyncAnimalNeedsPayload payload = new SyncAnimalNeedsPayload(entity.getId(), hunger, thirst);

        // Send to all players tracking this entity
        entity.level().players().forEach(player -> {
            if (player instanceof ServerPlayer serverPlayer) {
                if (serverPlayer.distanceTo(entity) < 128) { // Only send to players within range
                    ServerPlayNetworking.send(serverPlayer, payload);
                }
            }
        });
    }

    /**
     * Payload for syncing animal needs (hunger/thirst) from server to client.
     */
    public record SyncAnimalNeedsPayload(int entityId, float hunger, float thirst)
            implements CustomPacketPayload {

        public static final CustomPacketPayload.Type<SyncAnimalNeedsPayload> TYPE =
            new CustomPacketPayload.Type<>(SYNC_ANIMAL_NEEDS_ID);

        public static final StreamCodec<RegistryFriendlyByteBuf, SyncAnimalNeedsPayload> CODEC =
            StreamCodec.composite(
                StreamCodec.of(
                    (buf, value) -> buf.writeInt(value),
                    buf -> buf.readInt()
                ),
                SyncAnimalNeedsPayload::entityId,
                StreamCodec.of(
                    (buf, value) -> buf.writeFloat(value),
                    buf -> buf.readFloat()
                ),
                SyncAnimalNeedsPayload::hunger,
                StreamCodec.of(
                    (buf, value) -> buf.writeFloat(value),
                    buf -> buf.readFloat()
                ),
                SyncAnimalNeedsPayload::thirst,
                SyncAnimalNeedsPayload::new
            );

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }
}
