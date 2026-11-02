package com.gaspoweredcake.client33.utils.misc.input;

import java.util.BitSet;

/** Event-fed physical states. SDL keyboard scancodes and mouse button numbers are separate domains. */
public final class InputState {
    public enum Device { Keyboard, Mouse }
    private final BitSet keyboard=new BitSet(512),mouse=new BitSet(256);
    private static int limit(Device device) {return device==Device.Keyboard?512:256;}
    public void set(Device device,int code,boolean down) {
        if(code<0||code>=limit(device))return;
        (device==Device.Keyboard?keyboard:mouse).set(code,down);
    }
    public boolean pressed(Device device,int code) {
        return code>=0&&code<limit(device)&&(device==Device.Keyboard?keyboard:mouse).get(code);
    }
    public void clear() {keyboard.clear();mouse.clear();}
}
