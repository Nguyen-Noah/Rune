#type vertex
#version 450 core

layout(std140, binding = 0) uniform Camera
{
    mat4 u_ViewProjection;
};

const vec3 Pos[4] = vec3[4](
    vec3(-1.0, 0.0, -1.0),  // bottom left
    vec3( 1.0, 0.0, -1.0),  // bottom right
    vec3( 1.0, 0.0,  1.0),  // top right
    vec3(-1.0, 0.0,  1.0)   // top left
);

const int Indices[6] = int[6](0, 2, 1, 2, 0, 3);

void main() {
    int Index = Indices[gl_VertexIndex];
    vec4 vPos = vec4(Pos[Index], 1.0);
    gl_Position = u_ViewProjection * vPos;
}

#type fragment
#version 450 core

layout(location = 0) out vec4 o_Color;

void main() {
    o_Color = vec4(0.0);
}