package rune.platforms.opengl

import org.lwjgl.opengl.GL45.*
import org.lwjgl.stb.STBImage.*
import org.lwjgl.system.MemoryStack
import rune.renderer.Texture2D
import java.nio.ByteBuffer
import java.nio.IntBuffer

class OpenGLTexture(private val path: String) : Texture2D {
    override val width: Int
    override val height: Int
    private val rendererID: Int

    init {
        MemoryStack.stackPush().use { stack ->
            // allocate buffers for image dimensions and channels
            val w: IntBuffer = stack.mallocInt(1)
            val h: IntBuffer = stack.mallocInt(1)
            val channels: IntBuffer = stack.mallocInt(1)

            // load images vertically
            stbi_set_flip_vertically_on_load(true)

            val data: ByteBuffer = stbi_load(path, w, h, channels, 0) ?: throw RuntimeException("failed to load image: $path")

            width = w[0]
            height = h[0]

            val (internalFormat, dataFormat) = when (channels[0]) {
                4 -> Pair(GL_RGBA8, GL_RGBA)
                3 -> Pair(GL_RGB8, GL_RGB)
                else -> {
                    stbi_image_free(data)
                    throw RuntimeException("Unsupported channel count: ${channels[0]}")
                }
            }

            rendererID = glCreateTextures(GL_TEXTURE_2D)
            glTextureStorage2D(rendererID, 1, internalFormat, width, height)
            glTextureParameteri(rendererID, GL_TEXTURE_MIN_FILTER, GL_LINEAR)
            glTextureParameteri(rendererID, GL_TEXTURE_MAG_FILTER, GL_NEAREST)
            glTextureSubImage2D(rendererID, 0, 0, 0, width, height, dataFormat, GL_UNSIGNED_BYTE, data)

            // free image data
            stbi_image_free(data)
        }
    }

    override fun bind(slot: Int) {
        glBindTextureUnit(slot, rendererID)
    }
}