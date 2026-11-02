package com.gaspoweredcake.client33.utils.render;

import it.unimi.dsi.fastutil.longs.Long2ByteOpenHashMap;

/** Memoizes chunk liveness for one render frame; the world can change between frames. */
public final class LiveChunkVisibility {
    @FunctionalInterface public interface Lookup { boolean isLive(int x, int z); }
    private final Long2ByteOpenHashMap states = new Long2ByteOpenHashMap();

    public void beginFrame() { states.clear(); }
    public boolean isLive(int x, int z, Lookup lookup) {
        long key = ((long) x << 32) | (z & 0xffffffffL);
        byte value = states.get(key);
        if (value == 0) {
            value = (byte) (lookup.isLive(x, z) ? 1 : 2);
            states.put(key, value);
        }
        return value == 1;
    }
}
