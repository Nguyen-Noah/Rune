package rune.renderer.renderer3d

import rune.asset.Material

enum class MeshType { STATIC, ANIMATED, PROCEDURAL }

data class SubMesh(
    val indexOffset: Int,
    val indexCount: Int,
    val material: Material,
    val localAABB: AABB
) {
    override fun toString(): String {
        return "  SubMesh(offset=$indexOffset, count=$indexCount, material=$material, aabb=$localAABB),"
    }
}

sealed class Mesh {
    abstract val name: String
    abstract val buffers: MeshBuffers
    abstract val subMeshes: List<SubMesh>

    data class Static(
        override val name: String,
        override val buffers: MeshBuffers,
        override val subMeshes: List<SubMesh>
    ) : Mesh()

    data class Animated(
        override val name: String,
        override val buffers: MeshBuffers,
        override val subMeshes: List<SubMesh>,
        val skeleton: List<Bone>,
        //val animations: List<AnimationClip>
    ) : Mesh()
}

// TODO: link this model via UUID to serialize
class Model(val mesh: Mesh) {
    override fun toString(): String {
        return buildString {
            appendLine("Model(meshes=[")
            appendLine("  $mesh")
            append("])")
        }
    }
}