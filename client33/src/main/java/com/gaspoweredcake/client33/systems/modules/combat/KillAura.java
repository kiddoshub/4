/*
 * Adapted from Payload's KillAura (https://github.com/vcvvve4/add), GPL-3.0.
 */
package com.gaspoweredcake.client33.systems.modules.combat;

import com.gaspoweredcake.client33.events.entity.player.SendMovementPacketsEvent;
import com.gaspoweredcake.client33.events.packets.PacketEvent;
import com.gaspoweredcake.client33.events.world.TickEvent;
import com.gaspoweredcake.client33.settings.*;
import com.gaspoweredcake.client33.systems.friends.Friends;
import com.gaspoweredcake.client33.systems.modules.Categories;
import com.gaspoweredcake.client33.systems.modules.Module;
import com.gaspoweredcake.client33.systems.modules.Modules;
import com.gaspoweredcake.client33.systems.modules.player.AutoEat;
import com.gaspoweredcake.client33.systems.modules.player.AutoGap;
import com.gaspoweredcake.client33.utils.entity.EntityUtils;
import com.gaspoweredcake.client33.utils.combat.AttackCycle;
import com.gaspoweredcake.client33.utils.combat.MouseAim;
import com.gaspoweredcake.client33.utils.combat.MouseTurnBudget;
import com.gaspoweredcake.client33.systems.modules.render.Freecam;
import com.gaspoweredcake.client33.systems.modules.render.FreeLook;
import com.gaspoweredcake.client33.systems.modules.world.HighwayBuilder;
import com.gaspoweredcake.client33.utils.player.InvUtils;
import com.gaspoweredcake.client33.utils.player.Rotations;
import com.gaspoweredcake.client33.utils.player.SwingUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundAcceptTeleportationPacket;
import net.minecraft.network.protocol.game.ServerboundAttackPacket;
import net.minecraft.network.Connection;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ShulkerBullet;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.Random;

public class KillAura extends Module {
    private final SettingGroup general = settings.getDefaultGroup();
    private final SettingGroup targeting = settings.createGroup("Targeting");
    private final Setting<RotationMode> rotation = general.add(new EnumSetting.Builder<RotationMode>()
        .name("rotation-mode").description("None, immediate visible rotation, silent packet rotation, or bounded mouse-count rotation.")
        .defaultValue(RotationMode.None).build());
    private final Setting<Priority> priority = targeting.add(new EnumSetting.Builder<Priority>()
        .name("priority").description("Select the closest or lowest-health enemy.").defaultValue(Priority.Closest).build());
    private final Setting<SwapMode> swap = general.add(new EnumSetting.Builder<SwapMode>()
        .name("auto-swap").description("Search the hotbar or inventory for a weapon.").defaultValue(SwapMode.Normal).build());
    private final Setting<Double> radius = targeting.add(new DoubleSetting.Builder()
        .name("radius").description("Maximum target distance.").defaultValue(4).range(0.1, 10).sliderRange(0.1, 10).build());
    private final Setting<Double> wallRadius = targeting.add(new DoubleSetting.Builder()
        .name("wall-radius").description("Maximum distance through walls.").defaultValue(0).range(0, 10).sliderRange(0, 10).build());
    private final Setting<Boolean> animals = bool(targeting, "target-animals", false);
    private final Setting<Boolean> monsters = bool(targeting, "target-monsters", true);
    private final Setting<Boolean> players = bool(targeting, "target-players", true);
    private final Setting<Boolean> friends = bool(targeting, "target-friends", false);
    private final Setting<Boolean> legit = bool(general, "legit", false);
    private final Setting<Boolean> weaponOnly = bool(general, "weapon-only", false);
    private final Setting<Boolean> customDelay = bool(general, "use-custom-delay", false);
    private final Setting<Double> delay = general.add(new DoubleSetting.Builder()
        .name("custom-delay-seconds").description("Seconds between attacks when custom delay is enabled.")
        .defaultValue(1).range(0, 2.5).sliderRange(0, 2.5).build());
    private final Setting<Boolean> superMace = bool(general, "super-mace", false);
    private final Setting<Boolean> teleportAckExperiment = general.add(new BoolSetting.Builder()
        .name("teleport-confirm-experiment")
        .description("Once per activation, briefly hold a teleport confirmation until an attack or timeout. Server reach rules still apply.")
        .defaultValue(false)
        .onChanged(enabled -> { releaseTeleportConfirmation(); synchronized (this) { teleportArmed = enabled && isActive(); } })
        .build());
    private final Setting<Integer> rotationPriority = general.add(new IntSetting.Builder()
        .name("rotation-priority").description("Priority of Payload packet rotations.").defaultValue(25).range(0, 100).sliderRange(0, 100).build());
    private final Setting<Double> rotationStep = general.add(new DoubleSetting.Builder()
        .name("rotation-steps").description("Fraction of the maximum yaw and pitch turn per tick in Payload mode.")
        .defaultValue(0.6).range(0.01, 1).sliderRange(0.01, 1).build());

