package rune.renderer

import rune.platforms.opengl.GLComputeShader

interface ComputeShader {
    companion object {
        fun create(): ComputeShader {
            return when (Renderer.getAPI()) {
                RendererPlatform.OpenGL -> GLComputeShader()
                RendererPlatform.None   -> TODO()
            }
        }
    }
}