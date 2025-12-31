package me.javavirtualenv.mixin;

import me.javavirtualenv.ecology.mixin.ChunkPersistentData;
import me.javavirtualenv.ecology.spawning.ChunkSpawnData;
import me.javavirtualenv.ecology.spawning.ChunkSpawnDataStorage;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin for LevelChunk to add persistent data storage.
 * Enables saving/loading of pre-computed spawn positions to chunk NBT.
 */
@Mixin(LevelChunk.class)
public class LevelChunkMixin implements ChunkPersistentData {

    @Unique
    private static final String BETTER_ECOLOGY_DATA_KEY = "BetterEcologyData";

    @Unique
    private CompoundTag betterEcology$persistentData;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void betterEcology$onConstruct(CallbackInfo ci) {
        this.betterEcology$persistentData = new CompoundTag();
    }

    /**
     * Load spawn data from chunk NBT when chunk is loaded from disk.
     */
    @Inject(method = "loadAll", at = @At("RETURN"))
    private void betterEcology$onLoadAll(CompoundTag tag, CallbackInfo ci) {
        if (tag.contains(BETTER_ECOLOGY_DATA_KEY)) {
            CompoundTag ecologyTag = tag.getCompound(BETTER_ECOLOGY_DATA_KEY);
            ChunkSpawnData spawnData = ChunkSpawnData.fromNbt(ecologyTag);

            if (spawnData.hasAnySpawns()) {
                LevelChunk chunk = (LevelChunk) (Object) this;
                ChunkSpawnDataStorage.set(chunk, spawnData);
            }
        }
    }

    /**
     * Save spawn data to chunk NBT when chunk is saved to disk.
     */
    @Inject(method = "saveAll", at = @At("RETURN"))
    private void betterEcology$onSaveAll(CompoundTag tag, CallbackInfo ci) {
        LevelChunk chunk = (LevelChunk) (Object) this;
        ChunkSpawnData spawnData = ChunkSpawnDataStorage.get(chunk);

        if (spawnData != null && spawnData.hasAnySpawns()) {
            CompoundTag ecologyTag = spawnData.toNbt();
            tag.put(BETTER_ECOLOGY_DATA_KEY, ecologyTag);
        }
    }

    @Override
    public CompoundTag getOrCreatePersistentData() {
        if (this.betterEcology$persistentData == null) {
            this.betterEcology$persistentData = new CompoundTag();
        }
        return this.betterEcology$persistentData;
    }
}
