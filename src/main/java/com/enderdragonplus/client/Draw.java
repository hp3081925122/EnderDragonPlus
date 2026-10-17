package com.enderdragonplus.client;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;

import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

public final class Draw {
    private static long terrainFrame = Long.MIN_VALUE;
    private static double terrainX, terrainZ;
    private static float terrainHalf, terrainLift;
    private static int terrainCells;
    private static final RandomSource LIGHTNING_RANDOM = RandomSource.create(0L);
    private static long frameNumber;

    public static long frameNumber() {
        return frameNumber;
    }

    private static final Int2ObjectOpenHashMap<float[][]> CIRCLES = new Int2ObjectOpenHashMap<>();
    private static final Long2ObjectOpenHashMap<float[]> SPHERES = new Long2ObjectOpenHashMap<>();
    private static double[] sideX = new double[16];
    private static double[] sideY = new double[16];
    private static double[] sideZ = new double[16];
    private static double[] terrainHeights = new double[0];
    private static final Vec3[] beamPoints = new Vec3[2];
    private static final float[] beamWidths = new float[2];
    private static final float[] beamAlphas = new float[2];

    private static float[][] circle(int segments) {
        float[][] result = CIRCLES.get(segments);
        if (result == null) {
            result = new float[2][segments + 1];
            for (int i = 0; i <= segments; i++) {
                float angle = Mth.TWO_PI * i / segments;
                result[0][i] = Mth.cos(angle);
                result[1][i] = Mth.sin(angle);
            }
            CIRCLES.put(segments, result);
        }
        return result;
    }

    private static float[] sphereMesh(int rings, int segments) {
        long key = ((long) rings << 32) | (segments & 0xffffffffL);
        float[] result = SPHERES.get(key);
        if (result == null) {
            result = new float[(rings + 1) * (segments + 1) * 3];
            for (int i = 0; i <= rings; i++) {
                float theta = Mth.PI * i / rings;
                float st = Mth.sin(theta);
                for (int j = 0; j <= segments; j++) {
                    float phi = Mth.TWO_PI * j / segments;
                    int index = (i * (segments + 1) + j) * 3;
                    result[index] = st * Mth.cos(phi);
                    result[index + 1] = Mth.cos(theta);
                    result[index + 2] = st * Mth.sin(phi);
                }
            }
            SPHERES.put(key, result);
        }
        return result;
    }

    public enum Material {
        ENERGY(true),
        GROUND(true),
        GROUND_ALPHA(false),
        SPHERE(true),
        SPHERE_ALPHA(false),
        DISTORT(false);

        final boolean additive;

        Material(boolean additive) {
            this.additive = additive;
        }
    }

    public static final int MODE_STRIP = 0;
    public static final int MODE_RADIAL = 1;
    public static final int MODE_RING = 2;
    public static final int MODE_RIFT = 3;

    public static final int GROUND_SCORCH = 0;
    public static final int GROUND_SCORCH_GLOW = 1;
    public static final int GROUND_CRACK = 2;
    public static final int GROUND_WARNING = 3;
    public static final int GROUND_SAFE = 4;
    public static final int GROUND_DANGER = 5;

    public static final int SPHERE_SHIELD = 0;
    public static final int SPHERE_DARK = 1;
    public static final int SPHERE_ORB = 2;
    public static final int SPHERE_SHELL = 3;

    public static final int DISTORT_LENS = 0;
    public static final int DISTORT_RIFT = 1;
    public static final int DISTORT_BAND = 2;

    private static BufferBuilder buffer;
    private static Material current;
    private static boolean hasNormal;
    private static float nx;
    private static float ny = 1.0F;
    private static float nz;
    private static double cx;
    private static double cy;
    private static double cz;
    private static Vec3 camPos = Vec3.ZERO;
    private static Vec3 camRight = new Vec3(1.0, 0.0, 0.0);
    private static Vec3 camUp = new Vec3(0.0, 1.0, 0.0);

    private Draw() {
    }

    public static void setup(Camera camera) {
        frameNumber++;
        camPos = camera.getPosition();
        cx = camPos.x;
        cy = camPos.y;
        cz = camPos.z;
        Vector3f left = camera.getLeftVector();
        Vector3f up = camera.getUpVector();
        camRight = new Vec3(-left.x(), -left.y(), -left.z());
        camUp = new Vec3(up.x(), up.y(), up.z());
    }

