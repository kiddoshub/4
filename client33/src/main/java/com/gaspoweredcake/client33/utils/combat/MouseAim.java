package com.gaspoweredcake.client33.utils.combat;

import java.util.Random;

/** Builds bounded mouse-count movements along a cubic curve. No OS input is generated. */
public final class MouseAim {
    public record Delta(int rawX, int rawY, double gain) {
        public double turnX(boolean inverted) { return rawX * gain * (inverted ? -1 : 1); }
        public double turnY(boolean inverted) { return rawY * gain * (inverted ? -1 : 1); }
    }

    private final Random random;
    private boolean active;
    private double startYaw, startPitch, endYaw, endPitch, c1Yaw, c1Pitch, c2Yaw, c2Pitch;
    private double progress, pathLength;

    public MouseAim(Random random) { this.random = random; }
    public void reset() { active = false; }

    /** Vanilla's non-cinematic mouse sensitivity multiplier before Entity.turn's 0.15 factor. */
    public static double gain(double sensitivity, boolean scoped) {
        double base = Math.max(0, Math.min(1, sensitivity)) * 0.6000000238418579 + 0.20000000298023224;
        return base * base * base * (scoped ? 1 : 8);
    }

    public Delta next(double yaw, double pitch, double targetYaw, double targetPitch, double sensitivity,
                      boolean scoped, boolean invertX, boolean invertY, double maxDegreesPerTick) {
        if (!Double.isFinite(yaw) || !Double.isFinite(pitch) || !Double.isFinite(targetYaw)
            || !Double.isFinite(targetPitch) || !Double.isFinite(sensitivity)
            || !Double.isFinite(maxDegreesPerTick) || maxDegreesPerTick <= 0) {
            reset();
            return new Delta(0, 0, 0);
        }
        targetPitch = Math.max(-90, Math.min(90, targetPitch));
        double desiredYaw = yaw + wrap(targetYaw - yaw);
        double endpointChange = Math.hypot(wrap(desiredYaw - endYaw), targetPitch - endPitch);
        if (!active || endpointChange > 3 || (progress >= 1 && endpointChange >= gain(sensitivity, scoped) * 0.15)) {
            startYaw = yaw; startPitch = pitch; endYaw = desiredYaw; endPitch = targetPitch;
            double dyaw = endYaw - startYaw, dpitch = endPitch - startPitch;
            double offset = (random.nextDouble() * 0.24 - 0.12);
            double first = 0.22 + random.nextDouble() * 0.12;
            double second = 0.66 + random.nextDouble() * 0.12;
            c1Yaw = startYaw + dyaw * first - dpitch * offset;
            c1Pitch = startPitch + dpitch * first + dyaw * offset;
            c2Yaw = startYaw + dyaw * second - dpitch * offset;
            c2Pitch = startPitch + dpitch * second + dyaw * offset;
            progress = 0;
            pathLength = Math.hypot(dyaw, dpitch);
            active = true;
        }
        progress = Math.min(1, progress + maxDegreesPerTick / Math.max(1, pathLength));
        double aimYaw = cubic(startYaw, c1Yaw, c2Yaw, endYaw, progress);
        double aimPitch = Math.max(-90, Math.min(90, cubic(startPitch, c1Pitch, c2Pitch, endPitch, progress)));
        double dx = wrap(aimYaw - yaw), dy = aimPitch - pitch;
        double distance = Math.hypot(dx, dy);
        if (distance > maxDegreesPerTick) { dx *= maxDegreesPerTick / distance; dy *= maxDegreesPerTick / distance; }
        double gain = gain(sensitivity, scoped), unit = gain * 0.15;
        // Sub-count motion is skipped; the next point is always compared with actual rotation.
        int x = (int) Math.copySign(Math.floor(Math.abs(dx) / unit), dx);
        int y = (int) Math.copySign(Math.floor(Math.abs(dy) / unit), dy);
        return new Delta(invertX ? -x : x, invertY ? -y : y, gain);
    }

    private static double cubic(double a, double b, double c, double d, double t) {
        double u = 1 - t;
        return u * u * u * a + 3 * u * u * t * b + 3 * u * t * t * c + t * t * t * d;
    }

    private static double wrap(double angle) {
        double result = angle % 360;
        return result >= 180 ? result - 360 : result < -180 ? result + 360 : result;
    }
}
