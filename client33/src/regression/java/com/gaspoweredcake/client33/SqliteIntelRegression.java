package com.gaspoweredcake.client33;

import com.gaspoweredcake.client33.utils.intel.SqliteBlockEntityDatabase;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

public final class SqliteIntelRegression {
    public static void run() throws Exception {
        Path dir = Files.createTempDirectory("client33-intel-");
        Path file = dir.resolve("observations.sqlite3");
        try {
            try (var db = new SqliteBlockEntityDatabase(file)) {
                byte[] first = {1, 2, 3};
                db.put("server-a", "overworld", 10, -5, 20, "chest", 100, first);
                first[0] = 9;
                check(db.get("server-a", "overworld", 10, -5, 20).orElseThrow().nbt()[0] == 1, "Writes own the stored bytes");
                check(db.get("server-b", "overworld", 10, -5, 20).isEmpty(), "Servers are isolated");
                check(db.get("server-a", "nether", 10, -5, 20).isEmpty(), "Dimensions are isolated");
                db.put("server-a", "overworld", 10, -5, 20, "spawner", 200, new byte[]{4, 5});
            }
            try (var reopened = new SqliteBlockEntityDatabase(file)) {
                var entry = reopened.get("server-a", "overworld", 10, -5, 20).orElseThrow();
                check(entry.type().equals("spawner") && entry.observedMillis() == 200 && Arrays.equals(entry.nbt(), new byte[]{4,5}), "Latest observation persists after reopening");
                byte[] result = entry.nbt(); result[0] = 0;
                check(entry.nbt()[0] == 4, "Returned values are detached");
            }
            System.out.println("SQLite intel regressions passed.");
        } finally {
            Files.deleteIfExists(file); Files.deleteIfExists(dir);
        }
    }
    private static void check(boolean value, String message) { if (!value) throw new AssertionError(message); }
}
