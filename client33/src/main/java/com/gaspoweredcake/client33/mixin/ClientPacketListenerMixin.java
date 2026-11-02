
package com.gaspoweredcake.client33.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalBooleanRef;
import com.llamalad7.mixinextras.sugar.ref.LocalFloatRef;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.gaspoweredcake.client33.Client33;
import com.gaspoweredcake.client33.commands.Commands;
import com.gaspoweredcake.client33.events.entity.EntityDestroyEvent;
import com.gaspoweredcake.client33.events.entity.player.PickItemsEvent;
import com.gaspoweredcake.client33.events.game.GameJoinedEvent;
import com.gaspoweredcake.client33.events.game.GameLeftEvent;
import com.gaspoweredcake.client33.events.game.SendMessageEvent;
import com.gaspoweredcake.client33.events.packets.ContainerSlotUpdateEvent;
import com.gaspoweredcake.client33.events.packets.InventoryEvent;
import com.gaspoweredcake.client33.events.packets.SlotPacketObservedEvent;
import com.gaspoweredcake.client33.events.packets.AppliedSpawnerDataEvent;
import com.gaspoweredcake.client33.events.packets.PlaySoundPacketEvent;
import com.gaspoweredcake.client33.events.world.ChunkDataEvent;
import com.gaspoweredcake.client33.events.world.AppliedBlockUpdateEvent;
import com.gaspoweredcake.client33.events.world.AppliedEntityMetadataEvent;
import com.gaspoweredcake.client33.events.world.ServerPositionEvent;
import com.gaspoweredcake.client33.mixininterface.IClientboundExplodePacket;
import com.gaspoweredcake.client33.pathing.BaritoneUtils;
import com.gaspoweredcake.client33.systems.config.Config;
import com.gaspoweredcake.client33.systems.modules.Modules;
import com.gaspoweredcake.client33.systems.modules.movement.Velocity;
import com.gaspoweredcake.client33.systems.modules.player.NoRotate;
import com.gaspoweredcake.client33.systems.modules.render.NoRender;
import com.gaspoweredcake.client33.utils.player.ChatUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientCommonPacketListenerImpl;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.CommonListenerCookie;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.*;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public abstract class ClientPacketListenerMixin extends ClientCommonPacketListenerImpl {
    @Shadow
    private ClientLevel level;

    protected ClientPacketListenerMixin(Minecraft client, Connection connection, CommonListenerCookie connectionState) {
        super(client, connection, connectionState);
    }

    @Inject(method = "handleAddEntity", at = @At("HEAD"), cancellable = true)
    private void onHandleAddEntity(ClientboundAddEntityPacket packet, CallbackInfo ci) {
        if (packet != null && packet.getType() != null) {
            if (Modules.get().get(NoRender.class).noEntity(packet.getType()) && Modules.get().get(NoRender.class).getDropSpawnPacket()) {
                ci.cancel();
            }
        }
    }

    @Inject(method = "handleLogin", at = @At("HEAD"))
    private void onHandleLoginHead(ClientboundLoginPacket packet, CallbackInfo ci, @Share("worldNotNull") LocalBooleanRef worldNotNull) {
        worldNotNull.set(level != null);
    }

    @Inject(method = "handleLogin", at = @At("TAIL"))
    private void onHandleLoginTail(ClientboundLoginPacket packet, CallbackInfo ci, @Share("worldNotNull") LocalBooleanRef worldNotNull) {
        if (worldNotNull.get()) {
            Client33.EVENT_BUS.post(GameLeftEvent.get());
        }

        Client33.EVENT_BUS.post(GameJoinedEvent.get());
    }

    // the server sends a GameJoin packet after the reconfiguration phase
    @Inject(method = "handleConfigurationStart", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/protocol/PacketUtils;ensureRunningOnSameThread(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketListener;Lnet/minecraft/network/PacketProcessor;)V", shift = At.Shift.AFTER))
    private void onHandleConfigurationStart(ClientboundStartConfigurationPacket packet, CallbackInfo ci) {
        Client33.EVENT_BUS.post(GameLeftEvent.get());
    }

    @Inject(method = "handleSoundEvent", at = @At("TAIL"))
    private void onHandleSoundEvent(ClientboundSoundPacket packet, CallbackInfo ci) {
        Client33.EVENT_BUS.post(PlaySoundPacketEvent.get(packet));
    }

    @Inject(method = "handleLevelChunkWithLight", at = @At("TAIL"))
    private void onHandleLevelChunkWithLight(ClientboundLevelChunkWithLightPacket packet, CallbackInfo ci) {
        LevelChunk chunk = minecraft.level.getChunk(packet.x(), packet.z());
        Client33.EVENT_BUS.post(new ChunkDataEvent(chunk));
        Client33.EVENT_BUS.post(new AppliedSpawnerDataEvent(packet));
    }

    @Inject(method = "handleAddEntity", at = @At("TAIL"))
    private void onRawEntitySpawn(ClientboundAddEntityPacket packet, CallbackInfo ci) {
        if (level == null) return;
        Entity entity = level.getEntity(packet.getId());
        if (entity != null) Client33.EVENT_BUS.post(new ServerPositionEvent(entity,
            new Vec3(packet.getX(), packet.getY(), packet.getZ()), "entity-spawn", System.nanoTime()));
    }

    @Inject(method = "handleMoveEntity", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/protocol/PacketUtils;ensureRunningOnSameThread(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketListener;Lnet/minecraft/network/PacketProcessor;)V", shift = At.Shift.AFTER))
    private void onRawEntityMove(ClientboundMoveEntityPacket packet, CallbackInfo ci) {
        if (!packet.hasPosition() || level == null) return;
        Entity entity = packet.getEntity(level);
        if (entity == null) return;
        Vec3 position = packet.getPositionDelta().decode(entity.getPositionCodec()).endPosition();
        Client33.EVENT_BUS.post(new ServerPositionEvent(entity, position, "relative-move", System.nanoTime()));
    }

    @Inject(method = "handleEntityPositionSync", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/protocol/PacketUtils;ensureRunningOnSameThread(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketListener;Lnet/minecraft/network/PacketProcessor;)V", shift = At.Shift.AFTER))
    private void onRawEntitySync(ClientboundEntityPositionSyncPacket packet, CallbackInfo ci) {
        if (level == null) return;
        Entity entity = level.getEntity(packet.id());
        if (entity != null) Client33.EVENT_BUS.post(new ServerPositionEvent(entity, packet.position().endPosition(), "position-sync", System.nanoTime()));
    }

    @Inject(method = "handleTeleportEntity", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/protocol/PacketUtils;ensureRunningOnSameThread(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketListener;Lnet/minecraft/network/PacketProcessor;)V", shift = At.Shift.AFTER))
    private void onRawEntityTeleport(ClientboundTeleportEntityPacket packet, CallbackInfo ci) {
        if (level == null) return;
        Entity entity = level.getEntity(packet.id());
        if (entity == null) return;
        Vec3 position = net.minecraft.world.entity.PositionMoveRotation.calculateAbsolute(
            net.minecraft.world.entity.PositionMoveRotation.of(entity), packet.change(), packet.relatives()).position();
        Client33.EVENT_BUS.post(new ServerPositionEvent(entity, position, "teleport", System.nanoTime()));
    }

    @Inject(method = "handleSetEntityData", at = @At("TAIL"))
    private void onAppliedEntityData(ClientboundSetEntityDataPacket packet, CallbackInfo ci) {
        Client33.EVENT_BUS.post(new AppliedEntityMetadataEvent(packet));
    }

    @Inject(method = "handleBlockUpdate", at = @At("TAIL"))
    private void onAppliedBlockUpdate(ClientboundBlockUpdatePacket packet, CallbackInfo ci) {
        Client33.EVENT_BUS.post(new AppliedBlockUpdateEvent(packet));
    }

    @Inject(method = "handleChunkBlocksUpdate", at = @At("TAIL"))
    private void onAppliedChunkBlocksUpdate(ClientboundSectionBlocksUpdatePacket packet, CallbackInfo ci) {
        Client33.EVENT_BUS.post(new AppliedBlockUpdateEvent(packet));
    }

    @Inject(method = "handleLightUpdatePacket", at = @At("TAIL"))
    private void onAppliedLightUpdate(ClientboundLightUpdatePacket packet, CallbackInfo ci) {
        Client33.EVENT_BUS.post(new AppliedBlockUpdateEvent(packet));
    }

    @Inject(method = "handleBlockEntityData", at = @At("TAIL"))
    private void onAppliedBlockEntityData(ClientboundBlockEntityDataPacket packet, CallbackInfo ci) {
        Client33.EVENT_BUS.post(new AppliedSpawnerDataEvent(packet));
    }

    @Inject(method = "handleForgetLevelChunk", at = @At("TAIL"))
    private void onAppliedForgetChunk(ClientboundForgetLevelChunkPacket packet, CallbackInfo ci) {
        Client33.EVENT_BUS.post(new AppliedSpawnerDataEvent(packet));
    }

    @Inject(method = "handleContainerSetSlot", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/protocol/PacketUtils;ensureRunningOnSameThread(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketListener;Lnet/minecraft/network/PacketProcessor;)V", shift = At.Shift.AFTER))
    private void onObserveContainerSetSlot(ClientboundContainerSetSlotPacket packet, CallbackInfo ci) {
        Client33.EVENT_BUS.post(new SlotPacketObservedEvent(packet));
    }

    @Inject(method = "handleContainerContent", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/protocol/PacketUtils;ensureRunningOnSameThread(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketListener;Lnet/minecraft/network/PacketProcessor;)V", shift = At.Shift.AFTER))
    private void onObserveContainerContent(ClientboundContainerSetContentPacket packet, CallbackInfo ci) {
        Client33.EVENT_BUS.post(new SlotPacketObservedEvent(packet));
    }

    @Inject(method = "handleContainerSetSlot", at = @At("TAIL"))
    private void onHandleContainerSetSlot(ClientboundContainerSetSlotPacket packet, CallbackInfo ci) {
        Client33.EVENT_BUS.post(ContainerSlotUpdateEvent.get(packet));
    }

    @Inject(method = "handleContainerContent", at = @At("TAIL"))
    private void onHandleContainerContent(ClientboundContainerSetContentPacket packet, CallbackInfo ci) {
        Client33.EVENT_BUS.post(InventoryEvent.get(packet));
    }

    @Inject(method = "handleRemoveEntities", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/protocol/game/ClientboundRemoveEntitiesPacket;entityIds()Lit/unimi/dsi/fastutil/ints/IntList;"))
    private void onHandleRemoveEntities(ClientboundRemoveEntitiesPacket packet, CallbackInfo ci) {
        for (int id : packet.entityIds()) {
            Client33.EVENT_BUS.post(EntityDestroyEvent.get(minecraft.level.getEntity(id)));
        }
    }

    @Inject(method = "handleExplosion", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/protocol/PacketUtils;ensureRunningOnSameThread(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketListener;Lnet/minecraft/network/PacketProcessor;)V", shift = At.Shift.AFTER))
    private void onHandleExplosionVelocity(ClientboundExplodePacket packet, CallbackInfo ci) {
        Velocity velocity = Modules.get().get(Velocity.class);
        if (!velocity.explosions.get()) return;

        IClientboundExplodePacket explosionPacket = (IClientboundExplodePacket) (Object) packet;
        explosionPacket.client33$setVelocityX((float) (packet.playerKnockback().orElse(Vec3.ZERO).x * velocity.getHorizontal(velocity.explosionsHorizontal)));
        explosionPacket.client33$setVelocityY((float) (packet.playerKnockback().orElse(Vec3.ZERO).y * velocity.getVertical(velocity.explosionsVertical)));
        explosionPacket.client33$setVelocityZ((float) (packet.playerKnockback().orElse(Vec3.ZERO).z * velocity.getHorizontal(velocity.explosionsHorizontal)));
    }

    @Inject(method = "handleTakeItemEntity", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/ClientLevel;getEntity(I)Lnet/minecraft/world/entity/Entity;", ordinal = 0))
    private void onHandleTakeItemEntity(ClientboundTakeItemEntityPacket packet, CallbackInfo ci) {
        Entity itemEntity = minecraft.level.getEntity(packet.getItemId());
        Entity entity = minecraft.level.getEntity(packet.getPlayerId());

        if (itemEntity instanceof ItemEntity item && entity == minecraft.player) {
            Client33.EVENT_BUS.post(PickItemsEvent.get(item.getItem(), packet.getAmount()));
        }
    }

    @Inject(method = "handleMovePlayer", at = @At("HEAD"))
    private void onHandleMovePlayerHead(ClientboundPlayerPositionPacket packet, CallbackInfo ci, @Share("noRotateYaw") LocalFloatRef yawRef, @Share("noRotatePitch") LocalFloatRef pitchRef) {
        NoRotate noRotate = Modules.get().get(NoRotate.class);
        if (!noRotate.isActive() || minecraft.player == null) return;

        yawRef.set(minecraft.player.getYRot());
        pitchRef.set(minecraft.player.getXRot());
    }

    @Inject(method = "handleMovePlayer", at = @At("RETURN"))
    private void onHandleMovePlayerReturn(ClientboundPlayerPositionPacket packet, CallbackInfo ci, @Share("noRotateYaw") LocalFloatRef yawRef, @Share("noRotatePitch") LocalFloatRef pitchRef) {
        NoRotate noRotate = Modules.get().get(NoRotate.class);
        if (!noRotate.isActive() || minecraft.player == null) return;

        float savedYaw = yawRef.get();
        float savedPitch = pitchRef.get();

        //not noticeable by player but forces a server update
        minecraft.player.setYRot(savedYaw + 0.000001f);
        minecraft.player.setXRot(savedPitch + 0.000001f);
        minecraft.player.yHeadRot = savedYaw;
        minecraft.player.yBodyRot = savedYaw;
    }

    @Inject(method = "sendChat", at = @At("HEAD"), cancellable = true)
    private void onSendChatMessage(String message, CallbackInfo ci, @Local(argsOnly = true, name = "content") LocalRef<String> messageRef) {
        if (!message.startsWith(Config.get().prefix.get()) && !(BaritoneUtils.IS_AVAILABLE && message.startsWith(BaritoneUtils.getPrefix()))) {
            SendMessageEvent event = Client33.EVENT_BUS.post(SendMessageEvent.get(message));

            if (!event.isCancelled()) {
                messageRef.set(event.message);
            } else {
                ci.cancel();
            }

            return;
        }

        if (message.startsWith(Config.get().prefix.get())) {
            try {
                Commands.dispatch(message.substring(Config.get().prefix.get().length()));
            } catch (CommandSyntaxException e) {
                ChatUtils.error(e.getMessage());
            }

            minecraft.gui.hud.getChat().addRecentChat(message);
            ci.cancel();
        }
    }
}
