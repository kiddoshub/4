
package com.gaspoweredcake.client33.systems.modules.movement;

import com.gaspoweredcake.client33.events.entity.player.PlayerMoveEvent;
import com.gaspoweredcake.client33.events.packets.PacketEvent;
import com.gaspoweredcake.client33.events.world.TickEvent;
import com.gaspoweredcake.client33.mixin.LocalPlayerAccessor;
import com.gaspoweredcake.client33.mixin.ServerboundMovePlayerPacketAccessor;
import com.gaspoweredcake.client33.settings.*;
import com.gaspoweredcake.client33.systems.modules.Categories;
import com.gaspoweredcake.client33.systems.modules.Module;
import com.gaspoweredcake.client33.mixininterface.IVec3;
import com.gaspoweredcake.client33.utils.Utils;
import com.gaspoweredcake.client33.utils.entity.EntityUtils;
import com.gaspoweredcake.client33.utils.movement.FlightGradient;
import com.gaspoweredcake.client33.systems.modules.Modules;
import meteordevelopment.orbit.EventHandler;
import meteordevelopment.orbit.EventPriority;
import net.minecraft.network.protocol.game.ClientboundPlayerAbilitiesPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.entity.MoverType;
import java.util.concurrent.atomic.AtomicBoolean;

public class Flight extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgAntiKick = settings.createGroup("Anti Kick"); //Pog

    private final Setting<Mode> mode = sgGeneral.add(new EnumSetting.Builder<Mode>()
        .name("mode")
        .description("The mode for Flight.")
        .defaultValue(Mode.Auto)
        .onChanged(mode -> {
            if (!isActive() || !Utils.canUpdate()) return;
            restoreAbilities();
            resetAntiKick();
            if (usesAbilities()) applyAbilities();
        })
        .build()
    );

    private final Setting<Double> speed = sgGeneral.add(new DoubleSetting.Builder()
        .name("speed")
        .description("Your speed when flying.")
        .defaultValue(0.1)
        .min(0.0)
        .build()
    );

    private final SettingGroup sgPacketGlide = settings.createGroup("Packet Glide");
    private final Setting<Double> acceleration = sgPacketGlide.add(new DoubleSetting.Builder()
        .name("acceleration").description("Maximum change to each velocity component per tick.")
        .defaultValue(0.08).range(0.001, 2).sliderRange(0.01, 0.5)
        .visible(() -> mode.get() == Mode.PacketGlide).build());
    private final Setting<Double> horizontalCap = sgPacketGlide.add(new DoubleSetting.Builder()
        .name("speed-cap").description("Maximum horizontal velocity in blocks per tick; increase General Speed to reach it. Server limits vary.")
        .defaultValue(1.0).range(0.01, 10.0).sliderRange(0.01, 10.0)
        .visible(() -> mode.get() == Mode.PacketGlide).build());
    private final Setting<Double> verticalCap = sgPacketGlide.add(new DoubleSetting.Builder()
        .name("vertical-speed-cap").description("Maximum upward or downward velocity in blocks per tick; server limits vary.")
        .defaultValue(0.5).range(0.01, 10.0).sliderRange(0.01, 10.0)
        .visible(() -> mode.get() == Mode.PacketGlide).build());
    private final Setting<Double> descent = sgPacketGlide.add(new DoubleSetting.Builder()
        .name("idle-descent").description("Downward velocity when no vertical input is held.")
        .defaultValue(0.03).range(0, 1).sliderRange(0, 0.2)
        .visible(() -> mode.get() == Mode.PacketGlide).build());
    private final Setting<Boolean> stopOnCorrection = sgPacketGlide.add(new BoolSetting.Builder()
        .name("stop-on-correction").description("Disable Flight when the server sends a position correction.")
        .defaultValue(true).visible(() -> mode.get() == Mode.PacketGlide).build());

    private final Setting<Boolean> verticalSpeedMatch = sgGeneral.add(new BoolSetting.Builder()
        .name("vertical-speed-match")
        .description("Matches your vertical speed to your horizontal speed, otherwise uses vanilla ratio.")
        .defaultValue(false)
        .build()
    );

    private final Setting<InputMode> inputMode = sgGeneral.add(new EnumSetting.Builder<InputMode>()
        .name("input-mode")
        .description("Player Input follows processed movement controls; Keybinds reads the bound keys directly.")
        .defaultValue(InputMode.PlayerInput)
        .visible(() -> mode.get() != Mode.Abilities)
        .build()
    );

    private final Setting<Boolean> noSneak = sgGeneral.add(new BoolSetting.Builder()
        .name("no-sneak")
        .description("Prevents you from sneaking while flying.")
        .defaultValue(false)
        .visible(() -> mode.get() != Mode.Abilities)
        .build()
    );

    private final Setting<AntiKickMode> antiKickMode = sgAntiKick.add(new EnumSetting.Builder<AntiKickMode>()
        .name("mode")
        .description("The mode for anti kick.")
        .defaultValue(AntiKickMode.Packet)
        .build()
    );

    private final Setting<Integer> delay = sgAntiKick.add(new IntSetting.Builder()
        .name("delay")
        .description("The amount of delay, in ticks, between flying down a bit and return to original position")
        .defaultValue(20)
        .min(1)
        .sliderMax(200)
        .build()
    );

    // Anti Kick
    private final Setting<Integer> offTime = sgAntiKick.add(new IntSetting.Builder()
        .name("off-time")
        .description("The amount of delay, in ticks, to fly down a bit to reset floating ticks.")
        .defaultValue(1)
        .min(1)
        .sliderRange(1, 20)
        .build()
    );

    private int delayLeft;
    private int offLeft;
    private boolean flip;
    private float lastYaw;
    private double lastPacketY = Double.MAX_VALUE;
    private boolean savedAbilities;
    private boolean savedAllowFlying;
    private boolean savedFlying;
    private float savedFlySpeed;
    private Vec3 gradientVelocity = Vec3.ZERO;
    private final AtomicBoolean corrected = new AtomicBoolean();
    private boolean sendingGlidePacket;

    public Flight() {
        super(Categories.Movement, "flight", "Fly with server abilities or direct movement control.");
    }

    @Override
    public void onActivate() {
        if (!Utils.canUpdate()) return;
        savedAllowFlying = mc.player.getAbilities().mayfly;
        savedFlying = mc.player.getAbilities().flying;
        savedFlySpeed = mc.player.getAbilities().getFlyingSpeed();
        savedAbilities = true;
        resetAntiKick();
        if (usesAbilities()) applyAbilities();
    }

    @Override
    public void onDeactivate() {
        if (Utils.canUpdate()) restoreAbilities();
        savedAbilities = false;
        gradientVelocity = Vec3.ZERO;
        corrected.set(false);
    }

    @EventHandler
    private void onPreTick(TickEvent.Pre event) {
        if (!Utils.canUpdate()) return;
        if (mode.get() == Mode.PacketGlide) {
            if (corrected.getAndSet(false) && stopOnCorrection.get()) { warning("Server position correction; Flight stopped."); disable(); return; }
            if (mc.player.isPassenger() || !mc.player.isAlive()) { disable(); return; }
            if (mc.player.isFallFlying()) { gradientVelocity = Vec3.ZERO; return; }
            Vec3 target = getFlightVelocity();
            double targetY = target.y == 0 ? -descent.get() : target.y;
            var next = FlightGradient.next(new FlightGradient.Velocity(gradientVelocity.x, gradientVelocity.y, gradientVelocity.z),
                new FlightGradient.Velocity(target.x, targetY, target.z), acceleration.get(), horizontalCap.get(), verticalCap.get());
            gradientVelocity = new Vec3(next.x(), next.y(), next.z());
        }
        if (!usesAbilities()) {
            mc.player.getAbilities().flying = false;
            mc.player.setDeltaMovement(flightVelocity());
        }
        float currentYaw = mc.player.getYRot();
        if (usesAbilities() && mc.player.fallDistance >= 3f && currentYaw == lastYaw && mc.player.getDeltaMovement().length() < 0.003d) {
            mc.player.setYRot(currentYaw + (flip ? 1 : -1));
            flip = !flip;
        }
        lastYaw = currentYaw;
    }

    @EventHandler
    private void onPostTick(TickEvent.Post event) {
        if (!Utils.canUpdate() || packetGlidePaused()) return;
        if (delayLeft > 0) delayLeft--;

        if (offLeft <= 0 && delayLeft <= 0) {
            delayLeft = delay.get();
            offLeft = offTime.get();

            if (antiKickMode.get() == AntiKickMode.Packet) {
                // Resend movement packets
                ((LocalPlayerAccessor) mc.player).client33$setPositionReminder(20);
            }
        } else if (delayLeft <= 0) {
            boolean shouldReturn = false;

            if (antiKickMode.get() == AntiKickMode.Normal) {
                if (usesAbilities()) {
                    mc.player.getAbilities().flying = false;
                    shouldReturn = true;
                }
            } else if (antiKickMode.get() == AntiKickMode.Packet && offLeft == offTime.get()) {
                // Resend movement packets
                ((LocalPlayerAccessor) mc.player).client33$setPositionReminder(20);
            }

            offLeft--;

            if (shouldReturn) return;
        }

        if (mc.player.getYRot() != lastYaw) mc.player.setYRot(lastYaw);

        if (usesAbilities()) applyAbilities();
        else {
            mc.player.getAbilities().flying = false;
            mc.player.setDeltaMovement(flightVelocity());
            if (noSneak.get()) mc.player.setOnGround(false);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    private void onPlayerMove(PlayerMoveEvent event) {
        if (event.type != MoverType.SELF || usesAbilities() || packetGlidePaused()) return;
        Vec3 velocity = flightVelocity();
        ((IVec3) event.movement).client33$set(velocity.x, velocity.y, velocity.z);
    }

    private boolean packetGlidePaused() { return mode.get() == Mode.PacketGlide && mc.player != null && mc.player.isFallFlying(); }

    private Vec3 flightVelocity() { return mode.get() == Mode.PacketGlide ? gradientVelocity : getFlightVelocity(); }

    private Vec3 getFlightVelocity() {
        boolean keybinds = inputMode.get() == InputMode.Keybinds;
        boolean forward = keybinds ? mc.options.keyUp.isDown() : mc.player.input.keyPresses.forward();
        boolean backward = keybinds ? mc.options.keyDown.isDown() : mc.player.input.keyPresses.backward();
        boolean left = keybinds ? mc.options.keyLeft.isDown() : mc.player.input.keyPresses.left();
        boolean right = keybinds ? mc.options.keyRight.isDown() : mc.player.input.keyPresses.right();
        boolean jump = keybinds ? mc.options.keyJump.isDown() : mc.player.input.keyPresses.jump();
        boolean sneak = keybinds ? mc.options.keyShift.isDown() : mc.player.input.keyPresses.shift();

        Vec3 horizontal = Vec3.directionFromRotation(0, mc.player.getYRot()).scale((forward ? 1 : 0) - (backward ? 1 : 0))
            .add(Vec3.directionFromRotation(0, mc.player.getYRot() + 90).scale((right ? 1 : 0) - (left ? 1 : 0)));
        if (horizontal.lengthSqr() > 1) horizontal = horizontal.normalize();
        double horizontalSpeed = speed.get() * (mc.player.isSprinting() ? 15 : 10);
        double verticalSpeed = speed.get() * (verticalSpeedMatch.get() ? 10 : 5);
        return new Vec3(horizontal.x * horizontalSpeed, ((jump ? 1 : 0) - (sneak ? 1 : 0)) * verticalSpeed, horizontal.z * horizontalSpeed);
    }

    private boolean usesAbilities() {
        return mode.get() == Mode.Abilities || (mode.get() == Mode.Auto && savedAllowFlying);
    }

    private void resetAntiKick() {
        delayLeft = delay.get();
        offLeft = offTime.get();
        lastPacketY = Double.MAX_VALUE;
        gradientVelocity = Vec3.ZERO;
        corrected.set(false);
        flip = false;
        lastYaw = mc.player.getYRot();
    }

    private void applyAbilities() {
        if (mc.player.isSpectator()) return;
        mc.player.getAbilities().mayfly = true;
        mc.player.getAbilities().flying = true;
        mc.player.getAbilities().setFlyingSpeed(speed.get().floatValue());
    }

    private void restoreAbilities() {
        if (!savedAbilities) return;
        mc.player.getAbilities().mayfly = savedAllowFlying;
        mc.player.getAbilities().flying = savedFlying;
        mc.player.getAbilities().setFlyingSpeed(savedFlySpeed);
    }

    private void antiKickPacket(ServerboundMovePlayerPacket packet, double currentY) {
        // maximum time we can be "floating" is 80 ticks, so 4 seconds max
        if (this.delayLeft <= 0 && this.lastPacketY != Double.MAX_VALUE &&
            shouldFlyDown(currentY, this.lastPacketY) && EntityUtils.isOnAir(mc.player)) {
            // actual check is for >= -0.03125D, but we have to do a bit more than that
            // due to the fact that it's a bigger or *equal* to, and not just a bigger than
            ((ServerboundMovePlayerPacketAccessor) packet).client33$setY(lastPacketY - 0.03130D);
        } else {
            lastPacketY = currentY;
        }
    }

    /**
     * @see net.minecraft.network.protocol.game.ServerGamePacketListener#handleMovePlayer(ServerboundMovePlayerPacket)
     */
    @EventHandler
    private void onSendPacket(PacketEvent.Send event) {
        if (!(event.packet instanceof ServerboundMovePlayerPacket packet) || !Utils.canUpdate()) return;
        if (mode.get() == Mode.PacketGlide) {
            if (packetGlidePaused()) return;
            if (sendingGlidePacket || event.isCancelled() || Modules.get().isActive(Blink.class) || Modules.get().isActive(ChunkProbe.class)) return;
            var player = mc.player;
            ServerboundMovePlayerPacket.PosRot rewritten = new ServerboundMovePlayerPacket.PosRot(
                packet.getX(player.getX()), packet.getY(player.getY()), packet.getZ(player.getZ()),
                packet.getYRot(player.getYRot()), packet.getXRot(player.getXRot()), packet.isOnGround(), player.horizontalCollision);
            if (antiKickMode.get() == AntiKickMode.Packet) antiKickPacket(rewritten, packet.getY(player.getY()));
            event.cancel();
            sendingGlidePacket = true;
            try { mc.getConnection().send(rewritten); } finally { sendingGlidePacket = false; }
            return;
        }
        if (antiKickMode.get() != AntiKickMode.Packet) return;

        double currentY = packet.getY(Double.MAX_VALUE);
        if (currentY != Double.MAX_VALUE) {
            antiKickPacket(packet, currentY);
        } else {
            // if the packet is a Rot packet or an StatusOnly packet then we need to
            // make it a PosRot packet or a Pos packet respectively, so it has a Y value
            ServerboundMovePlayerPacket fullPacket;
            if (packet.hasRotation()) {
                fullPacket = new ServerboundMovePlayerPacket.PosRot(
                    mc.player.getX(),
                    mc.player.getY(),
                    mc.player.getZ(),
                    packet.getYRot(0),
                    packet.getXRot(0),
                    packet.isOnGround(),
                    mc.player.horizontalCollision
                );
            } else {
                fullPacket = new ServerboundMovePlayerPacket.Pos(
                    mc.player.getX(),
                    mc.player.getY(),
                    mc.player.getZ(),
                    packet.isOnGround(),
                    mc.player.horizontalCollision
                );
            }
            event.cancel();
            antiKickPacket(fullPacket, mc.player.getY());
            mc.getConnection().send(fullPacket);
        }
    }

    @EventHandler
    private void onReceivePacket(PacketEvent.Receive event) {
        if (event.packet instanceof ClientboundPlayerPositionPacket && mode.get() == Mode.PacketGlide) { corrected.set(true); return; }
        if (!Utils.canUpdate() || !(event.packet instanceof ClientboundPlayerAbilitiesPacket packet)) return;
        if (savedAbilities) {
            savedAllowFlying = packet.canFly();
            savedFlying = packet.isFlying();
            savedFlySpeed = packet.getFlyingSpeed();
        }
        if (!usesAbilities()) return;
        event.cancel();
        mc.player.getAbilities().invulnerable = packet.isInvulnerable();
        mc.player.getAbilities().instabuild = packet.canInstabuild();
        mc.player.getAbilities().setWalkingSpeed(packet.getWalkingSpeed());
        applyAbilities();
    }

    private boolean shouldFlyDown(double currentY, double lastY) {
        if (currentY >= lastY) {
            return true;
        } else return lastY - currentY < 0.03130D;
    }

    public float getFlyingSpeed() {
        return -1;
    }

    public boolean noSneak() {
        return isActive() && !usesAbilities() && !packetGlidePaused() && noSneak.get();
    }

    public enum Mode {
        Auto,
        Abilities,
        Velocity,
        PacketGlide
    }

    public enum InputMode {
        PlayerInput,
        Keybinds
    }

    public enum AntiKickMode {
        Normal,
        Packet,
        None
    }
}
