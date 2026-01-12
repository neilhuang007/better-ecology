package me.javavirtualenv.mixin.animal;

import me.javavirtualenv.behavior.rabbit.RabbitBehaviorGoal;
import me.javavirtualenv.behavior.rabbit.RabbitBurrowCachingGoal;
import me.javavirtualenv.behavior.rabbit.RabbitEvasionConfig;
import me.javavirtualenv.behavior.rabbit.RabbitForagingConfig;
import me.javavirtualenv.behavior.rabbit.RabbitThumpConfig;
import me.javavirtualenv.ecology.AnimalBehaviorRegistry;
import me.javavirtualenv.ecology.ai.LowHealthFleeGoal;
import me.javavirtualenv.ecology.ai.SeekFoodItemGoal;
import me.javavirtualenv.ecology.ai.SeekWaterGoal;
import me.javavirtualenv.ecology.AnimalConfig;
import me.javavirtualenv.ecology.EcologyComponent;
import me.javavirtualenv.ecology.api.EcologyAccess;
import me.javavirtualenv.ecology.handles.*;
import me.javavirtualenv.ecology.handles.reproduction.NestBuildingHandle;
import me.javavirtualenv.mixin.MobAccessor;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Rabbit;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin for Rabbit behavior registration with burrow nesting support.
 * <p>
 * Registers rabbit entity with the Better Ecology behavior system.
 * All rabbit-specific behaviors and configurations are defined in:
 * src/main/resources/data/better-ecology/mobs/passive/rabbit/mod_registry.yaml
 * <p>
 * Rabbit characteristics:
 * - Small prey animal with high evasion and speed
 * - Crepuscular activity pattern (active at dawn and dusk)
 * - Herbivorous diet (carrots, dandelions, grass, flowers)
 * - Solitary social structure
 * - Very fast movement speed (walk: 0.3, run: 0.55)
 * - 3 health (1.5 hearts)
 * - Good jumper (1.5 block height)
 * - Breeds with carrots, golden carrots, and dandelions
 * - Flee behavior from wolves, foxes, and ocelots with zigzag evasion
 * - Build burrow systems underground for nesting and shelter
 * - Gather grass, fur, and dirt for burrow construction
 * - Create hidden entrances under bushes for warren systems
 * <p>
 * Special behaviors implemented:
 * <ul>
 *   <li>Thumping - Foot thumping to warn other rabbits of danger</li>
 *   <li>Evasion - Zigzag escape patterns with freezing behavior</li>
 *   <li>Burrowing - Digging and using burrows for safety and nesting</li>
 *   <li>Foraging - Crop eating, snow digging, standing on hind legs</li>
 * </ul>
 */
@Mixin(Rabbit.class)
public abstract class RabbitMixin {

    @Unique
    private static boolean behaviorsRegistered = false;

    /**
     * Constructor injection point for behavior registration.
     */
    @Inject(method = "<init>", at = @At("RETURN"))
    private void onInit(EntityType<? extends Rabbit> entityType, Level level, CallbackInfo ci) {
        Rabbit rabbit = (Rabbit) (Object) this;

        // Register custom behaviors
        if (!areBehaviorsRegistered()) {
            registerBehaviors();
            markBehaviorsRegistered();
        }

        // Add rabbit-specific goals
        EcologyComponent component = ((EcologyAccess) rabbit).betterEcology$getEcologyComponent();
        MobAccessor accessor = (MobAccessor) rabbit;

        // Low health flee goal - highest priority, triggers at 85% health
        accessor.betterEcology$getGoalSelector().addGoal(1, new LowHealthFleeGoal(rabbit, 0.85, 1.6));

        // Primary behavior goal - second highest priority for escape behaviors
        RabbitBehaviorGoal rabbitGoal = new RabbitBehaviorGoal(rabbit, component);
        accessor.betterEcology$getGoalSelector().addGoal(2, rabbitGoal);

        // Water seeking goal - seek water when thirsty
        accessor.betterEcology$getGoalSelector().addGoal(3, new SeekWaterGoal(rabbit, 1.2, 16));

        // Food seeking goal - seek carrots and dandelions when hungry
        accessor.betterEcology$getGoalSelector().addGoal(4, new SeekFoodItemGoal(rabbit, 1.2, 16,
            stack -> stack.is(Items.CARROT) || stack.is(Items.GOLDEN_CARROT) || stack.is(Items.DANDELION)));

        // Food caching goal - lower priority, runs when safe
        RabbitBurrowCachingGoal cachingGoal = new RabbitBurrowCachingGoal(rabbit, component);
        accessor.betterEcology$getGoalSelector().addGoal(6, cachingGoal);
    }

    /**
     * Check if behaviors have been registered.
     */
    @Unique
    private boolean areBehaviorsRegistered() {
        return behaviorsRegistered;
    }

    /**
     * Mark behaviors as registered.
     */
    @Unique
    private void markBehaviorsRegistered() {
        behaviorsRegistered = true;
    }

