package rune.platforms.opengl

import org.lwjgl.opengl.GL45.*
import org.lwjgl.stb.STBImage.*
import org.lwjgl.system.MemoryStack
import rune.renderer.gpu.Texture2D
import java.nio.ByteBuffer
import java.nio.IntBuffer

class OpenGLTexture : Texture2D {
    override val width: Int
    override val height: Int
    override val rendererID: Int
    override var assetPath: String?

    private val internalFormat: Int
    private val dataFormat: Int

    constructor(width: Int, height: Int, filter: Int) {
        this.width = width
        this.height = height
        this.assetPath = null
        internalFormat = GL_RGBA8
        dataFormat = GL_RGBA

        rendererID = glCreateTextures(GL_TEXTURE_2D)
        glTextureStorage2D(rendererID, 1, internalFormat, width, height)

        glTextureParameteri(rendererID, GL_TEXTURE_MIN_FILTER, GL_LINEAR)
        glTextureParameteri(rendererID, GL_TEXTURE_MAG_FILTER, filter)
        //glTextureParameteri(rendererID, GL_TEXTURE_MAG_FILTER, GL_LINEAR)

        glTextureParameteri(rendererID, GL_TEXTURE_WRAP_S, GL_REPEAT)
        glTextureParameteri(rendererID, GL_TEXTURE_WRAP_T, GL_REPEAT)
    }

    constructor(path: String, filter: Int) {
        this.assetPath = path
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

            val formats = when (channels[0]) {
                4 -> Pair(GL_RGBA8, GL_RGBA)
                3 -> Pair(GL_RGB8, GL_RGB)
                else -> {
                    stbi_image_free(data)
                    throw RuntimeException("Unsupported channel count: ${channels[0]}")
                }
            }
            internalFormat = formats.first
            dataFormat = formats.second

            rendererID = glCreateTextures(GL_TEXTURE_2D)
            glTextureStorage2D(rendererID, 1, internalFormat, width, height)

            glTextureParameteri(rendererID, GL_TEXTURE_MIN_FILTER, GL_LINEAR)
            glTextureParameteri(rendererID, GL_TEXTURE_MAG_FILTER, filter)
            //glTextureParameteri(rendererID, GL_TEXTURE_MAG_FILTER, GL_LINEAR)

            glTextureParameteri(rendererID, GL_TEXTURE_WRAP_S, GL_REPEAT)
            glTextureParameteri(rendererID, GL_TEXTURE_WRAP_T, GL_REPEAT)

            glTextureSubImage2D(rendererID, 0, 0, 0, width, height, dataFormat, GL_UNSIGNED_BYTE, data)

            // free image data
            stbi_image_free(data)
        }
    }

    // use: data.whiteTex?.setData(0xffffffff.toInt(), 4)
    override fun setData(color: Int, size: Int) {
        val bpp = if (dataFormat == GL_RGBA) 4 else 3
        require(size == width * height * bpp) { "Size must equal the entire texture data size!" }

        val pixelCount = width * height
        val buffer = ByteBuffer.allocateDirect(pixelCount * bpp)

        repeat(pixelCount) {
            if (bpp == 4) {
                // Assuming color is 0xRRGGBBAA
                buffer.put((color shr 24 and 0xFF).toByte()) // Red
                buffer.put((color shr 16 and 0xFF).toByte()) // Green
                buffer.put((color shr 8 and 0xFF).toByte())  // Blue
                buffer.put((color and 0xFF).toByte())          // Alpha
            } else {
                // For RGB textures ignore the alpha channel.
                buffer.put((color shr 24 and 0xFF).toByte()) // Red
                buffer.put((color shr 16 and 0xFF).toByte()) // Green
                buffer.put((color shr 8 and 0xFF).toByte())  // Blue
            }
        }
        buffer.flip()
        glTextureSubImage2D(rendererID, 0, 0, 0, width, height, dataFormat, GL_UNSIGNED_BYTE, buffer)
    }

    override fun bind(slot: Int) {
        glBindTextureUnit(slot, rendererID)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is OpenGLTexture) return false

        return rendererID == other.rendererID
    }

    override fun hashCode(): Int {
        return rendererID
    }

    override fun toString(): String {
        return "Texture2D(RendererID: $rendererID)"
    }
}