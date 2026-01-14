package me.javavirtualenv.mixin.animal;

import me.javavirtualenv.behavior.core.AnimalThresholds;
import me.javavirtualenv.behavior.core.FleeFromPredatorGoal;
import me.javavirtualenv.behavior.core.FollowTraderGoal;
import me.javavirtualenv.behavior.core.ProtectTraderGoal;
import me.javavirtualenv.behavior.core.SeekWaterGoal;
import me.javavirtualenv.mixin.MobAccessor;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.horse.TraderLlama;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin that registers ecology-based goals for TraderLlama.
 * Trader llamas are protective pack animals that follow and defend wandering traders.
 * They spit at hostile mobs and form defensive formations when the trader is threatened.
 */
@Mixin(TraderLlama.class)
public abstract class TraderLlamaMixin {

    /**
     * Register ecology goals when the trader llama is constructed.
     * Since TraderLlama doesn't override registerGoals, we inject into the constructor.
     */
    @Inject(method = "<init>(Lnet/minecraft/world/entity/EntityType;Lnet/minecraft/world/level/Level;)V", at = @At("TAIL"))
    private void betterEcology$init(EntityType<? extends TraderLlama> entityType, Level level, CallbackInfo ci) {
        TraderLlama llama = (TraderLlama) (Object) this;
        var goalSelector = ((MobAccessor) llama).getGoalSelector();

        // Priority 1: Flee from predators with enhanced speed when trader is threatened
        goalSelector.addGoal(
            AnimalThresholds.PRIORITY_FLEE,
            new FleeFromPredatorGoal(
                llama,
                1.8,  // enhanced speed multiplier when fleeing
                20,   // detection range
                28,   // flee distance
                Zombie.class
            )
        );

        // Priority 2: Protect the wandering trader from hostile mobs
        goalSelector.addGoal(
            AnimalThresholds.PRIORITY_CRITICAL,
            new ProtectTraderGoal(
                llama,
                16.0,  // protection range for finding trader
                16.0,  // threat detection range around trader
                1.3    // speed when moving to defensive position
            )
        );

        // Priority 3: Seek water when thirsty
        goalSelector.addGoal(
            AnimalThresholds.PRIORITY_NORMAL,
            new SeekWaterGoal(llama, 1.0, 20)
        );

        // Priority 4: Follow the wandering trader
        goalSelector.addGoal(
            AnimalThresholds.PRIORITY_HUNT,
            new FollowTraderGoal(llama, 1.1)
        );
    }
}
