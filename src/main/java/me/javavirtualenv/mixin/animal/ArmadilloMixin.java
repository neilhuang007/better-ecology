package me.javavirtualenv.mixin.animal;

import me.javavirtualenv.behavior.armadillo.ArmadilloComponent;
import me.javavirtualenv.behavior.armadillo.ArmadilloPredatorAvoidance;
import me.javavirtualenv.ecology.AnimalBehaviorRegistry;
import me.javavirtualenv.ecology.AnimalConfig;
import me.javavirtualenv.ecology.CodeBasedHandle;
import me.javavirtualenv.ecology.EcologyComponent;
import me.javavirtualenv.ecology.EcologyProfile;
import me.javavirtualenv.ecology.ai.LowHealthFleeGoal;
import me.javavirtualenv.ecology.handles.*;
import me.javavirtualenv.ecology.state.EntityState;
import me.javavirtualenv.mixin.MobAccessor;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.Difficulty;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.armadillo.Armadillo;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Mixin for Armadillo entity behavior registration.
 * <p>
 * Armadillos are unique animals with:
 * - Rolling defense: curl into armored ball when threatened
 * - Insect foraging: dig and eat insects from blocks
 * - Burrowing: dig extensive burrow systems for shelter
 * - Predator avoidance: flee first, roll if cornered
 * - Crepuscular activity: active at dawn/dusk/night
 * <p>
 * Behaviors are scientifically-based and configurable via JSON.
 */
@Mixin(Armadillo.class)
public abstract class ArmadilloMixin extends Mob {

    private static final String ARMADILLO_ID = "minecraft:armadillo";
    private static boolean behaviorsRegistered = false;

    public ArmadilloMixin(EntityType<? extends Mob> entityType, Level level) {
        super(entityType, level);
    }

    @Inject(method = "<init>", at = @At("RETURN"))
    private void onInit(EntityType<? extends net.minecraft.world.entity.animal.armadillo.Armadillo> entityType, Level level, CallbackInfo ci) {
        if (!behaviorsRegistered) {
            registerArmadilloBehaviors();
            behaviorsRegistered = true;
        }
    }

    /**
     * Registers armadillo behaviors with the system.
     */
    private void registerArmadilloBehaviors() {
        AnimalConfig config = AnimalConfig.builder(
            net.minecraft.resources.ResourceLocation.parse(ARMADILLO_ID))
            // Basic survival handles
            .addHandle(new ArmadilloHungerHandle())
            .addHandle(new ArmadilloThirstHandle())
            .addHandle(new ArmadilloConditionHandle())
            .addHandle(new ArmadilloEnergyHandle())
            .addHandle(new ArmadilloAgeHandle())
            .addHandle(new ArmadilloHealthHandle())
            .addHandle(new ArmadilloMovementHandle())
            .addHandle(new ArmadilloTemporalHandle())
            .addHandle(new ArmadilloDietHandle())
            // Armadillo-specific behavior handle
            .addHandle(new ArmadilloBehaviorHandle())
            .build();

        AnimalBehaviorRegistry.register(ARMADILLO_ID, config);
    }

    /**
     * Hunger handle with armadillo-specific values.
     */
    private static final class ArmadilloHungerHandle extends CodeBasedHandle {
        private static final String NBT_HUNGER = "hunger";
        private static final String NBT_LAST_DAMAGE_TICK = "lastDamageTick";

        private static final int MAX_HUNGER = 100;
        private static final int STARTING_HUNGER = 80;
        private static final double DECAY_RATE = 0.015; // Slower decay - armadillos are efficient
        private static final int DAMAGE_THRESHOLD = 5;
        private static final float DAMAGE_AMOUNT = 1.0f;
        private static final int DAMAGE_INTERVAL = 200;

        @Override
        public String id() {
            return "hunger";
        }

        @Override
        public int tickInterval() {
            return 20;
        }

