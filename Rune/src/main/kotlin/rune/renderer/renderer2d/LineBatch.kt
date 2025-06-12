package rune.renderer.renderer2d

import glm_.vec3.Vec3
import glm_.vec4.Vec4
import rune.renderer.*
import rune.renderer.gpu.*
import rune.rhi.pipeline
import rune.rhi.renderPass

const val lineWidth = 1f

class LineBatch(
    maxLines: Int = 5_000,
    private val shader: Shader
) : Batch {

    private val maxVertices = maxLines * 2
    private val layout = VertexLayout.build {
        attr(0, BufferType.Float3)  // position
        attr(1, BufferType.Float4)  // color
        attr(2, BufferType.Int1)    // entityID
    }

    private val pipeline = pipeline {
        debugName = "Line-Renderer2D"
        shader = Renderer.getShader("Renderer2D_Line")
        layout = this@LineBatch.layout
    }

    private val linePass = renderPass {
        debugName = "Line-Renderer2D"
        targetFramebuffer = Renderer2D.framebuffer
        pipeline = this@LineBatch.pipeline
    }

    private val writer = VertexBufferWriter(maxVertices, layout.stride)
    private val vbo = VertexBuffer.create(maxVertices * layout.stride)

    private var verts = 0

    override fun begin() {
        writer.reset()
        verts = 0
    }

    override fun isFull(vertexCount: Int, indexCount: Int): Boolean =
        verts + vertexCount > maxVertices

    override fun flush() {
        if (verts == 0) return
        shader.bind()

        SubmitRender("Line-flush") {
            pipeline.bind()

            vbo.setData(writer.slice())
            Renderer.drawLines(linePass, verts)

            pipeline.unbind()
        }

        //Renderer.setLineThickness(lineWidth)
        Renderer.stats.drawCalls++
    }

    /* --------------- Render API ----------------- */

    fun pushLine(
        p0: Vec3,
        p1: Vec3,
        color: Vec4,
        entityId: Int = -1
    ) {
        writer.write {
            putFloat(p0.x); putFloat(p0.y); putFloat(p0.z)
            putFloat(color.r); putFloat(color.g); putFloat(color.b); putFloat(color.a)
            putInt(entityId)
        }
        writer.write {
            putFloat(p1.x); putFloat(p1.y); putFloat(p1.z)
            putFloat(color.r); putFloat(color.g); putFloat(color.b); putFloat(color.a)
            putInt(entityId)
        }
        verts += 2
    }
}