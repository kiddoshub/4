
package com.gaspoweredcake.client33.systems.modules.movement;

import com.gaspoweredcake.client33.events.world.TickEvent;
import com.gaspoweredcake.client33.renderer.ShapeMode;
import com.gaspoweredcake.client33.settings.*;
import com.gaspoweredcake.client33.systems.modules.Categories;
import com.gaspoweredcake.client33.systems.modules.Module;
import com.gaspoweredcake.client33.utils.Utils;
import com.gaspoweredcake.client33.utils.misc.ListMode;
import com.gaspoweredcake.client33.utils.player.FindItemResult;
import com.gaspoweredcake.client33.utils.player.InvUtils;
import com.gaspoweredcake.client33.utils.player.PlayerUtils;
import com.gaspoweredcake.client33.utils.player.Rotations;
import com.gaspoweredcake.client33.utils.player.RotationStep;
import com.gaspoweredcake.client33.utils.render.RenderUtils;
import com.gaspoweredcake.client33.utils.render.color.SettingColor;
import com.gaspoweredcake.client33.utils.world.BlockUtils;
import com.gaspoweredcake.client33.utils.world.ScaffoldFace;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class Scaffold extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgRender = settings.createGroup("Render");

    private final Setting<List<Block>> blocks = sgGeneral.add(new BlockListSetting.Builder()
        .name("blocks")
        .description("Selected blocks.")
        .build()
    );

    private final Setting<ListMode> blocksFilter = sgGeneral.add(new EnumSetting.Builder<ListMode>()
        .name("blocks-filter")
        .description("How to use the block list setting")
        .defaultValue(ListMode.Blacklist)
        .build()
    );

    private final Setting<Boolean> fastTower = sgGeneral.add(new BoolSetting.Builder()
        .name("fast-tower")
        .description("Whether or not to scaffold upwards faster.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Double> towerSpeed = sgGeneral.add(new DoubleSetting.Builder()
        .name("tower-speed")
        .description("The speed at which to tower.")
        .defaultValue(0.5)
        .min(0)
        .sliderMax(1)
        .visible(fastTower::get)
        .build()
    );

    private final Setting<Boolean> whileMoving = sgGeneral.add(new BoolSetting.Builder()
        .name("while-moving")
        .description("Allows you to tower while moving.")
        .defaultValue(false)
        .visible(fastTower::get)
        .build()
    );

    private final Setting<Boolean> onlyOnClick = sgGeneral.add(new BoolSetting.Builder()
        .name("only-on-click")
        .description("Only places blocks when holding right click.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> renderSwing = sgGeneral.add(new BoolSetting.Builder()
        .name("swing")
        .description("Renders your client-side swing.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> autoSwitch = sgGeneral.add(new BoolSetting.Builder()
        .name("auto-switch")
        .description("Automatically swaps to a block before placing.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> rotate = sgGeneral.add(new BoolSetting.Builder()
        .name("rotate")
        .description("Rotates towards the blocks being placed.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> instantRotation = sgGeneral.add(new BoolSetting.Builder()
        .name("instant-rotation").description("Rotate immediately to the placement face.")
        .defaultValue(false).visible(rotate::get).build());

    private final Setting<Double> rotationStep = sgGeneral.add(new DoubleSetting.Builder()
        .name("rotation-step").description("Maximum yaw and pitch change per tick when instant rotation is disabled.")
        .defaultValue(45).range(1, 180).sliderRange(1, 180)
        .visible(() -> rotate.get() && !instantRotation.get()).build());

    private final Setting<Boolean> airPlace = sgGeneral.add(new BoolSetting.Builder()
        .name("air-place")
        .description("Allow air place. This also allows you to modify scaffold radius.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Double> aheadDistance = sgGeneral.add(new DoubleSetting.Builder()
        .name("ahead-distance")
        .description("How far ahead to place blocks.")
        .defaultValue(0)
        .min(0)
        .sliderMax(1)
        .visible(() -> !airPlace.get())
        .build()
    );

    private final Setting<Double> placeRange = sgGeneral.add(new DoubleSetting.Builder()
        .name("closest-block-range")
        .description("How far can scaffold place blocks when you are in air.")
        .defaultValue(4)
        .min(0)
        .sliderMax(8)
        .visible(() -> !airPlace.get())
        .build()
    );

    private final Setting<Double> radius = sgGeneral.add(new DoubleSetting.Builder()
        .name("radius")
        .description("Scaffold radius.")
        .defaultValue(0)
        .min(0)
        .max(6)
        .visible(airPlace::get)
        .build()
    );

    private final Setting<Integer> blocksPerTick = sgGeneral.add(new IntSetting.Builder()
        .name("blocks-per-tick")
        .description("How many blocks to place in one tick.")
        .defaultValue(3)
        .min(1)
        .visible(airPlace::get)
        .build()
    );

    // Render

    private final Setting<Boolean> render = sgRender.add(new BoolSetting.Builder()
        .name("render")
        .description("Whether to render blocks that have been placed.")
        .defaultValue(true)
        .build()
    );

    private final Setting<ShapeMode> shapeMode = sgRender.add(new EnumSetting.Builder<ShapeMode>()
        .name("shape-mode")
        .description("How the shapes are rendered.")
        .defaultValue(ShapeMode.Both)
        .visible(render::get)
        .build()
    );

    private final Setting<SettingColor> sideColor = sgRender.add(new ColorSetting.Builder()
        .name("side-color")
        .description("The side color of the target block rendering.")
        .defaultValue(new SettingColor(197, 137, 232, 10))
        .visible(render::get)
        .build()
    );

    private final Setting<SettingColor> lineColor = sgRender.add(new ColorSetting.Builder()
        .name("line-color")
        .description("The line color of the target block rendering.")
        .defaultValue(new SettingColor(197, 137, 232))
        .visible(render::get)
        .build()
    );

    private final BlockPos.MutableBlockPos bp = new BlockPos.MutableBlockPos();
    private long placementGeneration;
    private boolean rotationQueued;
    private boolean hasScaffoldRotation;

    public Scaffold() {
        super(Categories.Movement, "scaffold", "Automatically places blocks under you.");
    }


    @Override public void onActivate() { hasScaffoldRotation = false; }
    @Override public void onDeactivate() { placementGeneration++; hasScaffoldRotation = false; }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        placementGeneration++;
        rotationQueued = false;
        if (!Utils.canUpdate()) return;
        if (onlyOnClick.get() && !mc.options.keyUse.isDown()) return;

        Vec3 vec = mc.player.position().add(mc.player.getDeltaMovement()).add(0, -0.75, 0);
        if (airPlace.get()) {
            bp.set(vec.x(), vec.y(), vec.z());
        } else {
            Vec3 pos = mc.player.position().add(mc.player.getDeltaMovement().multiply(1, 0, 1));
            BlockPos below = mc.player.blockPosition().below();
            if (aheadDistance.get() != 0 && !towering() && !mc.level.getBlockState(below).getCollisionShape(mc.level, below).isEmpty()) {
                Vec3 dir = Vec3.directionFromRotation(0, mc.player.getYRot()).multiply(aheadDistance.get(), 0, aheadDistance.get());
                if (mc.options.keyUp.isDown()) pos = pos.add(dir.x, 0, dir.z);
                if (mc.options.keyDown.isDown()) pos = pos.add(-dir.x, 0, -dir.z);
                if (mc.options.keyLeft.isDown()) pos = pos.add(dir.z, 0, -dir.x);
                if (mc.options.keyRight.isDown()) pos = pos.add(-dir.z, 0, dir.x);
            }
            bp.set(pos.x, vec.y, pos.z);
        }
        if (mc.options.keyShift.isDown() && !mc.options.keyJump.isDown() && bp.getY() > mc.level.getMinY()) {
            bp.setY(bp.getY() - 1);
        }
        if (bp.getY() >= mc.player.blockPosition().getY()) {
            bp.setY(mc.player.blockPosition().getY() - 1);
        }
        BlockPos targetBlock = bp.immutable();

        if (!airPlace.get() && findSupport(bp) == null) {
            Vec3 eye = mc.player.getEyePosition();
            double reach = mc.player.blockInteractionRange();
            // Any reachable face is within this conservative cube around the feet.
            double searchRange = Math.min(placeRange.get(), reach + mc.player.getEyeHeight(mc.player.getPose()) + 2);
            double reachSq = reach * reach;
            BlockPos best = null;
            double bestDistance = Double.MAX_VALUE;
            int minY = Math.max(mc.level.getMinY(), Mth.floor(mc.player.getY() - searchRange));
            int maxY = Math.min(mc.level.getMinY() + mc.level.getHeight() - 1, Mth.floor(mc.player.getY() + searchRange));

            for (int x = Mth.floor(mc.player.getX() - searchRange); x <= Mth.floor(mc.player.getX() + searchRange); x++) {
                for (int z = Mth.floor(mc.player.getZ() - searchRange); z <= Mth.floor(mc.player.getZ() + searchRange); z++) {
                    for (int y = minY; y <= maxY; y++) {
                        bp.set(x, y, z);
                        if (!BlockUtils.canPlace(bp)) continue;
                        BlockHitResult support = findSupport(bp);
                        if (support == null) continue;
                        Vec3 hitPos = support.getLocation();
                        if (eye.distanceToSqr(hitPos) > reachSq) continue;

                        double distance = bp.distSqr(targetBlock);
                        if (distance < bestDistance) {
                            bestDistance = distance;
                            best = bp.immutable();
                        }
                    }
                }
            }
            if (best == null) return;
            bp.set(best);
        }

        if (airPlace.get()) {
            List<BlockPos> blocks = new ArrayList<>();
            double radiusSq = radius.get() * radius.get();
            for (int x = Mth.ceil(bp.getX() - radius.get()); x <= Mth.floor(bp.getX() + radius.get()); x++) {
                for (int z = Mth.ceil(bp.getZ() - radius.get()); z <= Mth.floor(bp.getZ() + radius.get()); z++) {
                    BlockPos blockPos = BlockPos.containing(x, bp.getY(), z);
                    int dx = x - bp.getX();
                    int dz = z - bp.getZ();
                    if (dx * dx + dz * dz <= radiusSq) {
                        blocks.add(blockPos);
                    }
                }
            }

            if (!blocks.isEmpty()) {
                blocks.sort(Comparator.comparingDouble(PlayerUtils::squaredDistanceTo));
                int counter = 0;
                for (BlockPos block : blocks) {
                    if (place(block)) {
                        counter++;
                    }

                    if (counter >= blocksPerTick.get()) {
                        break;
                    }
                }
            }
        } else {
            place(bp);
        }

        FindItemResult result = InvUtils.findInHotbar(itemStack -> validItem(itemStack, mc.player.blockPosition().below()));
        if (fastTower.get() && mc.options.keyJump.isDown() && !mc.options.keyShift.isDown()
            && (whileMoving.get() || !PlayerUtils.isMoving()) && result.found()
            && (autoSwitch.get() || result.getHand() != null) && hasTowerSupport()) {
            Vec3 velocity = mc.player.getDeltaMovement();
            AABB playerBox = mc.player.getBoundingBox();
            if (!mc.level.getBlockCollisions(mc.player, playerBox.move(0, 1, 0)).iterator().hasNext()) {
                mc.player.setDeltaMovement(velocity.x, towerSpeed.get(), velocity.z);
            } else if (velocity.y > 0) {
                mc.player.setDeltaMovement(velocity.x, 0, velocity.z);
            }
        }
    }

    public boolean scaffolding() {
        return isActive() && (!onlyOnClick.get() || (onlyOnClick.get() && mc.options.keyUse.isDown()));
    }

    public boolean towering() {
        if (!Utils.canUpdate()) return false;
        FindItemResult result = InvUtils.findInHotbar(itemStack -> validItem(itemStack, mc.player.blockPosition().below()));
        return scaffolding() && fastTower.get() && mc.options.keyJump.isDown() && !mc.options.keyShift.isDown() &&
            (whileMoving.get() || !PlayerUtils.isMoving()) && result.found() && (autoSwitch.get() || result.getHand() != null) && hasTowerSupport();
    }

    private boolean hasTowerSupport() {
        BlockPos below = mc.player.blockPosition().below();
        if (!mc.level.getBlockState(below).getCollisionShape(mc.level, below).isEmpty()) return true;
        return BlockUtils.canPlace(below) && (airPlace.get() || BlockUtils.getPlaceSide(below) != null);
    }

    private boolean validItem(ItemStack itemStack, BlockPos pos) {
        if (!(itemStack.getItem() instanceof BlockItem)) return false;

        Block block = ((BlockItem) itemStack.getItem()).getBlock();

        if (!blocksFilter.get().allows(blocks.get().contains(block))) return false;

        if (!Block.isShapeFullBlock(block.defaultBlockState().getCollisionShape(mc.level, pos))) return false;
        return !(block instanceof FallingBlock) || !FallingBlock.isFree(mc.level.getBlockState(pos.below()));
    }

    private boolean place(BlockPos pos) {
        BlockPos target = pos.immutable();
        FindItemResult item = InvUtils.findInHotbar(stack -> validItem(stack, target));
        if (!item.found() || (item.getHand() == null && !autoSwitch.get())) return false;
        if (!BlockUtils.canPlace(target)) return false;
        BlockHitResult support = airPlace.get() ? null : findSupport(target);
        if (support == null && !airPlace.get()) return false;

        var owner = mc.player;
        var world = mc.level;
        int selected = owner.getInventory().getSelectedSlot();
        int itemSlot = item.slot();
        ItemStack expected = (item.isOffhand() ? owner.getOffhandItem() : owner.getInventory().getItem(itemSlot)).copy();
        long generation = placementGeneration;
        Vec3 hit = support == null ? Vec3.atCenterOf(target) : support.getLocation();
        Runnable placement = () -> {
            if (!isActive() || generation != placementGeneration || mc.player != owner || mc.level != world
                || mc.gameMode == null || !owner.isAlive() || (onlyOnClick.get() && !mc.options.keyUse.isDown())
                || owner.getInventory().getSelectedSlot() != selected) return;
            ItemStack current = item.isOffhand() ? owner.getOffhandItem() : owner.getInventory().getItem(itemSlot);
            if (current.isEmpty() || !ItemStack.isSameItemSameComponents(expected, current) || !validItem(current, target)
                || !BlockUtils.canPlace(target)) return;
            if (support != null) {
                BlockHitResult currentHit = findSupport(target);
                if (currentHit == null || !currentHit.getBlockPos().equals(support.getBlockPos())
                    || currentHit.getDirection() != support.getDirection()
                    || currentHit.getLocation().distanceToSqr(support.getLocation()) > 1e-8) return;
                if (!InvUtils.swap(item.slot(), true)) return;
                try { BlockUtils.interact(currentHit, item.isOffhand() ? net.minecraft.world.InteractionHand.OFF_HAND : net.minecraft.world.InteractionHand.MAIN_HAND, renderSwing.get()); }
                finally { if (!item.isOffhand()) InvUtils.swapBack(); }
            } else if (!airPlace.get() || !BlockUtils.place(target, item, false, 50, renderSwing.get(), true)) return;
            if (render.get())
                RenderUtils.renderTickingBlock(target, sideColor.get(), lineColor.get(), shapeMode.get(), 0, 8, true, false);
        };
        if (!rotate.get()) placement.run();
        else if (instantRotation.get()) Rotations.rotate(Rotations.getYaw(hit), Rotations.getPitch(hit), 50, placement);
        else {
            if (rotationQueued) return false;
            rotationQueued = true;
            double yaw = Rotations.getYaw(hit), pitch = Rotations.getPitch(hit);
            boolean recent = hasScaffoldRotation && Rotations.rotationTimer <= 1;
            RotationStep step = RotationStep.toward(recent ? Rotations.serverYaw : owner.getYRot(),
                recent ? Rotations.serverPitch : owner.getXRot(), yaw, pitch, rotationStep.get());
            Rotations.rotate(step.yaw(), step.pitch(), 50, () -> {
                if (!isActive() || generation != placementGeneration || mc.player != owner || mc.level != world) return;
                hasScaffoldRotation = true;
                if (step.ready()) placement.run();
            });
        }
        return true;
    }

    @SuppressWarnings("deprecation") // The 26.3 legacy solidity check distinguishes support blocks from thin decorations.
    private BlockHitResult findSupport(BlockPos target) {
        Vec3 eye = mc.player.getEyePosition();
        Vec3 look = Vec3.atCenterOf(target).subtract(eye);
        double best = -Double.MAX_VALUE;
        BlockHitResult selected = null;
        for (Direction side : Direction.values()) {
            BlockPos pos = target.relative(side);
            var state = mc.level.getBlockState(pos);
            if (!state.isSolid() || !state.getFluidState().isEmpty() || BlockUtils.isClickable(state.getBlock())
                || state.getCollisionShape(mc.level, pos).isEmpty()) continue;
            var shape = state.getShape(mc.level, pos, CollisionContext.of(mc.player));
            BlockHitResult hit = ScaffoldFace.hit(shape, pos, side.getOpposite());
            if (hit == null || eye.distanceToSqr(hit.getLocation()) > mc.player.blockInteractionRange() * mc.player.blockInteractionRange()) continue;
            double relevance = side.getAxis().choose(look.x, look.y, look.z) * side.getAxisDirection().getStep();
            if (relevance > best) { best = relevance; selected = hit; }
        }
        return selected;
    }

}
