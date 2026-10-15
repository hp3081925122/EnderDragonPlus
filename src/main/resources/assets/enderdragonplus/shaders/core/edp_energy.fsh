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
    float speed = EdpParams.x;
    float scale = EdpParams.y;
    float sharp = max(EdpParams.z, 0.1);
    float mode = EdpParams.w;
    vec2 uv = texCoord0;
    float glow = 0.0;
    float hot = 0.0;
    if (mode < 0.5) {
        float across = abs(uv.y * 2.0 - 1.0);
        float core = pow(clamp(1.0 - across, 0.0, 1.0), sharp);
        float n1 = texture(Sampler0, vec2(uv.x * scale - t * speed, uv.y * 0.6 + t * 0.05)).r;
        float n2 = texture(Sampler0, vec2(uv.x * scale * 0.5 + t * speed * 0.37, uv.y * 0.9 - t * 0.11)).g;
        glow = core * (0.45 + 1.8 * n1 * n2);
        hot = pow(core, 6.0);
    } else if (mode < 1.5) {
        vec2 p = uv * 2.0 - 1.0;
        float d = length(p);
        float ang = atan(p.y, p.x) / 6.2831853;
        float core = pow(clamp(1.0 - d, 0.0, 1.0), sharp);
        float n = texture(Sampler0, vec2(ang * 2.0 + t * speed, d * scale - t * 0.4)).r;
        glow = core * (0.55 + 0.9 * n);
        hot = pow(core, 5.0);
    } else if (mode < 2.5) {
        float band = pow(clamp(1.0 - abs(uv.y * 2.0 - 1.0), 0.0, 1.0), sharp);
        float n1 = texture(Sampler0, vec2(uv.x * scale + t * speed, uv.y * 0.5 - t * 0.2)).r;
        float n2 = texture(Sampler0, vec2(uv.x * scale * 2.0 - t * speed * 0.6, uv.y * 0.8)).g;
        glow = band * (0.3 + 2.4 * n1 * n2);
        hot = pow(band, 8.0) * n1;
    } else {
        vec2 p = uv * 2.0 - 1.0;
        float w = 0.32 * (1.0 - p.y * p.y);
        float n = texture(Sampler0, vec2(p.y * scale + t * speed, t * 0.3 + p.x)).r;
        float edge = abs(abs(p.x) - w * (0.8 + 0.4 * n));
        float inside = 1.0 - step(w, abs(p.x));
        glow = exp(-edge * sharp) * (1.0 - step(1.0, abs(p.y))) + inside * 0.3 * n;
        hot = exp(-edge * sharp * 3.0);
    }
    vec3 col = mix(vertexColor.rgb, vec3(1.0), clamp(hot, 0.0, 1.0) * 0.85);
    float alpha = clamp(glow, 0.0, 1.0) * vertexColor.a * ColorModulator.a;
    if (alpha < 0.004) {
        discard;
    }
    fragColor = vec4(col * ColorModulator.rgb, alpha);
}
