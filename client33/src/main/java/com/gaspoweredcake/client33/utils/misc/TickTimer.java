package com.gaspoweredcake.client33.utils.misc;

/** A tick clock whose cooldown continues while the owning action is paused. */
public final class TickTimer {
    private long tick;
    private long readyAt;

    public void reset() {
        tick = 0;
        readyAt = 0;
    }

    public void advance() {
        tick++;
    }

    public void waitTicks(int ticks) {
        readyAt = tick + Math.max(0, ticks);
    }

    public boolean ready() {
        return tick >= readyAt;
    }

    public static int serverDelay(int ticks, double tps, boolean sync) {
        if (!sync) return Math.max(0, ticks);
        double rate = Double.isFinite(tps) ? Math.clamp(tps, 1, 20) : 20;
        return (int) Math.min(Integer.MAX_VALUE, Math.ceil(Math.max(0, ticks) * 20.0 / rate));
    }
}
