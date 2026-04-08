
#ifndef SKYBOX_GLSL
#define SKYBOX_GLSL

layout(binding = 4) uniform samplerCube u_Texture;

vec3 sampleSkybox(vec3 direction) {
    return texture(u_Texture, normalize(direction)).rgb;
}

#endif