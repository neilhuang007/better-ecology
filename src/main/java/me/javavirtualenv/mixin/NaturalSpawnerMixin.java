package me.javavirtualenv.mixin;

import me.javavirtualenv.ecology.EcologyProfile;
import me.javavirtualenv.ecology.EcologyProfileRegistry;
import me.javavirtualenv.ecology.spawning.SpawnDensityTracker;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.chunk.ChunkAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Mixin into NaturalSpawner to enforce spawn density limits.
 * Intercepts spawning at multiple points to check carrying capacity.
 */
@Mixin(net.minecraft.world.level.NaturalSpawner.class)
public class NaturalSpawnerMixin {

    /**
     * Inject at HEAD of spawnCategoryForPosition to check global density limits.
     * This is called once per spawn attempt for a category.
     */
    @Inject(
        method = "spawnCategoryForPosition",
        at = @At("HEAD"),
        cancellable = true,
        require = 0
    )
    private static void betterEcology$checkCategoryDensity(
        MobCategory mobCategory,
        ServerLevel serverLevel,
        ChunkAccess chunkAccess,
        BlockPos blockPos,
        net.minecraft.world.level.NaturalSpawner.SpawnPredicate spawnPredicate,
        net.minecraft.world.level.NaturalSpawner.AfterSpawnCallback afterSpawnCallback,
        CallbackInfo ci
    ) {
        // Category-level density checks could go here
        // Currently, we defer to per-entity-type checks in isValidPositionForMob
    }

    /**
     * Inject into isValidPositionForMob to check per-entity-type density limits.
     * This is called just before the mob is added to the world, giving us access
     * to the fully constructed Mob instance with its EntityType.
     * Prevents double-spawning by checking regional density for ecology-managed mobs.
     */
    @Inject(
        method = "isValidPositionForMob",
        at = @At("HEAD"),
        cancellable = true,
        require = 0
    )
    private static void betterEcology$checkEntityDensity(
        ServerLevel serverLevel,
        Mob mob,
        double distanceToPlayer,
        CallbackInfoReturnable<Boolean> cir
    ) {
        EntityType<?> entityType = mob.getType();
        net.minecraft.resources.ResourceLocation key = BuiltInRegistries.ENTITY_TYPE.getKey(entityType);

        if (key == null) {
            return;
        }

        EcologyProfile profile = EcologyProfileRegistry.get(key);
        if (profile == null) {
            return;
        }

        int perChunkCap = profile.getInt("population.carrying_capacity.caps.per_chunk", Integer.MAX_VALUE);
        if (perChunkCap == Integer.MAX_VALUE) {
            return;
        }

        SpawnDensityTracker tracker = SpawnDensityTracker.getInstance(serverLevel);
        if (tracker == null) {
            return;
        }

        BlockPos pos = mob.blockPosition();
        SpawnDensityTracker.SpawnResult result = tracker.canSpawn(serverLevel, pos, entityType, perChunkCap);

        if (!result.isAllowed()) {
            cir.setReturnValue(false);
        }
    }
}
