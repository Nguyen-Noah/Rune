package rune.renderer

import rune.platforms.opengl.OpenGLTexture
import rune.renderer.Renderer
import rune.renderer.RendererPlatform

interface Texture {
    val width: Int
    val height: Int
    fun bind(slot: Int = 0)
}

interface Texture2D : Texture {
    companion object {
        fun create(path: String): Texture2D {
            return when (Renderer.getAPI()) {
                RendererPlatform.OpenGL -> OpenGLTexture(path)
                RendererPlatform.None   -> TODO()
            }
        }
    }
}