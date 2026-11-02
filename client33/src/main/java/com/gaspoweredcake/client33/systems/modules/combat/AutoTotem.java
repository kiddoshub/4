
package com.gaspoweredcake.client33.systems.modules.combat;

import com.gaspoweredcake.client33.events.packets.PacketEvent;
import com.gaspoweredcake.client33.events.world.TickEvent;
import com.gaspoweredcake.client33.events.game.GameLeftEvent;
import com.gaspoweredcake.client33.utils.misc.TickTimer;
import com.gaspoweredcake.client33.settings.*;
import com.gaspoweredcake.client33.systems.modules.Categories;
import com.gaspoweredcake.client33.systems.modules.Module;
import com.gaspoweredcake.client33.utils.player.FindItemResult;
import com.gaspoweredcake.client33.utils.player.InvUtils;
import com.gaspoweredcake.client33.utils.player.PlayerUtils;
import meteordevelopment.orbit.EventHandler;
import meteordevelopment.orbit.EventPriority;
import net.minecraft.network.protocol.game.ClientboundEntityEventPacket;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityEvent;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Items;

public class AutoTotem extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Mode> mode = sgGeneral.add(new EnumSetting.Builder<Mode>()
        .name("mode")
        .description("Determines when to hold a totem, strict will always hold.")
        .defaultValue(Mode.Smart)
        .build()
    );

    private final Setting<Integer> delay = sgGeneral.add(new IntSetting.Builder()
        .name("delay")
        .description("The ticks between slot movements.")
        .defaultValue(0)
        .min(0)
        .build()
    );

    private final Setting<Integer> health = sgGeneral.add(new IntSetting.Builder()
        .name("health")
        .description("The health to hold a totem at.")
        .defaultValue(10)
        .range(0, 36)
        .sliderMax(36)
        .visible(() -> mode.get() == Mode.Smart)
        .build()
    );

    private final Setting<Boolean> elytra = sgGeneral.add(new BoolSetting.Builder()
        .name("elytra")
        .description("Will always hold a totem when flying with elytra.")
        .defaultValue(true)
        .visible(() -> mode.get() == Mode.Smart)
        .build()
    );

    private final Setting<Boolean> fall = sgGeneral.add(new BoolSetting.Builder()
        .name("fall")
        .description("Will hold a totem when fall damage could kill you.")
        .defaultValue(true)
        .visible(() -> mode.get() == Mode.Smart)
        .build()
    );

    private final Setting<Boolean> explosion = sgGeneral.add(new BoolSetting.Builder()
        .name("explosion")
        .description("Will hold a totem when explosion damage could kill you.")
        .defaultValue(true)
        .visible(() -> mode.get() == Mode.Smart)
        .build()
    );

    public boolean locked;
    private int totems;
    private final TickTimer moveTimer = new TickTimer();

    public AutoTotem() {
        super(Categories.Combat, "auto-totem", "Automatically equips a totem in your offhand.");
    }

    @Override
    public void onActivate() {
        reset();
    }

    @Override
    public void onDeactivate() {
        reset();
    }

    @EventHandler
    private void onGameLeft(GameLeftEvent event) {
        reset();
    }

    private void reset() {
        locked = false;
        totems = 0;
        moveTimer.reset();
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    private void onTick(TickEvent.Pre event) {
        moveTimer.advance();
        if (mc.player == null || mc.gameMode == null || !mc.player.isAlive() || mc.player.isSpectator()) {
            locked = false;
            return;
        }
        FindItemResult result = InvUtils.find(Items.TOTEM_OF_UNDYING);
        totems = result.count();
        if (totems == 0) {
            locked = false;
            return;
        }

        // Compute protection every tick; inventory delay must not leave the lock stale.
        locked = mode.get() == Mode.Strict;
        if (!locked) {
            double effectiveHealth = mc.player.getHealth() + mc.player.getAbsorptionAmount();
            boolean flying = elytra.get() && mc.player.getItemBySlot(EquipmentSlot.CHEST).is(Items.ELYTRA) && mc.player.isFallFlying();
            locked = flying || effectiveHealth <= health.get()
                || effectiveHealth - PlayerUtils.possibleHealthReductions(explosion.get(), fall.get()) <= health.get();
        }

        if (!locked || mc.player.getOffhandItem().is(Items.TOTEM_OF_UNDYING) || !moveTimer.ready()) return;
        // Never repurpose a container's slot IDs or an item held by the cursor.
        if (mc.player.containerMenu != mc.player.inventoryMenu || !mc.player.containerMenu.getCarried().isEmpty()) return;
        if (!result.found()) return;
        InvUtils.move().from(result.slot()).toOffhand();
        moveTimer.waitTicks(delay.get());
    }

    @EventHandler(priority = EventPriority.HIGH)
    private void onReceivePacket(PacketEvent.Receive event) {
        if (!(event.packet instanceof ClientboundEntityEventPacket packet) || mc.level == null) return;
        if (packet.getEventId() != EntityEvent.PROTECTED_FROM_DEATH) return;
        Entity entity = packet.getEntity(mc.level);
        if (entity == mc.player) moveTimer.waitTicks(delay.get());
    }

    public boolean isLocked() {
        return isActive() && locked;
    }

    @Override
    public String getInfoString() {
        return String.valueOf(totems);
    }

    public enum Mode {
        Smart,
        Strict
    }
}
