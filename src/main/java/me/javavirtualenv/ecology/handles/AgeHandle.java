package me.javavirtualenv.ecology.handles;

import me.javavirtualenv.ecology.EcologyComponent;
import me.javavirtualenv.ecology.EcologyHandle;
import me.javavirtualenv.ecology.EcologyProfile;
import me.javavirtualenv.ecology.state.EntityState;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Mob;

/**
 * Handles age progression and lifecycle transitions.
 * Baby → Adult → (optional) Elderly → Death
 */
public final class AgeHandle implements EcologyHandle {
    private static final String CACHE_KEY = "better-ecology:age-cache";
    private static final String NBT_AGE_TICKS = "ageTicks";
    private static final String NBT_IS_ELDERLY = "isElderly";

    @Override
    public String id() {
        return "age";
    }

    @Override
    public boolean supports(EcologyProfile profile) {
        AgeCache cache = profile.cached(CACHE_KEY, () -> buildCache(profile));
        return cache != null && cache.enabled;
    }

    @Override
    public int tickInterval() {
        return 20; // Age updates once per second
    }

    @Override
    public void tick(Mob mob, EcologyComponent component, EcologyProfile profile) {
        AgeCache cache = profile.cached(CACHE_KEY, () -> buildCache(profile));
        if (cache == null) {
            return;
        }

        CompoundTag tag = component.getHandleTag(id());
        int ageTicks = getAgeTicks(tag);
        long elapsed = component.elapsedTicks();
        ageTicks += elapsed;
        setAgeTicks(tag, ageTicks);

        EntityState state = component.state();

        // Handle baby → adult transition
        if (mob instanceof AgeableMob ageable) {
            boolean wasBaby = ageable.isBaby();
            boolean shouldBeBaby = ageTicks < cache.babyDuration;

            if (wasBaby && !shouldBeBaby) {
                // Force grow up
                ageable.setAge(0);
            } else if (!wasBaby && shouldBeBaby) {
                ageable.setBaby(true);
            }
        }

        // Handle adult → elderly transition
        boolean isElderly = cache.elderlyAge > 0 && ageTicks >= cache.elderlyAge;
        tag.putBoolean(NBT_IS_ELDERLY, isElderly);
        state.setIsElderly(isElderly);

        // Handle natural death from old age
        if (cache.maxAge > 0 && ageTicks >= cache.maxAge) {
            mob.discard();
        }
    }

    @Override
    public void readNbt(Mob mob, EcologyComponent component, EcologyProfile profile, CompoundTag tag) {
        // Loaded automatically
    }

    @Override
    public void writeNbt(Mob mob, EcologyComponent component, EcologyProfile profile, CompoundTag tag) {
        CompoundTag handleTag = component.getHandleTag(id());
        tag.put(id(), handleHandleTag());
    }

    private CompoundTag handleHandleTag() {
        return new CompoundTag();
    }

    private AgeCache buildCache(EcologyProfile profile) {
        boolean enabled = profile.getBoolFast("age", "enabled", false);
        if (!enabled) {
            return null;
        }

        int babyDuration = profile.getIntFast("age", "baby_duration", 24000);
        int maturityAge = profile.getIntFast("age", "maturity_age", 24000);
        int elderlyAge = profile.getIntFast("age", "elderly_age", 0);
        int maxAge = profile.getIntFast("age", "max_age", 0);

        return new AgeCache(enabled, babyDuration, maturityAge, elderlyAge, maxAge);
    }

    private int getAgeTicks(CompoundTag tag) {
        if (!tag.contains(NBT_AGE_TICKS)) {
            return 0;
        }
        return tag.getInt(NBT_AGE_TICKS);
    }

    private void setAgeTicks(CompoundTag tag, int value) {
        tag.putInt(NBT_AGE_TICKS, value);
    }

    public static boolean isAdult(EcologyComponent component, int maturityAge) {
        CompoundTag tag = component.getHandleTag("age");
        if (!tag.contains(NBT_AGE_TICKS)) {
            return true; // No age system = assume adult
        }
        return tag.getInt(NBT_AGE_TICKS) >= maturityAge;
    }

    public static boolean isElderly(EcologyComponent component) {
        CompoundTag tag = component.getHandleTag("age");
        return tag.getBoolean(NBT_IS_ELDERLY);
    }

    public static int getAgeTicks(EcologyComponent component) {
        CompoundTag tag = component.getHandleTag("age");
        if (!tag.contains(NBT_AGE_TICKS)) {
            return 0;
        }
        return tag.getInt(NBT_AGE_TICKS);
    }

    private record AgeCache(
            boolean enabled,
            int babyDuration,
            int maturityAge,
            int elderlyAge,
            int maxAge
    ) {}
}
