package com.gaspoweredcake.client33;

import com.gaspoweredcake.client33.utils.intel.SlotDataStore;
import java.util.List;
import java.util.Map;

public final class SlotDataStoreRegression {
    public static void run() {
        var store = new SlotDataStore(2);
        byte[] raw = {1,2};
        var first = new SlotDataStore.Slot(4, 0, 1, "minecraft:iron_ingot", 3, 0, "", List.of("Lore"), Map.of(), raw, 100, false);
        store.put(first); raw[0] = 9;
        check(store.get(4,0).orElseThrow().nbt()[0] == 1, "Input data is detached");
        byte[] copy = store.get(4,0).orElseThrow().nbt(); copy[0] = 8;
        check(store.get(4,0).orElseThrow().nbt()[0] == 1, "Output data is detached");
        store.replace(4, List.of(new SlotDataStore.Slot(4,1,2,"",0,0,"",List.of(),Map.of(),new byte[0],110,false)));
        check(store.get(4,0).isEmpty() && store.get(4,1).isPresent(), "Full content replaces stale slots");
        store.put(new SlotDataStore.Slot(-2,1,3,"minecraft:stone",1,0,"",List.of(),Map.of(),new byte[0],120,false));
        store.put(new SlotDataStore.Slot(9,1,4,"minecraft:chest",1,0,"",List.of(),Map.of(),new byte[0],130,false));
        check(store.get(4,1).isEmpty() && store.get(-2,1).isPresent(), "Bounded cache retains independent container IDs");
        store.clear(); check(store.snapshot().isEmpty(), "Session reset clears entries");
        System.out.println("Slot data store regressions passed.");
    }
    private static void check(boolean result, String message) { if (!result) throw new AssertionError(message); }
}
