package rune.rhi

//! ------- Face visibility ------- !//

//* Tells the GPU which triangle faces to discard
/**
 *      NONE - draw both sides
 *      FRONT - rare (mirror views)
 *      BACK - default for opaque objects
 *      FRONT_AND_BACK - useful for occlusion queries
 */
enum class CullMode     { NONE, FRONT, BACK, FRONT_AND_BACK }

//* Declares which vertex winding order is considered "front"
/**
 *      CCW - Counter-clockwise (default in OpenGL)
 *      CW - Clockwise, nice if model exports in opposite winding
 */
enum class FrontFace    { CCW, CW }

//* Chooses how to rasterize a polygon
/**
 *      FILL - solid
 *      LINE - wireframe debug views
 *      POINT - vertex dots
 */
enum class PolygonMode  { FILL, LINE, POINT }

//! ------- Depth/Stencil ------- !//
//* Comparison function run between incoming fragment depth and existing depth
/**
 *      NEVER - enable depth test
 *      LESS - typical
 *      LEQUAL - good for reverse-Z
 *      ALWAYS - disable depth test
 */
enum class CompareOp    { NEVER, LESS, LEQUAL, EQUAL, GREATER, NOTEQUAL, GEQUAL, ALWAYS }

//! ------- blending ------- !//

//* Multiplier added to src or dst color before the blend op
/**
 *      SRC_ALPHA, ONE_MINUS_SRC_ALPHA - classic transparency
 *      ONE, ZERO - additive effects
 */
enum class BlendFactor  { ZERO, ONE, SRC_ALPHA, ONE_MINUS_SRC_ALPHA, DST_ALPHA, ONE_MINUS_DST_ALPHA }

//* Math used to merge src and dst
enum class BlendOp      { ADD, SUB, REVERSE_SUB, MIN, MAX }


//! ------- Vertex Input ------- !//
//* Whether a binding advances per-vertex or per instance
enum class InputRate    { Vertex, Instance }

//* Size of the indices in an index buffer
enum class IndexType    { UINT16, UINT32 }

/** Scalar kind for a vertex attribute.
 *  - FLOAT  : 32-bit IEEE
 *  - HALF   : 16-bit IEEE
 *  - DOUBLE : 64-bit IEEE
 *  - INT/UINT/… : integer variants                                      */
enum class ScalarType { FLOAT, HALF, DOUBLE, INT, UINT, SHORT, USHORT, BYTE, UBYTE }


//* Encodes component count & type so the backend can call (glVertexAttribPointer/VkVertexInputAttributeDescription)
enum class AttributeFormat(
    val components: Int,
    val scalar: ScalarType,
    val bytes: Int
) {
//    R32_SFLOAT(1, 4),
//    R32G32_SFLOAT(2, 8),
//    R32G32B32_SFLOAT(3, 12),
//    R32G32B32A32_SFLOAT(4, 16)
/* ---------- 32-bit floats ---------- */
R32_SFLOAT              (1, ScalarType.FLOAT , 4),
    R32G32_SFLOAT           (2, ScalarType.FLOAT , 4),
    R32G32B32_SFLOAT        (3, ScalarType.FLOAT , 4),
    R32G32B32A32_SFLOAT     (4, ScalarType.FLOAT , 4),

    /* ---------- 16-bit floats ---------- */
    R16_SFLOAT              (1, ScalarType.HALF  , 2),
    R16G16_SFLOAT           (2, ScalarType.HALF  , 2),

    /* ---------- 64-bit doubles ---------- */
    R64_SFLOAT              (1, ScalarType.DOUBLE, 8),
    R64G64_SFLOAT           (2, ScalarType.DOUBLE, 8),
    R64G64B64_SFLOAT        (3, ScalarType.DOUBLE, 8),
    R64G64B64A64_SFLOAT     (4, ScalarType.DOUBLE, 8),

    /* ---------- signed integers ---------- */
    R32_SINT                (1, ScalarType.INT   , 4),
    R32G32_SINT             (2, ScalarType.INT   , 4),
    R16_SINT                (1, ScalarType.SHORT , 2),
    R8_SINT                 (1, ScalarType.BYTE  , 1),

    /* ---------- unsigned integers ---------- */
    R32_UINT                (1, ScalarType.UINT  , 4),
    R16_UINT                (1, ScalarType.USHORT, 2),
    R8_UINT                 (1, ScalarType.UBYTE , 1);
}

//! ------- RenderPass Attachments ------- !//
//* Pixel format of a render target slot
enum class AttachmentFormat { RGBA16F, SRGBA8, RGBA8, R32I, DEPTH24STENCIL8 }

//* Whether the contents of an attachment are kept or discarded at the start/end of a pass
enum class LoadOp           { Load, Clear, DontCare }
enum class StoreOp          { Store, DontCare }
