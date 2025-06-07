package rune.platforms.opengl

import org.lwjgl.opengl.GL45.*
import rune.rhi.RenderPass
import rune.rhi.ClearValues
import rune.rhi.Pipeline
import rune.rhi.BufferHandle
import rune.rhi.CommandEncoder
import rune.rhi.IndexType

class GLCommandEncoder(private val device: GLDevice): CommandEncoder {
    private var currentPass: GLRenderPass? = null

    override fun beginRenderPass(pass: RenderPass, clear: ClearValues) {
        currentPass = pass as? GLRenderPass ?: error("RenderPass is not an OpenGL pass.")

        currentPass?.let {
            // 1. bind the fbo and viewport
            it.bind()
            if (it.glDrawBuffers.isNotEmpty())
                glDrawBuffers(it.glDrawBuffers)

            // clear per request
            var mask = 0
            clear.colors.firstOrNull()?.let { c ->
                glClearColor(c.r, c.g, c.b, c.a)
                mask = mask or GL_COLOR_BUFFER_BIT
            }
            clear.depth?.let { d ->
                glClearDepthf(d)
                mask = mask or GL_DEPTH_BUFFER_BIT
            }
            if (mask != 0)
                glClear(mask)
        }
    }

    override fun nextSubpass() {/*  */}

    override fun setPipeline(pipeline: Pipeline) {
        (pipeline as? GLPipeline)?.bind() ?: error("Pipeline supplied is not an OpenGLPipeline.")
    }

    override fun bindVertexBuffers(first: Int, buffers: List<BufferHandle>) {
        buffers.forEachIndexed { i, h ->
            val buf = device.buffer(h)
            glBindBuffer(GL_ARRAY_BUFFER, buf.rendererID)
        }
    }

    override fun bindIndexBuffer(buffer: BufferHandle, offset: Long, type: IndexType) {
        val ibo = device.buffer(buffer) as GLIndexable
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, ibo.rendererID)
    }

    override fun drawIndexed(indexCount: Int, instanceCount: Int) {
        glDrawElementsInstanced(GL_TRIANGLES, indexCount, GL_UNSIGNED_INT, 0L, instanceCount)
    }

    override fun endRenderPass() {
        currentPass?.unbind()
        currentPass = null
    }
}
