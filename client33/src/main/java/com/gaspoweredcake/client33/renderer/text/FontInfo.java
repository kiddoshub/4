
package com.gaspoweredcake.client33.renderer.text;

import org.jspecify.annotations.NonNull;

public record FontInfo(String family, Type type) {
    @Override
    public @NonNull String toString() {
        return family + " " + type;
    }

    public enum Type {
        Regular,
        Bold,
        Italic,
        BoldItalic;

        public static Type fromString(String str) {
            Type type = parse(str);
            return type == null ? Regular : type;
        }

        /** Parse persisted display names as well as enum names; unknown names are rejected. */
        public static Type parse(String str) {
            if (str == null) return null;
            return switch (str) {
                case "Regular" -> Regular;
                case "Bold" -> Bold;
                case "Italic" -> Italic;
                case "Bold Italic", "BoldItalic" -> BoldItalic;
                default -> null;
            };
        }

        @Override
        public String toString() {
            return switch (this) {
                case Bold -> "Bold";
                case Italic -> "Italic";
                case BoldItalic -> "Bold Italic";
                default -> "Regular";
            };
        }
    }
}
