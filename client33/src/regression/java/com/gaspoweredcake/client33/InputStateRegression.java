package com.gaspoweredcake.client33;

import com.gaspoweredcake.client33.utils.misc.input.InputState;
import com.gaspoweredcake.client33.utils.misc.input.LegacyInputCodes;
import static com.gaspoweredcake.client33.utils.misc.input.InputState.Device.*;

public final class InputStateRegression {
    public static void main(String[] args) {run();}
    public static void run() {
        var state=new InputState();
        state.set(Mouse,4,true);
        check(!state.pressed(Keyboard,4)&&state.pressed(Mouse,4),"SDL mouse 4 must not activate keyboard A/scancode 4");
        state.set(Keyboard,4,true);state.set(Mouse,4,false);
        check(state.pressed(Keyboard,4)&&!state.pressed(Mouse,4),"Releasing one device must not release another");
        state.set(Keyboard,229,true);state.set(Keyboard,229,true);
        check(state.pressed(Keyboard,229),"Repeated SDL right-shift presses preserve held state");
        state.set(Keyboard,229,false);
        check(!state.pressed(Keyboard,229),"Release updates state independently of GUI event dispatch");
        for(int invalid:new int[]{-2,-1,512,Integer.MAX_VALUE}) {
            state.set(Keyboard,invalid,true);check(!state.pressed(Keyboard,invalid),"Invalid scancodes remain safe");
        }
        state.clear();check(!state.pressed(Keyboard,4)&&!state.pressed(Mouse,4),"Reset clears both devices");
        check(LegacyInputCodes.keyboard(65)==4&&LegacyInputCodes.keyboard(90)==29&&LegacyInputCodes.keyboard(344)==229,
            "Legacy letter and modifier bindings preserve their physical keys");
        check(LegacyInputCodes.keyboard(32)==44&&LegacyInputCodes.keyboard(290)==58&&LegacyInputCodes.keyboard(-1)==0,
            "Legacy space, function key and unbound values migrate correctly");
        check(LegacyInputCodes.mouse(0)==1&&LegacyInputCodes.mouse(1)==3&&LegacyInputCodes.mouse(2)==2,
            "SDL left/right/middle mapping preserves legacy mouse bindings");
        check(LegacyInputCodes.modifiers(1|2|8)==(3|192|3072),"Legacy shortcut masks map into SDL bit groups");
        System.out.println("SDL typed input-state regressions passed.");
    }
    private static void check(boolean value,String message) {if(!value)throw new AssertionError(message);}
}
