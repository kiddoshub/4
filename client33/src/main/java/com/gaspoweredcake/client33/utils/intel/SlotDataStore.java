package com.gaspoweredcake.client33.utils.intel;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Bounded session cache of the inventory data actually sent by the server. */
public final class SlotDataStore {
    public record Slot(int container, int slot, int state, String itemId, int count, int damage,
                       String customName, List<String> lore, Map<String, Integer> enchantments,
                       byte[] nbt, long observedNanos, boolean truncated) {
        public Slot { lore = List.copyOf(lore); enchantments = Map.copyOf(enchantments); nbt = nbt.clone(); }
        @Override public byte[] nbt() { return nbt.clone(); }
    }
    private record Key(int container, int slot) {}
    private final int limit;
    private final LinkedHashMap<Key, Slot> slots = new LinkedHashMap<>();
    public SlotDataStore(int limit) { if (limit < 1) throw new IllegalArgumentException("Invalid limit"); this.limit = limit; }
    public synchronized void replace(int container, List<Slot> values) {
        slots.keySet().removeIf(key -> key.container == container);
        for (Slot value : values) put(value);
    }
    public synchronized void put(Slot value) {
        Key key = new Key(value.container, value.slot); slots.remove(key); slots.put(key, value);
        while (slots.size() > limit) slots.remove(slots.keySet().iterator().next());
    }
    public synchronized Optional<Slot> get(int container, int slot) { return Optional.ofNullable(slots.get(new Key(container, slot))); }
    public synchronized List<Slot> snapshot() { return List.copyOf(slots.values()); }
    public synchronized void clear() { slots.clear(); }
}
