package me.javavirtualenv.mixin.animal;

import me.javavirtualenv.behavior.core.AnimalThresholds;
import me.javavirtualenv.behavior.core.FleeFromPredatorGoal;
import me.javavirtualenv.behavior.core.HerdCohesionGoal;
import me.javavirtualenv.behavior.core.SquidInkCloudDefenseGoal;
import me.javavirtualenv.mixin.MobAccessor;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.GlowSquid;
import net.minecraft.world.entity.animal.axolotl.Axolotl;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin that registers ecology-based goals for GlowSquid.
 * Glow squids are deep water variants that school together and flee from axolotls.
 * They do not have baby/adult distinction.
 */
@Mixin(GlowSquid.class)
public abstract class GlowSquidMixin {

    /**
     * Register ecology goals after the glow squid is initialized.
     */
    @Inject(method = "<init>(Lnet/minecraft/world/entity/EntityType;Lnet/minecraft/world/level/Level;)V", at = @At("TAIL"))
    private void betterEcology$init(EntityType<?> entityType, Level level, CallbackInfo ci) {
        GlowSquid glowSquid = (GlowSquid) (Object) this;
        var goalSelector = ((MobAccessor) glowSquid).getGoalSelector();

        // Priority 1: Ink cloud defense when threatened
        // Glow squids eject ink clouds to blind predators and escape
        goalSelector.addGoal(
            AnimalThresholds.PRIORITY_FLEE,
            new SquidInkCloudDefenseGoal(
                glowSquid,
                12,   // detection range
                Axolotl.class
            )
        );

        // Priority 1: Flee from axolotls (primary predator)
        goalSelector.addGoal(
            AnimalThresholds.PRIORITY_FLEE,
            new FleeFromPredatorGoal(
                glowSquid,
                1.4,  // speed multiplier when fleeing
                12,   // detection range
                16,   // flee distance
                Axolotl.class
            )
        );

        // Priority 5: Schooling behavior (stay together with other glow squids)
        goalSelector.addGoal(
            AnimalThresholds.PRIORITY_SOCIAL,
            new HerdCohesionGoal(
                glowSquid,
                GlowSquid.class,
                1.0,  // normal speed when schooling
                16,   // cohesion radius
                2,    // minimum school size
                10.0, // max distance from school
                0.3f  // quorum threshold (30%)
            )
        );
    }
}
