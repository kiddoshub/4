package com.gaspoweredcake.client33.utils.intel;

import com.gaspoweredcake.client33.Client33;
import com.gaspoweredcake.client33.utils.network.Client33Executor;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/** Serial worker-backed persistence of server-provided block entity payloads. */
public final class BlockEntityStore {
    private sealed interface Operation permits Write, Read {}
    private record Write(String server, String dimension, int x, int y, int z, String type, long time, CompoundTag tag) implements Operation {}
    private record Read(String server, String dimension, int x, int y, int z, CompletableFuture<Optional<Entry>> result) implements Operation {}
    public record Entry(String type, long observedMillis, CompoundTag nbt) {}
    private static final int LIMIT = 1024, MAX_NBT_BYTES = 1_048_576;
    private final ArrayBlockingQueue<Operation> queue = new ArrayBlockingQueue<>(LIMIT);
    private final AtomicBoolean draining = new AtomicBoolean();
    private final AtomicLong rejected = new AtomicLong();
    private final Path file;

    public BlockEntityStore(Path file) { this.file = file; }
    public long rejected() { return rejected.get(); }

    /** Called on the client thread; queued NBT is detached before returning. */
    public boolean offer(String server, String dimension, int x, int y, int z, String type, long time, CompoundTag nbt) {
        if (nbt == null || nbt.sizeInBytes() > MAX_NBT_BYTES) { rejected.incrementAndGet(); return false; }
        if (!queue.offer(new Write(server, dimension, x, y, z, type, time, nbt.copy()))) { rejected.incrementAndGet(); return false; }
        schedule(); return true;
    }

    /** Reads are ordered after earlier queued writes. Completion runs on the worker thread. */
    public CompletableFuture<Optional<Entry>> get(String server, String dimension, int x, int y, int z) {
        var result = new CompletableFuture<Optional<Entry>>();
        if (!queue.offer(new Read(server, dimension, x, y, z, result))) {
            rejected.incrementAndGet(); result.completeExceptionally(new RejectedExecutionException("Block entity queue is full"));
        } else schedule();
        return result;
    }

    private void schedule() {
        if (!draining.compareAndSet(false, true)) return;
        try { Client33Executor.execute(this::drain); }
        catch (RuntimeException failure) {
            draining.set(false); failPending(failure); Client33.LOG.error("Unable to schedule block entity storage", failure);
        }
    }

    private void failPending(Throwable failure) {
        Operation operation;
        while ((operation = queue.poll()) != null) {
            rejected.incrementAndGet();
            if (operation instanceof Read read) read.result.completeExceptionally(failure);
        }
    }

    private void drain() {
        try (SqliteBlockEntityDatabase db = new SqliteBlockEntityDatabase(file)) {
            Operation operation;
            while ((operation = queue.poll()) != null) {
                try {
                    if (operation instanceof Write item) {
                        ByteArrayOutputStream bytes = new ByteArrayOutputStream(); NbtIo.writeCompressed(item.tag, bytes);
                        db.put(item.server, item.dimension, item.x, item.y, item.z, item.type, item.time, bytes.toByteArray());
                    } else if (operation instanceof Read item) {
                        var result = db.get(item.server, item.dimension, item.x, item.y, item.z);
                        if (result.isEmpty()) item.result.complete(Optional.empty());
                        else {
                            var value = result.get();
                            item.result.complete(Optional.of(new Entry(value.type(), value.observedMillis(),
                                NbtIo.readCompressed(new ByteArrayInputStream(value.nbt()), NbtAccounter.create(MAX_NBT_BYTES)))));
                        }
                    }
                } catch (Exception failure) {
                    rejected.incrementAndGet();
                    if (operation instanceof Read item) item.result.completeExceptionally(failure);
                    Client33.LOG.error("Failed to process block entity observation", failure);
                }
            }
        } catch (Exception | LinkageError failure) {
            Client33.LOG.error("Unable to open block entity database", failure); failPending(failure);
        } finally {
            draining.set(false);
            if (!queue.isEmpty()) schedule();
        }
    }

}
