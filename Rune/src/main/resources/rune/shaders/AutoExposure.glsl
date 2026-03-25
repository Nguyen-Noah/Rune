#type compute
#version 450 core

layout(local_size_x = 16, local_size_y = 16) in;

layout(binding = 0, rgba16f) readonly uniform image2D u_Scene;
layout(std430, binding = 1) buffer Histogram { uint bins[256]; };

void main() {
    ivec2 id = ivec2(gl_GlobalInvocationID.xy);
    ivec2 size = imageSize(u_Scene);

    if (id.x >= size.x || id.y >= size.y)
        return;

    vec3 hdr = imageLoad(u_Scene, id).rgb;
    float Y = dot(hdr, vec3(0.2126, 0.7152, 0.0722));
    Y = max(Y, 1e-4);
    float logL = log2(Y);       // range roughly [-12, 12]
    int idx = int(clamp((logL + 12.0) * 10.666, 0.0, 255.0));
    atomicAdd(bins[idx], 1u);
}