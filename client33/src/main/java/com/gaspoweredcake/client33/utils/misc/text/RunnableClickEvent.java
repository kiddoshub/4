
package com.gaspoweredcake.client33.utils.misc.text;

/**
 * Allows arbitrary code execution in a click event
 */
public class RunnableClickEvent extends Client33ClickEvent {
    public final Runnable runnable;

    public RunnableClickEvent(Runnable runnable) {
        super(null); // Should ensure no vanilla code is triggered, and only we handle it
        this.runnable = runnable;
    }
}
