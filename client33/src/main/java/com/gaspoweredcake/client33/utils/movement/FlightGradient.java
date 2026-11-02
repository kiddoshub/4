package com.gaspoweredcake.client33.utils.movement;

/** Advances a capped velocity once per client tick. */
public final class FlightGradient {
    public static final double TOTAL_SPEED_LIMIT = 10.0;
    public record Velocity(double x, double y, double z) {}
    private FlightGradient() {}
    public static double step(double current, double target, double limit) {
        if (!Double.isFinite(target)) return 0;
        if (!Double.isFinite(current)) current = 0;
        if (!Double.isFinite(limit) || limit <= 0) return current;
        return current + Math.max(-limit, Math.min(limit, target - current));
    }
    public static Velocity next(Velocity current, Velocity target, double acceleration, double horizontalCap, double verticalCap) {
        Velocity limitedTarget = cap(target, horizontalCap, verticalCap);
        return cap(new Velocity(step(current.x, limitedTarget.x, acceleration), step(current.y, limitedTarget.y, acceleration),
            step(current.z, limitedTarget.z, acceleration)), horizontalCap, verticalCap);
    }
    private static Velocity cap(Velocity value, double horizontal, double vertical) {
        if (!Double.isFinite(value.x) || !Double.isFinite(value.y) || !Double.isFinite(value.z)
            || !Double.isFinite(horizontal) || horizontal <= 0 || !Double.isFinite(vertical) || vertical <= 0) return new Velocity(0, 0, 0);
        double magnitude = Math.hypot(value.x, value.z);
        double scale = magnitude > horizontal ? horizontal / magnitude : 1;
        double x = value.x * scale, y = Math.max(-vertical, Math.min(vertical, value.y)), z = value.z * scale;
        double total = Math.hypot(Math.hypot(x, z), y);
        if (total > TOTAL_SPEED_LIMIT) {
            double totalScale = TOTAL_SPEED_LIMIT / total;
            x *= totalScale; y *= totalScale; z *= totalScale;
        }
        return new Velocity(x, y, z);
    }
}
