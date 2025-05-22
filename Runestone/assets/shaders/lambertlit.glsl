#type vertex
#version 450 core

layout(location = 0) in vec3 a_Position;
layout(location = 1) in vec3 a_Normal;

/* ── Camera (once per frame) ────────────────────────── */
layout(std140, binding = 0) uniform Camera
{
    mat4 u_ViewProjection;
};

/* ── Model (once per draw) ──────────────────────────── */
layout(std140, binding = 1) uniform Model
{
    mat4 u_Model;
    mat4 u_NormalMatrix;   // mat4 is std140-friendly; use mat3(u_NormalMatrix) in code
};

/* ── outputs to fragment stage ──────────────────────── */
layout(location = 0) out vec3 v_Normal;

void main()
{
    v_Normal    = mat3(u_NormalMatrix) * a_Normal;
    gl_Position = u_ViewProjection * u_Model * vec4(a_Position, 1.0);
}

#type fragment
#version 450 core

layout(location = 0) in  vec3 v_Normal;
layout(location = 0) out vec4 o_Color;

/* ── Material (per draw / per material) ─────────────── */
layout(std140, binding = 2) uniform Material
{
    vec4 u_Color;     // vec3 padded to vec4 automatically
    vec4 u_LightDir;  // ditto
};

void main()
{
    vec3 N = normalize(v_Normal);
    vec3 L = normalize(-u_LightDir.xyz);
    float NdotL = max(dot(N, L), 0.0);

    vec3 diffuse = u_Color.rgb * NdotL;
    vec3 ambient = u_Color.rgb * 0.10;

    o_Color = vec4(diffuse + ambient, 1.0);
}