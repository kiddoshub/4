package com.gaspoweredcake.client33.utils.intel;

import com.gaspoweredcake.client33.Client33;
import com.gaspoweredcake.client33.events.game.GameLeftEvent;
import com.gaspoweredcake.client33.events.entity.EntityAddedEvent;
import com.gaspoweredcake.client33.events.entity.EntityRemovedEvent;
import com.gaspoweredcake.client33.events.packets.AppliedSpawnerDataEvent;
import com.gaspoweredcake.client33.events.packets.SlotPacketObservedEvent;
import com.gaspoweredcake.client33.events.world.AppliedBlockUpdateEvent;
import com.gaspoweredcake.client33.events.world.AppliedEntityMetadataEvent;
import com.gaspoweredcake.client33.events.world.EntityMetadataIntelEvent;
import com.gaspoweredcake.client33.events.world.BlockEntityIntelEvent;
import com.gaspoweredcake.client33.events.world.BlockEntityPresenceEvent;
import com.gaspoweredcake.client33.events.world.ChunkDataEvent;
import com.gaspoweredcake.client33.events.world.SectionIntelEvent;
import com.gaspoweredcake.client33.events.world.ServerPositionEvent;
import com.gaspoweredcake.client33.events.world.TickEvent;
import com.gaspoweredcake.client33.utils.PreInit;
import com.gaspoweredcake.client33.mixin.SynchedEntityDataAccessor;
import io.netty.buffer.Unpooled;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.Entity;
import com.gaspoweredcake.client33.utils.network.Client33Executor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.network.protocol.game.ClientboundContainerSetContentPacket;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket;
import net.minecraft.network.protocol.game.ClientboundForgetLevelChunkPacket;
import net.minecraft.network.protocol.game.ClientboundLightUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundSectionBlocksUpdatePacket;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.NbtIo;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.Optional;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import static com.gaspoweredcake.client33.Client33.mc;

/** Bounded shared acquisition pipeline; live game objects are read only on the client thread. */
public final class IntelBus {
    private static final IntelBus INSTANCE = new IntelBus();
    private static final int MAX_PENDING = 512, MAX_CACHE = 1024, MAX_IN_FLIGHT = 8;
    private static volatile int sectionBudget = 2;
    private static volatile boolean persistBlockEntities = true;
    private record Key(int x, int y, int z) {}
    private record Pending(Key key, LevelChunk chunk, long revision) {}
    private record Finished(Key key, long revision, SectionIntel data) {}
    private final LinkedHashMap<Key, Pending> pending = new LinkedHashMap<>();
    private final LinkedHashMap<Key, Long> versions = new LinkedHashMap<>();
    private final LinkedHashMap<Key, SectionIntel> latest = new LinkedHashMap<>();
    private final SectionFingerprintHistory fingerprints = new SectionFingerprintHistory(4096, 128);
    private final LinkedHashMap<BlockPos, BlockEntityPresence> blockEntities = new LinkedHashMap<>();
    private final ArrayBlockingQueue<Finished> finished = new ArrayBlockingQueue<>(128);
    private final AtomicInteger inFlight = new AtomicInteger();
    private final AtomicLong droppedSections = new AtomicLong();
    private record SlotJob(long epoch, int container, int state, int firstSlot, boolean full, java.util.List<ItemStack> items, RegistryAccess.Frozen registries, long nanos) {}
    private final Object slotLock = new Object();
    private final ArrayBlockingQueue<SlotJob> slotQueue = new ArrayBlockingQueue<>(128);
    private final SlotDataStore slotStore = new SlotDataStore(8192);
    private final AtomicBoolean slotWorker = new AtomicBoolean();
    private final AtomicLong droppedSlots = new AtomicLong();
    private long slotEpoch;
    private record PositionJob(long version, PositionHistory.Position position) {}
    private final Object positionLock = new Object();
    private final LinkedHashMap<Integer, Long> positionVersions = new LinkedHashMap<>();
    private final PositionHistory positions = new PositionHistory(1024, 32);
    private final ArrayBlockingQueue<PositionJob> positionQueue = new ArrayBlockingQueue<>(2048);
    private final AtomicBoolean positionWorker = new AtomicBoolean();
    private final AtomicLong droppedPositions = new AtomicLong();
    private record MetadataJob(long revision, EntityMetadataIntel data) {}
    private final LinkedHashMap<Integer, Long> metadataVersions = new LinkedHashMap<>();
    private final LinkedHashMap<Integer, EntityMetadataIntel> metadata = new LinkedHashMap<>();
    private final ArrayBlockingQueue<MetadataJob> metadataQueue = new ArrayBlockingQueue<>(256);
    private final ArrayBlockingQueue<MetadataJob> metadataReady = new ArrayBlockingQueue<>(256);
    private final AtomicBoolean metadataWorker = new AtomicBoolean();
    private final AtomicLong droppedMetadata = new AtomicLong();
    private record RawItem(long epoch, PacketForensics.Packet packet) {}
    private final Object rawLock = new Object();
    private final ArrayBlockingQueue<RawItem> rawQueue = new ArrayBlockingQueue<>(2048);
    private final PacketForensics forensics = new PacketForensics(2048);
    private final AtomicBoolean processingRaw = new AtomicBoolean();
    private final AtomicLong droppedRaw = new AtomicLong();
    private long rawEpoch;
    private final BlockEntityStore store = new BlockEntityStore(Client33.FOLDER.toPath().resolve("intel").resolve("block-entities.sqlite3"));
    private String serverKey;
    private long revision;
    private Object world;

