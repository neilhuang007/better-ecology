package me.javavirtualenv.mixin.animal;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.animal.Turtle;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * Accessor mixin to access protected methods in Turtle class.
 */
@Mixin(Turtle.class)
public interface TurtleAccessor {

    /**
     * Gets the home position for this turtle.
     */
    @Invoker("getHomePos")
    BlockPos invokeGetHomePos();
}
