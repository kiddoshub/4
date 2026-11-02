
package com.gaspoweredcake.client33.mixin;

import com.mojang.authlib.GameProfile;
import com.gaspoweredcake.client33.mixininterface.IGuiMessageVisible;
import net.minecraft.client.multiplayer.chat.GuiMessage;
import net.minecraft.util.FormattedCharSequence;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

@Mixin(GuiMessage.Line.class)
public abstract class GuiMessageVisibleMixin implements IGuiMessageVisible {
    @Shadow
    @Final
    private FormattedCharSequence content;
    @Unique
    private int id;
    @Unique
    private GameProfile sender;
    @Unique
    private boolean startOfEntry;

    @Override
    public String client33$getText() {
        StringBuilder sb = new StringBuilder();

        content.accept((_, _, codePoint) -> {
            sb.appendCodePoint(codePoint);
            return true;
        });

        return sb.toString();
    }

    @Override
    public int client33$getId() {
        return id;
    }

    @Override
    public void client33$setId(int id) {
        this.id = id;
    }

    @Override
    public GameProfile client33$getSender() {
        return sender;
    }

    @Override
    public void client33$setSender(GameProfile profile) {
        sender = profile;
    }

    @Override
    public boolean client33$isStartOfEntry() {
        return startOfEntry;
    }

    @Override
    public void client33$setStartOfEntry(boolean start) {
        startOfEntry = start;
    }
}
