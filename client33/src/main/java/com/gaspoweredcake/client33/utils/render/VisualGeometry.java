package com.gaspoweredcake.client33.utils.render;

import com.gaspoweredcake.client33.renderer.Renderer3D;
import com.gaspoweredcake.client33.utils.render.color.Color;

/** Bounded procedural geometry shared by entity and combat visuals. */
public final class VisualGeometry {
    private VisualGeometry() {}

    public static void ring(Renderer3D renderer, double x, double y, double z, double radius,
                            double phase, double sweep, int segments, Color color) {
        if (segments <= 0) return;
        double start = phase + sweep * 0 / segments;
        double ax = x + Math.cos(start) * radius, az = z + Math.sin(start) * radius;
        for (int i = 0; i < segments; i++) {
            double b = phase + sweep * (i + 1) / segments;
            double bx = x + Math.cos(b) * radius, bz = z + Math.sin(b) * radius;
            renderer.line(ax, y, az, bx, y, bz, color);
            ax = bx; az = bz;
        }
    }

    public static void band(Renderer3D renderer, double x, double y, double z, double radius,
                            double height, int segments, Color color) {
        if (segments <= 0) return;
        Color clear = new Color(color.r, color.g, color.b, 0);
        double start = Math.PI * 2 * 0 / segments;
        double ax = x + Math.cos(start) * radius, az = z + Math.sin(start) * radius;
        for (int i = 0; i < segments; i++) {
            double b = Math.PI * 2 * (i + 1) / segments;
            double bx = x + Math.cos(b) * radius, bz = z + Math.sin(b) * radius;
            renderer.quad(ax, y, az, bx, y, bz, bx, y + height, bz, ax, y + height, az,
                color, color, clear, clear);
            ax = bx; az = bz;
        }
    }

    public static double progress(double elapsed, double duration) {
        if (!Double.isFinite(elapsed) || !Double.isFinite(duration) || duration <= 0) return 1;
        return Math.max(0, Math.min(1, elapsed / duration));
    }
}
