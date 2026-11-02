package com.gaspoweredcake.client33.utils.render;

/** Bounded animation phase driven by a monotonic clock, independent of frame rate. */
public final class AnimationPhase {
    private long previousNanos;
    private double phase;

    public AnimationPhase(long nowNanos) { previousNanos = nowNanos; }

    public float sample(long nowNanos, double cyclesPerSecond) {
        long elapsed = nowNanos - previousNanos;
        previousNanos = nowNanos;
        if (elapsed > 0 && Double.isFinite(cyclesPerSecond) && cyclesPerSecond > 0) {
            phase = (phase + elapsed / 1_000_000_000.0 * cyclesPerSecond) % 1.0;
        }
        return (float) phase;
    }
}
