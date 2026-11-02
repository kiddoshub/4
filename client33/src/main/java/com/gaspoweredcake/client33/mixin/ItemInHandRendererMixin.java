package com.gaspoweredcake.client33.mixin;

import com.gaspoweredcake.client33.Client33;
import com.gaspoweredcake.client33.events.render.ArmRenderEvent;
import com.gaspoweredcake.client33.events.render.HeldItemRendererEvent;
import com.gaspoweredcake.client33.systems.modules.Modules;
import com.gaspoweredcake.client33.systems.modules.render.HandView;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.FirstPersonHandsAndItemsRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.level.FirstPersonHandsAndItemsRenderState;
import net.minecraft.client.renderer.state.level.PlayerRenderState;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import net.minecraft.tags.ItemTags;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FirstPersonHandsAndItemsRenderer.class)
public abstract class ItemInHandRendererMixin {
    @ModifyVariable(method = "submitArmWithItem", at = @At("HEAD"), argsOnly = true, ordinal = 2)
    private float modifySwing(float attack, @Local(argsOnly = true) InteractionHand hand, @Local(argsOnly = true) ItemStack item) {
        HandView module = Modules.get().get(HandView.class);
        if (!module.isActive()) return attack;
        if (module.swordSlash() && hand == InteractionHand.MAIN_HAND && item.is(ItemTags.SWORDS)) return 0;
        if (item.isEmpty()) return attack;
        return attack + (hand == InteractionHand.MAIN_HAND ? module.mainSwing.get() : module.offSwing.get()).floatValue();
    }

    @Inject(method = "submitArmWithItem", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/item/ItemStackRenderState;submit(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;III)V"))
    private void onRenderItem(PlayerRenderState player, FirstPersonHandsAndItemsRenderState state, float partialTicks, float xRot,
                              InteractionHand hand, float attack, ItemStack item, float height, PoseStack matrices,
                              SubmitNodeCollector collector, int light, CallbackInfo ci) {
        Client33.EVENT_BUS.post(HeldItemRendererEvent.get(hand, matrices));
    }

    @Inject(method = "submitArmWithItem", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/FirstPersonHandsAndItemsRenderer;renderPlayerArm(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;IFFLnet/minecraft/world/entity/HumanoidArm;Lnet/minecraft/client/renderer/state/level/PlayerRenderState;)V"))
    private void onRenderArm(PlayerRenderState player, FirstPersonHandsAndItemsRenderState state, float partialTicks, float xRot,
                             InteractionHand hand, float attack, ItemStack item, float height, PoseStack matrices,
                             SubmitNodeCollector collector, int light, CallbackInfo ci) {
        Client33.EVENT_BUS.post(ArmRenderEvent.get(hand, matrices));
    }

    @Inject(method = "applyEatTransform", at = @At(value = "INVOKE", target = "Ljava/lang/Math;pow(DD)D"), cancellable = true)
    private void cancelTransformations(PoseStack matrices, float partialTicks, HumanoidArm arm, float remainingTicks, int duration, CallbackInfo ci) {
        if (Modules.get().get(HandView.class).disableFoodAnimation()) ci.cancel();
    }
}