    private IntelBus() {}
    @PreInit public static void init() { Client33.EVENT_BUS.subscribe(INSTANCE); }

    public static java.util.List<SectionIntel> sections() {
        if (!mc.isSameThread()) throw new IllegalStateException("IntelBus cache is client-thread only");
        return java.util.List.copyOf(INSTANCE.latest.values());
    }

    public static java.util.List<SectionFingerprintHistory.Change> sectionChanges() {
        if (!mc.isSameThread()) throw new IllegalStateException("IntelBus cache is client-thread only");
        return INSTANCE.fingerprints.changes();
    }

    /** Most recent non-invalidated observation, or null. Call on the client thread. */
    public static SectionIntel section(int x, int y, int z) {
        if (!mc.isSameThread()) throw new IllegalStateException("IntelBus cache is client-thread only");
        return INSTANCE.latest.get(new Key(x, y, z));
    }

    private void reset() {
        pending.clear(); versions.clear(); latest.clear(); finished.clear(); blockEntities.clear(); fingerprints.clear();
        metadataVersions.clear(); metadata.clear(); metadataQueue.clear(); metadataReady.clear();
        synchronized (positionLock) { positionVersions.clear(); positions.clear(); positionQueue.clear(); }
        synchronized (slotLock) { slotEpoch++; slotQueue.clear(); slotStore.clear(); }
        world = mc.level; serverKey = null;
        revision++; // Older workers cannot publish into a later world.
    }
    @EventHandler private void onLeave(GameLeftEvent event) {
        reset();
        synchronized (rawLock) { rawEpoch++; rawQueue.clear(); forensics.clear(); }
    }

    /** Called by the Netty observer; processing is handed off to the shared worker pool. */
    public static void rawPacket(PacketForensics.Packet packet) {
        synchronized (INSTANCE.rawLock) {
            if (!INSTANCE.rawQueue.offer(new RawItem(INSTANCE.rawEpoch, packet))) { INSTANCE.droppedRaw.incrementAndGet(); return; }
        }
        INSTANCE.scheduleRaw();
    }
    public static PacketForensics packetForensics() { return INSTANCE.forensics; }
    public static long droppedRawPackets() { return INSTANCE.droppedRaw.get(); }
    public static long droppedSectionObservations() { return INSTANCE.droppedSections.get(); }
    public static void rawFrameDropped() { INSTANCE.droppedRaw.incrementAndGet(); }

    private void scheduleRaw() {
        if (!processingRaw.compareAndSet(false, true)) return;
        try { Client33Executor.execute(this::drainRaw); }
        catch (RuntimeException failure) { processingRaw.set(false); Client33.LOG.error("Failed to schedule packet observations", failure); }
    }
    private void drainRaw() {
        try {
            RawItem item;
            while ((item = rawQueue.poll()) != null) {
                synchronized (rawLock) { if (item.epoch == rawEpoch) forensics.add(item.packet); }
            }
        } finally {
            processingRaw.set(false);
            if (!rawQueue.isEmpty()) scheduleRaw();
        }
    }

