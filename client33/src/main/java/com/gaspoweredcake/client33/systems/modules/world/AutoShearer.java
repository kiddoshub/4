
package com.gaspoweredcake.client33.systems.modules.world;

import com.gaspoweredcake.client33.events.world.TickEvent;
import com.gaspoweredcake.client33.settings.BoolSetting;
import com.gaspoweredcake.client33.settings.DoubleSetting;
import com.gaspoweredcake.client33.settings.Setting;
import com.gaspoweredcake.client33.settings.SettingGroup;
import com.gaspoweredcake.client33.systems.modules.Categories;
import com.gaspoweredcake.client33.systems.modules.Module;
import com.gaspoweredcake.client33.utils.player.FindItemResult;
import com.gaspoweredcake.client33.utils.player.InvUtils;
import com.gaspoweredcake.client33.utils.player.PlayerUtils;
import com.gaspoweredcake.client33.utils.player.Rotations;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.sheep.Sheep;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.EntityHitResult;

public class AutoShearer extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Double> distance = sgGeneral.add(new DoubleSetting.Builder()
        .name("distance")
        .description("The maximum distance the sheep have to be to be sheared.")
        .min(0.0)
        .defaultValue(5.0)
        .build()
    );

    private final Setting<Boolean> antiBreak = sgGeneral.add(new BoolSetting.Builder()
        .name("anti-break")
        .description("Prevents shears from being broken.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> rotate = sgGeneral.add(new BoolSetting.Builder()
        .name("rotate")
        .description("Automatically faces towards the animal being sheared.")
        .defaultValue(true)
        .build()
    );

    private long generation;

    public AutoShearer() {
        super(Categories.World, "auto-shearer", "Automatically shears sheep.");
    }

    @Override
    public void onDeactivate() {
        generation++;
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        long tickGeneration = ++generation;
        var owner = mc.player;
        var world = mc.level;
        if (owner == null || world == null || mc.gameMode == null) return;

        for (Entity entity : world.entitiesForRendering()) {
            if (!(entity instanceof Sheep sheep) || sheep.isSheared() || sheep.isBaby() || !PlayerUtils.isWithin(entity, distance.get())) continue;
            FindItemResult shears = InvUtils.findInHotbar(this::usableShears);
            if (!shears.found()) return;
            boolean offhand = shears.isOffhand();
            int selected = owner.getInventory().getSelectedSlot();
            ItemStack expected = (offhand ? owner.getOffhandItem() : owner.getInventory().getItem(shears.slot())).copy();
            Runnable action = () -> {
                if (!isActive() || generation != tickGeneration || mc.player != owner || mc.level != world || mc.gameMode == null
                    || !owner.isAlive() || sheep.level() != world || sheep.isRemoved() || !sheep.isAlive()
                    || sheep.isSheared() || sheep.isBaby() || !PlayerUtils.isWithin(sheep, distance.get())
                    || owner.getInventory().getSelectedSlot() != selected) return;
                ItemStack current = offhand ? owner.getOffhandItem() : owner.getInventory().getItem(shears.slot());
                if (current.isEmpty() || !ItemStack.isSameItemSameComponents(expected, current) || !usableShears(current)) return;

                boolean swapped = !offhand && shears.slot() != selected;
                if (swapped && !InvUtils.swap(shears.slot(), true)) return;
                try {
                    EntityHitResult location = new EntityHitResult(sheep, sheep.getBoundingBox().getCenter());
                    mc.gameMode.interact(owner, sheep, location, offhand ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND);
                } finally {
                    if (swapped) {
                        if (mc.player == owner && owner.getInventory().getSelectedSlot() != shears.slot())
                            InvUtils.swap(owner.getInventory().getSelectedSlot(), false);
                        else InvUtils.swapBack();
                    }
                }
            };
            if (rotate.get()) Rotations.rotate(Rotations.getYaw(sheep), Rotations.getPitch(sheep), -100, action);
            else action.run();
            return;
        }
    }

    private boolean usableShears(ItemStack stack) {
        return stack.getItem() == Items.SHEARS && (!antiBreak.get() || stack.getDamageValue() < stack.getMaxDamage() - 1);
    }
}
