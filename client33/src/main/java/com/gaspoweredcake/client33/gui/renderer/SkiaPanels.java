package com.gaspoweredcake.client33.gui.renderer;

import io.github.humbleui.skija.Bitmap;
import io.github.humbleui.skija.ColorAlphaType;
import io.github.humbleui.skija.ColorType;
import io.github.humbleui.skija.ImageInfo;
import io.github.humbleui.skija.Paint;
import io.github.humbleui.skija.PathBuilder;
import io.github.humbleui.skija.Surface;
import io.github.humbleui.types.RRect;
import org.inspirenxe.strata.api.extension.Painter;
import org.inspirenxe.strata.api.geometry.Rect;
import org.inspirenxe.strata.api.visual.PixelBuffer;

import java.util.LinkedHashMap;
import java.util.Map;

/** Skia rasterizes vector curves once; Strata owns upload, clipping and texture disposal. */
public final class SkiaPanels {
    private record Size(int width, int height) {}
    private final Map<Size, PixelBuffer> cache = new LinkedHashMap<>(8, 0.75f, true);

    public void paint(Painter painter, Rect bounds) {
        if (bounds.isEmpty()) return;
        float density = Math.max(1, painter.pixelsPerUnit());
        Size size = new Size(Math.clamp(Math.round(bounds.width() * density), 1, 1024),
            Math.clamp(Math.round(bounds.height() * density), 1, 512));
        PixelBuffer pixels = cache.get(size);
        if (pixels == null) {
            pixels = rasterize(size.width, size.height);
            cache.put(size, pixels);
            if (cache.size() > 8) cache.remove(cache.keySet().iterator().next());
        }
        painter.pixels(pixels, bounds, 0xffffffff);
    }

    public static PixelBuffer rasterize(int width, int height) {
        if (width < 1 || height < 1 || width > 1024 || height > 512)
            throw new IllegalArgumentException("Panel raster exceeds 1024 x 512");
        try (Surface surface = Surface.makeRaster(ImageInfo.makeN32Premul(width, height));
             Paint paint = new Paint().setAntiAlias(true);
             Bitmap bitmap = new Bitmap()) {
            var canvas = surface.getCanvas();
            float scale = Math.min(width / 300f, height / 88f);
            float radius = Math.max(2, 8 * scale);
            canvas.clear(0);
            canvas.drawRRect(RRect.makeXYWH(0, 0, width, height, radius), paint.setColor(0xee202735));
            // Sparse directional curves stay along the edge, leaving controls and text quiet.
            canvas.save();
            canvas.clipRRect(RRect.makeXYWH(0, 0, width, height, radius));
            paint.setStroke(true).setStrokeWidth(Math.max(0.8f, 0.7f * scale));
            for (int i = 0; i < 5; i++) {
                float offset = i * 5 * scale;
                try (PathBuilder path = new PathBuilder()) {
                    path.moveTo(width * .68f + offset, height + 8 * scale)
                        .cubicTo(width * .78f + offset, height * .48f,
                            width * .92f + offset, height * 1.2f, width + 6 * scale, height * .17f + offset);
                    try (var curve = path.build()) {
                        canvas.drawPath(curve, paint.setColor(i == 0 ? 0x567daec9 : 0x248ea3c0));
                    }
                }
            }
            canvas.restore();
            canvas.drawRRect(RRect.makeXYWH(.5f, .5f, width - 1, height - 1, radius),
                paint.setColor(0x485f718c).setStrokeWidth(1));
            var info = new ImageInfo(width, height, ColorType.RGBA_8888, ColorAlphaType.UNPREMUL);
            if (!bitmap.allocPixels(info) || !surface.readPixels(bitmap, 0, 0))
                throw new IllegalStateException("Skia could not read the panel raster");
            byte[] rgba = bitmap.readPixels();
            PixelBuffer pixels = PixelBuffer.of(width, height);
            int[] argb = pixels.array();
            for (int i = 0; i < argb.length; i++) {
                int offset = i * 4;
                argb[i] = (rgba[offset + 3] & 255) << 24 | (rgba[offset] & 255) << 16
                    | (rgba[offset + 1] & 255) << 8 | rgba[offset + 2] & 255;
            }
            return pixels.changed();
        }
    }
}
