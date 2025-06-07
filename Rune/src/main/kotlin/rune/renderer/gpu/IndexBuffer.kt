package rune.renderer.gpu

import rune.platforms.opengl.GLIndexBuffer
import rune.renderer.Renderer
import rune.renderer.RendererPlatform

interface IndexBuffer {
    fun bind()
    fun unbind()

    companion object {
        fun create(indices: IntArray, count: Int): IndexBuffer {
            return when (Renderer.getAPI()) {
                RendererPlatform.OpenGL -> GLIndexBuffer(indices, count)
                RendererPlatform.None -> TODO()
            }
        }

        fun create(indexBuffer: List<Int>): IndexBuffer {
            return when (Renderer.getAPI()) {
                RendererPlatform.OpenGL -> GLIndexBuffer(indexBuffer)
                RendererPlatform.None -> TODO()
            }
        }
    }
}