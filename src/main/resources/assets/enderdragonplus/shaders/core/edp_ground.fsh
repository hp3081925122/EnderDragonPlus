#version 150

uniform sampler2D Sampler0;
uniform vec4 ColorModulator;
uniform float GameTime;
uniform vec4 EdpParams;

in vec2 texCoord0;
in vec4 vertexColor;

out vec4 fragColor;

void main() {
    float t = GameTime * 1200.0;
    vec2 p = texCoord0 * 2.0 - 1.0;
    float r = length(p);
    float mode = EdpParams.x;
    float prog = EdpParams.y;
    vec2 seed = vec2(EdpParams.z, EdpParams.z * 1.37);
    float n = texture(Sampler0, p * 0.35 + seed).r;
    float n2 = texture(Sampler0, p * 0.8 + seed * 2.0).g;
    float cell = texture(Sampler0, p * 0.45 + seed).b;
    float ang = atan(p.y, p.x) / 6.2831853;
    float inDisc = 1.0 - step(1.0, r);
    vec3 col = vertexColor.rgb;
    float a = 0.0;
    if (mode < 0.5) {
        float edge = r + (n - 0.5) * 0.55;
        a = smoothstep(1.0, 0.55, edge) * 0.85;
        col = vertexColor.rgb * 0.12;
    } else if (mode < 1.5) {
        float edge = r + (n - 0.5) * 0.55;
        float rim = exp(-abs(edge - 0.8) * 9.0);
        float ember = smoothstep(0.62, 0.8, n2) * smoothstep(0.95, 0.3, edge);
        a = rim * 0.9 + ember * (0.6 + 0.4 * sin(t * 6.0 + n * 20.0));
    } else if (mode < 2.5) {
        float crack = 1.0 - smoothstep(0.0, 0.07, cell);
        float ray = texture(Sampler0, vec2(ang * 3.0 + EdpParams.z, r * 0.15)).b;
        float rays = (1.0 - smoothstep(0.0, 0.05, ray)) * smoothstep(0.0, 0.15, r);
        a = max(crack, rays) * smoothstep(1.0, 0.35, r + (n - 0.5) * 0.3);
        a += exp(-r * 6.0) * 0.8 * (1.0 - prog);
    } else if (mode < 3.5) {
        float ring = exp(-abs(r - 0.93) * 40.0);
        float fill = (1.0 - step(prog, r)) * 0.18 * (0.6 + 0.4 * n);
        float front = exp(-abs(r - prog) * 30.0) * 0.7;
        float dash = step(0.5, fract(ang * 16.0 + t * 0.5));
        a = (ring * (0.6 + 0.4 * dash) + fill + front) * inDisc;
    } else if (mode < 4.5) {
        float ring = exp(-abs(r - 0.92) * 30.0);
        float inner = exp(-abs(r - 0.7 + 0.03 * sin(t * 3.0)) * 50.0) * 0.6;
        float rune = step(0.65, fract(ang * 10.0 - t * 0.2)) * exp(-abs(r - 0.82) * 40.0);
        float fill = 0.12 * smoothstep(1.0, 0.0, r);
        a = (ring + inner + rune + fill) * inDisc;
    } else {
        float wave = 0.5 + 0.5 * sin(r * 30.0 - t * 4.0);
        a = (0.12 + 0.3 * wave * n) * smoothstep(1.0, 0.6, r) * (0.4 + 0.6 * prog);
    }
    a *= vertexColor.a * ColorModulator.a;
    if (a < 0.004) {
        discard;
    }
    fragColor = vec4(col * ColorModulator.rgb, clamp(a, 0.0, 1.0));
}
