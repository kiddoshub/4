package com.gaspoweredcake.client33.utils.storage;

import com.gaspoweredcake.client33.utils.combat.CombatTelemetry;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.protocol.game.ClientboundContainerSetContentPacket;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.vehicle.ContainerEntity;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.properties.ChestType;

import java.util.ArrayList;
import java.util.List;

import static com.gaspoweredcake.client33.Client33.mc;

/** Receives applied inventory packets on the client thread. No inventory requests are sent. */
public final class ContainerObservations {
    private record BlockKey(BlockPos pos, Object partner) {}
    private record Target(Object key, Object source, Object partner, int capacity) {}
    private final ContainerMemory<Object> memory = new ContainerMemory<>(256);
    private Target pending, active;
    private long attemptedAt;
    private AbstractContainerMenu menu;
    private Object world;
    private boolean serverObserved;
    private final List<Integer> indices = new ArrayList<>();
    private final List<ContainerSnapshot.Stack> slots = new ArrayList<>();

    public void clear() { memory.clear(); pending = active = null; menu = null; indices.clear(); slots.clear(); world = mc.level; }
    public void attempt(Object source) {
        if (world != mc.level) clear();
        // A new interaction invalidates an earlier unacknowledged attempt.
        pending = target(source);
        attemptedAt = CombatTelemetry.now();
    }
    public void tick(long ttl) {
        if (world != mc.level) clear();
        long now = CombatTelemetry.now();
        memory.expire(now, ttl);
        if (pending != null && now - attemptedAt > 2000) pending = null;
        if (menu != null && valid(active) && slots.size() == indices.size()) {
            // The server may acknowledge predicted clicks without echoing matching slot contents.
            List<ContainerSnapshot.Stack> view = new ArrayList<>();
            for (int index : indices) view.add(describe(menu.getSlot(index).getItem()));
            if (!view.equals(slots)) {
                slots.clear(); slots.addAll(view); serverObserved = false; remember();
            }
        }
        if (menu != null && (mc.player == null || mc.player.containerMenu != menu || !valid(active))) {
            menu = null; active = null; slots.clear(); indices.clear();
        }
    }
    public void full(ClientboundContainerSetContentPacket packet) {
        if (world != mc.level || mc.player == null) return;
        var current = mc.player.containerMenu;
        if (packet.containerId() != current.containerId || current == mc.player.inventoryMenu) return;
        if (current != menu) {
            if (pending == null || CombatTelemetry.now() - attemptedAt > 2000 || !valid(pending) || !compatible(pending, current)) return;
            if (!(mc.gui.screen() instanceof AbstractContainerScreen<?>)) return;
            List<Integer> found = new ArrayList<>();
            for (int i = 0; i < current.slots.size(); i++) if (current.slots.get(i).container != mc.player.getInventory()) found.add(i);
            if (found.size() != pending.capacity || found.size() > 108 || packet.items().size() != current.slots.size()) return;
            active = pending; pending = null; menu = current;
            indices.clear(); indices.addAll(found);
        }
        if (!valid(active) || packet.items().size() != current.slots.size()) return;
        slots.clear(); serverObserved = true;
        for (int index : indices) slots.add(describe(packet.items().get(index)));
        remember();
    }
    public void slot(ClientboundContainerSetSlotPacket packet) {
        if (mc.player == null || menu == null || menu != mc.player.containerMenu || packet.getContainerId() != menu.containerId || !valid(active)) return;
        int index = indices.indexOf(packet.getSlot());
        if (index < 0 || index >= slots.size()) return;
        slots.set(index, describe(packet.getItem()));
        remember();
    }
    private void remember() { memory.put(active.key, active.source, ContainerSnapshot.summarize(CombatTelemetry.now(), slots, serverObserved)); }
    public ContainerSnapshot snapshot(Object source, long ttl) {
        Target target = target(source);
        if (target == null || !valid(target)) return null;
        ContainerSnapshot snapshot = memory.get(target.key, target.source, CombatTelemetry.now(), ttl);
        return snapshot != null && snapshot.slots() == target.capacity ? snapshot : null;
    }
    public boolean live(Object source) {
        Target candidate = target(source);
        return candidate != null && active != null && candidate.source == active.source && valid(active)
            && mc.player != null && mc.player.containerMenu == menu;
    }
    public static int capacity(Object source) { Target target = target(source); return target == null ? 0 : target.capacity; }
    private static ContainerSnapshot.Stack describe(ItemStack stack) {
        if (stack.isEmpty()) return null;
        int durability = stack.isDamageableItem() ? (int) (100L * Math.max(0, stack.getMaxDamage() - stack.getDamageValue()) / stack.getMaxDamage()) : -1;
        return new ContainerSnapshot.Stack(BuiltInRegistries.ITEM.getKey(stack.getItem()).toString(), stack.getHoverName().getString(),
            stack.getCount(), stack.getMaxStackSize(), !stack.getEnchantments().isEmpty(), durability);
    }
    private static Target target(Object source) {
        if (source instanceof BlockEntity block) {
            block = canonical(block);
            Object partner = partner(block);
            int capacity = block instanceof EnderChestBlockEntity ? 27 : block instanceof Container container ? container.getContainerSize() : 0;
            if (partner instanceof Container container) capacity += container.getContainerSize();
            return new Target(new BlockKey(block.getBlockPos(), partner), block, partner, capacity);
        }
        if (source instanceof Entity entity && source instanceof ContainerEntity container)
            return new Target(entity.getUUID(), entity, null, container.getContainerSize());
        return null;
    }
    private static boolean valid(Target target) {
        if (target == null || mc.level == null) return false;
        if (target.source instanceof BlockEntity block) {
            BlockPos pos = block.getBlockPos();
            return mc.level.hasChunk(pos.getX() >> 4, pos.getZ() >> 4) && mc.level.getBlockEntity(pos) == block && partner(block) == target.partner;
        }
        return target.source instanceof Entity entity && entity.isAlive() && mc.level.getEntity(entity.getId()) == entity;
    }
    public static BlockEntity canonical(BlockEntity block) {
        BlockEntity partner = partner(block);
        return partner != null && block.getBlockState().getValue(ChestBlock.TYPE) == ChestType.RIGHT ? partner : block;
    }
    public static BlockEntity partner(BlockEntity block) {
        var state = block.getBlockState();
        if (!(block instanceof ChestBlockEntity) || !(state.getBlock() instanceof ChestBlock) || state.getValue(ChestBlock.TYPE) == ChestType.SINGLE || mc.level == null) return null;
        var type = state.getValue(ChestBlock.TYPE);
        var facing = state.getValue(ChestBlock.FACING);
        BlockPos pos = block.getBlockPos().relative(type == ChestType.LEFT ? facing.getClockWise() : facing.getCounterClockWise());
        if (!mc.level.hasChunk(pos.getX() >> 4, pos.getZ() >> 4)) return null;
        BlockEntity other = mc.level.getBlockEntity(pos);
        if (!(other instanceof ChestBlockEntity) || other.getType() != block.getType()) return null;
        var otherState = other.getBlockState();
        return otherState.getBlock() == state.getBlock() && otherState.getValue(ChestBlock.FACING) == facing
            && otherState.getValue(ChestBlock.TYPE) == (type == ChestType.LEFT ? ChestType.RIGHT : ChestType.LEFT) ? other : null;
    }
    private static boolean compatible(Target target, AbstractContainerMenu menu) {
        Object source = target.source;
        if (source instanceof ChestBlockEntity || source instanceof BarrelBlockEntity || source instanceof EnderChestBlockEntity)
            return menu instanceof ChestMenu chest && chest.getRowCount() * 9 == target.capacity;
        if (source instanceof ContainerEntity) return (menu instanceof ChestMenu chest && chest.getRowCount() * 9 == target.capacity) || (target.capacity == 5 && menu instanceof HopperMenu);
        if (source instanceof ShulkerBoxBlockEntity) return menu instanceof ShulkerBoxMenu;
        if (source instanceof HopperBlockEntity) return menu instanceof HopperMenu;
        if (source instanceof BlastFurnaceBlockEntity) return menu instanceof BlastFurnaceMenu;
        if (source instanceof SmokerBlockEntity) return menu instanceof SmokerMenu;
        if (source instanceof FurnaceBlockEntity) return menu instanceof FurnaceMenu;
        if (source instanceof BrewingStandBlockEntity) return menu instanceof BrewingStandMenu;
        if (source instanceof DispenserBlockEntity) return menu instanceof DispenserMenu;
        return source instanceof CrafterBlockEntity && menu instanceof CrafterMenu;
    }
}
