#version 150

uniform sampler2D Sampler0;
uniform vec4 ColorModulator;
uniform float GameTime;
uniform vec4 EdpParams;

in vec2 texCoord0;
in vec4 vertexColor;
in vec3 viewNormal;
in vec3 viewDir;

out vec4 fragColor;

void main() {
    float t = GameTime * 1200.0;
    vec3 nrm = normalize(viewNormal);
    float facing = abs(dot(nrm, normalize(viewDir)));
    float fres = 1.0 - facing;
    float mode = EdpParams.x;
    float power = max(EdpParams.y, 0.1);
    float scale = EdpParams.z;
    float speed = EdpParams.w;
    float n1 = texture(Sampler0, texCoord0 * scale + vec2(t * speed, t * speed * 0.6)).r;
    float n2 = texture(Sampler0, texCoord0 * scale * 2.0 - vec2(t * speed * 0.8, 0.0)).g;
    vec3 col = vertexColor.rgb;
    float a = 0.0;
    if (mode < 0.5) {
        float rim = pow(fres, power);
        float cell = texture(Sampler0, texCoord0 * vec2(scale * 2.0, scale)).b;
        float hex = 1.0 - smoothstep(0.0, 0.08, cell);
        float ripple = 0.5 + 0.5 * sin(texCoord0.y * 40.0 - t * 5.0 + n1 * 6.0);
        a = rim * 0.9 + hex * (0.18 + 0.35 * ripple) * (0.4 + rim) + n1 * n2 * 0.15;
        col = mix(col, vec3(1.0), pow(rim, 3.0) * 0.6);
    } else if (mode < 1.5) {
        float rim = pow(fres, power);
        col = mix(vec3(0.0), vertexColor.rgb * (1.2 + n1), rim);
        a = 1.0;
    } else if (mode < 2.5) {
        float core = pow(facing, 2.0);
        float rim = pow(fres, power);
        a = (core * 0.9 + rim * 0.7) * (0.5 + 1.8 * n1 * n2);
        col = mix(col, vec3(1.0), core * 0.7);
    } else {
        float rim = pow(fres, power);
        float dissolve = smoothstep(0.35, 0.75, n1 + n2 * 0.5);
        a = (rim * 0.85 + 0.12) * dissolve;
        col = mix(col, vec3(1.0), rim * rim * 0.5);
    }
    a *= vertexColor.a * ColorModulator.a;
    if (a < 0.004) {
        discard;
    }
    fragColor = vec4(col * ColorModulator.rgb, clamp(a, 0.0, 1.0));
}
