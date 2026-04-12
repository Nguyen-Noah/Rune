package rune.renderer.renderer3d

import glm_.mat4x4.Mat4

data class Bone(
    // the name of the bone
    val name: String,

    // matrix that transforms from mesh space to bone space in bind pose
    val offsetMatrix: Mat4,

    // the vertices affected by this bone
    val weights: List<BoneWeight>
)
