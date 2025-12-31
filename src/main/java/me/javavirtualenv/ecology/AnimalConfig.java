package me.javavirtualenv.ecology;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

/**
 * Code-based configuration for animal behaviors.
 * Replaces YAML-driven configuration with direct Java values.
 */
public final class AnimalConfig {
    private final ResourceLocation entityId;
    private final List<EcologyHandle> handles;

    private AnimalConfig(Builder builder) {
        this.entityId = builder.entityId;
        this.handles = List.copyOf(builder.handles);
    }

    public ResourceLocation entityId() {
        return entityId;
    }

    public List<EcologyHandle> getHandles() {
        return handles;
    }

    /**
     * Creates a new builder for AnimalConfig.
     */
    public static Builder builder(ResourceLocation entityId) {
        return new Builder(entityId);
    }

    /**
     * Builder pattern for constructing AnimalConfig instances.
     */
    public static final class Builder {
        private final ResourceLocation entityId;
        private final List<EcologyHandle> handles = new ArrayList<>();

        private Builder(ResourceLocation entityId) {
            this.entityId = entityId;
        }

        /**
         * Add a handle to this configuration.
         */
        public Builder addHandle(EcologyHandle handle) {
            handles.add(handle);
            return this;
        }

        /**
         * Add multiple handles to this configuration.
         */
        public Builder addHandles(List<EcologyHandle> handlesToAdd) {
            handles.addAll(handlesToAdd);
            return this;
        }

        /**
         * Add a handle conditionally.
         */
        public Builder addHandleIf(boolean condition, EcologyHandle handle) {
            if (condition) {
                handles.add(handle);
            }
            return this;
        }

        /**
         * Build the final AnimalConfig instance.
         */
        public AnimalConfig build() {
            return new AnimalConfig(this);
        }
    }
}
