package me.javavirtualenv.mixin.animal;

import me.javavirtualenv.behavior.core.AnimalThresholds;
import me.javavirtualenv.behavior.core.FollowParentGoal;
import me.javavirtualenv.behavior.core.HerdCohesionGoal;
import me.javavirtualenv.behavior.core.HuntPreyGoal;
import me.javavirtualenv.mixin.MobAccessor;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.axolotl.Axolotl;
import net.minecraft.world.entity.animal.Cod;
import net.minecraft.world.entity.animal.Salmon;
import net.minecraft.world.entity.animal.Squid;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin that registers ecology-based goals for Axolotl.
 * Axolotls are aquatic predators that:
 * - Hunt fish, squid, and drowned
 * - Baby axolotls follow adults
 * - Exhibit group behavior with other axolotls
 * - Play dead when damaged (vanilla behavior)
 */
@Mixin(Axolotl.class)
public abstract class AxolotlMixin {

    /**
     * Register ecology goals after the axolotl is initialized.
     * Axolotls use the Brain AI system but still have a goalSelector.
     */
    @Inject(method = "<init>(Lnet/minecraft/world/entity/EntityType;Lnet/minecraft/world/level/Level;)V", at = @At("TAIL"))
    private void betterEcology$init(EntityType<?> entityType, Level level, CallbackInfo ci) {
        Axolotl axolotl = (Axolotl) (Object) this;
        var goalSelector = ((MobAccessor) axolotl).getGoalSelector();

        // Priority 4: Baby axolotls follow adult axolotls
        goalSelector.addGoal(
            AnimalThresholds.PRIORITY_HUNT,
            new FollowParentGoal(axolotl, Axolotl.class)
        );

        // Priority 4: Hunt prey when hungry
        goalSelector.addGoal(
            AnimalThresholds.PRIORITY_HUNT,
            new HuntPreyGoal(
                axolotl,
                1.2,  // speed when hunting
                24,   // hunt range
                Cod.class,
                Salmon.class,
                Squid.class
            )
        );

        // Priority 5: Group behavior with other axolotls
        goalSelector.addGoal(
            AnimalThresholds.PRIORITY_SOCIAL,
            new HerdCohesionGoal(axolotl, Axolotl.class)
        );
    }
}
