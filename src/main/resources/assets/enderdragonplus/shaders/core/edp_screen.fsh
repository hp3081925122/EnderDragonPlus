#version 150

uniform sampler2D Sampler0;
uniform vec4 EdpParams;
uniform vec4 EdpTint;

in vec2 texCoord0;

out vec4 fragColor;

void main() {
    vec2 uv = texCoord0;
    vec2 dir = uv - 0.5;
    float d = length(dir);
    float ca = EdpParams.x * 0.025 * d;
    float r = texture(Sampler0, uv + dir * ca).r;
    float g = texture(Sampler0, uv).g;
    float b = texture(Sampler0, uv - dir * ca).b;
    vec3 col = vec3(r, g, b);
    float vig = smoothstep(0.3, 0.85, d) * EdpParams.z;
    col = mix(col, EdpTint.rgb, clamp(vig * EdpTint.a, 0.0, 1.0));
    col = mix(col, vec3(1.0), clamp(EdpParams.y, 0.0, 1.0));
    fragColor = vec4(col, 1.0);
}
