package rune.renderer.renderer3d.mesh

import glm_.vec2.Vec2
import glm_.vec3.Vec3

//data class Vertex(
//    var position: Vec3 = Vec3(0f),
//    var normal: Vec3 = Vec3(0f),
//    var tangent: Vec3 = Vec3(0f),
//    var binornmal: Vec3 = Vec3(0f),
//    var texCoord: Vec2 = Vec2(0f)
//)
data class Vertex(
    var position: Vec3 = Vec3(0f),
    var normal: Vec3 = Vec3(0f),
    var bitangent: Vec3 = Vec3(0f),
    var tangent: Vec3 = Vec3(0f),
    var texCoords: Vec2 = Vec2(0f),
    /** Second UV set; [Vertex] `(pos, normal, uv)` sets this equal to the first UV. */
    var texCoords1: Vec2 = Vec2(0f),
) {
    constructor(position: Vec3, normal: Vec3, bitangent: Vec3, tangent: Vec3, uv: Vec2) : this(position, normal, bitangent, tangent,  uv, uv)
}
