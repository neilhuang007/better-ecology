package me.javavirtualenv.ecology.mixin;

import java.util.UUID;
import me.javavirtualenv.ecology.EcologyComponent;
import me.javavirtualenv.ecology.api.EcologyAccess;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.animal.Animal;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

/**
 * Mixin to capture animal breeding events and track mother-offspring relationships.
 * When a baby is born from breeding, we store the mother's UUID in the baby's ecology component.
 * This enables behaviors like following mother, mother protection, and separation distress.
 */
@Mixin(Animal.class)
public class AnimalBreedingMixin {

    /**
     * Injects at the end of spawnChildFromBreeding to capture the baby and set mother UUID.
     * The parent entity is always the mother in this method (the other parent is the parameter).
     */
    @Inject(
            method = "spawnChildFromBreeding",
            at = @At("RETURN"),
            locals = LocalCapture.CAPTURE_FAILHARD
    )
    private void betterEcology$onChildBorn(
            ServerLevel level,
            Animal otherParent,
            AgeableMob baby,
            CallbackInfo ci
    ) {
        if (baby == null) {
            return;
        }

        Animal mother = (Animal) (Object) this;
        UUID motherUuid = mother.getUUID();

        if (baby instanceof EcologyAccess access) {
            EcologyComponent component = access.betterEcology$getEcologyComponent();
            if (component != null) {
                me.javavirtualenv.ecology.handles.ParentChildHandle.setMotherUuid(component, motherUuid);
            }
        }
    }
}