    private final Setting<Double> mouseTurnSpeed = general.add(new DoubleSetting.Builder()
        .name("mouse-turn-speed").description("Maximum combined simulated turn in degrees per second.")
        .defaultValue(180).range(30, 180).sliderRange(30, 180).visible(() -> rotation.get() == RotationMode.Mouse).build());

    private final MouseAim mouseAim = new MouseAim(new Random());
    private final MouseTurnBudget mouseBudget = new MouseTurnBudget();
    private Entity mouseTarget;
    private Runnable pendingMouseHit;
    private long mouseGeneration = -1;
    private float expectedYaw, expectedPitch;
    private boolean expectedMouseAngles;
    private Entity target;
    private long lastAttack;
    private final AttackCycle cycle = new AttackCycle();
    private long generation;
    private float packetYaw, packetPitch;
    private boolean teleportArmed;
    private Connection teleportConnection;
    private ServerboundAcceptTeleportationPacket heldTeleport;
    private long teleportDeadline;
    public boolean attacking;

    public KillAura() {
        super(Categories.Combat, "kill-aura", "Payload combat targeting, rotations, weapon swapping and attack timing.");
    }

    private Setting<Boolean> bool(SettingGroup group, String name, boolean value) {
        return group.add(new BoolSetting.Builder().name(name).description(name.replace('-', ' ') + ".").defaultValue(value).build());
    }

    @Override
    public void onActivate() {
        synchronized (this) { teleportArmed = teleportAckExperiment.get(); }
        lastAttack = System.nanoTime();
        target = null;
        attacking = false;
        cycle.invalidate();
        resetMouseAim();
        if (mc.player != null) { packetYaw = mc.player.getYRot(); packetPitch = mc.player.getXRot(); }
    }

    @Override
    public void onDeactivate() {
        releaseTeleportConfirmation();
        synchronized (this) { teleportArmed = false; }
        target = null;
        attacking = false;
        cycle.invalidate();
        resetMouseAim();
    }

    private void resetMouseAim() {
        mouseAim.reset(); mouseBudget.reset(); mouseTarget = null; pendingMouseHit = null;
        mouseGeneration = -1; expectedMouseAngles = false;
    }

    private boolean canOperate() {
        return isActive() && mc.player != null && mc.level != null && mc.gameMode != null
            && mc.player.isAlive() && !mc.player.isSpectator()
            && !Modules.get().get(AutoEat.class).eating && !Modules.get().get(AutoGap.class).isEating();
    }

    @EventHandler
    private synchronized void onTeleportConfirmation(PacketEvent.Send event) {
        if (!(event.packet instanceof ServerboundAcceptTeleportationPacket packet)
            || (!teleportArmed && heldTeleport == null)) return;
        if (heldTeleport != null && event.connection != teleportConnection) return;
        // A newer correction supersedes the earlier teleport ID on the same connection.
        if (heldTeleport == null) teleportDeadline = System.nanoTime() + 500_000_000L;
        heldTeleport = packet;
        teleportConnection = event.connection;
        teleportArmed = false;
        event.cancel();
    }

    @EventHandler
    private void onAttackSent(PacketEvent.Sent event) {
        if (event.packet instanceof ServerboundAttackPacket) {
            synchronized (this) { if (event.connection != teleportConnection) return; }
            releaseTeleportConfirmation();
        }
    }

