package com.gaspoweredcake.client33.utils.intel;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Optional;

/** One worker-owned connection; callers serialize access. */
public final class SqliteBlockEntityDatabase implements AutoCloseable {
    public record Value(String type, long observedMillis, byte[] nbt) {
        public Value { nbt = nbt.clone(); }
        @Override public byte[] nbt() { return nbt.clone(); }
    }
    private final Connection db;
    private final PreparedStatement write, read;

    public SqliteBlockEntityDatabase(Path file) throws IOException, SQLException, ClassNotFoundException {
        Path parent = file.toAbsolutePath().getParent();
        if (parent != null) Files.createDirectories(parent);
        Class.forName("org.sqlite.JDBC");
        db = DriverManager.getConnection("jdbc:sqlite:" + file.toAbsolutePath());
        try {
            try (var stmt = db.createStatement()) {
                stmt.execute("PRAGMA busy_timeout=5000");
                stmt.execute("CREATE TABLE IF NOT EXISTS block_entities (server TEXT NOT NULL, dimension TEXT NOT NULL, x INTEGER NOT NULL, y INTEGER NOT NULL, z INTEGER NOT NULL, type TEXT NOT NULL, observed_ms INTEGER NOT NULL, nbt BLOB NOT NULL, PRIMARY KEY(server, dimension, x, y, z))");
            }
            write = db.prepareStatement("INSERT INTO block_entities(server,dimension,x,y,z,type,observed_ms,nbt) VALUES(?,?,?,?,?,?,?,?) ON CONFLICT(server,dimension,x,y,z) DO UPDATE SET type=excluded.type,observed_ms=excluded.observed_ms,nbt=excluded.nbt");
            read = db.prepareStatement("SELECT type, observed_ms, nbt FROM block_entities WHERE server=? AND dimension=? AND x=? AND y=? AND z=?");
        } catch (SQLException failure) { db.close(); throw failure; }
    }
    public void put(String server, String dimension, int x, int y, int z, String type, long time, byte[] nbt) throws SQLException {
        bind(write, server, dimension, x, y, z);
        write.setString(6, type); write.setLong(7, time); write.setBytes(8, nbt); write.executeUpdate();
    }
    public Optional<Value> get(String server, String dimension, int x, int y, int z) throws SQLException {
        bind(read, server, dimension, x, y, z);
        try (var rows = read.executeQuery()) {
            return rows.next() ? Optional.of(new Value(rows.getString(1), rows.getLong(2), rows.getBytes(3))) : Optional.empty();
        }
    }
    private static void bind(PreparedStatement statement, String server, String dimension, int x, int y, int z) throws SQLException {
        statement.setString(1, server); statement.setString(2, dimension);
        statement.setInt(3, x); statement.setInt(4, y); statement.setInt(5, z);
    }
    @Override public void close() throws SQLException {
        try { write.close(); } finally { try { read.close(); } finally { db.close(); } }
    }
}
