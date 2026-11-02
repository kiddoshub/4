
package com.gaspoweredcake.client33.utils.world;

import com.gaspoweredcake.client33.utils.misc.LookaheadIterator;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import static com.gaspoweredcake.client33.Client33.mc;

/** Resolves live block entities through the public 26.2 position API, without a private-map accessor. */
public final class BlockEntityIterator extends LookaheadIterator<BlockEntity> {
    private final ClientLevel level = mc.level;
    private final ChunkIterator chunks = new ChunkIterator(false);
    private Iterator<BlockPos> positions = Collections.emptyIterator();

    @Override protected BlockEntity findNext() {
        if (level == null || level != mc.level) return null;
        while (true) {
            while (positions.hasNext()) {
                BlockPos position = positions.next();
                if (!level.hasChunk(position.getX() >> 4, position.getZ() >> 4)) continue;
                BlockEntity entity = level.getBlockEntity(position);
                if (entity != null && !entity.isRemoved()) return entity;
            }
            if (!chunks.hasNext()) return null;
            positions = new ArrayList<>(chunks.next().getBlockEntitiesPos()).iterator();
        }
    }
}
