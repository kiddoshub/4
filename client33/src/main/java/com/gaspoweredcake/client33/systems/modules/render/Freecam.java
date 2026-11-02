
package com.gaspoweredcake.client33.systems.modules.render;


import com.gaspoweredcake.client33.events.game.GameLeftEvent;
import com.gaspoweredcake.client33.events.entity.player.DoItemUseEvent;
import com.gaspoweredcake.client33.events.game.OpenScreenEvent;
import com.gaspoweredcake.client33.events.client33.KeyInputEvent;
import com.gaspoweredcake.client33.events.client33.MouseClickEvent;
import com.gaspoweredcake.client33.events.client33.MouseScrollEvent;
import com.gaspoweredcake.client33.events.packets.PacketEvent;
import com.gaspoweredcake.client33.events.world.ChunkOcclusionEvent;
import com.gaspoweredcake.client33.events.world.TickEvent;
import com.gaspoweredcake.client33.pathing.PathManagers;
import com.gaspoweredcake.client33.settings.BoolSetting;
import com.gaspoweredcake.client33.settings.DoubleSetting;
import com.gaspoweredcake.client33.settings.Setting;
import com.gaspoweredcake.client33.settings.SettingGroup;
import com.gaspoweredcake.client33.systems.modules.Categories;
import com.gaspoweredcake.client33.systems.modules.Module;
import com.gaspoweredcake.client33.systems.modules.Modules;
import com.gaspoweredcake.client33.systems.modules.movement.GUIMove;
import com.gaspoweredcake.client33.utils.Utils;
import com.gaspoweredcake.client33.utils.misc.input.Input;
import com.gaspoweredcake.client33.utils.misc.input.KeyAction;
import com.gaspoweredcake.client33.utils.player.Rotations;
import meteordevelopment.orbit.EventHandler;
import meteordevelopment.orbit.EventPriority;
import net.minecraft.client.Camera;
import net.minecraft.client.CameraType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.network.protocol.game.ClientboundPlayerCombatKillPacket;
import net.minecraft.network.protocol.game.ClientboundRespawnPacket;
import net.minecraft.network.protocol.game.ClientboundSetHealthPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.BlockGetter;
import com.gaspoweredcake.client33.utils.player.ContainerReach;
import net.minecraft.world.phys.*;
import net.minecraft.world.phys.shapes.CollisionContext;
import org.joml.Vector3d;
import org.jspecify.annotations.Nullable;
import com.mojang.blaze3d.platform.InputConstants;

