package com.enderdragonplus.client;

import com.mojang.blaze3d.vertex.VertexConsumer;

public final class TintedVertexConsumer implements VertexConsumer {
    private final VertexConsumer delegate;
    private final int red;
    private final int green;
    private final int blue;

    public TintedVertexConsumer(VertexConsumer delegate, int color) {
        this.delegate = delegate;
        this.red = color >> 16 & 255;
        this.green = color >> 8 & 255;
        this.blue = color & 255;
    }

    @Override
    public VertexConsumer vertex(double x, double y, double z) {
        this.delegate.vertex(x, y, z);
        return this;
    }

    @Override
    public VertexConsumer color(int red, int green, int blue, int alpha) {
        this.delegate.color(red * this.red / 255, green * this.green / 255, blue * this.blue / 255, alpha);
        return this;
    }

    @Override
    public VertexConsumer uv(float u, float v) {
        this.delegate.uv(u, v);
        return this;
    }

    @Override
    public VertexConsumer overlayCoords(int u, int v) {
        this.delegate.overlayCoords(u, v);
        return this;
    }

    @Override
    public VertexConsumer uv2(int u, int v) {
        this.delegate.uv2(u, v);
        return this;
    }

    @Override
    public VertexConsumer normal(float x, float y, float z) {
        this.delegate.normal(x, y, z);
        return this;
    }

    @Override
    public void endVertex() {
        this.delegate.endVertex();
    }

    @Override
    public void defaultColor(int red, int green, int blue, int alpha) {
        this.delegate.defaultColor(red * this.red / 255, green * this.green / 255, blue * this.blue / 255, alpha);
    }

    @Override
    public void unsetDefaultColor() {
        this.delegate.unsetDefaultColor();
    }
}
