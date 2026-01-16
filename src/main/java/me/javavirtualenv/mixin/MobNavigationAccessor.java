package me.javavirtualenv.mixin;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Accessor mixin to set navigation and movement control on Mob class.
 */
@Mixin(Mob.class)
public interface MobNavigationAccessor {

    @Accessor("navigation")
    void setNavigation(PathNavigation navigation);

    @Accessor("moveControl")
    void setMoveControl(MoveControl moveControl);
}
