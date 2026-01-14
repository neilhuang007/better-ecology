package me.javavirtualenv.mixin.client;

import net.minecraft.client.model.WolfModel;
import net.minecraft.client.model.geom.ModelPart;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Accessor mixin to access private fields of WolfModel.
 *
 * <p>This is needed to position items in the wolf's mouth correctly,
 * since the head ModelPart is private in the vanilla WolfModel.
 */
@Mixin(WolfModel.class)
public interface WolfModelAccessor {

    /**
     * Gets the head model part for positioning items.
     *
     * @return the head ModelPart
     */
    @Accessor("head")
    ModelPart betterEcology$getHead();
}
