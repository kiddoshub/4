
package com.gaspoweredcake.client33.mixininterface;

import net.minecraft.core.BlockPos;

public interface IAABB {
    void client33$expand(double v);

    void client33$set(double x1, double y1, double z1, double x2, double y2, double z2);

    default void client33$set(BlockPos pos) {
        client33$set(pos.getX(), pos.getY(), pos.getZ(), pos.getX() + 1, pos.getY() + 1, pos.getZ() + 1);
    }
}
