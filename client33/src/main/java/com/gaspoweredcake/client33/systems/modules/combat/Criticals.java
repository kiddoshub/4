
package com.gaspoweredcake.client33.systems.modules.combat;


import com.gaspoweredcake.client33.events.packets.PacketEvent;
import com.gaspoweredcake.client33.events.game.GameLeftEvent;
import com.gaspoweredcake.client33.events.world.TickEvent;
import com.gaspoweredcake.client33.mixininterface.IServerboundMovePlayerPacket;
import com.gaspoweredcake.client33.mixininterface.IVec3;
import com.gaspoweredcake.client33.settings.*;
import com.gaspoweredcake.client33.systems.modules.Categories;
import com.gaspoweredcake.client33.systems.modules.Module;
import com.gaspoweredcake.client33.systems.modules.Modules;
import com.gaspoweredcake.client33.utils.entity.EntityUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.protocol.game.ServerboundAttackPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundPunchPacket;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.MaceItem;
import net.minecraft.world.item.ItemStack;

public class Criticals extends Module {

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgMace = settings.createGroup("Mace");

    private final Setting<Mode> mode = sgGeneral.add(new EnumSetting.Builder<Mode>()
        .name("mode")
        .description("The mode on how Criticals will function.")
        .defaultValue(Mode.Packet)
        .build()
    );

    private final Setting<Boolean> ka = sgGeneral.add(new BoolSetting.Builder()
        .name("only-killaura")
        .description("Only performs crits when using killaura.")
        .defaultValue(false)
        .visible(() -> mode.get() != Mode.None)
        .build()
    );

