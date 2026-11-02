package com.gaspoweredcake.client33.modintegration;

import com.gaspoweredcake.client33.utils.render.CachedStorageIndex;
import de.johni0702.minecraft.bobby.FakeChunk;
import de.johni0702.minecraft.bobby.ext.ClientChunkCacheExt;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.status.ChunkStatus;

import java.util.ArrayList;
import java.util.List;
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.nbt.ListTag;

/** Bobby 5.2.16 / Minecraft 26.3. Cached records always remain distinct from live storage. */
public final class BobbyStorageBridge {
    // Bobby deserializes on worker threads. Values contain no chunk or NBT references.
    private static final Map<FakeChunk, List<CachedStorageIndex.Entry>> diskRecords =
        Collections.synchronizedMap(new WeakHashMap<>());
    private BobbyStorageBridge() {}

    public static void captureDiskRecords(FakeChunk chunk, ListTag tags) {
        diskRecords.put(chunk, BobbyStorageRecords.read(tags));
    }

    public static List<FakeChunk> chunks(ClientLevel level) {
        var manager = ((ClientChunkCacheExt) level.getChunkSource()).bobby_getFakeChunkManager();
        if (manager == null) return List.of();
        var result = new ArrayList<FakeChunk>();
        for (LevelChunk chunk : manager.getFakeChunks()) if (chunk instanceof FakeChunk fake) result.add(fake);
        return result;
    }

    public static boolean isCurrent(ClientLevel level, FakeChunk chunk) {
        var manager = ((ClientChunkCacheExt) level.getChunkSource()).bobby_getFakeChunkManager();
        return manager != null && chunk.getLevel() == level
            && manager.getChunk(chunk.getPos().x(), chunk.getPos().z()) == chunk;
    }

    public static boolean isLive(ClientLevel level, int x, int z) {
        var chunk = level.getChunkSource().getChunk(x, z, ChunkStatus.FULL, false);
        return chunk != null && !(chunk instanceof FakeChunk);
    }

    public static List<CachedStorageIndex.Entry> records(FakeChunk chunk) {
        List<CachedStorageIndex.Entry> saved = diskRecords.get(chunk);
        if (saved == null) {
            if (chunk.serializedBlockEntities == null) return List.of();
            saved = BobbyStorageRecords.read(chunk.serializedBlockEntities);
        }
        var result = new ArrayList<CachedStorageIndex.Entry>();
        for (var entry : saved) {
            if (chunk.getLevel().isInValidBounds(new BlockPos(entry.x(), entry.y(), entry.z()))) result.add(entry);
        }
        return result;
    }
}
