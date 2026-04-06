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
    o_Color = vec4(sceneLinear, 1.0);
}