    @EventHandler private void onChunk(ChunkDataEvent event) {
        if (world != mc.level) reset();
        LevelChunk chunk = event.chunk();
        for (int i = 0; i < chunk.getSections().length; i++) enqueue(chunk, chunk.getSectionYFromSectionIndex(i));
    }

    @EventHandler private void onBlocks(AppliedBlockUpdateEvent event) {
        if (world != mc.level) reset();
        if (mc.level == null) return;
        if (event.packet() instanceof ClientboundBlockUpdatePacket block) {
            invalidateBlockEntity(block.getPos(), block.getBlockState());
            changed(block.getPos());
        }
        else if (event.packet() instanceof ClientboundSectionBlocksUpdatePacket section) {
            // All entries are in one section. Coalescing avoids repeated snapshots.
            ArrayList<BlockPos> one = new ArrayList<>(1);
            section.runUpdates((pos, state) -> { invalidateBlockEntity(pos, state); if (one.isEmpty()) one.add(pos.immutable()); });
            if (!one.isEmpty()) changed(one.getFirst());
        } else if (event.packet() instanceof ClientboundLightUpdatePacket light && mc.level.hasChunk(light.x(), light.z())) {
            LevelChunk chunk = mc.level.getChunk(light.x(), light.z());
            for (int i = 0; i < chunk.getSections().length; i++) enqueue(chunk, chunk.getSectionYFromSectionIndex(i));
        }
    }

    public static java.util.List<SlotDataStore.Slot> slots() { return INSTANCE.slotStore.snapshot(); }
    public static Optional<SlotDataStore.Slot> slot(int container, int slot) { return INSTANCE.slotStore.get(container, slot); }
    public static long droppedSlotObservations() { return INSTANCE.droppedSlots.get(); }

    @EventHandler private void onSlotPacket(SlotPacketObservedEvent event) {
        if (world != mc.level) reset();
        if (mc.getConnection() == null) return;
        int container, state, first; boolean full; java.util.List<ItemStack> items = new ArrayList<>();
        if (event.packet() instanceof ClientboundContainerSetContentPacket content) {
            container = content.containerId(); state = content.stateId(); first = 0; full = true;
            for (ItemStack item : content.items()) {
                if (items.size() >= 512) { droppedSlots.incrementAndGet(); break; }
                items.add(item.copy());
            }
            items.add(content.carriedItem().copy());
        } else if (event.packet() instanceof ClientboundContainerSetSlotPacket update) {
            container = update.getContainerId(); state = update.getStateId(); first = update.getSlot(); full = false;
            items.add(update.getItem().copy());
        } else return;
        synchronized (slotLock) {
            SlotJob job = new SlotJob(slotEpoch, container, state, first, full, items, mc.getConnection().registryAccess(), System.nanoTime());
            if (!slotQueue.offer(job)) { droppedSlots.incrementAndGet(); return; }
        }
        scheduleSlots();
    }
    private void scheduleSlots() {
        if (!slotWorker.compareAndSet(false, true)) return;
        try { Client33Executor.execute(this::drainSlots); }
        catch (RuntimeException failure) { slotWorker.set(false); Client33.LOG.error("Failed to schedule slot observations", failure); }
    }
    private void drainSlots() {
        try {
            SlotJob job;
            while ((job = slotQueue.poll()) != null) {
                try {
                    var values = new ArrayList<SlotDataStore.Slot>(job.items.size());
                    for (int i = 0; i < job.items.size(); i++) {
                        int slot = job.full && i == job.items.size() - 1 ? -1 : job.firstSlot + i;
                        values.add(snapshotSlot(job, slot, job.items.get(i)));
                    }
                    synchronized (slotLock) {
                        if (job.epoch != slotEpoch) continue;
                        if (job.full) slotStore.replace(job.container, values);
                        else for (var value : values) slotStore.put(value);
                    }
                } catch (RuntimeException failure) {
                    droppedSlots.incrementAndGet();
                    Client33.LOG.error("Failed to process slot observations", failure);
                }
            }
        } finally { slotWorker.set(false); if (!slotQueue.isEmpty()) scheduleSlots(); }
    }
    private SlotDataStore.Slot snapshotSlot(SlotJob job, int slot, ItemStack item) {
        var itemId = item.isEmpty() ? null : BuiltInRegistries.ITEM.getKey(item.getItem());
        String id = itemId == null ? "" : itemId.toString();
        var custom = item.get(DataComponents.CUSTOM_NAME);
        String name = custom == null ? "" : custom.getString();
        boolean truncated = name.length() > 512;
        if (truncated) name = name.substring(0, 512);
        var lore = new ArrayList<String>();
        var itemLore = item.get(DataComponents.LORE);
        if (itemLore != null) for (var line : itemLore.lines()) {
            if (lore.size() >= 64) { truncated = true; break; }
            String text = line.getString();
            if (text.length() > 512) { text = text.substring(0, 512); truncated = true; }
            lore.add(text);
        }
        Map<String, Integer> enchantments = new LinkedHashMap<>();
        for (var entry : item.getEnchantments().entrySet()) {
            if (enchantments.size() >= 64) { truncated = true; break; }
            String key = entry.getKey().unwrapKey().map(k -> k.identifier().toString()).orElse("unregistered");
            enchantments.put(key, entry.getIntValue());
        }
        byte[] nbt = new byte[0];
        if (!item.isEmpty()) try {
            var tag = ItemStack.CODEC.encodeStart(job.registries.createSerializationContext(NbtOps.INSTANCE), item).result().orElse(null);
            if (tag == null || tag.sizeInBytes() > 1_048_576) truncated = true;
            else {
                var output = new ByteArrayOutputStream();
                try (var data = new DataOutputStream(output)) { NbtIo.writeAnyTag(tag, data); }
                nbt = output.toByteArray();
            }
        } catch (Exception failure) { truncated = true; }
        return new SlotDataStore.Slot(job.container, slot, job.state, id, item.getCount(), item.getDamageValue(), name, lore, enchantments, nbt, job.nanos, truncated);
    }