    /**
     * Register all rabbit behaviors and configurations.
     * <p>
     * Creates an AnimalConfig for the rabbit entity. The actual behavior values
     * (hunger, movement, diet, breeding, etc.) are loaded from the YAML profile
     * by EcologyResourceReloader and processed by the registered handles in
     * EcologyBootstrap (HungerHandle, MovementHandle, DietHandle, etc.).
     * <p>
     * Key rabbit configurations from YAML:
     * <ul>
     *   <li>Identity: PASSIVE, PRIMARY_CONSUMER, HERBIVORE, SOLITARY, CREPUSCULAR</li>
     *   <li>Health: 1.5 (3 HP), baby multiplier: 0.5</li>
     *   <li>Size: width 0.4, height 0.5, baby scale 0.5</li>
     *   <li>Biomass: adult 15, baby 5, transfer efficiency 0.1</li>
     *   <li>Movement: walk 0.3, run 0.55, swim 0.15, jump height 1.5</li>
     *   <li>Capabilities: can swim, can jump, avoids water and cliffs</li>
     *   <li>Conditional modifiers: baby 0.7, fleeing 1.8 (very fast escape)</li>
     *   <li>Hunger: max 100, start 80, decay 0.03/tick (fast metabolism)</li>
     *   <li>Condition: max 100, start 70</li>
     *   <li>Energy: max 100, recovery 0.6/tick, sprint cost 0.4, flee cost 0.5</li>
     *   <li>Age: baby duration 24000 ticks, maturity at 24000</li>
     *   <li>Daily schedule: crepuscular with high activity at dawn (0-3000) and dusk (12000-14000)</li>
     *   <li>Weather: rain reduces activity to 0.5, seeks shelter</li>
     *   <li>Biomes: prefers plains, flower forests, meadows, forests, taiga, snowy plains, desert</li>
     *   <li>Home range: radius 48, returns to burrow area</li>
     *   <li>Shelter: needs shelter for sleeping, breeding, baby rearing, hiding</li>
     *   <li>Diet: primary (carrots, dandelions), secondary (grass, flowers), emergency (tall grass)</li>
     *   <li>Foraging: search radius 16 (32 when hungry, 64 when starving), interval 150 ticks</li>
     *   <li>Predation: prey animal, flees from wolves (extreme threat), foxes (extreme), ocelots (high)</li>
     *   <li>Flee behavior: speed multiplier 1.8, zigzag pattern, flees toward shelter</li>
     *   <li>Reproduction: sexual, min age 24000, min condition 60, min hunger 50</li>
     *   <li>Breeding: cooldown 18000 ticks, instant gestation, 1-3 offspring</li>
     *   <li>Parental care: 12000 ticks duration, offspring follows parent, no protection</li>
     *   <li>Social: solitary, optimal group size 1</li>
     *   <li>NestBuilding: burrow nesting with underground warrens, grass and fur lining</li>
     *   <li>Terrain: destroys crop blocks (carrots, wheat) when foraging (80% chance)</li>
     *   <li>Player interaction: fearful base attitude, habituates through feeding</li>
     *   <li>Player breeding: carrots, golden carrots, dandelions</li>
     *   <li>Drops: rabbit hide, rabbit meat, rabbit foot (10% chance)</li>
     *   <li>Sounds: ambient, hurt, death, baby ambient</li>
     *   <li>Idle behaviors: nose twitch, ear rotation, stand on hind legs, grooming</li>
     *   <li>Personality: very timid (boldness 0.2), moderately active (0.6), low sociability (0.3)</li>
     *   <li>AI priorities: escape_danger (1), flee_predator (2), eat_food (5), find_shelter (6)</li>
     * </ul>
     *
     * Rabbit-specific behavior configurations:
     * <ul>
     *   <li>Evasion: zigzag pattern, freezing before flight, 1.8x speed when fleeing</li>
     *   <li>Thumping: warns nearby rabbits within 12 blocks, chain reaction</li>
     *   <li>Burrowing: digs burrows in valid locations, warren systems with connected nests</li>
     *   <li>Foraging: eats crops (carrots, wheat, etc.), digs through snow</li>
     * </ul>
     */
    @Unique
    private void registerBehaviors() {
        AnimalConfig config = AnimalConfig.builder(ResourceLocation.withDefaultNamespace("rabbit"))
                .addHandle(new HungerHandle())
                .addHandle(new ThirstHandle())
                .addHandle(new ConditionHandle())
                .addHandle(new EnergyHandle())
                .addHandle(new AgeHandle())
                .addHandle(new SocialHandle())
                .addHandle(new MovementHandle())
                .addHandle(new HealthHandle())
                .addHandle(new BreedingHandle())
                .addHandle(new PredationHandle())
                .addHandle(new DietHandle())
                .addHandle(new TemporalHandle())
                .addHandle(new NestBuildingHandle())
                .build();

        AnimalBehaviorRegistry.register("minecraft:rabbit", config);
    }
}
