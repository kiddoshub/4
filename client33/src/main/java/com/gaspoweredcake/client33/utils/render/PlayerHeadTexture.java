package com.gaspoweredcake.client33.utils.render;

import com.mojang.renderpearl.api.GpuFormat;
import com.mojang.blaze3d.platform.TextureUtil;
import com.mojang.renderpearl.api.textures.FilterMode;
import com.gaspoweredcake.client33.Client33;
import com.gaspoweredcake.client33.renderer.Texture;
import com.gaspoweredcake.client33.utils.network.Http;
import org.lwjgl.BufferUtils;
import org.lwjgl.stb.STBImage;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import static com.gaspoweredcake.client33.Client33.mc;

public class PlayerHeadTexture extends Texture {
    private static final int MAX_SKIN_BYTES = 4 * 1024 * 1024;
    private static final int MAX_SKIN_DIMENSION = 1024;
    private boolean needsRotate;

    public PlayerHeadTexture(byte[] head, boolean needsRotate) {
        super(8, 8, GpuFormat.RGBA8_UNORM, FilterMode.NEAREST, FilterMode.NEAREST);

        upload(BufferUtils.createByteBuffer(head.length).put(head));
        this.needsRotate = needsRotate;
    }

    public PlayerHeadTexture() {
        super(8, 8, GpuFormat.RGBA8_UNORM, FilterMode.NEAREST, FilterMode.NEAREST);

        try (InputStream inputStream = mc.getResourceManager().getResource(Client33.identifier("textures/steve.png")).get().open()) {
            ByteBuffer data = TextureUtil.readResource(inputStream);
            data.rewind();

            try (MemoryStack stack = MemoryStack.stackPush()) {
                IntBuffer width = stack.mallocInt(1);
                IntBuffer height = stack.mallocInt(1);
                IntBuffer comp = stack.mallocInt(1);

                ByteBuffer image = STBImage.stbi_load_from_memory(data, width, height, comp, 4);
                upload(image);
                STBImage.stbi_image_free(image);
            }
            MemoryUtil.memFree(data);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public boolean needsRotate() {
        return needsRotate;
    }

    public static byte[] downloadHead(String url) throws IOException {
        byte[] bytes;
        try (InputStream in = Http.get(url).sendInputStream()) {
            if (in == null) throw new IOException("Skin download failed.");
            bytes = in.readNBytes(MAX_SKIN_BYTES + 1);
        }
        if (bytes.length > MAX_SKIN_BYTES) throw new IOException("Skin image exceeds size limit.");
        return decodeHead(bytes);
    }

    public static byte[] decodeHead(byte[] bytes) throws IOException {
        if (bytes.length > MAX_SKIN_BYTES) throw new IOException("Skin image exceeds size limit.");
        BufferedImage skin;
        try (ImageInputStream stream = ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {
            if (stream == null) throw new IOException("Failed to open skin image.");
            var readers = ImageIO.getImageReaders(stream);
            if (!readers.hasNext()) throw new IOException("Failed to decode skin image.");
            ImageReader reader = readers.next();
            try {
                reader.setInput(stream, true, true);
                int width = reader.getWidth(0), height = reader.getHeight(0);
                if (width < 48 || height < 16 || width > MAX_SKIN_DIMENSION || height > MAX_SKIN_DIMENSION)
                    throw new IOException("Unsupported skin dimensions.");
                skin = reader.read(0);
            } finally { reader.dispose(); }
        }
        if (skin == null) throw new IOException("Failed to decode skin image.");

        byte[] head = new byte[8 * 8 * 4];

        int i = 0;
        for (int x = 8; x < 16; x++) {
            for (int y = 8; y < 16; y++) {
                int pixel = skin.getRGB(x, y);
                head[i++] = (byte) (pixel >> 16);
                head[i++] = (byte) (pixel >> 8);
                head[i++] = (byte) pixel;
                head[i++] = (byte) (pixel >>> 24);
            }
        }

        i = 0;
        for (int x = 40; x < 48; x++) {
            for (int y = 8; y < 16; y++) {
                int pixel = skin.getRGB(x, y);
                if ((pixel >>> 24) != 0) {
                    head[i++] = (byte) (pixel >> 16);
                    head[i++] = (byte) (pixel >> 8);
                    head[i++] = (byte) pixel;
                    head[i++] = (byte) (pixel >>> 24);
                } else i += 4;
            }
        }

        return head;
    }
}
