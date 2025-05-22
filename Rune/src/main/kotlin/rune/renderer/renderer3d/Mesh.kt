package rune.renderer.renderer3d

import rune.asset.Material
import rune.renderer.gpu.VertexArray

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

class Mesh(
    val name: String,
    val buffers: MeshBuffers,
    val subMeshes: List<SubMesh>
) {
    override fun toString(): String {
        return buildString {
            appendLine("Mesh(name=$name")
            subMeshes.forEach { appendLine("  $it") }
            append(" ])")
        }
    }
}

// TODO: link this model via UUID to serialize
class Model(val mesh: Mesh, val vao: VertexArray) {
    override fun toString(): String {
        return buildString {
            appendLine("Model(meshes=[")
            appendLine("  $mesh")
            append("])")
        }
    }
}