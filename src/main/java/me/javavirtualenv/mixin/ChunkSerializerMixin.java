package me.javavirtualenv.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import me.javavirtualenv.ecology.spawning.ChunkSpawnData;
import me.javavirtualenv.ecology.spawning.ChunkSpawnDataStorage;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.storage.ChunkSerializer;

/**
 * Mixin for ChunkSerializer to save/load custom chunk data.
 * Enables saving/loading of spawn positions to chunk NBT.
 */
@Mixin(ChunkSerializer.class)
public class ChunkSerializerMixin {

    private static final String BETTER_ECOLOGY_DATA_KEY = "BetterEcologyData";

    /**
     * Injects after chunk is read from disk to load our custom spawn data.
     * The chunk parameter contains the loaded chunk at this point.
     */
    @Inject(method = "read(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/ai/village/poi/PoiManager;Lnet/minecraft/world/level/chunk/storage/RegionStorageInfo;Lnet/minecraft/world/level/ChunkPos;Lnet/minecraft/nbt/CompoundTag;)Lnet/minecraft/world/level/chunk/ProtoChunk;", at = @At("RETURN"), locals = LocalCapture.CAPTURE_FAILHARD)
    private static void betterEcology$onChunkRead(
            net.minecraft.server.level.ServerLevel serverLevel,
            net.minecraft.world.entity.ai.village.poi.PoiManager poiManager,
            net.minecraft.world.level.chunk.storage.RegionStorageInfo regionStorageInfo,
            ChunkPos chunkPos,
            CompoundTag compoundTag,
            CallbackInfoReturnable<net.minecraft.world.level.chunk.ProtoChunk> cir) {
        net.minecraft.world.level.chunk.ProtoChunk protoChunk = cir.getReturnValue();
        if (protoChunk == null || !compoundTag.contains(BETTER_ECOLOGY_DATA_KEY)) {
            return;
        }

        CompoundTag ecologyTag = compoundTag.getCompound(BETTER_ECOLOGY_DATA_KEY);
        ChunkSpawnData spawnData = ChunkSpawnData.fromNbt(ecologyTag);

        if (spawnData.hasAnySpawns()) {
            // Store the spawn data - it will be transferred to LevelChunk when upgraded
            ChunkSpawnDataStorage.setForProtoChunk(protoChunk.getPos(), spawnData);
        }
    }

    /**
     * Injects before chunk NBT is written to disk to save our custom spawn data.
     */
    @Inject(method = "write(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/level/chunk/ChunkAccess;)Lnet/minecraft/nbt/CompoundTag;", at = @At("RETURN"))
    private static void betterEcology$onChunkWrite(
            net.minecraft.server.level.ServerLevel serverLevel,
            net.minecraft.world.level.chunk.ChunkAccess chunkAccess,
            CallbackInfoReturnable<CompoundTag> cir) {
        if (!(chunkAccess instanceof LevelChunk levelChunk)) {
            return;
        }

        ChunkSpawnData spawnData = ChunkSpawnDataStorage.get(levelChunk);
        if (spawnData == null || !spawnData.hasAnySpawns()) {
            return;
        }

        CompoundTag compoundTag = cir.getReturnValue();
        CompoundTag ecologyTag = spawnData.toNbt();
        compoundTag.put(BETTER_ECOLOGY_DATA_KEY, ecologyTag);
    }
}
