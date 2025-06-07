package rune.renderer.gpu

import org.lwjgl.opengl.GL45.*
import rune.rhi.*

data class VertexAttribute(
    val name: String,
    val count: Int,
    val type: Int = GL_FLOAT,
    val normalized: Boolean = false
)
class VertexBufferLayout {
    private val attributes = mutableListOf<VertexAttribute>()
    val stride: Int
        get() = attributes.sumOf { it.count * 4 }

    fun attribute(name: String, count: Int, type: Int = GL_FLOAT, normalized: Boolean = false) {
        attributes.add(VertexAttribute(name, count, type, normalized))
    }

    fun computeOffsets(): List<Triple<Int, Int, VertexAttribute>> {
        var offset = 0
        return attributes.mapIndexed { index, attr ->
            val triple = Triple(index, offset, attr)
            offset += attr.count * 4
            triple
        }
    }

    fun toRhi(binding: Int = 0)
            : Pair<List<VertexInputBinding>, List<VertexAttributeDesc>> {

        /* one binding that advances per-vertex */
        val bindingDesc = VertexInputBinding(stride, InputRate.Vertex)

        /* helper: choose AttributeFormat from component count + scalar kind */
        fun chooseFormat(count: Int, scalar: ScalarType): AttributeFormat = when (scalar) {
            ScalarType.FLOAT  -> when (count) {
                1 -> AttributeFormat.R32_SFLOAT
                2 -> AttributeFormat.R32G32_SFLOAT
                3 -> AttributeFormat.R32G32B32_SFLOAT
                4 -> AttributeFormat.R32G32B32A32_SFLOAT
                else -> error("Unsupported vec size $count for float")
            }
            ScalarType.HALF   -> when (count) {
                1 -> AttributeFormat.R16_SFLOAT
                2 -> AttributeFormat.R16G16_SFLOAT
                else -> error("Unsupported vec size $count for half-float")
            }
            ScalarType.DOUBLE -> when (count) {
                1 -> AttributeFormat.R64_SFLOAT
                2 -> AttributeFormat.R64G64_SFLOAT
                3 -> AttributeFormat.R64G64B64_SFLOAT
                4 -> AttributeFormat.R64G64B64A64_SFLOAT
                else -> error("Unsupported vec size $count for double")
            }
            ScalarType.INT    -> when (count) {
                1 -> AttributeFormat.R32_SINT
                2 -> AttributeFormat.R32G32_SINT
//                3 -> AttributeFormat.R32G32B32_SINT
//                4 -> AttributeFormat.R32G32B32A32_SINT
                else -> error("Unsupported vec size $count for int")
            }
            ScalarType.UINT   -> when (count) {
                1 -> AttributeFormat.R32_UINT
                else -> error("Unsupported vec size $count for uint")
            }
            ScalarType.SHORT  -> AttributeFormat.R16_SINT
            ScalarType.USHORT -> AttributeFormat.R16_UINT
            ScalarType.BYTE   -> AttributeFormat.R8_SINT
            ScalarType.UBYTE  -> AttributeFormat.R8_UINT
        }

        val attribs = computeOffsets().map { (loc, offset, attr) ->

            /* map the GL constant carried by the DSL to a neutral ScalarType */
            val scalar = when (attr.type) {
                GL_FLOAT            -> ScalarType.FLOAT
                GL_HALF_FLOAT       -> ScalarType.HALF
                GL_DOUBLE           -> ScalarType.DOUBLE
                GL_INT              -> ScalarType.INT
                GL_UNSIGNED_INT     -> ScalarType.UINT
                GL_SHORT            -> ScalarType.SHORT
                GL_UNSIGNED_SHORT   -> ScalarType.USHORT
                GL_BYTE             -> ScalarType.BYTE
                GL_UNSIGNED_BYTE    -> ScalarType.UBYTE
                else -> error("Unknown attribute scalar type 0x${attr.type.toString(16)}")
            }

            VertexAttributeDesc(
                location = loc,
                binding  = binding,
                format   = chooseFormat(attr.count, scalar),
                offset   = offset
            )
        }

        return listOf(bindingDesc) to attribs
    }

    internal fun getAttributes(): List<VertexAttribute> = attributes
}

fun bufferLayout(init: VertexBufferLayout.() -> Unit): VertexBufferLayout {
    return VertexBufferLayout().apply(init)
}