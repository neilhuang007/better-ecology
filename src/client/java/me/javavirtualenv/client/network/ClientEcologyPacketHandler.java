package me.javavirtualenv.client.network;

import me.javavirtualenv.network.EcologyPackets;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Client-side handler for ecology network packets.
 * Stores synced animal needs data for rendering in the debug overlay.
 */
public final class ClientEcologyPacketHandler {

    private ClientEcologyPacketHandler() {
        // Utility class
    }

    // Client-side cache of entity needs data
    private static final Map<Integer, AnimalNeedsData> needsCache = new ConcurrentHashMap<>();

    /**
     * Registers client-side packet handlers.
     * Must be called during client initialization.
     */
    public static void register() {
        ClientPlayNetworking.registerGlobalReceiver(
            EcologyPackets.SyncAnimalNeedsPayload.TYPE,
            (payload, context) -> {
                // Update cache with received data
                needsCache.put(
                    payload.entityId(),
                    new AnimalNeedsData(payload.hunger(), payload.thirst())
                );
            }
        );
    }

    /**
     * Gets cached needs data for an entity.
     * Returns null if not available.
     */
    public static AnimalNeedsData getNeedsData(int entityId) {
        return needsCache.get(entityId);
    }

    /**
     * Clears the needs cache (e.g., when disconnecting from server).
     */
    public static void clearCache() {
        needsCache.clear();
    }

    /**
     * Stores hunger and thirst data for an entity.
     */
    public record AnimalNeedsData(float hunger, float thirst) {
    }
}
