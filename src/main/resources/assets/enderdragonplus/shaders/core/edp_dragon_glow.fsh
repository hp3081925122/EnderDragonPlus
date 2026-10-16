#version 330

#moj_import <minecraft:fog.glsl>
#moj_import <minecraft:dynamictransforms.glsl>

uniform sampler2D Sampler0;

in float sphericalVertexDistance;
in float cylindricalVertexDistance;
in vec4 vertexColor;
in vec2 texCoord0;

out vec4 fragColor;

float glow_fog_fade(float distance, float start, float end) {
    if (distance <= start) {
        return 1.0;
    }
    if (distance >= end) {
        return 0.0;
    }
    return 1.0 - smoothstep(start, end, distance);
}

void main() {
    vec4 color = texture(Sampler0, texCoord0) * vertexColor * ColorModulator;
    float fade = min(
        glow_fog_fade(sphericalVertexDistance, FogEnvironmentalStart, FogEnvironmentalEnd),
        glow_fog_fade(cylindricalVertexDistance, FogRenderDistanceStart, FogRenderDistanceEnd)
    );
    fragColor = color * fade;
}
