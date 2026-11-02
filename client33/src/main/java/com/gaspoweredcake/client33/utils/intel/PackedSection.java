package com.gaspoweredcake.client33.utils.intel;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;

/** Immutable copy of Minecraft's non-crossing packed palette indices and their values. */
public final class PackedSection<T> {
    private final long[] words;
    private final int bits, size, perWord;
    private final long mask;
    private final Map<Integer, T> values;

    public PackedSection(long[] words, int bits, int size, Map<Integer, T> values) {
        if (bits < 0 || bits > 32 || size < 0) throw new IllegalArgumentException("Invalid packed dimensions");
        this.bits = bits;
        this.size = size;
        this.perWord = bits == 0 ? 0 : 64 / bits;
        this.mask = bits == 0 ? 0 : (1L << bits) - 1;
        int expected = bits == 0 ? 0 : (int) (((long) size + perWord - 1) / perWord);
        if (words.length != expected) throw new IllegalArgumentException("Unexpected packed word count");
        this.words = words.clone();
        this.values = Map.copyOf(new LinkedHashMap<>(values));
        if (size > 0 && bits == 0 && !this.values.containsKey(0)) throw new IllegalArgumentException("Missing single palette value");
    }

    public int size() { return size; }
    public int paletteSize() { return values.size(); }
    public boolean maybeHas(Predicate<? super T> test) { return values.values().stream().anyMatch(test); }

    public int paletteId(int index) {
        Objects.checkIndex(index, size);
        if (bits == 0) return 0;
        return (int) ((words[index / perWord] >>> ((index % perWord) * bits)) & mask);
    }

    public T get(int index) {
        int id = paletteId(index);
        T value = values.get(id);
        if (value == null) throw new IllegalStateException("Missing palette entry " + id);
        return value;
    }

    public T get(int x, int y, int z) {
        if (x < 0 || x >= 16 || y < 0 || y >= 16 || z < 0 || z >= 16) throw new IndexOutOfBoundsException();
        return get((y << 8) | (z << 4) | x);
    }
}
