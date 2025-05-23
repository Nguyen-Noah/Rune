package rune.renderer

import rune.platforms.opengl.OpenGLComputeShader

interface ComputeShader {
    companion object {
        fun create(): ComputeShader {
            return when (Renderer.getAPI()) {
                RendererPlatform.OpenGL -> OpenGLComputeShader()
                RendererPlatform.None   -> TODO()
            }
        }
    }
}