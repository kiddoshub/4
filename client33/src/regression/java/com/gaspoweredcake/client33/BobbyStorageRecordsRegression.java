package com.gaspoweredcake.client33;

import com.gaspoweredcake.client33.modintegration.BobbyStorageRecords;
import com.gaspoweredcake.client33.utils.render.CachedStorageIndex;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import java.util.List;

public final class BobbyStorageRecordsRegression {
    public static void run() {
        var tags = new ListTag();
        var chest = new CompoundTag();
        chest.putString("id", "minecraft:chest");
        chest.putInt("x", -4800); chest.putInt("y", -32); chest.putInt("z", 4800);
        chest.putString("Items", "synthetic inventory data");
        tags.add(chest);
        var malformed = new CompoundTag(); malformed.putString("id", "minecraft:barrel");
        malformed.putInt("x", 1); malformed.putInt("y", 2); tags.add(malformed);
        var blank = new CompoundTag(); blank.putString("id", ""); blank.putInt("x", 1);
        blank.putInt("y", 2); blank.putInt("z", 3); tags.add(blank);
        var records = BobbyStorageRecords.read(tags);
        if (!records.equals(List.of(new CachedStorageIndex.Entry(-4800, -32, 4800, "minecraft:chest"))))
            throw new AssertionError("Disk cache extracts only valid detached storage coordinates and types");
        chest.putInt("x", 1);
        if (records.getFirst().x() != -4800) throw new AssertionError("Captured metadata must be detached from NBT");
        if (!BobbyStorageRecords.read(new ListTag()).isEmpty()) throw new AssertionError("Empty cache must be empty");
        System.out.println("Bobby disk storage metadata regressions passed.");
    }
}
