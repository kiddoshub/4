package com.gaspoweredcake.client33;

import com.gaspoweredcake.client33.utils.files.AtomicFile;
import com.gaspoweredcake.client33.utils.files.ProfilePath;
import com.gaspoweredcake.client33.utils.files.StashFiles;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;

/** Real filesystem failure scenarios: the previously saved configuration must survive. */
public final class PersistenceRegression {
    public static void run() throws IOException {
        Path root = Files.createTempDirectory("client33-persistence-");
        try {
            Path target = root.resolve("nested/settings.nbt");
            AtomicFile.write(target, stage -> Files.writeString(stage, "first"));
            check(Files.readString(target).equals("first"), "First save creates missing parent directories");
            AtomicFile.write(target, stage -> {
                check(stage.getParent().equals(target.getParent()), "Staging must share the destination filesystem");
                Files.writeString(stage, "replacement");
            });
            check(Files.readString(target).equals("replacement"), "Complete save replaces old bytes");
            IOException expected = new IOException("Synthetic writer failure");
            try {
                AtomicFile.write(target, stage -> {
                    Files.writeString(stage, "partial");
                    throw expected;
                });
                throw new AssertionError("Failed writer must not report success");
            } catch (IOException failure) {
                check(failure == expected, "Original writer failure must propagate");
            }
            check(Files.readString(target).equals("replacement"), "Partial writer cannot overwrite the previous save");
            Path profileRoot = Files.createDirectory(root.resolve("profiles"));
            check(ProfilePath.resolve(profileRoot, "My Profile-1").equals(profileRoot.resolve("My Profile-1")), "Normal profile name resolves inside root");
            for (String name : new String[] {"", "..", "../other", "absolute/path"}) {
                try { ProfilePath.resolve(profileRoot, name); throw new AssertionError("Unsafe profile name accepted: " + name); }
                catch (IOException expectedInvalid) { }
            }
            Path outside = Files.createDirectory(root.resolve("outside"));
            Files.createSymbolicLink(profileRoot.resolve("linked"), outside);
            try { ProfilePath.resolve(profileRoot, "linked"); throw new AssertionError("Symlink profile accepted"); }
            catch (IOException expectedInvalid) { }
            Path json = root.resolve("stash.json"), csv = root.resolve("stash.csv");
            Files.writeString(json, "broken");
            Files.writeString(csv, "4\n5\n");
            StashFiles.Reader<Integer> parser = in -> {
                var result = new java.util.ArrayList<Integer>();
                for (String line; (line = in.readLine()) != null;) result.add(Integer.parseInt(line));
                return result;
            };
            var loaded = StashFiles.load(json, csv, parser, parser, n -> n != null && n >= 0);
            check(loaded.entries().equals(java.util.List.of(4, 5)) && loaded.protectJson() && !loaded.protectCsv(), "Bad JSON falls back to complete CSV and remains protected");
            Files.writeString(csv, "4\nbroken\n");
            loaded = StashFiles.load(json, csv, parser, parser, n -> n != null && n >= 0);
            check(loaded.entries().isEmpty() && loaded.protectJson() && loaded.protectCsv(), "Two invalid files cannot leak partial records");
            check(Files.readString(json).equals("broken") && Files.readString(csv).equals("4\nbroken\n"), "Failed reads preserve original files");
            Files.writeString(json, "7\n");
            loaded = StashFiles.load(json, csv, parser, parser, n -> n != null && n >= 0);
            check(loaded.entries().equals(java.util.List.of(7)) && !loaded.protectJson() && loaded.protectCsv(), "Valid primary wins while invalid fallback stays protected");
            Path directory = Files.createDirectory(root.resolve("occupied"));
            Files.writeString(directory.resolve("keep"), "preserved");
            try {
                AtomicFile.write(directory, stage -> Files.writeString(stage, "cannot replace a populated directory"));
                throw new AssertionError("Failed rename must not report success");
            } catch (IOException expectedMoveFailure) {
                check(Files.readString(directory.resolve("keep")).equals("preserved"), "Failed replacement must preserve destination");
            }
            try (var paths = Files.walk(root)) {
                check(paths.noneMatch(path -> path.getFileName().toString().endsWith(".tmp")), "Failed and successful saves clean their staging files");
            }
            System.out.println("Persistence failure regressions passed.");
        } finally {
            try (var paths = Files.walk(root)) {
                for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) Files.delete(path);
            }
        }
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
