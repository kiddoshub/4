
package com.gaspoweredcake.client33.utils.world;

import com.gaspoweredcake.client33.Client33;
import com.gaspoweredcake.client33.events.game.GameLeftEvent;
import com.gaspoweredcake.client33.events.world.TickEvent;
import com.gaspoweredcake.client33.utils.PreInit;
import com.gaspoweredcake.client33.utils.intel.SectionSnapshot;
import com.gaspoweredcake.client33.utils.Utils;
import meteordevelopment.orbit.EventHandler;
import meteordevelopment.orbit.EventPriority;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import java.util.function.BiConsumer;
import static com.gaspoweredcake.client33.Client33.mc;

/** Shared, same-tick scan adapter. Callback positions are borrowed and must not be retained. */
public final class BlockIterator {
    private static final BlockScanBatch<BlockState> scans = new BlockScanBatch<>(failure ->
        Client33.LOG.error("Block scan callback failed and was removed from the current batch.", failure));
    private static final BlockPos.MutableBlockPos position = new BlockPos.MutableBlockPos();
    private static Object world;
    private static boolean disableCurrent;

    private BlockIterator() {}
    @PreInit public static void init() { Client33.EVENT_BUS.subscribe(BlockIterator.class); }

    private static void alignWorld() {
        if (world != mc.level) { scans.clear(); world = mc.level; }
    }
    @EventHandler private static void onLeave(GameLeftEvent event) { scans.clear(); world = null; }

    @EventHandler(priority = EventPriority.LOWEST - 1)
    private static void onTick(TickEvent.Pre event) {
        alignWorld();
        if (!Utils.canUpdate()) { scans.clear(); return; }
        var level = mc.level;
        scans.run(mc.player.getBlockX(), mc.player.getBlockY(), mc.player.getBlockZ(),
            level.getMinY(), level.getMinY() + level.getHeight(), (x, y, z) -> SectionSnapshot.live(level, position.set(x, y, z)));
    }

    public static void register(int horizontalRadius, int verticalRadius, BiConsumer<BlockPos, BlockState> function) {
        alignWorld();
        scans.register(horizontalRadius, verticalRadius, (x, y, z, state) -> {
            disableCurrent = false;
            function.accept(position.set(x, y, z), state);
            return !disableCurrent;
        });
    }
    public static void disableCurrent() { disableCurrent = true; }
    public static void after(Runnable callback) { alignWorld(); scans.after(callback); }
}
