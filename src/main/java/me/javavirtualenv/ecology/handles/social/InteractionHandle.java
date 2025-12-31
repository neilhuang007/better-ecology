package me.javavirtualenv.ecology.handles.social;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import me.javavirtualenv.ecology.EcologyComponent;
import me.javavirtualenv.ecology.EcologyHandle;
import me.javavirtualenv.ecology.EcologyProfile;
import me.javavirtualenv.ecology.spatial.SpatialIndex;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;

/**
 * Comprehensive Animal-Animal Interactions System
 *
 * Manages cross-species relationships including:
 * - Predator-prey dynamics (beyond basic fleeing)
 * - Symbiotic relationships
 * - Competitive relationships
 * - Mixed herding
 * - Dynamic relationship building (trust, familiarity, grudges)
 * - Social behaviors (warning calls, altruism, teaching, grief, play)
 */
public final class InteractionHandle implements EcologyHandle {
    private static final String CACHE_KEY = "better-ecology:interaction-cache";
    private static final int INTERACTION_TICK_INTERVAL = 40;
    private static final int RELATIONSHIP_DECAY_INTERVAL = 600;

    private static final String NBT_RELATIONSHIPS = "relationships";
    private static final String NBT_PACK_MEMBERS = "pack_members";
    private static final String NBT_SYMBIOTIC_PARTNERS = "symbiotic_partners";
    private static final String NBT_LAST_INTERACTION_TICK = "last_interaction_tick";
    private static final String NBT_INTERACTION_HISTORY = "interaction_history";
    private static final String NBT_WARNING_COOLDOWN = "warning_cooldown";
    private static final String NBT_MOURNING_TARGET = "mourning_target";
    private static final String NBT_MOURNING_END_TICK = "mourning_end_tick";

    @Override
    public String id() {
        return "interaction";
    }

    @Override
    public boolean supports(EcologyProfile profile) {
        InteractionCache cache = profile.cached(CACHE_KEY, () -> buildCache(profile));
        return cache != null && (cache.hasPredators || cache.hasPrey ||
                cache.hasSymbioticPartners || cache.hasCompetitors ||
                cache.canJoinMixedHerds);
    }

    @Override
    public int tickInterval() {
        return INTERACTION_TICK_INTERVAL;
    }

    @Override
    public void tick(Mob mob, EcologyComponent component, EcologyProfile profile) {
        InteractionCache cache = profile.cached(CACHE_KEY, () -> buildCache(profile));
        if (cache == null) {
            return;
        }

        CompoundTag tag = component.getHandleTag(id());
        int currentTick = mob.tickCount;
        long elapsedTicks = component.elapsedTicks();

        // Decay relationships periodically
        decayRelationships(tag, currentTick, elapsedTicks, cache);

        // Process dynamic relationship building
        processRelationshipBuilding(mob, tag, cache);

        // Check for warning call opportunities
        processWarningCalls(mob, tag, cache);

        // Process mourning if active
        processMourning(mob, tag, cache);
    }

    @Override
    public void registerGoals(Mob mob, EcologyComponent component, EcologyProfile profile) {
        InteractionCache cache = profile.cached(CACHE_KEY, () -> buildCache(profile));
        if (cache == null || !(mob instanceof PathfinderMob pathfinderMob)) {
            return;
        }

        // Register avoidance goals for predators
        registerPredatorAvoidance(mob, cache);

        // Register hunting goals for prey
        registerPreyHunting(mob, cache);

        // Register symbiotic behavior goals
        registerSymbioticGoals(mob, cache);

        // Register competition avoidance goals
        registerCompetitionAvoidance(mob, cache);

        // Register mixed herding goals
        registerMixedHerdingGoals(mob, cache);

        // Register social behavior goals
        registerSocialBehaviorGoals(mob, cache);
    }

    @Override
    public void readNbt(Mob mob, EcologyComponent component, EcologyProfile profile, CompoundTag tag) {
        // NBT data loaded automatically through component.getHandleTag()
    }

    @Override
    public void writeNbt(Mob mob, EcologyComponent component, EcologyProfile profile, CompoundTag tag) {
        CompoundTag handleTag = component.getHandleTag(id());
        tag.put(id(), handleTag.copy());
    }