    private void releaseTeleportConfirmation() {
        Connection connection;
        ServerboundAcceptTeleportationPacket packet;
        synchronized (this) {
            connection = teleportConnection;
            packet = heldTeleport;
            teleportConnection = null;
            heldTeleport = null;
        }
        if (connection != null && connection.isConnected()) connection.send(packet, null, true);
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        boolean expired;
        synchronized (this) { expired = heldTeleport != null && System.nanoTime() - teleportDeadline >= 0; }
        if (expired) releaseTeleportConfirmation();
        generation = cycle.begin();
        pendingMouseHit = null;
        target = null;
        attacking = false;
        if (!canOperate()) return;
        double best = Double.POSITIVE_INFINITY;
        for (Entity candidate : mc.level.getEntities(mc.player, mc.player.getBoundingBox().inflate(radius.get()), this::valid)) {
            double score = priority.get() == Priority.Closest ? mc.player.distanceToSqr(candidate)
                : candidate instanceof LivingEntity living ? living.getHealth() + living.getAbsorptionAmount() : 0;
            if (target == null || score < best || (score == best && candidate.getId() < target.getId())) {
                target = candidate;
                best = score;
            }
        }
        if (target == null) { resetMouseAim(); return; }
        if (!weapon(mc.player.getMainHandItem()) && swap.get() != SwapMode.Off) selectWeapon();
        attacking = !weaponOnly.get() || weapon(mc.player.getMainHandItem());
    }

    @EventHandler(priority = 100)
    private void onMovement(SendMovementPacketsEvent.Pre event) {
        if (!canOperate() || !attacking || target == null || !valid(target)) return;
        Entity selected = target;
        long prepared = generation;
        var player = mc.player;
        var world = mc.level;
        int slot = player.getInventory().getSelectedSlot();
        ItemStack held = player.getMainHandItem().copy();
        Runnable hit = () -> {
            if (!cycle.valid(prepared) || !canOperate() || mc.player != player || mc.level != world
                || target != selected || !valid(selected) || player.getInventory().getSelectedSlot() != slot
                || !ItemStack.isSameItemSameComponents(held, player.getMainHandItem())
                || (weaponOnly.get() && !weapon(player.getMainHandItem())) || !ready()
                || (legit.get() && mc.crosshairPickEntity != selected)) return;
            if (!cycle.claim(prepared, false)) return;
            if (superMace.get() && player.getMainHandItem().is(Items.MACE)) {
                for (int i = 0; i < 20; i++) player.connection.send(new ServerboundMovePlayerPacket.StatusOnly(false, false));
                player.connection.send(new ServerboundMovePlayerPacket.Pos(player.getX(), player.getY() + 200, player.getZ(), false, false));
            }
            mc.gameMode.attack(player, selected);
            SwingUtils.swing(InteractionHand.MAIN_HAND);
            lastAttack = System.nanoTime();
        };
        double yaw = Rotations.getYaw(selected.getEyePosition());
        double pitch = Rotations.getPitch(selected.getEyePosition());
        switch (rotation.get()) {
            case Vanilla -> { player.setYRot((float) yaw); player.setXRot((float) pitch); hit.run(); }
            case Payload -> {
                float step = rotationStep.get().floatValue();
                packetYaw += Mth.clamp(Mth.wrapDegrees((float) yaw - packetYaw), -180 * step, 180 * step);
                packetPitch += Mth.clamp((float) pitch - packetPitch, -90 * step, 90 * step);
                Rotations.rotate(packetYaw, packetPitch, rotationPriority.get(), hit);
            }
            case Mouse -> {
                if (mouseGeneration == prepared) return;
                mouseGeneration = prepared;
                if (mc.gui.screen() != null || mc.getCameraEntity() != player || mc.options.smoothCamera
                    || Modules.get().get(Freecam.class).isActive() || Modules.get().get(FreeLook.class).cameraMode()
                    || Modules.get().get(HighwayBuilder.class).isActive()) {
                    resetMouseAim();
                    return;
                }
                if (mouseTarget != selected || (expectedMouseAngles
                    && Math.hypot(Mth.wrapDegrees(player.getYRot() - expectedYaw), player.getXRot() - expectedPitch) > 4)) {
                    mouseAim.reset();
                    mouseTarget = selected;
                }
                boolean scoped = mc.options.getCameraType().isFirstPerson() && player.isScoping();
                boolean invertX = mc.options.invertMouseX().get(), invertY = mc.options.invertMouseY().get();
                double budget = mouseBudget.advance(System.nanoTime(), mouseTurnSpeed.get());
                if (budget <= 0) return;
                var delta = mouseAim.next(player.getYRot(), player.getXRot(), yaw, pitch,
                    mc.options.sensitivity().get(), scoped, invertX, invertY, budget);
                double turnX = delta.turnX(invertX), turnY = delta.turnY(invertY);
                player.turn(turnX, turnY);
                mouseBudget.consume(Math.hypot(turnX, turnY) * 0.15);
                expectedYaw = player.getYRot(); expectedPitch = player.getXRot(); expectedMouseAngles = true;
                pendingMouseHit = hit;
            }
            case None -> hit.run();
        }
    }

