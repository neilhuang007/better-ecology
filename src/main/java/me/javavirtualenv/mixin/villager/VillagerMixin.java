package me.javavirtualenv.mixin.villager;

import me.javavirtualenv.behavior.villager.*;
import me.javavirtualenv.ecology.api.EcologyAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

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
    private boolean betterEcology$behaviorsInitialized = false;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void onConstruct(EntityType<? extends Villager> entityType, Level level, CallbackInfo ci) {
        Villager villager = (Villager) (Object) this;
        betterEcology$tradingReputation = new TradingReputation(villager);
        betterEcology$gossipSystem = new GossipSystem(villager);
        betterEcology$workStationAI = new WorkStationAI(villager);
        betterEcology$dailyRoutine = new DailyRoutine(villager);
        betterEcology$enhancedFarming = new EnhancedFarming(villager);
    }

    @Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
    private void onSave(CompoundTag tag, CallbackInfo ci) {
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

        // Initialize behaviors if not done
        if (!betterEcology$behaviorsInitialized) {
            initializeBehaviors();
            betterEcology$behaviorsInitialized = true;
        }

        // Tick behavior systems
        betterEcology$dailyRoutine.tick();
        betterEcology$workStationAI.tick();
        betterEcology$enhancedFarming.tick();

        // Decay gossip periodically
        if (villager.level().getGameTime() % 1200 == 0) {
            betterEcology$gossipSystem.decayGossip();
        }
    }

    @Unique
    private void initializeBehaviors() {
        Villager villager = (Villager) (Object) this;

        // Goals will be registered through the goal selector system
        // This is handled by the separate goal registration system
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
    public WorkStationAI betterEcology$getWorkStationAI() {
        return betterEcology$workStationAI;
    }

    @Unique
    public DailyRoutine betterEcology$getDailyRoutine() {
        return betterEcology$dailyRoutine;
    }

    @Unique
    public EnhancedFarming betterEcology$getEnhancedFarming() {
        return betterEcology$enhancedFarming;
    }

    @Unique
    @Override
    public me.javavirtualenv.ecology.EcologyComponent betterEcology$getEcologyComponent() {
        // Return null - villagers use their own mixin-based behavior systems
        return null;
    }

    /**
     * Custom accessor for villager-specific behavior systems.
     */
    @Unique
    public static TradingReputation getTradingReputation(Villager villager) {
        if (villager instanceof VillagerMixin mixin) {
            return mixin.betterEcology$tradingReputation;
        }
        return null;
    }

    @Unique
    public static GossipSystem getGossipSystem(Villager villager) {
        if (villager instanceof VillagerMixin mixin) {
            return mixin.betterEcology$gossipSystem;
        }
        return null;
    }

    @Unique
    public static WorkStationAI getWorkStationAI(Villager villager) {
        if (villager instanceof VillagerMixin mixin) {
            return mixin.betterEcology$workStationAI;
        }
        return null;
    }

    @Unique
    public static DailyRoutine getDailyRoutine(Villager villager) {
        if (villager instanceof VillagerMixin mixin) {
            return mixin.betterEcology$dailyRoutine;
        }
        return null;
    }

    @Unique
    public static EnhancedFarming getEnhancedFarming(Villager villager) {
        if (villager instanceof VillagerMixin mixin) {
            return mixin.betterEcology$enhancedFarming;
        }
        return null;
    }
}
