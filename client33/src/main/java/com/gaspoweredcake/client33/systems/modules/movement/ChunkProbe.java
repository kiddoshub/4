package com.gaspoweredcake.client33.systems.modules.movement;

import com.gaspoweredcake.client33.events.packets.PacketEvent;
import com.gaspoweredcake.client33.events.world.TickEvent;
import com.gaspoweredcake.client33.settings.*;
import com.gaspoweredcake.client33.systems.modules.Categories;
import com.gaspoweredcake.client33.systems.modules.Module;
import com.gaspoweredcake.client33.systems.modules.Modules;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.world.phys.Vec3;
import java.util.concurrent.atomic.AtomicBoolean;

/** One explicit probe; the server remains authoritative over movement and chunk streaming. */
public class ChunkProbe extends Module {
    private final SettingGroup sg = settings.getDefaultGroup();
    private final Setting<Integer> offsetX = sg.add(new IntSetting.Builder().name("offset-x").description("Probe X offset in blocks.").defaultValue(0).range(-4096, 4096).sliderRange(-128, 128).build());
    private final Setting<Integer> offsetZ = sg.add(new IntSetting.Builder().name("offset-z").description("Probe Z offset in blocks.").defaultValue(0).range(-4096, 4096).sliderRange(-128, 128).build());
    private final Setting<Integer> targetY = sg.add(new IntSetting.Builder().name("target-y").description("Absolute Y coordinate for the probe.").defaultValue(-2).range(-2048, 2048).sliderRange(-64, 128).build());
    private enum Phase { READY, WAITING, DONE }
    private volatile Phase phase = Phase.DONE;
    private final AtomicBoolean corrected = new AtomicBoolean();
    private volatile boolean sending;
    private Vec3 target;
    private Object owner, world;

    public ChunkProbe() { super(Categories.Movement, "chunk-probe", "Attempts a one-tick server position probe, then reports the current position again."); }

    @Override public void onActivate() {
        phase = Phase.DONE; corrected.set(false); sending = false;
        if (mc.player == null || mc.level == null || mc.getConnection() == null || mc.player.isPassenger() || !mc.player.isAlive()) {
            error("A living, unmounted player is required."); disable(); return;
        }
        if (Modules.get().isActive(Blink.class)) { error("Disable Blink before probing."); disable(); return; }
        if (targetY.get() < mc.level.getMinY() || targetY.get() >= mc.level.getMinY() + mc.level.getHeight()) {
            error("Target Y is outside the current dimension."); disable(); return;
        }
        double x = mc.player.getX() + offsetX.get(), z = mc.player.getZ() + offsetZ.get();
        if (Math.abs(x) >= 29999984 || Math.abs(z) >= 29999984) { error("Probe is outside the world border limits."); disable(); return; }
        target = new Vec3(x, targetY.get(), z);
        owner = mc.player; world = mc.level; phase = Phase.READY;
    }

    @Override public void onDeactivate() {
        if (phase == Phase.WAITING && !corrected.get() && mc.player == owner && mc.level == world && mc.getConnection() != null)
            send(mc.player.position(), mc.player.onGround(), mc.player.horizontalCollision);
        phase = Phase.DONE; owner = world = null;
    }

    @EventHandler private void onTick(TickEvent.Post event) {
        if (phase == Phase.DONE) return;
        if (mc.player != owner || mc.level != world || mc.player == null || mc.getConnection() == null || !mc.player.isAlive() || mc.player.isPassenger()) { disable(); return; }
        if (corrected.get()) { warning("Server position correction received; probe stopped."); phase = Phase.DONE; disable(); return; }
        if (phase == Phase.READY) {
            phase = Phase.WAITING;
            send(target, false, false);
        } else {
            send(mc.player.position(), mc.player.onGround(), mc.player.horizontalCollision);
            phase = Phase.DONE;
            info("Probe attempted. The server decides whether to accept it or send data.");
            disable();
        }
    }

    @EventHandler private void onSend(PacketEvent.Send event) {
        if (phase == Phase.WAITING && !sending && event.packet instanceof ServerboundMovePlayerPacket) event.cancel();
    }
    @EventHandler private void onReceive(PacketEvent.Receive event) {
        if (phase == Phase.WAITING && event.packet instanceof ClientboundPlayerPositionPacket) corrected.set(true);
    }
    private void send(Vec3 pos, boolean ground, boolean collision) {
        sending = true;
        try { mc.getConnection().send(new ServerboundMovePlayerPacket.Pos(pos, ground, collision)); }
        finally { sending = false; }
    }
}
