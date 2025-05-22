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
    var texCoords: Vec2 = Vec2(0f)
)