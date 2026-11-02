package com.gaspoweredcake.client33.utils.inventory;

import java.util.Arrays;

/** Keeps retries and pacing tied to one open container identity. */
public final class ChestStealSession {
    private Object ready;
    private Object current;
    private long tick;
    private int cooldown;
    private final long[] retryAfter = new long[54];

    public void reset() {
        ready = current = null;
        tick = 0;
        cooldown = 0;
        Arrays.fill(retryAfter, 0);
    }

    public void markReady(Object menu) { ready = menu; }

    public void beginTick(Object menu) {
        if (current != menu) {
            if (ready != menu) ready = null;
            current = menu;
            tick = 0;
            cooldown = 0;
            Arrays.fill(retryAfter, 0);
        }
        tick++;
    }

    public boolean canRun(Object menu, boolean emptyCursor, boolean otherAutoMover) {
        if (ready != menu || !emptyCursor || otherAutoMover) return false;
        if (cooldown > 0) { cooldown--; return false; }
        return true;
    }

    public boolean canAttempt(int slot) { return tick >= retryAfter[slot]; }
    public void attempted(int slot) { retryAfter[slot] = tick + 20; }
    public void finishBatch(int delay) { cooldown = delay; }
}
