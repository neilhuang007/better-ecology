package me.javavirtualenv.mixin.animal;

import me.javavirtualenv.ecology.AnimalBehaviorRegistry;
import me.javavirtualenv.ecology.AnimalConfig;
import me.javavirtualenv.ecology.CodeBasedHandle;
import me.javavirtualenv.ecology.EcologyComponent;
import me.javavirtualenv.ecology.EcologyProfile;
import me.javavirtualenv.ecology.ai.LowHealthFleeGoal;
import me.javavirtualenv.ecology.handles.*;
import me.javavirtualenv.mixin.MobAccessor;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin for Cat-specific behavior registration.
 * <p>
 * Cats are tameable, carnivorous predators that hunt phantoms and creepers.
 * They exhibit crepuscular activity patterns, avoid water, and can gift items to players.
 * <p>
 * Key behaviors:
 * - Hunts rabbits, chickens, phantoms, and creepers
 * - Flees from wolves and ocelots
 * - Tameable with cod and salmon
 * - Avoids water and seeks shelter during rain
 * - Crepuscular activity pattern (most active at dawn/dusk)
 * <p>
 * Special feline behaviors:
 * - Stalking and pouncing on prey
 * - Quiet stealth movement
 * - Gift giving to trusted players
 * - Creeper detection and deterrence
 * - Phantom repelling while sleeping
 * - Social behaviors (purring, hissing, rubbing affection)
 * - Sleeping on beds, chests, and furnaces
 * - Landing on feet (no fall damage)
 * - Play behavior with items and players
 */
@Mixin(Cat.class)
public abstract class CatMixin {

    private static final String CAT_ID = "minecraft:cat";
    private static boolean catBehaviorsRegistered = false;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void onInit(EntityType<? extends Cat> entityType, Level level, CallbackInfo ci) {
        registerBehaviors();
    }

    protected void registerBehaviors() {
        if (catBehaviorsRegistered) {
            return;
        }

        AnimalConfig config = AnimalConfig.builder(
                net.minecraft.resources.ResourceLocation.parse(CAT_ID))
                .addHandle(new HungerHandle())
                .addHandle(new ConditionHandle())
                .addHandle(new EnergyHandle())
                .addHandle(new AgeHandle())
                .addHandle(new MovementHandle())
                .addHandle(new TemporalHandle())
                .addHandle(new DietHandle())
                .addHandle(new CatPredationHandle())
                .addHandle(new BreedingHandle())
                .addHandle(new FelineBehaviorHandle())
                .build();

        AnimalBehaviorRegistry.register(CAT_ID, config);
        catBehaviorsRegistered = true;
    }

    protected boolean areBehaviorsRegistered() {
        return catBehaviorsRegistered;
    }

    protected void markBehaviorsRegistered() {
        catBehaviorsRegistered = true;
    }

    /**
     * Predation handle for cats with low health flee behavior.
     * Cats are agile predators that will flee when injured.
     */
    private static final class CatPredationHandle extends CodeBasedHandle {

        @Override
        public String id() {
            return "predation";
        }

        @Override
        public void registerGoals(Mob mob, EcologyComponent component, EcologyProfile profile) {
            if (!(mob instanceof Cat cat)) {
                return;
            }

            MobAccessor accessor = (MobAccessor) mob;

            // Register low health flee goal (highest priority)
            // Cats flee at 55% health with 1.5 speed multiplier (cats are agile)
            accessor.betterEcology$getGoalSelector().addGoal(1,
                new LowHealthFleeGoal(cat, 0.55, 1.5));
        }
    }
}
