package com.gaspoweredcake.client33.mixin;

import com.gaspoweredcake.client33.systems.modules.Modules;
import com.gaspoweredcake.client33.systems.modules.render.HandView;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.player.FirstPersonHandsAndItems;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(FirstPersonHandsAndItems.class)
public abstract class FirstPersonHandsAndItemsMixin {
    @Shadow private float mainHandHeight;
    @Shadow private float offHandHeight;
    @Shadow private ItemStack mainHandItem;
    @Shadow private ItemStack offHandItem;
    @Shadow protected abstract boolean shouldInstantlyReplaceVisibleItem(ItemStack current, ItemStack expected, LocalPlayer player);

    @ModifyReturnValue(method = "shouldInstantlyReplaceVisibleItem", at = @At("RETURN"))
    private boolean modifySkipSwapAnimation(boolean original) {
        return original || Modules.get().get(HandView.class).skipSwapping();
    }

    @ModifyArg(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/Mth;clamp(FFF)F", ordinal = 2), index = 0)
    private float modifyEquipProgressMainhand(float value, @Local(argsOnly = true) LocalPlayer player) {
        HandView view = Modules.get().get(HandView.class);
        if (view.swordSlash() && player.getMainHandItem().is(ItemTags.SWORDS)) return value;
        float scale = player.getItemSwapScale(1);
        float height = view.oldAnimations() ? 1 : scale * scale * scale;
        return (shouldInstantlyReplaceVisibleItem(mainHandItem, player.getMainHandItem(), player) ? height : 0) - mainHandHeight;
    }

    @ModifyArg(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/Mth;clamp(FFF)F", ordinal = 3), index = 0)
    private float modifyEquipProgressOffhand(float value, @Local(argsOnly = true) LocalPlayer player) {
        return (shouldInstantlyReplaceVisibleItem(offHandItem, player.getOffhandItem(), player) ? 1 : 0) - offHandHeight;
    }
}
