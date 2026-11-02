package com.gaspoweredcake.client33.utils.intel;

import java.util.List;
import java.util.UUID;

/** Serialized tracked data values; entries are limited to what the protocol/client actually accepts. */
public record EntityMetadataIntel(int id, UUID uuid, String entityType, long capturedNanos, String source, List<Value> values, int skipped) {
    public EntityMetadataIntel { values = List.copyOf(values); }
    public record Value(int index, byte[] wireValue) {
        public Value { wireValue = wireValue.clone(); }
        @Override public byte[] wireValue() { return wireValue.clone(); }
    }
}
