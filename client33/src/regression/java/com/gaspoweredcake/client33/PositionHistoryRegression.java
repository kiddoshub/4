package com.gaspoweredcake.client33;

import com.gaspoweredcake.client33.utils.intel.PositionHistory;
import java.util.UUID;

public final class PositionHistoryRegression {
    public static void run() {
        var h = new PositionHistory(2, 2);
        UUID a = UUID.fromString("00000000-0000-0000-0000-000000000001"), b = UUID.fromString("00000000-0000-0000-0000-000000000002");
        h.add(new PositionHistory.Position(1, a, "player", 0, 2, 0, "move", 1));
        h.add(new PositionHistory.Position(1, a, "player", 1, 2, 0, "move", 2));
        h.add(new PositionHistory.Position(1, a, "player", 2, 2, 0, "move", 3));
        check(h.get(1).size() == 2 && h.get(1).getFirst().x() == 1, "Track is bounded");
        check(h.nearby(2, 2, 0, 1).size() == 1 && h.nearby(0, 0, 0, 1).isEmpty(), "Position filtering uses the latest raw sample");
        h.add(new PositionHistory.Position(1, b, "player", 4, 2, 0, "move", 4));
        h.remove(1, a);
        check(h.get(1).size() == 1 && h.get(1).getFirst().uuid().equals(b), "Reused entity IDs do not inherit or delete another track");
        h.add(new PositionHistory.Position(2, a, "player", Double.NaN, 0, 0, "move", 5));
        check(h.get(2).isEmpty(), "Invalid coordinates are ignored");
        h.clear(); check(h.get(1).isEmpty(), "World reset clears tracks");
        System.out.println("Raw position regressions passed.");
    }
    private static void check(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
}
