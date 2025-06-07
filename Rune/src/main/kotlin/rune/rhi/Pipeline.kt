package rune.rhi

import rune.platforms.opengl.GLPipeline
import rune.renderer.Renderer
import rune.renderer.RendererPlatform
import rune.renderer.gpu.Shader
import rune.renderer.gpu.VertexLayout

interface Pipeline {
    fun bind()
    fun unbind()

    companion object {
        fun create(spec: PipelineSpec): Pipeline {
            return when (Renderer.getAPI()) {
                RendererPlatform.OpenGL -> GLPipeline(spec)
                RendererPlatform.None -> TODO()
            }
        }
    }
}

data class PipelineSpec(
    val debugName: String,
    val shader: Shader,
    val subpass: Int = 0,

//    val vertexBindings: List<VertexInputBinding>,
//    val vertexAttributes: List<VertexAttributeDesc>,

    //val layout: Pair<VertexInputBinding, List<VertexAttributeDesc>>,
    //val layout: VertexBufferLayout,
    val layout: VertexLayout,

    val raster: RasterState = RasterState(),
    val depth: DepthState = DepthState(),
    val blends: List<BlendAttachment> = listOf()
)

class PipelineBuilder {
    var debugName: String = "Unnamed-Pipeline"
    var shader: Shader? = null
    var layout: VertexLayout? = null
    var raster: RasterState = RasterState()
    var depth: DepthState = DepthState()
    var blends: List<BlendAttachment> = listOf()

    fun build(): PipelineSpec {
        return PipelineSpec(
            debugName = debugName,
            shader = shader!!,
            layout = layout!!,
            raster = raster,
            depth = depth,
            blends = blends
        )
    }
}

fun pipeline(init: PipelineBuilder.() -> Unit): Pipeline =
    Pipeline.create(PipelineBuilder().apply(init).build())