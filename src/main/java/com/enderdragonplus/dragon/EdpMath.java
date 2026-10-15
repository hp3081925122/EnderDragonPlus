package com.enderdragonplus.dragon;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public final class EdpMath {
    private EdpMath() {
    }

    public static Vec3 direction(float yaw, float pitch) {
        float cp = Mth.cos(pitch);
        return new Vec3(Mth.sin(yaw) * cp, Mth.sin(pitch), -Mth.cos(yaw) * cp);
    }

    public static float yawOf(Vec3 dir) {
        return (float) Math.atan2(dir.x, -dir.z);
    }

    public static float pitchOf(Vec3 dir) {
        return (float) Math.atan2(dir.y, Math.sqrt(dir.x * dir.x + dir.z * dir.z));
    }

    public static Vec3 mouth(Vec3 headCenter, float yawDegrees) {
        float yaw = yawDegrees * Mth.DEG_TO_RAD;
        return headCenter.add(Mth.sin(yaw) * 1.2, -0.2, -Mth.cos(yaw) * 1.2);
    }

    public static double distanceToSegment(Vec3 point, Vec3 a, Vec3 b) {
        Vec3 ab = b.subtract(a);
        double len = ab.lengthSqr();
        if (len < 1.0E-6) {
            return point.distanceTo(a);
        }
        double k = Mth.clamp(point.subtract(a).dot(ab) / len, 0.0, 1.0);
        return point.distanceTo(a.add(ab.scale(k)));
    }

    public static float easeOut(float k) {
        float c = Mth.clamp(k, 0.0F, 1.0F);
        return 1.0F - (1.0F - c) * (1.0F - c);
    }

    public static float easeIn(float k) {
        float c = Mth.clamp(k, 0.0F, 1.0F);
        return c * c;
    }

    public static int[] packDirections(Vec3[] dirs) {
        int[] out = new int[dirs.length * 3];
        for (int i = 0; i < dirs.length; i++) {
            out[i * 3] = Math.round((float) dirs[i].x * 10000.0F);
            out[i * 3 + 1] = Math.round((float) dirs[i].y * 10000.0F);
            out[i * 3 + 2] = Math.round((float) dirs[i].z * 10000.0F);
        }
        return out;
    }

    public static Vec3[] unpackDirections(int[] packed) {
        Vec3[] out = new Vec3[packed.length / 3];
        for (int i = 0; i < out.length; i++) {
            out[i] = new Vec3(packed[i * 3] / 10000.0, packed[i * 3 + 1] / 10000.0, packed[i * 3 + 2] / 10000.0);
        }
        return out;
    }
}
