package rune.renderer.renderer2d

import glm_.mat4x4.Mat4
import glm_.vec4.Vec4
import rune.renderer.*
import rune.renderer.gpu.*
import rune.rhi.pipeline
import rune.rhi.renderPass

class CircleBatch(
    maxCircles: Int = 10_000,
    private val shader: Shader
) : Batch {
    private val ibo = makeIndexBuffer()

    private val maxVertices = maxCircles * 4
    private val maxIndices = maxCircles * 6

    private val pipeline = pipeline {
        debugName = "Circle-Renderer2D"
        shader = Renderer.getShader("Renderer2D_Circle")
        layout =  VertexLayout.build {
            attr(0, BufferType.Float3)  // worldPosition
            attr(1, BufferType.Float3)  // localPosition
            attr(2, BufferType.Float4)  // color
            attr(3, BufferType.Float1)  // thickness
            attr(4, BufferType.Float1)  // fade
            attr(5, BufferType.Int1)    // entityID
        }
    }

    private val circlePass = renderPass {
        debugName = "Circle-Renderer2D"
        targetFramebuffer = Renderer2D.framebuffer
        pipeline = this@CircleBatch.pipeline
    }


    private val writer = VertexBufferWriter(maxVertices, pipeline.spec.layout.stride)
    private var indices = 0
    private val vbo = VertexBuffer.create(maxVertices * pipeline.spec.layout.stride)

    init {
        SubmitRender("CircleBatch-init") {
            pipeline.bind()

            pipeline.attachVBO(vbo)
            ibo.bind()

            pipeline.unbind()
        }

    }

    override fun begin() {
        writer.reset()
        indices = 0
    }

    override fun isFull(vertexCount: Int, indexCount: Int): Boolean =
        indices + indexCount > maxIndices

    override fun flush() {
        if (indices == 0) return
        shader.bind()

        SubmitRender("Circle-flush") {
            vbo.setData(writer.slice())
            Renderer.drawIndexed(circlePass, indices)
        }
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