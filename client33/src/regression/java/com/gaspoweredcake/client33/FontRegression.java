package com.gaspoweredcake.client33;

import com.gaspoweredcake.client33.renderer.Fonts;
import com.gaspoweredcake.client33.renderer.text.BuiltinFontFace;
import com.gaspoweredcake.client33.renderer.text.FontFamily;
import com.gaspoweredcake.client33.renderer.text.FontInfo;
import com.gaspoweredcake.client33.renderer.text.FontCatalog;
import net.minecraft.nbt.CompoundTag;
import com.gaspoweredcake.client33.utils.render.FontUtils;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

final class FontRegression {
    static void run() throws Exception {
        var oldFamilies = List.copyOf(Fonts.FONT_FAMILIES);
        var oldDefault = Fonts.DEFAULT_FONT;
        var tmp = Files.createTempDirectory("client33-font-regression");
        try {
            var regular = new BuiltinFontFace(new FontInfo("Example", FontInfo.Type.Regular), "Comfortaa");
            var boldItalic = new BuiltinFontFace(new FontInfo("Example", FontInfo.Type.BoldItalic), "Comfortaa");
            var other = new BuiltinFontFace(new FontInfo("Other", FontInfo.Type.Regular), "Comfortaa");
            var family = new FontFamily("Example"); family.addFont(regular); family.addFont(boldItalic);
            Fonts.FONT_FAMILIES.clear(); Fonts.FONT_FAMILIES.add(family); Fonts.DEFAULT_FONT = regular;
            var saved = new CompoundTag(); saved.putString("family", boldItalic.info.family()); saved.putString("type", boldItalic.info.type().toString());
            check(FontCatalog.resolve(Fonts.FONT_FAMILIES, saved.getStringOr("family", ""), FontInfo.Type.parse(saved.getStringOr("type", ""))) == boldItalic,
                "Bold Italic display name round trips through NBT");
            check(FontCatalog.resolve(Fonts.FONT_FAMILIES, "example", FontInfo.Type.BoldItalic) == boldItalic, "Family search ignores case");
            check(FontCatalog.resolve(Fonts.FONT_FAMILIES, other.info.family(), other.info.type()) == null,
                "A style in a different family does not make a missing face valid");
            check(FontCatalog.resolve(Fonts.FONT_FAMILIES, "Example", FontInfo.Type.parse("Unknown")) == null, "Unknown styles are rejected");
            for (FontInfo.Type type : FontInfo.Type.values()) check(FontInfo.Type.parse(type.toString()) == type, "Every style display name round trips");
            check(FontInfo.Type.parse("invalid") == null && FontInfo.Type.fromString("unknown") == FontInfo.Type.Regular,
                "Persisted invalid styles are rejected while font metadata retains its regular fallback");

            var local = new ArrayList<FontFamily>();
            FontUtils.loadBuiltin(local, "Comfortaa");
            check(local.size() == 1 && local.getFirst().get(FontInfo.Type.Regular) != null, "Builtin discovery populates its supplied catalog");
            check(Fonts.FONT_FAMILIES.equals(List.of(family)), "Discovery never mutates the active catalog");
            try {
                Files.createSymbolicLink(tmp.resolve("loop"), tmp);
                FontUtils.loadSystem(local, tmp.toFile());
            } catch (UnsupportedOperationException | java.nio.file.FileSystemException ignored) { }
        } finally {
            Files.deleteIfExists(tmp.resolve("loop")); Files.deleteIfExists(tmp);
            Fonts.FONT_FAMILIES.clear(); Fonts.FONT_FAMILIES.addAll(oldFamilies); Fonts.DEFAULT_FONT = oldDefault;
        }
        System.out.println("Font discovery and persisted style regressions passed.");
    }
    private static void check(boolean value, String message) { if (!value) throw new AssertionError(message); }
}
