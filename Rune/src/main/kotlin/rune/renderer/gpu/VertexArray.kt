package rune.renderer.gpu

import rune.platforms.opengl.GLVertexArray
import rune.renderer.Renderer
import rune.renderer.RendererPlatform

// A vertex array wrapper that mimics ModernGL's syntax
// takes in a vbo, a layout string, and a list of attribute names
interface VertexArray {
    fun bind()
    fun unbind()
    fun addVertexBuffer(vbo: VertexBuffer)
    fun setIndexBuffer(ibo: IndexBuffer)
    fun getIndexBuffer(): IndexBuffer?

    companion object {
        fun create(vbo: VertexBuffer, layout: VertexBufferLayout): VertexArray {
            when (Renderer.getAPI()) {
                RendererPlatform.OpenGL -> return GLVertexArray(vbo, layout)
                RendererPlatform.None -> TODO()
            }
        }
    }
}