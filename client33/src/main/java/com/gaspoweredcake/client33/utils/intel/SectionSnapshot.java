package com.gaspoweredcake.client33.utils.intel;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.chunk.GlobalPalette;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.Palette;
import net.minecraft.world.level.chunk.PalettedContainer;
import java.util.HashMap;
import java.util.Map;
import static com.gaspoweredcake.client33.Client33.mc;

/** Captures detached section data on the client thread for use on worker threads. */
public final class SectionSnapshot {
    private SectionSnapshot() {}

    /** Same-thread live lookup for shared bulk scans; it preserves vanilla bounds and debug-world handling. */
    public static BlockState live(ClientLevel level, BlockPos pos) {
        if (!mc.isSameThread()) throw new IllegalStateException("Live sections must be read on the client thread");
        if (!level.isInValidBounds(pos)) return Blocks.VOID_AIR.defaultBlockState();
        if (level.isDebug()) return level.getBlockState(pos);
        var chunk = level.getChunk(pos.getX() >> 4, pos.getZ() >> 4);
        int sectionIndex = chunk.getSectionIndex(pos.getY());
        if (sectionIndex < 0 || sectionIndex >= chunk.getSections().length) return Blocks.AIR.defaultBlockState();
        var section = chunk.getSection(sectionIndex);
        if (section.hasOnlyAir()) return Blocks.AIR.defaultBlockState();
        int index = ((pos.getY() & 15) << 8) | ((pos.getZ() & 15) << 4) | (pos.getX() & 15);
        return liveSection(section, index);
    }

    /** Same-thread direct read from an already identified section. */
    public static BlockState liveSection(LevelChunkSection section, int index) {
        if (!mc.isSameThread()) throw new IllegalStateException("Live sections must be read on the client thread");
        if (index < 0 || index >= 4096) throw new IndexOutOfBoundsException(index);
        var data = section.getStates().data;
        var storage = data.storage();
        int bits = storage.getBits();
        int id;
        if (bits == 0) id = 0;
        else {
            int perWord = 64 / bits;
            id = (int) ((storage.getRaw()[index / perWord] >>> ((index % perWord) * bits)) & ((1L << bits) - 1));
        }
        return data.palette().valueFor(id);
    }

    public static PackedSection<BlockState> capture(LevelChunkSection section) {
        if (!mc.isSameThread()) throw new IllegalStateException("Section snapshots must be captured on the client thread");
        PalettedContainer<BlockState> container = section.getStates();
        container.acquire();
        try {
            var data = container.data;
            var storage = data.storage();
            Palette<BlockState> palette = data.palette();
            Map<Integer, BlockState> values = new HashMap<>();
            if (palette instanceof GlobalPalette<?>) {
                // The global palette is a registry, not a list of the section's actual values.
                for (int i = 0; i < storage.getSize(); i++) {
                    int id = storage.get(i);
                    if (!values.containsKey(id)) values.put(id, palette.valueFor(id));
                }
            } else {
                for (int i = 0; i < palette.getSize(); i++) values.put(i, palette.valueFor(i));
            }
            return new PackedSection<>(storage.getRaw(), storage.getBits(), storage.getSize(), values);
        } finally {
            container.release();
        }
    }
}
