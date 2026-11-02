
package com.gaspoweredcake.client33.systems.modules.render;

import com.gaspoweredcake.client33.events.entity.player.InteractBlockEvent;
import com.gaspoweredcake.client33.events.entity.player.InteractEntityEvent;
import com.gaspoweredcake.client33.events.packets.InventoryEvent;
import com.gaspoweredcake.client33.events.packets.ContainerSlotUpdateEvent;
import com.gaspoweredcake.client33.events.render.Render2DEvent;
import com.gaspoweredcake.client33.renderer.Renderer2D;
import com.gaspoweredcake.client33.renderer.text.TextRenderer;
import com.gaspoweredcake.client33.utils.combat.CombatTelemetry;
import com.gaspoweredcake.client33.utils.storage.ContainerObservations;
import com.gaspoweredcake.client33.utils.storage.ContainerSnapshot;
import com.gaspoweredcake.client33.utils.render.NametagUtils;
import com.gaspoweredcake.client33.utils.render.StoragePillar;
import com.gaspoweredcake.client33.utils.render.StorageLocator;
import com.gaspoweredcake.client33.utils.render.CachedStorageIndex;
import com.gaspoweredcake.client33.utils.render.StorageView;
import com.gaspoweredcake.client33.utils.render.LiveChunkVisibility;
import com.gaspoweredcake.client33.utils.render.StorageLeadSelector;
import com.mojang.blaze3d.systems.RenderSystem;
import org.joml.Matrix4f;
import com.gaspoweredcake.client33.modintegration.BobbyStorageBridge;
import de.johni0702.minecraft.bobby.FakeChunk;
import com.gaspoweredcake.client33.utils.render.StorageClusters;
import com.gaspoweredcake.client33.utils.render.StorageHistory;
import com.gaspoweredcake.client33.utils.render.StorageSignals;
import com.gaspoweredcake.client33.events.packets.PlaySoundPacketEvent;
import com.gaspoweredcake.client33.events.world.ChunkDataEvent;
import com.gaspoweredcake.client33.utils.render.LabelLayout;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.Nameable;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import org.joml.Vector3d;
import java.util.ArrayList;
import java.util.Comparator;
import com.gaspoweredcake.client33.events.render.Render3DEvent;
import com.gaspoweredcake.client33.events.world.TickEvent;
import com.gaspoweredcake.client33.events.game.GameLeftEvent;
import net.minecraft.world.entity.vehicle.ContainerEntity;
import net.minecraft.world.level.chunk.ChunkAccess;
import java.util.ArrayDeque;
import java.util.LinkedHashMap;
import com.gaspoweredcake.client33.gui.GuiTheme;
import com.gaspoweredcake.client33.gui.widgets.WWidget;
import com.gaspoweredcake.client33.gui.widgets.containers.WVerticalList;
import com.gaspoweredcake.client33.gui.widgets.pressable.WButton;
import com.gaspoweredcake.client33.renderer.MeshBuilder;
import com.gaspoweredcake.client33.renderer.MeshRenderer;
import com.gaspoweredcake.client33.renderer.Client33RenderPipelines;
import com.gaspoweredcake.client33.renderer.ShapeMode;
import com.gaspoweredcake.client33.settings.*;
import com.gaspoweredcake.client33.systems.modules.Categories;
import com.gaspoweredcake.client33.systems.modules.Module;
import com.gaspoweredcake.client33.utils.Utils;
import com.gaspoweredcake.client33.utils.player.PlayerUtils;
import com.gaspoweredcake.client33.utils.render.MeshBuilderVertexConsumerProvider;
import com.gaspoweredcake.client33.utils.render.RenderUtils;
import com.gaspoweredcake.client33.utils.render.SimpleBlockRenderer;
import com.gaspoweredcake.client33.utils.render.color.Color;
import com.gaspoweredcake.client33.utils.render.color.SettingColor;
import com.gaspoweredcake.client33.utils.render.postprocess.PostProcessShaders;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.entity.*;

import java.util.List;

