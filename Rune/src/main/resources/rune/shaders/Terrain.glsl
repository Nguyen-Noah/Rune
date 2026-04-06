#type vertex
#version 450 core

layout(location = 0) in vec3 a_Position;
layout(location = 1) in vec3 a_Normal;
layout(location = 2) in vec2 a_TexCoords;

layout(std140, binding = 0) uniform Camera {
    mat4 u_ViewProjection;
    mat4 u_SkyboxProjection;
};

layout(std140, binding = 1) uniform Transform {
    mat4 u_ModelTransform;
};

layout(location = 0) out vec2 v_TexCoords;
layout(location = 1) out vec3 v_Normal;
layout(location = 2) out vec3 v_Position;

void main() {
    v_TexCoords = a_TexCoords;
    v_Normal = normalize(transpose(inverse(mat3(u_ModelTransform))) * a_Normal);
    v_Position = a_Position;
    gl_Position = u_ViewProjection * u_ModelTransform * vec4(a_Position, 1.0);
}

#type fragment
#version 450 core

layout(location = 0) out vec3 o_Position;
layout(location = 1) out vec3 o_Normal;
layout(location = 2) out vec4 o_AlbedoSpec;

layout(location = 0) in vec2 v_TexCoords;
layout(location = 1) in vec3 v_Normal;
layout(location = 2) in vec3 v_Position;

layout(binding = 0) uniform sampler2D u_Albedo;
layout(binding = 1) uniform sampler2D u_Normal;
layout(binding = 2) uniform sampler2D u_Specular;

void main() {
    o_Position = v_Position;
    vec3 n = normalize(v_Normal);
    o_Normal = n * 0.5 + 0.5;

    vec3 albedoSample = texture(u_Albedo, v_TexCoords).rgb;
    float elevation = v_Position.y;
    float t = clamp(elevation * 0.08 + 0.35, 0.0, 1.0);
    vec3 lowland = vec3(0.12, 0.38, 0.14);
    vec3 highland = vec3(0.42, 0.36, 0.28);
    vec3 terrainTint = mix(lowland, highland, t);
    o_AlbedoSpec.rgb = albedoSample * terrainTint;
    o_AlbedoSpec.a = texture(u_Specular, v_TexCoords).r;
}
