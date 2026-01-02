package me.javavirtualenv.mixin.villager;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import me.javavirtualenv.behavior.villager.DailyRoutine;
import me.javavirtualenv.behavior.villager.EnhancedFarming;
import me.javavirtualenv.behavior.villager.GossipSystem;
import me.javavirtualenv.behavior.villager.TradingReputation;
import me.javavirtualenv.behavior.villager.VillagerThreatResponse;
import me.javavirtualenv.behavior.villager.WorkStationAI;
import me.javavirtualenv.ecology.api.EcologyAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.level.Level;

/**
 * Mixin for Villager entity to add enhanced behaviors.
 */
@Mixin(Villager.class)
public class VillagerMixin implements EcologyAccess {

    @Unique
    private TradingReputation betterEcology$tradingReputation;

    @Unique
    private GossipSystem betterEcology$gossipSystem;

    @Unique
    private WorkStationAI betterEcology$workStationAI;

    @Unique
    private DailyRoutine betterEcology$dailyRoutine;

    @Unique
    private EnhancedFarming betterEcology$enhancedFarming;

    @Unique
    private VillagerThreatResponse betterEcology$threatResponse;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void onConstruct(EntityType<? extends Villager> entityType, Level level, CallbackInfo ci) {
        betterEcology$ensureInitialized();
    }

    @Unique
    private void betterEcology$ensureInitialized() {
        Villager villager = (Villager) (Object) this;
        if (betterEcology$tradingReputation == null) {
            betterEcology$tradingReputation = new TradingReputation(villager);
        }
        if (betterEcology$gossipSystem == null) {
            betterEcology$gossipSystem = new GossipSystem(villager);
        }
        if (betterEcology$workStationAI == null) {
            betterEcology$workStationAI = new WorkStationAI(villager);
        }
        if (betterEcology$dailyRoutine == null) {
            betterEcology$dailyRoutine = new DailyRoutine(villager);
        }
        if (betterEcology$enhancedFarming == null) {
            betterEcology$enhancedFarming = new EnhancedFarming(villager);
        }
        if (betterEcology$threatResponse == null) {
            betterEcology$threatResponse = new VillagerThreatResponse(villager);
        }
    }

    @Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
    private void onSave(CompoundTag tag, CallbackInfo ci) {
        betterEcology$ensureInitialized();

        CompoundTag ecologyTag = new CompoundTag();

        ecologyTag.put("TradingReputation", betterEcology$tradingReputation.save());
        ecologyTag.put("GossipSystem", betterEcology$gossipSystem.save());
        ecologyTag.put("WorkStationAI", betterEcology$workStationAI.save());
        ecologyTag.put("DailyRoutine", betterEcology$dailyRoutine.save());
        ecologyTag.put("EnhancedFarming", betterEcology$enhancedFarming.save());

        tag.put("BetterEcology", ecologyTag);
    }

    @Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
    private void onLoad(CompoundTag tag, CallbackInfo ci) {
        betterEcology$ensureInitialized();

        if (tag.contains("BetterEcology")) {
            CompoundTag ecologyTag = tag.getCompound("BetterEcology");

            if (ecologyTag.contains("TradingReputation")) {
                betterEcology$tradingReputation.load(ecologyTag.getCompound("TradingReputation"));
            }
            if (ecologyTag.contains("GossipSystem")) {
                betterEcology$gossipSystem.load(ecologyTag.getCompound("GossipSystem"));
            }
            if (ecologyTag.contains("WorkStationAI")) {
                betterEcology$workStationAI.load(ecologyTag.getCompound("WorkStationAI"));
            }
            if (ecologyTag.contains("DailyRoutine")) {
                betterEcology$dailyRoutine.load(ecologyTag.getCompound("DailyRoutine"));
            }
            if (ecologyTag.contains("EnhancedFarming")) {
                betterEcology$enhancedFarming.load(ecologyTag.getCompound("EnhancedFarming"));
            }
        }
    }

    @Inject(method = "customServerAiStep", at = @At("TAIL"))
    private void onTick(CallbackInfo ci) {
        Villager villager = (Villager) (Object) this;

        // Ensure behaviors are initialized
        betterEcology$ensureInitialized();

        // Tick behavior systems
        betterEcology$dailyRoutine.tick();
        betterEcology$workStationAI.tick();
        betterEcology$enhancedFarming.tick();
        betterEcology$threatResponse.tick();

        // Decay gossip periodically
        if (villager.level().getGameTime() % 1200 == 0) {
            betterEcology$gossipSystem.decayGossip();
        }

        // Update supply/demand periodically
        if (villager.level().getGameTime() % 600 == 0) {
            betterEcology$tradingReputation.updateSupplyDemand();
        }
    }

    // Getter methods for behavior systems - override interface defaults

    @Override
    public TradingReputation betterEcology$getTradingReputation() {
        return betterEcology$tradingReputation;
    }

    @Override
    public GossipSystem betterEcology$getGossipSystem() {
        return betterEcology$gossipSystem;
    }

    @Override
    public WorkStationAI betterEcology$getWorkStationAI() {
        return betterEcology$workStationAI;
    }

    @Override
    public DailyRoutine betterEcology$getDailyRoutine() {
        return betterEcology$dailyRoutine;
    }

    @Override
    public EnhancedFarming betterEcology$getEnhancedFarming() {
        return betterEcology$enhancedFarming;
    }

    @Override
    public VillagerThreatResponse betterEcology$getThreatResponse() {
        return betterEcology$threatResponse;
    }

    @Unique
    @Override
    public me.javavirtualenv.ecology.EcologyComponent betterEcology$getEcologyComponent() {
        // Return null - villagers use their own mixin-based behavior systems
        return null;
    }

}
