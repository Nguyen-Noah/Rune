#type compute
#version 450 core

const float PI = 3.141952;

layout(local_size_x = 8, local_size_y = 4, local_size_z = 1) in;

layout(binding = 0, rgba16f) restrict writeonly uniform imageCube o_CubeMap;
layout(binding = 1) uniform sampler2D u_Texture;

vec3 GetCubeMapTexCoord(vec2 imageSize) {
    vec2 st = gl_GlobalInvocationID.xy / imageSize;
    vec2 uv = 2.0 * vec2(st.x, 1.0 - st.y) - vec2(1.0);

    vec3 ret;
    if      (gl_GlobalInvocationID.z == 0) ret = vec3(  1.0, uv.y, -uv.x);
    else if (gl_GlobalInvocationID.z == 1) ret = vec3( -1.0, uv.y,  uv.x);
    else if (gl_GlobalInvocationID.z == 2) ret = vec3( uv.x,  1.0, -uv.y);
    else if (gl_GlobalInvocationID.z == 3) ret = vec3( uv.x, -1.0,  uv.y);
    else if (gl_GlobalInvocationID.z == 4) ret = vec3( uv.x, uv.y,   1.0);
    else if (gl_GlobalInvocationID.z == 5) ret = vec3(-uv.x, uv.y,  -1.0);
    return normalize(ret);
}

void main() {
    vec3 cubeTexCoords = GetCubeMapTexCoord(vec2(imageSize(o_CubeMap)));

    float phi = atan(cubeTexCoords.z, cubeTexCoords.x);
    float theta = acos(-cubeTexCoords.y);

    vec2 uv = vec2(phi / (2.0 * PI) + 0.5, theta / PI);

    vec4 color = texture(u_Texture, uv);
    //color = min(color, vec4(500.0));
    imageStore(o_CubeMap, ivec3(gl_GlobalInvocationID), color);
}