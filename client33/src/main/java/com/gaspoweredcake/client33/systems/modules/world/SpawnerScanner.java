package com.gaspoweredcake.client33.systems.modules.world;

import com.gaspoweredcake.client33.events.game.GameLeftEvent;
import com.gaspoweredcake.client33.events.packets.AppliedSpawnerDataEvent;
import com.gaspoweredcake.client33.events.world.TickEvent;
import com.gaspoweredcake.client33.gui.GuiTheme;
import com.gaspoweredcake.client33.gui.widgets.WWidget;
import com.gaspoweredcake.client33.gui.widgets.containers.WVerticalList;
import com.gaspoweredcake.client33.systems.modules.Categories;
import com.gaspoweredcake.client33.systems.modules.Module;
import com.gaspoweredcake.client33.utils.inventory.SpawnerMenu;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.network.protocol.game.ClientboundForgetLevelChunkPacket;
import net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket;
import net.minecraft.world.level.block.entity.BlockEntityTypes;

import java.util.LinkedHashMap;

/** Lists the keys the server actually sent for loaded spawners. */
public class SpawnerScanner extends Module {
    private final LinkedHashMap<BlockPos, String> observed = new LinkedHashMap<>();
    private Object world;
    private String lastMenuObservation;

    public SpawnerScanner() {
        super(Categories.World, "spawner-scanner", "Lists received spawner block-entity NBT keys for loaded chunks.");
    }

    private void clear() { observed.clear(); lastMenuObservation = null; world = mc.level; }
    @Override public void onActivate() { clear(); }
    @Override public void onDeactivate() { clear(); }
    @EventHandler private void onLeave(GameLeftEvent event) { clear(); }

    @EventHandler private void onPacket(AppliedSpawnerDataEvent event) {
        if (world != mc.level) clear();
        if (mc.level == null) return;
        if (event.packet() instanceof ClientboundLevelChunkWithLightPacket chunk) {
            observed.keySet().removeIf(pos -> pos.getX() >> 4 == chunk.x() && pos.getZ() >> 4 == chunk.z());
            chunk.chunkData().forEachBlockEntityTag(chunk.x(), chunk.z(), (pos, type, tag) -> {
                if (type == BlockEntityTypes.MOB_SPAWNER) record(pos, tag);
            });
        } else if (event.packet() instanceof ClientboundBlockEntityDataPacket update && update.getType() == BlockEntityTypes.MOB_SPAWNER) {
            BlockPos pos = update.getPos();
            if (mc.level.hasChunk(pos.getX() >> 4, pos.getZ() >> 4)) record(pos, update.getTag());
        } else if (event.packet() instanceof ClientboundForgetLevelChunkPacket unload) {
            observed.keySet().removeIf(pos -> pos.getX() >> 4 == unload.pos().x() && pos.getZ() >> 4 == unload.pos().z());
        }
    }

    @EventHandler private void onTick(TickEvent.Post event) {
        if (world != mc.level) clear();
        if (mc.level != null) observed.keySet().removeIf(pos -> !mc.level.hasChunk(pos.getX() >> 4, pos.getZ() >> 4));
        if (mc.player != null && mc.gui.screen() instanceof AbstractContainerScreen<?> screen
            && screen.getMenu() == mc.player.containerMenu) {
            String observation = SpawnerMenu.read(screen);
            if (observation != null) lastMenuObservation = observation;
        }
    }

    private void record(BlockPos pos, CompoundTag tag) {
        String keys = tag == null ? "no NBT" : String.join(", ", tag.keySet().stream().sorted().limit(32).toList());
        if (keys.isEmpty()) keys = "no keys";
        if (keys.length() > 320) keys = keys.substring(0, 320) + "…";
        observed.remove(pos);
        if (observed.size() >= 256) observed.remove(observed.keySet().iterator().next());
        observed.put(pos.immutable(), keys);
    }

    @Override public WWidget getWidget(GuiTheme theme) {
        WVerticalList list = theme.verticalList();
        list.add(theme.label("Last matching open menu: " + (lastMenuObservation == null ? "none observed" : lastMenuObservation), 360));
        list.add(theme.label("Menu observations are not linked to a spawner position.", 360));
        list.add(theme.label("Received NBT keys only; missing keys do not establish access.", 360));
        list.add(theme.label("Observations start when new chunk or block-entity packets arrive.", 360));
        for (var entry : observed.entrySet()) {
            BlockPos pos = entry.getKey();
            list.add(theme.label(pos.getX() + ", " + pos.getY() + ", " + pos.getZ() + ": " + entry.getValue(), 360));
        }
        if (observed.isEmpty()) list.add(theme.label("No spawner NBT received yet."));
        return list;
    }

    @Override public String getInfoString() { return observed.size() + " observed"; }
}
