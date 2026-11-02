
package com.gaspoweredcake.client33.renderer;

import com.gaspoweredcake.client33.Client33;
import com.gaspoweredcake.client33.events.client33.CustomFontChangedEvent;
import com.gaspoweredcake.client33.gui.WidgetScreen;
import com.gaspoweredcake.client33.renderer.text.CustomTextRenderer;
import com.gaspoweredcake.client33.renderer.text.FontFace;
import com.gaspoweredcake.client33.renderer.text.FontFamily;
import com.gaspoweredcake.client33.renderer.text.FontInfo;
import com.gaspoweredcake.client33.systems.config.Config;
import com.gaspoweredcake.client33.utils.PreInit;
import com.gaspoweredcake.client33.utils.render.FontUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static com.gaspoweredcake.client33.Client33.mc;

public class Fonts {
    public static final String[] BUILTIN_FONTS = {"JetBrains Mono", "Comfortaa", "Tw Cen MT", "Pixelation"};

    public static String DEFAULT_FONT_FAMILY;
    public static FontFace DEFAULT_FONT;

    public static final List<FontFamily> FONT_FAMILIES = new ArrayList<>();
    public static CustomTextRenderer RENDERER;

    private Fonts() {
    }

    @PreInit
    public static void refresh() {
        // Build a fresh catalog first; an unreadable font directory must not erase the current list.
        List<FontFamily> discovered = new ArrayList<>();

        for (String builtinFont : BUILTIN_FONTS) {
            FontUtils.loadBuiltin(discovered, builtinFont);
        }

        for (String fontPath : FontUtils.getSearchPaths().stream().sorted(String.CASE_INSENSITIVE_ORDER.thenComparing(Comparator.naturalOrder())).toList()) {
            FontUtils.loadSystem(discovered, new File(fontPath));
        }

        discovered.sort(Comparator.comparing(FontFamily::getName, String.CASE_INSENSITIVE_ORDER).thenComparing(FontFamily::getName));

        FontInfo defaultInfo = FontUtils.getBuiltinFontInfo(BUILTIN_FONTS[1]);
        if (defaultInfo == null) throw new IllegalStateException("Default builtin font could not be read: " + BUILTIN_FONTS[1]);
        FontFamily defaultFamily = discovered.stream().filter(f -> f.getName().equalsIgnoreCase(defaultInfo.family())).findFirst().orElse(null);
        FontFace defaultFace = defaultFamily == null ? null : defaultFamily.get(FontInfo.Type.Regular);
        if (defaultFace == null) throw new IllegalStateException("Default regular font is unavailable: " + defaultInfo.family());

        List<FontFamily> previousFamilies = List.copyOf(FONT_FAMILIES);
        String previousDefaultFamily = DEFAULT_FONT_FAMILY;
        FontFace previousDefault = DEFAULT_FONT;
        CustomTextRenderer previousRenderer = RENDERER;
        FONT_FAMILIES.clear();
        FONT_FAMILIES.addAll(discovered);
        Client33.LOG.info("Found {} font families.", discovered.size());

        DEFAULT_FONT_FAMILY = defaultInfo.family();
        DEFAULT_FONT = defaultFace;

        Config config = Config.get();
        try {
            load(config != null ? config.font.get() : DEFAULT_FONT);
        } catch (RuntimeException failure) {
            if (RENDERER == previousRenderer) {
                FONT_FAMILIES.clear(); FONT_FAMILIES.addAll(previousFamilies);
                DEFAULT_FONT_FAMILY = previousDefaultFamily; DEFAULT_FONT = previousDefault;
            }
            throw failure;
        }
    }

    public static void load(FontFace fontFace) {
        if (fontFace == null) fontFace = DEFAULT_FONT;
        if (fontFace == null) throw new IllegalStateException("No default font is available");
        if (RENDERER != null && RENDERER.fontFace.equals(fontFace)) return;

        CustomTextRenderer next;
        try {
            next = new CustomTextRenderer(fontFace);
        } catch (Exception e) {
            if (fontFace.equals(DEFAULT_FONT) || DEFAULT_FONT == null) {
                throw new RuntimeException("Failed to load default font: " + fontFace, e);
            }

            Client33.LOG.error("Failed to load font: {}", fontFace, e);
            if (RENDERER != null && RENDERER.fontFace.equals(DEFAULT_FONT)) return;
            load(Fonts.DEFAULT_FONT);
            return;
        }

        CustomTextRenderer previous = RENDERER;
        RENDERER = next;
        if (previous != null) previous.destroy();
        Client33.EVENT_BUS.post(CustomFontChangedEvent.get());

        if (mc.gui.screen() instanceof WidgetScreen widgetScreen && Config.get() != null && Config.get().customFont.get()) {
            widgetScreen.invalidate();
        }
    }

    public static FontFamily getFamily(String name) {
        for (FontFamily fontFamily : Fonts.FONT_FAMILIES) {
            if (fontFamily.getName().equalsIgnoreCase(name)) {
                return fontFamily;
            }
        }

        return null;
    }
}
