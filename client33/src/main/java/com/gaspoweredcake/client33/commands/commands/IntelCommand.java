package com.gaspoweredcake.client33.commands.commands;

import com.gaspoweredcake.client33.commands.Command;
import com.gaspoweredcake.client33.utils.intel.IntelBus;
import com.gaspoweredcake.client33.utils.intel.TranslationGuard;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.client.multiplayer.ClientSuggestionProvider;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import java.util.HexFormat;

/** Local inspection of bounded data actually observed by the shared acquisition layer. */
public final class IntelCommand extends Command {
    public IntelCommand() { super("intel", "Inspect received chunk, entity, inventory and packet observations."); }

    @Override public void build(LiteralArgumentBuilder<ClientSuggestionProvider> builder) {
        builder.executes(context -> { status(); return SINGLE_SUCCESS; });
        builder.then(literal("status").executes(context -> { status(); return SINGLE_SUCCESS; }));
        builder.then(literal("sections").executes(context -> {
            var sections = IntelBus.sections(); int start = Math.max(0, sections.size() - 8);
            if (sections.isEmpty()) info("No section snapshots available yet.");
            for (int i = start; i < sections.size(); i++) {
                var section = sections.get(i);
                String light = section.lightEvidence.available() ? Integer.toString(section.lightEvidence.brightUndergroundSamples()) : "unknown";
                info("Chunk %d, %d / section %d: hash %s, underground light samples %s, emissive palette %s.",
                    section.chunkX, section.chunkZ, section.sectionY, Long.toUnsignedString(section.hash, 16), light, section.hasEmissiveBlock);
            }
            return SINGLE_SUCCESS;
        }));
        builder.then(literal("changes").executes(context -> {
            var changes = IntelBus.sectionChanges(); int start = Math.max(0, changes.size() - 8);
            if (changes.isEmpty()) info("No section hash changes observed this session.");
            for (int i = start; i < changes.size(); i++) {
                var change = changes.get(i); var key = change.key();
                info("Chunk %d, %d / section %d: %s -> %s.", key.chunkX(), key.chunkZ(), key.sectionY(),
                    Long.toUnsignedString(change.previousHash(), 16), Long.toUnsignedString(change.currentHash(), 16));
            }
            return SINGLE_SUCCESS;
        }));
        builder.then(literal("packets").executes(context -> {
            var packets = IntelBus.packetForensics().packets(); int start = Math.max(0, packets.size() - 8);
            if (packets.isEmpty()) info("No raw clientbound packet observations available.");
            for (int i = start; i < packets.size(); i++) {
                var packet = packets.get(i); var timing = IntelBus.packetForensics().timing(packet.connection(), packet.phase(), packet.id());
                info("Connection %d / %s / ID %d: %d bytes, prefix %s; %d intervals, mean %.2f ms.",
                    packet.connection(), packet.phase(), packet.id(), packet.length(), HexFormat.of().formatHex(packet.prefix()), timing.intervals(), timing.meanNanos() / 1_000_000);
            }
            return SINGLE_SUCCESS;
        }));
        builder.then(literal("entities").executes(context -> {
            if (mc.player == null) return SINGLE_SUCCESS;
            var entries = IntelBus.positionsNear(mc.player.getX(), mc.player.getY(), mc.player.getZ(), 128);
            if (entries.isEmpty()) info("No received entity positions in range.");
            for (int i = 0; i < Math.min(8, entries.size()); i++) {
                var position = entries.get(i); var meta = IntelBus.metadata(position.id());
                info("%s #%d at %.2f, %.2f, %.2f (%s); tracked metadata indices: %s.", position.type(), position.id(),
                    position.x(), position.y(), position.z(), position.source(), meta == null ? "unknown" : meta.values().stream().map(v -> Integer.toString(v.index())).toList());
            }
            return SINGLE_SUCCESS;
        }));
        builder.then(literal("slots").executes(context -> {
            var slots = IntelBus.slots(); int start = Math.max(0, slots.size() - 8);
            if (slots.isEmpty()) info("No server inventory slots observed.");
            for (int i = start; i < slots.size(); i++) {
                var slot = slots.get(i);
                info("Container %d slot %d: %s x%d, damage %d%s.", slot.container(), slot.slot(),
                    slot.itemId().isEmpty() ? "empty" : slot.itemId(), slot.count(), slot.damage(), slot.truncated() ? " (partial)" : "");
            }
            return SINGLE_SUCCESS;
        }));
        builder.then(literal("block").executes(context -> {
            if (!(mc.hitResult instanceof BlockHitResult hit) || hit.getType() != HitResult.Type.BLOCK || mc.level == null) {
                warning("Look at a block to query its received NBT cache."); return SINGLE_SUCCESS;
            }
            var pos = hit.getBlockPos().immutable(); var world = mc.level;
            IntelBus.blockEntity(pos).whenComplete((entry, failure) -> mc.execute(() -> {
                if (mc.level != world) return;
                if (failure != null) warning("Could not read cached block entity data.");
                else if (entry.isEmpty()) info("No cached NBT for %s.", pos.toShortString());
                else info("%s: %s, observed at %d; NBT keys: %s.", pos.toShortString(), entry.get().type(), entry.get().observedMillis(), entry.get().nbt().keySet());
            }));
            return SINGLE_SUCCESS;
        }));
    }

    private void status() {
        info("%d cached sections, %d slot observations, %d raw packets.", IntelBus.sections().size(), IntelBus.slots().size(), IntelBus.packetForensics().packets().size());
        info("Translation guard: %d installed mod keys; %d neutralizations.", TranslationGuard.protectedKeyCount(), TranslationGuard.neutralizedCount());
        info("Dropped: section %d, raw %d, slot %d, metadata %d, position %d; rejected NBT %d.", IntelBus.droppedSectionObservations(), IntelBus.droppedRawPackets(), IntelBus.droppedSlotObservations(), IntelBus.droppedMetadataObservations(), IntelBus.droppedPositionObservations(), IntelBus.rejectedBlockEntityObservations());
    }
}
