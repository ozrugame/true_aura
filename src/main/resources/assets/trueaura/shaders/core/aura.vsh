#version 150

#moj_import <fog.glsl>

in vec3 Position;
in vec4 Color;
in vec2 UV0;
uniform mat4 ModelViewMat;
uniform mat4 ProjMat;
uniform int FogShape;
out vec2 uv;
out vec4 vertexColor;
out float vertexDistance;

void main() {
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);
    vertexDistance = fog_distance(ModelViewMat, Position, FogShape);
    uv = UV0;
    vertexColor = Color;
}
