#type vertex
#version 450 core

#include "common.glsl"

layout(location = 0) in vec3 a_Position;
layout(location = 1) in vec2 a_TexCoords;

layout(location = 0) out vec2 v_TexCoords;
layout(location = 1) out vec3 v_SkyboxPos;

void main() {
    v_TexCoords = a_TexCoords;
    gl_Position = vec4(a_Position, 1.0);

    v_SkyboxPos = (u_Camera.u_SkyProjection * vec4(a_Position.xy, 0.0, 1.0)).xyz;
}

#type fragment
#version 450 core

#include "common.glsl"
#include "brdf.glsl"
#include "gbuffer.glsl"
#include "skybox.glsl"

layout(location = 0) out vec4 o_Color;

layout(location = 0) in vec2 v_TexCoords;
layout(location = 1) in vec3 v_SkyboxPos;

void main() {
    int toonColorLevels = 1;
    float toonScaleFactor = 1.0f / toonColorLevels;

    GBufferData g = sampleGBuffer(v_TexCoords);
    DirectionalLight dLight = u_DirLight.dirLight;

    float diffuseFactor = dot(g.normal, normalize(-dLight.direction));
    if (u_Settings.enableCelShading == 1) {
        diffuseFactor = ceil(diffuseFactor * toonColorLevels) * toonScaleFactor;
    }

    vec3 ambient = calcAmbient(dLight.color, m_Params.mat.Diffuse.rgb, g.albedo);
    vec3 diffuse = calcDiffuse(dLight.color, dLight.diffuseIntensity, g.albedo, diffuseFactor);
    float specular = calcSpecular(u_Camera.u_CameraPos, g.normal, normalize(-dLight.direction));

    vec4 finalColor = vec4(ambient + diffuse, 1.0);
    if (u_Settings.enableCelShading == 0) {
        finalColor.rgb += specular;
    } else {
        float rimThreshold = 0.6;  // how wide the rim is
        float rimAmount = 0.3;     // how bright

        vec3 viewDirection = normalize(u_Camera.u_CameraPos - g.position);
        float rim = 1.0 - max(dot(g.normal, viewDirection), 0.0);

        // optionally mask rim by light facing — avoids rim on the dark side
        float NdotL = max(dot(g.normal, normalize(-dLight.direction)), 0.0);
        rim *= ceil(NdotL * toonColorLevels) * toonScaleFactor;

        vec3 rimColor = dLight.color * 0.3; // or a custom uniform
        finalColor.rgb += rim * rimColor;
    }

    // sky sample
    vec3 skyColor = sampleSkybox(v_SkyboxPos);

    float hasGeom = step(g.depth, 0.9999);
    vec3 sceneLinear = mix(skyColor, finalColor.rgb, hasGeom);
    o_Color = vec4(sceneLinear, 1.0);
}