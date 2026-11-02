package com.gaspoweredcake.client33;

import com.gaspoweredcake.client33.utils.intel.PackedSection;
import java.util.HashMap;
import java.util.Map;

public final class PackedSectionRegression {
    public static void run() {
        for (int bits : new int[]{1, 4, 5, 9, 15, 32}) {
            int size = 4096, per = 64 / bits;
            long mask = (1L << bits) - 1;
            long[] words = new long[(size + per - 1) / per];
            Map<Integer, String> values = new HashMap<>();
            int[] expected = new int[size];
            for (int i = 0; i < size; i++) {
                int id = (int) ((i * 17L + 11) & mask);
                expected[i] = id; values.put(id, "state-" + id);
                words[i / per] |= (id & mask) << ((i % per) * bits);
            }
            PackedSection<String> snapshot = new PackedSection<>(words, bits, size, values);
            for (int i = 0; i < size; i++) check(snapshot.get(i).equals("state-" + expected[i]), "Packed value " + bits + "/" + i);
            check(snapshot.get(7, 13, 11).equals(snapshot.get((13 << 8) | (11 << 4) | 7)), "Section coordinate order");
            words[0] = ~words[0]; values.clear();
            check(snapshot.get(0).equals("state-" + expected[0]), "Snapshot detaches backing data");
            check(snapshot.maybeHas(s -> s.equals("state-" + expected[0])) && !snapshot.maybeHas(s -> s.equals("absent")), "Palette filtering");
        }
        var single = new PackedSection<>(new long[0], 0, 4096, Map.of(0, "air"));
        check(single.get(4095).equals("air"), "Single-value section");
        try { new PackedSection<>(new long[1], 5, 4096, Map.of()); throw new AssertionError("Invalid word count accepted"); }
        catch (IllegalArgumentException expected) { }
        System.out.println("Packed section regressions passed.");
    }
    private static void check(boolean result, String message) { if (!result) throw new AssertionError(message); }
}