    public static java.util.List<PositionHistory.Position> positionsNear(double x, double y, double z, double range) { return INSTANCE.positions.nearby(x, y, z, range); }
    public static java.util.List<PositionHistory.Position> positionHistory(int entityId) { return INSTANCE.positions.get(entityId); }
    public static long droppedPositionObservations() { return INSTANCE.droppedPositions.get(); }

    @EventHandler private void onServerPosition(ServerPositionEvent event) {
        if (world != mc.level) reset();
        Entity entity = event.entity();
        if (mc.level == null || entity == null) return;
        var type = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
        var pos = event.position();
        var sample = new PositionHistory.Position(entity.getId(), entity.getUUID(), type == null ? "unknown" : type.toString(),
            pos.x, pos.y, pos.z, event.source(), event.capturedNanos());
        synchronized (positionLock) {
            Long version = positionVersions.get(entity.getId());
            if (version == null) { version = ++revision; positionVersions.put(entity.getId(), version); }
            if (!positionQueue.offer(new PositionJob(version, sample))) { droppedPositions.incrementAndGet(); return; }
            while (positionVersions.size() > 1024) {
                Integer oldest = positionVersions.keySet().iterator().next(); positionVersions.remove(oldest); positions.removeId(oldest);
            }
        }
        schedulePositions();
    }
    private void schedulePositions() {
        if (!positionWorker.compareAndSet(false, true)) return;
        try { Client33Executor.execute(this::drainPositions); }
        catch (RuntimeException failure) { positionWorker.set(false); Client33.LOG.error("Failed to schedule position observations", failure); }
    }
    private void drainPositions() {
        try {
            PositionJob item;
            while ((item = positionQueue.poll()) != null) synchronized (positionLock) {
                if (Long.valueOf(item.version).equals(positionVersions.get(item.position.id()))) positions.add(item.position);
            }
        } finally { positionWorker.set(false); if (!positionQueue.isEmpty()) schedulePositions(); }
    }

    /** Serialized tracker values for an entity ID, when received in the current world. */
    public static EntityMetadataIntel metadata(int entityId) {
        if (!mc.isSameThread()) throw new IllegalStateException("IntelBus cache is client-thread only");
        return INSTANCE.metadata.get(entityId);
    }
    public static long droppedMetadataObservations() { return INSTANCE.droppedMetadata.get(); }

