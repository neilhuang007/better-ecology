package me.javavirtualenv.mixin;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.AbstractFish;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(LivingEntity.class)
public abstract class FishHurtMixin {

    @Inject(method = "hurt", at = @At("RETURN"))
    private void betterEcology$onFishHurt(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValue()) return;

        LivingEntity self = (LivingEntity)(Object)this;

        if (!(self instanceof AbstractFish)) return;
        if (self.level().isClientSide()) return;

        net.minecraft.world.entity.Entity attacker = source.getEntity();
        if (attacker == null) return;

        List<? extends Mob> nearbyFish = self.level().getEntitiesOfClass(
            self.getClass().asSubclass(Mob.class),
            self.getBoundingBox().inflate(16.0),
            fish -> fish != self && fish.isAlive()
        );

        for (Mob fish : nearbyFish) {
            if (attacker instanceof LivingEntity livingAttacker) {
                fish.setLastHurtByMob(livingAttacker);
            }
        }
    }
}
