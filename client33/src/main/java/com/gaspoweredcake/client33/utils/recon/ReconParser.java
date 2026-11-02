package com.gaspoweredcake.client33.utils.recon;

import java.util.*;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/** Strict, bounded text observations. Coordinates are chunk coordinates; no membership/location inference. */
public final class ReconParser {
    public enum Source { Chat, Tab, Scoreboard, Map }
    public record Cell(int x, int z) {}
    public record Claim(Cell cell, String owner, Source source, long seen) {}
    public record Power(String faction, double current, double maximum, Source source, long seen) {}
    public static final String CLAIM = "(?i)^Claim\\s*\\[(-?\\d+),\\s*(-?\\d+)\\]\\s*[:=]\\s*([\\w -]{1,48})$";
    public static final String POWER = "(?i)^([\\w -]{1,48})\\s+Power\\s*[:=]\\s*(-?\\d+(?:\\.\\d+)?)/([0-9]+(?:\\.\\d+)?)$";
    private static final Pattern FACTION = Pattern.compile("(?i)^Faction\\s*[:=]\\s*([\\w -]{1,48})$");
    private static final Pattern POWER_LINE = Pattern.compile("(?i)^Power\\s*[:=]\\s*(-?\\d+(?:\\.\\d+)?)/([0-9]+(?:\\.\\d+)?)$");
    public static final String MAP_HEADER = "(?i)^Faction Map\\s*\\[(-?\\d+),\\s*(-?\\d+)\\]$";
    public static final String MAP_ROW = "^\\|([A-Za-z0-9+ .-]{1,63})\\|$";
    public static final String MAP_LEGEND = "(?:Legend: |,)([A-Za-z0-9])=([\\w -]{1,48})(?=,|$)";
    private Pattern mapHeader = Pattern.compile(MAP_HEADER), mapRow = Pattern.compile(MAP_ROW), mapLegend = Pattern.compile(MAP_LEGEND);
    private Pattern claim = Pattern.compile(CLAIM), power = Pattern.compile(POWER);
    private final LinkedHashMap<Cell, Claim> claims = new LinkedHashMap<>();
    private final LinkedHashMap<String, EnumMap<Source, Power>> powers = new LinkedHashMap<>();
    private final EnumMap<Source, String> context = new EnumMap<>(Source.class);
    private final EnumMap<Source, Long> contextTime = new EnumMap<>(Source.class);
    private final List<String> mapRows = new ArrayList<>();
    private Cell mapCenter;
    private long mapTime;

    public boolean configure(String claimRegex, String powerRegex) {
        try {
            if (claimRegex.length() > 512 || powerRegex.length() > 512) return false;
            Pattern c = Pattern.compile(claimRegex), p = Pattern.compile(powerRegex);
            if (c.matcher("").groupCount() != 3 || p.matcher("").groupCount() != 3) return false;
            claim = c; power = p;
            return true;
        } catch (PatternSyntaxException ex) { return false; }
    }

    public boolean configureMap(String header, String row, String legend) {
        try {
            if (header.length() > 512 || row.length() > 512 || legend.length() > 512) return false;
            Pattern h = Pattern.compile(header), r = Pattern.compile(row), l = Pattern.compile(legend);
            if (h.matcher("").groupCount() != 2 || r.matcher("").groupCount() != 1 || l.matcher("").groupCount() != 2) return false;
            mapHeader = h; mapRow = r; mapLegend = l;
            mapCenter = null; mapRows.clear();
            return true;
        } catch (PatternSyntaxException ex) { return false; }
    }

    public void accept(String input, Source source, long now) {
        if (input == null || input.length() > 1024) return;
        String line = input.replaceAll("§[0-9A-FK-ORa-fk-or]", "").strip();
        var c = claim.matcher(line);
        if (c.matches()) {
            try {
                Cell cell = cell(c.group(1), c.group(2));
                if (cell != null) putClaim(cell, c.group(3), source, now);
            } catch (RuntimeException ignored) { /* Malformed configured captures are not observations. */ }
        }
        var p = power.matcher(line);
        if (p.matches()) putPower(p.group(1), p.group(2), p.group(3), source, now);
        var f = FACTION.matcher(line);
        if (f.matches()) { context.put(source, f.group(1)); contextTime.put(source, now); }
        var value = POWER_LINE.matcher(line);
        if (value.matches() && context.containsKey(source) && now - contextTime.get(source) <= 5000)
            putPower(context.get(source), value.group(1), value.group(2), source, now);
        if (source == Source.Chat) acceptMap(line, now);
    }

