package com.gaspoweredcake.client33;

import com.gaspoweredcake.client33.utils.inventory.ChestStealSession;

final class ChestStealerRegression {
    static void run() {
        ChestStealSession session = new ChestStealSession();
        Object chest = new Object(), other = new Object();
        session.beginTick(chest);
        check(!session.canRun(chest, true, false), "wait for initialized contents");
        session.markReady(chest);
        check(!session.canRun(chest, false, false), "occupied cursor pauses transfers");
        check(!session.canRun(chest, true, true), "other automatic mover pauses transfers");
        check(session.canRun(chest, true, false), "initialized chest may transfer");
        session.attempted(0);
        check(!session.canAttempt(0) && session.canAttempt(1), "attempts wait before retry without blocking other slots");
        session.finishBatch(1);
        session.beginTick(chest);
        check(!session.canRun(chest, true, false), "batch delay blocks the next tick");
        session.beginTick(chest);
        check(session.canRun(chest, true, false), "batch delay expires");
        for (int i = 0; i < 18; i++) session.beginTick(chest);
        check(session.canAttempt(0), "retry eventually resumes");
        session.beginTick(other);
        check(!session.canRun(other, true, false), "new menu requires its own contents");
        session.beginTick(chest);
        check(!session.canRun(chest, true, false), "old menu readiness cannot be reused after switching");
        session.beginTick(other);
        session.markReady(other);
        check(session.canRun(other, true, false) && session.canAttempt(0), "new menu resets retries");
        session.reset();
        session.beginTick(other);
        check(!session.canRun(other, true, false), "closing or deactivation clears readiness");
        System.out.println("Chest stealer session regressions passed.");
    }
    private static void check(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
}
