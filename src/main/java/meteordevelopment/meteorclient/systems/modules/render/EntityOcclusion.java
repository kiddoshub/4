package meteordevelopment.meteorclient.systems.modules.render;

import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;

public class EntityOcclusion {
    private static final MinecraftClient mc = MinecraftClient.getInstance();

    public static float visibleFraction(Entity entity, boolean strictOcclusion) {
        if (mc.world == null || mc.player == null) return 1.0f;

        Box box = entity.getBoundingBox();
        Vec3d eyePos = mc.gameRenderer.getCamera().getPos();

        Vec3d[] samples = new Vec3d[9];
        // 8 corners
        samples[0] = new Vec3d(box.minX, box.minY, box.minZ);
        samples[1] = new Vec3d(box.maxX, box.minY, box.minZ);
        samples[2] = new Vec3d(box.minX, box.minY, box.maxZ);
        samples[3] = new Vec3d(box.maxX, box.minY, box.maxZ);
        samples[4] = new Vec3d(box.minX, box.maxY, box.minZ);
        samples[5] = new Vec3d(box.maxX, box.maxY, box.minZ);
        samples[6] = new Vec3d(box.minX, box.maxY, box.maxZ);
        samples[7] = new Vec3d(box.maxX, box.maxY, box.maxZ);
        // center
        samples[8] = box.getCenter();

        int visible = 0;
        int total = samples.length;

        for (Vec3d sample : samples) {
            if (isVisible(eyePos, sample, strictOcclusion)) {
                visible++;
            }
        }

        return total == 0 ? 1.0f : (float) visible / (float) total;
    }

    private static boolean isVisible(Vec3d from, Vec3d to, boolean strictOcclusion) {
        if (mc.world == null) return true;

        RaycastContext context = new RaycastContext(
            from,
            to,
            strictOcclusion ? RaycastContext.ShapeType.COLLIDER : RaycastContext.ShapeType.OUTLINE,
            RaycastContext.FluidHandling.NONE,
            mc.player
        );

        BlockHitResult result = mc.world.raycast(context);
        if (result.getType() == HitResult.Type.MISS) {
            return true;
        }

        // Check if hit block is occluding
        BlockPos pos = result.getBlockPos();
        BlockState state = mc.world.getBlockState(pos);

        if (!strictOcclusion) {
            // Normal occlusion - solid blocks occlude
            if (state.isSolidBlock(mc.world, pos)) {
                // Check if it's between from and to
                double distToHit = result.getPos().distanceTo(from);
                double distToSample = to.distanceTo(from);
                if (distToHit < distToSample - 1e-6) {
                    return false;
                }
            }
            return true;
        } else {
            // Strict occlusion - any non-FULL_CUBE shape and any non-solid collision shape occludes
            if (!state.isFullCube(mc.world, pos) || !state.isSolid()) {
                // Also check collision shape
                if (!state.getCollisionShape(mc.world, pos, ShapeContext.of(mc.player)).isEmpty()) {
                    double distToHit = result.getPos().distanceTo(from);
                    double distToSample = to.distanceTo(from);
                    if (distToHit < distToSample - 1e-6) {
                        return false;
                    }
                } else {
                    double distToHit = result.getPos().distanceTo(from);
                    double distToSample = to.distanceTo(from);
                    if (distToHit < distToSample - 1e-6) {
                        return false;
                    }
                }
            } else if (state.isSolidBlock(mc.world, pos)) {
                double distToHit = result.getPos().distanceTo(from);
                double distToSample = to.distanceTo(from);
                if (distToHit < distToSample - 1e-6) {
                    return false;
                }
            }
            return true;
        }
    }
}