    private void acceptMap(String line, long now) {
        if (mapCenter != null && now - mapTime > 5000) { mapCenter = null; mapRows.clear(); }
        var header = mapHeader.matcher(line);
        if (header.matches()) {
            mapCenter = cell(header.group(1), header.group(2)); mapTime = now; mapRows.clear(); return;
        }
        if (mapCenter == null) return;
        var rowMatcher = mapRow.matcher(line);
        if (rowMatcher.matches()) {
            if (rowMatcher.group(1) == null || rowMatcher.group(1).isEmpty() || rowMatcher.group(1).length() > 63) { mapCenter = null; mapRows.clear(); return; }
            if (mapRows.size() < 63) mapRows.add(rowMatcher.group(1));
            else { mapCenter = null; mapRows.clear(); }
            return;
        }
        Map<Character, String> legend = new HashMap<>();
        var entries = mapLegend.matcher(line);
        while (entries.find()) {
            String symbol = entries.group(1), owner = entries.group(2);
            if (symbol != null && symbol.length() == 1 && owner != null && !owner.isBlank() && owner.length() <= 48)
                legend.put(symbol.charAt(0), owner.strip());
        }
        if (legend.isEmpty()) return;
        int centerX = -1, centerZ = -1, centers = 0;
        for (int z = 0; z < mapRows.size(); z++) for (int x = 0; x < mapRows.get(z).length(); x++) {
            if (mapRows.get(z).charAt(x) == '+') { centerX = x; centerZ = z; centers++; }
        }
        boolean rectangular = !mapRows.isEmpty() && mapRows.stream().allMatch(row -> row.length() == mapRows.getFirst().length());
        if (centers == 1 && rectangular) for (int z = 0; z < mapRows.size(); z++) for (int x = 0; x < mapRows.get(z).length(); x++) {
            String owner = legend.get(mapRows.get(z).charAt(x));
            // '+' locates the origin but does not identify its owner.
            if (owner != null) putClaim(new Cell(mapCenter.x + x - centerX, mapCenter.z + z - centerZ), owner, Source.Map, now);
        }
        mapCenter = null; mapRows.clear();
    }

    private static Cell cell(String x, String z) {
        try {
            int cx = Integer.parseInt(x), cz = Integer.parseInt(z);
            return Math.abs((long) cx) <= 1_875_000 && Math.abs((long) cz) <= 1_875_000 ? new Cell(cx, cz) : null;
        } catch (NumberFormatException ex) { return null; }
    }
    private void putClaim(Cell cell, String owner, Source source, long now) {
        if (Math.abs((long) cell.x()) > 1_875_000 || Math.abs((long) cell.z()) > 1_875_000
            || owner == null || owner.isBlank() || owner.length() > 48) return;
        claims.remove(cell); claims.put(cell, new Claim(cell, owner.strip(), source, now));
        while (claims.size() > 2048) claims.remove(claims.keySet().iterator().next());
    }
    private void putPower(String faction, String current, String maximum, Source source, long now) {
        try {
            if (faction == null || faction.isBlank() || faction.length() > 48) return;
            double c = Double.parseDouble(current), m = Double.parseDouble(maximum);
            if (!Double.isFinite(c) || !Double.isFinite(m) || m < 0 || m > 1e9 || Math.abs(c) > 1e9) return;
            String name = faction.strip();
            powers.computeIfAbsent(name.toLowerCase(Locale.ROOT), ignored -> new EnumMap<>(Source.class))
                .put(source, new Power(name, c, m, source, now));
            while (powers.size() > 256) powers.remove(powers.keySet().iterator().next());
        } catch (RuntimeException ignored) { }
    }
    public void expire(long now, long ttl) {
        claims.values().removeIf(c -> now - c.seen > ttl);
        powers.values().forEach(values -> values.values().removeIf(p -> now - p.seen > ttl));
        powers.values().removeIf(Map::isEmpty);
    }
    public Collection<Claim> claims() { return Collections.unmodifiableCollection(claims.values()); }
    public Map<String, EnumMap<Source, Power>> powers() { return Collections.unmodifiableMap(powers); }
    public static Power newest(Collection<Power> values) {
        return values.stream().max(Comparator.comparingLong(Power::seen)).orElse(null);
    }
    public static List<String> changedLines(List<String> old, List<String> current) {
        if (current.equals(old)) return List.of();
        boolean newContext = current.stream().anyMatch(line -> line.matches("(?i)^Faction\\s*[:=].*") && (old == null || !old.contains(line)));
        return current.stream().filter(line -> line.matches("(?i)^Faction\\s*[:=].*") || old == null || !old.contains(line)
            || (newContext && line.matches("(?i)^Power\\s*[:=].*"))).toList();
    }
    public void clear() { claims.clear(); powers.clear(); context.clear(); contextTime.clear(); mapRows.clear(); mapCenter = null; }
}
