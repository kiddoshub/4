
package com.gaspoweredcake.client33.systems.modules.misc;

import com.gaspoweredcake.client33.events.packets.PacketEvent;
import com.gaspoweredcake.client33.events.world.TickEvent;
import com.gaspoweredcake.client33.settings.*;
import com.gaspoweredcake.client33.systems.modules.Categories;
import com.gaspoweredcake.client33.systems.modules.Module;
import com.gaspoweredcake.client33.utils.Utils;
import com.gaspoweredcake.client33.utils.misc.text.RunnableClickEvent;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.ChatFormatting;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.protocol.common.ClientboundResourcePackPushPacket;
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.ServerboundResourcePackPacket;
import net.minecraft.network.protocol.common.custom.BrandPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import org.apache.commons.lang3.Strings;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public class ServerSpoof extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Boolean> spoofBrand = sgGeneral.add(new BoolSetting.Builder()
        .name("spoof-brand")
        .description("Whether or not to spoof the brand.")
        .defaultValue(true)
        .build()
    );

    private final Setting<String> brand = sgGeneral.add(new StringSetting.Builder()
        .name("brand")
        .description("Specify the brand that will be send to the server.")
        .defaultValue("vanilla")
        .visible(spoofBrand::get)
        .build()
    );

    private final Setting<Boolean> resourcePack = sgGeneral.add(new BoolSetting.Builder()
        .name("resource-pack")
        .description("Spoof accepting server resource pack.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> blockChannels = sgGeneral.add(new BoolSetting.Builder()
        .name("block-channels")
        .description("Whether or not to block some channels.")
        .defaultValue(true)
        .build()
    );

    private final Setting<List<String>> channels = sgGeneral.add(new StringListSetting.Builder()
        .name("channels")
        .description("If the channel contains the keyword, this outgoing channel will be blocked.")
        .defaultValue("fabric", "minecraft:register")
        .visible(blockChannels::get)
        .build()
    );

    private final AtomicReference<MutableComponent> msg = new AtomicReference<>();
    public volatile boolean silentAcceptResourcePack = false;

    public ServerSpoof() {
        super(Categories.Misc, "server-spoof", "Spoof client brand, resource pack and channels.");

        runInMainMenu = true;
    }

    @Override public void onDeactivate() { msg.set(null); silentAcceptResourcePack = false; }

    @EventHandler
    private void onPacketSend(PacketEvent.Send event) {
        if (!isActive()) return;

        if (event.packet instanceof ServerboundCustomPayloadPacket(CustomPacketPayload payload)) {
            Identifier id = payload.type().id();

            if (blockChannels.get()) {
                for (String channel : channels.get()) {
                    if (Strings.CI.contains(id.toString(), channel)) {
                        event.cancel();
                        return;
                    }
                }
            }

            if (spoofBrand.get() && id.equals(BrandPayload.TYPE.id())) {
                ServerboundCustomPayloadPacket spoofedPacket = new ServerboundCustomPayloadPacket(new BrandPayload(brand.get()));

                event.sendSilently(spoofedPacket);
                event.cancel();
            }
        }

        // we want to accept the pack silently to prevent the server detecting you bypassed it when logging in
        if (silentAcceptResourcePack && event.packet instanceof ServerboundResourcePackPacket) event.cancel();
    }

    @EventHandler
    private void onPacketReceive(PacketEvent.Receive event) {
        if (!isActive() || !resourcePack.get()) return;
        if (!(event.packet instanceof ClientboundResourcePackPushPacket packet)) return;

        event.cancel();
        event.connection.send(new ServerboundResourcePackPacket(packet.id(), ServerboundResourcePackPacket.Action.ACCEPTED));
        event.connection.send(new ServerboundResourcePackPacket(packet.id(), ServerboundResourcePackPacket.Action.DOWNLOADED));
        event.connection.send(new ServerboundResourcePackPacket(packet.id(), ServerboundResourcePackPacket.Action.SUCCESSFULLY_LOADED));

        MutableComponent message = Component.literal("This server has ");
        message.append(packet.required() ? "a required " : "an optional ").append("resource pack. ");

        URL parsedUrl = getParsedResourcePackUrl(packet.url());
        if (parsedUrl == null) {
            message.append("The resource pack URL is invalid.");
            msg.set(message);
            return;
        }

        MutableComponent link = Component.literal("[Open URL]");
        link.setStyle(link.getStyle()
            .withColor(ChatFormatting.BLUE)
            .withUnderlined(true)
            .withClickEvent(new ClickEvent.OpenUrl(URI.create(parsedUrl.toExternalForm())))
            .withHoverEvent(new HoverEvent.ShowText(Component.literal("Click to open the pack url")))
        );

        MutableComponent acceptance = Component.literal("[Accept Pack]");
        Connection sourceConnection = event.connection;
        acceptance.setStyle(acceptance.getStyle()
            .withColor(ChatFormatting.DARK_GREEN)
            .withUnderlined(true)
            .withClickEvent(new RunnableClickEvent(() -> {
                if (isActive() && mc.getConnection() != null && mc.getConnection().getConnection() == sourceConnection) {
                    silentAcceptResourcePack = true;
                    mc.getDownloadedPackSource().pushPack(packet.id(), parsedUrl, packet.hash());
                }
            }))
            .withHoverEvent(new HoverEvent.ShowText(Component.literal("Click to accept and apply the pack.")))
        );

        message.append(link).append(" ");
        message.append(acceptance).append(".");
        msg.set(message);
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (!isActive() || !Utils.canUpdate()) return;
        MutableComponent message = msg.getAndSet(null);
        if (message != null) info(message);
    }

    private static URL getParsedResourcePackUrl(String url) {
        try {
            URI uri = new URI(url);
            if (uri.getHost() == null || !("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme()))) return null;
            return uri.toURL();
        } catch (MalformedURLException | URISyntaxException | IllegalArgumentException _) {
            return null;
        }
    }
}
