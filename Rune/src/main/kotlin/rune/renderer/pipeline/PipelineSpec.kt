package rune.renderer.pipeline

import rune.renderer.gpu.Framebuffer
import rune.renderer.gpu.Shader
import rune.renderer.gpu.VertexArray
import rune.renderer.gpu.VertexLayout

data class PipelineSpec(
    val shader: Shader,
    val targetFramebuffer: Framebuffer,
    val layout: VertexLayout,
    val vao: VertexArray,
    val enableDepthTest: Boolean = true,
    val debugName: String = "Untitled Pipeline",
    val wireframe: Boolean = false,
    val backfaceCulling: Boolean = true
)

class PipelineSpecBuilder {
    var shader: Shader?                 = null
    var targetFramebuffer: Framebuffer? = null
    var layout: VertexLayout?           = null
    var vao: VertexArray?               = null
    var enableDepthTest: Boolean        = true
    var debugName: String               = "Untitled-Pipeline"
    var wireframe: Boolean              = false
    var backfaceCulling: Boolean        = true

    fun build(): PipelineSpec {
        val s = shader ?: error("Shader must be specified.")
        val fbo = targetFramebuffer ?: error("Framebuffer must be specified.")
        val l = layout ?: error("Layout must be specified.")
        val v = vao ?: error("Vertex Array must be specified.")

        return PipelineSpec(
            shader = s,
            targetFramebuffer = fbo,
            layout = l,
            vao = v,
            enableDepthTest = enableDepthTest,
            debugName = debugName,
            wireframe = wireframe,
            backfaceCulling = backfaceCulling
        )
    }
}

fun pipelineSpec(block: PipelineSpecBuilder.() -> Unit): PipelineSpec =
    PipelineSpecBuilder().apply(block).build()