public class StorageESP extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgData = settings.createGroup("Container Intelligence");
    private final SettingGroup sgVisual = settings.createGroup("Signature Visuals");
    private final SettingGroup sgLeads = settings.createGroup("Storage Leads");
    private final Setting<Boolean> storageLeads = sgLeads.add(new BoolSetting.Builder().name("storage-leads")
        .description("Show bearings and coordinates for groups of received storage, including offscreen groups. A lead is not proof of a base.")
        .defaultValue(true).build());
    private final Setting<Integer> leadMinimum = sgLeads.add(new IntSetting.Builder().name("minimum-containers")
        .description("Minimum observed containers per spatial group.").defaultValue(3).range(1,64).sliderRange(1,16).build());
    private final Setting<Integer> leadRows = sgLeads.add(new IntSetting.Builder().name("lead-rows")
        .description("Number of groups on the compact storage compass; does not affect discovery or box rendering.")
        .defaultValue(4).range(1,12).sliderRange(1,8).build());
    private final Setting<StorageLeadSelector.Order> leadOrder = sgLeads.add(new EnumSetting.Builder<StorageLeadSelector.Order>()
        .name("lead-order").description("Prioritize larger groups or nearby groups within each observation source.")
        .defaultValue(StorageLeadSelector.Order.Largest).build());
    private List<StorageClusters.Lead> leads = List.of();
    private record RememberedLead(StorageClusters.Lead lead, long lastSeen) { }
    private final CachedStorageIndex cachedIndex = new CachedStorageIndex();
    private final LiveChunkVisibility frameChunkVisibility = new LiveChunkVisibility();
    private final ArrayDeque<java.lang.ref.WeakReference<FakeChunk>> cachedScanQueue = new ArrayDeque<>();
    private List<CachedStorageIndex.Entry> cachedLocations = List.of();
    private List<CachedStorageIndex.Page> cachedPages = List.of();
    private List<StorageClusters.Lead> cachedLeads = List.of();
    private List<StorageLeadSelector.Candidate> displayLeads = List.of();
    private final Setting<Boolean> cachedStorage = sgLeads.add(new BoolSetting.Builder().name("cached-storage")
        .description("Index Bobby's saved container records across 300-chunk terrain range. Cached markers have unknown observation age and never imply live contents.")
        .defaultValue(true).onChanged(value -> { if (!value) clearCachedStorage(); }).build());
    private final StorageHistory history = new StorageHistory();
    private List<StorageHistory.Sighting> rememberedLocations = List.of();
    private List<RememberedLead> rememberedLeads = List.of();
    private final Setting<Boolean> rememberLocations = sgLeads.add(new BoolSetting.Builder().name("remember-locations")
        .description("Keep session coordinates after chunk unload, explicitly labeled Last seen. Clears on world change or deactivation.")
        .defaultValue(true).onChanged(value -> { if (!value) { history.clear(); rememberedLeads = List.of(); rememberedLocations = List.of(); } }).build());
    private final StorageSignals soundSignals = new StorageSignals();
    private List<StorageSignals.Signal> heardSignals = List.of();
    private final Setting<Boolean> storageSounds = sgLeads.add(new BoolSetting.Builder().name("storage-sounds")
        .description("Add server-sent container sound coordinates as Heard leads. Sounds do not prove a container, base or another player's activity.")
        .defaultValue(true).onChanged(value -> { if (!value) { soundSignals.clear(); heardSignals = List.of(); } }).build());
    private final Setting<Integer> locationLifetime = sgLeads.add(new IntSetting.Builder().name("location-lifetime-minutes")
        .description("Expire old location leads; no distance-based eviction. Revisited chunks replace their old observations.")
        .defaultValue(120).range(1,1440).sliderRange(5,240).visible(rememberLocations::get).build());
    private final ContainerObservations observations = new ContainerObservations();
    private record Overlay(Object source, AABB bounds, Color color, double distance) {}
    private final List<Overlay> overlays = new ArrayList<>();
    private Object focused;
    private final Setting<Boolean> labels = sgData.add(new BoolSetting.Builder().name("labels").description("Decluttered storage type, distance and observed occupancy labels.").defaultValue(true).build());
    private final Setting<Boolean> inspector = sgData.add(new BoolSetting.Builder().name("inspector").description("Point at storage for coordinates, block properties and remembered content summaries.").defaultValue(true).build());
    private final Setting<Boolean> rememberContents = sgData.add(new BoolSetting.Builder().name("remember-contents").description("Remember server-sent contents after you open a container. Session only; never opens containers for you.").defaultValue(true).onChanged(value -> { if (!value) observations.clear(); }).build());
    private final Setting<Integer> memorySeconds = sgData.add(new IntSetting.Builder().name("memory-seconds").description("Expire remembered contents; other players may change them sooner.").defaultValue(300).range(10, 1800).sliderRange(10, 600).build());
    private final Setting<Integer> labelBudget = sgData.add(new IntSetting.Builder().name("label-budget").description("Maximum nonoverlapping labels per frame; closest containers have priority.").defaultValue(20).range(1, 64).sliderRange(1, 64).build());
    private final Setting<Boolean> coordinates = sgData.add(new BoolSetting.Builder().name("label-coordinates").description("Add XYZ coordinates to each label; always shown in the inspector.").defaultValue(false).build());
    private final Setting<Integer> contentLines = sgData.add(new IntSetting.Builder().name("content-lines").description("Most numerous item types to list in the inspector.").defaultValue(5).range(1, 12).sliderRange(1, 12).build());
    private final Setting<Double> labelScale = sgVisual.add(new DoubleSetting.Builder().name("label-scale").description("Scale of container labels and inspector text.").defaultValue(0.7).range(0.4, 1.2).sliderRange(0.4, 1.2).build());
    private final Setting<Boolean> distantLocators = sgVisual.add(new BoolSetting.Builder().name("distant-locators")
        .description("Add camera-facing diamonds of approximately five pixels when storage boxes become too small to distinguish.")
        .defaultValue(true).build());
    private final Setting<Boolean> pillars = sgVisual.add(new BoolSetting.Builder().name("vertical-pillars")
        .description("Draw translucent pillars to build height for all loaded storage, without a distance cutoff.")
        .defaultValue(false).build());
    private final Setting<Double> pillarWidth = sgVisual.add(new DoubleSetting.Builder().name("pillar-width")
        .description("Width of storage pillars in blocks.").defaultValue(0.2).range(0.05, 1).sliderRange(0.05, 1)
        .visible(pillars::get).build());
    private final Setting<Integer> pillarOpacity = sgVisual.add(new IntSetting.Builder().name("pillar-opacity")
        .description("Pillar opacity multiplied by the storage color's opacity.")
        .defaultValue(48).range(0, 255).sliderRange(0, 128).visible(pillars::get).build());
    private final Setting<Boolean> animate = sgVisual.add(new BoolSetting.Builder().name("animate").description("Animate hologram scan bands and locator rings.").defaultValue(true).build());
    private final Setting<SettingColor> accent = sgVisual.add(new ColorSetting.Builder().name("scan-accent").description("Hologram scan and focused-container accent.").defaultValue(new SettingColor(106, 232, 217, 220)).build());
    private final SettingGroup sgOpened = settings.createGroup("Opened Rendering");

    private final LinkedHashMap<BlockPos, BlockEntity> containers = new LinkedHashMap<>();
    private final ArrayDeque<ChunkAccess> scanQueue = new ArrayDeque<>();
    private Object scanWorld;
    private int scanTick;
    private final Setting<Integer> chunksPerTick = sgGeneral.add(new IntSetting.Builder().name("chunks-per-tick")
        .description("Loaded chunks indexed each tick. Controls discovery latency, never rendering range.").defaultValue(32).range(1, 256).sliderRange(1, 128).build());
    private final Setting<Boolean> vehicles = sgGeneral.add(new BoolSetting.Builder().name("container-vehicles")
        .description("Include loaded container entities such as chest boats and chest/hopper minecarts.").defaultValue(true).build());
    private final Setting<Boolean> framedShulkers = sgGeneral.add(new BoolSetting.Builder().name("framed-shulkers")
        .description("Highlight shulker box items in loaded item frames. Their contents are unknown.").defaultValue(false).build());
    private final Setting<Mode> mode = sgGeneral.add(new EnumSetting.Builder<Mode>()
        .name("mode")
        .description("Rendering mode.")
        .defaultValue(Mode.Box)
        .build()
    );

    private final Setting<BoxStyle> boxStyle = sgGeneral.add(new EnumSetting.Builder<BoxStyle>()
        .name("box-style")
        .description("Full edges, corner brackets, or an animated hologram with a locator ring.")
        .defaultValue(BoxStyle.Hologram)
        .visible(() -> mode.get() == Mode.Box)
        .build()
    );

    private final Setting<List<BlockEntityType<?>>> storageBlocks = sgGeneral.add(new StorageBlockListSetting.Builder()
        .name("storage-blocks")
        .description("Select the storage blocks to display.")
        .defaultValue(StorageBlockListSetting.STORAGE_BLOCKS)
        .build()
    );

    private final Setting<Boolean> tracers = sgGeneral.add(new BoolSetting.Builder()
        .name("tracers")
        .description("Draws tracers to storage blocks.")
        .defaultValue(false)
        .build()
    );

    public final Setting<ShapeMode> shapeMode = sgGeneral.add(new EnumSetting.Builder<ShapeMode>()
        .name("shape-mode")
        .description("How the shapes are rendered.")
        .defaultValue(ShapeMode.Both)
        .build()
    );

    public final Setting<Integer> fillOpacity = sgGeneral.add(new IntSetting.Builder()
        .name("fill-opacity")
        .description("The opacity of the shape fill.")
        .visible(() -> shapeMode.get() != ShapeMode.Lines)
        .defaultValue(28)
        .range(0, 255)
        .sliderMax(255)
        .build()
    );

    public final Setting<Integer> outlineWidth = sgGeneral.add(new IntSetting.Builder()
        .name("width")
        .description("The width of the shader outline.")
        .visible(() -> mode.get() == Mode.Shader)
        .defaultValue(1)
        .range(1, 10)
        .sliderRange(1, 5)
        .build()
    );

    public final Setting<Double> glowMultiplier = sgGeneral.add(new DoubleSetting.Builder()
        .name("glow-multiplier")
        .description("Multiplier for glow effect")
        .visible(() -> mode.get() == Mode.Shader)
        .decimalPlaces(3)
        .defaultValue(0.7)
        .min(0)
        .sliderMax(10)
        .build()
    );

    private final Setting<SettingColor> chest = sgGeneral.add(new ColorSetting.Builder()
        .name("chest")
        .description("The color of chests.")
        .defaultValue(new SettingColor(226, 188, 139, 220))
        .build()
    );

    private final Setting<SettingColor> trappedChest = sgGeneral.add(new ColorSetting.Builder()
        .name("trapped-chest")
        .description("The color of trapped chests.")
        .defaultValue(new SettingColor(231, 141, 148, 220))
        .build()
    );

    private final Setting<SettingColor> barrel = sgGeneral.add(new ColorSetting.Builder()
        .name("barrel")
        .description("The color of barrels.")
        .defaultValue(new SettingColor(140, 204, 176, 220))
        .build()
    );

    private final Setting<SettingColor> shulker = sgGeneral.add(new ColorSetting.Builder()
        .name("shulker")
        .description("The color of Shulker Boxes.")
        .defaultValue(new SettingColor(160, 183, 225, 220))
        .build()
    );

    private final Setting<SettingColor> enderChest = sgGeneral.add(new ColorSetting.Builder()
        .name("ender-chest")
        .description("The color of Ender Chests.")
        .defaultValue(new SettingColor(185, 158, 224, 220))
        .build()
    );

    private final Setting<SettingColor> other = sgGeneral.add(new ColorSetting.Builder()
        .name("other")
        .description("The color of furnaces, dispensers, droppers and hoppers.")
        .defaultValue(new SettingColor(173, 193, 198, 210))
        .build()
    );

    private final Setting<Double> fadeDistance = sgGeneral.add(new DoubleSetting.Builder()
        .name("fade-distance")
        .description("Distance from the camera over which nearby storage fades in.")
        .defaultValue(6)
        .min(0)
        .sliderMax(12)
        .build()
    );

    private final Setting<Boolean> hideOpened = sgOpened.add(new BoolSetting.Builder()
        .name("hide-opened")
        .description("Hides containers with an unexpired server-observed contents snapshot.")
        .defaultValue(false)
        .build()
    );

    private final Setting<SettingColor> openedColor = sgOpened.add(new ColorSetting.Builder()
        .name("opened-color")
        .description("Optional setting to change colors of opened chests, as opposed to not rendering. Disabled at zero opacity.")
        .defaultValue(new SettingColor(154, 171, 181, 0)) // TRANSPARENT BY DEFAULT.
        .build()
    );


    private final Color lineColor = new Color(0, 0, 0, 0);
    private final Color sideColor = new Color(0, 0, 0, 0);
    private boolean render;
    private int count;

    private final MeshBuilder mesh;
    private final MeshBuilderVertexConsumerProvider vertexConsumerProvider;

    public StorageESP() {
        super(Categories.Render, "storage-esp", "Indexes every loaded container and vehicle storage without distance or overlay count limits.");

        mesh = new MeshBuilder(Client33RenderPipelines.WORLD_COLORED);
        vertexConsumerProvider = new MeshBuilderVertexConsumerProvider(mesh);
    }

    private void getBlockEntityColor(BlockEntity blockEntity) {
        render = false;

        if (!storageBlocks.get().contains(blockEntity.getType())) return;

        if (blockEntity instanceof TrappedChestBlockEntity)
            lineColor.set(trappedChest.get()); // Must come before ChestBlockEntity as it is the superclass of TrappedChestBlockEntity
        else if (blockEntity instanceof ChestBlockEntity) lineColor.set(chest.get());
        else if (blockEntity instanceof BarrelBlockEntity) lineColor.set(barrel.get());
        else if (blockEntity instanceof ShulkerBoxBlockEntity) lineColor.set(shulker.get());
        else if (blockEntity instanceof EnderChestBlockEntity) lineColor.set(enderChest.get());
        else if (blockEntity instanceof AbstractFurnaceBlockEntity || blockEntity instanceof BrewingStandBlockEntity || blockEntity instanceof ChiseledBookShelfBlockEntity || blockEntity instanceof CrafterBlockEntity || blockEntity instanceof DispenserBlockEntity || blockEntity instanceof DecoratedPotBlockEntity || blockEntity instanceof HopperBlockEntity)
            lineColor.set(other.get());
        else return;

        render = true;

        if (shapeMode.get() == ShapeMode.Sides || shapeMode.get() == ShapeMode.Both) {
            sideColor.set(lineColor);
            sideColor.a = lineColor.a * fillOpacity.get() / 255;
        }
    }

    @Override
    public WWidget getWidget(GuiTheme theme) {
        WVerticalList list = theme.verticalList();

        // Button to Clear Interacted Blocks
        WButton clear = list.add(theme.button("Clear Rendering Cache")).expandX().widget();

        clear.action = this::clearCache;

        return list;
    }

    @EventHandler
    private void onBlockInteract(InteractBlockEvent event) {
        if (!event.isCancelled() && mc.level != null) observations.attempt(mc.level.getBlockEntity(event.result.getBlockPos()));
    }

    @EventHandler private void onEntityInteract(InteractEntityEvent event) {
        if (!event.isCancelled()) observations.attempt(event.entity);
    }
    @EventHandler private void onInventory(InventoryEvent event) {
        if (rememberContents.get()) observations.full(event.packet);
    }
    @EventHandler private void onSlot(ContainerSlotUpdateEvent event) {
        if (rememberContents.get()) observations.slot(event.packet);
    }

    @EventHandler
    private void onRender(Render3DEvent event) {
        count = 0; overlays.clear();
        if (mc.level == null || mc.player == null) return;
        Mode renderMode = mode.get();

        if (scanWorld != mc.level) clearCache();
        renderRememberedLocations(event);
        renderCachedLocations(event);
        if (pillars.get() && pillarOpacity.get() > 0) renderPillars(event);
        for (BlockEntity blockEntity : containers.values()) {
            BlockPos livePos = blockEntity.getBlockPos();
            if (!mc.level.hasChunk(livePos.getX() >> 4, livePos.getZ() >> 4)
                || mc.level.getBlockEntity(livePos) != blockEntity) continue;
            if (renderMode == Mode.Box && isSecondHalfOfDoubleChest(blockEntity)) continue;
            // Check if the block has been interacted with (opened)
            var snapshot = observations.snapshot(blockEntity, memorySeconds.get() * 1000L);
            boolean interacted = snapshot != null;
            if (interacted && hideOpened.get()) continue;  // Skip rendering if "hideOpened" is true

            getBlockEntityColor(blockEntity);
            if (!render) continue;

            // Set the color to openedColor if its alpha is greater than 0
            if (interacted && openedColor.get().a > 0) {
                // openedColor takes precedence.
                lineColor.set(openedColor.get());
                sideColor.set(openedColor.get());
                sideColor.a = lineColor.a * fillOpacity.get() / 255;
            }

            if (render) {
                double a = opacityAt(blockEntity.getBlockPos());
                if (a < 0.075) continue;

                // Only start a mesh when there's something to render
                if (count == 0 && mode.get() == Mode.Shader) {
                    mesh.begin();
                }

                int prevLineA = lineColor.a;
                int prevSideA = sideColor.a;

                lineColor.a *= a;
                sideColor.a *= a;

                if (tracers.get()) {
                    event.renderer.line(RenderUtils.center.x, RenderUtils.center.y, RenderUtils.center.z, blockEntity.getBlockPos().getX() + 0.5, blockEntity.getBlockPos().getY() + 0.5, blockEntity.getBlockPos().getZ() + 0.5, lineColor);
                }

                if (focused == ContainerObservations.canonical(blockEntity)) lineColor.set(accent.get()).a((int) (accent.get().a * a));
                if (!isSecondHalfOfDoubleChest(blockEntity)) overlays.add(new Overlay(ContainerObservations.canonical(blockEntity), bounds(blockEntity), new Color(lineColor),
                    Math.sqrt(PlayerUtils.squaredDistanceToCamera(livePos.getX() + 0.5, livePos.getY() + 0.5, livePos.getZ() + 0.5))));

                if (mode.get() == Mode.Box) {
                    renderBox(event, blockEntity);
                }
                if (!isSecondHalfOfDoubleChest(blockEntity)) renderLocator(event, bounds(blockEntity), lineColor);

                if (mode.get() == Mode.Shader) {
                    renderShader(event, blockEntity);
                }

                lineColor.a = prevLineA;
                sideColor.a = prevSideA;

                count++;
            }
        }

        if (mode.get() == Mode.Shader && count > 0) {
            MeshRenderer.begin()
                .attachments(PostProcessShaders.STORAGE_OUTLINE.framebuffer)
                .clearColor(Color.CLEAR)
                .pipeline(Client33RenderPipelines.WORLD_COLORED)
                .mesh(mesh, event.matrices)
                .end();

            PostProcessShaders.STORAGE_OUTLINE.render();
        }
        if (vehicles.get()) for (var entity : mc.level.entitiesForRendering()) {
            if (!(entity instanceof ContainerEntity) || !entity.isAlive()) continue;
            double opacity = opacityAt(entity.blockPosition());
            if (opacity < 0.075) continue;
            Color line = new Color(other.get()).a((int) (other.get().a * opacity));
            Color fill = new Color(line).a(line.a * fillOpacity.get() / 255);
            var bounds = entity.getBoundingBox().move(Mth.lerp(event.tickDelta, entity.xOld, entity.getX()) - entity.getX(),
                Mth.lerp(event.tickDelta, entity.yOld, entity.getY()) - entity.getY(), Mth.lerp(event.tickDelta, entity.zOld, entity.getZ()) - entity.getZ());
            if (hideOpened.get() && observations.snapshot(entity, memorySeconds.get() * 1000L) != null) continue;
            if (openedColor.get().a > 0 && observations.snapshot(entity, memorySeconds.get() * 1000L) != null) {
                line.set(openedColor.get()).a((int) (openedColor.get().a * opacity)); fill.set(line).a(line.a * fillOpacity.get() / 255);
            }
            if (focused == entity) line.set(accent.get());
            event.renderer.box(bounds, fill, line, shapeMode.get(), 0);
            renderLocator(event, bounds, line);
            if (boxStyle.get() == BoxStyle.Hologram) hologram(event, bounds, line);
            if (tracers.get()) event.renderer.line(RenderUtils.center.x, RenderUtils.center.y, RenderUtils.center.z, bounds.getCenter().x, bounds.getCenter().y, bounds.getCenter().z, line);
            overlays.add(new Overlay(entity, bounds, line, Math.sqrt(PlayerUtils.squaredDistanceToCamera(entity.getX(), entity.getY(), entity.getZ()))));
            count++;
        }
        if (framedShulkers.get()) for (var entity : mc.level.entitiesForRendering()) {
            if (!isFramedShulker(entity)) continue;
            double opacity = opacityAt(entity.blockPosition());
            if (opacity < 0.075) continue;
            Color line = new Color(shulker.get()).a((int) (shulker.get().a * opacity));
            Color fill = new Color(line).a(line.a * fillOpacity.get() / 255);
            AABB bounds = entity.getBoundingBox();
            if (focused == entity) line.set(accent.get());
            event.renderer.box(bounds, fill, line, shapeMode.get(), 0);
            renderLocator(event, bounds, line);
            if (tracers.get()) event.renderer.line(RenderUtils.center.x, RenderUtils.center.y, RenderUtils.center.z,
                bounds.getCenter().x, bounds.getCenter().y, bounds.getCenter().z, line);
            overlays.add(new Overlay(entity, bounds, line, Math.sqrt(PlayerUtils.squaredDistanceToCamera(entity.getX(), entity.getY(), entity.getZ()))));
        }
    }

    private boolean isFramedShulker(Object source) {
        return source instanceof ItemFrame frame && frame.isAlive()
            && frame.getItem().getItem() instanceof BlockItem item && item.getBlock() instanceof ShulkerBoxBlock;
    }

    private void renderLocator(Render3DEvent event, AABB bounds, Color color) {
        if (!distantLocators.get() || color.a <= 0) return;
        var center = bounds.getCenter();
        var projected = StorageLocator.project(center.x - event.offsetX, center.y - event.offsetY,
            center.z - event.offsetZ, RenderUtils.projection.m11(), mc.getWindow().getHeight());
        if (projected == null) return;
        center = new Vec3(event.offsetX + projected.x(), event.offsetY + projected.y(), event.offsetZ + projected.z());
        var diamond = projected.diamond();
        double rx = diamond.rightX(), rz = diamond.rightZ();
        double ux = diamond.upX(), uy = diamond.upY(), uz = diamond.upZ();
        event.renderer.line(center.x + rx, center.y, center.z + rz, center.x + ux, center.y + uy, center.z + uz, color);
        event.renderer.line(center.x + ux, center.y + uy, center.z + uz, center.x - rx, center.y, center.z - rz, color);
        event.renderer.line(center.x - rx, center.y, center.z - rz, center.x - ux, center.y - uy, center.z - uz, color);
        event.renderer.line(center.x - ux, center.y - uy, center.z - uz, center.x + rx, center.y, center.z + rz, color);
    }

    private void renderCachedLocations(Render3DEvent event) {
        if (!cachedStorage.get() || !distantLocators.get()) return;
        Color color = new Color(159, 141, 211, 160);
        var clip = new Matrix4f(RenderUtils.projection).mul(RenderSystem.getModelViewStack()).mul(event.matrices.last().pose());
        var view = new StorageView(clip, event.offsetX, event.offsetY, event.offsetZ,
            mc.getWindow().getWidth(), mc.getWindow().getHeight());
        frameChunkVisibility.beginFrame();
        var level = mc.level;
        LiveChunkVisibility.Lookup liveLookup = (x, z) -> BobbyStorageBridge.isLive(level, x, z);
        for (var page : cachedPages) {
            // Near hologram rings extend 0.15 blocks beyond the unit container bounds.
            if (!view.intersects(page.minX() - .2, page.minY() - .2, page.minZ() - .2,
                page.maxX() + .2, page.maxY() + .2, page.maxZ() + .2)) continue;
            for (var entry : page.entries()) {
                if (!view.intersects(entry.x() - .2, entry.y() - .2, entry.z() - .2,
                    entry.x() + 1.2, entry.y() + 1.2, entry.z() + 1.2)) continue;
                if (frameChunkVisibility.isLive(entry.x() >> 4, entry.z() >> 4, liveLookup)) continue;
                double cx = entry.x() + .5, cy = entry.y() + .5, cz = entry.z() + .5;
                var marker = StorageLocator.project(cx - event.offsetX, cy - event.offsetY, cz - event.offsetZ,
                    RenderUtils.projection.m11(), mc.getWindow().getHeight());
                if (marker == null) {
                    // Cached close-up storage uses corner rails, never a filled live box.
                    hologram(event, new AABB(entry.x(), entry.y(), entry.z(), entry.x() + 1, entry.y() + 1, entry.z() + 1), color);
                    continue;
                }
                double x = event.offsetX + marker.x(), y = event.offsetY + marker.y(), z = event.offsetZ + marker.z();
                var d = marker.diamond();
                // Violet brackets distinguish disk cache from solid live diamonds and history crosses.
                for (int sign = -1; sign <= 1; sign += 2) {
                    event.renderer.line(x + sign * d.rightX(), y, z + sign * d.rightZ(),
                        x + sign * d.rightX() + d.upX(), y + d.upY(), z + sign * d.rightZ() + d.upZ(), color);
                    event.renderer.line(x + sign * d.rightX(), y, z + sign * d.rightZ(),
                        x + sign * d.rightX() - d.upX(), y - d.upY(), z + sign * d.rightZ() - d.upZ(), color);
                }
            }
        }
    }

    private void clearCachedStorage() {
        cachedIndex.clear(); cachedScanQueue.clear(); cachedLocations = List.of(); cachedPages = List.of(); cachedLeads = List.of();
    }

    public void indexCachedChunk(FakeChunk chunk) {
        if (!isActive() || !cachedStorage.get() || mc.level == null || !mc.isSameThread()) return;
        if (!BobbyStorageBridge.isCurrent(mc.level, chunk)) return;
        if (scanWorld != mc.level) clearCache();
        cachedIndex.replace(chunk.getPos().x(), chunk.getPos().z(), BobbyStorageBridge.records(chunk));
    }

    private void renderRememberedLocations(Render3DEvent event) {
        if (!rememberLocations.get() || !distantLocators.get()) return;
        long now = CombatTelemetry.now();
        long lifetime = locationLifetime.get() * 60_000L;
        Color color = new Color(accent.get()).a(Math.min(150, accent.get().a));
        for (var sighting : rememberedLocations) {
            var p = sighting.point();
            if (now - sighting.lastSeenMillis() > lifetime
                || BobbyStorageBridge.isLive(mc.level, (int) Math.floor(p.x()) >> 4, (int) Math.floor(p.z()) >> 4)) continue;
            var marker = StorageLocator.project(p.x() - event.offsetX, p.y() - event.offsetY,
                p.z() - event.offsetZ, RenderUtils.projection.m11(), mc.getWindow().getHeight());
            if (marker == null) continue;
            double x = event.offsetX + marker.x(), y = event.offsetY + marker.y(), z = event.offsetZ + marker.z();
            var d = marker.diamond();
            // A broken cross distinguishes remembered coordinates from solid live diamonds.
            for (int sign = -1; sign <= 1; sign += 2) {
                event.renderer.line(x + sign * d.rightX() * .4, y, z + sign * d.rightZ() * .4,
                    x + sign * d.rightX(), y, z + sign * d.rightZ(), color);
                event.renderer.line(x + sign * d.upX() * .4, y + sign * d.upY() * .4, z + sign * d.upZ() * .4,
                    x + sign * d.upX(), y + sign * d.upY(), z + sign * d.upZ(), color);
            }
        }
    }

    private void renderPillars(Render3DEvent event) {
        for (BlockEntity blockEntity : containers.values()) {
            BlockPos pos = blockEntity.getBlockPos();
            if (!mc.level.hasChunk(pos.getX() >> 4, pos.getZ() >> 4)
                || mc.level.getBlockEntity(pos) != blockEntity || isSecondHalfOfDoubleChest(blockEntity)) continue;
            var snapshot = observations.snapshot(blockEntity, memorySeconds.get() * 1000L);
            if (snapshot != null && hideOpened.get()) continue;
            getBlockEntityColor(blockEntity);
            if (!render) continue;
            if (snapshot != null && openedColor.get().a > 0) lineColor.set(openedColor.get());
            renderPillar(event, bounds(blockEntity), lineColor);
        }
        if (!vehicles.get() && !framedShulkers.get()) return;
        for (var entity : mc.level.entitiesForRendering()) {
            if (framedShulkers.get() && isFramedShulker(entity)) {
                renderPillar(event, entity.getBoundingBox(), shulker.get());
                continue;
            }
            if (!vehicles.get() || !(entity instanceof ContainerEntity) || !entity.isAlive()) continue;
            var snapshot = observations.snapshot(entity, memorySeconds.get() * 1000L);
            if (snapshot != null && hideOpened.get()) continue;
            Color color = snapshot != null && openedColor.get().a > 0 ? openedColor.get() : other.get();
            AABB bounds = entity.getBoundingBox().move(Mth.lerp(event.tickDelta, entity.xOld, entity.getX()) - entity.getX(),
                Mth.lerp(event.tickDelta, entity.yOld, entity.getY()) - entity.getY(), Mth.lerp(event.tickDelta, entity.zOld, entity.getZ()) - entity.getZ());
            renderPillar(event, bounds, color);
        }
    }

    private boolean renderPillar(Render3DEvent event, AABB bounds, Color color) {
        double x = (bounds.minX + bounds.maxX) / 2, z = (bounds.minZ + bounds.maxZ) / 2;
        double centerY = (bounds.minY + bounds.maxY) / 2;
        double fade = StoragePillar.opacity(Math.sqrt(PlayerUtils.squaredDistanceToCamera(x, centerY, z)));
        double top = (double) mc.level.getMaxY() + 1;
        int alpha = (int) Math.round(color.a * pillarOpacity.get() / 255.0 * fade);
        if (alpha <= 0 || bounds.maxY >= top) return false;
        double half = pillarWidth.get() / 2;
        Color fill = new Color(color).a(alpha);
        event.renderer.box(x - half, bounds.maxY, z - half, x + half, top, z + half, fill, Color.CLEAR, ShapeMode.Sides, 0);
        return true;
    }

    private void clearCache() {
        clearCachedStorage();
        containers.clear(); history.clear(); rememberedLocations = List.of(); soundSignals.clear(); heardSignals = List.of(); leads = List.of(); rememberedLeads = List.of(); displayLeads = List.of(); overlays.clear(); observations.clear(); focused = null; scanQueue.clear(); scanTick = 0; scanWorld = mc.level;
    }
    @Override public void onActivate() { clearCache(); }
    @Override public void onDeactivate() { clearCache(); }
    @EventHandler private void onLeave(GameLeftEvent event) { clearCache(); }
    @EventHandler private void onStorageSound(PlaySoundPacketEvent event) {
        if (!storageSounds.get() || mc.level == null) return;
        var packet = event.packet;
        String id = packet.getSound().value().location().toString();
        var world = mc.level;
        mc.execute(() -> {
            if (!isActive() || world != mc.level) return;
            if (scanWorld != world) clearCache();
            soundSignals.hear(id, packet.getX(), packet.getY(), packet.getZ(), CombatTelemetry.now());
        });
    }

    @EventHandler private void onChunkData(ChunkDataEvent event) {
        var world = mc.level;
        mc.execute(() -> {
            if (!isActive() || world == null || world != mc.level) return;
            if (scanWorld != world) clearCache();
            ChunkAccess chunk = event.chunk();
            if (world.hasChunk(chunk.getPos().x(), chunk.getPos().z())
                && world.getChunk(chunk.getPos().x(), chunk.getPos().z()) == chunk) indexChunk(chunk);
        });
    }

    private void indexChunk(ChunkAccess chunk) {
        cachedIndex.remove(chunk.getPos().x(), chunk.getPos().z());
        List<StorageClusters.Point> positions = new ArrayList<>();
        for (BlockPos pos : chunk.getBlockEntitiesPos()) {
            BlockEntity entity = mc.level.getBlockEntity(pos);
            if (entity != null && storageBlocks.get().contains(entity.getType())) {
                containers.put(pos.immutable(), entity);
                if (!isSecondHalfOfDoubleChest(entity)) positions.add(new StorageClusters.Point(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5));
            } else containers.remove(pos);
        }
        if (rememberLocations.get()) history.observe(chunk.getPos().x(), chunk.getPos().z(), positions, CombatTelemetry.now());
    }

    @EventHandler private void onTick(TickEvent.Post event) {
        if (scanWorld != mc.level) clearCache();
        if (mc.level == null || mc.player == null) return;
        observations.tick(memorySeconds.get() * 1000L);
        if (++scanTick % 20 == 0) containers.entrySet().removeIf(entry -> !mc.level.hasChunk(entry.getKey().getX() >> 4, entry.getKey().getZ() >> 4)
            || mc.level.getBlockEntity(entry.getKey()) != entry.getValue());
        // Sweep actual received slots, including chunks outside the terrain renderer's radius.
        if (scanQueue.isEmpty()) for (ChunkAccess chunk : Utils.chunks()) scanQueue.addLast(chunk);
        for (int i = 0; i < chunksPerTick.get() && !scanQueue.isEmpty(); i++) {
            ChunkAccess chunk = scanQueue.removeFirst();
            if (!mc.level.hasChunk(chunk.getPos().x(), chunk.getPos().z()) || mc.level.getChunk(chunk.getPos().x(), chunk.getPos().z()) != chunk) continue;
            indexChunk(chunk);
        }
        if (cachedStorage.get()) {
            if (cachedScanQueue.isEmpty() && scanTick % 20 == 0) {
                for (var chunk : BobbyStorageBridge.chunks(mc.level)) cachedScanQueue.addLast(new java.lang.ref.WeakReference<>(chunk));
            }
            for (int i = 0; i < chunksPerTick.get() && !cachedScanQueue.isEmpty(); i++) {
                var chunk = cachedScanQueue.removeFirst().get();
                if (chunk == null || !BobbyStorageBridge.isCurrent(mc.level, chunk)) continue;
                cachedIndex.replace(chunk.getPos().x(), chunk.getPos().z(), BobbyStorageBridge.records(chunk));
            }
        }
        heardSignals = storageSounds.get() ? soundSignals.recent(CombatTelemetry.now(), 120_000) : List.of();
        if (scanTick % 20 == 1) updateLeads();
    }


    private void updateLeads() {
        var selectedIds = new java.util.HashSet<String>();
        for (var type : storageBlocks.get()) selectedIds.add(BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(type).toString());
        cachedLocations = cachedStorage.get() ? cachedIndex.matching(selectedIds::contains,
            (x, z) -> BobbyStorageBridge.isLive(mc.level, x, z)) : List.of();
        cachedPages = CachedStorageIndex.pages(cachedLocations);
        cachedLeads = storageLeads.get() ? StorageClusters.group(cachedLocations.stream()
            .map(entry -> new StorageClusters.Point(entry.x() + .5, entry.y() + .5, entry.z() + .5)).toList(), leadMinimum.get()) : List.of();
        rememberedLocations = rememberLocations.get() ? history.unloaded((x, z) -> BobbyStorageBridge.isLive(mc.level, x, z),
            CombatTelemetry.now(), locationLifetime.get() * 60_000L) : List.of();
        if (!storageLeads.get()) { leads = List.of(); rememberedLeads = List.of(); displayLeads = List.of(); return; }
        List<StorageClusters.Point> positions = new ArrayList<>();
        var unique = java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<Object, Boolean>());
        for (BlockEntity block : containers.values()) {
            BlockPos pos = block.getBlockPos();
            if (!storageBlocks.get().contains(block.getType()) || !mc.level.hasChunk(pos.getX() >> 4, pos.getZ() >> 4)
                || mc.level.getBlockEntity(pos) != block) continue;
            Object source = ContainerObservations.canonical(block);
            if (!unique.add(source) || (hideOpened.get() && observations.snapshot(source, memorySeconds.get() * 1000L) != null)) continue;
            positions.add(new StorageClusters.Point(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5));
        }
        if (vehicles.get()) for (var entity : mc.level.entitiesForRendering()) {
            if (entity instanceof ContainerEntity && entity.isAlive()
                && (!hideOpened.get() || observations.snapshot(entity, memorySeconds.get() * 1000L) == null))
                positions.add(new StorageClusters.Point(entity.getX(), entity.getY(), entity.getZ()));
        }
        leads = StorageClusters.group(positions, leadMinimum.get());
        rememberedLeads = List.of();
        if (rememberLocations.get()) {
            var old = rememberedLocations;
            var times = new java.util.HashMap<StorageClusters.Point, Long>();
            for (var sighting : old) times.put(sighting.point(), sighting.lastSeenMillis());
            rememberedLeads = StorageClusters.group(times.keySet(), leadMinimum.get()).stream()
                .map(lead -> new RememberedLead(lead, times.get(lead.anchor()))).toList();
        }
        var candidates = new ArrayList<StorageLeadSelector.Candidate>();
        for (var lead : leads) candidates.add(new StorageLeadSelector.Candidate(StorageLeadSelector.Source.CURRENT, lead.anchor(), lead.containers(), 0, ""));
        for (var remembered : rememberedLeads) candidates.add(new StorageLeadSelector.Candidate(StorageLeadSelector.Source.REMEMBERED, remembered.lead().anchor(), remembered.lead().containers(), remembered.lastSeen(), ""));
        for (var lead : cachedLeads) candidates.add(new StorageLeadSelector.Candidate(StorageLeadSelector.Source.CACHED, lead.anchor(), lead.containers(), 0, ""));
        for (var signal : heardSignals) candidates.add(new StorageLeadSelector.Candidate(StorageLeadSelector.Source.HEARD, signal.point(), 0, signal.heardAt(), signal.label()));
        displayLeads = StorageLeadSelector.select(candidates, leadRows.get(), leadOrder.get(), mc.player.getX(), mc.player.getY(), mc.player.getZ());
    }

    private double opacityAt(BlockPos pos) {
        double distanceSquared = PlayerUtils.squaredDistanceToCamera(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);

        double distance = Math.sqrt(distanceSquared);
        double opacity = 1;
        double nearFade = fadeDistance.get();
        if (nearFade > 0) {
            double t = Mth.clamp(distance / nearFade, 0, 1);
            opacity *= t * t * (3 - 2 * t);
        }

        return opacity;
    }

    private boolean isSecondHalfOfDoubleChest(BlockEntity blockEntity) {
        return ContainerObservations.canonical(blockEntity) != blockEntity;
    }

    private AABB bounds(BlockEntity blockEntity) {
        BlockPos pos = blockEntity.getBlockPos();
        double x1 = pos.getX();
        double y1 = pos.getY();
        double z1 = pos.getZ();
        double x2 = x1 + 1;
        double y2 = y1 + 1;
        double z2 = z1 + 1;

        BlockEntity otherHalf = ContainerObservations.partner(blockEntity);
        if (otherHalf != null) {
            BlockPos otherPos = otherHalf.getBlockPos();
            x1 = Math.min(x1, otherPos.getX()); z1 = Math.min(z1, otherPos.getZ());
            x2 = Math.max(x2, otherPos.getX() + 1); z2 = Math.max(z2, otherPos.getZ() + 1);
        }

        if (blockEntity instanceof ChestBlockEntity || blockEntity instanceof EnderChestBlockEntity) {
            double inset = 1.0 / 16.0;
            x1 += inset;
            z1 += inset;
            x2 -= inset;
            y2 -= inset * 2;
            z2 -= inset;
        }

        return new AABB(x1, y1, z1, x2, y2, z2);
    }

    private void renderBox(Render3DEvent event, BlockEntity blockEntity) {
        AABB box = bounds(blockEntity);
        double x1 = box.minX, y1 = box.minY, z1 = box.minZ, x2 = box.maxX, y2 = box.maxY, z2 = box.maxZ;
        if (boxStyle.get() == BoxStyle.Hologram) hologram(event, box, lineColor);
        if (boxStyle.get() == BoxStyle.Full) {
            event.renderer.box(x1, y1, z1, x2, y2, z2, sideColor, lineColor, shapeMode.get(), 0);
        } else {
            if (shapeMode.get().sides()) event.renderer.boxSides(x1, y1, z1, x2, y2, z2, sideColor, 0);
            if (shapeMode.get().lines()) renderCorners(event, x1, y1, z1, x2, y2, z2);
        }
    }

    private void renderCorners(Render3DEvent event, double x1, double y1, double z1, double x2, double y2, double z2) {
        double dx = Math.min(0.25, (x2 - x1) * 0.28);
        double dy = Math.min(0.25, (y2 - y1) * 0.28);
        double dz = Math.min(0.25, (z2 - z1) * 0.28);
        for (int ix = 0; ix < 2; ix++) {
            double x = ix == 0 ? x1 : x2;
            double nextX = x + (ix == 0 ? dx : -dx);
            for (int iy = 0; iy < 2; iy++) {
                double y = iy == 0 ? y1 : y2;
                double nextY = y + (iy == 0 ? dy : -dy);
                for (int iz = 0; iz < 2; iz++) {
                    double z = iz == 0 ? z1 : z2;
                    double nextZ = z + (iz == 0 ? dz : -dz);
                    event.renderer.line(x, y, z, nextX, y, z, lineColor);
                    event.renderer.line(x, y, z, x, nextY, z, lineColor);
                    event.renderer.line(x, y, z, x, y, nextZ, lineColor);
                }
            }
        }
    }

    private void hologram(Render3DEvent event, AABB box, Color color) {
        if (!shapeMode.get().lines()) return;
        double phase = animate.get() ? (mc.level.getGameTime() + event.tickDelta) * 0.045 : 0;
        double y = box.minY + box.getYsize() * (0.5 + Math.sin(phase) * 0.45);
        Color scan = new Color(accent.get()).a(Math.min(color.a, accent.get().a));
        event.renderer.line(box.minX, y, box.minZ, box.maxX, y, box.minZ, scan);
        event.renderer.line(box.maxX, y, box.minZ, box.maxX, y, box.maxZ, scan);
        event.renderer.line(box.maxX, y, box.maxZ, box.minX, y, box.maxZ, scan);
        event.renderer.line(box.minX, y, box.maxZ, box.minX, y, box.minZ, scan);
        double cx = (box.minX + box.maxX) / 2, cz = (box.minZ + box.maxZ) / 2;
        double radius = Math.max(box.getXsize(), box.getZsize()) * 0.65;
        for (int i = 0; i < 24; i++) {
            if (i % 6 == 5) continue;
            double a = phase * 0.4 + i * Math.PI / 12, b = a + Math.PI / 12;
            event.renderer.line(cx + Math.cos(a) * radius, box.minY + 0.02, cz + Math.sin(a) * radius,
                cx + Math.cos(b) * radius, box.minY + 0.02, cz + Math.sin(b) * radius, color);
        }
    }

    private String name(Object source) {
        if (isFramedShulker(source)) return "Framed shulker item";
        if (source instanceof Entity entity) return ContainerSnapshot.clean(entity.getDisplayName().getString(), 34);
        BlockEntity block = (BlockEntity) source;
        if (block instanceof Nameable named && named.hasCustomName()) return ContainerSnapshot.clean(named.getDisplayName().getString(), 34);
        return (ContainerObservations.partner(block) == null ? "" : "Double ") + ContainerSnapshot.clean(block.getBlockState().getBlock().getName().getString(), 26);
    }
    private BlockPos position(Object source) { return source instanceof Entity entity ? entity.blockPosition() : ((BlockEntity) source).getBlockPos(); }
    private String location(Object source) { BlockPos p = position(source); return p.getX() + " / " + p.getY() + " / " + p.getZ(); }
    private ContainerSnapshot snapshot(Object source) { return rememberContents.get() ? observations.snapshot(source, memorySeconds.get() * 1000L) : null; }
    private String summary(Object source, ContainerSnapshot data) {
        if (data != null) return data.occupied() + "/" + data.slots() + " slots  ·  " + data.items() + (data.items() == 1 ? " item" : " items");
        int capacity = ContainerObservations.capacity(source);
        return (capacity == 0 ? "" : capacity + " slots  ·  ") + "Contents unknown";
    }
    private Object hitSource() {
        if (mc.hitResult instanceof BlockHitResult hit) {
            BlockEntity block = mc.level.getBlockEntity(hit.getBlockPos());
            return block == null ? null : ContainerObservations.canonical(block);
        }
        if (mc.hitResult instanceof EntityHitResult hit) return hit.getEntity();
        return null;
    }

    @EventHandler private void onLabels(Render2DEvent event) {
        if (mc.player == null || scanWorld != mc.level || mc.gui.screen() != null || (!labels.get() && !inspector.get() && !storageLeads.get())) { focused = null; return; }
        double width = mc.getWindow().getWidth(), height = mc.getWindow().getHeight();
        overlays.sort(Comparator.comparingDouble(Overlay::distance));
        Object hit = hitSource(); Overlay selection = null;
        double nearest = 75 * 75;
        for (Overlay overlay : overlays) {
            if (overlay.source == hit) { selection = overlay; break; }
            var center = overlay.bounds.getCenter();
            Vector3d projected = new Vector3d(center.x, center.y, center.z);
            if (!NametagUtils.to2D(projected, 1, false) || projected.z < 0 || projected.z > 1) continue;
            double dx = projected.x - width / 2, dy = projected.y - height / 2;
            double distance = dx * dx + dy * dy;
            if (distance < nearest) { nearest = distance; selection = overlay; }
        }
        focused = selection == null ? null : selection.source;
        Renderer2D.COLOR.begin();
        TextRenderer text = TextRenderer.get(); text.begin(event.graphics, labelScale.get());
        LabelLayout layout = new LabelLayout(width, height);
        if (storageLeads.get()) drawLeads(text, width, height, layout);
        if (inspector.get() && selection != null) drawInspector(text, selection, width, height, layout);
        int drawn = 0;
        if (labels.get()) for (Overlay overlay : overlays) {
            if (drawn >= labelBudget.get()) break;
            AABB box = overlay.bounds;
            Vector3d point = new Vector3d((box.minX + box.maxX) / 2, box.maxY + 0.28, (box.minZ + box.maxZ) / 2);
            if (!NametagUtils.to2D(point, 1, false) || point.z < 0 || point.z > 1) continue;
            var data = snapshot(overlay.source);
            String title = name(overlay.source) + "  /  " + Math.round(overlay.distance) + " m";
            String detail = summary(overlay.source, data);
            String state = data == null ? "NOT OBSERVED" : (observations.live(overlay.source) ? "RECEIVING" : (data.serverObserved() ? "RECEIVED " : "MENU VIEW ") + Math.max(0, (CombatTelemetry.now() - data.observedAt()) / 1000) + "s AGO");
            List<String> lines = new ArrayList<>(List.of(title, detail, state));
            if (coordinates.get()) lines.add(location(overlay.source));
            double w = lines.stream().mapToDouble(text::getWidth).max().orElse(0) + 20;
            double h = lines.size() * (text.getHeight() + 3) + 15;
            var rect = layout.place(point.x, point.y, w, h);
            if (rect == null) continue;
            panel(rect, overlay.source == focused ? accent.get() : overlay.color);
            for (int i = 0; i < lines.size(); i++) text.render(lines.get(i), rect.x() + 9, rect.y() + 6 + i * (text.getHeight() + 3), i == 0 ? Color.WHITE : new Color(171, 188, 202), false);
            if (data != null) {
                Renderer2D.COLOR.quad(rect.x() + 8, rect.y() + h - 6, w - 16, 2, new Color(49, 58, 68));
                Renderer2D.COLOR.quad(rect.x() + 8, rect.y() + h - 6, (w - 16) * data.occupancy(), 2, overlay.color);
            }
            Renderer2D.COLOR.line(point.x, point.y + 8, rect.x() + w / 2, rect.y() + h, overlay.color);
            drawn++;
        }
        Renderer2D.COLOR.render(); text.end();
    }

    private void panel(LabelLayout.Rect rect, Color color) {
        Renderer2D.COLOR.quad(rect.x(), rect.y(), rect.width(), rect.height(), new Color(12, 16, 23, 218));
        Renderer2D.COLOR.boxLines(rect.x(), rect.y(), rect.width(), rect.height(), new Color(57, 72, 86, 210));
        Renderer2D.COLOR.quad(rect.x(), rect.y(), 2, rect.height(), color);
        Renderer2D.COLOR.quad(rect.x(), rect.y(), Math.min(36, rect.width()), 2, color);
    }

    private void drawInspector(TextRenderer text, Overlay overlay, double screenWidth, double screenHeight, LabelLayout layout) {
        List<String> lines = new ArrayList<>();
        Object source = overlay.source;
        var data = snapshot(source);
        BlockPos pos = position(source);
        lines.add("STORAGE INTEL  /  " + name(source));
        lines.add("XYZ  " + location(source) + "  ·  " + Math.round(overlay.distance) + " m");
        lines.add("Chunk " + (pos.getX() >> 4) + ", " + (pos.getZ() >> 4) + "  /  " + mc.level.dimension().identifier().getPath());
        if (source instanceof BlockEntity block) {
            lines.add(BuiltInRegistries.BLOCK.getKey(block.getBlockState().getBlock()).toString());
            StringBuilder properties = new StringBuilder();
            for (var entry : block.getBlockState().getValues().sorted(Comparator.comparing(e -> e.property().getName())).toList()) {
                String value = entry.property().getName() + "=" + entry.valueName();
                if (properties.length() + value.length() > 44) { lines.add(properties.toString()); properties.setLength(0); }
                if (!properties.isEmpty()) properties.append("  "); properties.append(value);
            }
            if (!properties.isEmpty()) lines.add(properties.toString());
            if (block instanceof ChestBlockEntity) {
                BlockEntity partner = ContainerObservations.partner(block);
                boolean obstructed = mc.level.getBlockState(pos.above()).isRedstoneConductor(mc.level, pos.above())
                    || (partner != null && mc.level.getBlockState(partner.getBlockPos().above()).isRedstoneConductor(mc.level, partner.getBlockPos().above()));
                lines.add("Block above: " + (obstructed ? "obstructed" : "clear") + "  ·  " + (partner == null ? "single" : "paired"));
            }
        } else if (source instanceof Entity entity) {
            lines.add(BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString());
            lines.add("Entity #" + entity.getId() + "  ·  " + (entity.isAlive() ? "loaded" : "removed"));
        }
        lines.add(summary(source, data));
        if (data == null) lines.add(isFramedShulker(source) ? "Framed item; contents not observed." : (rememberContents.get() ? "Open container to observe its contents." : "Content memory disabled."));
        else {
            lines.add((data.serverObserved() ? "Server snapshot " : "Client menu view ") + Math.max(0, (CombatTelemetry.now() - data.observedAt()) / 1000) + "s ago  ·  may have changed");
            if (data.enchanted() > 0 || data.lowestDurability() >= 0)
                lines.add(data.enchanted() + (data.enchanted() == 1 ? " enchanted stack" : " enchanted stacks") + (data.lowestDurability() < 0 ? "" : "  ·  min durability " + data.lowestDurability() + "%"));
            int shown = 0;
            for (var item : data.contents()) { if (shown++ >= contentLines.get()) break; lines.add(item.count() + " × " + item.name()); }
            if (data.contents().size() > contentLines.get()) lines.add("+ " + (data.contents().size() - contentLines.get()) + " more item types");
            if (data.occupied() == 0) lines.add("Empty when observed");
        }
        double lineHeight = text.getHeight() + 5;
        int fit = Math.max(1, (int) ((screenHeight - 130) / lineHeight));
        if (lines.size() > fit) lines = new ArrayList<>(lines.subList(0, fit));
        double w = Math.min(screenWidth - 24, Math.max(250, Math.min(440, lines.stream().mapToDouble(text::getWidth).max().orElse(0) + 24)));
        double h = lines.size() * lineHeight + 20;
        var rect = new LabelLayout.Rect(Math.max(12, screenWidth - w - 18), Math.max(12, Math.min(90, screenHeight - h - 12)), w, h);
        layout.reserve(rect); panel(rect, accent.get());
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            while (line.length() > 1 && text.getWidth(line) > w - 24) line = line.substring(0, line.length() - 2) + "…";
            text.render(line, rect.x() + 12, rect.y() + 9 + i * lineHeight, i == 0 ? accent.get() : i < 3 ? Color.WHITE : new Color(181, 197, 209), false);
        }
    }

    private void drawLeads(TextRenderer text, double width, double height, LabelLayout layout) {
        List<String> rows = new ArrayList<>();
        rows.add("STORAGE / " + leads.size() + " current / " + rememberedLeads.size() + " remembered / " + cachedLocations.size() + " cached / " + heardSignals.size() + " heard");
        if (leads.isEmpty() && rememberedLeads.isEmpty() && cachedLeads.isEmpty() && heardSignals.isEmpty()) rows.add("Scanning received chunks / no matching group");
        double lineHeight = text.getHeight() + 4;
        int available = Math.max(0, Math.min(leadRows.get(), (int) ((height - 100 - 16 - lineHeight) / (2 * lineHeight))));
        int shown = 0;
        long now = CombatTelemetry.now();
        for (var candidate : displayLeads) {
            if (shown >= available) break;
            if (candidate.source() == StorageLeadSelector.Source.CACHED && !cachedStorage.get()) continue;
            if (candidate.source() == StorageLeadSelector.Source.REMEMBERED && (!rememberLocations.get() || now - candidate.time() > locationLifetime.get() * 60_000L)) continue;
            if (candidate.source() == StorageLeadSelector.Source.HEARD && (!storageSounds.get() || now - candidate.time() > 120_000L)) continue;
            var p = candidate.point();
            long distance = Math.round(Math.sqrt(p.squaredDistance(mc.player.getX(), mc.player.getY(), mc.player.getZ())));
            String bearing = StorageClusters.bearing(p.x() - mc.player.getX(), p.z() - mc.player.getZ());
            String description = switch (candidate.source()) {
                case CURRENT -> "CURRENT " + candidate.containers();
                case REMEMBERED -> "LAST SEEN " + Math.max(0, (now - candidate.time()) / 60_000) + "m / " + candidate.containers();
                case CACHED -> "CACHED / age unknown / " + candidate.containers();
                case HEARD -> "HEARD " + Math.max(0, (now - candidate.time()) / 1000) + "s / " + candidate.label();
            };
            rows.add(description + " / " + bearing + " / " + distance + " m");
            rows.add("XYZ " + (int) Math.floor(p.x()) + " " + (int) Math.floor(p.y()) + " " + (int) Math.floor(p.z()));
            shown++;
        }
        double maxTextWidth = Math.max(0, width - 36);
        if (height < 100 + 16 + lineHeight || maxTextWidth <= 0) return;
        for (int i = 0; i < rows.size(); i++) {
            String line = rows.get(i);
            while (!line.isEmpty() && text.getWidth(line) > maxTextWidth) line = line.substring(0, line.length() - 1);
            rows.set(i, line);
        }
        double w = rows.stream().mapToDouble(text::getWidth).max().orElse(0) + 20;
        double h = rows.size() * lineHeight + 16;
        var rect = new LabelLayout.Rect(8, 80, w, h);
        layout.reserve(rect); panel(rect, accent.get());
        for (int i = 0; i < rows.size(); i++) text.render(rows.get(i), rect.x() + 10, rect.y() + 8 + i * lineHeight,
            i == 0 ? accent.get() : i % 2 == 1 ? Color.WHITE : new Color(171, 188, 202), false);
    }

    private void renderShader(Render3DEvent event, BlockEntity blockEntity) {
        vertexConsumerProvider.setColor(lineColor);
        SimpleBlockRenderer.renderWithBlockEntity(blockEntity, event.tickDelta, vertexConsumerProvider);
    }

    @Override
    public String getInfoString() {
        return Integer.toString(count);
    }

    public boolean isShader() {
        return isActive() && mode.get() == Mode.Shader;
    }

    public enum Mode {
        Box,
        Shader
    }

    public enum BoxStyle {
        Corners,
        Full,
        Hologram
    }
}
