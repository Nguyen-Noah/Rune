#type vertex
#version 450 core

#include "common.glsl"

layout(location = 0) in vec3 a_Position;
layout(location = 1) in vec3 a_Normal;
layout(location = 2) in vec3 a_Bitangent;
layout(location = 3) in vec3 a_Tangent;
layout(location = 4) in vec2 a_TexCoords0;
layout(location = 5) in vec2 a_TexCoords1;

layout(std140, binding = 1) uniform Transform {
    mat4 u_ModelTransform;
};

layout(std140, binding = 6) uniform GeometryUvSets {
    int u_AlbedoUvSet;
    int u_NormalUvSet;
    int u_SpecularUvSet;
} u_GeometryUv;

// TODO: this should be a struct
layout(location = 0) out vec2 v_UvAlbedo;
layout(location = 1) out vec2 v_UvSpecular;
layout(location = 2) out vec2 v_UvNormal;
layout(location = 3) out vec3 v_Normal;
layout(location = 4) out vec3 v_Tangent;
layout(location = 5) out vec3 v_Bitangent;
layout(location = 6) out vec3 v_Position;

// picks between 2 UVs given u_GeometryUv
vec2 pickUv(int set, vec2 u0, vec2 u1) {
    return mix(u0, u1, float(clamp(set, 0, 1)));
}

void main() {
    mat3 normalMatrix = transpose(inverse(mat3(u_ModelTransform)));

    v_UvAlbedo = pickUv(u_GeometryUv.u_AlbedoUvSet, a_TexCoords0, a_TexCoords1);
    v_UvSpecular = pickUv(u_GeometryUv.u_SpecularUvSet, a_TexCoords0, a_TexCoords1);
    v_UvNormal = pickUv(u_GeometryUv.u_NormalUvSet, a_TexCoords0, a_TexCoords1);

    v_Normal = normalize(normalMatrix * a_Normal);
    v_Tangent = normalize(normalMatrix * a_Tangent);
    v_Bitangent = normalize(normalMatrix * a_Bitangent);
    v_Position = (u_ModelTransform * vec4(a_Position, 1.0)).xyz;

    gl_Position = u_Camera.u_ViewProjection * u_ModelTransform * vec4(a_Position, 1.0);
}

#type fragment
#version 450 core

layout(location = 0) out vec4 o_Position;
layout(location = 1) out vec4 o_Normal;
layout(location = 2) out vec4 o_AlbedoSpec;

layout(location = 0) in vec2 v_UvAlbedo;
layout(location = 1) in vec2 v_UvSpecular;
layout(location = 2) in vec2 v_UvNormal;
layout(location = 3) in vec3 v_Normal;
layout(location = 4) in vec3 v_Tangent;
layout(location = 5) in vec3 v_Bitangent;
layout(location = 6) in vec3 v_Position;

layout(binding = 0) uniform sampler2D u_Albedo;
layout(binding = 1) uniform sampler2D u_Normal;
layout(binding = 2) uniform sampler2D u_Specular;

void main() {
    o_Position = vec4(v_Position, 1.0);

    // normal mapping
    vec3 N = normalize(v_Normal);
    vec3 B = normalize(v_Bitangent);
    vec3 T = normalize(v_Tangent);
    mat3 TBN = mat3(T, B, N);

    vec3 tangentNormal = texture(u_Normal, v_UvNormal).rgb * 2.0 - 1.0;
    N = normalize(TBN * tangentNormal);
    o_Normal = vec4(N * 0.5 + 0.5, 1.0);

//    vec3 n = normalize(v_Normal);
//    o_Normal = vec4(n * 0.5 + 0.5, 1.0);

    o_AlbedoSpec.rgb = texture(u_Albedo, v_UvAlbedo).rgb;
    o_AlbedoSpec.a = texture(u_Specular, v_UvSpecular).r;
}
