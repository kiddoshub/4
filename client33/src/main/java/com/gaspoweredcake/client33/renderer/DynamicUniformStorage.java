package com.gaspoweredcake.client33.renderer;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.renderpearl.api.buffers.GpuBufferSlice;
import net.minecraft.client.renderer.MappableRingBuffer;
import java.nio.ByteBuffer;
import java.util.*;

/** Frame-local immutable uniform cache backed by aligned RenderPearl ring-buffer pages. */
public final class DynamicUniformStorage<T extends DynamicUniformStorage.DynamicUniform> implements AutoCloseable {
    public interface DynamicUniform { void write(ByteBuffer buffer); }
    private final String name;
    private final int stride,capacity;
    private final List<MappableRingBuffer> pages=new ArrayList<>();
    private final Map<T,GpuBufferSlice> values=new HashMap<>();
    private int writes;
    private boolean closed;

    public DynamicUniformStorage(String name,int blockSize,int capacity) {
        if(blockSize<=0||capacity<=0)throw new IllegalArgumentException("Uniform dimensions must be positive");
        this.name=Objects.requireNonNull(name);this.capacity=capacity;
        int alignment=RenderSystem.getDevice().getDeviceInfo().limits().minUniformOffsetAlignment();
        stride=Math.multiplyExact(Math.floorDiv(Math.addExact(blockSize,alignment-1),alignment),alignment);
    }
    public GpuBufferSlice writeUniform(T value) {
        if(closed)throw new IllegalStateException("Uniform storage is closed");
        GpuBufferSlice cached=values.get(Objects.requireNonNull(value));
        if(cached!=null)return cached;
        int page=writes/capacity,slot=writes%capacity;
        if(page==pages.size())pages.add(new MappableRingBuffer(()->name+" / page "+page,130,Math.multiplyExact(stride,capacity)));
        var slice=pages.get(page).currentBuffer().slice((long)slot*stride,stride);
        try(var mapped=slice.map(false,true)) {value.write(mapped.data());}
        values.put(value,slice);writes++;return slice;
    }
    public void endFrame() {
        int used=(writes+capacity-1)/capacity;
        for(int i=0;i<used;i++)pages.get(i).rotate();
        values.clear();writes=0;
    }
    @Override public void close() {
        for(var page:pages)page.close();
        pages.clear();values.clear();writes=0;closed=true;
    }
}
