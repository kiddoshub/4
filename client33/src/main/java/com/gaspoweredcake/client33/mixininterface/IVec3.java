
package com.gaspoweredcake.client33.mixininterface;

import net.minecraft.core.Vec3i;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3d;

@SuppressWarnings("UnusedReturnValue")
public interface IVec3 {
    Vec3 client33$set(double x, double y, double z);

    default Vec3 client33$set(Vec3i vec) {
        return client33$set(vec.getX(), vec.getY(), vec.getZ());
    }

    default Vec3 client33$set(Vector3d vec) {
        return client33$set(vec.x, vec.y, vec.z);
    }

    default Vec3 client33$set(Vec3 pos) {
        return client33$set(pos.x, pos.y, pos.z);
    }

    Vec3 client33$setXZ(double x, double z);

    Vec3 client33$setY(double y);
}
