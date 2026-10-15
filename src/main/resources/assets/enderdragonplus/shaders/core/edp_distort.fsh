#version 150

uniform sampler2D Sampler0;
uniform sampler2D Sampler1;
uniform vec4 ColorModulator;
uniform vec2 ScreenSize;
uniform float GameTime;
uniform vec4 EdpParams;

in vec2 texCoord0;
in vec4 vertexColor;
in vec3 viewNormal;
in vec3 viewDir;

out vec4 fragColor;

void main() {
    float t = GameTime * 1200.0;
    vec2 screenUv = gl_FragCoord.xy / ScreenSize;
    float strength = EdpParams.x * vertexColor.a;
    float mode = EdpParams.y;
    vec2 offset = vec2(0.0);
    float mask = 1.0;
    if (mode < 0.5) {
        vec3 nrm = normalize(viewNormal);
        float facing = abs(dot(nrm, normalize(viewDir)));
        mask = smoothstep(0.0, 0.5, facing);
        vec2 swirl = vec2(-nrm.y, nrm.x);
        offset = (nrm.xy * 0.6 + swirl * 0.8) * facing * strength;
    } else {
        vec2 p = texCoord0 * 2.0 - 1.0;
        float n1 = texture(Sampler1, texCoord0 * EdpParams.z + vec2(t * EdpParams.w, -t * EdpParams.w * 0.7)).r;
        float n2 = texture(Sampler1, texCoord0 * EdpParams.z * 1.3 - vec2(0.0, t * EdpParams.w)).g;
        if (mode < 1.5) {
            float w = 0.38 * (1.0 - p.y * p.y);
            mask = smoothstep(w + 0.25, w * 0.4, abs(p.x)) * smoothstep(1.0, 0.85, abs(p.y));
        } else {
            mask = exp(-abs(p.y) * 3.0);
        }
        offset = (vec2(n1, n2) - 0.5) * strength * 2.0;
    }
    if (mask < 0.004) {
        discard;
    }
    vec3 col = texture(Sampler0, clamp(screenUv + offset * mask, vec2(0.001), vec2(0.999))).rgb;
    col = mix(col, col * vertexColor.rgb * 1.5, 0.25 * mask);
    fragColor = vec4(col * ColorModulator.rgb, mask);
}
