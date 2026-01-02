package me.javavirtualenv.mixin.animal;

import me.javavirtualenv.behavior.camel.CamelBehaviorHandle;
import me.javavirtualenv.behavior.camel.DesertEnduranceHandle;
import me.javavirtualenv.behavior.camel.SandMovementBehavior;
import me.javavirtualenv.ecology.AnimalBehaviorRegistry;
import me.javavirtualenv.ecology.AnimalConfig;
import me.javavirtualenv.ecology.ai.LowHealthFleeGoal;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.camel.Camel;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin for Camel behavior registration.
 * Camels are desert-adapted herbivores with unique movement capabilities,
 * including the ability to dash and carry two players.
 *
 * Better Ecology adds the following camel-specific behaviors:
 * - Spitting Defense: Camels spit at threats when provoked
 * - Desert Endurance: Reduced hunger/thirst in desert biomes, heat resistance
 * - Caravan Behavior: Camels follow each other in single-file lines
 * - Sand Movement: Efficient movement on sand blocks without sinking
 *
 * Key characteristics from mod_registry.yaml:
 * - Desert habitat preference
 * - Cactus-based diet with health regeneration
 * - Catthemeral activity pattern (active throughout day and night)
 * - Solitary social behavior (forms caravans when traveling)
 * - Mountable with dash ability
 * - No thirst system (water-conserving adaptation)
 */
@Mixin(Camel.class)
public abstract class CamelMixin {

    private static final ResourceLocation CAMEL_ID = ResourceLocation.fromNamespaceAndPath("minecraft", "camel");

    /**
     * Inject into Camel constructor to register behaviors.
     * This ensures behaviors are registered when a Camel entity is created.
     */
    @Inject(method = "<init>", at = @At("RETURN"))
    private void onConstruct(EntityType<? extends Camel> entityType, Level level, CallbackInfo ci) {
        registerBehaviors();

        // Register AI goals for this specific camel
        Camel camel = (Camel) (Object) this;
        registerCamelGoals(camel);
    }

    /**
     * Register camel-specific AI goals.
     */
    private void registerCamelGoals(Camel camel) {
        me.javavirtualenv.mixin.MobAccessor accessor = (me.javavirtualenv.mixin.MobAccessor) camel;

        // Priority 1: Low health flee (critical survival - camels are tough but will flee when hurt)
        accessor.betterEcology$getGoalSelector().addGoal(1, new LowHealthFleeGoal(camel, 0.45, 1.4));
    }

    /**
     * Register camel-specific behaviors using the code-based system.
     *
     * The camel behaviors include:
     * 1. SpittingDefenseBehavior - Spits at threats with warning animation
     * 2. DesertEnduranceHandle - Reduces hunger/thirst in desert, provides heat resistance
     * 3. CaravanBehavior - Forms caravans when traveling with other camels
     * 4. SandMovementBehavior - Efficient movement on sand without sinking
     *
     * Configuration highlights:
     * - Physical: 32 HP, 1.7x2.375 size, walk speed 0.194, run speed 0.41
     * - Movement: Can swim, jump 1.25 blocks, avoids cliffs (threshold 4)
     * - Hunger: Enabled, max 100, starting 80, decay rate 0.015
     * - Thirst: DISABLED (desert adaptation)
     * - Condition: Enabled, max 100, starting 70
     * - Energy: Enabled with sprinting cost 0.3, fleeing cost 0.4, swimming cost 0.2
     * - Age: Baby duration 24000 ticks (20 minutes), maturity at 24000
     * - Temporal: Catthemeral with varying activity levels throughout day
     * - Habitat: Prefers desert and desert_hills biomes
     * - Home range: 64 block radius
     * - Diet: Primary food is cactus (nutrition 20, heals 2 HP)
     * - Predation: Flee response with 1.5x speed multiplier, 48 block flee distance
     * - Reproduction: Sexual breeding, 5 minute cooldown, parental care enabled
     * - Social: Solitary (group size 1), but forms caravans
     * - Spawning: Weight 8, spawns individually on sand/terracotta in desert
     */
    private void registerBehaviors() {
        // Create configuration using builder pattern with camel-specific handles
        AnimalConfig config = AnimalConfig.builder(CAMEL_ID)
                .addHandle(new CamelBehaviorHandle())
                .addHandle(new DesertEnduranceHandle())
                .build();

        // Register the configuration with the global registry
        AnimalBehaviorRegistry.register(CAMEL_ID.toString(), config);
    }
}
