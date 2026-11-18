#version 150

in vec2 uv;
in vec4 vertexColor;
in float vertexDistance;
uniform vec4 ColorModulator;
uniform float FogStart;
uniform float FogEnd;
uniform float AuraTime;
uniform float AuraIntensity;
uniform float ViewAngle;
uniform int AuraShape;
uniform vec3 MainColor;
uniform vec3 HighlightColor;
uniform vec3 BaseColor;
out vec4 fragColor;

// Every angular frequency is integral: the seam remains continuous around the body.
void main() {
    float a = uv.x * 6.28318530718;
    float h = uv.y;
    float t = AuraTime;
    // Explicit h*k - t*w phases make highlights travel from feet to head.
    // The old time-nested angular wobble read as stationary stripes on a tube.
    float wave = 0.65 * sin(h * 8.0 - t * 4.0 + a * 2.0)
               + 0.24 * sin(h * 17.0 - t * 7.0 + a * 5.0);
    float flow = 0.5 + 0.5 * sin(a * 7.0 + wave);
    float rising = 0.5 + 0.5 * sin(h * 18.0 - t * 8.0 + a * 3.0);
    if (AuraShape == 1) {
        flow = 0.5 + 0.5 * sin(a * 3.0 + h * 8.0 - t * 2.4 + wave * 0.3);
        rising = 0.5 + 0.5 * sin(h * 12.0 - t * 4.8 + a * 3.0);
    } else if (AuraShape == 2) {
        flow = pow(0.5 + 0.5 * cos(a * 8.0 + t * 0.8 + wave * 0.25), 1.5);
        rising = 0.5 + 0.5 * sin(h * 22.0 - t * 10.0);
    }
    float rim = pow(1.0 - abs(cos(a - ViewAngle)), 2.0);
    float footFade = smoothstep(0.0, 0.045, h);
    float alpha;
    vec3 color;

    if (AuraShape >= 3 && vertexColor.r < 0.75) {
        float top = 1.0 - smoothstep(0.80, 1.0, h);
        float pattern;
        if (AuraShape == 3) {
            // Three continuous helical ribbons; gaps reveal the player underneath.
            pattern = pow(0.5 + 0.5 * cos(a * 3.0 - h * 10.0 + t * 4.0), 14.0);
        } else if (AuraShape == 4) {
            float zigzag = (2.0 / 3.14159265) * asin(sin(h * 26.0 - t * 8.0));
            pattern = pow(0.5 + 0.5 * cos(a * 6.0 + zigzag * 2.0), 35.0);
            pattern *= 0.55 + 0.45 * pow(0.5 + 0.5 * sin(t * 18.0 + h * 12.0), 2.0);
        } else if (AuraShape == 5) {
            // Broken horizontal waves travel upward instead of forming a solid sphere.
            pattern = pow(0.5 + 0.5 * cos(h * 22.0 - t * 6.0), 12.0);
            pattern *= smoothstep(0.15, 0.65, 0.5 + 0.5 * sin(a * 5.0 + h * 3.0 - t * 2.0));
        } else if (AuraShape == 6) {
            pattern = pow(0.5 + 0.5 * cos(a * 9.0 + sin(h * 12.0 - t * 7.0) * 1.2), 6.0);
            pattern *= 0.3 + 0.7 * pow(0.5 + 0.5 * sin(h * 24.0 - t * 12.0 + a * 3.0), 2.0);
            top = 1.0 - smoothstep(0.55 + pattern * 0.2, 0.82 + pattern * 0.18, h);
        } else if (AuraShape == 7) {
            float petal = 0.5 + 0.5 * cos(a * 6.0 + t * 0.8);
            pattern = smoothstep(0.35 + h * 0.35, 0.95, petal);
            pattern *= 0.35 + 0.65 * pow(0.5 + 0.5 * cos(h * 10.0 - t * 3.0), 3.0);
            top = 1.0 - smoothstep(0.45 + petal * 0.30, 0.62 + petal * 0.38, h);
        } else if (AuraShape == 8) {
            pattern = pow(0.5 + 0.5 * cos(a * 4.0 - h * 3.0 + t * 2.0), 24.0);
            pattern *= smoothstep(0.05, 0.28, h) * (0.65 + 0.35 * sin(h * 8.0 - t * 3.0));
        } else {
            pattern = pow(0.5 + 0.5 * sin(a * 5.0 + h * 5.0 + sin(h * 9.0 - t * 3.0)), 4.0);
            pattern *= 0.30 + 0.70 * pow(0.5 + 0.5 * cos(h * 15.0 - t * 4.0), 2.0);
        }
        alpha = pattern * top * footFade * (vertexColor.r > 0.25 ? 0.85 : 0.48);
        color = mix(MainColor, HighlightColor, pow(pattern, 2.0));
    } else if (vertexColor.r > 0.75) {
        float needles = pow(max(0.0, sin(a * 11.0 + wave)), 6.0);
        float top = 1.0 - smoothstep(0.12 + needles * 0.55, 0.30 + needles * 0.65, h);
        alpha = top * (0.025 + needles * 0.75) * (0.45 + rising * 0.55) * footFade;
        color = mix(BaseColor * vec3(0.58, 0.24, 0.83), mix(BaseColor, vec3(1.0), 0.25), needles);
    } else if (vertexColor.r > 0.25) {
        float strands = pow(flow, 9.0);
        float tip = 0.43 + 0.54 * pow(flow, 0.4);
        float top = 1.0 - smoothstep(tip - 0.13, tip, h);
        alpha = strands * top * footFade * (0.25 + rim * 0.65) * (0.2 + rising * 0.8);
        color = mix(MainColor, HighlightColor, pow(flow, 5.0));
    } else {
        float tip = 0.30 + 0.68 * pow(flow, 0.65);
        float top = 1.0 - smoothstep(tip - 0.12, tip, h);
        // No continuous opacity floor: negative space breaks up the solid-cylinder look.
        float tongues = smoothstep(0.33 + h * 0.22, 0.9, flow);
        float body = tongues * (0.16 + rim * 0.24);
        alpha = body * top * footFade * (0.45 + rising * 0.55);
        color = mix(MainColor * vec3(1.0, 0.26, 0.25), MainColor, flow);
    }
    float fog = 1.0 - smoothstep(FogStart, max(FogStart + 0.001, FogEnd), vertexDistance);
    alpha *= vertexColor.a * AuraIntensity * ColorModulator.a * fog;
    if (alpha < 0.002) discard;
    fragColor = vec4(color * ColorModulator.rgb * (1.0 + 0.2 * max(0.0, AuraIntensity - 2.0)), min(alpha, 0.95));
}
