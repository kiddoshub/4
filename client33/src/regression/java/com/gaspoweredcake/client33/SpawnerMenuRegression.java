package com.gaspoweredcake.client33;

import com.gaspoweredcake.client33.utils.inventory.SpawnerMenu;
import java.util.List;

public final class SpawnerMenuRegression {
    public static void run() {
        List<String> screenshot = List.of("Stacked: 3", "Stored Loot: 332 items", "Page: 1 / 1", "Produces when a player is nearby");
        check(SpawnerMenu.matches("Iron Golem Spawner", "Iron Golem Spawner", screenshot), "Matches the provided menu and item text");
        check(!SpawnerMenu.matches("Iron Golem Spawner", "Zombie Spawner", screenshot), "A different item name cannot identify the menu");
        check(!SpawnerMenu.matches("Iron Golem Spawner", "Iron Golem Spawner", List.of("Stacked: 3")), "Incomplete lore does not identify this menu");
        check(!SpawnerMenu.matches("Large Chest", "Iron Golem Spawner", screenshot), "An ordinary chest title is not a spawner menu");
        check(SpawnerMenu.matches("Any title", "Spawner Info", List.of()), "Earlier named info items remain supported");
        System.out.println("Spawner menu regressions passed.");
    }
    private static void check(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
}
