package rune.platforms.opengl

import org.lwjgl.opengl.GL45.*
import org.lwjgl.stb.STBImage.*
import org.lwjgl.system.MemoryStack
import rune.renderer.gpu.Texture2D
import rune.renderer.SubmitRender
import java.nio.ByteBuffer
import java.nio.FloatBuffer
import java.nio.IntBuffer

class GLTexture : Texture2D {
    override val width: Int
    override val height: Int
    override val rendererID: Int
    override var assetPath: String?

    private val internalFormat: Int
    private val dataFormat: Int
    private val typeFormat: Int

    constructor(width: Int, height: Int, filter: Int) {
        this.width = width
        this.height = height
        this.assetPath = null
        internalFormat = GL_RGBA8
        dataFormat = GL_RGBA
        typeFormat = GL_FLOAT

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

            val isHdr = stbi_is_hdr(path)
            val dataF: FloatBuffer?
            val dataB: ByteBuffer?

            // load images vertically
            stbi_set_flip_vertically_on_load(true)

            if (isHdr) {
                dataF = stbi_loadf(path, w, h, channels, 0)
                dataB = null
                require(dataF != null) { "Failed to load HDR: $path" }
            } else {
                dataB = stbi_load(path, w, h, channels, 0)
                dataF = null
                require(dataB != null) { "Failed to load HDR: $path" }
            }

            width = w[0]
            height = h[0]

            if (isHdr) {
                internalFormat = if (channels[0] == 3) GL_RGB16F else GL_RGBA16F
                dataFormat     = if (channels[0] == 3) GL_RGB    else GL_RGBA
                typeFormat     = GL_FLOAT
            } else {
                internalFormat = if (channels[0] == 3) GL_RGB8 else GL_RGBA8
                dataFormat     = if (channels[0] == 3) GL_RGB  else GL_RGBA
                typeFormat     = GL_UNSIGNED_BYTE
            }

            rendererID = glCreateTextures(GL_TEXTURE_2D)
            glTextureStorage2D(rendererID, 1, internalFormat, width, height)

            glTextureParameteri(rendererID, GL_TEXTURE_MIN_FILTER, GL_LINEAR)
            glTextureParameteri(rendererID, GL_TEXTURE_MAG_FILTER, filter)
            glTextureParameteri(rendererID, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE)
            glTextureParameteri(rendererID, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE)

            // free image data
            if (isHdr) {
                glTextureSubImage2D(rendererID, 0, 0, 0, width, height,
                    dataFormat, typeFormat, dataF!!)
                stbi_image_free(dataF)
            } else {
                glTextureSubImage2D(rendererID, 0, 0, 0, width, height,
                    dataFormat, typeFormat, dataB!!)
                stbi_image_free(dataB)
            }
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
        if (other !is GLTexture) return false

        return rendererID == other.rendererID
    }

    override fun hashCode(): Int {
        return rendererID
    }

    override fun toString(): String {
        return "Texture2D(RendererID: $rendererID)"
    }
}