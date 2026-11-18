#version 150
in vec4 vertexColor;
in float vertexDistance;
uniform vec4 ColorModulator;
uniform float FogStart;
uniform float FogEnd;
out vec4 fragColor;
void main() {
    float fog = 1.0 - smoothstep(FogStart, max(FogStart + 0.001, FogEnd), vertexDistance);
    fragColor = vertexColor * ColorModulator;
    fragColor.a *= fog;
    if (fragColor.a < 0.002) discard;
}
