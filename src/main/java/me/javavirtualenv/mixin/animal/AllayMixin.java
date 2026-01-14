package me.javavirtualenv.mixin.animal;

import me.javavirtualenv.behavior.core.AnimalThresholds;
import me.javavirtualenv.behavior.core.FleeFromPredatorGoal;
import me.javavirtualenv.behavior.core.HerdCohesionGoal;
import me.javavirtualenv.mixin.MobAccessor;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.allay.Allay;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.monster.Skeleton;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Spider;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin that registers ecology-based goals for Allay.
 * Allays are peaceful flying mobs that flock together and flee from hostile mobs.
 * Minimal behaviors to avoid conflicts with vanilla item collection mechanics.
 */
@Mixin(Allay.class)
public abstract class AllayMixin {

    /**
     * Register ecology goals after the allay is initialized.
     * Allays use the Brain AI system but still have a goalSelector.
     */
    @Inject(method = "<init>(Lnet/minecraft/world/entity/EntityType;Lnet/minecraft/world/level/Level;)V", at = @At("TAIL"))
    private void betterEcology$init(EntityType<?> entityType, Level level, CallbackInfo ci) {
        Allay allay = (Allay) (Object) this;
        var goalSelector = ((MobAccessor) allay).getGoalSelector();

        // Priority 1: Flee from hostile mobs (zombies, skeletons, creepers, spiders)
        // Allays are fragile and should avoid danger
        goalSelector.addGoal(
            AnimalThresholds.PRIORITY_FLEE,
            new FleeFromPredatorGoal(
                allay,
                1.5,  // speed multiplier when fleeing (allays fly fast when scared)
                10,   // detection range
                16,   // flee distance
                Zombie.class,
                Skeleton.class,
                Creeper.class,
                Spider.class
            )
        );

        // Priority 5: Flock with other allays
        // Allays stay together in groups
        goalSelector.addGoal(
            AnimalThresholds.PRIORITY_SOCIAL,
            new HerdCohesionGoal(allay, Allay.class)
        );
    }
}
