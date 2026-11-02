
package com.gaspoweredcake.client33.systems.modules.movement.speed.modes;

import com.gaspoweredcake.client33.Client33;
import com.gaspoweredcake.client33.events.entity.player.PlayerMoveEvent;
import com.gaspoweredcake.client33.mixininterface.IVec3;
import com.gaspoweredcake.client33.systems.modules.Modules;
import com.gaspoweredcake.client33.systems.modules.movement.Anchor;
import com.gaspoweredcake.client33.systems.modules.movement.speed.SpeedMode;
import com.gaspoweredcake.client33.systems.modules.movement.speed.SpeedModes;
import com.gaspoweredcake.client33.utils.player.PlayerUtils;
import org.joml.Vector2d;

public class Strafe extends SpeedMode {

    public Strafe() {
        super(SpeedModes.Strafe);
    }

    private long timer = 0L;

    @Override
    public void onMove(PlayerMoveEvent event) {
        switch (stage) {
            case 0 -> { // Reset
                if (PlayerUtils.isMoving()) {
                    stage = 1;
                    speed = 1.18f * getDefaultSpeed() - 0.01;
                }
            }
            case 1 -> { // Jump
                if (!PlayerUtils.isMoving() || !mc.player.onGround()) break;

                ((IVec3) event.movement).client33$setY(getHop(0.40123128));
                speed *= settings.ncpSpeed.get();
                stage = 2;
            }
            case 2 -> { // Slowdown after jump
                speed = distance - 0.76 * (distance - getDefaultSpeed());
                stage = 3;
            }
            case 3 -> { // Reset on collision or predict and update speed
                if (!mc.level.noCollision(mc.player.getBoundingBox().move(0.0, mc.player.getDeltaMovement().y, 0.0))
                    || mc.player.verticalCollision && stage > 0) {
                    stage = 0;
                }
                speed = distance - (distance / 159.0);
            }
        }

        speed = Math.max(speed, getDefaultSpeed());

        if (settings.ncpSpeedLimit.get()) {
            if (System.currentTimeMillis() - timer > 2500L) {
                timer = System.currentTimeMillis();
            }

            speed = Math.min(speed, System.currentTimeMillis() - timer > 1250L ? 0.44D : 0.43D);
        }

        Vector2d change = transformStrafe(speed);

        Anchor anchor = Modules.get().get(Anchor.class);
        if (anchor.isActive() && anchor.controlMovement) {
            change.set(anchor.deltaX, anchor.deltaZ);
        }

        ((IVec3) event.movement).client33$setXZ(change.x, change.y);
    }

    public static Vector2d transformStrafe(double speed) {
        float forward = Math.signum(Client33.mc.player.input.getMoveVector().y);
        float side = Math.signum(Client33.mc.player.input.getMoveVector().x);
        float yaw = Client33.mc.player.getYRot(Client33.mc.getDeltaTracker().getGameTimeDeltaPartialTick(true));

        if (forward == 0.0f && side == 0.0f) return new Vector2d();

        float strafe = 90 * side;
        if (forward != 0) strafe *= forward * 0.5f;

        yaw = yaw - strafe;
        if (forward < 0) yaw -= 180;
        double yawRadians = Math.toRadians(yaw);

        return new Vector2d(-Math.sin(yawRadians) * speed, Math.cos(yawRadians) * speed);
    }

    @Override
    public void onTick() {
        distance = Math.sqrt((mc.player.getX() - mc.player.xo) * (mc.player.getX() - mc.player.xo) + (mc.player.getZ() - mc.player.zo) * (mc.player.getZ() - mc.player.zo));
    }
}
