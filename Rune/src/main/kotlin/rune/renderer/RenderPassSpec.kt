package rune.renderer

import glm_.vec4.Vec4
import rune.renderer.pipeline.Pipeline

data class RenderPassSpec(
    val pipeline: Pipeline,
    val debugName: String,
    val markerColor: Vec4,
    val clearColor: Vec4,
    val clearDepth: Boolean
)

class RenderPassSpecBuilder {
    var pipeline: Pipeline? = null
    var debugName: String = ""
    var markerColor: Vec4 = Vec4(0f, 1f, 0f, 1f)
    var clearColor: Vec4 = Vec4(0.1f, 0.1f, 0.1f, 1.0f)
    var clearDepth: Boolean = true

    fun build(): RenderPassSpec {
        val p = pipeline ?: error("Pipeline must be set.")

        return RenderPassSpec(
            pipeline = p,
            debugName = debugName,
            markerColor = markerColor,
            clearColor = clearColor,
            clearDepth = clearDepth
        )
    }
}

fun renderPassSpec(block: RenderPassSpecBuilder.() -> Unit) =
    RenderPassSpecBuilder().apply(block).build()