        @Override
        public void tick(Mob mob, EcologyComponent component, EcologyProfile profile) {
            CompoundTag handleTag = component.getHandleTag(id());
            int currentHunger = getCurrentHunger(handleTag);

            // Apply slower hunger decay while rolled (conserving energy)
            long elapsedTicks = component.elapsedTicks();
            ArmadilloComponent armadilloComponent = new ArmadilloComponent(component.getHandleTag("armadillo"));
            double actualDecay = armadilloComponent.isRolled() ? DECAY_RATE * 0.3 : DECAY_RATE;

            long effectiveTicks = Math.max(1, elapsedTicks);
            long scaledDecay = (long) (actualDecay * effectiveTicks);
            int newHunger = (int) (currentHunger - scaledDecay);

            if (elapsedTicks > 1) {
                int safeMinimum = DAMAGE_THRESHOLD + 1;
                newHunger = Math.max(safeMinimum, newHunger);
            } else {
                newHunger = Math.max(0, newHunger);
            }
            setHunger(handleTag, newHunger);

            if (elapsedTicks <= 1 && shouldApplyStarvation(mob, newHunger)) {
                int currentTick = mob.tickCount;
                int lastDamageTick = getLastDamageTick(handleTag);
                int ticksSinceDamage = currentTick - lastDamageTick;

                if (ticksSinceDamage >= DAMAGE_INTERVAL) {
                    mob.hurt(mob.level().damageSources().starve(), DAMAGE_AMOUNT);
                    setLastDamageTick(handleTag, currentTick);
                }
            }
        }

        private int getCurrentHunger(CompoundTag handleTag) {
            return handleTag.contains(NBT_HUNGER) ? handleTag.getInt(NBT_HUNGER) : STARTING_HUNGER;
        }

        private void setHunger(CompoundTag handleTag, int value) {
            handleTag.putInt(NBT_HUNGER, value);
        }

        private int getLastDamageTick(CompoundTag handleTag) {
            return handleTag.getInt(NBT_LAST_DAMAGE_TICK);
        }

        private void setLastDamageTick(CompoundTag handleTag, int tick) {
            handleTag.putInt(NBT_LAST_DAMAGE_TICK, tick);
        }

        private boolean shouldApplyStarvation(Mob mob, int hunger) {
            return mob.level().getDifficulty() != Difficulty.PEACEFUL && hunger <= DAMAGE_THRESHOLD;
        }
    }

    /**
     * Thirst handle with armadillo-specific values.
     */
    private static final class ArmadilloThirstHandle extends CodeBasedHandle {
        private static final String NBT_THIRST = "thirst";
        private static final String NBT_LAST_DAMAGE_TICK = "lastThirstDamageTick";

        private static final long MAX_CATCH_UP_TICKS = 24000L; // 1 Minecraft day
        private static final int MAX_THIRST = 100;
        private static final int STARTING_THIRST = 85;
        private static final double DECAY_RATE = 0.015;
        private static final int THIRST_THRESHOLD = 20;
        private static final int DEHYDRATION_THRESHOLD = 5;
        private static final float DEHYDRATION_DAMAGE = 1.0f;
        private static final int DAMAGE_INTERVAL = 200;

        @Override
        public String id() {
            return "thirst";
        }

        @Override
        public int tickInterval() {
            return 20;
        }

        @Override
        public void tick(Mob mob, EcologyComponent component, EcologyProfile profile) {
            CompoundTag tag = component.getHandleTag(id());
            int currentThirst = getCurrentThirst(tag);

            long elapsed = component.elapsedTicks();
            // Cap elapsed ticks to prevent instant dehydration
            long effectiveTicks = Math.min(elapsed, MAX_CATCH_UP_TICKS);
            boolean isCatchUp = elapsed > 1;

            double decay = DECAY_RATE * effectiveTicks;
            int newThirst;

            if (isCatchUp) {
                // During catch-up, keep thirst above dehydration threshold
                int safeMinimum = DEHYDRATION_THRESHOLD + 1;
                newThirst = Math.max(safeMinimum, (int) (currentThirst - decay));
            } else {
                newThirst = (int) Math.max(0, currentThirst - decay);
            }
            setThirst(tag, newThirst);

            boolean isThirsty = newThirst < THIRST_THRESHOLD;
            component.state().setIsThirsty(isThirsty);

            // Only apply dehydration damage during active updates
            if (!isCatchUp && newThirst <= DEHYDRATION_THRESHOLD) {
                int currentTick = mob.tickCount;
                int lastDamageTick = tag.getInt(NBT_LAST_DAMAGE_TICK);
                if (currentTick - lastDamageTick >= DAMAGE_INTERVAL) {
                    if (mob.level().getDifficulty() != Difficulty.PEACEFUL) {
                        mob.hurt(mob.level().damageSources().starve(), DEHYDRATION_DAMAGE);
                        tag.putInt(NBT_LAST_DAMAGE_TICK, currentTick);
                    }
                }
            }
        }

