package me.javavirtualenv.mixin.villager;

import me.javavirtualenv.behavior.core.AnimalThresholds;
import me.javavirtualenv.behavior.core.FollowParentGoal;
import me.javavirtualenv.behavior.core.SeekWaterGoal;
import me.javavirtualenv.mixin.MobAccessor;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin that registers ecology-based goals for Villager.
 * Villagers have complex vanilla behaviors (trading, professions, schedules),
 * so we keep ecology behaviors minimal to avoid conflicts.
 *
 * Behaviors added:
 * - Baby villagers follow adult villagers
 * - Villagers seek water when thirsty
 */
@Mixin(Villager.class)
public abstract class VillagerMixin {

    /**
     * Register ecology goals after the villager is constructed.
     * Villagers use the Brain AI system but also have a goalSelector that can be used.
     */
    @Inject(method = "<init>(Lnet/minecraft/world/entity/EntityType;Lnet/minecraft/world/level/Level;)V", at = @At("TAIL"))
    private void betterEcology$init(EntityType<?> entityType, Level level, CallbackInfo ci) {
        Villager villager = (Villager) (Object) this;
        var goalSelector = ((MobAccessor) villager).getGoalSelector();

        // Priority 3: Seek water when thirsty
        goalSelector.addGoal(
            AnimalThresholds.PRIORITY_NORMAL,
            new SeekWaterGoal(villager, 1.0, 24)
        );

        // Priority 5: Baby villagers follow adult villagers
        // Using PRIORITY_SOCIAL to avoid interfering with trading behavior
        goalSelector.addGoal(
            AnimalThresholds.PRIORITY_SOCIAL,
            new FollowParentGoal(villager, Villager.class)
        );
    }
}
