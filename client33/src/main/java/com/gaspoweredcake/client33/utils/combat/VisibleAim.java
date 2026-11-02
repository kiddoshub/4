package com.gaspoweredcake.client33.utils.combat;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Optional;
import java.util.function.Predicate;

/** Deterministic interior samples ordered by distance, checked against a caller's world raycast. */
public final class VisibleAim {
    public record Point(double x, double y, double z) {
        public double squaredDistance(Point other) {
            double dx = x - other.x, dy = y - other.y, dz = z - other.z;
            return dx * dx + dy * dy + dz * dz;
        }
    }
    private VisibleAim() {}
    public static Optional<Point> find(Point eye, Point min, Point max, double range, Predicate<Point> visible) {
        if (range < 0 || !Double.isFinite(range)) return Optional.empty();
        var points = new ArrayList<Point>(28);
        double inset = Math.min(.02, Math.min(max.x - min.x, Math.min(max.y - min.y, max.z - min.z)) / 4);
        if (inset < 0) return Optional.empty();
        points.add(new Point(clamp(eye.x, min.x + inset, max.x - inset), clamp(eye.y, min.y + inset, max.y - inset), clamp(eye.z, min.z + inset, max.z - inset)));
        for (double x : new double[]{.08, .5, .92}) for (double y : new double[]{.08, .5, .92}) for (double z : new double[]{.08, .5, .92})
            points.add(new Point(min.x + (max.x - min.x) * x, min.y + (max.y - min.y) * y, min.z + (max.z - min.z) * z));
        points.sort(Comparator.comparingDouble(p -> p.squaredDistance(eye)));
        double maxDistance = range * range;
        for (Point point : points) if (point.squaredDistance(eye) <= maxDistance && visible.test(point)) return Optional.of(point);
        return Optional.empty();
    }
    private static double clamp(double value, double min, double max) { return Math.max(min, Math.min(max, value)); }
}
