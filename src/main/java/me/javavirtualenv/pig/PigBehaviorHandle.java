package me.javavirtualenv.pig;

import me.javavirtualenv.BetterEcology;
import me.javavirtualenv.ecology.CodeBasedHandle;
import me.javavirtualenv.ecology.EcologyComponent;
import me.javavirtualenv.ecology.EcologyProfile;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.level.Level;

/**
 * Handle for managing pig-specific behaviors.
 * Handles mud effect timer and behavior-specific state.
 */
public class PigBehaviorHandle extends CodeBasedHandle {
    private static final String NBT_MUD_TIMER = "mudEffectTimer";
    private static final String NBT_EXCITEMENT_TIMER = "excitementTimer";
    private static final String MUD_EFFECT_DURATION = 6000;
    private static final int EXCITEMENT_DURATION = 300;

    @Override
    public String id() {
        return "pig_behavior";
    }

    @Override
    public int tickInterval() {
        return 20;
    }

    @Override
    public void tick(Mob mob, EcologyComponent component, EcologyProfile profile) {
        if (!(mob instanceof Pig pig)) {
            return;
        }

        CompoundTag pigData = component.getHandleTag(id());

        updateMudEffect(pig, pigData);
        updateExcitement(pig, pigData);
        checkNearbyTruffles(pig, pigData);
    }

    private void updateMudEffect(Pig pig, CompoundTag pigData) {
        int mudTimer = pigData.getInt(NBT_MUD_TIMER);
        if (mudTimer > 0) {
            pigData.putInt(NBT_MUD_TIMER, mudTimer - 1);
        }
    }

    private void updateExcitement(Pig pig, CompoundTag pigData) {
        int excitementTimer = pigData.getInt(NBT_EXCITEMENT_TIMER);
        if (excitementTimer > 0) {
            pigData.putInt(NBT_EXCITEMENT_TIMER, excitementTimer - 1);

            if (pig.level().isClientSide() && excitementTimer % 20 == 0) {
                showExcitementParticles(pig);
            }
        }
    }

    private void checkNearbyTruffles(Pig pig, CompoundTag pigData) {
        Level level = pig.level();
        if (level.isClientSide()) {
            return;
        }

        var nearbyItems = level.getEntitiesOfClass(
            net.minecraft.world.entity.item.ItemEntity.class,
            pig.getBoundingBox().inflate(8.0)
        );

        boolean hasNearbyTruffle = nearbyItems.stream()
            .anyMatch(item -> {
                var stack = item.getItem();
                return stack.getDescriptionId().contains("truffle");
            });

        if (hasNearbyTruffle) {
            pigData.putInt(NBT_EXCITEMENT_TIMER, EXCITEMENT_DURATION);
        }
    }

    private void showExcitementParticles(Pig pig) {
        Level level = pig.level();
        if (level.isClientSide()) {
            var pos = pig.position();
            for (int i = 0; i < 3; i++) {
                double offsetX = (Math.random() - 0.5) * 0.5;
                double offsetZ = (Math.random() - 0.5) * 0.5;
                level.addParticle(
                    net.minecraft.core.particles.ParticleTypes.HEART,
                    pos.x + offsetX,
                    pos.y + 0.5,
                    pos.z + offsetZ,
                    0.0,
                    0.1,
                    0.0
                );
            }
        }
    }

    @Override
    public void writeNbt(Mob mob, EcologyComponent component, EcologyProfile profile, CompoundTag tag) {
        tag.put(id(), component.getHandleTag(id()).copy());
    }

    public static boolean hasMudEffect(Pig pig) {
        if (!(pig instanceof me.javavirtualenv.ecology.api.EcologyAccess access)) {
            return false;
        }

        EcologyComponent component = access.betterEcology$getEcologyComponent();
        if (component == null) {
            return false;
        }

        CompoundTag pigData = component.getHandleTag("pig_behavior");
        return pigData.getInt(NBT_MUD_TIMER) > 0;
    }

    public static boolean isExcited(Pig pig) {
        if (!(pig instanceof me.javavirtualenv.ecology.api.EcologyAccess access)) {
            return false;
        }

        EcologyComponent component = access.betterEcology$getEcologyComponent();
        if (component == null) {
            return false;
        }

        CompoundTag pigData = component.getHandleTag("pig_behavior");
        return pigData.getInt(NBT_EXCITEMENT_TIMER) > 0;
    }

    public static void applyMudEffect(Pig pig, int duration) {
        if (!(pig instanceof me.javavirtualenv.ecology.api.EcologyAccess access)) {
            return;
        }

        EcologyComponent component = access.betterEcology$getEcologyComponent();
        if (component == null) {
            return;
        }

        CompoundTag pigData = component.getHandleTag("pig_behavior");
        pigData.putInt(NBT_MUD_TIMER, duration);

        BetterEcology.LOGGER.debug("Applied mud effect to pig for {} ticks", duration);
    }
}
