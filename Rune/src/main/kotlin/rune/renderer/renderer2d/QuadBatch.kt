package rune.renderer.renderer2d

import glm_.mat4x4.Mat4
import glm_.vec2.Vec2
import glm_.vec4.Vec4
import rune.core.Logger
import rune.renderer.*
import rune.renderer.gpu.*
import rune.rhi.Pipeline
import rune.rhi.pipeline

class QuadBatch(
    maxQuads: Int = 10_000,
    private val maxTextureSlots: Int = 32,  // TODO: gpu capabilities
    private val shader: Shader,
    whiteTex: Texture2D
) : Batch {

    private val ibo = makeIndexBuffer()

    val vBufferLayout = bufferLayout {
        attribute("a_Position", 3)
        attribute("a_Color", 4)
        attribute("a_TexCoords", 2)
        attribute("a_TilingFactor", 1)
        attribute("a_EntityID", 1)
    }

    val pipelineSpec = pipeline {
        debugName = "Renderer2D-Quad"
        shader = this@QuadBatch.shader
        layout = VertexLayout.build {
            attr(0, BufferType.Float3)  // position
            attr(1, BufferType.Float4)  // color
            attr(2, BufferType.Float2)  // texcoords
            attr(3, BufferType.Float1)  // texIndex
            attr(4, BufferType.Float1)  // tilingFactor
            attr(5, BufferType.Int1)    // entityID
        }
    }

    //val pipeline = Pipeline.create(pipelineSpec)

//    val pipelineSpec = pipelineSpec {
//        debugName = "Renderer2D-Quad"
//        shader = this@QuadBatch.shader
//        targetFramebuffer = null
//        layout = VertexLayout.build {
//            attr(0, BufferType.Float3)  // position
//            attr(1, BufferType.Float4)  // color
//            attr(2, BufferType.Float2)  // texcoords
//            attr(3, BufferType.Float1)  // texIndex
//            attr(4, BufferType.Float1)  // tilingFactor
//            attr(5, BufferType.Int1)    // entityID
//        }
//        vao = VertexArray.create(vbo, bufferLayout {
//            attribute("a_Position", 3)
//            attribute("a_Color", 4)
//            attribute("a_TexCoord", 2)
//            attribute("a_TexIndex", 1)
//            attribute("a_TilingFactor", 1)
//            attribute("a_EntityID", 1)
//        }).apply { setIndexBuffer(ibo) }
//        enableDepthTest = true
//        backfaceCulling = false
//    }
//    val pipeline = Pipeline.create(pipelineSpec)
//
//    val quadSpec = renderPassSpec {
//        pipeline = this@QuadBatch.pipeline
//        debugName = "Renderer2D-Quad"
//    }


    /* ------------- constants and buffers --------------- */
    private val maxVertices = maxQuads * 4
    private val maxIndices = maxQuads * 6

    private val layout = VertexLayout.build {
        attr(0, BufferType.Float3)  // position
        attr(1, BufferType.Float4)  // color
        attr(2, BufferType.Float2)  // texcoords
        attr(3, BufferType.Float1)  // texIndex
        attr(4, BufferType.Float1)  // tilingFactor
        attr(5, BufferType.Int1)    // entityID
    }

    private val writer = VertexBufferWriter(maxVertices, layout.stride)
    private var indices = 0
    private val vbo = VertexBuffer.create(maxVertices * layout.stride)
    private val vao = VertexArray.create(vbo, bufferLayout {
        attribute("a_Position", 3)
        attribute("a_Color", 4)
        attribute("a_TexCoord", 2)
        attribute("a_TexIndex", 1)
        attribute("a_TilingFactor", 1)
        attribute("a_EntityID", 1)
    }).apply { setIndexBuffer(ibo) }

    private val textureSlots = arrayOfNulls<Texture2D>(maxTextureSlots).apply { this[0] = whiteTex }
    private var texSlotIdx = 1

    override fun begin() {
        writer.reset()
        indices = 0
        texSlotIdx = 1
    }

    override fun isFull(vertexCount: Int, indexCount: Int): Boolean =
        indices + indexCount > maxIndices

    override fun flush() {
        if (indices == 0) return

        vbo.setData(writer.slice())
        textureSlots.forEachIndexed { i, tex -> tex?.bind(i) }

        shader.bind()
        Renderer.drawIndexed(vao, indices)
        Renderer.stats.drawCalls++
    }

    /* --------------- Render API ----------------- */

    fun pushQuad(
        transform: Mat4,
        color: Vec4 = Vec4(1f),
        tex: Texture2D? = null,
        tilingFactor: Float = 1f,
        entityId: Int = -1
    ) {
        // texture slot bookkeeping
        val texIdx = when (tex) {
            null -> 0f
            else -> {
                var idx = textureSlots.indexOfFirst { it == tex }
                if (idx == -1) {
                    if (texSlotIdx >= maxTextureSlots)
                        Logger.error("Texture slot overflow")

                    idx = texSlotIdx
                    textureSlots[texSlotIdx++] = tex
                }
                idx.toFloat()
            }
        }

        // writing the vertices
        val quadPos = QuadPositions
        val uv = QuadUVs
        repeat(4) { i ->
            writer.write {
                (transform * quadPos[i]).toVec3().also { p -> putFloat(p.x); putFloat(p.y); putFloat(p.z) }
                putFloat(color.r); putFloat(color.g); putFloat(color.b); putFloat(color.a)
                putFloat(uv[i].x); putFloat(uv[i].y)
                putFloat(texIdx)
                putFloat(tilingFactor)
                putInt(entityId)
            }
        }

        indices += 6
    }

    /* ------------- private helper -------------- */

    private fun makeIndexBuffer(): IndexBuffer {
        val idx = IntArray(maxIndices)
        var offs = 0
        for (i in 0 until maxIndices step 6) {
            idx[i + 0] = offs + 0
            idx[i + 1] = offs + 1
            idx[i + 2] = offs + 2

            idx[i + 3] = offs + 2
            idx[i + 4] = offs + 3
            idx[i + 5] = offs + 0
            offs += 4
        }
        return IndexBuffer.create(idx, maxIndices)
    }

    private companion object {
        // cached immutable arrays keep math allocations at zero
        val QuadPositions = arrayOf(
            Vec4(-0.5f, -0.5f, 0f, 1f), Vec4( 0.5f, -0.5f, 0f, 1f),
            Vec4( 0.5f,  0.5f, 0f, 1f), Vec4(-0.5f,  0.5f, 0f, 1f)
        )
        val QuadUVs = arrayOf(
            Vec2(0f, 0f), Vec2(1f, 0f),
            Vec2(1f, 1f), Vec2(0f, 1f)
        )
    }
}