    @EventHandler private void onEntityAdded(EntityAddedEvent event) {
        if (world != mc.level) reset();
        if (mc.level != null && event.entity != null) {
            synchronized (positionLock) {
                positionVersions.put(event.entity.getId(), ++revision);
                while (positionVersions.size() > 1024) {
                    int oldest = positionVersions.keySet().iterator().next(); positionVersions.remove(oldest); positions.removeId(oldest);
                }
            }
            captureMetadata(event.entity, "entity-added");
        }
    }
    @EventHandler private void onEntityRemoved(EntityRemovedEvent event) {
        if (event.entity == null) return;
        metadataVersions.remove(event.entity.getId()); metadata.remove(event.entity.getId());
        synchronized (positionLock) { positionVersions.remove(event.entity.getId()); positions.remove(event.entity.getId(), event.entity.getUUID()); }
    }
    @EventHandler private void onMetadata(AppliedEntityMetadataEvent event) {
        if (world != mc.level) reset();
        if (mc.level == null) return;
        Entity entity = mc.level.getEntity(event.packet().id());
        if (entity != null) captureMetadata(entity, "server-metadata");
    }
    private void captureMetadata(Entity entity, String source) {
        var items = ((SynchedEntityDataAccessor) entity.getEntityData()).client33$getItemsById();
        var values = new ArrayList<EntityMetadataIntel.Value>(items.length);
        int skipped = 0;
        for (var item : items) {
            if (item == null) continue;
            var buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(64, 65536), mc.level.registryAccess());
            try {
                var value = item.value();
                value.write(buffer);
                byte[] bytes = new byte[buffer.readableBytes()]; buffer.getBytes(buffer.readerIndex(), bytes);
                values.add(new EntityMetadataIntel.Value(value.id(), bytes));
            } catch (RuntimeException failure) { skipped++; }
            finally { buffer.release(); }
        }
        int id = entity.getId(); long version = ++revision;
        var type = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
        var data = new EntityMetadataIntel(id, entity.getUUID(), type == null ? "unknown" : type.toString(), System.nanoTime(), source, values, skipped);
        metadataVersions.put(id, version); metadata.remove(id);
        if (!metadataQueue.offer(new MetadataJob(version, data))) { metadataVersions.remove(id); droppedMetadata.incrementAndGet(); return; }
        while (metadataVersions.size() > 1024) {
            int oldest = metadataVersions.keySet().iterator().next(); metadataVersions.remove(oldest); metadata.remove(oldest);
        }
        scheduleMetadata();
    }
    private void scheduleMetadata() {
        if (!metadataWorker.compareAndSet(false, true)) return;
        try { Client33Executor.execute(this::drainMetadata); }
        catch (RuntimeException failure) { metadataWorker.set(false); Client33.LOG.error("Failed to schedule metadata observations", failure); }
    }
    private void drainMetadata() {
        try {
            MetadataJob item;
            while ((item = metadataQueue.poll()) != null) if (!metadataReady.offer(item)) droppedMetadata.incrementAndGet();
        } finally { metadataWorker.set(false); if (!metadataQueue.isEmpty()) scheduleMetadata(); }
    }

    private String serverKey() {
        if (serverKey != null) return serverKey;
        String source = null;
        if (mc.isLocalServer() && mc.getSingleplayerServer() != null)
            source = "local:" + mc.getSingleplayerServer().getWorldPath(LevelResource.ROOT).toAbsolutePath().normalize();
        else if (mc.getCurrentServer() != null) source = "server:" + mc.getCurrentServer().ip;
        else if (mc.getConnection() != null && mc.getConnection().getConnection().getRemoteAddress() != null)
            source = "remote:" + mc.getConnection().getConnection().getRemoteAddress();
        if (source == null) return null;
        try { serverKey = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(source.getBytes(StandardCharsets.UTF_8))); }
        catch (NoSuchAlgorithmException impossible) { throw new IllegalStateException(impossible); }
        return serverKey;
    }

    /** Reads the current server and dimension cache; completion runs on the worker pool. */
    public static CompletableFuture<Optional<BlockEntityStore.Entry>> blockEntity(BlockPos pos) {
        if (!mc.isSameThread()) throw new IllegalStateException("Use the client thread to select a world");
        if (mc.level == null || INSTANCE.world != mc.level || INSTANCE.serverKey() == null)
            return CompletableFuture.failedFuture(new IllegalStateException("No identified world"));
        return INSTANCE.store.get(INSTANCE.serverKey(), mc.level.dimension().identifier().toString(), pos.getX(), pos.getY(), pos.getZ());
    }

    public static long rejectedBlockEntityObservations() { return INSTANCE.store.rejected(); }
    public static void setBlockEntityPersistence(boolean enabled) { persistBlockEntities = enabled; }
    public static void setSectionBudget(int budget) {
        if (budget < 1 || budget > 32) throw new IllegalArgumentException("Section budget must be between 1 and 32");
        sectionBudget = budget;
    }

    /** Packet-advertised block entities in one loaded chunk; no world block-entity map is scanned. */
    public static java.util.List<BlockEntityPresence> blockEntities(int chunkX, int chunkZ) {
        if (!mc.isSameThread()) throw new IllegalStateException("IntelBus cache is client-thread only");
        var result = new ArrayList<BlockEntityPresence>();
        for (var entry : INSTANCE.blockEntities.values())
            if (entry.pos().getX() >> 4 == chunkX && entry.pos().getZ() >> 4 == chunkZ) result.add(entry);
        return java.util.List.copyOf(result);
    }

    private void invalidateBlockEntity(BlockPos pos, BlockState state) {
        BlockEntityPresence presence = blockEntities.get(pos);
        if (presence != null && !presence.type().isValid(state)) blockEntities.remove(pos);
    }

    private void observedBlockEntity(BlockPos pos, BlockEntityType<?> type, CompoundTag tag) {
        if (mc.level == null || type == null) return;
        long time = System.currentTimeMillis();
        BlockPos keyPos = pos.immutable();
        var presence = new BlockEntityPresence(keyPos, type, time, tag != null);
        blockEntities.remove(keyPos); blockEntities.put(keyPos, presence);
        while (blockEntities.size() > 8192) blockEntities.remove(blockEntities.keySet().iterator().next());
        Client33.EVENT_BUS.post(new BlockEntityPresenceEvent(presence));
        if (tag == null) return;
        String key = serverKey();
        if (key != null && persistBlockEntities) {
            var registryId = BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(type);
            String typeId = registryId == null ? "unknown" : registryId.toString();
            store.offer(key, mc.level.dimension().identifier().toString(), pos.getX(), pos.getY(), pos.getZ(), typeId, time, tag);
        }
        Client33.EVENT_BUS.post(new BlockEntityIntelEvent(pos, type, time, tag));
    }

    private void changed(BlockPos pos) {
        if (mc.level.hasChunk(pos.getX() >> 4, pos.getZ() >> 4)) enqueue(mc.level.getChunk(pos.getX() >> 4, pos.getZ() >> 4), pos.getY() >> 4);
    }

    @EventHandler private void onAppliedPacket(AppliedSpawnerDataEvent event) {
        if (world != mc.level) reset();
        if (mc.level == null) return;
        if (event.packet() instanceof ClientboundLevelChunkWithLightPacket chunk) {
            blockEntities.keySet().removeIf(pos -> pos.getX() >> 4 == chunk.x() && pos.getZ() >> 4 == chunk.z());
            chunk.chunkData().forEachBlockEntityTag(chunk.x(), chunk.z(), this::observedBlockEntity);
        } else if (event.packet() instanceof ClientboundBlockEntityDataPacket block) {
            observedBlockEntity(block.getPos(), block.getType(), block.getTag());
        } else if (event.packet() instanceof ClientboundForgetLevelChunkPacket unload) {
            int x = unload.pos().x(), z = unload.pos().z();
            pending.keySet().removeIf(key -> key.x == x && key.z == z);
            versions.keySet().removeIf(key -> key.x == x && key.z == z);
            latest.keySet().removeIf(key -> key.x == x && key.z == z);
            blockEntities.keySet().removeIf(pos -> pos.getX() >> 4 == x && pos.getZ() >> 4 == z);
        }
    }

    private void enqueue(LevelChunk chunk, int sectionY) {
        int index = chunk.getSectionIndexFromSectionY(sectionY);
        if (index < 0 || index >= chunk.getSections().length) return;
        Key key = new Key(chunk.getPos().x(), sectionY, chunk.getPos().z());
        long version = ++revision;
        latest.remove(key); versions.put(key, version);
        pending.remove(key); pending.put(key, new Pending(key, chunk, version));
        while (pending.size() > MAX_PENDING) {
            Key oldest = pending.keySet().iterator().next();
            pending.remove(oldest); versions.remove(oldest); latest.remove(oldest); droppedSections.incrementAndGet();
        }
        while (versions.size() > MAX_CACHE + MAX_PENDING + MAX_IN_FLIGHT) {
            Key oldest = versions.keySet().iterator().next();
            versions.remove(oldest); pending.remove(oldest); latest.remove(oldest);
        }
    }

    @EventHandler private void onTick(TickEvent.Post event) {
        if (world != mc.level) reset();
        if (mc.level == null) return;
        for (int i = 0; i < 128; i++) {
            MetadataJob item = metadataReady.poll();
            if (item == null) break;
            int id = item.data.id();
            if (!Long.valueOf(item.revision).equals(metadataVersions.get(id))) continue;
            Entity live = mc.level.getEntity(id);
            if (live == null || !live.getUUID().equals(item.data.uuid())) continue;
            metadata.remove(id); metadata.put(id, item.data);
            Client33.EVENT_BUS.post(new EntityMetadataIntelEvent(item.data));
        }
        for (int i = 0; i < 128; i++) {
            Finished item = finished.poll();
            if (item == null) break;
            if (!Long.valueOf(item.revision).equals(versions.get(item.key))) continue;
            latest.remove(item.key); latest.put(item.key, item.data);
            fingerprints.observe(new SectionFingerprintHistory.Key(item.key.x, item.key.y, item.key.z), item.data.hash, item.data.capturedNanos);
            while (latest.size() > MAX_CACHE) {
                Key oldest = latest.keySet().iterator().next();
                latest.remove(oldest); versions.remove(oldest);
            }
            Client33.EVENT_BUS.post(new SectionIntelEvent(item.data));
        }
        for (int i = 0; i < sectionBudget && inFlight.get() < MAX_IN_FLIGHT && !pending.isEmpty(); i++) {
            Iterator<Pending> it = pending.values().iterator();
            Pending task = it.next(); it.remove();
            Key key = task.key;
            if (!mc.level.hasChunk(key.x, key.z) || mc.level.getChunk(key.x, key.z) != task.chunk) { versions.remove(key); continue; }
            int index = task.chunk.getSectionIndexFromSectionY(key.y);
            PackedSection<BlockState> snapshot;
            try { snapshot = SectionSnapshot.capture(task.chunk.getSection(index)); }
            catch (RuntimeException failure) {
                versions.remove(key); droppedSections.incrementAndGet();
                Client33.LOG.error("Failed to capture section snapshot", failure);
                continue;
            }
            var lightEngine = mc.level.getChunkSource().getLightEngine();
            SectionPos sectionPos = SectionPos.of(key.x, key.y, key.z);
            var blockData = lightEngine.getLayerListener(LightLayer.BLOCK).getDataLayerData(sectionPos);
            var skyData = lightEngine.getLayerListener(LightLayer.SKY).getDataLayerData(sectionPos);
            byte[] block = blockData == null ? null : blockData.copy().getData();
            byte[] sky = skyData == null ? null : skyData.copy().getData();
            Map<Heightmap.Types, long[]> heights = new EnumMap<>(Heightmap.Types.class);
            for (var entry : task.chunk.getHeightmaps()) heights.put(entry.getKey(), entry.getValue().getRawData().clone());
            int minY = task.chunk.getMinY(), worldHeight = task.chunk.getHeight();
            long captured = System.nanoTime();
            inFlight.incrementAndGet();
            try {
                Client33Executor.execute(() -> {
                    try {
                        long hash = 0xcbf29ce484222325L;
                        for (int n = 0; n < snapshot.size(); n++) { hash ^= Block.getId(snapshot.get(n)); hash *= 0x100000001b3L; }
                        LightFusion.Result evidence = LightFusion.analyze(key.y, minY, worldHeight,
                            heights.get(Heightmap.Types.WORLD_SURFACE), block, sky);
                        boolean emissive = snapshot.maybeHas(state -> state.getLightEmission() > 0);
                        SectionIntel result = new SectionIntel(key.x, key.y, key.z, captured, hash, snapshot, block, sky, heights, evidence, emissive);
                        if (!finished.offer(new Finished(key, task.revision, result))) droppedSections.incrementAndGet();
                    } catch (RuntimeException failure) {
                        droppedSections.incrementAndGet();
                        Client33.LOG.error("Failed to process section snapshot", failure);
                    } finally { inFlight.decrementAndGet(); }
                });
            } catch (RuntimeException failure) {
                inFlight.decrementAndGet(); droppedSections.incrementAndGet();
                Client33.LOG.error("Failed to schedule section snapshot", failure);
            }
        }
    }
}
