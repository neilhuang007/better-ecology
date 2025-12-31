package me.javavirtualenv.mixin.block;

import me.javavirtualenv.ecology.EcologyComponent;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.NoteBlock;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin for NoteBlock to notify nearby parrots when played.
 */
@Mixin(NoteBlock.class)
public class NoteBlockMixin {

    /**
     * Inject after note block is played to notify nearby parrots.
     */
    @Inject(method = "playNote", at = @At("TAIL"))
    private void onNotePlayed(Level level, BlockPos pos, CallbackInfo ci) {
        // Notify nearby parrots that a note was played
        notifyNearbyParrots(level, pos);
    }

    /**
     * Notifies all nearby parrots that a note block was played.
     */
    private void notifyNearbyParrots(Level level, BlockPos pos) {
        if (level.isClientSide) {
            return;
        }

        // Get all nearby parrots (64 block radius)
        var nearbyParrots = level.getEntitiesOfClass(
            net.minecraft.world.entity.animal.Parrot.class,
            new AABB(pos).inflate(64.0)
        );

        for (var parrot : nearbyParrots) {
            EcologyComponent component = EcologyComponent.getOrCreate(parrot);
            if (component != null) {
                // Store note block play time in the parrot's component
                var noteData = component.getHandleTag("note_blocks");
                noteData.putLong(pos.toString(), level.getGameTime());
                component.setHandleTag("note_blocks", noteData);
            }
        }
    }
}
