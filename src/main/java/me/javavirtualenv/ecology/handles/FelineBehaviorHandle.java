package me.javavirtualenv.ecology.handles;

import me.javavirtualenv.behavior.ai.SteeringBehaviorGoal;
import me.javavirtualenv.behavior.feline.*;
import me.javavirtualenv.behavior.predation.HuntingState;
import me.javavirtualenv.ecology.EcologyComponent;
import me.javavirtualenv.ecology.EcologyHandle;
import me.javavirtualenv.ecology.EcologyProfile;
import me.javavirtualenv.mixin.MobAccessor;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.entity.animal.Ocelot;

/**
 * Handle for managing feline-specific behaviors.
 * <p>
 * This handle registers special behaviors for cats and ocelots:
 * - Hunting behaviors (stalking, pouncing, creeping)
 * - Stealth behaviors (quiet movement, squeezing through gaps)
 * - Social behaviors (purring, hissing, rubbing affection)
 * - Gift giving behavior
 * - Creeper detection and phantom repelling
 */
public class FelineBehaviorHandle implements EcologyHandle {

    private static final String AFFECTION_KEY = "affection";

    @Override
    public String id() {
        return "feline_behavior";
    }

    @Override
    public boolean supports(EcologyProfile profile) {
        String entityType = profile.getString("entity_type", "");
        return entityType.equals("cat") || entityType.equals("ocelot");
    }

    @Override
    public void registerGoals(Mob mob, EcologyComponent component, EcologyProfile profile) {
        // Register feline-specific goals
        int priority = profile.getInt("feline.priority", 1);

        // Stalk behavior goal
        StalkBehavior stalkBehavior = new StalkBehavior(
            profile.getDouble("feline.stalk_speed", 0.3),
            profile.getDouble("feline.stalk_detection_range", 16.0),
            profile.getDouble("feline.pounce_distance", 3.0),
            profile.getDouble("feline.give_up_angle", 45.0)
        );

        // Pounce behavior goal
        PounceBehavior pounceBehavior = new PounceBehavior(
            profile.getDouble("feline.pounce_speed", 1.5),
            profile.getDouble("feline.pounce_range", 4.0),
            profile.getDouble("feline.pounce_cooldown", 60.0),
            profile.getDouble("feline.pounce_force", 0.8)
        );

        // Gift giving goal (tamed cats only)
        if (mob instanceof Cat) {
            GiftGivingBehavior giftBehavior = new GiftGivingBehavior(
                profile.getInt("feline.gift_cooldown", 24000),
                profile.getDouble("feline.gift_trust_threshold", 0.7),
                profile.getInt("feline.gift_search_range", 32)
            );
        }

        // Register goals through goal selector
        // Note: Full goal registration would be handled in a separate FelineBehaviorGoal class
    }

    @Override
    public void tick(Mob mob, EcologyComponent component, EcologyProfile profile) {
        // Update feline behaviors each tick
        // This is where behavior state would be updated
    }

    @Override
    public void readNbt(Mob mob, EcologyComponent component, EcologyProfile profile, CompoundTag tag) {
        if (tag.contains(AFFECTION_KEY)) {
            CompoundTag affectionTag = tag.getCompound(AFFECTION_KEY);
            CatAffectionComponent affection = new CatAffectionComponent();
            affection.fromNbt(affectionTag);

            // Store affection component in entity data
            // This would need a proper attachment mechanism
        }
    }

    @Override
    public void writeNbt(Mob mob, EcologyComponent component, EcologyProfile profile, CompoundTag tag) {
        CompoundTag handleTag = component.getHandleTag(id());

        // Save affection data if it exists
        // This would retrieve the CatAffectionComponent and serialize it

        tag.put(id(), handleTag.copy());
    }

    /**
     * Get or create the affection component for a cat.
     */
    public static CatAffectionComponent getAffectionComponent(Mob mob) {
        // This would retrieve the stored affection component
        // For now, return a new one
        return new CatAffectionComponent();
    }

    /**
     * Save the affection component for a cat.
     */
    public static void setAffectionComponent(Mob mob, CatAffectionComponent affection) {
        // This would save the affection component to persistent storage
    }
}
