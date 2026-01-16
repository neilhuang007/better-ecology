package me.javavirtualenv.mixin;

import me.javavirtualenv.behavior.pathfinding.core.SmoothPathNavigation;
import me.javavirtualenv.behavior.pathfinding.movement.RealisticMoveControl;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin that injects realistic pathfinding into Animal entities.
 * This replaces vanilla navigation and move control with slope-aware, momentum-based alternatives.
 */
@Mixin(Animal.class)
public abstract class AnimalPathfindingMixin extends Mob {

    protected AnimalPathfindingMixin(EntityType<? extends Mob> entityType, Level level) {
        super(entityType, level);
    }

    @Inject(method = "<init>", at = @At("TAIL"))
    private void betterEcology$injectPathfinding(EntityType<? extends Animal> entityType, Level level, CallbackInfo ci) {
        Animal animal = (Animal) (Object) this;

        // Replace navigation with smooth, slope-aware navigation
        SmoothPathNavigation smoothNav = new SmoothPathNavigation(animal, level);
        ((MobNavigationAccessor) animal).setNavigation(smoothNav);

        // Replace move control with momentum-based movement
        RealisticMoveControl realisticMove = new RealisticMoveControl(animal);
        ((MobNavigationAccessor) animal).setMoveControl(realisticMove);
    }
}
