package me.javavirtualenv.mixin.villager;

import me.javavirtualenv.behavior.villager.*;
import me.javavirtualenv.ecology.api.EcologyAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.npc.WanderingTrader;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin for WanderingTrader entity to add enhanced behaviors.
 */
@Mixin(WanderingTrader.class)
public class WanderingTraderMixin implements EcologyAccess {

    @Unique
    private TradingReputation betterEcology$tradingReputation;

    @Unique
    private GossipSystem betterEcology$gossipSystem;

    @Unique
    private boolean betterEcology$behaviorsInitialized = false;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void onConstruct(EntityType<? extends WanderingTrader> entityType, Level level, CallbackInfo ci) {
        WanderingTrader trader = (WanderingTrader) (Object) this;
        betterEcology$tradingReputation = new TradingReputation(trader);
        betterEcology$gossipSystem = new GossipSystem(trader);
    }

    @Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
    private void onSave(CompoundTag tag, CallbackInfo ci) {
        CompoundTag ecologyTag = new CompoundTag();

        ecologyTag.put("TradingReputation", betterEcology$tradingReputation.save());
        ecologyTag.put("GossipSystem", betterEcology$gossipSystem.save());

        tag.put("BetterEcology", ecologyTag);
    }

    @Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
    private void onLoad(CompoundTag tag, CallbackInfo ci) {
        if (tag.contains("BetterEcology")) {
            CompoundTag ecologyTag = tag.getCompound("BetterEcology");

            if (ecologyTag.contains("TradingReputation")) {
                betterEcology$tradingReputation.load(ecologyTag.getCompound("TradingReputation"));
            }
            if (ecologyTag.contains("GossipSystem")) {
                betterEcology$gossipSystem.load(ecologyTag.getCompound("GossipSystem"));
            }
        }
    }

    @Inject(method = "customServerAiStep", at = @At("TAIL"))
    private void onTick(CallbackInfo ci) {
        WanderingTrader trader = (WanderingTrader) (Object) this;

        // Initialize behaviors if not done
        if (!betterEcology$behaviorsInitialized) {
            betterEcology$behaviorsInitialized = true;
        }

        // Decay gossip periodically (less frequent for wandering traders)
        if (trader.level().getGameTime() % 2400 == 0) {
            betterEcology$gossipSystem.decayGossip();
        }
    }

    // Getter methods for behavior systems

    @Unique
    public TradingReputation betterEcology$getTradingReputation() {
        return betterEcology$tradingReputation;
    }

    @Unique
    public GossipSystem betterEcology$getGossipSystem() {
        return betterEcology$gossipSystem;
    }

    @Unique
    @Override
    public me.javavirtualenv.ecology.EcologyComponent betterEcology$getEcologyComponent() {
        return null;
    }

    /**
     * Custom accessor for trader-specific behavior systems.
     */
    @Unique
    public static TradingReputation getTradingReputation(WanderingTrader trader) {
        if (trader instanceof WanderingTraderMixin mixin) {
            return mixin.betterEcology$tradingReputation;
        }
        return null;
    }

    @Unique
    public static GossipSystem getGossipSystem(WanderingTrader trader) {
        if (trader instanceof WanderingTraderMixin mixin) {
            return mixin.betterEcology$gossipSystem;
        }
        return null;
    }
}
