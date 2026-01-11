package me.javavirtualenv.behavior.parrot;

import me.javavirtualenv.BetterEcology;
import me.javavirtualenv.debug.BehaviorLogger;
import me.javavirtualenv.ecology.EcologyComponent;
import me.javavirtualenv.ecology.api.EcologyAccess;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.pathfinder.Path;

import java.util.EnumSet;

/**
 * AI goal for parrot perching behavior.
 * Parrots seek high perches, player shoulders, and perform short flights.
 */
public class ParrotPerchGoal extends Goal {

    private static final double SHOULDER_PERCH_DISTANCE = 3.0;
    private static final double PERCH_ARRIVAL_DISTANCE = 2.0;
    private static final double FLIGHT_SPEED = 1.0;
    private static final int PERCH_SEARCH_INTERVAL = 100;
    private static final int MAX_PERCH_TICKS = 1200;

    private final PathfinderMob parrot;
    private final PerchBehavior perchBehavior;
    private final PerchBehavior.PerchConfig config;
    private final EcologyComponent component;

    private BlockPos targetPerch;
    private Path currentPath;
    private int perchSearchTimer;
    private int perchTicks;
    private boolean isFlyingToPerch;
    private boolean isPerched;
    private boolean wasSpooked;

    private String lastDebugMessage = "";
    private boolean wasFlyingLastCheck = false;

