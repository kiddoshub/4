
package com.gaspoweredcake.client33.settings;

import com.gaspoweredcake.client33.renderer.Fonts;
import com.gaspoweredcake.client33.renderer.text.FontFace;
import com.gaspoweredcake.client33.renderer.text.FontFamily;
import com.gaspoweredcake.client33.renderer.text.FontInfo;
import com.gaspoweredcake.client33.renderer.text.FontCatalog;
import net.minecraft.nbt.CompoundTag;

import java.util.List;
import java.util.function.Consumer;

public class FontFaceSetting extends Setting<FontFace> {
    private static final List<String> SUGGESTIONS = List.of("JetBrainsMono-Regular", "Arial-Bold");

    public FontFaceSetting(String name, String description, FontFace defaultValue, Consumer<FontFace> onChanged, Consumer<Setting<FontFace>> onModuleActivated, IVisible visible) {
        super(name, description, defaultValue, onChanged, onModuleActivated, visible);
    }

    @Override
    protected FontFace parseImpl(String str) {
        String[] split = str.replace(" ", "").split("-");
        if (split.length != 2) return null;

        for (FontFamily family : Fonts.FONT_FAMILIES) {
            if (family.getName().replace(" ", "").equalsIgnoreCase(split[0])) {
                return family.get(FontInfo.Type.parse(split[1]));
            }
        }

        return null;
    }

    @Override
    public Iterable<String> getSuggestions() {
        return SUGGESTIONS;
    }

    @Override
    protected boolean isValueValid(FontFace value) {
        if (value == null) return false;

        return FontCatalog.resolve(Fonts.FONT_FAMILIES, value.info.family(), value.info.type()) != null;
    }

    @Override
    protected CompoundTag save(CompoundTag tag) {
        tag.putString("family", get().info.family());
        tag.putString("type", get().info.type().toString());
        return tag;
    }

    @Override
    protected FontFace load(CompoundTag tag) {
        String family = tag.getStringOr("family", "");
        FontInfo.Type type = FontInfo.Type.parse(tag.getStringOr("type", ""));
        if (type == null) {
            set(Fonts.DEFAULT_FONT);
            return get();
        }

        FontFace restored = FontCatalog.resolve(Fonts.FONT_FAMILIES, family, type);
        if (!set(restored)) set(Fonts.DEFAULT_FONT);

        return get();
    }

    public static class Builder extends SettingBuilder<Builder, FontFace, FontFaceSetting> {
        public Builder() {
            super(Fonts.DEFAULT_FONT);
        }

        @Override
        public FontFaceSetting build() {
            return new FontFaceSetting(name, description, defaultValue, onChanged, onModuleActivated, visible);
        }
    }
}
