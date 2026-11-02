
package com.gaspoweredcake.client33.mixin;

import com.gaspoweredcake.client33.mixininterface.IServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(ServerboundMovePlayerPacket.class)
public abstract class ServerboundMovePlayerPacketMixin implements IServerboundMovePlayerPacket {
    @Unique
    private int tag;

    @Override
    public void client33$setTag(int tag) {
        this.tag = tag;
    }

    @Override
    public int client33$getTag() {
        return this.tag;
    }
}
