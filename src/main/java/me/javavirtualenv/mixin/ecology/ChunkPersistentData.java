package me.javavirtualenv.mixin.ecology;

import net.minecraft.nbt.CompoundTag;

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
