package com.gaspoweredcake.client33;

import com.gaspoweredcake.client33.utils.misc.TickTimer;
import com.gaspoweredcake.client33.utils.render.ScreenBounds;

/** Dependency-free regression checks, run by Gradle check. */
public final class ModuleLogicRegression {
    public static void main(String[] args) throws Exception {
        var cursor = new com.gaspoweredcake.client33.utils.misc.LookaheadIterator<Integer>() {
            private int calls;
            @Override protected Integer findNext() { return ++calls <= 2 ? calls : null; }
        };
        check(cursor.hasNext() && cursor.hasNext() && cursor.next() == 1, "hasNext must not consume values");
        check(cursor.next() == 2, "next must work without an explicit hasNext");
        check(!cursor.hasNext() && !cursor.hasNext(), "Exhaustion remains stable");
        try { cursor.next(); throw new AssertionError("Exhausted next must throw"); }
        catch (java.util.NoSuchElementException expected) { }
        AnimationPhaseRegression.run();
        SurfaceMeshRegression.run();
        WorkspacePageRegression.run();
        StorageRegression.run();
        ChestStealerRegression.run();
        GuardedActionsRegression.run();
        BlockLightRegression.run();
        StoragePillarRegression.run();
        StorageClustersRegression.run();
        StorageLeadSelectorRegression.run();
        StorageHistoryRegression.run();
        CachedStorageRegression.run();
        StorageViewRegression.run();
        BobbyStorageRecordsRegression.run();
        StorageSignalsRegression.run();
        ActivityLeadsRegression.run();
        MaterialImageRegression.run();
        InputStateRegression.run();
        FontRegression.run();
        SpawnerMenuRegression.run();
        PackedSectionRegression.run();
        LightFusionRegression.run();
        SqliteIntelRegression.run();
        PacketForensicsRegression.run();
        PositionHistoryRegression.run();
        SlotDataStoreRegression.run();
        TranslationGuardRegression.run();
        FlightGradientRegression.run();
        BlockScanRegression.run();
        LifecycleRegression.run();
        HttpRegression.run();
        PersistenceRegression.run();
        PlayerHeadRegression.run();
        ReconCombatRegression.run();
        KillAuraRegression.run();
        MouseAimRegression.run();
        SprintRulesRegression.run();
        ContainerReachRegression.run();
        ScaffoldFaceRegression.run();
        TickTimer timer = new TickTimer();
        check(timer.ready(), "A newly enabled action must be ready");
        timer.waitTicks(3);
        check(!timer.ready(), "A delay must block on the scheduling tick");
        timer.advance();
        timer.advance();
        check(!timer.ready(), "A three-tick delay must not expire early");
        timer.advance();
        check(timer.ready(), "A delay must expire on exactly the requested tick");
        timer.waitTicks(4);
        for (int i = 0; i < 10; i++) timer.advance();
        check(timer.ready(), "Cooldown must advance while target/action is absent");
        timer.waitTicks(2);
        timer.reset();
        check(timer.ready(), "Reset must discard cooldown from the previous session");
        timer.waitTicks(0);
        check(timer.ready(), "Zero delay must be immediately ready");
        check(TickTimer.serverDelay(10, 10, true) == 20, "Half TPS must double custom delay");
        check(TickTimer.serverDelay(10, 0, true) == 200, "Zero TPS must remain bounded");
        check(TickTimer.serverDelay(10, Double.NaN, true) == 10, "Invalid TPS needs a usable fallback");
        check(TickTimer.serverDelay(10, 30, true) == 10, "High TPS must not speed attacks up");
        check(TickTimer.serverDelay(10, 10, false) == 10, "Disabled sync must preserve the delay");
        check(TickTimer.serverDelay(1, 19, true) == 2, "Fractional delays must round up");

        check(com.gaspoweredcake.client33.utils.render.EspLabels.format("Alex", 12L, 17.5f).equals("Alex / 12 m / 17.5 hp"), "ESP labels combine selected fields");
        check(com.gaspoweredcake.client33.utils.render.EspLabels.format("Alex", null, null).equals("Alex"), "Names work without distance");
        check(com.gaspoweredcake.client33.utils.render.EspLabels.format(null, 12L, null).equals("12 m"), "Distance works without a name");
        check(com.gaspoweredcake.client33.utils.render.EspLabels.format(null, null, Float.NaN).isEmpty(), "Invalid health is omitted");
        check(com.gaspoweredcake.client33.utils.render.EspLabels.format(null, null, -1f).equals("0.0 hp"), "Health labels do not show negative health");

        ScreenBounds bounds = new ScreenBounds();
        bounds.reset();
        check(!bounds.clip(800, 600), "Empty bounds must not render");
        bounds.reset();
        bounds.include(-40, -20, 0.5);
        bounds.include(900, 700, 0.9);
        check(bounds.clip(800, 600), "Partially offscreen geometry should be clipped");
        check(bounds.left == 0 && bounds.top == 0 && bounds.right == 800 && bounds.bottom == 600,
            "All edges must lie inside the viewport");
        bounds.reset();
        bounds.include(900, 100, 0.5);
        bounds.include(1000, 200, 0.5);
        check(!bounds.clip(800, 600), "Fully offscreen geometry must not render");
        bounds.reset();
        bounds.include(10, 10, 0.5);
        bounds.include(11, 11, 0.5);
        check(!bounds.clip(800, 600), "Subpixel boxes must not render");
        for (double invalid : new double[] {Double.NaN, Double.POSITIVE_INFINITY}) {
            bounds.reset();
            bounds.include(10, 10, 0.5);
            bounds.include(20, 20, 0.5);
            bounds.include(invalid, 30, 0.5);
            check(!bounds.clip(800, 600), "One invalid corner must invalidate the entire box");
        }
        bounds.reset();
        bounds.include(10, 10, -0.1);
        bounds.include(20, 20, 0.5);
        check(!bounds.clip(800, 600), "Near-plane crossings must not create giant boxes");
        bounds.reset();
        bounds.include(10, 10, 0.5);
        bounds.include(20, 20, 0.5);
        check(bounds.clip(800, 600), "A rejected entity must not poison the next entity");
        System.out.println("Module logic regressions passed.");
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
