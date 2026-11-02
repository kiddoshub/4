package com.gaspoweredcake.client33.renderer.text;

import java.util.List;

/** Resolve saved font identities without depending on UI or GPU state. */
public final class FontCatalog {
    private FontCatalog() {}

    public static FontFace resolve(List<FontFamily> families, String name, FontInfo.Type type) {
        if (name == null || type == null) return null;
        for (FontFamily family : families) if (family.getName().equalsIgnoreCase(name)) return family.get(type);
        return null;
    }
}
