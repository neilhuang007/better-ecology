package me.javavirtualenv.behavior.wolf;

import me.javavirtualenv.behavior.shared.AnimalItemStorage;
import me.javavirtualenv.ecology.handles.WolfBehaviorHandle;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.item.ItemStack;

import java.util.EnumSet;
import java.util.List;
import java.util.UUID;

/**
 * Goal for wolves to share food with hungry pack members.
 * <p>
 * When a wolf is carrying food, it will seek out hungry pack members
 * and bring food to them. Priority order:
 * <ol>
 *   <li>Mate (highest priority)</li>
 *   <li>Alpha wolf</li>
 *   <li>Other hungry pack members</li>
 * </ol>
 * <p>
 * This behavior simulates social food sharing observed in wolf packs,
 * where wolves bring food back to the pack for puppies, elders, and
 * breeding pairs.
 */
public class WolfShareFoodGoal extends Goal {

    private static final String STORAGE_KEY = "wolf_item_storage";
    private static final double SEARCH_RADIUS = 32.0;
    private static final double DELIVERY_DISTANCE = 2.0;
    private static final int HIERARCHY_ALPHA = 1;

    private final Wolf wolf;
    private final AnimalItemStorage storage;
    private Wolf targetWolf;
    private UUID mateId;

    public WolfShareFoodGoal(Wolf wolf) {
        this.wolf = wolf;
        this.storage = AnimalItemStorage.get(wolf, STORAGE_KEY);
        setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        if (!storage.hasItem()) {
            return false;
        }

        if (wolf.getRandom().nextInt(reducedTickDelay(5)) != 0) {
            return false;
        }

        if (WolfBehaviorHandle.isHungry(wolf)) {
            return false;
        }

        targetWolf = findHungryPackMember();
        return targetWolf != null;
    }

    @Override
    public boolean canContinueToUse() {
        return targetWolf != null
            && targetWolf.isAlive()
            && storage.hasItem()
            && !WolfBehaviorHandle.isHungry(wolf);
    }

    @Override
    public void start() {
        mateId = getMateId();
    }

    @Override
    public void stop() {
        targetWolf = null;
    }

    @Override
    public void tick() {
        if (targetWolf == null || !storage.hasItem()) {
            return;
        }

        if (!targetWolf.isAlive() || !WolfBehaviorHandle.isHungry(targetWolf)) {
            targetWolf = null;
            return;
        }

        wolf.getLookControl().setLookAt(targetWolf, 30.0f, 30.0f);

        double distance = wolf.position().distanceTo(targetWolf.position());
        if (distance > DELIVERY_DISTANCE) {
            wolf.getNavigation().moveTo(targetWolf, 1.3);
        } else {
            dropFoodForTarget();
            targetWolf = null;
        }
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    /**
     * Find a hungry pack member to feed.
     * Prioritizes: mate > alpha > other hungry members.
     */
    private Wolf findHungryPackMember() {
        List<Wolf> nearbyWolves = wolf.level().getEntitiesOfClass(
            Wolf.class,
            wolf.getBoundingBox().inflate(SEARCH_RADIUS)
        );

        List<Wolf> packMembers = nearbyWolves.stream()
            .filter(w -> !w.equals(wolf))
            .filter(w -> !w.isTame())
            .filter(w -> WolfBehaviorHandle.isSamePack(wolf, w))
            .filter(WolfBehaviorHandle::isHungry)
            .toList();

        if (packMembers.isEmpty()) {
            return null;
        }

        Wolf mate = findMate(packMembers);
        if (mate != null) {
            return mate;
        }

        Wolf alpha = findAlpha(packMembers);
        if (alpha != null && WolfBehaviorHandle.isHungry(alpha)) {
            return alpha;
        }

        return packMembers.getFirst();
    }

    /**
     * Get the ID of this wolf's mate.
     */
    private UUID getMateId() {
        if (!wolf.isInLove()) {
            return null;
        }

        List<Wolf> nearbyWolves = wolf.level().getEntitiesOfClass(
            Wolf.class,
            wolf.getBoundingBox().inflate(16.0)
        );

        for (Wolf other : nearbyWolves) {
            if (other.isInLove() && !other.equals(wolf) && !other.isTame()) {
                return other.getUUID();
            }
        }

        return null;
    }

    /**
     * Find mate among pack members.
     */
    private Wolf findMate(List<Wolf> packMembers) {
        if (mateId == null) {
            return null;
        }

        for (Wolf member : packMembers) {
            if (member.getUUID().equals(mateId)) {
                return member;
            }
        }

        return null;
    }

    /**
     * Find alpha wolf among pack members.
     */
    private Wolf findAlpha(List<Wolf> packMembers) {
        return packMembers.stream()
            .filter(w -> WolfBehaviorHandle.getHierarchyRank(w) == HIERARCHY_ALPHA)
            .findFirst()
            .orElse(null);
    }

    /**
     * Drop food for the target wolf to eat.
     */
    private void dropFoodForTarget() {
        if (!wolf.level().isClientSide && storage.hasItem()) {
            ItemStack food = storage.getItem();
            storage.clearItem();

            wolf.level().addFreshEntity(new net.minecraft.world.entity.item.ItemEntity(
                wolf.level(),
                wolf.getX(),
                wolf.getY(),
                wolf.getZ(),
                food
            ));
        }
    }
}
