package me.javavirtualenv.mixin.villager;

import me.javavirtualenv.behavior.core.AnimalThresholds;
import me.javavirtualenv.behavior.core.FleeFromPredatorGoal;
import me.javavirtualenv.behavior.core.SeekWaterGoal;
import me.javavirtualenv.mixin.MobAccessor;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.npc.WanderingTrader;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin that registers ecology-based goals for WanderingTrader.
 * Wandering traders have complex vanilla behavior (trading, despawning, llama companions).
 * We keep behaviors minimal to avoid conflicts.
 */
@Mixin(WanderingTrader.class)
public abstract class WanderingTraderMixin {

    /**
     * Register ecology goals after the wandering trader is constructed.
     * Wandering traders use the Brain AI system but also have a goalSelector that can be used.
     */
    @Inject(method = "<init>(Lnet/minecraft/world/entity/EntityType;Lnet/minecraft/world/level/Level;)V", at = @At("TAIL"))
    private void betterEcology$init(EntityType<?> entityType, Level level, CallbackInfo ci) {
        WanderingTrader trader = (WanderingTrader) (Object) this;
        var goalSelector = ((MobAccessor) trader).getGoalSelector();

        // Priority 1: Flee from hostile mobs (zombies, etc.)
        goalSelector.addGoal(
            AnimalThresholds.PRIORITY_FLEE,
            new FleeFromPredatorGoal(
                trader,
                1.3,  // speed multiplier when fleeing
                16,   // detection range
                24,   // flee distance
                Zombie.class
            )
        );

        // Priority 3: Seek water when thirsty
        goalSelector.addGoal(
            AnimalThresholds.PRIORITY_NORMAL,
            new SeekWaterGoal(trader, 1.0, 20)
        );
    }
}
