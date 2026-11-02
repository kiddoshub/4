
package com.gaspoweredcake.client33.mixininterface;

import com.mojang.authlib.GameProfile;

public interface IGuiMessage {
    String client33$getText();

    int client33$getId();

    void client33$setId(int id);

    GameProfile client33$getSender();

    void client33$setSender(GameProfile profile);
}