    private InteractionCache buildCache(EcologyProfile profile) {
        boolean hasPredators = !profile.getStringList("interaction.predators").isEmpty();
        boolean hasPrey = !profile.getStringList("interaction.prey").isEmpty();
        boolean hasSymbioticPartners = !profile.getStringList("interaction.symbiotic_partners").isEmpty();
        boolean hasCompetitors = !profile.getStringList("interaction.competitors").isEmpty();
        boolean canJoinMixedHerds = !profile.getStringList("interaction.herd_compatible").isEmpty();

        double relationshipDecayRate = profile.getDouble("interaction.relationship_decay_rate", 0.001);
        double trustBuildRate = profile.getDouble("interaction.trust_build_rate", 0.01);
        double grudgeBuildRate = profile.getDouble("interaction.grudge_build_rate", 0.05);

        int warningCooldown = profile.getInt("interaction.warning_cooldown", 200);
        int mourningDuration = profile.getInt("interaction.mourning_duration", 1200);

        boolean givesWarningCalls = profile.getBool("interaction.gives_warning_calls", false);
        boolean canMourn = profile.getBool("interaction.can_mourn", false);
        boolean canGrieve = profile.getBool("interaction.can_grieve", false);
        boolean canPlay = profile.getBool("interaction.can_play", false);
        boolean canBeAltruistic = profile.getBool("interaction.can_be_altruistic", false);

        return new InteractionCache(
                hasPredators, hasPrey, hasSymbioticPartners, hasCompetitors, canJoinMixedHerds,
                relationshipDecayRate, trustBuildRate, grudgeBuildRate,
                warningCooldown, mourningDuration,
                givesWarningCalls, canMourn, canGrieve, canPlay, canBeAltruistic
        );
    }

    private void decayRelationships(CompoundTag tag, int currentTick, long elapsedTicks, InteractionCache cache) {
        int lastDecayTick = tag.getInt("last_relationship_decay");
        if (currentTick - lastDecayTick < RELATIONSHIP_DECAY_INTERVAL) {
            return;
        }

        tag.putInt("last_relationship_decay", currentTick);

        ListTag relationshipsList = tag.getList(NBT_RELATIONSHIPS, 10);
        if (relationshipsList.isEmpty()) {
            return;
        }

        ListTag newList = new ListTag();
        for (int i = 0; i < relationshipsList.size(); i++) {
            CompoundTag relationshipTag = relationshipsList.getCompound(i);
            double value = relationshipTag.getDouble("value");

            // Apply decay
            double decayAmount = cache.relationshipDecayRate * elapsedTicks;
            double newValue = value - decayAmount;

            // Remove relationships that have decayed to near-zero
            if (Math.abs(newValue) > 0.01) {
                relationshipTag.putDouble("value", newValue);
                newList.add(relationshipTag);
            }
        }

        tag.put(NBT_RELATIONSHIPS, newList);
    }

    private void processRelationshipBuilding(Mob mob, CompoundTag tag, InteractionCache cache) {
        List<Mob> nearbyMobs = SpatialIndex.getNearbyMobs(mob, 32);

        for (Mob other : nearbyMobs) {
            if (other == mob || !other.isAlive()) {
                continue;
            }

            ResourceLocation otherType = getEntityType(other);
            if (otherType == null) {
                continue;
            }

            // Determine if this is a known relationship type
            double change = calculateRelationshipChange(mob, other, cache);

            if (Math.abs(change) > 0.001) {
                modifyRelationship(tag, other.getUUID(), otherType, change);
                recordInteraction(tag, other.getUUID(), otherType, change);
            }
        }
    }

    private double calculateRelationshipChange(Mob mob, Mob other, InteractionCache cache) {
        ResourceLocation otherType = getEntityType(other);
        if (otherType == null) {
            return 0;
        }

        double distance = mob.distanceTo(other);

        // Positive relationships: trust building through proximity
        if (distance < 8) {
            return cache.trustBuildRate;
        }

        // Negative relationships: territorial behavior
        if (distance < 16 && isCompetitor(mob, other)) {
            return -cache.grudgeBuildRate;
        }

        return 0;
    }

    private boolean isCompetitor(Mob mob, Mob other) {
        // Check if entities share similar ecological niches
        String mobType = getEntityTypeString(mob);
        String otherType = getEntityTypeString(other);

        // Wolves and foxes compete
        if ((mobType.equals("minecraft:wolf") && otherType.equals("minecraft:fox")) ||
            (mobType.equals("minecraft:fox") && otherType.equals("minecraft:wolf"))) {
            return true;
        }

        // Cats and dogs avoid each other
        if ((mobType.equals("minecraft:cat") && otherType.equals("minecraft:wolf")) ||
            (mobType.equals("minecraft:wolf") && otherType.equals("minecraft:cat"))) {
            return true;
        }

        return false;
    }

