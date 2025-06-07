package rune.renderer.renderer2d

import glm_.mat4x4.Mat4
import glm_.vec4.Vec4
import rune.renderer.*
import rune.renderer.gpu.*

class CircleBatch(
    maxCircles: Int = 10_000,
    private val shader: Shader
) : Batch {

    private val maxVertices = maxCircles * 4
    private val maxIndices = maxCircles * 6

    private val layout = VertexLayout.build {
        attr(0, BufferType.Float3)  // worldPosition
        attr(1, BufferType.Float3)  // localPosition
        attr(2, BufferType.Float4)  // color
        attr(3, BufferType.Float1)  // thickness
        attr(4, BufferType.Float1)  // fade
        attr(5, BufferType.Int1)    // entityID
    }

    private val writer = VertexBufferWriter(maxVertices, layout.stride)
    private val ibo = makeIndexBuffer()
    private var indices = 0
    private val vbo = VertexBuffer.create(maxVertices * layout.stride)
    private val vao = VertexArray.create(vbo, bufferLayout {
        attribute("a_WorldPosition", 3)
        attribute("a_LocalPosition", 3)
        attribute("a_Color", 4)
        attribute("a_Thickness", 1)
        attribute("a_Fade", 1)
        attribute("a_EntityID", 1)
    }).apply { setIndexBuffer(ibo) }

    override fun begin() {
        writer.reset()
        indices = 0
    }

    override fun isFull(vertexCount: Int, indexCount: Int): Boolean =
        indices + indexCount > maxIndices

    override fun flush() {
        if (indices == 0) return

        vbo.setData(writer.slice())

        shader.bind()
        Renderer.drawIndexed(vao, indices)
        Renderer.stats.drawCalls++
    }

    /* --------------- Render API ----------------- */

    fun pushCircle(
        transform: Mat4,
        color: Vec4,
        thickness: Float,
        fade: Float,
        entityId: Int = -1
    ) {
        repeat(4) { i ->
            val worldPos = (transform * QuadPos[i]).toVec3()
            val localPos = (QuadPos[i] * 2f).toVec3()

            writer.write {
                putFloat(worldPos.x); putFloat(worldPos.y); putFloat(worldPos.z)
                putFloat(localPos.x); putFloat(localPos.y); putFloat(localPos.z)
                putFloat(color.r); putFloat(color.g); putFloat(color.b); putFloat(color.a)
                putFloat(thickness)
                putFloat(fade)
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
        val QuadPos = arrayOf(
            Vec4(-0.5f, -0.5f, 0f, 1f), Vec4( 0.5f, -0.5f, 0f, 1f),
            Vec4( 0.5f,  0.5f, 0f, 1f), Vec4(-0.5f,  0.5f, 0f, 1f)
        )
    }
}