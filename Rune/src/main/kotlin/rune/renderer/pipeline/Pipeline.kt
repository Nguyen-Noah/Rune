package rune.renderer.pipeline

import rune.platforms.opengl.OpenGLPipeline
import rune.renderer.Renderer
import rune.renderer.RendererPlatform

interface Pipeline {
    val spec: PipelineSpec

    companion object {
        fun create(spec: PipelineSpec): Pipeline {
            return when (Renderer.getAPI()) {
                RendererPlatform.OpenGL -> OpenGLPipeline(spec)
                RendererPlatform.None -> TODO()
            }
        }
    }
}