package com.gaspoweredcake.client33;

import com.gaspoweredcake.client33.utils.render.CachedStorageIndex;
import com.gaspoweredcake.client33.utils.render.LiveChunkVisibility;
import java.util.List;

public final class CachedStorageRegression {
    public static void main(String[] args) { run(); }
    public static void run() {
        var index = new CachedStorageIndex();
        var chest = new CachedStorageIndex.Entry(4800, 64, -4800, "minecraft:chest");
        var furnace = new CachedStorageIndex.Entry(4801, -20, -4800, "minecraft:furnace");
        index.replace(300, -300, List.of(chest, chest, furnace,
            new CachedStorageIndex.Entry(0, 64, 0, "minecraft:chest")));
        check(index.matching(_ -> true, (_, _) -> false).size() == 2, "Duplicate and wrong-chunk records must not create false cache storage");
        check(index.matching("minecraft:chest"::equals, (_, _) -> false).equals(List.of(chest)), "Storage filters apply to saved records");
        check(index.matching(_ -> true, (x, z) -> x == 300 && z == -300).isEmpty(), "Live chunks supersede cached data");
        index.replace(300, -300, List.of());
        check(index.matching(_ -> true, (_, _) -> false).isEmpty(), "Fresh empty cache replaces former storage");
        for (int x = -300; x <= 300; x++) for (int z = -300; z <= 300; z++) {
            index.replace(x, z, List.of(new CachedStorageIndex.Entry(x * 16, 64, z * 16, "minecraft:chest")));
        }
        check(index.matching(_ -> true, (_, _) -> false).size() == 601 * 601,
            "Every chunk in a 300-chunk square radius remains indexed without a distance or count quota");
        index.remove(300, -300);
        check(index.matching(_ -> true, (_, _) -> false).size() == 601 * 601 - 1, "Fresh server observations invalidate the corresponding cache");
        index.clear();
        check(index.matching(_ -> true, (_, _) -> false).isEmpty(), "World changes clear cached coordinates");
        var visibility = new LiveChunkVisibility();
        int[] calls = {0};
        LiveChunkVisibility.Lookup lookup = (x, z) -> { calls[0]++; return x == -2 && z == 3; };
        for (int i = 0; i < 10_000; i++) {
            check(visibility.isLive(-2, 3, lookup), "Live chunk suppresses stale markers");
            check(!visibility.isLive(3, -2, lookup), "A different signed chunk remains cached");
        }
        check(calls[0] == 2, "Repeated markers query each chunk once per frame");
        visibility.beginFrame();
        check(!visibility.isLive(-2, 3, (x, z) -> false), "Liveness is refreshed next frame");
        System.out.println("300-chunk cached storage regressions passed (361201 indexed chunks).");
    }
    private static void check(boolean value, String message) { if (!value) throw new AssertionError(message); }
}