    @EventHandler(priority = -100)
    private void onMovementSent(SendMovementPacketsEvent.Post event) {
        Runnable hit = pendingMouseHit;
        pendingMouseHit = null;
        if (hit == null || rotation.get() != RotationMode.Mouse || !canOperate() || target == null || Rotations.rotating) return;
        var eye = mc.player.getEyePosition();
        var end = eye.add(net.minecraft.world.phys.Vec3.directionFromRotation(mc.player.getXRot(), mc.player.getYRot()).scale(radius.get()));
        var bounds = target.getBoundingBox();
        if (bounds.contains(eye) || bounds.clip(eye, end).isPresent()) hit.run();
    }

    private boolean ready() {
        return customDelay.get() ? System.nanoTime() - lastAttack >= delay.get() * 1_000_000_000L
            : mc.player.getAttackStrengthScale(0) >= 1;
    }

    private boolean valid(Entity entity) {
        if (entity == mc.player || entity.isRemoved() || !entity.isAlive() || entity.level() != mc.level
            || entity instanceof LivingEntity living && living.isDeadOrDying()) return false;
        if (mc.player.distanceToSqr(entity) > radius.get() * radius.get()) return false;
        if (!mc.player.hasLineOfSight(entity) && mc.player.distanceToSqr(entity) > wallRadius.get() * wallRadius.get()) return false;
        if (entity instanceof Player player) return players.get() && !player.isSpectator() && !player.isCreative()
            && (friends.get() || Friends.get().shouldAttack(player));
        if (entity instanceof Enemy || entity instanceof ShulkerBullet) return monsters.get();
        return animals.get() && entity instanceof Animal && mc.player.getVehicle() != entity;
    }

    private boolean weapon(ItemStack stack) {
        return stack.is(ItemTags.SWORDS) || stack.is(ItemTags.AXES) || stack.is(Items.TRIDENT) || stack.is(Items.MACE);
    }

    private void selectWeapon() {
        int end = swap.get() == SwapMode.Normal ? 8 : 35;
        // Payload prefers swords, then tridents, axes, and maces.
        for (int kind = 0; kind < 4; kind++) {
            for (int slot = 0; slot <= end; slot++) {
                ItemStack stack = mc.player.getInventory().getItem(slot);
                boolean match = switch (kind) {
                    case 0 -> stack.is(ItemTags.SWORDS);
                    case 1 -> stack.is(Items.TRIDENT);
                    case 2 -> stack.is(ItemTags.AXES);
                    default -> stack.is(Items.MACE);
                };
                if (!match) continue;
                if (slot < 9) InvUtils.swap(slot, false);
                else if (mc.player.containerMenu == mc.player.inventoryMenu && mc.player.containerMenu.getCarried().isEmpty()) {
                    InvUtils.move().from(slot).toHotbar(mc.player.getInventory().getSelectedSlot());
                }
                return;
            }
        }
    }

    public Entity getTarget() { return isActive() ? target : null; }
    public Entity getAuraEntity() { return getTarget(); }
    @Override
    public String getInfoString() { return target == null ? null : EntityUtils.getName(target); }
    public enum Priority { LowestHP, Closest }
    public enum SwapMode { Off, Normal, Inventory }
    public enum RotationMode { Payload, None, Vanilla, Mouse }
}
