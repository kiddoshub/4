
package com.gaspoweredcake.client33.events.world;

import net.minecraft.world.level.chunk.LevelChunk;

/**
 * @author Crosby
 * @implNote Shouldn't be put in a {@link com.gaspoweredcake.client33.utils.misc.Pool} to avoid a race-condition, or in a {@link ThreadLocal} as it is shared between threads.
 */
public record ChunkDataEvent(LevelChunk chunk) {
}
