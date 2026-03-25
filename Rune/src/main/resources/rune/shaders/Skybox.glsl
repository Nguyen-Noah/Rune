#type vertex
#version 450 core

layout(location = 0) in vec3 a_Position;
layout(location = 1) in vec2 a_TexCoords;

layout(location = 0) out vec3 v_Position;

layout(std140, binding = 0) uniform Camera {
    mat4 u_ViewProjection;
    mat4 u_SkyProjection;
};

void main() {
    vec4 position = vec4(a_Position.xy, 0.0, 1.0);
    gl_Position = position;

    v_Position = (u_SkyProjection * position).xyz;
}

#type fragment
#version 450 core

layout(location = 0) in vec3 v_TexCoords;

layout(location = 0) out vec4 o_Color;

layout(binding = 0) uniform samplerCube u_Texture;

void main() {
    o_Color = texture(u_Texture, v_TexCoords);
    //o_Color -= 0.3;
    o_Color.a = 1.0f;
}