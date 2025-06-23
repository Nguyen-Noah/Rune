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

    //f_EntityID = a_EntityID;
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
    // store the fragment position in the first gBuffer texture
    o_Position = v_Position;
    // store the normal per-fragment in the next gBuffer
    vec3 n = normalize(v_Normal);
    o_Normal = n * 0.5 + 0.5;
    // store both the albedo texture and the specular into a single rgba buffer
    o_AlbedoSpec.rgb = texture(u_Albedo, v_TexCoords).rgb;
    o_AlbedoSpec.a = texture(u_Specular, v_TexCoords).r;
}