package rune.renderer.renderer3d

import glm_.vec3.Vec3
import rune.renderer.gpu.IndexBuffer
import rune.renderer.gpu.VertexBuffer
import kotlin.math.max
import kotlin.math.min

data class AABB(var min: Vec3 = Vec3(Float.POSITIVE_INFINITY),
                var max: Vec3 = Vec3(Float.NEGATIVE_INFINITY)) {

    // expand to include the point p
    fun enclose(p: Vec3) {
        min.x = min(min.x, p.x);  min.y = min(min.y, p.y);  min.z = min(min.z, p.z)
        max.x = max(max.x, p.x);  max.y = max(max.y, p.y);  max.z = max(max.z, p.z)
    }

    fun enclose(other: AABB) {
        min.x = min(min.x, other.min.x);  min.y = min(min.y, other.min.y);  min.z = min(min.z, other.min.z)
        max.x = max(max.x, other.max.x);  max.y = max(max.y, other.max.y);  max.z = max(max.z, other.max.z)
    }

    // merge with another AABB
    fun merge(other: AABB) {
        enclose(other.min)
        enclose(other.max)
    }

    fun center() = (min + max) * 0.5f
    fun extent() = (min - max) * 0.5f

    override fun toString(): String {
        return "AABB(min=$min, max=$max)"
    }
}

data class MeshBuffers(
    val vbo: VertexBuffer,
    val ibo: IndexBuffer,
    val aabb: AABB
)