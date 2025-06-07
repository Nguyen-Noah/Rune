package rune.rhi

import rune.platforms.opengl.GLDevice
import rune.renderer.RendererAPI
import rune.renderer.RendererPlatform
import rune.renderer.renderer3d.mesh.Vertex

enum class BufferUsage { Immutable, Dynamic, Stream }

interface Device {
    fun createRenderPass(spec: RenderPassSpec): RenderPassHandle
    fun createPipeline(desc: PipelineSpec): PipelineHandle

    fun createVertexBuffer(sizeBytes: Int,
                           usage: BufferUsage = BufferUsage.Immutable): BufferHandle
    fun createVertexBuffer(vertices: FloatArray,
                           usage: BufferUsage = BufferUsage.Immutable): BufferHandle
    fun createVertexBuffer(vertices: List<Vertex>,
                           usage: BufferUsage = BufferUsage.Immutable): BufferHandle

    fun createIndexBuffer (size: Int,
                           usage: BufferUsage = BufferUsage.Immutable,
                           data: java.nio.ByteBuffer? = null,
                           type: IndexType = IndexType.UINT32): BufferHandle

    fun newRenderPass(spec: RenderPassSpec): RenderPass


    companion object {
        fun create(): Device {
            return when(RendererAPI.getAPI()) {
                RendererPlatform.OpenGL -> GLDevice()
                RendererPlatform.None -> TODO()
            }
        }
    }
}