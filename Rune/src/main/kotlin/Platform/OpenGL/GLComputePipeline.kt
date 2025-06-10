package rune.platforms.opengl

import org.lwjgl.opengl.GL45.glDispatchCompute
import org.lwjgl.opengl.GL45.glMemoryBarrier
import org.lwjgl.opengl.GL45.GL_SHADER_IMAGE_ACCESS_BARRIER_BIT
import rune.renderer.SubmitRender
import rune.renderer.gpu.Shader
import rune.rhi.ComputePipeline

class GLComputePipeline(val shader: Shader) : ComputePipeline {
    override fun begin() {
        shader.bind()
    }

    override fun dispatch(groupsX: Int, groupsY: Int, groupsZ: Int) {
        SubmitRender("GLCompute-dispatch") {
            glDispatchCompute(groupsX, groupsY, groupsZ)
        }
    }

    override fun end() {
        SubmitRender("GLCompute-end") {
            glMemoryBarrier(GL_SHADER_IMAGE_ACCESS_BARRIER_BIT)
        }

    }
}