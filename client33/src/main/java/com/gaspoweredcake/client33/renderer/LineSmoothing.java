package com.gaspoweredcake.client33.renderer;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/** Carries the OpenGL extension across RenderPearl's asynchronous pipeline compilation. */
public final class LineSmoothing {
    private static final Set<String> PIPELINES = ConcurrentHashMap.newKeySet();

    private LineSmoothing() {}

    public static void register(String name, boolean enabled) {
        if (enabled) PIPELINES.add(name);
        else PIPELINES.remove(name);
    }

    public static boolean enabled(String name) {
        return PIPELINES.contains(name);
    }
}