    private final Setting<Boolean> mace = sgMace.add(new BoolSetting.Builder()
        .name("smash-attack")
        .description("Will always perform smash attacks when using a mace.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Double> extraHeight = sgMace.add(new DoubleSetting.Builder()
        .name("additional-height")
        .description("The amount of additional height to spoof. More height means more damage.")
        .defaultValue(0.0)
        .min(0)
        .sliderRange(0, 100)
        .visible(mace::get)
        .build()
    );

    private ServerboundAttackPacket attackPacket;
    private ServerboundPunchPacket swingPacket;
    private LocalPlayer queuedPlayer;
    private ClientLevel queuedWorld;
    private Entity queuedTarget;
    private ItemStack queuedWeapon;
    private int queuedSlot;
    private int queuedTicks;
    private Mode queuedMode;
    private boolean replaying;
    private boolean sendPackets;
    private int sendTimer;
    private double lastY;
    private boolean waitingForPeak;

    public Criticals() {
        super(Categories.Combat, "criticals", "Performs critical attacks when you hit your target.");
    }

    @Override
    public void onActivate() {
        clearPending();
    }

    @Override
    public void onDeactivate() {
        clearPending();
    }

    @EventHandler
    private void onLeave(GameLeftEvent event) {
        clearPending();
    }

    private void clearPending() {
        attackPacket = null;
        swingPacket = null;
        queuedPlayer = null;
        queuedWorld = null;
        queuedTarget = null;
        queuedWeapon = null;
        queuedMode = null;
        queuedTicks = 0;
        sendPackets = false;
        sendTimer = 0;
        lastY = 0;
        waitingForPeak = false;
    }

    @EventHandler
    private void onSendPacket(PacketEvent.Send event) {
        if (replaying || event.isCancelled() || mc.player == null || mc.level == null) return;
        if (event.packet instanceof ServerboundAttackPacket(int entityId)) {
            if (sendPackets) {
                // One jump owns one attack; another click must not replace its context.
                event.cancel();
                return;
            }
            if (mace.get() && mc.player.getMainHandItem().getItem() instanceof MaceItem) {
                if (mc.player.isFallFlying()) return;

                sendPacket(0);
                sendPacket(1.501 + extraHeight.get());
                sendPacket(0);
            } else {
                if (skipCrit()) return;

                Entity entity = mc.level.getEntity(entityId);

                if (!(entity instanceof LivingEntity) || (entity != Modules.get().get(KillAura.class).getTarget() && ka.get()))
                    return;

                switch (mode.get()) {
                    case Packet -> {
                        sendPacket(0.0625);
                        sendPacket(0);
                    }
                    case UpdatedNCP -> {
                        sendPacket(0.0000008);
                        sendPacket(0);
                    }
                    case OldNCP -> {
                        sendPacket(0.11);
                        sendPacket(0.1100013579);
                        sendPacket(0.0000013579);
                    }
                    case Jump, MiniJump -> {
                        if (!sendPackets) {
                            sendPackets = true;
                            attackPacket = (ServerboundAttackPacket) event.packet;
                            queuedPlayer = mc.player;
                            queuedWorld = mc.level;
                            queuedTarget = entity;
                            queuedWeapon = mc.player.getMainHandItem().copy();
                            queuedSlot = mc.player.getInventory().getSelectedSlot();
                            queuedMode = mode.get();
                            queuedTicks = 0;

                            if (mode.get() == Mode.Jump) {
                                mc.player.jumpFromGround();
                                waitingForPeak = true;
                                lastY = mc.player.getY();
                            } else {
                                ((IVec3) mc.player.getDeltaMovement()).client33$setY(0.25);
                                sendTimer = 4;
                            }
                            event.cancel();
                        }
                    }
                }
            }
        } else if (event.packet instanceof ServerboundPunchPacket punch && mode.get() != Mode.Packet) {
            if (sendPackets) {
                if (swingPacket == null) swingPacket = punch;
                event.cancel();
            }
        }
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (sendPackets) {
            if (++queuedTicks > 30 || mc.player != queuedPlayer || mc.level != queuedWorld
                || queuedTarget == null || !queuedTarget.isAlive() || queuedTarget.isRemoved()
                || mc.level.getEntity(queuedTarget.getId()) != queuedTarget || queuedPlayer.isDeadOrDying()
                || mode.get() != queuedMode || queuedPlayer.getInventory().getSelectedSlot() != queuedSlot
                || !ItemStack.isSameItemSameComponents(queuedWeapon, queuedPlayer.getMainHandItem())) {
                clearPending();
                return;
            }
            if (mode.get() == Mode.Jump && waitingForPeak) {
                double currentY = mc.player.getY();
                if (currentY <= lastY) {
                    waitingForPeak = false;
                    sendTimer = 0; // Attack on next tick after reaching peak
                }
                lastY = currentY;
                return;
            }

            if (sendTimer <= 0) {
                if (attackPacket == null || swingPacket == null) {
                    clearPending();
                    return;
                }
                var attack = attackPacket;
                var swing = swingPacket;
                var connection = queuedPlayer.connection;
                clearPending();
                replaying = true;
                try {
                    connection.send(attack);
                    if (mc.player != null && mc.player.connection == connection) connection.send(swing);
                } finally {
                    replaying = false;
                }
            } else {
                sendTimer--;
            }
        }
    }

    private void sendPacket(double height) {
        double x = mc.player.getX();
        double y = mc.player.getY();
        double z = mc.player.getZ();

        ServerboundMovePlayerPacket packet = new ServerboundMovePlayerPacket.Pos(x, y + height, z, false, false);
        ((IServerboundMovePlayerPacket) packet).client33$setTag(1337);
        mc.player.connection.send(packet);
    }

    private boolean skipCrit() {
        if (EntityUtils.isInCobweb(mc.player) && (mode.get() == Mode.Jump || mode.get() == Mode.MiniJump))
            return true;

        return !mc.player.onGround() || mc.player.isInWater() || mc.player.isInLava() || mc.player.onClimbable();
    }

    @Override
    public String getInfoString() {
        return mode.get().name();
    }

    public enum Mode {
        None,
        Packet,
        UpdatedNCP,
        OldNCP,
        Jump,
        MiniJump
    }
}
