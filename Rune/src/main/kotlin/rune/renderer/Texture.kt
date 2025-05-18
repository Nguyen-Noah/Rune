package rune.renderer

import org.lwjgl.opengl.GL11.GL_LINEAR
import rune.platforms.opengl.OpenGLTexture
import rune.renderer.Renderer
import rune.renderer.RendererPlatform
import java.nio.ByteBuffer

interface Texture {
    val width: Int
    val height: Int
    val rendererID: Int
    var assetPath: String?
    fun bind(slot: Int = 0)
    fun setData(color: Int, size: Int)
}

interface Texture2D : Texture {
    companion object {
        fun create(path: String, filter: Int = GL_LINEAR): Texture2D {
            return when (Renderer.getAPI()) {
                RendererPlatform.OpenGL -> OpenGLTexture(path, filter)
                RendererPlatform.None   -> TODO()
            }
        }
        fun create(width: Int, height: Int, filter: Int = GL_LINEAR): Texture2D {
            return when (Renderer.getAPI()) {
                RendererPlatform.OpenGL -> OpenGLTexture(width, height, filter)
                RendererPlatform.None   -> TODO()
            }
        }
    }
}