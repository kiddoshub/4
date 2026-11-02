package com.gaspoweredcake.client33;

import com.gaspoweredcake.client33.utils.render.StoragePillar;
import com.gaspoweredcake.client33.utils.render.StorageLocator;

public final class StoragePillarRegression {
    public static void main(String[] args) { run(); }
    public static void run() {
        for (double distance : new double[] {0, 32, 64, 80, 128, 512, 4096, 1_000_000}) {
            check(StoragePillar.opacity(distance) == 1, "No distance fade at " + distance);
        }
        check(StoragePillar.opacity(-1) == 0, "Invalid negative distance does not render");
        check(StoragePillar.opacity(Double.NaN) == 0 && StoragePillar.opacity(Double.POSITIVE_INFINITY) == 0, "Nonfinite distances do not render");
        check(StorageLocator.at(0, 0, 10, 1, 1080) == null, "Near boxes need no extra locator");
        for (int height : new int[] {720, 1080, 2160}) {
            for (double distance : new double[] {1000, 4096, 1_000_000}) {
                var marker = StorageLocator.at(0, 0, distance, 1.3, height);
                check(marker != null, "Far storage retains a locator");
                double screenWidth = 2 * Math.abs(marker.rightX()) * 1.3 * height / (2 * distance);
                check(Math.abs(screenWidth - 5) < 1e-9, "Five-pixel locator across distances and resolutions");
            }
        }
        var vertical = StorageLocator.at(0, 4096, 0, 1, 1080);
        check(vertical != null && Double.isFinite(vertical.upZ()), "Vertical sight lines produce finite geometry");
        var diagonal = StorageLocator.at(2000, 3000, 4000, 1, 1080);
        check(Math.abs(diagonal.upX() * 2000 + diagonal.upY() * 3000 + diagonal.upZ() * 4000) < 1e-8,
            "Locator faces the camera");
        check(StorageLocator.at(Double.NaN, 0, 1, 1, 1080) == null, "Invalid positions are rejected");
        check(StorageLocator.at(0, 0, 1000, 0, 1080) == null, "Invalid projection is rejected");
        for (double distance : new double[] {4800, 6000, 1_000_000}) {
            double dx = distance * .3, dy = distance * .2, dz = distance;
            var source = StorageLocator.at(dx, dy, dz, 1.3, 1080);
            var projected = StorageLocator.project(dx, dy, dz, 1.3, 1080);
            check(projected != null && Math.abs(Math.hypot(Math.hypot(projected.x(), projected.z()), projected.y()) - 32) < 1e-9,
                "300-chunk and farther markers remain inside the vanilla far plane");
            double sourceX = dx + source.rightX(), sourceY = dy, sourceZ = dz + source.rightZ();
            var d = projected.diamond();
            double px = projected.x() + d.rightX(), py = projected.y(), pz = projected.z() + d.rightZ();
            for (double yaw : new double[] {-1.1, -.3, 0, .7}) {
                for (double pitch : new double[] {-.5, 0, .4}) {
                    double[] before = perspective(sourceX, sourceY, sourceZ, yaw, pitch);
                    double[] after = perspective(px, py, pz, yaw, pitch);
                    check(Math.abs(before[0] - after[0]) < 1e-9 && Math.abs(before[1] - after[1]) < 1e-9,
                        "Far-plane compression preserves projected marker direction and size under camera rotation");
                }
            }
        }
        check(StorageLocator.project(0, 0, 10, 1, 1080) == null, "Near storage still uses its original box");
        System.out.println("Storage unlimited-distance pillar regressions passed.");
    }
    private static double[] perspective(double x, double y, double z, double yaw, double pitch) {
        double vx = x * Math.cos(yaw) - z * Math.sin(yaw);
        double vz = x * Math.sin(yaw) + z * Math.cos(yaw);
        double vy = y * Math.cos(pitch) - vz * Math.sin(pitch);
        double depth = y * Math.sin(pitch) + vz * Math.cos(pitch);
        return new double[] {vx / depth, vy / depth};
    }
    private static void check(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
}
