#type vertex
#version 450 core

layout(location = 0) in vec3 a_Position;
layout(location = 1) in vec3 a_Normal;
layout(location = 2) in vec2 a_TexCoords;
//layout(location = 3) in int a_EntityID;

layout(location = 0) out vec2 v_TexCoords;
layout(location = 1) out vec3 v_Normal;
//layout(location = 1) out flat int f_EntityID;

layout(std140, binding = 0) uniform Camera {
    mat4 u_ViewProjection;
};

layout(std140, binding = 1) uniform Transform {
    mat4 u_ModelTransform;
};

void main() {
    v_TexCoords = a_TexCoords;
    v_Normal = normalize(transpose(inverse(mat3(u_ModelTransform))) * a_Normal);
    //v_Normal = a_Normal;

    //f_EntityID = a_EntityID;
    gl_Position = u_ViewProjection * u_ModelTransform * vec4(a_Position, 1.0);
}

#type fragment
#version 450 core

layout(location = 0) in vec2 v_TexCoords;
layout(location = 1) in vec3 v_Normal;
//layout(location = 1) in flat int f_EntityID;

layout(location = 0) out vec4 o_Color;
//layout(location = 1) out flat int o_EntityID;


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

// Phong lighting
layout(set = 0, binding = 0) uniform sampler2D u_AlbedoTexture;
layout(set = 0, binding = 1) uniform sampler2D u_NormalTexture;
layout(set = 0, binding = 2) uniform sampler2D u_SpecularTexture;

void main() {
    vec4 ambientColor = vec4(u_DirLight.dirLight.color, 1.0) *
                             m_Params.mat.Albedo *
                             vec4(m_Params.mat.Diffuse.rgb, 1.0);

    float diffuseFactor = dot(normalize(v_Normal), -normalize(u_DirLight.dirLight.direction));

    vec4 diffuseColor = vec4(0, 0, 0, 0);

    if (diffuseFactor > 0.0) {
        diffuseColor = vec4(u_DirLight.dirLight.color, 1.0)
        * u_DirLight.dirLight.diffuseIntensity
        * vec4(m_Params.mat.Diffuse.rgb, 1.0)
        * diffuseFactor;
    }

    //o_Color = vec4(vec3(diffuseFactor), 1.0);
    o_Color = texture(u_AlbedoTexture, v_TexCoords) * (ambientColor + diffuseColor);
    //o_Color = vec4(vec3(u_DirLight.dirLight.diffuseIntensity), 1.0);

    //o_EntityID = f_EntityID;
}