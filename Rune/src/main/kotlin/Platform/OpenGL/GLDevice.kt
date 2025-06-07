package rune.platforms.opengl

import rune.rhi.PipelineSpec
import rune.renderer.renderer3d.mesh.Vertex
import rune.rhi.*
import rune.rhi.RenderPass
import rune.rhi.RenderPassSpec
import java.nio.ByteBuffer

class GLDevice : Device {
    private val passes = arrayListOf<GLRenderPass>()
    private val pipelines = arrayListOf<GLPipeline>()
    private val buffers = arrayListOf<GLBuffer>()

    override fun createRenderPass(spec: RenderPassSpec): RenderPassHandle =
        RenderPassHandle(passes.addAndGet(GLRenderPass(spec)))

    override fun createPipeline(desc: PipelineSpec): PipelineHandle =
        PipelineHandle(pipelines.addAndGet(GLPipeline(desc)))

    override fun createVertexBuffer(sizeBytes: Int, usage: BufferUsage): BufferHandle =
        BufferHandle(buffers.addAndGet(GLVertexBuffer(sizeBytes)))

    override fun createVertexBuffer(vertices: FloatArray, usage: BufferUsage): BufferHandle =
        BufferHandle(buffers.addAndGet(GLVertexBuffer(vertices)))

    override fun createVertexBuffer(vertices: List<Vertex>, usage: BufferUsage): BufferHandle =
        BufferHandle(buffers.addAndGet(GLVertexBuffer(vertices)))

    override fun createIndexBuffer(size: Int, usage: BufferUsage, data: ByteBuffer?, type: IndexType): BufferHandle {
        TODO("Not yet implemented")
    }

    override fun newRenderPass(spec: RenderPassSpec): RenderPass =
        GLRenderPass(spec)

    fun buffer(i: BufferHandle) = buffers[i.id]
}

private fun <T> MutableList<T>.addAndGet(obj: T): Int {
    add(obj); return lastIndex
}