    private void modifyRelationship(CompoundTag tag, UUID entityUuid, ResourceLocation entityType, double change) {
        ListTag relationshipsList = tag.getList(NBT_RELATIONSHIPS, 10);
        CompoundTag relationshipTag = findRelationship(relationshipsList, entityUuid);

        if (relationshipTag == null) {
            relationshipTag = new CompoundTag();
            relationshipTag.putUUID("entity_uuid", entityUuid);
            relationshipTag.putString("entity_type", entityType.toString());
            relationshipTag.putDouble("value", change);
            relationshipTag.putInt("first_seen", entityUuid.hashCode()); // Use hash as timestamp proxy
            relationshipsList.add(relationshipTag);
        } else {
            double currentValue = relationshipTag.getDouble("value");
            double newValue = Math.max(-1.0, Math.min(1.0, currentValue + change));
            relationshipTag.putDouble("value", newValue);
        }

        tag.put(NBT_RELATIONSHIPS, relationshipsList);
    }

    private CompoundTag findRelationship(ListTag list, UUID entityUuid) {
        for (int i = 0; i < list.size(); i++) {
            CompoundTag tag = list.getCompound(i);
            if (tag.getUUID("entity_uuid").equals(entityUuid)) {
                return tag;
            }
        }
        return null;
    }

    private void recordInteraction(CompoundTag tag, UUID entityUuid, ResourceLocation entityType, double value) {
        ListTag historyList = tag.getList(NBT_INTERACTION_HISTORY, 10);

        // Limit history size
        if (historyList.size() > 50) {
            historyList.remove(0);
        }

        CompoundTag entry = new CompoundTag();
        entry.putUUID("entity_uuid", entityUuid);
        entry.putString("entity_type", entityType.toString());
        entry.putDouble("value", value);
        entry.putInt("tick", entityUuid.hashCode()); // Use hash as timestamp proxy

        historyList.add(entry);
        tag.put(NBT_INTERACTION_HISTORY, historyList);
    }

    private void processWarningCalls(Mob mob, CompoundTag tag, InteractionCache cache) {
        if (!cache.givesWarningCalls) {
            return;
        }

        int currentTick = mob.tickCount;
        int lastWarning = tag.getInt(NBT_WARNING_COOLDOWN);

        if (currentTick - lastWarning < cache.warningCooldown) {
            return;
        }

        // Check for nearby threats
        List<Mob> nearbyMobs = SpatialIndex.getNearbyMobs(mob, 24);
        boolean hasThreat = nearbyMobs.stream()
                .anyMatch(other -> isPredatorOf(mob, other));

        if (hasThreat) {
            emitWarningCall(mob, tag);
            tag.putInt(NBT_WARNING_COOLDOWN, currentTick);
        }
    }

    private boolean isPredatorOf(Mob prey, Mob potentialPredator) {
        String preyType = getEntityTypeString(prey);
        String predatorType = getEntityTypeString(potentialPredator);

        // Wolf prey
        if (preyType.equals("minecraft:sheep") || preyType.equals("minecraft:rabbit")) {
            return predatorType.equals("minecraft:wolf") || predatorType.equals("minecraft:fox");
        }

        // Fox prey
        if (preyType.equals("minecraft:chicken")) {
            return predatorType.equals("minecraft:fox");
        }

        // Cat/Ocelot prey
        if (preyType.equals("minecraft:parrot")) {
            return predatorType.equals("minecraft:cat") || predatorType.equals("minecraft:ocelot");
        }

        return false;
    }

    private void emitWarningCall(Mob mob, CompoundTag tag) {
        // Alert nearby herd members
        List<Mob> nearbyMobs = SpatialIndex.getNearbyMobs(mob, 32);

        for (Mob other : nearbyMobs) {
            if (isHerdMate(mob, other)) {
                // Trigger flee behavior in herd mates
                // This would interact with the fleeing behavior system
                CompoundTag otherTag = other.getPersistentData();
                otherTag.putBoolean("better_ecology:warning_received", true);
                otherTag.putInt("better_ecology:warning_tick", mob.tickCount);
            }
        }
    }

    private boolean isHerdMate(Mob mob, Mob other) {
        String mobType = getEntityTypeString(mob);
        String otherType = getEntityTypeString(other);

        // Same species
        if (mobType.equals(otherType)) {
            return true;
        }

        // Mixed herding: horses + donkeys
        if ((mobType.equals("minecraft:horse") && otherType.equals("minecraft:donkey")) ||
            (mobType.equals("minecraft:donkey") && otherType.equals("minecraft:horse"))) {
            return true;
        }

        // Mixed herding: cows + sheep
        if ((mobType.equals("minecraft:cow") || mobType.equals("minecraft:mooshroom")) &&
            (otherType.equals("minecraft:sheep"))) {
            return true;
        }

        return false;
    }

