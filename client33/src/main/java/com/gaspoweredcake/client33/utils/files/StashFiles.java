package com.gaspoweredcake.client33.utils.files;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/** Loads independent stash copies without retaining partial or previously loaded records. */
public final class StashFiles {
    private StashFiles() {}

    @FunctionalInterface public interface Reader<T> { List<T> read(BufferedReader in) throws IOException; }
    public record Result<T>(List<T> entries, boolean protectJson, boolean protectCsv) {}
    private record Attempt<T>(List<T> entries, boolean failed) {}

    public static <T> Result<T> load(Path json, Path csv, Reader<T> jsonReader, Reader<T> csvReader, Predicate<T> valid) {
        Attempt<T> primary = read(json, jsonReader, valid);
        Attempt<T> fallback = read(csv, csvReader, valid);
        List<T> entries = primary.entries != null ? primary.entries : fallback.entries;
        return new Result<>(entries == null ? new ArrayList<>() : new ArrayList<>(entries), primary.failed, fallback.failed);
    }

    private static <T> Attempt<T> read(Path path, Reader<T> parser, Predicate<T> valid) {
        if (Files.notExists(path)) return new Attempt<>(null, false);
        try (BufferedReader in = Files.newBufferedReader(path)) {
            List<T> entries = parser.read(in);
            if (entries == null || !entries.stream().allMatch(valid)) throw new IOException("Invalid stash records");
            return new Attempt<>(entries, false);
        } catch (IOException | RuntimeException failure) {
            return new Attempt<>(null, true);
        }
    }
}
