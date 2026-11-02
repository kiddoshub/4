
package com.gaspoweredcake.client33.systems.modules.render.blockesp;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import com.gaspoweredcake.client33.events.render.Render3DEvent;
import com.gaspoweredcake.client33.events.world.BlockUpdateEvent;
import com.gaspoweredcake.client33.events.world.ChunkDataEvent;
import com.gaspoweredcake.client33.events.world.TickEvent;
import com.gaspoweredcake.client33.renderer.ShapeMode;
import com.gaspoweredcake.client33.settings.*;
import com.gaspoweredcake.client33.systems.modules.Categories;
import com.gaspoweredcake.client33.systems.modules.Module;
import com.gaspoweredcake.client33.utils.Utils;
import com.gaspoweredcake.client33.utils.render.color.RainbowColors;
import com.gaspoweredcake.client33.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.dimension.DimensionType;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.ArrayDeque;
import net.minecraft.client.multiplayer.ClientLevel;

public class BlockESP extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    // General

    private final Setting<List<Block>> blocks = sgGeneral.add(new BlockListSetting.Builder()
        .name("blocks")
        .description("Blocks to search for.")
        .onChanged(_ -> {
            if (isActive() && Utils.canUpdate()) onActivate();
        })
        .build()
    );

    private final Setting<ESPBlockData> defaultBlockConfig = sgGeneral.add(new GenericSetting.Builder<ESPBlockData>()
        .name("default-block-config")
        .description("Default block config.")
        .defaultValue(
            new ESPBlockData(
                ShapeMode.Lines,
                new SettingColor(0, 255, 200),
                new SettingColor(0, 255, 200, 25),
                true,
                new SettingColor(0, 255, 200, 125)
            )
        )
        .build()
    );

    private final Setting<Map<Block, ESPBlockData>> blockConfigs = sgGeneral.add(new BlockDataSetting.Builder<ESPBlockData>()
        .name("block-configs")
        .description("Config for each block.")
        .defaultData(defaultBlockConfig)
        .build()
    );

    private final Setting<Boolean> tracers = sgGeneral.add(new BoolSetting.Builder()
        .name("tracers")
        .description("Render tracer lines.")
        .defaultValue(false)
        .build()
    );

    private final BlockPos.MutableBlockPos blockPos = new BlockPos.MutableBlockPos();

    private final Long2ObjectMap<ESPChunk> chunks = new Long2ObjectOpenHashMap<>();
    private final Set<ESPGroup> groups = new ReferenceOpenHashSet<>();
    private final ArrayDeque<ChunkAccess> pendingChunks = new ArrayDeque<>();
    private ClientLevel scanWorld;

    private DimensionType lastDimension;

    public BlockESP() {
        super(Categories.Render, "block-esp", "Renders specified blocks through walls.", "search");

        RainbowColors.register(this::onTickRainbow);
    }

    @Override
    public void onActivate() {
        synchronized (chunks) {
            pendingChunks.clear();
            chunks.clear();
            groups.clear();
        }

        scanWorld = mc.level;
        for (ChunkAccess chunk : Utils.chunks()) {
            searchChunk(chunk);
        }

        lastDimension = mc.level.dimensionType();
    }

    @Override
    public void onDeactivate() {
        synchronized (chunks) {
            pendingChunks.clear();
            chunks.clear();
            groups.clear();
        }
    }

    private void onTickRainbow() {
        if (!isActive()) return;

        defaultBlockConfig.get().tickRainbow();
        for (ESPBlockData blockData : blockConfigs.get().values()) blockData.tickRainbow();
    }

    ESPBlockData getBlockData(Block block) {
        ESPBlockData blockData = blockConfigs.get().get(block);
        return blockData == null ? defaultBlockConfig.get() : blockData;
    }

    private void updateChunk(int x, int z) {
        ESPChunk chunk = chunks.get(ChunkPos.pack(x, z));
        if (chunk != null) chunk.update();
    }

    private void updateBlock(int x, int y, int z) {
        ESPChunk chunk = chunks.get(ChunkPos.pack(x >> 4, z >> 4));
        if (chunk != null) chunk.update(x, y, z);
    }

    public ESPBlock getBlock(int x, int y, int z) {
        ESPChunk chunk = chunks.get(ChunkPos.pack(x >> 4, z >> 4));
        return chunk == null ? null : chunk.get(x, y, z);
    }

    public ESPGroup newGroup(Block block) {
        synchronized (chunks) {
            ESPGroup group = new ESPGroup(block);
            groups.add(group);
            return group;
        }
    }

    public void removeGroup(ESPGroup group) {
        synchronized (chunks) {
            groups.remove(group);
        }
    }

    @EventHandler
    private void onChunkData(ChunkDataEvent event) {
        searchChunk(event.chunk());
    }

    private void searchChunk(ChunkAccess chunk) {
        pendingChunks.removeIf(queued -> queued.getPos().equals(chunk.getPos()));
        pendingChunks.addLast(chunk);
    }

    private void scanNextChunk() {
        ChunkAccess chunk = pendingChunks.pollFirst();
        if (chunk == null || !isActive() || scanWorld != mc.level
            || mc.level.getChunk(chunk.getPos().x(), chunk.getPos().z(), net.minecraft.world.level.chunk.status.ChunkStatus.FULL, false) != chunk) return;
        ESPChunk schunk = ESPChunk.searchChunk(chunk, blocks.get());
        synchronized (chunks) {
            ESPChunk old = chunks.remove(chunk.getPos().pack());
            if (old != null && old.blocks != null) {
                for (ESPBlock block : old.blocks.values()) {
                    block.group.remove(block, false);
                    block.loaded = false;
                }
            }
            if (schunk.size() > 0) {
                chunks.put(chunk.getPos().pack(), schunk);
                schunk.update();
            }
            updateChunk(chunk.getPos().x() - 1, chunk.getPos().z());
            updateChunk(chunk.getPos().x() + 1, chunk.getPos().z());
            updateChunk(chunk.getPos().x(), chunk.getPos().z() - 1);
            updateChunk(chunk.getPos().x(), chunk.getPos().z() + 1);
        }
    }

    @EventHandler
    private void onBlockUpdate(BlockUpdateEvent event) {
        // Copy the borrowed event position before updating neighbour geometry.
        int bx = event.pos.getX();
        int by = event.pos.getY();
        int bz = event.pos.getZ();

        int chunkX = bx >> 4;
        int chunkZ = bz >> 4;
        long key = ChunkPos.pack(chunkX, chunkZ);

        boolean added = blocks.get().contains(event.newState.getBlock()) && !blocks.get().contains(event.oldState.getBlock());
        boolean removed = !added && !blocks.get().contains(event.newState.getBlock()) && blocks.get().contains(event.oldState.getBlock());

        if (added || removed) {
            synchronized (chunks) {
                ESPChunk chunk = chunks.get(key);

                if (chunk == null) {
                    chunk = new ESPChunk(chunkX, chunkZ);
                    if (chunk.shouldBeDeleted()) return;

                    chunks.put(key, chunk);
                }

                blockPos.set(bx, by, bz);

                if (added) chunk.add(blockPos);
                else chunk.remove(blockPos);

                // Update neighbour blocks
                for (int x = -1; x < 2; x++) {
                    for (int z = -1; z < 2; z++) {
                        for (int y = -1; y < 2; y++) {
                            if (x == 0 && y == 0 && z == 0) continue;

                            updateBlock(bx + x, by + y, bz + z);
                        }
                    }
                }
            }
        }
    }

    @EventHandler
    private void onPostTick(TickEvent.Post event) {
        DimensionType dimension = mc.level.dimensionType();

        if (lastDimension != dimension || scanWorld != mc.level) onActivate();
        scanNextChunk();
        lastDimension = dimension;
    }

    @EventHandler
    private void onRender(Render3DEvent event) {
        synchronized (chunks) {
            for (Iterator<ESPChunk> it = chunks.values().iterator(); it.hasNext(); ) {
                ESPChunk chunk = it.next();

                if (chunk.shouldBeDeleted()) {
                    for (ESPBlock block : chunk.blocks.values()) {
                        block.group.remove(block, false);
                        block.loaded = false;
                    }

                    it.remove();
                } else chunk.render(event);
            }

            if (tracers.get()) {
                for (ESPGroup group : groups) {
                    group.render(event);
                }
            }
        }
    }

    @Override
    public String getInfoString() {
        return "%s groups".formatted(groups.size());
    }
}
