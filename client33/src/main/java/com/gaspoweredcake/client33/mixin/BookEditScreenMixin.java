
package com.gaspoweredcake.client33.mixin;

import com.gaspoweredcake.client33.utils.misc.NbtUtils;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.BookEditScreen;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

import static com.gaspoweredcake.client33.Client33.mc;

@Mixin(BookEditScreen.class)
public abstract class BookEditScreenMixin extends Screen {
    @Shadow
    @Final
    private List<String> pages;
    @Shadow
    private int currentPage;

    @Shadow
    protected abstract void updatePageContent();

    @Shadow
    protected abstract void pageForward();

    @Shadow
    protected abstract void pageBack();

    public BookEditScreenMixin(Component title) {
        super(title);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void onInit(CallbackInfo ci) {
        addRenderableWidget(
            new Button.Builder(Component.literal("Copy"), _ -> {
                ListTag listTag = new ListTag();
                pages.stream().map(StringTag::valueOf).forEach(listTag::add);

                CompoundTag tag = new CompoundTag();
                tag.put("pages", listTag);
                tag.putInt("currentPage", currentPage);

                NbtUtils.toClipboard(tag);
            })
                .pos(4, 4)
                .size(120, 20)
                .build()
        );

        addRenderableWidget(
            new Button.Builder(Component.literal("Paste"), _ -> {
                CompoundTag tag = NbtUtils.fromClipboard();
                if (tag == null || tag.getList("pages").isEmpty()) return;
                ListTag listTag = tag.getListOrEmpty("pages");
                List<String> replacement = new java.util.ArrayList<>();
                for (int i = 0; i < listTag.size(); ++i) replacement.add(listTag.getStringOr(i, ""));
                if (replacement.isEmpty()) replacement.add("");
                pages.clear();
                pages.addAll(replacement);
                currentPage = Math.max(0, Math.min(tag.getIntOr("currentPage", 0), pages.size() - 1));
                updatePageContent();
            })
                .pos(4, 4 + 20 + 2)
                .size(120, 20)
                .build()
        );
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (verticalAmount == 0) return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);

        if (verticalAmount < 0) this.pageForward(); // scroll down
        else this.pageBack();                       // scroll up
        return true;
    }
}
