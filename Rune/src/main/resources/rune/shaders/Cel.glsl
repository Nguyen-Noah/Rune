
#ifndef CEL_GLSL
#define CEL_GLSL

#include "common.glsl"

float calcRimLightFactor(vec3 pixelToCamera, vec3 normal) {
    float rimFactor = dot(pixelToCamera, normal);
    rimFactor = 1.0 - rimFactor;
    rimFactor = max(0.0, rimFactor);
    rimFactor = pow(rimFactor, 4.0);
    return rimFactor;
}

#endif
