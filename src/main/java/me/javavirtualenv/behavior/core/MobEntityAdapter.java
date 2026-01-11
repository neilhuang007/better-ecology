package me.javavirtualenv.behavior.core;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Adapts a Minecraft Mob to the BehaviorEntity interface.
 * This allows behavior algorithms to work with Minecraft entities.
 */
public class MobEntityAdapter implements BehaviorEntity {

    private final Mob mob;
    private final Level level;

    public MobEntityAdapter(Mob mob) {
        this.mob = mob;
        this.level = mob.level();
    }

    @Override
    public Vec3d getPosition() {
        return new Vec3d(mob.getX(), mob.getY(), mob.getZ());
    }

    @Override
    public Vec3d getVelocity() {
        Vec3 delta = mob.getDeltaMovement();
        return new Vec3d(delta.x, delta.y, delta.z);
    }

    @Override
    public List<BehaviorEntity> getNearbyEntities(double radius) {
        Vec3 pos = mob.position();
        AABB searchBox = new AABB(
            pos.x - radius, pos.y - radius, pos.z - radius,
            pos.x + radius, pos.y + radius, pos.z + radius
        );

        return level.getEntities(mob, searchBox)
            .stream()
            .filter(entity -> entity instanceof Mob)
            .map(entity -> new MobEntityAdapter((Mob) entity))
            .collect(Collectors.toList());
    }

    @Override
    public Object getId() {
        return mob.getUUID();
    }

    @Override
    public Object getLevel() {
        return level;
    }

    @Override
    public Object getBlockPos() {
        return mob.blockPosition();
    }

    @Override
    public Object getUnderlyingEntity() {
        return mob;
    }

    /**
     * Gets the underlying Minecraft mob.
     * @return The wrapped Mob entity
     */
    public Mob getMob() {
        return mob;
    }

    /**
     * Gets the level this entity is in.
     * @return The Minecraft Level
     */
    public Level getLevelAsLevel() {
        return level;
    }
}
