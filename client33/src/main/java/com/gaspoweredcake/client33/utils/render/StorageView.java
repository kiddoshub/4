package com.gaspoweredcake.client33.utils.render;

import org.joml.Matrix4fc;

/** Side-plane rejection for depth-free locators. Deliberately has no near/far distance planes. */
public final class StorageView {
    private final double[][] planes = new double[4][4];
    private final double cameraX, cameraY, cameraZ;
    private final boolean valid;

    public StorageView(Matrix4fc clip, double cameraX, double cameraY, double cameraZ, int width, int height) {
        this.cameraX = cameraX; this.cameraY = cameraY; this.cameraZ = cameraZ;
        // Eight physical pixels cover locator strokes, near hologram rails and numerical edge error.
        double horizontal = 1 + 16.0 / Math.max(1, width);
        double vertical = 1 + 16.0 / Math.max(1, height);
        double[] w = {clip.m03(), clip.m13(), clip.m23(), clip.m33()};
        double[] x = {clip.m00(), clip.m10(), clip.m20(), clip.m30()};
        double[] y = {clip.m01(), clip.m11(), clip.m21(), clip.m31()};
        for (int i = 0; i < 4; i++) {
            planes[0][i] = horizontal * w[i] + x[i];
            planes[1][i] = horizontal * w[i] - x[i];
            planes[2][i] = vertical * w[i] + y[i];
            planes[3][i] = vertical * w[i] - y[i];
        }
        boolean finite = true;
        for (double[] plane : planes) for (double coefficient : plane) finite &= Double.isFinite(coefficient);
        valid = finite;
    }

    public boolean intersects(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
        if (!valid) return true;
        // Subtract in double precision before projecting, including near the world border.
        minX -= cameraX; maxX -= cameraX;
        minY -= cameraY; maxY -= cameraY;
        minZ -= cameraZ; maxZ -= cameraZ;
        double radius = Math.hypot(Math.hypot(Math.max(Math.abs(minX), Math.abs(maxX)),
            Math.max(Math.abs(minZ), Math.abs(maxZ))), Math.max(Math.abs(minY), Math.abs(maxY)));
        // Locators are drawn on a 32-block shell. Model-view translations (e.g. bobbing)
        // act after that scaling. Conservatively cover their larger angular effect there.
        double shellAllowance = Math.max(0, radius / 32 - 1);
        for (double[] plane : planes) {
            double furthest = plane[0] * (plane[0] >= 0 ? maxX : minX)
                + plane[1] * (plane[1] >= 0 ? maxY : minY)
                + plane[2] * (plane[2] >= 0 ? maxZ : minZ) + plane[3];
            if (furthest + Math.abs(plane[3]) * shellAllowance < -1e-7) return false;
        }
        return true;
    }
}
