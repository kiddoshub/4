package com.gaspoweredcake.client33.utils.render;

import java.util.ArrayList;
import java.util.List;

/** Formats the independently selectable parts of a screen-space ESP label. */
public final class EspLabels {
    private EspLabels() {}

    public static String format(String name, Long distance, Float health) {
        List<String> parts = new ArrayList<>(3);
        if (name != null && !name.isBlank()) parts.add(name);
        if (distance != null) parts.add(distance + " m");
        if (health != null && Float.isFinite(health)) parts.add((Math.round(Math.max(0, health.doubleValue()) * 10) / 10.0) + " hp");
        return String.join(" / ", parts);
    }
}
