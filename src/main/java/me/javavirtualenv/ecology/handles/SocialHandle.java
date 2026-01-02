package me.javavirtualenv.ecology.handles;

import me.javavirtualenv.ecology.EcologyComponent;
import me.javavirtualenv.ecology.EcologyHandle;
import me.javavirtualenv.ecology.EcologyProfile;
import me.javavirtualenv.ecology.spatial.SpatialIndex;
import me.javavirtualenv.ecology.state.EntityState;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Mob;

/**
 * Handles social needs for pack/herd animals.
 * Loneliness causes distress and affects behavior.
 */
public final class SocialHandle implements EcologyHandle {
    private static final String CACHE_KEY = "better-ecology:social-cache";
    private static final String NBT_SOCIAL = "social";
    private static final String NBT_LAST_GROUP_CHECK = "lastGroupCheck";
    private static final long MAX_CATCH_UP_TICKS = 24000L;

    @Override
    public String id() {
        return "social";
    }

    @Override
    public boolean supports(EcologyProfile profile) {
        SocialCache cache = profile.cached(CACHE_KEY, () -> buildCache(profile));
        return cache != null && cache.enabled;
    }

    @Override
    public int tickInterval() {
        return 20;
    }

    @Override
    public void tick(Mob mob, EcologyComponent component, EcologyProfile profile) {
        SocialCache cache = profile.cached(CACHE_KEY, () -> buildCache(profile));
        if (cache == null) {
            return;
        }

        CompoundTag tag = component.getHandleTag(id());
        int currentSocial = getCurrentSocial(tag, cache);
        boolean currentlyLonely = currentSocial < cache.lonelinessThreshold;

        // Check for nearby group members periodically
        // Use adaptive frequency: check less often when already with group
        int currentTick = mob.tickCount;
        int lastCheck = getLastGroupCheck(tag);
        boolean hadGroupNearby = tag.getBoolean("hasGroupNearby");
        int checkInterval = hadGroupNearby ? cache.checkInterval * 2 : cache.checkInterval;

        boolean hasGroupNearby;
        if (currentTick - lastCheck >= checkInterval) {
            hasGroupNearby = checkForGroup(mob, cache);
            setLastGroupCheck(tag, currentTick);
            tag.putBoolean("hasGroupNearby", hasGroupNearby);
        } else {
            hasGroupNearby = hadGroupNearby;
        }

        // Apply social change
        double change = hasGroupNearby ? cache.recoveryRate : -cache.decayRate;
        long elapsed = component.elapsedTicks();
        long effectiveTicks = Math.min(elapsed, MAX_CATCH_UP_TICKS);
        change *= effectiveTicks;

        int newSocial = (int) Math.round(Math.min(cache.maxValue, Math.max(0, currentSocial + change)));

        // During catch-up, keep social above loneliness threshold to prevent instant distress
        boolean isCatchUp = elapsed > 1;
        if (isCatchUp && newSocial < cache.lonelinessThreshold) {
            newSocial = cache.lonelinessThreshold;
        }

        // Only write if value changed to reduce NBT operations
        if (newSocial != currentSocial) {
            setSocial(tag, newSocial);
        }

        // Update loneliness state in entity state for other handlers to use
        boolean isLonely = newSocial < cache.lonelinessThreshold;
        component.state().setIsLonely(isLonely);
    }

    @Override
    public void registerGoals(Mob mob, EcologyComponent component, EcologyProfile profile) {
        // Could register a "SeekGroupGoal" for social animals
    }

    @Override
    public void readNbt(Mob mob, EcologyComponent component, EcologyProfile profile, CompoundTag tag) {
        // Loaded automatically
    }

    @Override
    public void writeNbt(Mob mob, EcologyComponent component, EcologyProfile profile, CompoundTag tag) {
        CompoundTag handleTag = component.getHandleTag(id());
        tag.put(id(), handleTag.copy());
    }

    private SocialCache buildCache(EcologyProfile profile) {
        boolean enabled = profile.getBoolFast("social", "enabled", false);
        if (!enabled) {
            return null;
        }

        int maxValue = profile.getIntFast("social", "max_value", 100);
        int startingValue = maxValue;
        double decayRate = profile.getDoubleFast("social", "decay_rate", 0.01);
        double recoveryRate = profile.getDoubleFast("social", "recovery_rate", 0.1);
        int lonelinessThreshold = profile.getIntFast("social", "loneliness_threshold", 30);
        int checkInterval = profile.getIntFast("social", "check_interval", 100);
        int groupRadius = profile.getIntFast("social", "group_radius", 32);

        return new SocialCache(enabled, maxValue, startingValue, decayRate, recoveryRate,
                lonelinessThreshold, checkInterval, groupRadius);
    }

    private int getCurrentSocial(CompoundTag tag, SocialCache cache) {
        if (!tag.contains(NBT_SOCIAL)) {
            return cache.startingValue;
        }
        return tag.getInt(NBT_SOCIAL);
    }

    private void setSocial(CompoundTag tag, int value) {
        tag.putInt(NBT_SOCIAL, value);
    }

    private int getLastGroupCheck(CompoundTag tag) {
        return tag.getInt(NBT_LAST_GROUP_CHECK);
    }

    private void setLastGroupCheck(CompoundTag tag, int tick) {
        tag.putInt(NBT_LAST_GROUP_CHECK, tick);
    }

    /**
     * Check for nearby group members using chunk-based spatial index.
     * O(1) chunk lookup + O(k) where k = entities in nearby chunks.
     * Much faster than getEntitiesOfClass which scans all loaded entities.
     */
    private boolean checkForGroup(Mob mob, SocialCache cache) {
        return SpatialIndex.hasNearbySameType(mob, cache.groupRadius);
    }

    public static boolean isLonely(EcologyComponent component) {
        CompoundTag tag = component.getHandleTag("social");
        if (!tag.contains(NBT_SOCIAL)) {
            return false; // No social system = not lonely
        }
        int socialValue = tag.getInt(NBT_SOCIAL);
        int threshold = tag.getInt("lonelinessThreshold");
        return socialValue < threshold;
    }

    private record SocialCache(
            boolean enabled,
            int maxValue,
            int startingValue,
            double decayRate,
            double recoveryRate,
            int lonelinessThreshold,
            int checkInterval,
            int groupRadius
    ) {}
}
