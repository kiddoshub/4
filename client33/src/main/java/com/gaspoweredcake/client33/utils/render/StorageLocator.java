package com.gaspoweredcake.client33.utils.render;

/** Camera-facing locator offsets for containers too small to distinguish on screen. */
public final class StorageLocator {
    private StorageLocator() { }

    public record Diamond(double rightX, double rightZ, double upX, double upY, double upZ) { }

    public record Projected(double x, double y, double z, Diamond diamond) { }

    /**
     * Homogeneous perspective preserves direction and angular size when all camera-relative
     * coordinates share a scale. Keep overlay geometry on a 32-block shell so vanilla's
     * terrain far plane cannot discard a marker thousands of blocks away. The source
     * coordinate is never changed; this projection is only for the depth-free locator.
     */
    public static Projected project(double dx, double dy, double dz, double projectionY, int height) {
        Diamond source = at(dx, dy, dz, projectionY, height);
        if (source == null) return null;
        double scale = Math.min(1, 32 / Math.hypot(Math.hypot(dx, dz), dy));
        return new Projected(dx * scale, dy * scale, dz * scale,
            new Diamond(source.rightX * scale, source.rightZ * scale, source.upX * scale,
                source.upY * scale, source.upZ * scale));
    }

    /** Returns null while a one-block container is already at least six pixels high. */
    public static Diamond at(double dx, double dy, double dz, double projectionY, int height) {
        double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
        double focalPixels = Math.abs(projectionY) * height / 2;
        if (!Double.isFinite(distance) || !Double.isFinite(focalPixels)
            || distance <= 0 || focalPixels <= 0 || focalPixels / distance >= 6) return null;

        double horizontal = Math.hypot(dx, dz);
        double rx = horizontal > 1e-9 ? -dz / horizontal : 1;
        double rz = horizontal > 1e-9 ? dx / horizontal : 0;
        // Unit up vector perpendicular to both the sight line and horizontal right.
        double ux = dy * rz / distance;
        double uy = (dz * rx - dx * rz) / distance;
        double uz = -dy * rx / distance;
        double radius = 2.5 * distance / focalPixels;
        return new Diamond(rx * radius, rz * radius, ux * radius, uy * radius, uz * radius);
    }
}
