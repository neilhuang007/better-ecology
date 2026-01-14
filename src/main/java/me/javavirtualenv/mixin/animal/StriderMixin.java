package me.javavirtualenv.mixin.animal;

import me.javavirtualenv.behavior.core.AnimalThresholds;
import me.javavirtualenv.behavior.core.FollowParentGoal;
import me.javavirtualenv.behavior.core.HerdCohesionGoal;
import me.javavirtualenv.behavior.core.MotherProtectBabyGoal;
import me.javavirtualenv.mixin.MobAccessor;
import net.minecraft.world.entity.monster.Strider;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin that registers ecology-based goals for Strider.
 * Striders are Nether mobs that walk on lava and exhibit herd behavior.
 * They protect their young and maintain group cohesion.
 */
@Mixin(Strider.class)
public abstract class StriderMixin {

    /**
     * Register ecology goals after the strider's default goals are registered.
     */
    @Inject(method = "registerGoals", at = @At("TAIL"))
    private void betterEcology$registerGoals(CallbackInfo ci) {
        Strider strider = (Strider) (Object) this;
        var goalSelector = ((MobAccessor) strider).getGoalSelector();

        // Priority 4: Baby striders follow adult striders
        goalSelector.addGoal(
            AnimalThresholds.PRIORITY_HUNT,
            new FollowParentGoal(strider, Strider.class)
        );

        // Priority 2: Adult striders protect baby striders
        goalSelector.addGoal(
            AnimalThresholds.PRIORITY_CRITICAL,
            new MotherProtectBabyGoal(strider, Strider.class, 12.0, 16.0, 1.2)
        );

        // Priority 5: Herd cohesion - stay near other striders
        goalSelector.addGoal(
            AnimalThresholds.PRIORITY_SOCIAL,
            new HerdCohesionGoal(strider, Strider.class)
        );
    }
}