public class Freecam extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgPathing = settings.createGroup("Pathing");

    private final Setting<Double> speed = sgGeneral.add(new DoubleSetting.Builder()
        .name("speed")
        .description("Your speed while in freecam.")
        .onChanged(aDouble -> speedValue = aDouble)
        .defaultValue(1.0)
        .min(0.0)
        .build()
    );

    private final Setting<Double> sprintMultiplier = sgGeneral.add(new DoubleSetting.Builder()
        .name("sprint-multiplier")
        .description("Camera speed multiplier while sprint is held.")
        .defaultValue(2.0)
        .min(0)
        .sliderMax(5)
        .build()
    );

    private final Setting<Double> verticalMultiplier = sgGeneral.add(new DoubleSetting.Builder()
        .name("vertical-multiplier")
        .description("Vertical camera speed relative to horizontal speed.")
        .defaultValue(1.0)
        .min(0)
        .sliderMax(5)
        .build()
    );

    private final Setting<Double> speedScrollSensitivity = sgGeneral.add(new DoubleSetting.Builder()
        .name("speed-scroll-sensitivity")
        .description("Allows you to change speed value using scroll wheel. 0 to disable.")
        .defaultValue(0)
        .min(0)
        .sliderMax(2)
        .build()
    );

    private final Setting<Boolean> staySneaking = sgGeneral.add(new BoolSetting.Builder()
        .name("stay-sneaking")
        .description("If you are sneaking when you enter freecam, whether your player should remain sneaking.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> toggleOnDamage = sgGeneral.add(new BoolSetting.Builder()
        .name("toggle-on-damage")
        .description("Disables freecam when you take damage.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> toggleOnDeath = sgGeneral.add(new BoolSetting.Builder()
        .name("toggle-on-death")
        .description("Disables freecam when you die.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> toggleOnLog = sgGeneral.add(new BoolSetting.Builder()
        .name("toggle-on-log")
        .description("Disables freecam when you disconnect from a server.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> reloadChunks = sgGeneral.add(new BoolSetting.Builder()
        .name("reload-chunks")
        .description("Disables cave culling.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> renderHands = sgGeneral.add(new BoolSetting.Builder()
        .name("show-hands")
        .description("Whether or not to render your hands in freecam.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> rotate = sgGeneral.add(new BoolSetting.Builder()
        .name("rotate")
        .description("Rotates to the block or entity you are looking at.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> interactThroughWalls = sgGeneral.add(new BoolSetting.Builder()
        .name("interact-through-walls")
        .description("Opens containers behind walls while your real player is within interaction reach.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Double> containerRayRange = sgGeneral.add(new DoubleSetting.Builder()
        .name("container-ray-range")
        .description("How far the freecam view searches for a container through walls.")
        .defaultValue(6)
        .min(1)
        .sliderMax(12)
        .visible(interactThroughWalls::get)
        .build()
    );

    private final Setting<Boolean> extendedContainerAttempt = sgGeneral.add(new BoolSetting.Builder()
        .name("extended-container-attempt")
        .description("Attempt interactions beyond the normal player range. The server can reject them.")
        .defaultValue(false).visible(interactThroughWalls::get).build());

    private final Setting<Double> containerAttemptRange = sgGeneral.add(new DoubleSetting.Builder()
        .name("container-attempt-range")
        .description("Maximum distance from your real player to attempt a container interaction; server limits still apply.")
        .defaultValue(20).range(1, 20).sliderRange(1, 20)
        .visible(() -> interactThroughWalls.get() && extendedContainerAttempt.get()).build());

    private final Setting<Boolean> staticView = sgGeneral.add(new BoolSetting.Builder()
        .name("static")
        .description("Disables settings that move the view.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> baritoneClick = sgPathing.add(new BoolSetting.Builder()
        .name("click-to-path")
        .description("Sets a pathfinding goal to any block/entity you click at.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> requireDoubleClick = sgPathing.add(new BoolSetting.Builder()
        .name("double-click")
        .description("Require two clicks to start pathing.")
        .defaultValue(false)
        .build()
    );

    public final Vector3d pos = new Vector3d();
    public final Vector3d prevPos = new Vector3d();

    private CameraType perspective;
    private double speedValue;

    public float yaw, pitch;
    public float lastYaw, lastPitch;

    private double fovScale;
    private boolean bobView;
    private boolean staticViewApplied, chunksReloaded;

    private boolean forward, backward, right, left, up, down, isSneaking;

    private long clickTs = 0;

    public Freecam() {
        super(Categories.Render, "freecam", "Allows the camera to move away from the player.");
    }

    @Override
    public void onActivate() {
        fovScale = mc.options.fovEffectScale().get();
        bobView = mc.options.bobView().get();
        staticViewApplied = staticView.get();
        chunksReloaded = reloadChunks.get();
        clickTs = 0;
        if (staticViewApplied) {
            mc.options.fovEffectScale().set((double) 0);
            mc.options.bobView().set(false);
        }
        yaw = mc.player.getYRot();
        pitch = mc.player.getXRot();

        perspective = mc.options.getCameraType();
        speedValue = speed.get();

        Utils.set(pos, mc.gameRenderer.mainCamera().position());
        Utils.set(prevPos, mc.gameRenderer.mainCamera().position());

        if (mc.options.getCameraType() == CameraType.THIRD_PERSON_FRONT) {
            yaw += 180;
            pitch *= -1;
        }

        lastYaw = yaw;
        lastPitch = pitch;

        isSneaking = mc.options.keyShift.isDown();

        forward = Input.isPressed(mc.options.keyUp);
        backward = Input.isPressed(mc.options.keyDown);
        right = Input.isPressed(mc.options.keyRight);
        left = Input.isPressed(mc.options.keyLeft);
        up = Input.isPressed(mc.options.keyJump);
        down = Input.isPressed(mc.options.keyShift);

        unpress();
        if (chunksReloaded) {
            mc.levelExtractor.allChanged();
        }
    }

    @Override
    public void onDeactivate() {
        if (chunksReloaded || reloadChunks.get()) {
            mc.execute(mc.levelExtractor::allChanged);
        }

        mc.options.setCameraType(perspective);

        if (staticViewApplied) {
            mc.options.fovEffectScale().set(fovScale);
            mc.options.bobView().set(bobView);
        }

        isSneaking = false;
        clearMovement();
        staticViewApplied = false;
        chunksReloaded = false;
    }

    @EventHandler
    private void onOpenScreen(OpenScreenEvent event) {
        if (event.screen != null && !Modules.get().get(GUIMove.class).isActive()) clearMovement();
        unpress();

        prevPos.set(pos);
        lastYaw = yaw;
        lastPitch = pitch;
    }

    private void clearMovement() {
        forward = backward = left = right = up = down = false;
    }

    private void unpress() {
        mc.options.keyUp.setDown(false);
        mc.options.keyDown.setDown(false);
        mc.options.keyRight.setDown(false);
        mc.options.keyLeft.setDown(false);
        mc.options.keyJump.setDown(false);
        mc.options.keyShift.setDown(false);
    }

    @EventHandler(priority = EventPriority.HIGH)
    private void onUse(DoItemUseEvent event) {
        if (!interactThroughWalls.get() || event.isCancelled() || mc.player == null || mc.level == null
            || mc.gameMode == null || mc.gui.screen() != null || mc.player.isShiftKeyDown()
            || mc.hitResult instanceof EntityHitResult) return;

        if (mc.hitResult instanceof BlockHitResult hit && hit.getType() == HitResult.Type.BLOCK
            && isContainer(hit.getBlockPos()) && mc.player.isWithinBlockInteractionRange(hit.getBlockPos(), 1)) return;

        BlockHitResult hit = findContainerThroughWalls();
        if (hit == null) return;

        for (InteractionHand hand : InteractionHand.values()) {
            InteractionResult result = mc.gameMode.useItemOn(mc.player, hand, hit);
            if (result instanceof InteractionResult.Success) {
                com.gaspoweredcake.client33.utils.player.SwingUtils.swing(hand);
                event.cancel();
                return;
            }
            if (result instanceof InteractionResult.Fail) {
                event.cancel();
                return;
            }
        }
    }

    @Nullable
    private BlockHitResult findContainerThroughWalls() {
        Vec3 start = new Vec3(pos.x, pos.y, pos.z);
        double range = extendedContainerAttempt.get() ? Math.max(containerRayRange.get(), containerAttemptRange.get()) : containerRayRange.get();
        Vec3 end = start.add(Vec3.directionFromRotation(pitch, yaw).scale(range));
        return BlockGetter.traverseBlocks(start, end, Boolean.TRUE, (ignored, position) -> {
            if (!mc.level.isInValidBounds(position) || !mc.level.hasChunk(position.getX() >> 4, position.getZ() >> 4)) return null;
            BlockPos target = position.immutable();
            if (!isContainer(target)) return null;
            boolean vanillaRange = mc.player.isWithinBlockInteractionRange(target, 1);
            if (!vanillaRange && (!extendedContainerAttempt.get()
                || !ContainerReach.withinAttemptRange(new AABB(target), mc.player.getEyePosition(), containerAttemptRange.get()))) return null;
            BlockState state = mc.level.getBlockState(target);
            return state.getShape(mc.level, target, CollisionContext.empty()).clip(start, end, target);
        }, ignored -> null);
    }

    private boolean isContainer(BlockPos blockPos) {
        BlockState state = mc.level.getBlockState(blockPos);
        return state.hasBlockEntity() && (state.getMenuProvider(mc.level, blockPos) != null || state.is(Blocks.ENDER_CHEST));
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (mc.player == null || mc.level == null || mc.getCameraEntity() == null) return;
        if (checkGuiMove()) {
            clearMovement();
            prevPos.set(pos);
            return;
        }
        if (!perspective.isFirstPerson()) mc.options.setCameraType(CameraType.FIRST_PERSON);

        Vec3 forward = Vec3.directionFromRotation(0, yaw);
        Vec3 right = Vec3.directionFromRotation(0, yaw + 90);
        double velX = 0;
        double velY = 0;
        double velZ = 0;

        if (rotate.get()) {
            BlockPos crossHairPos;
            Vec3 crossHairPosition;

            if (mc.hitResult instanceof EntityHitResult ehr) {
                crossHairPos = ehr.getEntity().blockPosition();
                Rotations.rotate(Rotations.getYaw(crossHairPos), Rotations.getPitch(crossHairPos), 0, null);
            } else if (mc.hitResult instanceof BlockHitResult hit) {
                crossHairPosition = hit.getLocation();
                crossHairPos = hit.getBlockPos();

                if (!mc.level.getBlockState(crossHairPos).isAir()) {
                    Rotations.rotate(Rotations.getYaw(crossHairPosition), Rotations.getPitch(crossHairPosition), 0, null);
                }
            }
        }

        double movementSpeed = 0.5 * speedValue * (Input.isPressed(mc.options.keySprint) ? sprintMultiplier.get() : 1);
        int longitudinal = (this.forward ? 1 : 0) - (this.backward ? 1 : 0);
        int lateral = (this.right ? 1 : 0) - (this.left ? 1 : 0);
        double horizontalScale = longitudinal != 0 && lateral != 0 ? 1 / Math.sqrt(2) : 1;
        velX = (forward.x * longitudinal + right.x * lateral) * movementSpeed * horizontalScale;
        velZ = (forward.z * longitudinal + right.z * lateral) * movementSpeed * horizontalScale;
        velY = ((this.up ? 1 : 0) - (this.down ? 1 : 0)) * movementSpeed * verticalMultiplier.get();

        prevPos.set(pos);
        pos.set(pos.x + velX, pos.y + velY, pos.z + velZ);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onKey(KeyInputEvent event) {
        if (Input.isKeyPressed(InputConstants.KEY_F3)) return;
        if (checkGuiMove()) return;

        if (onInput(event.key(), event.action)) event.cancel();
    }

    @Nullable
    private BlockPos rayCastEntity(Vec3 posVec, Vec3 max, short maxDist) {
        EntityHitResult res = ProjectileUtil.getEntityHitResult(
            mc.player,
            posVec,
            max,
            AABB.encapsulatingFullBlocks(BlockPos.containing(posVec.x, posVec.y, posVec.z), BlockPos.containing(max.x, max.y, max.z)),
            _ -> true,
            maxDist
        );

        if (res == null) return null;

        Vec3 vec = res.getLocation();

        return BlockPos.containing(vec.x, vec.y, vec.z);
    }

    @Nullable
    private BlockPos rayCastBlock(Vec3 posVec, Vec3 max) {
        ClipContext ctx = new ClipContext(
            posVec,
            max,
            ClipContext.Block.VISUAL,
            ClipContext.Fluid.SOURCE_ONLY,
            CollisionContext.empty()
        );

        BlockHitResult res = mc.level.clip(ctx);
        if (res.getType() == HitResult.Type.MISS) return null;

        // Don't move inside block
        return res.getBlockPos().offset(res.getDirection().getUnitVec3i());
    }

    private void setGoal() {
        long prevClick = clickTs;
        clickTs = System.currentTimeMillis();

        if (requireDoubleClick.get() && clickTs - prevClick > 500) return;

        Camera cam = mc.gameRenderer.mainCamera();
        Vec3 posVec = cam.position();
        Vec3 lookVec = Vec3.directionFromRotation(cam.xRot(), cam.yRot());
        short maxDist = 256;
        Vec3 max = posVec.add(lookVec.scale(maxDist));

        BlockPos pos = rayCastEntity(posVec, max, maxDist);
        if (pos == null) {
            pos = rayCastBlock(posVec, max);
        }

        if (pos == null) return;

        PathManagers.get().moveTo(pos);
    }

    @EventHandler(priority = EventPriority.HIGH)
    private void onMouseClick(MouseClickEvent event) {
        if (checkGuiMove()) return;

        if (baritoneClick.get() && event.action == KeyAction.Press && mc.options.keyAttack.matchesMouse(event.click)) {
            setGoal();
        }

        if (onInput(event.button(), event.action)) event.cancel();
    }

    private boolean onInput(int key, KeyAction action) {
        if (Input.getKey(mc.options.keyUp) == key) {
            forward = action != KeyAction.Release;
            mc.options.keyUp.setDown(false);
        } else if (Input.getKey(mc.options.keyDown) == key) {
            backward = action != KeyAction.Release;
            mc.options.keyDown.setDown(false);
        } else if (Input.getKey(mc.options.keyRight) == key) {
            right = action != KeyAction.Release;
            mc.options.keyRight.setDown(false);
        } else if (Input.getKey(mc.options.keyLeft) == key) {
            left = action != KeyAction.Release;
            mc.options.keyLeft.setDown(false);
        } else if (Input.getKey(mc.options.keyJump) == key) {
            up = action != KeyAction.Release;
            mc.options.keyJump.setDown(false);
        } else if (Input.getKey(mc.options.keyShift) == key) {
            down = action != KeyAction.Release;
            mc.options.keyShift.setDown(false);
        } else {
            return false;
        }

        return true;
    }

    @EventHandler(priority = EventPriority.LOW)
    private void onMouseScroll(MouseScrollEvent event) {
        if (speedScrollSensitivity.get() > 0 && mc.gui.screen() == null) {
            speedValue += event.value * 0.25 * (speedScrollSensitivity.get() * speedValue);
            if (speedValue < 0.1) speedValue = 0.1;

            event.cancel();
        }
    }

    @EventHandler
    private void onChunkOcclusion(ChunkOcclusionEvent event) {
        event.cancel();
    }

    @EventHandler
    private void onGameLeft(GameLeftEvent event) {
        if (!toggleOnLog.get()) return;

        toggle();
    }

    @EventHandler
    private void onPacketReceive(PacketEvent.Receive event) {
        if (mc.player == null || mc.level == null) return;
        if (event.packet instanceof ClientboundPlayerCombatKillPacket packet) {
            Entity entity = mc.level.getEntity(packet.playerId());
            if (entity == mc.player && toggleOnDeath.get()) {
                toggle();
                info("Toggled off because you died.");
            }
        } else if (event.packet instanceof ClientboundSetHealthPacket packet) {
            if (mc.player.getHealth() - packet.getHealth() > 0 && toggleOnDamage.get()) {
                toggle();
                info("Toggled off because you took damage.");
            }
        } else if (event.packet instanceof ClientboundRespawnPacket) {
            if (isActive()) {
                toggle();
                info("Toggled off because you changed dimensions.");
            }
        }
    }

    private boolean checkGuiMove() {
        GUIMove guiMove = Modules.get().get(GUIMove.class);
        if (mc.gui.screen() != null && !guiMove.isActive()) return true;
        return (mc.gui.screen() != null && guiMove.isActive() && guiMove.skip());
    }

    public void changeLookDirection(double deltaX, double deltaY) {
        lastYaw = yaw;
        lastPitch = pitch;

        yaw += (float) deltaX;
        pitch += (float) deltaY;

        pitch = Mth.clamp(pitch, -90, 90);
    }

    public boolean handlesContainerInteraction() {
        return isActive() && interactThroughWalls.get();
    }

    public boolean renderHands() {
        return !isActive() || renderHands.get();
    }

    public boolean staySneaking() {
        return isActive() && mc.player != null && !mc.player.getAbilities().flying && staySneaking.get() && isSneaking;
    }

    public double getX(float tickDelta) {
        return Mth.lerp(tickDelta, prevPos.x, pos.x);
    }

    public double getY(float tickDelta) {
        return Mth.lerp(tickDelta, prevPos.y, pos.y);
    }

    public double getZ(float tickDelta) {
        return Mth.lerp(tickDelta, prevPos.z, pos.z);
    }

    public double getYaw(float tickDelta) {
        return Mth.lerp(tickDelta, lastYaw, yaw);
    }

    public double getPitch(float tickDelta) {
        return Mth.lerp(tickDelta, lastPitch, pitch);
    }
}