        private int getCurrentThirst(CompoundTag tag) {
            return tag.contains(NBT_THIRST) ? tag.getInt(NBT_THIRST) : STARTING_THIRST;
        }

        private void setThirst(CompoundTag tag, int value) {
            tag.putInt(NBT_THIRST, value);
        }
    }

    /**
     * Condition handle with armadillo-specific values.
     */
    private static final class ArmadilloConditionHandle extends CodeBasedHandle {
        private static final String NBT_CONDITION = "condition";

        private static final int MAX_VALUE = 100;
        private static final int STARTING_VALUE = 85;
        private static final double GAIN_WHEN_SATIATED = 0.025;
        private static final double LOSS_WHEN_HUNGRY = 0.02;
        private static final double LOSS_WHEN_STARVING = 0.1;

        @Override
        public String id() {
            return "condition";
        }

        @Override
        public int tickInterval() {
            return 10;
        }

        @Override
        public void tick(Mob mob, EcologyComponent component, EcologyProfile profile) {
            CompoundTag tag = component.getHandleTag(id());
            int currentCondition = getCurrentCondition(tag);

            EntityState state = component.state();
            double change;

            if (state.isStarving()) {
                change = -LOSS_WHEN_STARVING;
            } else if (state.isHungry()) {
                change = -LOSS_WHEN_HUNGRY;
            } else if (isSatiated(component)) {
                change = GAIN_WHEN_SATIATED;
            } else {
                change = 0.0;
            }

            long elapsed = component.elapsedTicks();
            change *= elapsed;

            int newCondition = (int) Math.round(Math.min(MAX_VALUE, Math.max(0, currentCondition + change)));
            setCondition(tag, newCondition);
        }

        private int getCurrentCondition(CompoundTag tag) {
            return tag.contains(NBT_CONDITION) ? tag.getInt(NBT_CONDITION) : STARTING_VALUE;
        }

        private void setCondition(CompoundTag tag, int value) {
            tag.putInt(NBT_CONDITION, value);
        }

        private boolean isSatiated(EcologyComponent component) {
            CompoundTag hungerTag = component.getHandleTag("hunger");
            if (!hungerTag.contains("hunger")) {
                return false;
            }
            return hungerTag.getInt("hunger") > 75;
        }
    }

    /**
     * Energy handle with armadillo-specific values.
     */
    private static final class ArmadilloEnergyHandle extends CodeBasedHandle {
        private static final String NBT_ENERGY = "energy";
        private static final String NBT_IS_EXHAUSTED = "isExhausted";

        private static final int MAX_VALUE = 100;
        private static final double RECOVERY_RATE = 0.7;
        private static final int EXHAUSTION_THRESHOLD = 15;
        private static final double FORAGING_COST = 0.12;
        private static final double FLEEING_COST = 0.25;
        private static final double BURROWING_COST = 0.2;

        @Override
        public String id() {
            return "energy";
        }

        @Override
        public int tickInterval() {
            return 2;
        }

        @Override
        public void tick(Mob mob, EcologyComponent component, EcologyProfile profile) {
            EntityState state = component.state();
            CompoundTag tag = component.getHandleTag(id());
            int currentEnergy = getCurrentEnergy(tag);

            double cost = determineEnergyCost(state, component);
            double recovery = RECOVERY_RATE;

            long elapsed = component.elapsedTicks();

            int newEnergy;
            if (cost > 0) {
                newEnergy = Math.max(0, (int) Math.floor(currentEnergy - cost * elapsed));
            } else {
                newEnergy = Math.min(MAX_VALUE, (int) Math.ceil(currentEnergy + recovery * elapsed));
            }
            setEnergy(tag, newEnergy);

            boolean isExhausted = newEnergy < EXHAUSTION_THRESHOLD;
            tag.putBoolean(NBT_IS_EXHAUSTED, isExhausted);

            // Exhausted animals can't sprint/hunt effectively
            if (isExhausted && (state.isHunting() || state.isFleeing())) {
                state.setIsHunting(false);
                state.setIsFleeing(false);
            }
        }

        private int getCurrentEnergy(CompoundTag tag) {
            return tag.contains(NBT_ENERGY) ? tag.getInt(NBT_ENERGY) : MAX_VALUE;
        }

        private void setEnergy(CompoundTag tag, int value) {
            tag.putInt(NBT_ENERGY, value);
        }

