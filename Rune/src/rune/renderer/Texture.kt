package rune.renderer

import rune.platforms.opengl.OpenGLTexture
import rune.renderer.Renderer
import rune.renderer.RendererPlatform
import java.nio.ByteBuffer

interface Texture {
    val width: Int
    val height: Int
    val rendererID: Int
    fun bind(slot: Int = 0)
    fun setData(color: Int, size: Int)
}

interface Texture2D : Texture {
    companion object {
        fun create(path: String): Texture2D {
            return when (Renderer.getAPI()) {
                RendererPlatform.OpenGL -> OpenGLTexture(path)
                RendererPlatform.None   -> TODO()
            }
        }
        fun create(width: Int, height: Int): Texture2D {
            return when (Renderer.getAPI()) {
                RendererPlatform.OpenGL -> OpenGLTexture(width, height)
                RendererPlatform.None   -> TODO()
            }
        }
    }
}