package com.gaspoweredcake.client33.utils.render;

import com.gaspoweredcake.client33.Client33;
import com.gaspoweredcake.client33.renderer.Texture;
import com.mojang.renderpearl.api.GpuFormat;
import com.mojang.renderpearl.api.textures.FilterMode;
import org.lwjgl.BufferUtils;
import java.nio.ByteBuffer;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

import static com.gaspoweredcake.client33.Client33.mc;

/** Single-worker import, immutable content-addressed files, and one reusable GPU texture. */
public final class UploadedMaterial {
    private static final java.util.concurrent.ExecutorService DECODER = Executors.newSingleThreadExecutor(
        Thread.ofPlatform().daemon().name("Client33-Material-Decode").factory());
    private static final AtomicLong generation = new AtomicLong();
    private static MaterialImage image;
    private static Texture texture;
    private static ByteBuffer pixels;
    private static int shownFrame = -1;
    private static boolean changed;
    private static AnimationPhase clock = new AnimationPhase(System.nanoTime());
    private static String lastGoodPath = "";
    public static String status = "No uploaded material";

    private UploadedMaterial() { }

    public static void request(String source, Consumer<String> message, Consumer<String> persist) {
        long ticket = generation.incrementAndGet();
        if (source.isBlank()) {
            image = null; changed = true; lastGoodPath = ""; status = "No uploaded material"; message.accept(status); return;
        }
        status = "Importing image…"; message.accept(status);
        DECODER.execute(() -> {
            if (ticket != generation.get()) return;
            Path temporary = null;
            try {
                Path folder = Client33.FOLDER.toPath().resolve("materials"); Files.createDirectories(folder);
                byte[] bytes;
                try (var input = Files.newInputStream(Path.of(source))) {
                    bytes = input.readNBytes((int) MaterialImage.MAX_FILE_BYTES + 1);
                }
                if (bytes.length > MaterialImage.MAX_FILE_BYTES) throw new java.io.IOException("Image exceeds 16 MiB.");
                temporary = Files.createTempFile(folder, ".material-", ".tmp"); Files.write(temporary, bytes);
                MaterialImage decoded = MaterialImage.read(temporary);
                String hash = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
                String suffix = bytes.length >= 4 && bytes[0] == 'G' && bytes[1] == 'I' && bytes[2] == 'F' ? ".gif" : ".png";
                Path asset = folder.resolve(hash + suffix);
                if (ticket != generation.get()) return;
                try { Files.move(temporary, asset, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE); }
                catch (AtomicMoveNotSupportedException ignored) { Files.move(temporary, asset, StandardCopyOption.REPLACE_EXISTING); }
                temporary = null;
                mc.execute(() -> {
                    if (ticket != generation.get()) return;
                    image = decoded; changed = true; clock = new AnimationPhase(System.nanoTime()); lastGoodPath = asset.toString();
                    status = decoded.width() + " × " + decoded.height() + " / " + decoded.frameCount() + " frame(s) / local material";
                    persist.accept(lastGoodPath); message.accept(status);
                });
            } catch (Exception error) {
                mc.execute(() -> {
                    if (ticket != generation.get()) return;
                    status = "Import failed: " + error.getMessage(); persist.accept(lastGoodPath); message.accept(status);
                });
            } finally {
                if (temporary != null) try { Files.deleteIfExists(temporary); } catch (java.io.IOException ignored) { }
            }
        });
    }

    /** Client render thread only. GIF frames share a texture and upload only when their index changes. */
    public static Texture texture(long now, boolean animate, double speed) {
        if (changed) {
            if (texture != null) texture.close();
            texture = null; pixels = null; shownFrame = -1; changed = false;
            if (image != null) {
                texture = new Texture(image.width(), image.height(), GpuFormat.RGBA8_UNORM, FilterMode.LINEAR, FilterMode.LINEAR);
                pixels = BufferUtils.createByteBuffer(image.width() * image.height() * 4);
            }
        }
        if (image == null || texture == null) return null;
        float phase = clock.sample(now, animate ? speed * 1000.0 / image.durationMillis() : 0);
        int frame = image.frameAt((long) (phase * image.durationMillis()));
        if (frame != shownFrame) {
            pixels.clear(); image.frame(frame).writeRgba(pixels); pixels.flip(); texture.upload(pixels); shownFrame = frame;
        }
        return texture;
    }

}
