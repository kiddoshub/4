package com.gaspoweredcake.client33.utils.intel;

import com.gaspoweredcake.client33.Client33;
import com.gaspoweredcake.client33.events.game.ResourcePacksReloadedEvent;
import com.gaspoweredcake.client33.utils.PreInit;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import meteordevelopment.orbit.EventHandler;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.locale.Language;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.BlockEntityTypes;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import static com.gaspoweredcake.client33.Client33.mc;

/** Neutralizes installed-mod translation keys in sign NBT before decoded packet handlers run. */
public final class TranslationGuard {
    private static final TranslationGuard INSTANCE = new TranslationGuard();
    private static volatile Set<String> protectedKeys = Set.of();
    private static final AtomicLong neutralized = new AtomicLong();
    private TranslationGuard() {}

    @PreInit public static void init() {
        Client33.EVENT_BUS.subscribe(INSTANCE);
        refresh();
    }
    @EventHandler private void onReload(ResourcePacksReloadedEvent event) { refresh(); }

    private static void refresh() {
        String selected = mc.getLanguageManager() == null ? "en_us" : mc.getLanguageManager().getSelected();
        if (selected == null || !selected.matches("[a-z0-9_]{1,32}")) selected = "en_us";
        Set<String> keys = new HashSet<>();
        for (var mod : FabricLoader.getInstance().getAllMods()) {
            if (mod.getMetadata().getId().equals("minecraft") || mod.getMetadata().getId().equals("java")) continue;
            for (Path root : mod.getRootPaths()) {
                Path assets = root.resolve("assets");
                if (!Files.isDirectory(assets)) continue;
                try (var namespaces = Files.list(assets)) {
                    for (Path namespace : namespaces.toList()) {
                        readKeys(namespace.resolve("lang/en_us.json"), keys);
                        if (!selected.equals("en_us")) readKeys(namespace.resolve("lang").resolve(selected + ".json"), keys);
                    }
                } catch (Exception failure) { Client33.LOG.warn("Unable to inspect a mod language resource", failure); }
            }
        }
        keys.removeIf(Language.DEFAULT_INSTANCE::has);
        protectedKeys = Set.copyOf(keys);
        Client33.LOG.info("Translation guard loaded {} mod-only language keys", keys.size());
    }
    private static void readKeys(Path file, Set<String> keys) {
        if (!Files.isRegularFile(file)) return;
        try (InputStream input = Files.newInputStream(file)) { Language.loadFromJson(input, (key, value) -> keys.add(key)); }
        catch (Exception failure) { Client33.LOG.warn("Unable to read a mod language file", failure); }
    }

    public static long neutralizedCount() { return neutralized.get(); }
    public static int protectedKeyCount() { return protectedKeys.size(); }

    /** Runs on the network thread before PacketEvent.Receive and vanilla packet processing. */
    public static void filter(Packet<?> packet) {
        Set<String> keys = protectedKeys;
        if (keys.isEmpty()) return;
        if (packet instanceof ClientboundBlockEntityDataPacket data) filterSign(data.getType(), data.getTag(), keys);
        else if (packet instanceof ClientboundLevelChunkWithLightPacket chunk)
            chunk.chunkData().forEachBlockEntityTag(chunk.x(), chunk.z(), (pos, type, tag) -> filterSign(type, tag, keys));
    }
    private static void filterSign(BlockEntityType<?> type, CompoundTag tag, Set<String> keys) {
        if (tag == null || (type != BlockEntityTypes.SIGN && type != BlockEntityTypes.HANGING_SIGN)) return;
        sanitizeSign(tag, keys);
    }

    /** Sanitizes a detached or incoming sign payload using the supplied installed-mod key set. */
    public static void sanitizeSign(CompoundTag tag, Set<String> keys) {
        if (tag == null || keys.isEmpty()) return;
        try {
            scrubNbt(tag, keys, new int[]{0}, 0);
        } catch (RuntimeException failure) {
            // Complex sign payloads cannot be inspected reliably: blank their text fields only.
            tag.remove("front_text"); tag.remove("back_text");
            for (int i = 1; i <= 4; i++) { tag.remove("Text" + i); tag.remove("FilteredText" + i); }
            neutralized.incrementAndGet();
        }
    }
    private static void scrubNbt(Tag tag, Set<String> keys, int[] nodes, int depth) {
        if (++nodes[0] > 2048 || depth > 48) throw new IllegalArgumentException("Sign text too complex");
        if (tag instanceof CompoundTag compound) {
            String translate = compound.getString("translate").orElse(null);
            if (translate != null && keys.contains(translate)) {
                compound.remove("translate"); compound.remove("fallback"); compound.remove("with");
                compound.putString("text", translate); neutralized.incrementAndGet();
            }
            for (String key : new ArrayList<>(compound.keySet())) {
                Tag child = compound.get(key);
                if (child instanceof StringTag text) {
                    String result = scrubJson(text.value(), keys, nodes, depth + 1);
                    if (!result.equals(text.value())) compound.putString(key, result);
                } else if (child != null) scrubNbt(child, keys, nodes, depth + 1);
            }
        } else if (tag instanceof ListTag list) {
            for (int i = 0; i < list.size(); i++) {
                Tag child = list.get(i);
                if (child instanceof StringTag text) {
                    String result = scrubJson(text.value(), keys, nodes, depth + 1);
                    if (!result.equals(text.value())) list.set(i, StringTag.valueOf(result));
                } else scrubNbt(child, keys, nodes, depth + 1);
            }
        }
    }
    private static String scrubJson(String text, Set<String> keys, int[] nodes, int depth) {
        String trimmed = text.stripLeading();
        if (trimmed.isEmpty() || (trimmed.charAt(0) != '{' && trimmed.charAt(0) != '[')) return text;
        if (text.length() > 65536) throw new IllegalArgumentException("Sign component too large");
        int nesting = 0; boolean quoted = false, escaped = false;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (quoted) {
                if (escaped) escaped = false;
                else if (c == '\\') escaped = true;
                else if (c == '"') quoted = false;
            } else if (c == '"') quoted = true;
            else if (c == '{' || c == '[') { if (++nesting > 48) throw new IllegalArgumentException("Sign component too deep"); }
            else if (c == '}' || c == ']') nesting--;
        }
        JsonElement json;
        try { json = JsonParser.parseString(text); } catch (com.google.gson.JsonParseException invalid) { return text; }
        return scrubJsonNode(json, keys, nodes, depth) ? json.toString() : text;
    }
    private static boolean scrubJsonNode(JsonElement value, Set<String> keys, int[] nodes, int depth) {
        if (++nodes[0] > 2048 || depth > 48) throw new IllegalArgumentException("Sign component too complex");
        boolean changed = false;
        if (value instanceof JsonObject object) {
            JsonElement translate = object.get("translate");
            if (translate != null && translate.isJsonPrimitive() && translate.getAsJsonPrimitive().isString() && keys.contains(translate.getAsString())) {
                String key = translate.getAsString(); object.remove("translate"); object.remove("fallback"); object.remove("with"); object.addProperty("text", key);
                neutralized.incrementAndGet(); changed = true;
            }
            for (var entry : new ArrayList<>(object.entrySet())) changed |= scrubJsonNode(entry.getValue(), keys, nodes, depth + 1);
        } else if (value instanceof JsonArray array) for (JsonElement child : array) changed |= scrubJsonNode(child, keys, nodes, depth + 1);
        return changed;
    }
}
