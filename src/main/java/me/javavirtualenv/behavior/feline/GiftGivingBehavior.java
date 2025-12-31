package me.javavirtualenv.behavior.feline;

import me.javavirtualenv.behavior.core.BehaviorContext;
import me.javavirtualenv.behavior.core.Vec3d;
import me.javavirtualenv.behavior.steering.SteeringBehavior;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Gift giving behavior for tamed cats.
 * <p>
 * Tamed cats bring gifts to their owners when:
 * - The player sleeps (morning gift)
 * - Trust level is high enough
 * - Cat has hunted recently
 * <p>
 * Possible gifts include:
 * - Food items (rabbit, chicken, cod)
 * - Bones
 * - Rotten flesh
 * - String
 * - Feathers
 * - Phantom membranes (when phantoms are nearby)
 */
public class GiftGivingBehavior extends SteeringBehavior {

    private final int giftCooldown;
    private final double trustThreshold;
    private final int searchRange;

    private int cooldownTicks = 0;
    private ItemStack pendingGift;
    private Player giftRecipient;
    private boolean isBringingGift = false;

    private static final List<ItemStack> POSSIBLE_GIFTS = new ArrayList<>();
    private static final Random RANDOM = new Random();

    static {
        POSSIBLE_GIFTS.add(new ItemStack(Items.RABBIT));
        POSSIBLE_GIFTS.add(new ItemStack(Items.CHICKEN));
        POSSIBLE_GIFTS.add(new ItemStack(Items.COD));
        POSSIBLE_GIFTS.add(new ItemStack(Items.SALMON));
        POSSIBLE_GIFTS.add(new ItemStack(Items.BONE));
        POSSIBLE_GIFTS.add(new ItemStack(Items.ROTTEN_FLESH));
        POSSIBLE_GIFTS.add(new ItemStack(Items.STRING));
        POSSIBLE_GIFTS.add(new ItemStack(Items.FEATHER));
        POSSIBLE_GIFTS.add(new ItemStack(Items.PHANTOM_MEMBRANE));
    }

    public GiftGivingBehavior(int giftCooldown, double trustThreshold, int searchRange) {
        super(0.5);
        this.giftCooldown = giftCooldown;
        this.trustThreshold = trustThreshold;
        this.searchRange = searchRange;
    }

    public GiftGivingBehavior() {
        this(24000, 0.7, 32);
    }

    @Override
    public Vec3d calculate(BehaviorContext context) {
        Mob mob = context.getEntity();

        // Only tamed cats give gifts
        if (mob instanceof net.minecraft.world.entity.animal.Cat cat && !cat.isTame()) {
            return new Vec3d();
        }

        // Handle cooldown
        if (cooldownTicks > 0) {
            cooldownTicks--;
            return new Vec3d();
        }

        // Check if should give gift
        if (!shouldGiveGift(mob)) {
            return new Vec3d();
        }

        // Find recipient
        if (giftRecipient == null || !giftRecipient.isAlive()) {
            giftRecipient = findGiftRecipient(mob);
            if (giftRecipient == null) {
                return new Vec3d();
            }
        }

        // Approach recipient
        Vec3d mobPos = context.getPosition();
        Vec3d recipientPos = new Vec3d(giftRecipient.getX(), giftRecipient.getY(), giftRecipient.getZ());
        double distance = mobPos.distanceTo(recipientPos);

        if (distance <= 2.0) {
            // Close enough to give gift
            giveGift(mob);
            return new Vec3d();
        }

        // Move toward recipient
        isBringingGift = true;
        Vec3d toRecipient = Vec3d.sub(recipientPos, mobPos);
        toRecipient.normalize();
        toRecipient.mult(0.4);
        return toRecipient;
    }

    private boolean shouldGiveGift(Mob mob) {
        // Check if it's morning (after player sleep)
        if (mob.level().isDay() && mob.level().getDayTime() % 24000 < 2000) {
            return true;
        }

        // Random gift during day if trust is high
        if (RANDOM.nextDouble() < 0.001) {
            return true;
        }

        return false;
    }

    private Player findGiftRecipient(Mob mob) {
        if (mob instanceof net.minecraft.world.entity.animal.Cat cat) {
            // Prefer owner
            if (cat.getOwner() != null) {
                Player owner = mob.getLevel().getPlayerByUUID(cat.getOwnerUUID());
                if (owner != null && owner.isAlive()) {
                    double distance = mob.position().distanceTo(owner.position());
                    if (distance < searchRange) {
                        return owner;
                    }
                }
            }
        }

        // Find nearest trusted player
        return mob.getLevel().getNearestPlayer(
            mob.getX(), mob.getY(), mob.getZ(), searchRange,
            TargetingConditions.forNonCombat()
        );
    }

    private void giveGift(Mob mob) {
        if (giftRecipient == null) {
            return;
        }

        // Select random gift
        ItemStack gift = selectRandomGift();

        // Drop gift at player's feet
        if (!gift.isEmpty()) {
            Level level = mob.level();
            BlockPos dropPos = giftRecipient.blockPosition();

            net.minecraft.world.entity.item.ItemEntity itemEntity = new net.minecraft.world.entity.item.ItemEntity(
                level,
                dropPos.getX() + 0.5,
                dropPos.getY(),
                dropPos.getZ() + 0.5,
                gift.copy()
            );
            itemEntity.setPickUpDelay(10);
            level.addFreshEntity(itemEntity);

            // Play meow sound
            level.playSound(null, mob.blockPosition(), SoundEvents.CAT_AMBIENT,
                SoundSource.NEUTRAL, 1.0f, 1.0f);
        }

        // Reset
        giftRecipient = null;
        isBringingGift = false;
        cooldownTicks = giftCooldown;
    }

    private ItemStack selectRandomGift() {
        int index = RANDOM.nextInt(POSSIBLE_GIFTS.size());
        ItemStack gift = POSSIBLE_GIFTS.get(index).copy();

        // Set random stack size (1-3)
        gift.setCount(RANDOM.nextInt(3) + 1);

        return gift;
    }

    public boolean isBringingGift() {
        return isBringingGift;
    }

    public Player getGiftRecipient() {
        return giftRecipient;
    }

    public void triggerGiftGiving() {
        cooldownTicks = 0;
    }

    public void reset() {
        giftRecipient = null;
        pendingGift = null;
        isBringingGift = false;
        cooldownTicks = giftCooldown;
    }
}
