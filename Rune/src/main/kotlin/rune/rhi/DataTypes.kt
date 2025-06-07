package rune.rhi

import glm_.vec4.Vec4


//! ----------------------------------------------------------------------------
data class RasterState(
    val cullMode: CullMode = CullMode.BACK,
    val frontFace: FrontFace = FrontFace.CCW
)

data class DepthState(
    val test: Boolean = true,
    val write: Boolean = true,
    val compare: CompareOp = CompareOp.LESS
)

data class BlendAttachment(
    val enable: Boolean = false,
    val src: BlendFactor = BlendFactor.SRC_ALPHA,
    val dst: BlendFactor = BlendFactor.ONE_MINUS_SRC_ALPHA,
    val op: BlendOp = BlendOp.ADD
)

//* ------ vertex-input description
data class VertexInputBinding(val stride: Int, val rate: InputRate = InputRate.Vertex)
data class VertexAttributeDesc(
    val location: Int,
    val binding: Int,
    val format: AttributeFormat,
    val offset: Int
)

data class ClearValues(
    val colors: List<Vec4> = emptyList(),
    val depth: Float? = null,
    val stencil: Int? = null
)









