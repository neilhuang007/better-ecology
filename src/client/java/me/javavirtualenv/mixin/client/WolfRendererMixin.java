package me.javavirtualenv.mixin.client;

import me.javavirtualenv.client.render.WolfItemInMouthLayer;
import net.minecraft.client.model.WolfModel;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.WolfRenderer;
import net.minecraft.world.entity.animal.Wolf;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin that adds the item-in-mouth render layer to wolves.
 *
 * <p>This makes wolves visually hold items in their mouths, similar to how
 * foxes do in vanilla Minecraft. This is a more "vanilla-like" approach
 * because it uses the same rendering pattern that Mojang uses for foxes.
 *
 * <p>Realism notes:
 * <ul>
 *   <li>Real wolves carry food in their mouths to bring back to pups</li>
 *   <li>Foxes in Minecraft already exhibit this behavior</li>
 *   <li>This creates visual consistency between similar canid mobs</li>
 * </ul>
 */
@Mixin(WolfRenderer.class)
public abstract class WolfRendererMixin extends MobRenderer<Wolf, WolfModel<Wolf>> {

    // Dummy constructor required by mixin
    public WolfRendererMixin(EntityRendererProvider.Context context, WolfModel<Wolf> model, float shadowRadius) {
        super(context, model, shadowRadius);
    }

    /**
     * Adds the item-in-mouth layer after the wolf renderer is constructed.
     */
    @Inject(method = "<init>", at = @At("TAIL"))
    private void betterEcology$addItemLayer(EntityRendererProvider.Context context, CallbackInfo ci) {
        this.addLayer(new WolfItemInMouthLayer(this, context.getItemInHandRenderer()));
    }
}
