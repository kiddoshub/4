package com.gaspoweredcake.client33;

import com.gaspoweredcake.client33.utils.world.ScaffoldFace;
import com.gaspoweredcake.client33.utils.player.RotationStep;
import com.gaspoweredcake.client33.utils.player.RotationQueue;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.shapes.Shapes;

public final class ScaffoldFaceRegression {
    public static void run() {
        var pos = new BlockPos(100, 64, -50);
        var slab = Shapes.box(0, 0, 0, 1, .5, 1);
        var top = ScaffoldFace.hit(slab, pos, Direction.UP);
        check(top != null && top.getDirection() == Direction.UP && Math.abs(top.getLocation().y - 64.5) < 1e-5, "Slab hit uses the real top face");
        var multipart = Shapes.or(Shapes.box(0, 0, 0, .25, 1, .25), Shapes.box(.75, 0, .75, 1, 1, 1));
        var hit = ScaffoldFace.hit(multipart, pos, Direction.UP);
        check(hit != null && hit.getDirection() == Direction.UP && (hit.getLocation().x < 100.25 || hit.getLocation().x > 100.75), "Multipart support is hit on a component, not the empty center");
        check(ScaffoldFace.hit(Shapes.empty(), pos, Direction.UP) == null, "Empty shapes have no support face");
        var step = RotationStep.toward(0, 0, 100, 80, 45);
        check(step.yaw() == 45 && step.pitch() == 45 && !step.ready(), "Rotation advances by at most the configured step");
        var next = RotationStep.toward(step.yaw(), step.pitch(), 100, 80, 45);
        check(next.yaw() == 90 && next.pitch() == 80 && !next.ready(), "Rotation continues over multiple ticks");
        var last = RotationStep.toward(next.yaw(), next.pitch(), 100, 80, 45);
        check(last.yaw() == 100 && last.pitch() == 80 && last.ready(), "Placement becomes ready only on the final step");
        var wrapped = RotationStep.toward(179, 0, -179, 0, 45);
        check(wrapped.yaw() == 181 && wrapped.ready(), "Yaw uses the short path across the angle boundary");
        var queue = new RotationQueue<String>();
        queue.add("first", 5); queue.add("second", 5); queue.add("lower", 1);
        String inFlight = queue.poll();
        queue.add("new high", 10);
        check(inFlight.equals("first") && queue.poll().equals("new high") && queue.poll().equals("second")
            && queue.poll().equals("lower") && queue.isEmpty(), "In-flight rotation survives new higher-priority requests; ties stay ordered");
        System.out.println("Scaffold support face regressions passed.");
    }
    private static void check(boolean value, String message) { if (!value) throw new AssertionError(message); }
}
