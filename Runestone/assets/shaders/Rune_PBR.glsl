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

void main() {
    vec3 fragPos = texture(u_PositionTex, v_TexCoords).xyz;
    //vec3 normal = normalize(texture(u_NormalTex, v_TexCoords).xyz);
    vec3 n = texture(u_NormalTex, v_TexCoords).xyz * 2.0 - 1.0;
    vec3 normal = normalize(n);
    vec3 albedo = texture(u_AlbedoSpecTex, v_TexCoords).rgb;
    float specMask = texture(u_AlbedoSpecTex, v_TexCoords).a;

    vec3 ambientColor = u_DirLight.dirLight.color * m_Params.mat.Diffuse.rgb * albedo;

    vec3 L = normalize(-u_DirLight.dirLight.direction);
    float NdotL = max(dot(normal, L), 0.0);
    vec3 diffuse = u_DirLight.dirLight.color * u_DirLight.dirLight.diffuseIntensity * albedo * NdotL;

    o_Color = vec4(ambientColor + diffuse, 1.0);
    float d = texture(u_DepthTex, v_TexCoords).r;
    if (d >= 0.9999) {
        vec3 c = texture(u_Texture, v_SkyboxPos).rgb;
        o_Color = vec4(aces_tonemap(c), 1.0);
    }
}