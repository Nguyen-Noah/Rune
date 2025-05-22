package rune.renderer.renderer2d

import glm_.vec3.Vec3
import glm_.vec4.Vec4
import rune.renderer.*
import rune.renderer.gpu.*

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

    private val writer = VertexBufferWriter(maxVertices, layout.stride)
    private val vbo = VertexBuffer.create(maxVertices * layout.stride)
    private val vao = VertexArray.create(vbo, bufferLayout {
        attribute("a_Position", 3)
        attribute("a_Color", 4)
        attribute("a_EntityID", 1)
    })
    private var verts = 0

    override fun begin() {
        writer.reset()
        verts = 0
    }

    override fun isFull(vertexCount: Int, indexCount: Int): Boolean =
        verts + vertexCount > maxVertices

    override fun flush() {
        if (verts == 0) return

        vbo.setData(writer.slice())

        shader.bind()
        RenderCommand.setLineThickness(lineWidth)
        Renderer.stats.drawCalls++
        RenderCommand.drawLines(vao, verts)
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