package me.javavirtualenv.ecology.mixin;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.chunk.LevelChunk;

/**
 * Interface for accessing persistent data on chunks.
 * Implemented via mixin on LevelChunk.
 */
public interface ChunkPersistentData {
	/**
	 * Get or create the persistent data NBT tag for this chunk.
	 */
	public CompoundTag getOrCreatePersistentData();
}
