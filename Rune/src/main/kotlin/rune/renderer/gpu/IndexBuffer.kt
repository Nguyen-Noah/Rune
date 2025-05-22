package rune.renderer.gpu

import rune.platforms.opengl.OpenGLIndexBuffer
import rune.renderer.Renderer
import rune.renderer.RendererPlatform

interface IndexBuffer {
    fun bind()
    fun unbind()
    fun getCount(): Int

    companion object {
        fun create(indices: IntArray, count: Int): IndexBuffer {
            return when (Renderer.getAPI()) {
                RendererPlatform.OpenGL -> OpenGLIndexBuffer(indices, count)
                RendererPlatform.None -> TODO()
            }
        }

        fun create(indexBuffer: List<Int>): IndexBuffer {
            return when (Renderer.getAPI()) {
                RendererPlatform.OpenGL -> OpenGLIndexBuffer(indexBuffer)
                RendererPlatform.None -> TODO()
            }
        }
    }
}