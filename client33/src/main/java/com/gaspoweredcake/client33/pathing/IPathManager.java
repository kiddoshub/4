
package com.gaspoweredcake.client33.pathing;

import com.gaspoweredcake.client33.settings.Setting;
import com.gaspoweredcake.client33.settings.Settings;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.entity.Entity;
import net.minecraft.core.BlockPos;

import java.util.function.Predicate;

public interface IPathManager {
    String getName();

    boolean isPathing();

    void pause();

    void resume();

    void stop();

    default void moveTo(BlockPos pos) {
        moveTo(pos, false);
    }

    void moveTo(BlockPos pos, boolean ignoreY);

    void moveInDirection(float yaw);

    void mine(Block... blocks);

    void follow(Predicate<Entity> entity);

    float getTargetYaw();

    float getTargetPitch();

    ISettings getSettings();

    interface ISettings {
        Settings get();

        Setting<Boolean> getWalkOnWater();

        Setting<Boolean> getWalkOnLava();

        Setting<Boolean> getStep();

        Setting<Boolean> getNoFall();

        void save();
    }
}
