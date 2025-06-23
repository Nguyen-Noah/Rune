package rune.renderer

import rune.platforms.opengl.GLStorageBuffer
import java.nio.ByteBuffer

interface StorageBuffer {
    val rendererId: Int

    fun clear()
    fun setData(data: ByteBuffer, offset: Int, name: String)
    fun bind(binding: Int)

    companion object {
        fun create(size: Int): StorageBuffer {
            return when(Renderer.getAPI()) {
                RendererPlatform.OpenGL -> GLStorageBuffer(size)
                RendererPlatform.None -> TODO()
            }
        }
    }
}