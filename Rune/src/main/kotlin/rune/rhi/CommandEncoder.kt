package rune.rhi

import rune.platforms.opengl.GLCommandEncoder
import rune.platforms.opengl.GLDevice
import rune.renderer.RendererAPI
import rune.renderer.RendererPlatform

interface CommandEncoder {
    fun beginRenderPass(pass: RenderPass, clear: ClearValues)
    fun nextSubpass()
    fun setPipeline(pipeline: Pipeline)
    fun bindVertexBuffers(first: Int, buffers: List<BufferHandle>)
    fun bindIndexBuffer(buffer: BufferHandle, offset: Long, type: IndexType)
    fun drawIndexed(indexCount: Int, instanceCount: Int = 1)
    fun endRenderPass()

    companion object {
        fun create(device: Device): CommandEncoder {
            return when(RendererAPI.getAPI()) {
                RendererPlatform.OpenGL -> GLCommandEncoder(device as GLDevice)
                RendererPlatform.None -> TODO()
            }
        }
    }
}