    private void processMourning(Mob mob, CompoundTag tag, InteractionCache cache) {
        if (!cache.canMourn) {
            return;
        }

        if (!tag.contains(NBT_MOURNING_END_TICK)) {
            return;
        }

        int mourningEndTick = tag.getInt(NBT_MOURNING_END_TICK);
        if (mob.tickCount >= mourningEndTick) {
            // End mourning period
            tag.remove(NBT_MOURNING_TARGET);
            tag.remove(NBT_MOURNING_END_TICK);
            return;
        }

        // Apply mourning effects
        // Reduced movement, reduced eating, social behavior changes
    }

    private void registerPredatorAvoidance(Mob mob, InteractionCache cache) {
        if (!(mob instanceof PathfinderMob pathfinderMob)) {
            return;
        }

        // Wolves flee from
        // Foxes flee from

        // This would integrate with PredationHandle
        // Avoiding duplication by checking if already registered
    }

    private void registerPreyHunting(Mob mob, InteractionCache cache) {
        if (!(mob instanceof PathfinderMob pathfinderMob)) {
            return;
        }

        // Wolves hunt: rabbits, sheep, foxes
        // Foxes hunt: chickens, rabbits
        // Cats/Ocelots hunt: chickens, parrots
        // Frogs hunt: small insects
        // Axolotls hunt: fish, tadpoles
    }

    private void registerSymbioticGoals(Mob mob, InteractionCache cache) {
        // Bees + Flowers: pollination
        // Allay + Villagers: item collection
        // Camels + Riders: transport
    }

    private void registerCompetitionAvoidance(Mob mob, InteractionCache cache) {
        // Wolves vs Foxes: territory competition
        // Cats vs Dogs: mutual avoidance
    }

    private void registerMixedHerdingGoals(Mob mob, InteractionCache cache) {
        // Horses + Donkeys
        // Cows + Sheep
        // Chicken + Rooster
    }

    private void registerSocialBehaviorGoals(Mob mob, InteractionCache cache) {
        if (!(mob instanceof PathfinderMob)) {
            return;
        }

        // Warning calls
        if (cache.givesWarningCalls) {
            // mob.goalSelector.addGoal(3, new WarningCallGoal(mob, cache));
        }

        // Altruism
        if (cache.canBeAltruistic) {
            // mob.goalSelector.addGoal(4, new AltruismGoal(mob, cache));
        }

        // Play
        if (cache.canPlay) {
            // mob.goalSelector.addGoal(10, new PlayGoal(mob, cache));
        }

        // Teaching
        // mob.goalSelector.addGoal(8, new TeachingGoal(mob, cache));
    }

    private ResourceLocation getEntityType(Mob mob) {
        try {
            return net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType());
        } catch (Exception e) {
            return null;
        }
    }

    private String getEntityTypeString(Mob mob) {
        ResourceLocation loc = getEntityType(mob);
        return loc != null ? loc.toString() : "";
    }

    // Public API for querying relationships

    /**
     * Get the relationship value with another entity.
     * Range: -1.0 (enemy) to 1.0 (friend), 0.0 (neutral)
     */
    public static double getRelationship(Mob mob, UUID otherUuid) {
        // This would need access to the component
        return 0.0;
    }

    /**
     * Set a relationship value.
     */
    public static void setRelationship(Mob mob, UUID otherUuid, double value) {
        // This would need access to the component
    }

    /**
     * Check if another entity is a pack member.
     */
    public static boolean isPackMember(Mob mob, UUID otherUuid) {
        // This would need access to the component
        return false;
    }

    /**
     * Check if two entities have a symbiotic relationship.
     */
    public static boolean isSymbioticPartner(Mob mob, UUID otherUuid) {
        // This would need access to the component
        return false;
    }

    /**
     * Trigger mourning behavior for a deceased companion.
     */
    public static void triggerMourning(Mob mob, UUID deceasedUuid) {
        // This would need access to the component
    }

    /**
     * Check if this entity is currently mourning.
     */
    public static boolean isMourning(Mob mob) {
        // This would need access to the component
        return false;
    }

    private record InteractionCache(
            boolean hasPredators,
            boolean hasPrey,
            boolean hasSymbioticPartners,
            boolean hasCompetitors,
            boolean canJoinMixedHerds,
            double relationshipDecayRate,
            double trustBuildRate,
            double grudgeBuildRate,
            int warningCooldown,
            int mourningDuration,
            boolean givesWarningCalls,
            boolean canMourn,
            boolean canGrieve,
            boolean canPlay,
            boolean canBeAltruistic
    ) {}
}
