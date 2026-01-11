package me.javavirtualenv.ecology;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import me.javavirtualenv.ecology.EcologyHooks.UpdateMode;
import me.javavirtualenv.ecology.conservation.GeneticDiversityComponent;
import me.javavirtualenv.ecology.conservation.LineageRegistry;
import me.javavirtualenv.ecology.state.EntityState;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Mob;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Interface for validating component data before it is set.
 * Validators are registered per handle ID and can perform custom validation logic.
 */
interface ComponentValidator {
	/**
	 * Validates the given component data.
	 *
	 * @param handleId The ID of the handle being validated
	 * @param data The NBT data to validate
	 * @return true if the data is valid, false otherwise
	 */
	boolean validate(String handleId, CompoundTag data);

	/**
	 * Gets the error message describing why validation failed.
	 * Should only be called when validate() returns false.
	 *
	 * @return Error message describing the validation failure
	 */
	String getValidationError();
}

/**
 * Functional interface for listening to changes in component data.
 * Listeners are notified when handle data is modified through setHandleTag().
 */
@FunctionalInterface
interface ComponentChangeListener {
	/**
	 * Called when component data has changed.
	 *
	 * @param handleId The ID of the handle whose data changed
	 * @param oldValue The previous value (null if this is a new tag)
	 * @param newValue The new value
	 */
	void onChanged(String handleId, @Nullable CompoundTag oldValue, CompoundTag newValue);
}


public final class EcologyComponent {
	private static final Logger LOGGER = LoggerFactory.getLogger("BetterEcology");

	private final Mob mob;
	private final Map<String, CompoundTag> handleData = new HashMap<>();
	private final Map<String, CompoundTag> handleTagCache = new HashMap<>();
	private final Map<String, ComponentValidator> validators = new HashMap<>();
	private final Map<String, List<String>> dependencies = new HashMap<>();
	private final Set<String> initializedHandles = new HashSet<>();
	private final List<ComponentChangeListener> listeners = new CopyOnWriteArrayList<>();
	private final Map<String, Integer> componentVersions = new HashMap<>();
	private final EntityState entityState;
	private int profileGeneration = -1;
	private int componentVersion = 0; // Global component version, incremented on each refresh
	private boolean goalsRegistered = false;
	@Nullable
	private EcologyProfile profile;
	private List<EcologyHandle> handles = List.of();
	private int tickCount = -1;
	private CompoundTag[] cachedTags = new CompoundTag[8]; // Fast path for common handles
	private long elapsedTicks = 0; // Ticks since last update (for catch-up simulation)
	private UpdateMode updateMode = UpdateMode.ACTIVE; // Current update mode
	private String wakeReason = "unknown"; // Why the entity woke up (for handler logic)
	@Nullable
	private GeneticDiversityComponent geneticData; // Genetic tracking data

	public EcologyComponent(Mob mob) {
		this.mob = mob;
		this.entityState = new EntityState(mob);
		refresh();
	}


	/**
	 * Adds a change listener to be notified when component data changes.
	 * Thread-safe: can be called concurrently with notifications.
	 *
	 * @param listener The listener to add
	 */
	public void addListener(ComponentChangeListener listener) {
		if (listener != null) {
			listeners.add(listener);
		}
	}

	/**
	 * Removes a change listener.
	 * Thread-safe: can be called concurrently with notifications.
	 *
	 * @param listener The listener to remove
	 * @return true if the listener was present and removed
	 */
	public boolean removeListener(ComponentChangeListener listener) {
		return listeners.remove(listener);
	}

	public void refreshIfNeeded() {
		if (profileGeneration != EcologyProfileRegistry.generation()) {
			refresh();
		}
		// Prepare entity state for new tick (lazy evaluation)
		if (tickCount != mob.tickCount) {
			tickCount = mob.tickCount;
			entityState.prepareForTick();
			// Clear tag cache at start of tick
			handleTagCache.clear();
			// Reset fast path cache
			cachedTags = new CompoundTag[8];
		}
	}

