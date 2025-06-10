package rune.platforms.opengl

import org.lwjgl.opengl.GL45.*
import rune.platforms.opengl.gl
import rune.platforms.opengl.glInternal
import rune.renderer.gpu.Texture
import rune.renderer.gpu.TextureSpec
import rune.rhi.AttachmentFormat

class GLTextureCube(spec: TextureSpec) : Texture {
    override val width: Int = spec.width
    override val height: Int = spec.height
    override val rendererID: Int = glCreateTextures(GL_TEXTURE_CUBE_MAP)
    override var assetPath: String? = null

    private val internalFormat: Int = spec.format.glInternal
    private val dataFormat: Int = GL_RGBA

    init {
        glTextureStorage2D(rendererID, spec.mipLevels, internalFormat, width, height)
        setParameters(spec.filter.gl)
    }


    override fun bind(slot: Int) {
        //glBindTextureUnit(slot, rendererID)
        //glBindTextureUnit(GL_TEXTURE_CUBE_MAP, rendererID)
        glBindTexture(slot, rendererID)
    }

    override fun setData(color: Int, size: Int) {
        TODO("Not yet implemented")
    }

    fun open(slot: Int = 0) =
        glBindImageTexture(slot, rendererID, 0, true, 0, GL_WRITE_ONLY, internalFormat)

    private fun setParameters(filter: Int) {
        glTextureParameteri(rendererID, GL_TEXTURE_MIN_FILTER, filter)
        glTextureParameteri(rendererID, GL_TEXTURE_MAG_FILTER, filter)

        glTextureParameteri(rendererID, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE)
        glTextureParameteri(rendererID, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE)
        glTextureParameteri(rendererID, GL_TEXTURE_WRAP_R, GL_CLAMP_TO_EDGE)
    }
}