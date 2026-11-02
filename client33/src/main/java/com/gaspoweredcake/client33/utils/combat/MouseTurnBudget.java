package com.gaspoweredcake.client33.utils.combat;

/** Accumulates a bounded angular budget using monotonic time. */
public final class MouseTurnBudget {
    private boolean started;
    private long lastNanos;
    private double remaining;

    public void reset() { started = false; remaining = 0; }

    public double advance(long nowNanos, double degreesPerSecond) {
        if (!Double.isFinite(degreesPerSecond) || degreesPerSecond <= 0) { reset(); return 0; }
        if (!started) { started = true; lastNanos = nowNanos; return 0; }
        long elapsed = nowNanos - lastNanos;
        if (elapsed > 0) {
            remaining = Math.min(degreesPerSecond / 20, remaining + elapsed / 1_000_000_000.0 * degreesPerSecond);
            lastNanos = nowNanos;
        }
        return remaining;
    }

    public void consume(double degrees) {
        if (Double.isFinite(degrees) && degrees > 0) remaining = Math.max(0, remaining - degrees);
    }
}
