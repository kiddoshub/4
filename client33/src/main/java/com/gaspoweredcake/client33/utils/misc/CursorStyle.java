
package com.gaspoweredcake.client33.utils.misc;

import com.mojang.blaze3d.platform.cursor.CursorType;
import com.mojang.blaze3d.platform.cursor.CursorTypes;

public enum CursorStyle {
    Default(CursorType.DEFAULT),
    Click(CursorTypes.POINTING_HAND),
    Type(CursorTypes.IBEAM);

    private final CursorType cursor;

    CursorStyle(CursorType cursor) {
        this.cursor = cursor;
    }

    public CursorType getCursor() {
        return cursor;
    }
}
