package com.gaspoweredcake.client33.utils.recon;

import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;

/** Bounded network-to-client queue plus change detection; polling never refreshes stale evidence. */
public final class TextObservations {
    public record Input(String text, ReconParser.Source source) {}
    private final ArrayBlockingQueue<Input> pending = new ArrayBlockingQueue<>(128);
    private final Map<ReconParser.Source, List<String>> previous = new EnumMap<>(ReconParser.Source.class);
    public void offer(String text, ReconParser.Source source) {
        if (text == null || text.length() > 8192) return;
        pending.offer(new Input(text, source));
    }
    public void drain(ReconParser parser, long now) {
        for (int i = 0; i < 128; i++) {
            Input input = pending.poll();
            if (input == null) break;
            for (String line : input.text.split("\\R")) parser.accept(line, input.source, now);
        }
        var snapshot = ServerTextSnapshot.read();
        for (var source : new ReconParser.Source[]{ReconParser.Source.Tab, ReconParser.Source.Scoreboard}) {
            List<String> current = snapshot.stream().filter(line -> line.source() == source).map(ServerTextSnapshot.Line::text).toList();
            List<String> old = previous.put(source, current);
            ReconParser.changedLines(old, current).forEach(line -> parser.accept(line, source, now));
        }
    }
    public void clear() { pending.clear(); previous.clear(); }
}
