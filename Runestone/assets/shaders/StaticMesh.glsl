#type vertex
#version 450 core

layout(location = 0) in vec3 a_Position;
layout(location = 1) in vec3 a_Normal;
layout(location = 2) in vec2 a_TexCoords;
//layout(location = 3) in int a_EntityID;

layout(location = 0) out vec2 f_TexCoords;
//layout(location = 1) out flat int f_EntityID;

layout(std140, binding = 0) uniform Camera
{
    mat4 u_ViewProjection;
};

void main()
{
    f_TexCoords = a_TexCoords;
    //f_EntityID = a_EntityID;
    gl_Position = u_ViewProjection * vec4(a_Position, 1.0);
}

#type fragment
#version 450 core

layout(location = 0) in vec2 f_TexCoords;
//layout(location = 1) in flat int f_EntityID;

layout(location = 0) out vec4 o_Color;
//layout(location = 1) out flat int o_EntityID;

layout(binding = 0) uniform sampler2D u_Texture;

void main()
{
    o_Color = texture(u_Texture, f_TexCoords);

    //o_EntityID = f_EntityID;
}