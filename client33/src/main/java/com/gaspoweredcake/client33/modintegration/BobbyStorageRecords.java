package com.gaspoweredcake.client33.modintegration;

import com.gaspoweredcake.client33.utils.render.CachedStorageIndex;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;

import java.util.ArrayList;
import java.util.List;

/** Extracts only coordinates and block-entity type; inventory data stays with Bobby. */
public final class BobbyStorageRecords {
    private BobbyStorageRecords() {}

    public static List<CachedStorageIndex.Entry> read(ListTag tags) {
        var result = new ArrayList<CachedStorageIndex.Entry>();
        for (var value : tags) {
            if (!(value instanceof CompoundTag tag)) continue;
            var x = tag.getInt("x"); var y = tag.getInt("y"); var z = tag.getInt("z");
            var type = tag.getString("id");
            if (x.isPresent() && y.isPresent() && z.isPresent() && type.isPresent() && !type.get().isBlank())
                result.add(new CachedStorageIndex.Entry(x.get(), y.get(), z.get(), type.get()));
        }
        return List.copyOf(result);
    }
}