    public static Vec3 cameraPos() {
        return camPos;
    }

    private static ShaderInstance shaderOf(Material material) {
        return switch (material) {
            case ENERGY -> EdpShaders.energy;
            case GROUND, GROUND_ALPHA -> EdpShaders.ground;
            case SPHERE, SPHERE_ALPHA -> EdpShaders.sphere;
            case DISTORT -> EdpShaders.distort;
        };
    }

    public static boolean begin(Material material) {
        if (shaderOf(material) == null) {
            return false;
        }
        if (material == Material.DISTORT && !SceneCopy.ready()) {
            return false;
        }
        current = material;
        hasNormal = material == Material.SPHERE || material == Material.SPHERE_ALPHA || material == Material.DISTORT;
        VertexFormat format = DefaultVertexFormat.POSITION_TEX_COLOR;
        if (hasNormal) {
            format = DefaultVertexFormat.POSITION_TEX_COLOR_NORMAL;
        }
        buffer = Tesselator.getInstance().getBuilder();
        buffer.begin(VertexFormat.Mode.QUADS, format);
        return true;
    }

    public static void end(float a, float b, float c, float d) {
        BufferBuilder.RenderedBuffer mesh = buffer.endOrDiscardIfEmpty();
        buffer = null;
        if (mesh == null) {
            return;
        }
        long edpSubmit = DebugProfiler.begin();
        int edpVertices = mesh.drawState().vertexCount();
        ShaderInstance shader = shaderOf(current);
        shader.safeGetUniform("EdpParams").set(a, b, c, d);
        RenderSystem.setShader(() -> shader);
        if (current == Material.DISTORT) {
            RenderSystem.setShaderTexture(0, SceneCopy.textureId());
            RenderSystem.setShaderTexture(1, NoiseTexture.id());
        } else {
            RenderSystem.setShaderTexture(0, NoiseTexture.id());
        }
        if (current.additive) {
            RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
        } else {
            RenderSystem.defaultBlendFunc();
        }
        EdpShaders.setAdditive(current.additive);
        BufferUploader.drawWithShader(mesh);
        DebugProfiler.submit(edpSubmit, edpVertices);
    }

    public static void normal(double x, double y, double z) {
        nx = (float) x;
        ny = (float) y;
        nz = (float) z;
    }

    public static void vertex(double x, double y, double z, float u, float v, int rgb, float alpha) {
        float r = ((rgb >> 16) & 255) / 255.0F;
        float g = ((rgb >> 8) & 255) / 255.0F;
        float b = (rgb & 255) / 255.0F;
        VertexConsumer c = buffer.vertex((float) (x - cx), (float) (y - cy), (float) (z - cz)).uv(u, v).color(r, g, b, Mth.clamp(alpha, 0.0F, 1.0F));
        if (hasNormal) {
            c.normal(nx, ny, nz);
        }
        c.endVertex();
    }

    public static void vertex(Vec3 p, float u, float v, int rgb, float alpha) {
        vertex(p.x, p.y, p.z, u, v, rgb, alpha);
    }

    public static void quad(Vec3 a, Vec3 b, Vec3 c, Vec3 d, int rgb, float alpha) {
        vertex(a, 0.0F, 0.0F, rgb, alpha);
        vertex(b, 0.0F, 1.0F, rgb, alpha);
        vertex(c, 1.0F, 1.0F, rgb, alpha);
        vertex(d, 1.0F, 0.0F, rgb, alpha);
    }

    public static void plane(Vec3 center, Vec3 right, Vec3 up, int rgb, float alpha) {
        Vec3 n = right.cross(up);
        if (n.lengthSqr() > 1.0E-8) {
            n = n.normalize();
        }
        normal(n.x, n.y, n.z);
        quad(center.subtract(right).subtract(up), center.subtract(right).add(up), center.add(right).add(up), center.add(right).subtract(up), rgb, alpha);
    }

    public static void billboard(Vec3 center, float size, int rgb, float alpha) {
        float h = size * 0.5F;
        plane(center, camRight.scale(h), camUp.scale(h), rgb, alpha);
    }