        private double determineEnergyCost(EntityState state, EcologyComponent component) {
            if (state.isFleeing()) {
                return FLEEING_COST;
            }

            ArmadilloComponent armadilloComponent = new ArmadilloComponent(component.getHandleTag("armadillo"));

            // Check if foraging or burrowing
            if (armadilloComponent.getScentStrength() > 0.1) {
                return FORAGING_COST;
            }

            if (armadilloComponent.getBurrowPos() != null) {
                return BURROWING_COST;
            }

            // Very low cost when rolled (resting state)
            if (armadilloComponent.isRolled()) {
                return 0.005;
            }

            return 0.01;
        }
    }

    /**
     * Age handle with armadillo-specific values.
     */
    private static final class ArmadilloAgeHandle extends CodeBasedHandle {
        private static final String NBT_AGE_TICKS = "ageTicks";

        private static final long MAX_CATCH_UP_TICKS = 24000L; // 1 Minecraft day
        private static final int BABY_DURATION = 24000;

        @Override
        public String id() {
            return "age";
        }

        @Override
        public int tickInterval() {
            return 20;
        }

        @Override
        public void tick(Mob mob, EcologyComponent component, EcologyProfile profile) {
            CompoundTag tag = component.getHandleTag(id());
            int ageTicks = getAgeTicks(tag);
            long elapsed = component.elapsedTicks();
            long effectiveTicks = Math.min(elapsed, MAX_CATCH_UP_TICKS);
            ageTicks += effectiveTicks;
            setAgeTicks(tag, ageTicks);
        }

        private int getAgeTicks(CompoundTag tag) {
            return tag.contains(NBT_AGE_TICKS) ? tag.getInt(NBT_AGE_TICKS) : 0;
        }

        private void setAgeTicks(CompoundTag tag, int value) {
            tag.putInt(NBT_AGE_TICKS, value);
        }
    }

    /**
     * Health handle with armadillo-specific values.
     */
    private static final class ArmadilloHealthHandle extends CodeBasedHandle {

        private static final double BASE_HEALTH = 12.0; // Armadillos are tough

        @Override
        public String id() {
            return "health";
        }

        @Override
        public void registerGoals(Mob mob, EcologyComponent component, EcologyProfile profile) {
            AttributeInstance healthAttribute = mob.getAttribute(Attributes.MAX_HEALTH);
            if (healthAttribute != null) {
                healthAttribute.setBaseValue(BASE_HEALTH);
            }

            MobAccessor accessor = (MobAccessor) mob;
            Armadillo armadillo = (Armadillo) mob;

            // Priority 1: Low health flee (armadillos curl up but also flee when damaged)
            accessor.betterEcology$getGoalSelector().addGoal(1, new LowHealthFleeGoal(armadillo, 0.50, 1.2));
        }
    }

    /**
     * Movement handle with armadillo-specific values.
     * Armadillos are slow but steady movers.
     */
    private static final class ArmadilloMovementHandle extends CodeBasedHandle {

        private static final double WALK_SPEED = 0.15;
        private static final double ROLL_SPEED = 0.3; // Faster when rolled

        @Override
        public String id() {
            return "movement";
        }

        @Override
        public void registerGoals(Mob mob, EcologyComponent component, EcologyProfile profile) {
            AttributeInstance movementAttribute = mob.getAttribute(Attributes.MOVEMENT_SPEED);
            if (movementAttribute != null) {
                movementAttribute.setBaseValue(WALK_SPEED);
            }
        }
    }

    /**
     * Temporal handle - armadillos are crepuscular/nocturnal.
     */
    private static final class ArmadilloTemporalHandle extends CodeBasedHandle {

        @Override
        public String id() {
            return "temporal";
        }

        @Override
        public void tick(Mob mob, EcologyComponent component, EcologyProfile profile) {
            // Armadillos are most active at dawn, dusk, and night
            long dayTime = mob.level().getDayTime() % 24000;
            boolean isActiveTime = dayTime > 13000 || dayTime < 6000; // Night and early morning

            // Set foraging state based on time
            if (isActiveTime) {
                // Foraging is handled by the temporal handle
            }
        }
    }

    /**
     * Diet handle - armadillos are insectivores.
     */
    private static final class ArmadilloDietHandle extends CodeBasedHandle {

        @Override
        public String id() {
            return "diet";
        }

        @Override
        public boolean overrideIsFood(Mob mob, EcologyComponent component, EcologyProfile profile,
                                     net.minecraft.world.item.ItemStack stack, boolean original) {
            // Armadillos primarily eat insects (foraged from blocks)
            // They may also eat spider eyes and other small items
            return original;
        }
    }
}
