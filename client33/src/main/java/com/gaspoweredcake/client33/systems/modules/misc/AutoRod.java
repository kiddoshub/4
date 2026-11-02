/* Adapted from the legacy client's AutoRod. */
package com.gaspoweredcake.client33.systems.modules.misc;

import com.gaspoweredcake.client33.events.client33.MouseClickEvent;
import com.gaspoweredcake.client33.events.world.TickEvent;
import com.gaspoweredcake.client33.settings.EnumSetting;
import com.gaspoweredcake.client33.settings.IntSetting;
import com.gaspoweredcake.client33.settings.Setting;
import com.gaspoweredcake.client33.settings.SettingGroup;
import com.gaspoweredcake.client33.systems.modules.Categories;
import com.gaspoweredcake.client33.systems.modules.Module;
import com.gaspoweredcake.client33.systems.modules.Modules;
import com.gaspoweredcake.client33.systems.modules.player.AutoEat;
import com.gaspoweredcake.client33.systems.modules.player.AutoGap;
import com.gaspoweredcake.client33.systems.modules.player.MiddleClickExtra;
import com.gaspoweredcake.client33.utils.misc.input.Input;
import com.gaspoweredcake.client33.utils.misc.input.KeyAction;
import com.gaspoweredcake.client33.utils.player.InvUtils;
import com.gaspoweredcake.client33.utils.player.SwingUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

/** Uses a hotbar rod, or an egg/snowball when no rod is available. */
public final class AutoRod extends Module {
    private final SettingGroup general = settings.getDefaultGroup();
    private final Setting<Button> button = general.add(new EnumSetting.Builder<Button>()
        .name("button").description("Mouse button used for the quick action.").defaultValue(Button.Mouse4).build());
    private final Setting<Integer> delay = general.add(new IntSetting.Builder()
        .name("rod-switch-delay").description("Ticks before restoring the previous slot after using a rod.")
        .defaultValue(2).range(0, 20).sliderRange(0, 20).build());
    private LocalPlayer owner;
    private int previous = -1, borrowed = -1, ticks;

    public AutoRod() { super(Categories.Misc, "auto-rod", "Quickly uses a hotbar fishing rod or throwable."); }

    @Override public void onDeactivate() { restore(); }

    @EventHandler
    private void onClick(MouseClickEvent event) {
        if (event.action != KeyAction.Press || event.button() != button.get().code || mc.gui.screen() != null
            || mc.player == null || mc.gameMode == null || owner != null) return;
        if (mc.player.isUsingItem() || Input.isPressed(mc.options.keyUse) || InvUtils.previousSlot != -1
            || Modules.get().get(AutoEat.class).eating || Modules.get().get(AutoGap.class).isEating()) return;
        if (button.get() == Button.Middle && Modules.get().isActive(MiddleClickExtra.class)) return;
        int slot = find(Items.FISHING_ROD);
        if (slot < 0) {
            for (int i = 0; i < 9; i++) {
                var stack = mc.player.getInventory().getItem(i);
                if (stack.is(Items.EGG) || stack.is(Items.SNOWBALL)) { slot = i; break; }
            }
        }
        if (slot < 0) return;
        LocalPlayer player = mc.player;
        Item item = player.getInventory().getItem(slot).getItem();
        owner = player;
        previous = player.getInventory().getSelectedSlot();
        borrowed = slot;
        if (!InvUtils.swap(slot, false)) { clear(); return; }
        event.cancel();
        mc.gameMode.useItem(player, InteractionHand.MAIN_HAND);
        SwingUtils.swing(InteractionHand.MAIN_HAND);
        ticks = item == Items.FISHING_ROD ? delay.get() : 0;
        if (ticks == 0) restore();
    }

    @EventHandler private void onTick(TickEvent.Pre event) {
        if (owner == null) return;
        if (mc.player != owner || mc.gui.screen() != null || owner.getInventory().getSelectedSlot() != borrowed || --ticks <= 0) restore();
    }

    private int find(Item item) {
        for (int slot = 0; slot < 9; slot++) if (mc.player.getInventory().getItem(slot).is(item)) return slot;
        return -1;
    }

    private void restore() {
        if (owner != null && mc.player == owner && previous >= 0 && InvUtils.previousSlot == -1
            && owner.getInventory().getSelectedSlot() == borrowed)
            InvUtils.swap(previous, false);
        clear();
    }

    private void clear() { owner = null; previous = borrowed = -1; ticks = 0; }
    public enum Button {
        Middle(2), Mouse4(3), Mouse5(4);
        final int code;
        Button(int code) { this.code = code; }
    }
}
