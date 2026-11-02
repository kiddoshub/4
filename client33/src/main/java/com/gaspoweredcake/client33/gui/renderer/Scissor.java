
package com.gaspoweredcake.client33.gui.renderer;

import com.mojang.blaze3d.systems.RenderSystem;
import com.gaspoweredcake.client33.mixininterface.IGpuDevice;

import java.util.ArrayList;
import java.util.List;

import static com.gaspoweredcake.client33.utils.Utils.getWindowHeight;

public class Scissor {
    public int x, y;
    public int width, height;

    public final List<Runnable> postTasks = new ArrayList<>();

    public Scissor set(double x, double y, double width, double height) {
        if (width < 0) width = 0;
        if (height < 0) height = 0;

        this.x = (int) Math.round(x);
        this.y = (int) Math.round(y);
        this.width = (int) Math.round(width);
        this.height = (int) Math.round(height);

        postTasks.clear();

        return this;
    }

    public void push() {
        ((IGpuDevice) RenderSystem.getDevice()).client33$pushScissor(x, getWindowHeight() - y - height, width, height);
    }

    public void pop() {
        ((IGpuDevice) RenderSystem.getDevice()).client33$popScissor();
    }
}
