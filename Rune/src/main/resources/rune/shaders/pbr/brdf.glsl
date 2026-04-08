
#ifndef BRDF_GLSL
#define BRDF_GLSL

vec3 calcAmbient(vec3 lightColor, vec3 diffuseMat, vec3 albedo) {
    return lightColor * diffuseMat * albedo;
}

vec3 calcDiffuse(vec3 lightColor, float intensity, vec3 albedo, float diffuseFactor) {
    return lightColor * intensity * albedo * max(diffuseFactor, 0.0);
}

float calcSpecular(vec3 viewPos, vec3 normal, vec3 lightVector) {
    float specularIntensity = 32;
    return pow(max(dot(normal, lightVector), 0.0), specularIntensity);
}

#endif