    public ParrotPerchGoal(PathfinderMob parrot,
                          PerchBehavior perchBehavior,
                          PerchBehavior.PerchConfig config) {
        this.parrot = parrot;
        this.perchBehavior = perchBehavior;
        this.config = config;
        this.component = getComponent();
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.JUMP));
    }

    @Override
    public boolean canUse() {
        if (!parrot.isAlive()) {
            return false;
        }

        if (parrot.level().isClientSide) {
            return false;
        }

        if (parrot.isPassenger()) {
            return false;
        }

        CompoundTag danceTag = component.getHandleTag("dance");
        if (danceTag.getBoolean("is_dancing")) {
            return false;
        }

        return true;
    }

    @Override
    public boolean canContinueToUse() {
        if (!canUse()) {
            return false;
        }

        return isFlyingToPerch || isPerched;
    }

    @Override
    public void start() {
        debug("goal started");
        perchSearchTimer = 0;
        perchTicks = 0;
        isFlyingToPerch = false;
        isPerched = false;
        targetPerch = null;
    }

    @Override
    public void stop() {
        debug("goal stopped");
        stopPerching();
        targetPerch = null;
        currentPath = null;
        parrot.getNavigation().stop();
    }

    @Override
    public void tick() {
        boolean isFlyingNow = isFlyingToPerch;

        if (isFlyingNow != wasFlyingLastCheck) {
            debug("flight state changed: " + wasFlyingLastCheck + " -> " + isFlyingNow);
            wasFlyingLastCheck = isFlyingNow;
        }

        if (tryShoulderPerch()) {
            return;
        }

        if (isPerched) {
            tickPerching();
            return;
        }

        perchSearchTimer++;
        if (perchSearchTimer >= PERCH_SEARCH_INTERVAL) {
            perchSearchTimer = 0;
            searchForPerch();
        }

        if (isFlyingToPerch && targetPerch != null) {
            updateFlightToPerch();
        }
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    private boolean tryShoulderPerch() {
        if (parrot.isPassenger()) {
            return true;
        }

        if (!(parrot instanceof net.minecraft.world.entity.animal.Parrot vanillaParrot)) {
            return false;
        }

        if (!vanillaParrot.isTame()) {
            return false;
        }

        Player nearestPlayer = findNearestPlayer();
        if (nearestPlayer == null) {
            return false;
        }

        if (canPerchOnShoulder(nearestPlayer)) {
            perchOnShoulder(nearestPlayer);
            return true;
        }

        return false;
    }

    private Player findNearestPlayer() {
        return parrot.level().getNearestPlayer(
            parrot,
            SHOULDER_PERCH_DISTANCE
        );
    }

    private boolean canPerchOnShoulder(Player player) {
        CompoundTag leftShoulder = player.getShoulderEntityLeft();
        CompoundTag rightShoulder = player.getShoulderEntityRight();

        return leftShoulder.isEmpty() || rightShoulder.isEmpty();
    }

    private void perchOnShoulder(Player player) {
        if (!(parrot instanceof net.minecraft.world.entity.animal.Parrot vanillaParrot)) {
            return;
        }

        vanillaParrot.startRiding(player, true);

        CompoundTag perchTag = component.getHandleTag("perch");
        perchTag.putBoolean("is_perched", true);
        perchTag.putString("perch_type", "SHOULDER");
        perchTag.putUUID("shoulder_owner", player.getUUID());
        component.setHandleTag("perch", perchTag);

        isPerched = true;
        isFlyingToPerch = false;
        debug("perched on shoulder of " + player.getName().getString());
    }

    private void searchForPerch() {
        BlockPos perch = perchBehavior.findPerch();
        if (perch == null) {
            return;
        }

        PathNavigation navigation = parrot.getNavigation();
        currentPath = navigation.createPath(perch, 0);

        if (currentPath != null && currentPath.canReach()) {
            targetPerch = perch;
            isFlyingToPerch = true;
            debug("found perch at " + perch.getX() + "," + perch.getY() + "," + perch.getZ());
        } else {
            debug("perch found but unreachable");
        }
    }

    private void updateFlightToPerch() {
        if (targetPerch == null) {
            isFlyingToPerch = false;
            return;
        }

        double distance = parrot.position().distanceTo(
            net.minecraft.world.phys.Vec3.atCenterOf(targetPerch)
        );

        if (distance <= PERCH_ARRIVAL_DISTANCE) {
            startPerching(targetPerch);
            return;
        }

        PathNavigation navigation = parrot.getNavigation();
        if (!navigation.isInProgress() ||
            currentPath == null ||
            !currentPath.canReach()) {

            currentPath = navigation.createPath(targetPerch, 0);
            if (currentPath != null && currentPath.canReach()) {
                navigation.moveTo(targetPerch.getX() + 0.5, targetPerch.getY(), targetPerch.getZ() + 0.5, FLIGHT_SPEED);
                debug("flying to perch, distance=" + String.format("%.1f", distance));
            } else {
                debug("no path to perch, giving up");
                isFlyingToPerch = false;
                targetPerch = null;
            }
        }

        parrot.getLookControl().setLookAt(
            targetPerch.getX() + 0.5,
            targetPerch.getY() + 0.5,
            targetPerch.getZ() + 0.5,
            30.0f,
            30.0f
        );
    }

    private void startPerching(BlockPos pos) {
        targetPerch = pos;
        isFlyingToPerch = false;
        isPerched = true;
        perchTicks = 0;

        PerchBehavior.PerchType type = determinePerchType(pos);

        CompoundTag perchTag = component.getHandleTag("perch");
        perchTag.putBoolean("is_perched", true);
        perchTag.putString("perch_type", type.name());
        perchTag.putInt("perch_x", pos.getX());
        perchTag.putInt("perch_y", pos.getY());
        perchTag.putInt("perch_z", pos.getZ());
        perchTag.putInt("perch_ticks", 0);
        component.setHandleTag("perch", perchTag);

        debug("started perching at " + pos.getX() + "," + pos.getY() + "," + pos.getZ() + " (" + type + ")");
    }

    private void tickPerching() {
        perchTicks++;

        CompoundTag perchTag = component.getHandleTag("perch");
        perchTag.putInt("perch_ticks", perchTicks);
        component.setHandleTag("perch", perchTag);

        if (shouldLeavePerch()) {
            stopPerching();
            return;
        }

        if (perchTicks % 40 == 0) {
            adjustPerchPosition();
        }
    }

    private boolean shouldLeavePerch() {
        CompoundTag perchTag = component.getHandleTag("perch");

        if (wasSpooked) {
            debug("spooked, leaving perch");
            return true;
        }

        if (perchTicks > MAX_PERCH_TICKS) {
            debug("max perch time reached, leaving");
            return true;
        }

        if (targetPerch != null) {
            int perchX = perchTag.getInt("perch_x");
            int perchY = perchTag.getInt("perch_y");
            int perchZ = perchTag.getInt("perch_z");
            BlockPos currentPerch = new BlockPos(perchX, perchY, perchZ);

            if (!isValidPerch(currentPerch)) {
                debug("perch no longer valid, leaving");
                return true;
            }
        }

        return false;
    }

    private boolean isValidPerch(BlockPos pos) {
        if (pos == null) {
            return false;
        }

        double distance = parrot.distanceToSqr(
            pos.getX() + 0.5,
            pos.getY() + 0.5,
            pos.getZ() + 0.5
        );

        if (distance > config.perchSearchRadius * config.perchSearchRadius) {
            return false;
        }

        return true;
    }

    private void stopPerching() {
        isPerched = false;
        isFlyingToPerch = false;
        perchTicks = 0;

        CompoundTag perchTag = component.getHandleTag("perch");
        perchTag.putBoolean("is_perched", false);
        perchTag.remove("perch_type");
        perchTag.remove("perch_x");
        perchTag.remove("perch_y");
        perchTag.remove("perch_z");
        perchTag.putInt("perch_ticks", 0);
        component.setHandleTag("perch", perchTag);
    }

    private void adjustPerchPosition() {
        if (parrot.getRandom().nextDouble() < 0.3) {
            parrot.getJumpControl().jump();
        }

        float lookChange = parrot.getRandom().nextFloat() * 60 - 30;
        parrot.setYRot(parrot.getYRot() + lookChange);
    }

    private PerchBehavior.PerchType determinePerchType(BlockPos pos) {
        int height = pos.getY() - parrot.level().getMinBuildHeight();
        if (height >= config.minPerchHeight) {
            return PerchBehavior.PerchType.TREE_BRANCH;
        }
        return PerchBehavior.PerchType.GROUND;
    }

    private EcologyComponent getComponent() {
        if (!(parrot instanceof EcologyAccess access)) {
            return null;
        }
        return access.betterEcology$getEcologyComponent();
    }

    private void debug(String message) {
        lastDebugMessage = message;
        if (BehaviorLogger.isMinimal() || BetterEcology.DEBUG_MODE) {
            String prefix = "[ParrotPerch] Parrot #" + parrot.getId() + " ";
            BehaviorLogger.info(prefix + message);
        }
    }

    public String getLastDebugMessage() {
        return lastDebugMessage;
    }

    public String getDebugState() {
        CompoundTag perchTag = component.getHandleTag("perch");
        int perchTicks = perchTag.getInt("perch_ticks");

        return String.format(
            "flying=%s, perched=%s, target=%s, ticks=%d, path=%s",
            isFlyingToPerch,
            isPerched,
            targetPerch != null ? targetPerch.getX() + "," + targetPerch.getY() + "," + targetPerch.getZ() : "none",
            perchTicks,
            parrot.getNavigation().isInProgress() ? "moving" : "idle"
        );
    }

    public boolean isPerched() {
        return isPerched;
    }

    public BlockPos getTargetPerch() {
        return targetPerch;
    }
}
