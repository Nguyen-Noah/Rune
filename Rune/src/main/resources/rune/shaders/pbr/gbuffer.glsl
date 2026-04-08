/**
*   Holds information for the geometry buffer in the deferred renderer
*/

#ifndef GBUFFER_GLSL
#define GBUFFER_GLSL

layout(binding = 0) uniform sampler2D u_PositionTex;
layout(binding = 1) uniform sampler2D u_NormalTex;
layout(binding = 2) uniform sampler2D u_AlbedoSpecTex;
layout(binding = 3) uniform sampler2D u_DepthTex;

struct GBufferData {
    vec3 position;
    vec3 normal;
    vec3 albedo;
    float specMask;
    float depth;
};

GBufferData sampleGBuffer(vec2 uv) {
    GBufferData g;
    g.position = texture(u_PositionTex, uv).xyz;
    g.normal = normalize(texture(u_NormalTex, uv).xyz * 2.0 - 1.0);
    vec4 albedoSpecular = texture(u_AlbedoSpecTex, uv);
    g.albedo = albedoSpecular.rgb;
    g.specMask = albedoSpecular.a;
    g.depth = texture(u_DepthTex, uv).r;
    return g;
}

#endif