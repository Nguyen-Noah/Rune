#type vertex
#version 450 core

layout(location = 0) in vec3 a_Position;
layout(location = 1) in vec2 a_TexCoords;

layout(location = 0) out vec2 v_TexCoords;

void main() {
    v_TexCoords = a_TexCoords;
    gl_Position = vec4(a_Position, 1.0);
}

#type fragment
#version 450 core

layout(binding = 0) uniform sampler2D v_hdrTex;

layout(location = 0) in vec2 v_TexCoords;

layout(location = 0) out vec4 o_Color;

layout(std140, binding = 7) uniform RendererSettings {
    int aaMethod;   // 0=None, 1=FXAA, 2=TAA
    int toneMapper; // 0=Off, 1=ACES, 2=Reinhard

    float exposure;
    float gamma;
    float bloomIntensity;
    float vignetteStrength;

    int _pad0;
    int _pad1;
} u_Settings;

vec3 reinhard(vec3 v) {
    return v / (1.0f + v);
}

vec3 reinhard_extended(vec3 v, float max_white) {
    vec3 numerator = v * (1.0f + (v / vec3(max_white * max_white)));
    return numerator / (1.0f + v);
}

vec3 aces(vec3 color) {
    mat3 m1 = mat3(
        0.59719, 0.07600, 0.02840,
        0.35458, 0.90834, 0.13383,
        0.04823, 0.01566, 0.83777
    );
    mat3 m2 = mat3(
        1.60475, -0.10208, -0.00327,
        -0.53108,  1.10813, -0.07276,
        -0.07367, -0.00605,  1.07602
    );

    vec3 v = m1 * color;
    vec3 a = v * (v + 0.0245786) - 0.000090537;
    vec3 b = v * (0.983729 * v + 0.4329510) + 0.238081;
    return pow(clamp(m2 * (a / b), 0.0, 1.0), vec3(1.0 / 2.2));
}

void main() {
    vec3 c = texture(v_hdrTex, v_TexCoords).rgb;
    o_Color = vec4(clamp(c, 0.0, 1.0), 1.0);
    o_Color = vec4(aces(c), 1.0);
}
