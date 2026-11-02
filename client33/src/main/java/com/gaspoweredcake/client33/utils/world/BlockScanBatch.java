package com.gaspoweredcake.client33.utils.world;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/** One shared read per eligible coordinate; requests registered during a scan belong to the next batch. */
public final class BlockScanBatch<T> {
    @FunctionalInterface public interface Reader<T> { T read(int x, int y, int z); }
    @FunctionalInterface public interface Visitor<T> { boolean visit(int x, int y, int z, T value); }
    private record Request<T>(int horizontal, int vertical, Visitor<T> visitor) {}
    private final List<Request<T>> pending = new ArrayList<>();
    private final List<Runnable> completions = new ArrayList<>();
    private final Consumer<RuntimeException> failure;
    private long generation;

    public BlockScanBatch(Consumer<RuntimeException> failure) { this.failure = failure; }

    public void register(int horizontal, int vertical, Visitor<T> visitor) {
        if (horizontal < 0 || vertical < 0) throw new IllegalArgumentException("Negative scan radius");
        pending.add(new Request<>(horizontal, vertical, visitor));
    }
    public void after(Runnable callback) { completions.add(callback); }
    public void clear() { pending.clear(); completions.clear(); generation++; }

    /** maxY is exclusive, matching Minecraft's build-height boundary. */
    public void run(int px, int py, int pz, int minY, int maxY, Reader<T> reader) {
        var batch = new ArrayList<>(pending);
        var after = new ArrayList<>(completions);
        pending.clear(); completions.clear();
        long epoch = generation;
        int horizontal = 0, vertical = 0;
        for (var request : batch) {
            horizontal = Math.max(horizontal, request.horizontal);
            vertical = Math.max(vertical, request.vertical);
        }
        // Long endpoints avoid overflow at configured integer limits.
        long bottom = Math.max(minY, (long) py - vertical), top = Math.min((long) maxY - 1, (long) py + vertical);
        scan:
        for (long x = Math.max(Integer.MIN_VALUE, (long) px - horizontal); x <= Math.min(Integer.MAX_VALUE, (long) px + horizontal) && !batch.isEmpty(); x++) {
            for (long z = Math.max(Integer.MIN_VALUE, (long) pz - horizontal); z <= Math.min(Integer.MAX_VALUE, (long) pz + horizontal); z++) {
                for (long y = bottom; y <= top; y++) {
                    if (generation != epoch) return;
                    if (batch.isEmpty()) break scan;
                    long dx = Math.abs(x - px), dy = Math.abs(y - py), dz = Math.abs(z - pz);
                    T value = null;
                    boolean read = false;
                    for (int i = 0; i < batch.size();) {
                        Request<T> request = batch.get(i);
                        if (dx > request.horizontal || dz > request.horizontal || dy > request.vertical) { i++; continue; }
                        if (!read) { value = reader.read((int) x, (int) y, (int) z); read = true; }
                        boolean keep = false;
                        try { keep = request.visitor.visit((int) x, (int) y, (int) z, value); }
                        catch (RuntimeException ex) { failure.accept(ex); }
                        if (generation != epoch) return;
                        if (keep) i++;
                        else batch.remove(i);
                    }
                }
            }
        }
        for (Runnable callback : after) {
            if (generation != epoch) return;
            try { callback.run(); } catch (RuntimeException ex) { failure.accept(ex); }
        }
    }
}
