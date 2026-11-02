
package com.gaspoweredcake.client33.pathing;

import com.gaspoweredcake.client33.Client33;
import com.gaspoweredcake.client33.utils.PreInit;

import java.lang.reflect.InvocationTargetException;

public class PathManagers {
    private static IPathManager INSTANCE = new NopPathManager();

    public static IPathManager get() {
        return INSTANCE;
    }

    @PreInit
    public static void init() {
        if (exists("meteordevelopment.voyager.PathManager")) {
            try {
                INSTANCE = (IPathManager) Class.forName("meteordevelopment.voyager.PathManager").getConstructor().newInstance();
            } catch (InstantiationException | IllegalAccessException | InvocationTargetException |
                     NoSuchMethodException | ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
        }

        try {
            if (exists("baritone.api.BaritoneAPI")) {
                if (INSTANCE instanceof NopPathManager) INSTANCE = new BaritonePathManager();
                BaritoneUtils.IS_AVAILABLE = true;
            }
        } catch (LinkageError | RuntimeException e) {
            BaritoneUtils.IS_AVAILABLE = false;
            Client33.LOG.warn("Baritone integration could not start; check that its version matches Minecraft and Fabric.", e);
        }

        Client33.LOG.info("Path Manager: {}", INSTANCE.getName());
    }

    private static boolean exists(String name) {
        try {
            Class.forName(name);
            return true;
        } catch (ClassNotFoundException _) {
            return false;
        }
    }
}
