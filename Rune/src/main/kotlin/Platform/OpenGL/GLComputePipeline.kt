package rune.platforms.opengl

import org.lwjgl.opengl.GL45.*
import rune.renderer.gpu.Shader
import rune.rhi.ComputePipeline

class GLComputePipeline(val shader: Shader) : ComputePipeline {
    override fun begin() {
        shader.bind()
    }

    override fun dispatch(groupsX: Int, groupsY: Int, groupsZ: Int) {
        glDispatchCompute(groupsX, groupsY, groupsZ)
    }

    override fun end() {
        glMemoryBarrier(
        GL_SHADER_IMAGE_ACCESS_BARRIER_BIT or
            GL_SHADER_STORAGE_BARRIER_BIT or
            GL_BUFFER_UPDATE_BARRIER_BIT
        )
    }
}