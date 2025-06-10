package rune.platforms.opengl

import org.lwjgl.opengl.GL45.*
import rune.rhi.*

internal val FrontFace.gl
    get() = if (this == FrontFace.CCW) GL_CCW else GL_CW
internal val PolygonMode.gl
    get() = when(this) { PolygonMode.FILL -> GL_FILL; PolygonMode.LINE -> GL_LINE; PolygonMode.POINT -> GL_POINT }
internal val CompareOp.gl
    get() = when(this) {
        CompareOp.NEVER -> GL_NEVER; CompareOp.LESS -> GL_LESS; CompareOp.LEQUAL -> GL_LEQUAL;
        CompareOp.EQUAL -> GL_EQUAL; CompareOp.GREATER -> GL_GREATER; CompareOp.NOTEQUAL -> GL_NOTEQUAL;
        CompareOp.GEQUAL -> GL_GEQUAL; CompareOp.ALWAYS -> GL_ALWAYS
    }
internal val BlendFactor.gl
    get() = when(this) {
        BlendFactor.ZERO->GL_ZERO; BlendFactor.ONE->GL_ONE;
        BlendFactor.SRC_ALPHA -> GL_SRC_ALPHA; BlendFactor.ONE_MINUS_SRC_ALPHA -> GL_ONE_MINUS_SRC_ALPHA;
        BlendFactor.DST_ALPHA -> GL_DST_ALPHA; BlendFactor.ONE_MINUS_DST_ALPHA -> GL_ONE_MINUS_DST_ALPHA
    }
internal val BlendOp.gl
    get() = when(this) {
        BlendOp.ADD -> GL_FUNC_ADD; BlendOp.SUB -> GL_FUNC_SUBTRACT; BlendOp.REVERSE_SUB -> GL_FUNC_REVERSE_SUBTRACT;
        BlendOp.MIN -> GL_MIN; BlendOp.MAX -> GL_MAX
    }
internal val IndexType.gl
    get() = if (this == IndexType.UINT16) GL_UNSIGNED_SHORT else GL_UNSIGNED_INT
internal val AttachmentFormat.glInternal
    get() = when(this){
        AttachmentFormat.RGBA16F -> GL_RGBA16F; AttachmentFormat.SRGBA8 -> GL_SRGB8_ALPHA8;
        AttachmentFormat.RGBA8 -> GL_RGBA8; AttachmentFormat.DEPTH24STENCIL8 -> GL_DEPTH24_STENCIL8
        AttachmentFormat.R32I -> GL_RED_INTEGER
}

internal val BufferUsage.gl
    get() = when(this) {
        BufferUsage.Immutable -> GL_STATIC_DRAW
        BufferUsage.Dynamic -> GL_DYNAMIC_DRAW
        BufferUsage.Stream -> GL_STREAM_DRAW
}

internal val AttributeFormat.glType: Int
    get() = when (scalar) {
        ScalarType.FLOAT   -> GL_FLOAT
        ScalarType.HALF    -> GL_HALF_FLOAT
        ScalarType.DOUBLE  -> GL_DOUBLE
        ScalarType.INT     -> GL_INT
        ScalarType.UINT    -> GL_UNSIGNED_INT
        ScalarType.SHORT   -> GL_SHORT
        ScalarType.USHORT  -> GL_UNSIGNED_SHORT
        ScalarType.BYTE    -> GL_BYTE
        ScalarType.UBYTE   -> GL_UNSIGNED_BYTE
    }

internal val AttributeFormat.isInteger: Boolean
    get() = when (scalar) {
        ScalarType.INT, ScalarType.UINT,
        ScalarType.SHORT, ScalarType.USHORT,
        ScalarType.BYTE, ScalarType.UBYTE -> true
        else -> false
    }

internal val AttributeFormat.isDouble: Boolean
    get() = (scalar == ScalarType.DOUBLE)

internal val Filter.gl: Int
    get() = when(this) {
        Filter.LINEAR -> GL_LINEAR
    }