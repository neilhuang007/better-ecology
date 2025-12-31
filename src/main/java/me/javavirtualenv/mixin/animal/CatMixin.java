package me.javavirtualenv.mixin.animal;

import me.javavirtualenv.ecology.AnimalBehaviorRegistry;
import me.javavirtualenv.ecology.AnimalConfig;
import me.javavirtualenv.ecology.handles.*;
import me.javavirtualenv.ecology.EcologyHandle;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

/**
 * Mixin for Cat-specific behavior registration.
 * <p>
 * Cats are tameable, carnivorous predators that hunt phantoms and creepers.
 * They exhibit crepuscular activity patterns, avoid water, and can gift items to players.
 * <p>
 * Key behaviors from YAML:
 * - Hunts rabbits, turtles (specifically babies), phantoms, and creepers
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
 */
@Mixin(Cat.class)
public abstract class CatMixin extends AnimalMixin {

    private static final String CAT_ID = "minecraft:cat";
    private static boolean catBehaviorsRegistered = false;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void onInit(EntityType<? extends Cat> entityType, Level level, CallbackInfo ci) {
        registerBehaviors();
    }

    @Override
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
                .addHandle(new PredationHandle())
                .addHandle(new BreedingHandle())
                .addHandle(new BehaviorHandle())
                .addHandle(new FelineBehaviorHandle())
                .build();

        AnimalBehaviorRegistry.register(CAT_ID, config);
        catBehaviorsRegistered = true;
    }

    @Override
    protected boolean areBehaviorsRegistered() {
        return catBehaviorsRegistered;
    }

    @Override
    protected void markBehaviorsRegistered() {
        catBehaviorsRegistered = true;
    }
}
