/**
*   Common data structures for the Rune Engine
*
*/

#ifndef COMMON_GLSL
#define COMMON_GLSL

struct DirectionalLight {
    vec3 color;
    float diffuseIntensity;
    vec3 direction;
};

struct Material {
    vec4 Albedo;
    vec4 Diffuse;
    vec4 Specular;
};

layout(std140, binding = 0) uniform Camera {
    mat4 u_ViewProjection;
    mat4 u_SkyProjection;
    vec3 u_CameraPos;
} u_Camera;

layout(std140, binding = 2) uniform DirectionalLights {
    DirectionalLight dirLight;
} u_DirLight;

layout(std140, binding = 5) uniform PBRMaterial {
    Material mat;
} m_Params;

layout(std140, binding = 7) uniform RendererSettings {
    int aaMethod;   // 0=None, 1=FXAA, 2=TAA
    int toneMapper; // 0=Off, 1=ACES, 2=Reinhard

    float exposure;
    float gamma;
    float bloomIntensity;
    float vignetteStrength;

    bool enableCelShading;
    float specularBandWidth;
    float rimWidth;
    float rimIntensity;
    float specularColorIntensity;
    float rimColorIntensity;
} u_Settings;

#endif