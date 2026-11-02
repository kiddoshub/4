package com.gaspoweredcake.client33.systems.modules.world;

import baritone.api.BaritoneAPI;
import baritone.api.IBaritone;
import baritone.api.pathing.goals.Goal;
import baritone.api.pathing.goals.GoalBlock;
import baritone.api.pathing.goals.GoalYLevel;
import com.gaspoweredcake.client33.events.world.TickEvent;
import com.gaspoweredcake.client33.settings.*;
import com.gaspoweredcake.client33.systems.modules.Categories;
import com.gaspoweredcake.client33.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.Vec3;

/** Searches the chunks the server actually sends while Baritone travels underground. */
public class AutoBaseFinder extends Module {
    private final SettingGroup general = settings.getDefaultGroup();
    private final Setting<String> command = general.add(new StringSetting.Builder().name("teleport-command").description("Server teleport command, without the leading slash; empty skips teleport.").defaultValue("rtp").build());
    private final Setting<Integer> targetY = general.add(new IntSetting.Builder().name("target-y").description("Underground travel Y; must be inside the current dimension.").defaultValue(-2).range(-2048, 2048).sliderRange(-64, 128).build());
    private final Setting<Integer> minimumStorage = general.add(new IntSetting.Builder().name("minimum-storage").description("Storage blocks in one chunk required to stop.").defaultValue(4).range(1, 256).sliderRange(1, 20).build());
    private final Setting<Integer> scanRadius = general.add(new IntSetting.Builder().name("scan-radius-chunks").description("Radius of loaded chunks checked for storage.").defaultValue(8).range(1, 16).sliderRange(1, 16).build());
    private final Setting<Double> health = general.add(new DoubleSetting.Builder().name("stop-health").description("Stop when health reaches this value.").defaultValue(8).range(0, 20).sliderRange(0, 20).build());
    private final Setting<Boolean> stopPlayers = general.add(new BoolSetting.Builder().name("stop-on-player").description("Stop when another player is loaded.").defaultValue(true).build());
    private final Setting<Boolean> stopLava = general.add(new BoolSetting.Builder().name("stop-on-lava").description("Stop when lava is within two blocks of the player.").defaultValue(true).build());
    private final Setting<Boolean> disconnect = general.add(new BoolSetting.Builder().name("disconnect-on-stash").description("Disconnect after detecting storage.").defaultValue(false).build());

    private enum Phase { WAIT_TELEPORT, DESCEND, TUNNEL }
    private final IBaritone baritone = BaritoneAPI.getProvider().getPrimaryBaritone();
    private Phase phase;
    private Goal goal;
    private Vec3 origin, lastProgress;
    private Object world;
    private int ticks, stalled, scanIndex, centerX, centerZ;
    private float direction;

    public AutoBaseFinder() { super(Categories.World, "auto-base-finder", "Teleports, travels underground and stops when storage is found. Requires Baritone."); }

    @Override public void onActivate() {
        if (mc.player == null || mc.level == null || mc.getConnection() == null) { error("Join a world first."); toggle(); return; }
        if (targetY.get() < mc.level.getMinY() || targetY.get() >= mc.level.getMinY() + mc.level.getHeight()) { error("Target Y is outside this dimension."); toggle(); return; }
        if (baritone.getPathingControlManager().mostRecentInControl().isPresent()) { error("Stop the current Baritone process first."); toggle(); return; }
        world = mc.level; goal = null; ticks = stalled = scanIndex = 0;
        origin = lastProgress = mc.player.position(); direction = mc.player.getYRot();
        centerX = mc.player.blockPosition().getX() >> 4; centerZ = mc.player.blockPosition().getZ() >> 4;
        String cmd = command.get().trim();
        if (cmd.startsWith("/")) cmd = cmd.substring(1).trim();
        if (cmd.isEmpty()) beginDescent();
        else { phase = Phase.WAIT_TELEPORT; mc.getConnection().sendCommand(cmd); info("Waiting for teleport confirmation."); }
    }

    @Override public void onDeactivate() {
        if (goal != null && baritone.getCustomGoalProcess().getGoal() == goal) baritone.getPathingBehavior().cancelEverything();
        goal = null;
    }

