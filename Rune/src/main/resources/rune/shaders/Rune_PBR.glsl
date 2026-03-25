#type vertex
#version 450 core

layout(location = 0) in vec3 a_Position;
layout(location = 1) in vec2 a_TexCoords;

layout(location = 0) out vec2 v_TexCoords;
layout(location = 1) out vec3 v_SkyboxPos;

layout(std140, binding = 0) uniform Camera {
    mat4 u_ViewProjection;
    mat4 u_SkyProjection;
};

void main() {
    v_TexCoords = a_TexCoords;
    gl_Position = vec4(a_Position, 1.0);

    v_SkyboxPos = (u_SkyProjection * vec4(a_Position.xy, 0.0, 1.0)).xyz;
}

#type fragment
#version 450 core

layout(location = 0) out vec4 o_Color;

layout(location = 0) in vec2 v_TexCoords;
layout(location = 1) in vec3 v_SkyboxPos;

layout(binding = 0) uniform sampler2D u_PositionTex;
layout(binding = 1) uniform sampler2D u_NormalTex;
layout(binding = 2) uniform sampler2D u_AlbedoSpecTex;
layout(binding = 3) uniform sampler2D u_DepthTex;

layout(binding = 4) uniform samplerCube u_Texture;


struct DirectionalLight {
    vec3 color;
    float diffuseIntensity;
    vec3 direction;
};
layout(std140, binding = 2) uniform DirectionalLights {
    DirectionalLight dirLight;
} u_DirLight;

struct Material {
    vec4 Albedo;
    vec4 Diffuse;
    vec4 Specular;
};
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

    int _pad0;
    int _pad1;
} u_Settings;

vec3 aces_tonemap(vec3 color) {
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

vec3 applyTonemap(vec3 color, int tm, float exposure) {
    color *= exposure;
    if (tm == 1) return aces_tonemap(color);
    if (tm == 2) return color / (1.0 + color);
    return color;
}

vec3 maybeBloom(vec3 color, vec3 bloom, float bloomIntensity, bool on) {
    return mix(color, color + bloomIntensity * bloom, on ? 1.0 : 0.0);
}

void main() {
    vec3 fragPos = texture(u_PositionTex, v_TexCoords).xyz;
    vec3 normal = normalize(texture(u_NormalTex, v_TexCoords).xyz * 2.0 - 1.0);
    vec3 albedo = texture(u_AlbedoSpecTex, v_TexCoords).rgb;
    float specMask = texture(u_AlbedoSpecTex, v_TexCoords).a;
    float d = texture(u_DepthTex, v_TexCoords).r;

    vec3 ambientColor = u_DirLight.dirLight.color * m_Params.mat.Diffuse.rgb * albedo;

    vec3 L = normalize(-u_DirLight.dirLight.direction);
    float NdotL = max(dot(normal, L), 0.0);
    vec3 diffuse = u_DirLight.dirLight.color * u_DirLight.dirLight.diffuseIntensity * albedo * NdotL;

    // sky sample
    vec3 skyDir = normalize(v_SkyboxPos);
    vec3 skyColor = texture(u_Texture, skyDir).rgb;

    float hasGeom = step(d, 0.9999);
    vec3 sceneLinear = mix(skyColor, ambientColor + diffuse, hasGeom);

    vec3 mapped = aces_tonemap(sceneLinear);
    vec3 gamma = pow(mapped, vec3(1.0/2.22));
//    if (u_Settings.toneMapper == 1) {
//        o_Color = vec4(gamma, 1.0);
//    } else {
//        o_Color = vec4(sceneLinear, 1.0);
//    }
    o_Color = vec4(applyTonemap(sceneLinear, u_Settings.toneMapper, u_Settings.exposure), 1.0);
    //o_Color = vec4(aces_tonemap(sceneLinear), 1.0);
}