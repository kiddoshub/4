package com.gaspoweredcake.client33.utils.world;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Selects an actual exposed shape face, including for multipart or partial shapes. */
public final class ScaffoldFace {
    private ScaffoldFace() {}

    public static BlockHitResult hit(VoxelShape shape, BlockPos support, Direction face) {
        for (var box : shape.toAabbs()) {
            double x = (box.minX + box.maxX) / 2, y = (box.minY + box.maxY) / 2, z = (box.minZ + box.maxZ) / 2;
            switch (face) {
                case DOWN -> y = box.minY;
                case UP -> y = box.maxY;
                case NORTH -> z = box.minZ;
                case SOUTH -> z = box.maxZ;
                case WEST -> x = box.minX;
                case EAST -> x = box.maxX;
            }
            Vec3 point = new Vec3(support.getX() + x, support.getY() + y, support.getZ() + z);
            Vec3 normal = new Vec3(face.getStepX(), face.getStepY(), face.getStepZ()).scale(0.01);
            BlockHitResult result = shape.clip(point.add(normal), point.subtract(normal), support);
            if (result != null && result.getDirection() == face) return result;
        }
        return null;
    }
}
