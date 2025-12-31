package me.javavirtualenv.ecology.state;

import java.util.Set;

/**
 * A type-safe interface for managing entity component data.
 * <p>
 * This interface provides a contract for storing, retrieving, and managing
 * components attached to entities in a type-safe manner. Components are indexed
 * by their Class type, allowing for clean and type-safe access.
 * <p>
 * Implementations of this interface should handle thread-safety appropriately
 * for their use case. In the context of Minecraft/Fabric modding, most operations
 * will occur on the main server thread, but implementations may need to handle
 * concurrent access depending on their specific usage patterns.
 * <p>
 * This interface extends {@link AutoCloseable} to support resource cleanup
 * when the entity data is no longer needed.
 */
public interface EntityDataInterface extends AutoCloseable {

    /**
     * Removes and returns the component of the given type.
     * <p>
     * If no component of the specified type is present, returns {@code null}.
     *
     * @param <T> The type of component to remove
     * @param type The class of the component to remove
     * @return The removed component, or {@code null} if not present
     * @throws NullPointerException if type is null
     */
    <T> T remove(Class<T> type);

    /**
     * Retrieves the component of the given type without removing it.
     * <p>
     * If no component of the specified type is present, returns {@code null}.
     * <p>
     * This is a non-default method, requiring implementations to provide
     * their own type-safe retrieval logic.
     *
     * @param <T> The type of component to retrieve
     * @param type The class of the component to retrieve
     * @return The component of the given type, or {@code null} if not present
     * @throws NullPointerException if type is null
     */
    <T> T get(Class<T> type);

    /**
     * Stores a component of the given type.
     * <p>
     * If a component of this type already exists, it will be replaced
     * by the new component.
     *
     * @param <T> The type of component to store
     * @param type The class of the component
     * @param component The component instance to store
     * @throws NullPointerException if type or component is null
     */
    <T> void set(Class<T> type, T component);

    /**
     * Checks whether a component of the given type is present.
     *
     * @param <T> The type of component to check for
     * @param type The class of the component to check
     * @return {@code true} if a component of this type exists, {@code false} otherwise
     * @throws NullPointerException if type is null
     */
    <T> boolean has(Class<T> type);

    /**
     * Returns a set of all component types currently stored.
     * <p>
     * The returned set may be unmodifiable. Implementations should document
     * whether modifications to the returned set are supported.
     *
     * @return A set of Class objects representing all stored component types
     */
    Set<Class<?>> keys();

    /**
     * Removes all components from this entity data.
     * <p>
     * After calling this method, {@link #keys()} will return an empty set
     * and all {@link #get(Class)} calls will return {@code null}.
     */
    default void clear() {
        Set<Class<?>> allKeys = keys();
        for (Class<?> key : allKeys) {
            remove(key);
        }
    }

    /**
     * Closes this entity data and releases any associated resources.
     * <p>
     * Implementations should override this method to perform necessary cleanup,
     * such as releasing references or unregistering listeners.
     *
     * @throws Exception if an error occurs during cleanup
     */
    @Override
    default void close() throws Exception {
        clear();
    }
}
