package com.enderdragonplus.client;

import com.enderdragonplus.EnderDragonPlus;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

public final class NoiseTexture {
    private static final int SIZE = 256;
    private static DynamicTexture texture;

    private NoiseTexture() {
    }

    public static DynamicTexture texture() {
        if (texture == null) {
            create();
        }
        return texture;
    }

    private static void create() {
        float[] fbmA = fbm(8, 5, 1234L);
        float[] fbmB = fbm(4, 5, 5678L);
        float[] cells = cells(10, 91011L);
        NativeImage image = new NativeImage(SIZE, SIZE, false);
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                int i = y * SIZE + x;
                int r = Mth.clamp((int) (fbmA[i] * 255.0F), 0, 255);
                int g = Mth.clamp((int) (fbmB[i] * 255.0F), 0, 255);
                int b = Mth.clamp((int) (cells[i] * 255.0F), 0, 255);
                image.setPixelABGR(x, y, 0xFF000000 | (b << 16) | (g << 8) | r);
            }
        }
        texture = new DynamicTexture(() -> "EDP noise", image);
        Minecraft.getInstance().getTextureManager().register(EnderDragonPlus.id("noise"), texture);
    }

    private static float[] fbm(int basePeriod, int octaves, long seed) {
        float[] out = new float[SIZE * SIZE];
        float amplitude = 0.5F;
        float total = 0.0F;
        int period = basePeriod;
        RandomSource random = RandomSource.create(seed);
        for (int o = 0; o < octaves; o++) {
            float[] lattice = new float[period * period];
            for (int i = 0; i < lattice.length; i++) {
                lattice[i] = random.nextFloat();
            }
            for (int y = 0; y < SIZE; y++) {
                float fy = y * period / (float) SIZE;
                int y0 = (int) fy;
                float ty = smooth(fy - y0);
                int y1 = (y0 + 1) % period;
                for (int x = 0; x < SIZE; x++) {
                    float fx = x * period / (float) SIZE;
                    int x0 = (int) fx;
                    float tx = smooth(fx - x0);
                    int x1 = (x0 + 1) % period;
                    float a = Mth.lerp(tx, lattice[y0 * period + x0], lattice[y0 * period + x1]);
                    float b = Mth.lerp(tx, lattice[y1 * period + x0], lattice[y1 * period + x1]);
                    out[y * SIZE + x] += Mth.lerp(ty, a, b) * amplitude;
                }
            }
            total += amplitude;
            amplitude *= 0.5F;
            period *= 2;
        }
        float min = Float.MAX_VALUE;
        float max = -Float.MAX_VALUE;
        for (float v : out) {
            min = Math.min(min, v);
            max = Math.max(max, v);
        }
        float range = Math.max(max - min, 1.0E-4F);
        for (int i = 0; i < out.length; i++) {
            out[i] = (out[i] - min) / range;
        }
        return out;
    }

    private static float[] cells(int grid, long seed) {
        RandomSource random = RandomSource.create(seed);
        float[] px = new float[grid * grid];
        float[] py = new float[grid * grid];
        for (int i = 0; i < px.length; i++) {
            px[i] = random.nextFloat();
            py[i] = random.nextFloat();
        }
        float[] out = new float[SIZE * SIZE];
        for (int y = 0; y < SIZE; y++) {
            float fy = y * grid / (float) SIZE;
            int cy = (int) fy;
            for (int x = 0; x < SIZE; x++) {
                float fx = x * grid / (float) SIZE;
                int cx = (int) fx;
                float f1 = Float.MAX_VALUE;
                float f2 = Float.MAX_VALUE;
                for (int oy = -1; oy <= 1; oy++) {
                    for (int ox = -1; ox <= 1; ox++) {
                        int gx = Math.floorMod(cx + ox, grid);
                        int gy = Math.floorMod(cy + oy, grid);
                        float sx = cx + ox + px[gy * grid + gx];
                        float sy = cy + oy + py[gy * grid + gx];
                        float d = Mth.sqrt((sx - fx) * (sx - fx) + (sy - fy) * (sy - fy));
                        if (d < f1) {
                            f2 = f1;
                            f1 = d;
                        } else if (d < f2) {
                            f2 = d;
                        }
                    }
                }
                out[y * SIZE + x] = Mth.clamp((f2 - f1) * 1.5F, 0.0F, 1.0F);
            }
        }
        return out;
    }

    private static float smooth(float t) {
        return t * t * (3.0F - 2.0F * t);
    }
}
