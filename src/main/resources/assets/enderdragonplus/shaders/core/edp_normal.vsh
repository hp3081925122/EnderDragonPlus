#version 150

in vec3 Position;
in vec2 UV0;
in vec4 Color;
in vec3 Normal;

uniform mat4 ModelViewMat;
uniform mat4 ProjMat;

out vec2 texCoord0;
out vec4 vertexColor;
out vec3 viewNormal;
out vec3 viewDir;

void main() {
    vec4 viewPos = ModelViewMat * vec4(Position, 1.0);
    gl_Position = ProjMat * viewPos;
    texCoord0 = UV0;
    vertexColor = Color;
    viewNormal = normalize(mat3(ModelViewMat) * Normal);
    viewDir = normalize(-viewPos.xyz);
}
