
package com.gaspoweredcake.client33.systems.modules.combat;

import com.gaspoweredcake.client33.events.world.TickEvent;
import com.gaspoweredcake.client33.settings.*;
import com.gaspoweredcake.client33.systems.modules.Categories;
import com.gaspoweredcake.client33.systems.modules.Module;
import com.gaspoweredcake.client33.utils.Utils;
import com.gaspoweredcake.client33.utils.misc.TickTimer;
import com.gaspoweredcake.client33.utils.player.FindItemResult;
import com.gaspoweredcake.client33.utils.player.InvUtils;
import com.gaspoweredcake.client33.utils.player.Rotations;
import com.gaspoweredcake.client33.utils.player.SlotUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;

public class AutoEXP extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Mode> mode = sgGeneral.add(new EnumSetting.Builder<Mode>()
        .name("mode")
        .description("Which items to repair.")
        .defaultValue(Mode.Both)
        .build()
    );

    private final Setting<Boolean> replenish = sgGeneral.add(new BoolSetting.Builder()
        .name("replenish")
        .description("Automatically replenishes exp into a selected hotbar slot.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> onlyGround = sgGeneral.add(new BoolSetting.Builder()
        .name("only-on-ground")
        .description("Only throw when the player is on the ground.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Integer> slot = sgGeneral.add(new IntSetting.Builder()
        .name("exp-slot")
        .description("The slot to replenish exp into.")
        .visible(replenish::get)
        .defaultValue(6)
        .range(1, 9)
        .sliderRange(1, 9)
        .build()
    );

    private final Setting<Integer> minThreshold = sgGeneral.add(new IntSetting.Builder()
        .name("min-threshold")
        .description("The minimum durability percentage that an item needs to fall to, to be repaired.")
        .defaultValue(30)
        .range(1, 100)
        .sliderRange(1, 100)
        .build()
    );

    private final Setting<Integer> maxThreshold = sgGeneral.add(new IntSetting.Builder()
        .name("max-threshold")
        .description("The maximum durability percentage to repair items to.")
        .defaultValue(80)
        .range(1, 100)
        .sliderRange(1, 100)
        .build()
    );

    private final Setting<Integer> throwDelay = sgGeneral.add(new IntSetting.Builder()
        .name("throw-delay")
        .description("Ticks between throws, allowing XP to reach equipment before using more bottles.")
        .defaultValue(4)
        .range(1, 20)
        .sliderRange(1, 20)
        .build()
    );

    private final TickTimer throwTimer = new TickTimer();
    private int repairingI;
    private long generation;

    public AutoEXP() {
        super(Categories.Combat, "auto-exp", "Automatically repairs your armor and tools in pvp.");
    }

    @Override
    public void onActivate() {
        repairingI = -1;
        throwTimer.reset();
        generation++;
    }

    @Override public void onDeactivate() { generation++; repairingI = -1; }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        long actionGeneration = ++generation;
        throwTimer.advance();
        if (!throwTimer.ready()) return;
        if (mc.player.containerMenu != mc.player.inventoryMenu || !mc.player.containerMenu.getCarried().isEmpty() || mc.player.isUsingItem()) return;
        if (onlyGround.get() && !mc.player.onGround()) {
            return;
        }

        if (repairingI == -1) {
            if (mode.get() != Mode.Hands) {
                for (EquipmentSlot slot : EquipmentSlotGroup.ARMOR) {
                    ItemStack stack = mc.player.getItemBySlot(slot);
                    if (needsRepair(stack, minThreshold.get())) {
                        repairingI = SlotUtils.ARMOR_START + slot.getIndex();
                        break;
                    }
                }
            }

            if (mode.get() != Mode.Armor && repairingI == -1) {
                for (InteractionHand hand : InteractionHand.values()) {
                    if (needsRepair(mc.player.getItemInHand(hand), minThreshold.get())) {
                        repairingI = hand == InteractionHand.MAIN_HAND ? mc.player.getInventory().getSelectedSlot() : SlotUtils.OFFHAND;
                        break;
                    }
                }
            }
        }

        if (repairingI != -1) {
            if (!needsRepair(mc.player.getInventory().getItem(repairingI), maxThreshold.get())) {
                repairingI = -1;
                return;
            }

            FindItemResult exp = InvUtils.findInHotbar(Items.EXPERIENCE_BOTTLE);
            if (!exp.found() && replenish.get()) {
                var source = InvUtils.find(stack -> stack.is(Items.EXPERIENCE_BOTTLE), SlotUtils.MAIN_START, SlotUtils.MAIN_END);
                if (!source.found()) return;
                InvUtils.move().from(source.slot()).toHotbar(slot.get() - 1);
                exp = InvUtils.findInHotbar(Items.EXPERIENCE_BOTTLE);
            }
            if (!exp.found()) return;
            var world = mc.level;
            Rotations.rotate(mc.player.getYRot(), 90, () -> {
                if (!isActive() || generation != actionGeneration || mc.level != world || mc.player == null
                    || mc.player.containerMenu != mc.player.inventoryMenu || !mc.player.containerMenu.getCarried().isEmpty() || mc.player.isUsingItem()
                    || (onlyGround.get() && !mc.player.onGround()) || repairingI < 0
                    || !needsRepair(mc.player.getInventory().getItem(repairingI), maxThreshold.get())) return;
                // Re-resolve after replenishment/rotation; the original source slot may now hold another item.
                var current = InvUtils.findInHotbar(Items.EXPERIENCE_BOTTLE);
                if (!current.found()) return;
                var hand = current.getHand();
                if (hand != null) {
                    mc.gameMode.useItem(mc.player, hand);
                    throwTimer.waitTicks(throwDelay.get());
                }
                else if (InvUtils.swap(current.slot(), true)) {
                    try {
                        mc.gameMode.useItem(mc.player, InteractionHand.MAIN_HAND);
                        throwTimer.waitTicks(throwDelay.get());
                    }
                    finally { InvUtils.swapBack(); }
                }
            });
        }
    }

    private boolean needsRepair(ItemStack itemStack, double threshold) {
        if (!itemStack.isDamageableItem() || !Utils.hasEnchantments(itemStack, Enchantments.MENDING)) return false;
        return (itemStack.getMaxDamage() - itemStack.getDamageValue()) / (double) itemStack.getMaxDamage() * 100 <= threshold;
    }

    public enum Mode {
        Armor,
        Hands,
        Both
    }
}
