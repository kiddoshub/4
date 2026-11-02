package com.gaspoweredcake.client33.utils.render;

import org.lwjgl.sdl.SDLDialog;
import org.lwjgl.sdl.SDL_DialogFileCallback;
import org.lwjgl.sdl.SDL_DialogFileFilter;
import org.lwjgl.system.MemoryUtil;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import static com.gaspoweredcake.client33.Client33.mc;

/** One application-lifetime SDL callback; no legacy GLFW window handles or tinyfd dependency. */
public final class MaterialFilePicker {
    private static final AtomicReference<Consumer<Path>> pending = new AtomicReference<>();
    private static final SDL_DialogFileFilter.Buffer FILTER = SDL_DialogFileFilter.calloc(1)
        .name(MemoryUtil.memUTF8("PNG or animated GIF")).pattern(MemoryUtil.memUTF8("png;gif"));
    private static final SDL_DialogFileCallback CALLBACK = SDL_DialogFileCallback.create((user, files, filter) -> {
        Consumer<Path> selected = pending.getAndSet(null);
        if (selected == null || files == 0) return;
        long first = MemoryUtil.memGetAddress(files);
        if (first == 0) return;
        String path = MemoryUtil.memUTF8(first);
        mc.execute(() -> selected.accept(Path.of(path)));
    });
    private MaterialFilePicker() { }
    public static void open(Consumer<Path> selected) {
        if (!pending.compareAndSet(null, selected)) return;
        try { SDLDialog.SDL_ShowOpenFileDialog(CALLBACK, 0, mc.getWindow().handle(), FILTER, (CharSequence) null, false); }
        catch (RuntimeException error) { pending.set(null); throw error; }
    }
}
