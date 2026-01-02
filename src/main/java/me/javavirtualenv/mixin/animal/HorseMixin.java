package me.javavirtualenv.mixin.animal;

import me.javavirtualenv.ecology.AnimalBehaviorRegistry;
import me.javavirtualenv.ecology.AnimalConfig;
import me.javavirtualenv.ecology.handles.AgeHandle;
import me.javavirtualenv.ecology.handles.BehaviorHandle;
import me.javavirtualenv.ecology.handles.BreedingHandle;
import me.javavirtualenv.ecology.handles.ConditionHandle;
import me.javavirtualenv.ecology.handles.DietHandle;
import me.javavirtualenv.ecology.handles.EnergyHandle;
import me.javavirtualenv.ecology.handles.HealthHandle;
import me.javavirtualenv.ecology.handles.HungerHandle;
import me.javavirtualenv.ecology.handles.MovementHandle;
import me.javavirtualenv.ecology.handles.PredationHandle;
import me.javavirtualenv.ecology.handles.SocialHandle;
import me.javavirtualenv.ecology.handles.SpawnHandle;
import me.javavirtualenv.ecology.handles.TemporalHandle;
import me.javavirtualenv.behavior.horse.HorseBehaviorHandle;
import me.javavirtualenv.ecology.ai.LowHealthFleeGoal;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.horse.Horse;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin for Horse behavior registration.
 * Horses are rideable herd animals with the following key characteristics:
 * - Herbivores that graze on grass and eat wheat/hay blocks/apples
 * - Social animals that form herds with alarm calls and group cohesion
 * - Diurnal activity pattern with reduced night activity
 * - High movement speed with variable jump height (1.0 to 5.5 blocks)
 * - Flee from predators (wolves) with alarm calls to group
 * - Can be tamed and ridden with special mounting mechanics
 * - Parental care (offspring follow parents)
 * - Seed dispersers through gut passage
 * - Large biomass (450) with moderate health (11.25 hearts)
 *
 * The YAML profile at data/better-ecology/mobs/passive/horse/mod_registry.yaml
 * defines all behavior parameters for horses including:
 * - Identity: Domesticated large herbivore, herd animal
 * - Physical: Variable health, large size, high speed, capable jumper
 * - Internal State: Hunger (slow decay), condition, energy, age, social needs
 * - Temporal: Diurnal with reduced night activity, weather responses
 * - Spatial: Prefers grassy plains and savannas, large home range (96 blocks)
 * - Diet: Grass blocks (grazing), wheat, hay blocks, apples, sugar
 * - Predation: Flee from wolves, alarm calls trigger group flee
 * - Reproduction: Sexual with parental care, density-dependent breeding
 * - Social: Herd with group cohesion, alarm calls, contact calls
 * - Environmental: Converts grass to dirt when grazing, seed dispersal
 * - Player: Fearful but habitable, tameable by repeated mounting, rideable
 * - Aesthetics: Various idle behaviors (head shake, tail swish, ear twitch)
 */
@Mixin(Horse.class)
public abstract class HorseMixin {

    private static final ResourceLocation HORSE_ID = ResourceLocation.fromNamespaceAndPath("minecraft", "horse");
    private static boolean behaviorsRegistered = false;

    /**
     * Register horse behaviors using the YAML profile configuration.
     * The horse YAML defines comprehensive behavior systems:
     * - Hunger: Enabled with slow decay (0.015), max 100, starvation damage at 5
     * - Condition: Enabled with thresholds from excellent (85) to critical (10)
     * - Energy: Enabled with good stamina recovery (0.6), costs for sprinting/fleeing/swimming
     * - Age: Enabled, baby duration 24000 ticks, mature at 24000, no elderly/max age
     * - Social: Enabled, mild needs, loneliness threshold 35, herd cohesion
     * - Movement: Walk 0.225, run 0.45, can swim/jump, avoids cliffs (threshold 5)
     * - Health: Base 11.25 hearts (variable per individual), baby multiplier 0.5
     * - Size: Width 1.4, height 1.6, baby scale 0.5
     * - Diet: Grass blocks (grazing converts to dirt), wheat, hay blocks, apples, sugar
     * - Predation: Prey to wolves, flee response with speed 1.5x, alarm calls radius 32
     * - Breeding: Enabled, requires min age/health/condition, density-dependent
     * - Temporal: Daily cycle schedule (morning 1.0, night 0.3), weather modifiers
     * - Spawn: Weight 5, groups of 2-6, requires grass blocks in plains/savanna
     * - Behaviors: Head shake, tail swish, ear twitch, graze animation
     */
    protected void registerBehaviors() {
        if (behaviorsRegistered) {
            return;
        }

        AnimalConfig config = AnimalConfig.builder(HORSE_ID)
                // Internal state systems
                .addHandle(new HungerHandle())
                .addHandle(new ConditionHandle())
                .addHandle(new EnergyHandle())
                .addHandle(new AgeHandle())
                .addHandle(new SocialHandle())

                // Physical capabilities
                .addHandle(new MovementHandle())
                .addHandle(new HealthHandle())

                // Behavioral systems
                .addHandle(new DietHandle())
                .addHandle(new PredationHandle())
                .addHandle(new BreedingHandle())
                .addHandle(new TemporalHandle())

                // Population and aesthetics
                .addHandle(new SpawnHandle())
                // Note: BehaviorHandle comes from profile via mergeHandles

                // Horse-specific behaviors
                .addHandle(new HorseBehaviorHandle())
                .build();

        AnimalBehaviorRegistry.register(HORSE_ID.toString(), config);
        behaviorsRegistered = true;
    }

    /**
     * Injection point after Horse constructor.
     * Registers horse behaviors once when the first Horse entity is created.
     */
    @Inject(method = "<init>", at = @At("RETURN"))
    private void onInit(EntityType<? extends Horse> entityType, Level level, CallbackInfo ci) {
        registerBehaviors();

        // Register AI goals for this specific horse
        Horse horse = (Horse) (Object) this;
        registerHorseGoals(horse);
    }

    /**
     * Register horse-specific AI goals.
     */
    private void registerHorseGoals(Horse horse) {
        me.javavirtualenv.mixin.MobAccessor accessor = (me.javavirtualenv.mixin.MobAccessor) horse;

        // Priority 1: Low health flee (critical survival - horses are flight animals)
        accessor.betterEcology$getGoalSelector().addGoal(1, new LowHealthFleeGoal(horse, 0.55, 1.8));
    }
}
