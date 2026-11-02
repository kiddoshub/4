
package com.gaspoweredcake.client33.utils.world;

import com.gaspoweredcake.client33.mixin.ClientChunkCacheAccessor;
import com.gaspoweredcake.client33.mixin.ClientChunkMapAccessor;
import com.gaspoweredcake.client33.utils.misc.LookaheadIterator;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunk;
import java.util.concurrent.atomic.AtomicReferenceArray;
import static com.gaspoweredcake.client33.Client33.mc;

/** Iterates the captured world's loaded chunk slots without returning rejected edge chunks. */
public final class ChunkIterator extends LookaheadIterator<ChunkAccess> {
    private final ClientLevel level = mc.level;
    private final AtomicReferenceArray<LevelChunk> slots;
    private final boolean requireNeighbours;
    private int index;

    public ChunkIterator(boolean onlyWithLoadedNeighbours) {
        requireNeighbours = onlyWithLoadedNeighbours;
        slots = level == null ? new AtomicReferenceArray<>(0)
            : ((ClientChunkMapAccessor) (Object) ((ClientChunkCacheAccessor) level.getChunkSource()).client33$getStorage()).client33$getChunks();
    }

    @Override protected ChunkAccess findNext() {
        if (level == null || level != mc.level) return null;
        while (index < slots.length()) {
            LevelChunk candidate = slots.get(index++);
            if (candidate == null) continue;
            int x = candidate.getPos().x(), z = candidate.getPos().z();
            if (!level.hasChunk(x, z) || level.getChunk(x, z) != candidate) continue;
            if (requireNeighbours && !(level.hasChunk(x + 1, z) && level.hasChunk(x - 1, z)
                && level.hasChunk(x, z + 1) && level.hasChunk(x, z - 1))) continue;
            return candidate;
        }
        return null;
    }
}
