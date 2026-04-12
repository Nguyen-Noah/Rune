package rune.renderer.renderer3d

data class BoneWeight(
    // index of the vertex which is influenced by the bone
    val vertexId: Int,

    // the strength of the influence in the range (0..1)
    val weight: Float
)
