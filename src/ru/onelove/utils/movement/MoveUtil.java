package ru.onelove.utils.movement;

import net.minecraft.util.math.MathHelper;
import net.minecraft.client.Minecraft;

/**
 * Утилита для управления движением игрока.
 */
public class MoveUtil {
    private static final Minecraft mc = Minecraft.getInstance();

    /**
     * Устанавливает скорость движения игрока (только по X и Z).
     *
     * @param speed скорость (блоков/тик)
     */
    public static void setMotion(double speed) {
        if (mc.player == null) return;

        float yaw = mc.player.rotationYaw;
        double forward = mc.player.moveForward;
        double strafe = mc.player.moveStrafing;

        if (forward == 0 && strafe == 0) {
            mc.player.setMotion(0, mc.player.getMotion().y, 0);
            return;
        }

        if (forward != 0) {
            if (strafe > 0) {
                yaw += (forward > 0 ? -45 : 45);
            } else if (strafe < 0) {
                yaw += (forward > 0 ? 45 : -45);
            }
            strafe = 0;
            forward = forward > 0 ? 1 : -1;
        }

        double rad = Math.toRadians(yaw + 90);
        double motionX = forward * speed * Math.cos(rad) + strafe * speed * Math.sin(rad);
        double motionZ = forward * speed * Math.sin(rad) - strafe * speed * Math.cos(rad);

        mc.player.setMotion(motionX, mc.player.getMotion().y, motionZ);
    }
}
