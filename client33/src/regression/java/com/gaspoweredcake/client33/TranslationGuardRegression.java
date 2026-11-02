package com.gaspoweredcake.client33;

import com.gaspoweredcake.client33.utils.intel.TranslationGuard;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import java.util.Set;

public final class TranslationGuardRegression {
    public static void run() {
        Set<String> protectedKeys = Set.of("key.client33.open-gui", "custom.mod.setting");
        CompoundTag sign = new CompoundTag(), front = new CompoundTag(), component = new CompoundTag();
        component.putString("translate", "key.client33.open-gui");
        component.putString("fallback", "probe"); component.putString("color", "red");
        ListTag lines = new ListTag(); lines.add(component);
        lines.add(StringTag.valueOf("{\"translate\":\"block.minecraft.stone\"}"));
        lines.add(StringTag.valueOf("{\"text\":\"safe\",\"extra\":[{\"translate\":\"custom.mod.setting\"}]}"));
        front.put("messages", lines); sign.put("front_text", front);
        TranslationGuard.sanitizeSign(sign, protectedKeys);
        var first = sign.getCompoundOrEmpty("front_text").getListOrEmpty("messages").getCompoundOrEmpty(0);
        check(first.getStringOr("text", "").equals("key.client33.open-gui") && !first.contains("translate") && !first.contains("fallback"), "NBT translate key becomes a literal");
        check(first.getStringOr("color", "").equals("red"), "Text styling is preserved");
        var after = sign.getCompoundOrEmpty("front_text").getListOrEmpty("messages");
        check(after.getStringOr(1, "").equals("{\"translate\":\"block.minecraft.stone\"}"), "Ordinary vanilla translations are preserved");
        check(after.getStringOr(2, "").contains("\\\"text\\\":\\\"custom.mod.setting\\\"") || after.getStringOr(2, "").contains("\"text\":\"custom.mod.setting\""), "Nested JSON translation is neutralized");
        CompoundTag huge = new CompoundTag(), hfront = new CompoundTag();
        ListTag hlines = new ListTag(); hlines.add(StringTag.valueOf("[".repeat(60) + "]".repeat(60)));
        hfront.put("messages", hlines); huge.put("front_text", hfront);
        TranslationGuard.sanitizeSign(huge, protectedKeys);
        check(!huge.contains("front_text"), "Excessively nested text is blanked safely");
        System.out.println("Translation guard regressions passed.");
    }
    private static void check(boolean value, String message) { if (!value) throw new AssertionError(message); }
}