    public static void ground(Vec3 center, float half, int rgb, float alpha) {
        double y = center.y + 0.04;
        normal(0.0, 1.0, 0.0);
        vertex(center.x - half, y, center.z - half, 0.0F, 0.0F, rgb, alpha);
        vertex(center.x - half, y, center.z + half, 0.0F, 1.0F, rgb, alpha);
        vertex(center.x + half, y, center.z + half, 1.0F, 1.0F, rgb, alpha);
        vertex(center.x + half, y, center.z - half, 1.0F, 0.0F, rgb, alpha);
    }

    public static void terrain(Vec3 center, float half, int cells, float lift, int rgb, float alpha) {
        long edpTerrain = DebugProfiler.begin();
        try {
            ClientLevel level = Minecraft.getInstance().level;
            if (level == null) {
                return;
            }
            int n = cells + 1;
            if (terrainHeights.length < n * n) {
                terrainHeights = new double[n * n];
            }
            double[] heights = terrainHeights;
            double step = half * 2.0 / cells;
            double x0 = center.x - half;
            double z0 = center.z - half;
            if (terrainFrame != frameNumber || terrainX != center.x || terrainZ != center.z
                    || terrainHalf != half || terrainCells != cells || terrainLift != lift) {
                int previousX = Integer.MIN_VALUE;
                for (int i = 0; i < n; i++) {
                    int x = Mth.floor(x0 + i * step);
                    int previousZ = Integer.MIN_VALUE;
                    for (int j = 0; j < n; j++) {
                        int z = Mth.floor(z0 + j * step);
                        if (i > 0 && x == previousX) {
                            heights[i * n + j] = heights[(i - 1) * n + j];
                        } else if (j > 0 && z == previousZ) {
                            heights[i * n + j] = heights[i * n + j - 1];
                        } else {
                            DebugProfiler.heightSample();
                            heights[i * n + j] = level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z) + lift;
                        }
                        previousZ = z;
                    }
                    previousX = x;
                }
                terrainFrame = frameNumber;
                terrainX = center.x;
                terrainZ = center.z;
                terrainHalf = half;
                terrainCells = cells;
                terrainLift = lift;
            }
            double floor = center.y - 16.0;
            normal(0.0, 1.0, 0.0);
            for (int i = 0; i < cells; i++) {
                for (int j = 0; j < cells; j++) {
                    double h00 = heights[i * n + j];
                    double h01 = heights[i * n + j + 1];
                    double h11 = heights[(i + 1) * n + j + 1];
                    double h10 = heights[(i + 1) * n + j];
                    if (h00 < floor || h01 < floor || h11 < floor || h10 < floor) {
                        continue;
                    }
                    float u0 = i / (float) cells;
                    float u1 = (i + 1) / (float) cells;
                    float v0 = j / (float) cells;
                    float v1 = (j + 1) / (float) cells;
                    vertex(x0 + i * step, h00, z0 + j * step, u0, v0, rgb, alpha);
                    vertex(x0 + i * step, h01, z0 + (j + 1) * step, u0, v1, rgb, alpha);
                    vertex(x0 + (i + 1) * step, h11, z0 + (j + 1) * step, u1, v1, rgb, alpha);
                    vertex(x0 + (i + 1) * step, h10, z0 + j * step, u1, v0, rgb, alpha);
                }
            }
        } finally {
            DebugProfiler.terrain(edpTerrain);
        }
    }

    public static void ribbon(Vec3[] points, float[] widths, float[] alphas, int rgb, float uStart, float uEnd) {
        int n = points.length;
        if (n < 2) {
            return;
        }
        if (sideX.length < n) {
            sideX = new double[n];
            sideY = new double[n];
            sideZ = new double[n];
        }
        for (int i = 0; i < n; i++) {
            Vec3 prev = points[Math.max(0, i - 1)];
            Vec3 next = points[Math.min(n - 1, i + 1)];
            double tx = next.x - prev.x;
            double ty = next.y - prev.y;
            double tz = next.z - prev.z;
            double vx = camPos.x - points[i].x;
            double vy = camPos.y - points[i].y;
            double vz = camPos.z - points[i].z;
            double x = ty * vz - tz * vy;
            double y = tz * vx - tx * vz;
            double z = tx * vy - ty * vx;
            if (x * x + y * y + z * z < 1.0E-8) {
                x = camRight.x;
                y = camRight.y;
                z = camRight.z;
            }
            double length = Math.sqrt(x * x + y * y + z * z);
            double width = widths[i] * 0.5;
            sideX[i] = (length < 1.0E-4 ? 0.0 : x / length) * width;
            sideY[i] = (length < 1.0E-4 ? 0.0 : y / length) * width;
            sideZ[i] = (length < 1.0E-4 ? 0.0 : z / length) * width;
        }
        normal(0.0, 1.0, 0.0);
        for (int i = 0; i < n - 1; i++) {
            float u0 = Mth.lerp(i / (float) (n - 1), uStart, uEnd);
            float u1 = Mth.lerp((i + 1) / (float) (n - 1), uStart, uEnd);
            Vec3 a = points[i];
            Vec3 b = points[i + 1];
            vertex(a.x - sideX[i], a.y - sideY[i], a.z - sideZ[i], u0, 0.0F, rgb, alphas[i]);
            vertex(a.x + sideX[i], a.y + sideY[i], a.z + sideZ[i], u0, 1.0F, rgb, alphas[i]);
            vertex(b.x + sideX[i + 1], b.y + sideY[i + 1], b.z + sideZ[i + 1], u1, 1.0F, rgb, alphas[i + 1]);
            vertex(b.x - sideX[i + 1], b.y - sideY[i + 1], b.z - sideZ[i + 1], u1, 0.0F, rgb, alphas[i + 1]);
        }
    }

    public static void beam(Vec3 a, Vec3 b, float width, int rgb, float alpha, float uEnd) {
        beamPoints[0] = a;
        beamPoints[1] = b;
        beamWidths[0] = width;
        beamWidths[1] = width;
        beamAlphas[0] = alpha;
        beamAlphas[1] = alpha;
        ribbon(beamPoints, beamWidths, beamAlphas, rgb, 0.0F, uEnd);
    }

    public static Vec3[] basis(Vec3 axis) {
        Vec3 n = axis.normalize();
        Vec3 ref = new Vec3(0.0, 1.0, 0.0);
        if (Math.abs(n.y) > 0.9) {
            ref = new Vec3(1.0, 0.0, 0.0);
        }
        Vec3 u = n.cross(ref).normalize();
        Vec3 v = n.cross(u).normalize();
        return new Vec3[]{u, v};
    }

    public static void cylinder(Vec3 a, Vec3 b, float radius, int segments, int rgb, float alpha, float uEnd) {
        Vec3 axis = b.subtract(a);
        if (axis.lengthSqr() < 1.0E-6) {
            return;
        }
        Vec3[] base = basis(axis);
        float[][] angles = circle(segments);
        for (int i = 0; i < segments; i++) {
            double x0 = base[0].x * angles[0][i] + base[1].x * angles[1][i];
            double y0 = base[0].y * angles[0][i] + base[1].y * angles[1][i];
            double z0 = base[0].z * angles[0][i] + base[1].z * angles[1][i];
            double x1 = base[0].x * angles[0][i + 1] + base[1].x * angles[1][i + 1];
            double y1 = base[0].y * angles[0][i + 1] + base[1].y * angles[1][i + 1];
            double z1 = base[0].z * angles[0][i + 1] + base[1].z * angles[1][i + 1];
            float v0 = i / (float) segments;
            float v1 = (i + 1) / (float) segments;
            normal(x0, y0, z0);
            vertex(a.x + x0 * radius, a.y + y0 * radius, a.z + z0 * radius, 0.0F, v0, rgb, alpha);
            vertex(b.x + x0 * radius, b.y + y0 * radius, b.z + z0 * radius, uEnd, v0, rgb, alpha);
            normal(x1, y1, z1);
            vertex(b.x + x1 * radius, b.y + y1 * radius, b.z + z1 * radius, uEnd, v1, rgb, alpha);
            vertex(a.x + x1 * radius, a.y + y1 * radius, a.z + z1 * radius, 0.0F, v1, rgb, alpha);
        }
    }

    public static void sphere(Vec3 c, float radius, int rings, int segments, int rgb, float alpha) {
        float[] mesh = sphereMesh(rings, segments);
        for (int i = 0; i < rings; i++) {
            for (int j = 0; j < segments; j++) {
                int top = (i * (segments + 1) + j) * 3;
                int bottom = ((i + 1) * (segments + 1) + j) * 3;
                sphereVertex(c, radius, mesh, top, j / (float) segments, i / (float) rings, rgb, alpha);
                sphereVertex(c, radius, mesh, bottom, j / (float) segments, (i + 1) / (float) rings, rgb, alpha);
                sphereVertex(c, radius, mesh, bottom + 3, (j + 1) / (float) segments, (i + 1) / (float) rings, rgb, alpha);
                sphereVertex(c, radius, mesh, top + 3, (j + 1) / (float) segments, i / (float) rings, rgb, alpha);
            }
        }
    }

    private static void sphereVertex(Vec3 c, float radius, float[] mesh, int index, float u, float v, int rgb, float alpha) {
        double x = mesh[index];
        double y = mesh[index + 1];
        double z = mesh[index + 2];
        normal(x, y, z);
        vertex(c.x + x * radius, c.y + y * radius, c.z + z * radius, u, v, rgb, alpha);
    }

    public static void ring(Vec3 c, Vec3 axis, float inner, float outer, int segments, int rgb, float alphaInner, float alphaOuter, float uRepeat) {
        Vec3[] base = basis(axis);
        Vec3 n = axis.normalize();
        normal(n.x, n.y, n.z);
        float[][] angles = circle(segments);
        for (int i = 0; i < segments; i++) {
            double x0 = base[0].x * angles[0][i] + base[1].x * angles[1][i];
            double y0 = base[0].y * angles[0][i] + base[1].y * angles[1][i];
            double z0 = base[0].z * angles[0][i] + base[1].z * angles[1][i];
            double x1 = base[0].x * angles[0][i + 1] + base[1].x * angles[1][i + 1];
            double y1 = base[0].y * angles[0][i + 1] + base[1].y * angles[1][i + 1];
            double z1 = base[0].z * angles[0][i + 1] + base[1].z * angles[1][i + 1];
            float u0 = uRepeat * i / segments;
            float u1 = uRepeat * (i + 1) / segments;
            vertex(c.x + x0 * inner, c.y + y0 * inner, c.z + z0 * inner, u0, 0.0F, rgb, alphaInner);
            vertex(c.x + x0 * outer, c.y + y0 * outer, c.z + z0 * outer, u0, 1.0F, rgb, alphaOuter);
            vertex(c.x + x1 * outer, c.y + y1 * outer, c.z + z1 * outer, u1, 1.0F, rgb, alphaOuter);
            vertex(c.x + x1 * inner, c.y + y1 * inner, c.z + z1 * inner, u1, 0.0F, rgb, alphaInner);
        }
    }

    public static Vec3[] lightning(Vec3 a, Vec3 b, int segments, double jitter, long seed) {
        RandomSource random = LIGHTNING_RANDOM;
        random.setSeed(seed);
        Vec3 axis = b.subtract(a);
        Vec3[] base = basis(axis);
        Vec3[] points = new Vec3[segments + 1];
        for (int i = 0; i <= segments; i++) {
            double k = i / (double) segments;
            Vec3 p = a.add(axis.scale(k));
            if (i > 0 && i < segments) {
                double envelope = Math.sin(k * Math.PI);
                double ox = (random.nextDouble() - 0.5) * 2.0 * jitter * envelope;
                double oy = (random.nextDouble() - 0.5) * 2.0 * jitter * envelope;
                p = p.add(base[0].scale(ox)).add(base[1].scale(oy));
            }
            points[i] = p;
        }
        return points;
    }

    public static int rgb(float r, float g, float b) {
        int ri = Mth.clamp((int) (r * 255.0F), 0, 255);
        int gi = Mth.clamp((int) (g * 255.0F), 0, 255);
        int bi = Mth.clamp((int) (b * 255.0F), 0, 255);
        return (ri << 16) | (gi << 8) | bi;
    }

    public static int mix(int a, int b, float k) {
        float ar = ((a >> 16) & 255) / 255.0F;
        float ag = ((a >> 8) & 255) / 255.0F;
        float ab = (a & 255) / 255.0F;
        float br = ((b >> 16) & 255) / 255.0F;
        float bg = ((b >> 8) & 255) / 255.0F;
        float bb = (b & 255) / 255.0F;
        return rgb(Mth.lerp(k, ar, br), Mth.lerp(k, ag, bg), Mth.lerp(k, ab, bb));
    }
}
