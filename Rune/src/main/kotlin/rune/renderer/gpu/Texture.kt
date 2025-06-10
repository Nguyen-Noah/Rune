package rune.renderer.gpu

import org.lwjgl.opengl.GL11.GL_LINEAR
import rune.platforms.opengl.GLTexture
import rune.platforms.opengl.GLTextureCube
import rune.renderer.Renderer
import rune.renderer.RendererAPI
import rune.renderer.RendererPlatform
import rune.renderer.TextureType
import rune.rhi.AttachmentFormat
import rune.rhi.Filter

data class TextureSpec(
    val format: AttachmentFormat,
    val width: Int,
    val height: Int,
    val filter: Filter,
    val mipLevels: Int = 1
)

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

interface TextureCube : Texture {
    companion object {
        fun create(spec: TextureSpec): Texture {
            return when(RendererAPI.getAPI()) {
                RendererPlatform.OpenGL -> GLTextureCube(spec)
                RendererPlatform.None -> TODO()
            }
        }
    }
}

infix fun Texture.at(texSlot: TextureType): Pair<TextureType, Texture> = texSlot to this