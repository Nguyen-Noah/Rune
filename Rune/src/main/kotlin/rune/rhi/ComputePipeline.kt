package rune.rhi

import rune.platforms.opengl.GLComputePipeline
import rune.renderer.RendererAPI
import rune.renderer.RendererPlatform
import rune.renderer.gpu.Shader

interface ComputePipeline {
    fun begin()
    fun dispatch(groupsX: Int, groupsY: Int = 1, groupsZ: Int = 1)
    fun end()

    companion object {
        fun create(computeShader: Shader): ComputePipeline {
            return when(RendererAPI.getAPI()) {
                RendererPlatform.OpenGL -> GLComputePipeline(computeShader)
                RendererPlatform.None -> TODO()
            }
        }
    }
}