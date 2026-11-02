
package com.gaspoweredcake.client33.mixin;

import com.mojang.authlib.GameProfile;
import com.gaspoweredcake.client33.mixininterface.IGuiMessage;
import net.minecraft.client.multiplayer.chat.GuiMessage;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

@Mixin(GuiMessage.class)
public abstract class GuiMessageMixin implements IGuiMessage {
    @Shadow
    @Final
    private Component content;
    @Unique
    private int id;
    @Unique
    private GameProfile sender;

    @Override
    public String client33$getText() {
        return content.getString();
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
}