	public void refresh() {
		profileGeneration = EcologyProfileRegistry.generation();
		profile = EcologyProfileRegistry.getForMob(mob);

		// Always load the profile first (for behavior configs like steering)
		// Then merge with code-based handles if present
		AnimalConfig codeConfig = AnimalBehaviorRegistry.getForMob(mob);
		if (codeConfig != null) {
			// Merge code-based handles with profile-based handles
			// Code-based handles override profile-based handles with the same ID
			LOGGER.info("EcologyComponent.refresh: found code config for {} with {} handles",
				BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType()),
				codeConfig.getHandles().size());
			handles = mergeHandles(codeConfig.getHandles(), profile);
		} else {
			LOGGER.info("EcologyComponent.refresh: no code config for {}, using profile handles (profile={})",
				BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType()), profile);
			handles = profile == null ? List.of() : EcologyProfileRegistry.getHandles(profile);
		}

		LOGGER.info("EcologyComponent.refresh: final handle count for {}: {}",
			BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType()), handles.size());

		// Increment global component version
		componentVersion++;

		// Preserve versions for handles that still exist, clean up removed ones
		Map<String, Integer> newVersions = new HashMap<>();
		for (EcologyHandle handle : handles) {
			String handleId = handle.id();
			Integer oldVersion = componentVersions.get(handleId);
			if (oldVersion != null) {
				// Preserve existing version
				newVersions.put(handleId, oldVersion);
			} else {
				// New handle, start at current global version
				newVersions.put(handleId, componentVersion);
			}
		}

		// Replace versions map, removing handles that no longer exist
		componentVersions.clear();
		componentVersions.putAll(newVersions);

		goalsRegistered = false;
		handleData.clear();
		handleTagCache.clear();
		cachedTags = new CompoundTag[8];
		initializedHandles.clear();

		// Clear and re-register validators if defined in profile
		validators.clear();

		// Load dependencies from profile if available
		dependencies.clear();
		if (profile != null) {
			loadDependenciesFromProfile();
		}
	}

	/**
	 * Loads component dependencies from the ecology profile.
	 * Dependencies are expected to be defined in the profile under "handle_dependencies".
	 */
	private void loadDependenciesFromProfile() {
		if (profile == null) {
			return;
		}

		Map<String, Object> depsMap = profile.getMap("handle_dependencies");
		if (depsMap == null) {
			return;
		}

		for (Map.Entry<String, Object> entry : depsMap.entrySet()) {
			String handleId = entry.getKey();
			if (entry.getValue() instanceof List<?> depList) {
				// Convert raw list to List<String>
				List<String> stringDeps = new java.util.ArrayList<>();
				for (Object dep : depList) {
					if (dep instanceof String depId) {
						stringDeps.add(depId);
					}
				}
				if (!stringDeps.isEmpty()) {
					dependencies.put(handleId, stringDeps);
				}
			}
		}

		if (!dependencies.isEmpty()) {
			LOGGER.debug("Loaded {} handle dependencies from profile", dependencies.size());
		}
	}

	/**
	 * Merges code-based handles with profile-based handles.
	 * Code-based handles override profile-based handles with the same ID.
	 * This allows code-based entities to still use profile-based features like steering behaviors.
	 *
	 * @param codeHandles Handles from code-based config
	 * @param profile The ecology profile (may be null)
	 * @return Merged list of handles
	 */
	private List<EcologyHandle> mergeHandles(List<EcologyHandle> codeHandles, @Nullable EcologyProfile profile) {
		if (profile == null) {
			// No profile, return only code-based handles
			return codeHandles;
		}

		// Start with profile-based handles
		List<EcologyHandle> profileHandles = EcologyProfileRegistry.getHandles(profile);
		Map<String, EcologyHandle> mergedMap = new java.util.LinkedHashMap<>();

		// Add all profile handles first
		for (EcologyHandle handle : profileHandles) {
			mergedMap.put(handle.id(), handle);
		}

		// Override with code-based handles (code takes precedence)
		for (EcologyHandle handle : codeHandles) {
			mergedMap.put(handle.id(), handle);
		}

		List<EcologyHandle> result = new java.util.ArrayList<>(mergedMap.values());

		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("Merged {} profile handles with {} code handles for {}",
					profileHandles.size(), codeHandles.size(),
					BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType()));
		}

		return result;
	}

	public boolean hasProfile() {
		return profile != null;
	}

	@Nullable
	public EcologyProfile profile() {
		return profile;
	}

	public List<EcologyHandle> handles() {
		return handles;
	}

	/**
	 * Adds a dependency relationship between handles.
	 * The dependent handle requires all specified dependencies to be available.
	 *
	 * @param handleId The handle ID that requires dependencies
	 * @param dependencyId The handle ID that is required
	 */
	public void addDependency(String handleId, String dependencyId) {
		dependencies.computeIfAbsent(handleId, k -> new java.util.ArrayList<>()).add(dependencyId);
	}

	/**
	 * Checks if a handle has unmet dependencies.
	 * A dependency is considered unmet if the dependency handle ID is not present in the handles list.
	 *
	 * @param handleId The handle ID to check
	 * @return true if the handle has unmet dependencies, false otherwise
	 */
	public boolean hasUnmetDependencies(String handleId) {
		List<String> deps = dependencies.get(handleId);
		if (deps == null || deps.isEmpty()) {
			return false;
		}

		// Check if all dependencies are satisfied (i.e., the handle exists in the handles list)
		for (String depId : deps) {
			boolean depExists = handles.stream().anyMatch(h -> h.id().equals(depId));
			if (!depExists) {
				LOGGER.warn("Handle '{}' requires dependency '{}' which is not available", handleId, depId);
				return true;
			}
		}

		return false;
	}

	/**
	 * Gets the list of dependencies for a handle.
	 *
	 * @param handleId The handle ID to get dependencies for
	 * @return An immutable list of dependency handle IDs, or an empty list if none exist
	 */
	public List<String> getDependencies(String handleId) {
		List<String> deps = dependencies.get(handleId);
		return deps == null ? List.of() : List.copyOf(deps);
	}

	/**
	 * Get handle tag with per-tick caching to reduce HashMap lookups.
	 * Tags are cached once per tick and reused for subsequent accesses.
	 * Uses fast array indexing for common handles (hunger, thirst, breeding, social).
	 * Handles are lazily initialized on first access.
	 */
	public CompoundTag getHandleTag(String id) {
		// Initialize handle on first access
		initializeHandle(id);

		// Fast path for common handles using indexed cache
		int fastIndex = getFastHandleIndex(id);
		if (fastIndex >= 0) {
			CompoundTag tag = cachedTags[fastIndex];
			if (tag == null) {
				tag = handleData.computeIfAbsent(id, key -> new CompoundTag());
				cachedTags[fastIndex] = tag;
			}
			return tag;
		}

		// Check per-tick cache first for other handles
		CompoundTag cached = handleTagCache.get(id);
		if (cached != null) {
			return cached;
		}

		// Get from persistent storage
		CompoundTag tag = handleData.computeIfAbsent(id, key -> new CompoundTag());
		handleTagCache.put(id, tag);
		return tag;
	}

	/**
	 * Maps common handle IDs to array indices for fast access.
	 * Returns -1 for handles not in the fast path.
	 */
	private int getFastHandleIndex(String id) {
		return switch (id) {
			case "hunger" -> 0;
			case "thirst" -> 1;
			case "breeding" -> 2;
			case "social" -> 3;
			case "age" -> 4;
			case "health" -> 5;
			case "energy" -> 6;
			case "movement" -> 7;
			default -> -1;
		};
	}

	/**
	 * Checks if a handle has been initialized.
	 *
	 * @param handleId The handle ID to check
	 * @return true if the handle has been initialized, false otherwise
	 */
	public boolean isHandleInitialized(String handleId) {
		return initializedHandles.contains(handleId);
	}

	/**
	 * Initializes a handle on first access.
	 * Checks if the handle is registered, calls its initializer if available,
	 * and marks it as initialized. This method is idempotent - calling it
	 * multiple times for the same handle will only initialize once.
	 *
	 * @param handleId The handle ID to initialize
	 */
	public void initializeHandle(String handleId) {
		// Early return if already initialized
		if (initializedHandles.contains(handleId)) {
			return;
		}

		// Find the handle in the registered handles list
		EcologyHandle handle = handles.stream()
			.filter(h -> h.id().equals(handleId))
			.findFirst()
			.orElse(null);

		if (handle == null) {
			// Handle not registered - this is normal for dynamic tag access
			return;
		}

		// Mark as initialized before calling initializer to prevent recursion
		initializedHandles.add(handleId);

		// Call the handle's initializer
		try {
			handle.initialize(mob, this, profile);
		} catch (Exception e) {
			LOGGER.error("Failed to initialize handle '{}': {}", handleId, e.getMessage(), e);
		}
	}

	/**
	 * Clears all initialized handle states.
	 * This allows handles to be re-initialized on next access.
	 * Primarily useful for testing and debugging purposes.
	 */
	public void clearInitializedHandles() {
		initializedHandles.clear();
	}

	/**
	 * Sets the handle tag after validating the data and notifies listeners if changed.
	 * If a validator is registered for this handle and validation fails,
	 * a warning is logged and the data is not set.
	 * Comparison uses CompoundTag.equals() to check if tags are different.
	 * Notifies all registered listeners with old and new values.
	 * Increments the handle version when data is modified.
	 *
	 * @param id The handle ID
	 * @param tag The NBT data to set
	 */
	public void setHandleTag(String id, CompoundTag tag) {
		ComponentValidator validator = validators.get(id);
		if (validator != null) {
			if (!validator.validate(id, tag)) {
				LOGGER.warn("Validation failed for handle '{}': {}", id, validator.getValidationError());
				return;
			}
		}

		// Store old value for change detection
		CompoundTag oldValue = handleData.get(id);

		// Check if value actually changed
		boolean hasChanged = (oldValue == null && tag != null) ||
			                 (oldValue != null && !oldValue.equals(tag));

		// Update the data
		handleData.put(id, tag);

		// Increment version for this handle when data is modified
		if (hasChanged) {
			componentVersions.merge(id, 1, Integer::sum);
			notifyListeners(id, oldValue, tag);
		}
	}

	/**
	 * Notifies all registered listeners of a data change.
	 * Uses CopyOnWriteArrayList to allow concurrent modification during iteration.
	 *
	 * @param handleId The handle ID that changed
	 * @param oldValue The previous value
	 * @param newValue The new value
	 */
	private void notifyListeners(String handleId, @Nullable CompoundTag oldValue, CompoundTag newValue) {
		for (ComponentChangeListener listener : listeners) {
			try {
				listener.onChanged(handleId, oldValue, newValue);
			} catch (Exception e) {
				LOGGER.error("Error notifying component change listener", e);
			}
		}
	}


	/**
	 * Registers a validator for the specified handle ID.
	 *
	 * @param handleId The handle ID to register the validator for
	 * @param validator The validator to register
	 */
	public void registerValidator(String handleId, ComponentValidator validator) {
		validators.put(handleId, validator);
	}

	/**
	 * Unregisters the validator for the specified handle ID.
	 *
	 * @param handleId The handle ID to unregister the validator for
	 */
	public void unregisterValidator(String handleId) {
		validators.remove(handleId);
	}

	/**
	 * Validates the current data for the specified handle.
	 *
	 * @param handleId The handle ID to validate
	 * @return true if validation passes or no validator is registered, false otherwise
	 */
	public boolean validateHandle(String handleId) {
		ComponentValidator validator = validators.get(handleId);
		if (validator == null) {
			return true; // No validator means validation passes
		}

		CompoundTag data = handleData.get(handleId);
		if (data == null) {
			data = new CompoundTag();
		}

		return validator.validate(handleId, data);
	}

	/**
	 * Validates all handles that have registered validators.
	 *
	 * @return A map of handle IDs to error messages for any validations that failed.
	 *         Empty map if all validations pass or no validators are registered.
	 */
	public Map<String, String> validateAll() {
		Map<String, String> errors = new HashMap<>();
		for (Map.Entry<String, ComponentValidator> entry : validators.entrySet()) {
			String handleId = entry.getKey();
			ComponentValidator validator = entry.getValue();

			CompoundTag data = handleData.get(handleId);
			if (data == null) {
				data = new CompoundTag();
			}

			if (!validator.validate(handleId, data)) {
				errors.put(handleId, validator.getValidationError());
			}
		}
		return errors;
	}

	public boolean markGoalsRegistered() {
		if (goalsRegistered) {
			return false;
		}
		goalsRegistered = true;
		return true;
	}

	/**
	 * Get the dynamic state tracker for this entity.
	 * States are computed fresh each tick based on entity properties.
	 */
	public EntityState state() {
		return entityState;
	}

	/**
	 * Get the number of elapsed ticks since last update.
	 * Handlers use this for catch-up simulation - if 100 ticks have passed,
	 * hunger should decrease by 100x the normal rate.
	 *
	 * @return Number of ticks since last update (0 for regular updates)
	 */
	public long elapsedTicks() {
		return elapsedTicks;
	}

	/**
	 * Set the elapsed ticks for this update.
	 * Called by EcologyHooks when waking up a sleeping entity.
	 */
	public void setElapsedTicks(long elapsed) {
		this.elapsedTicks = elapsed;
	}

	/**
	 * Get the current update mode.
	 * Handlers can use this to optimize their behavior.
	 */
	public UpdateMode updateMode() {
		return updateMode;
	}

	/**
	 * Set the update mode.
	 * Called by EcologyHooks to indicate how handlers should process this tick.
	 */
	public void setUpdateMode(UpdateMode mode) {
		this.updateMode = mode;
	}

	/**
	 * Get the reason why this entity woke up.
	 * Handlers can use this to optimize their behavior.
	 */
	public String wakeReason() {
		return wakeReason;
	}

	/**
	 * Set the wake reason.
	 * Called by EcologyHooks when waking up a sleeping entity.
	 */
	public void setWakeReason(String reason) {
		this.wakeReason = reason;
	}

	/**
	 * Get the global component version number.
	 * This version is incremented on each refresh and can be used
	 * to detect if any component data has been reloaded.
	 *
	 * @return The current global component version
	 */
	public int componentVersion() {
		return componentVersion;
	}

	/**
	 * Get the version number for a specific handle.
	 * The version is incremented when the handle's data is modified.
	 *
	 * @param handleId The ID of the handle to query
	 * @return The current version of the handle, or 0 if the handle doesn't exist
	 */
	public int getHandleVersion(String handleId) {
		return componentVersions.getOrDefault(handleId, 0);
	}

	/**
	 * Check if a handle has been updated since a given version.
	 * This is useful for detecting hot-reload changes during runtime.
	 *
	 * @param handleId The ID of the handle to check
	 * @param knownVersion The version to compare against
	 * @return true if the handle has been updated since knownVersion, false otherwise
	 */
	public boolean isHandleOutdated(String handleId, int knownVersion) {
		Integer currentVersion = componentVersions.get(handleId);
		if (currentVersion == null) {
			// Handle doesn't exist, consider it outdated
			return true;
		}
		return currentVersion > knownVersion;
	}

	/**
	 * Gets the genetic diversity data for this entity.
	 * Creates a new component if it doesn't exist.
	 *
	 * @return Genetic diversity component
	 */
	public GeneticDiversityComponent getGeneticData() {
		if (geneticData == null) {
			CompoundTag tag = getHandleTag("genetic_data");
			geneticData = new GeneticDiversityComponent(tag);
			UUID entityUuid = mob.getUUID();
			LineageRegistry.registerFromNbt(entityUuid, tag);
		}
		return geneticData;
	}

	/**
	 * Sets the genetic diversity data for this entity.
	 * Also updates the LineageRegistry with the new data.
	 *
	 * @param data Genetic diversity component
	 */
	public void setGeneticData(@Nullable GeneticDiversityComponent data) {
		this.geneticData = data;
		if (data != null) {
			CompoundTag tag = getHandleTag("genetic_data");
			tag.merge(data.getData());
			LineageRegistry.registerFromNbt(mob.getUUID(), tag);
		}
	}

	/**
	 * Gets the mob entity that this component is attached to.
	 *
	 * @return The mob entity
	 */
	public Mob getMob() {
		return mob;
	}
}
