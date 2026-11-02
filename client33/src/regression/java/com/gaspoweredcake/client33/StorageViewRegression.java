package com.gaspoweredcake.client33;

import com.gaspoweredcake.client33.utils.render.CachedStorageIndex;
import com.gaspoweredcake.client33.utils.render.StorageView;
import com.gaspoweredcake.client33.utils.render.SignatureCulling;
import org.joml.Matrix4f;
import org.joml.Vector4d;
import java.util.ArrayList;
import java.util.Random;

public final class StorageViewRegression {
    public static void run() {
        var projection = new Matrix4f().perspective((float) Math.toRadians(90), 1, .1f, 64);
        var view = new StorageView(projection, 0, 0, 0, 1000, 1000);
        check(view.intersects(-1, -1, -4801, 1, 1, -4800), "300-chunk markers survive a 64-block terrain far plane");
        check(view.intersects(-1, -1, -1000001, 1, 1, -1000000), "No distance quota beyond 300 chunks");
        check(!view.intersects(-1, -1, 100, 1, 1, 101), "Behind-camera markers are rejected");
        check(!view.intersects(200, 0, -100, 201, 1, -99), "Offscreen pages are rejected");
        check(!SignatureCulling.include(view, 200, 0, -100, 201, 2, -99, 1, false), "Offscreen signature does not consume the visible budget");
        check(SignatureCulling.include(view, 200, 0, -100, 201, 2, -99, 1, true), "A tether crossing the viewport is preserved");
        check(SignatureCulling.include(view, 100, 0, -100, 101, 2, -99, 1, false), "Signature edges just inside the viewport are preserved");
        check(SignatureCulling.include(view, -.5, 0, -4801, .5, 2, -4800, 1, false), "Signature culling does not impose the terrain far plane");
        check(SignatureCulling.include(view, -1, -1, -1, 1, 1, 1, 2, false), "An entity surrounding the camera is preserved");
        check(view.intersects(100.5, 0, -100, 100.5, 0, -100), "Locator just outside viewport retains pixel padding");
        check(view.intersects(-100, -100, -100, 100, 100, 100), "A page spanning the camera is retained");
        var border = new StorageView(projection, 29999000, 70, -29999000, 1000, 1000);
        check(border.intersects(29998999, 69, -30003801, 29999001, 71, -30003800), "World border subtraction preserves far coordinates");
        var invalid = new StorageView(new Matrix4f().m00(Float.NaN), 0, 0, 0, 1000, 1000);
        check(invalid.intersects(200, 200, 200, 201, 201, 201), "Invalid graphics matrices fail open");
        var translatedClip = new Matrix4f(projection).translate(.75f, -.3f, .2f);
        var translatedView = new StorageView(translatedClip, 0, 0, 0, 1000, 1000);
        for (int x = -5200; x <= 5200; x += 13) {
            double scale = 32 / Math.hypot(x, 4800);
            var projectedShell = new Vector4d(x * scale, 0, -4800 * scale, 1).mul(translatedClip);
            if (projectedShell.w > 0 && Math.abs(projectedShell.x) <= projectedShell.w && Math.abs(projectedShell.y) <= projectedShell.w)
                check(translatedView.intersects(x, 0, -4800, x, 0, -4800), "Bobbing cannot reject visible shell geometry");
        }

        // Independent homogeneous clip-coordinate oracle, with camera yaw, pitch and non-square viewport.
        var random = new Random(90213);
        for (int rotation = 0; rotation < 20; rotation++) {
            var clip = new Matrix4f().perspective((float) Math.toRadians(35 + rotation * 4), 1.6f, .05f, 128)
                .rotateX((float) (rotation * .13)).rotateY((float) (rotation * .47));
            var rotated = new StorageView(clip, 0, 0, 0, 1600, 1000);
            for (int n = 0; n < 2000; n++) {
                double x = random.nextDouble(-10000, 10000), y = random.nextDouble(-10000, 10000), z = random.nextDouble(-10000, 10000);
                var p = new Vector4d(x, y, z, 1).mul(clip);
                boolean expected = p.x >= -1.01 * p.w && p.x <= 1.01 * p.w
                    && p.y >= -1.016 * p.w && p.y <= 1.016 * p.w;
                check(rotated.intersects(x, y, z, x, y, z) == expected, "Rotated side planes agree with clip coordinates");
            }
        }

        var entries = new ArrayList<CachedStorageIndex.Entry>();
        for (int x = -300; x <= 300; x++) for (int z = -300; z <= 300; z++)
            entries.add(new CachedStorageIndex.Entry(x * 16, -64 + Math.floorMod(x, 3) * 128, z * 16, "minecraft:chest"));
        var pages = CachedStorageIndex.pages(entries);
        check(pages.stream().mapToInt(p -> p.entries().size()).sum() == entries.size(), "Paging retains every one of 361201 records");
        long visited = 0, visible = 0;
        for (var page : pages) {
            for (var entry : page.entries()) {
                check(entry.x() >= page.minX() && entry.x() + 1 <= page.maxX()
                    && entry.y() >= page.minY() && entry.y() + 1 <= page.maxY()
                    && entry.z() >= page.minZ() && entry.z() + 1 <= page.maxZ(), "Page bounds contain complete unit boxes");
            }
            if (!view.intersects(page.minX(), page.minY(), page.minZ(), page.maxX(), page.maxY(), page.maxZ())) continue;
            visited += page.entries().size();
            for (var entry : page.entries()) if (visible(view, entry)) visible++;
        }
        check(visible == entries.stream().filter(e -> visible(view, e)).count(), "Page rejection drops no individually visible marker");
        check(visited < entries.size() / 2, "Offscreen pages avoid most record visits in the synthetic forward view");
        System.out.println("Storage view regressions passed: " + entries.size() + " retained, " + visited + " candidate visits, " + visible + " visible (synthetic, not an FPS benchmark).");
    }
    private static boolean visible(StorageView view, CachedStorageIndex.Entry entry) {
        return view.intersects(entry.x(), entry.y(), entry.z(), entry.x() + 1, entry.y() + 1, entry.z() + 1);
    }
    private static void check(boolean value, String message) { if (!value) throw new AssertionError(message); }
}