    private void beginDescent() {
        phase = Phase.DESCEND; ticks = stalled = 0; lastProgress = mc.player.position();
        setGoal(new GoalYLevel(targetY.get()));
        info("Travelling to Y %d.", targetY.get());
    }

    private void setGoal(Goal next) { goal = next; baritone.getCustomGoalProcess().setGoalAndPath(next); }

    @EventHandler private void onTick(TickEvent.Post event) {
        if (mc.player == null || mc.level == null || mc.level != world || !mc.player.isAlive()) { stop("World changed or player unavailable."); return; }
        if (mc.player.getHealth() <= health.get()) { stop("Low health."); return; }
        if (stopPlayers.get() && mc.level.players().stream().anyMatch(p -> p != mc.player && p.isAlive() && !p.isSpectator())) { stop("Player detected."); return; }
        if (stopLava.get() && lavaNearby()) { stop("Lava nearby."); return; }
        if (phase == Phase.WAIT_TELEPORT) {
            if (mc.player.position().distanceToSqr(origin) >= 256) beginDescent();
            else if (++ticks >= 1200) stop("Teleport not confirmed within 60 seconds.");
            return;
        }
        if (scanChunks()) return;
        if (baritone.getCustomGoalProcess().getGoal() != goal) { stop("Baritone goal was changed."); return; }
        if (mc.player.position().distanceToSqr(lastProgress) >= 1) { lastProgress = mc.player.position(); stalled = 0; }
        else if (++stalled >= 1200) { stop("No movement for 60 seconds."); return; }
        if (phase == Phase.DESCEND && mc.player.blockPosition().getY() == targetY.get()) {
            phase = Phase.TUNNEL; nextTunnel();
        } else if (phase == Phase.TUNNEL && goal.isInGoal(mc.player.blockPosition())) nextTunnel();
    }

    private void nextTunnel() {
        double radians = Math.toRadians(direction);
        BlockPos p = mc.player.blockPosition();
        setGoal(new GoalBlock(p.getX() - (int) Math.round(Math.sin(radians) * 128), targetY.get(), p.getZ() + (int) Math.round(Math.cos(radians) * 128)));
        info("Searching ahead at Y %d.", targetY.get());
    }

    private boolean scanChunks() {
        int cx = mc.player.blockPosition().getX() >> 4, cz = mc.player.blockPosition().getZ() >> 4;
        if (cx != centerX || cz != centerZ) { centerX = cx; centerZ = cz; }
        int radius = scanRadius.get(), width = radius * 2 + 1, total = width * width;
        for (int i = 0; i < 8; i++) {
            int index = Math.floorMod(scanIndex++, total), x = centerX + index % width - radius, z = centerZ + index / width - radius;
            if (!mc.level.hasChunk(x, z)) continue;
            LevelChunk chunk = mc.level.getChunk(x, z);
            int count = 0;
            for (BlockEntity entity : chunk.getBlockEntities().values()) {
                if (entity instanceof ChestBlockEntity || entity instanceof BarrelBlockEntity || entity instanceof ShulkerBoxBlockEntity
                    || entity instanceof HopperBlockEntity || entity instanceof DispenserBlockEntity || entity instanceof SpawnerBlockEntity) count++;
            }
            if (count >= minimumStorage.get()) {
                info("Possible stash: %d storage blocks in chunk %d, %d.", count, x, z);
                boolean logOut = disconnect.get(); toggle();
                if (logOut && mc.getConnection() != null) mc.getConnection().getConnection().disconnect(Component.literal("Auto Base Finder: stash detected"));
                return true;
            }
        }
        return false;
    }

    private boolean lavaNearby() {
        BlockPos p = mc.player.blockPosition();
        for (BlockPos pos : BlockPos.betweenClosed(p.offset(-2, -2, -2), p.offset(2, 2, 2)))
            if (mc.level.getFluidState(pos).is(FluidTags.LAVA)) return true;
        return false;
    }
    private void stop(String reason) { error("%s", reason); toggle(); }
}
