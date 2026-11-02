
package com.gaspoweredcake.client33.mixininterface;

import com.mojang.authlib.GameProfile;
import net.minecraft.client.multiplayer.chat.GuiMessageSource;
import net.minecraft.client.multiplayer.chat.GuiMessageTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MessageSignature;

public interface IChatListener {
    /**
     * Only valid inside of {@link net.minecraft.client.gui.components.ChatComponent#addMessage(Component, MessageSignature, GuiMessageSource, GuiMessageTag)} call
     */
    GameProfile client33$getSender();
}
