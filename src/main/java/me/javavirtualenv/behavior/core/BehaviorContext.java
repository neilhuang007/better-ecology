package me.javavirtualenv.behavior.core;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;

public class BehaviorContext {
    private final Mob entity;
    private final Level level;
    private final Vec3d position;
    private final Vec3d velocity;
    private List<Entity> neighbors;

    public BehaviorContext(Mob entity) {
        this.entity = entity;
        this.level = entity.level();
        this.position = new Vec3d(entity.getX(), entity.getY(), entity.getZ());
        this.velocity = new Vec3d(
            entity.getDeltaMovement().x,
            entity.getDeltaMovement().y,
            entity.getDeltaMovement().z
        );
        this.neighbors = new ArrayList<>();
    }

    public Mob getEntity() {
        return entity;
    }

    public Mob getMob() {
        return entity;
    }

    public Level getLevel() {
        return level;
    }

    public Vec3d getPosition() {
        return position;
    }

    public Vec3d getVelocity() {
        return velocity;
    }

    public BlockPos getBlockPos() {
        return entity.blockPosition();
    }

    public double getSpeed() {
        return velocity.magnitude();
    }

    public List<Entity> getNeighbors() {
        return neighbors;
    }

    public void setNeighbors(List<Entity> neighbors) {
        this.neighbors = neighbors != null ? neighbors : new ArrayList<>();
    }
}
