package com.gaspoweredcake.client33;

import com.gaspoweredcake.client33.utils.world.BlockScanBatch;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

public final class BlockScanRegression {
    public static void run() {
        AtomicInteger failures = new AtomicInteger(), one = new AtomicInteger(), wide = new AtomicInteger(), after = new AtomicInteger();
        var batch = new BlockScanBatch<Integer>(failure -> failures.incrementAndGet());
        Set<String> reads = new HashSet<>();
        batch.register(0, 0, (x, y, z, value) -> { one.incrementAndGet(); return true; });
        batch.register(1, 2, (x, y, z, value) -> { wide.incrementAndGet(); check(y >= -64 && y < -61, "Actual min/max Y clips scan"); return true; });
        batch.after(after::incrementAndGet);
        batch.run(0, -64, 0, -64, -61, (x, y, z) -> {
            check(reads.add(x + ":" + y + ":" + z), "Multiple modules share each world read");
            return 1;
        });
        check(one.get() == 1 && wide.get() == 27 && reads.size() == 27 && after.get() == 1, "Per-module radii and completion order");
        batch.run(0, 0, 0, -64, 320, (x, y, z) -> { throw new AssertionError("An empty batch must not query the world"); });

        AtomicInteger cancelled = new AtomicInteger(), healthy = new AtomicInteger();
        batch.register(1, 1, (x, y, z, value) -> { cancelled.incrementAndGet(); return false; });
        batch.register(1, 1, (x, y, z, value) -> { throw new IllegalArgumentException("broken module"); });
        batch.register(1, 1, (x, y, z, value) -> { healthy.incrementAndGet(); return true; });
        batch.run(0, 0, 0, -64, 320, (x, y, z) -> 1);
        check(cancelled.get() == 1 && failures.get() == 1 && healthy.get() == 27, "Cancellation and a failing module do not poison other scans");

        AtomicInteger nested = new AtomicInteger();
        batch.register(0, 0, (x, y, z, value) -> {
            batch.register(0, 0, (a, b, c, v) -> { nested.incrementAndGet(); return true; });
            return true;
        });
        batch.run(0, 0, 0, -64, 320, (x, y, z) -> 1);
        check(nested.get() == 0, "Reentrant registrations wait until next batch");
        batch.run(0, 0, 0, -64, 320, (x, y, z) -> 1);
        check(nested.get() == 1, "Next-batch registration is retained");
        batch.register(1, 1, (x, y, z, value) -> { batch.clear(); return true; });
        batch.after(() -> { throw new AssertionError("World transition must cancel completion callbacks"); });
        batch.run(0, 0, 0, -64, 320, (x, y, z) -> 1);
        System.out.println("Shared block scan regressions passed.");
    }
    private static void check(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
}
