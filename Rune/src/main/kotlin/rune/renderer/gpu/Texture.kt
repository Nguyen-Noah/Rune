package rune.renderer.gpu

import org.lwjgl.opengl.GL11.GL_LINEAR
import rune.platforms.opengl.GLTexture
import rune.renderer.Renderer
import rune.renderer.RendererPlatform
import rune.renderer.TextureType

interface Texture {
    val width: Int
    val height: Int
    val rendererID: Int
    var assetPath: String?
    fun bind(slot: Int = 0)
    fun setData(color: Int, size: Int)
}

interface Texture2D : Texture {
    override fun toString(): String
    companion object {
        fun create(path: String, filter: Int = GL_LINEAR): Texture2D {
            return when (Renderer.getAPI()) {
                RendererPlatform.OpenGL -> GLTexture(path, filter)
                RendererPlatform.None   -> TODO()
            }
        }
        fun create(width: Int, height: Int, filter: Int = GL_LINEAR): Texture2D {
            return when (Renderer.getAPI()) {
                RendererPlatform.OpenGL -> GLTexture(width, height, filter)
                RendererPlatform.None   -> TODO()
            }
        }
    }
}

infix fun Texture.at(texSlot: TextureType): Pair<TextureType, Texture> = texSlot to this