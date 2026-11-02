
package com.gaspoweredcake.client33.systems.modules.movement;

import com.gaspoweredcake.client33.events.packets.PacketEvent;
import com.gaspoweredcake.client33.events.world.TickEvent;
import com.gaspoweredcake.client33.settings.BoolSetting;
import com.gaspoweredcake.client33.settings.EnumSetting;
import com.gaspoweredcake.client33.settings.Setting;
import com.gaspoweredcake.client33.settings.SettingGroup;
import com.gaspoweredcake.client33.systems.modules.Categories;
import com.gaspoweredcake.client33.systems.modules.Module;
import com.gaspoweredcake.client33.systems.modules.Modules;
import com.gaspoweredcake.client33.systems.modules.misc.InventoryTweaks;
import com.gaspoweredcake.client33.systems.modules.misc.ChestStealer;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import com.gaspoweredcake.client33.utils.movement.SprintRules;
import meteordevelopment.orbit.EventHandler;
import meteordevelopment.orbit.EventPriority;
import net.minecraft.network.protocol.game.ServerboundAttackPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;

public class Sprint extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    public enum Mode {
        Strict,
        Rage
    }

    public final Setting<Mode> mode = sgGeneral.add(new EnumSetting.Builder<Mode>()
        .name("sprint-mode")
        .description("What mode of sprinting.")
        .defaultValue(Mode.Strict)
        .build()
    );

    private final Setting<Boolean> keepSprint = sgGeneral.add(new BoolSetting.Builder()
        .name("keep-sprint")
        .description("Whether to keep sprinting after attacking.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> unsprintOnHit = sgGeneral.add(new BoolSetting.Builder()
        .name("unsprint-on-hit")
        .description("Whether to stop sprinting before attacking, to ensure you get crits and sweep attacks.")
        .defaultValue(false)
        .build()
    );

    public final Setting<Boolean> unsprintInWater = sgGeneral.add(new BoolSetting.Builder()
        .name("unsprint-in-water")
        .description("Whether to stop sprinting when in water.")
        .defaultValue(true)
        .visible(() -> mode.get() == Mode.Rage)
        .build()
    );

    private final Setting<Boolean> permaSprint = sgGeneral.add(new BoolSetting.Builder()
        .name("sprint-while-stationary")
        .description("Sprint even when not moving.")
        .defaultValue(false)
        .visible(() -> mode.get() == Mode.Rage)
        .build()
    );

    public Sprint() {
        super(Categories.Movement, "sprint", "Automatically sprints.");
    }

    @EventHandler(priority = EventPriority.HIGH)
    private void onTickMovement(TickEvent.Post event) {
        if (mc.player == null) return;
        mc.player.setSprinting(shouldSprint());
    }

    @EventHandler(priority = EventPriority.HIGH)
    private void onPacketSend(PacketEvent.Send event) {
        if (!unsprintOnHit.get() || mc.player == null || mc.getConnection() == null) return;
        if (!(event.packet instanceof ServerboundAttackPacket)) return;

        mc.getConnection().send(new ServerboundPlayerCommandPacket(mc.player, ServerboundPlayerCommandPacket.Action.STOP_SPRINTING));
        mc.player.setSprinting(false);
    }

    @EventHandler
    private void onPacketSent(PacketEvent.Sent event) {
        if (!unsprintOnHit.get() || !keepSprint.get() || mc.player == null || mc.getConnection() == null) return;
        if (!(event.packet instanceof ServerboundAttackPacket)) return;
        if (!shouldSprint() || mc.player.isSprinting()) return;

        mc.getConnection().send(new ServerboundPlayerCommandPacket(mc.player, ServerboundPlayerCommandPacket.Action.START_SPRINTING));
        mc.player.setSprinting(true);
    }

    public boolean shouldSprint() {
        if (!isActive() || mc.player == null || mc.level == null || !mc.player.isAlive() || mc.player.isSpectator()) return false;
        if (unsprintInWater.get() && mc.player.isInWater()) return false;
        if (mc.gui.screen() != null) {
            GUIMove guiMove = Modules.get().get(GUIMove.class);
            if (guiMove == null || !guiMove.isActive() || !guiMove.sprint.get() || guiMove.skip()) return false;
            if (mc.gui.screen() instanceof AbstractContainerScreen<?>) {
                InventoryTweaks inventory = Modules.get().get(InventoryTweaks.class);
                ChestStealer stealer = Modules.get().get(ChestStealer.class);
                if (inventory != null && inventory.isAutoMoving()
                    || stealer != null && stealer.isActive() && mc.gui.screen() instanceof ContainerScreen) return false;
            }
        }

        float movement = mode.get() == Mode.Rage
            ? (Math.abs(mc.player.zza) + Math.abs(mc.player.xxa))
            : mc.player.zza;

        if (movement <= (mc.player.isUnderWater() ? 1.0E-5F : 0.8)) {
            if (mode.get() == Mode.Strict || !permaSprint.get()) return false;
        }

        if (mode.get() == Mode.Rage) return true;
        var vehicle = mc.player.getVehicle();
        return SprintRules.strictEligible(mc.player.isInShallowWater(), mc.player.isMobilityRestricted(),
            mc.player.isPassenger(), vehicle != null && vehicle.canSprint() && vehicle.isLocalInstanceAuthoritative(),
            mc.player.getFoodData().hasEnoughFood(), !mc.player.horizontalCollision || mc.player.minorHorizontalCollision);
    }

    public boolean rageSprint() {
        return isActive() && mode.get() == Mode.Rage;
    }

    public boolean unsprintInWater() {
        return isActive() && unsprintInWater.get();
    }

    public boolean stopSprinting() {
        return !isActive() || !keepSprint.get();
    }
}
