
package com.gaspoweredcake.client33.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.gaspoweredcake.client33.systems.modules.Modules;
import com.gaspoweredcake.client33.systems.modules.movement.NoSlow;
import com.gaspoweredcake.client33.systems.modules.movement.Slippy;
import com.gaspoweredcake.client33.systems.modules.render.Xray;
import com.gaspoweredcake.client33.utils.misc.ListMode;
import net.minecraft.core.Direction;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Block.class)
public abstract class BlockMixin extends BlockBehaviour implements ItemLike {
    public BlockMixin(Properties properties) {
        super(properties);
    }

    @ModifyReturnValue(method = "getFriction", at = @At("RETURN"))
    public float getFriction(float original) {
        // For some retarded reason Tweakeroo calls this method before client33 is initialized
        if (Modules.get() == null) return original;

        Slippy slippy = Modules.get().get(Slippy.class);
        Block block = (Block) (Object) this;

        boolean blockInList = (slippy.listMode.get() == ListMode.Whitelist ? slippy.allowedBlocks.get() : slippy.ignoredBlocks.get()).contains(block);
        if (slippy.isActive() && slippy.listMode.get().allows(blockInList)) {
            return slippy.friction.get().floatValue();
        }

        if (block == Blocks.SLIME_BLOCK && Modules.get().get(NoSlow.class).slimeBlock()) return 0.6F;
        else return original;
    }

    // For More Culling compatibility - runs before More Culling's inject to force-render whitelisted Xray blocks
    @Inject(method = "shouldRenderFace", at = @At("HEAD"), cancellable = true)
    private static void client33$forceXrayFace(BlockState state, BlockState neighborState, Direction direction, CallbackInfoReturnable<Boolean> cir) {
        Modules modules = Modules.get();
        if (modules == null) return;

        Xray xray = modules.get(Xray.class);
        if (xray.isActive() && !xray.isBlocked(state.getBlock(), null)) {
            cir.setReturnValue(true);
        }
    }
}
