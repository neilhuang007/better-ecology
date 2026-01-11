package me.javavirtualenv.behavior.core;

import net.minecraft.world.phys.Vec3;

/**
 * Utility class for converting between Vec3d and Minecraft's Vec3.
 * This isolates Minecraft dependencies to a single class for easier testing.
 */
public final class Vec3dConverter {

    private Vec3dConverter() {
        // Utility class
    }

    /**
     * Creates a Vec3d from Minecraft's Vec3.
     * @param minecraftVec Minecraft vector
     * @return New Vec3d with same components
     */
    public static Vec3d fromMinecraft(Vec3 minecraftVec) {
        return new Vec3d(minecraftVec.x, minecraftVec.y, minecraftVec.z);
    }

    /**
     * Converts a Vec3d to Minecraft's Vec3.
     * @param vec3d Pure Java vector
     * @return New Minecraft Vec3 with same components
     */
    public static Vec3 toMinecraft(Vec3d vec3d) {
        return new Vec3(vec3d.x, vec3d.y, vec3d.z);
    }
}
