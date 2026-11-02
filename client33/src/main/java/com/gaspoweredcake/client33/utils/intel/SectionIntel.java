package com.gaspoweredcake.client33.utils.intel;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

/** Detached observation, with null light meaning the client had no section light data. */
public final class SectionIntel {
    public final int chunkX, sectionY, chunkZ;
    public final long capturedNanos, hash;
    public final PackedSection<BlockState> blocks;
    public final LightFusion.Result lightEvidence;
    public final boolean hasEmissiveBlock;
    private final byte[] blockLight, skyLight;
    private final Map<Heightmap.Types, long[]> heightmaps;

    public SectionIntel(int x, int y, int z, long capturedNanos, long hash, PackedSection<BlockState> blocks,
                        byte[] blockLight, byte[] skyLight, Map<Heightmap.Types, long[]> heightmaps,
                        LightFusion.Result lightEvidence, boolean hasEmissiveBlock) {
        this.chunkX = x; this.sectionY = y; this.chunkZ = z; this.capturedNanos = capturedNanos;
        this.hash = hash; this.blocks = blocks; this.lightEvidence = lightEvidence; this.hasEmissiveBlock = hasEmissiveBlock;
        this.blockLight = blockLight == null ? null : blockLight.clone();
        this.skyLight = skyLight == null ? null : skyLight.clone();
        EnumMap<Heightmap.Types, long[]> copy = new EnumMap<>(Heightmap.Types.class);
        heightmaps.forEach((key, value) -> copy.put(key, value.clone()));
        this.heightmaps = Collections.unmodifiableMap(copy);
    }
    public byte[] blockLight() { return blockLight == null ? null : blockLight.clone(); }
    public byte[] skyLight() { return skyLight == null ? null : skyLight.clone(); }
    public Map<Heightmap.Types, long[]> heightmaps() {
        EnumMap<Heightmap.Types, long[]> copy = new EnumMap<>(Heightmap.Types.class);
        heightmaps.forEach((key, value) -> copy.put(key, value.clone()));
        return Collections.unmodifiableMap(copy);
    }
}
