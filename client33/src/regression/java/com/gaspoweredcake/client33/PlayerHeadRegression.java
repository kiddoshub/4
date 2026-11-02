package com.gaspoweredcake.client33;

import com.gaspoweredcake.client33.utils.render.PlayerHeadTexture;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import javax.imageio.ImageIO;

public final class PlayerHeadRegression {
    public static void run() throws IOException {
        BufferedImage image = new BufferedImage(64, 32, BufferedImage.TYPE_INT_ARGB);
        image.setRGB(8, 8, 0xff123456);
        image.setRGB(40, 8, 0x80654321);
        image.setRGB(8, 9, 0xffaabbcc);
        byte[] head = PlayerHeadTexture.decodeHead(png(image));
        check((head[0] & 255) == 0x65 && (head[1] & 255) == 0x43 && (head[2] & 255) == 0x21 && (head[3] & 255) == 0x80, "Overlay preserves RGBA");
        check((head[4] & 255) == 0xaa && (head[5] & 255) == 0xbb && (head[6] & 255) == 0xcc && (head[7] & 255) == 0xff, "Transparent overlay keeps the base pixel");
        expectInvalid(new byte[] {1, 2, 3});
        expectInvalid(png(new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB)));
        expectInvalid(png(new BufferedImage(1025, 16, BufferedImage.TYPE_INT_ARGB)));
        expectInvalid(new byte[4 * 1024 * 1024 + 1]);
        System.out.println("Player head image regressions passed.");
    }
    private static byte[] png(BufferedImage image) throws IOException {
        var output = new ByteArrayOutputStream();
        if (!ImageIO.write(image, "png", output)) throw new IOException("PNG writer unavailable");
        return output.toByteArray();
    }
    private static void expectInvalid(byte[] bytes) throws IOException {
        try { PlayerHeadTexture.decodeHead(bytes); throw new AssertionError("Invalid skin was accepted"); }
        catch (IOException expected) { }
    }
    private static void check(boolean ok, String message) { if (!ok) throw new AssertionError(message); }
}
