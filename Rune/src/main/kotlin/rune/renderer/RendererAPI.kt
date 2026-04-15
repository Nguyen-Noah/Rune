package rune.renderer

import glm_.mat4x4.Mat4
import glm_.vec4.Vec4
import rune.renderer.gpu.IndexBuffer
import rune.renderer.gpu.Texture
import rune.renderer.gpu.VertexBuffer
import rune.renderer.renderer3d.Mesh
import rune.rhi.Pipeline
import rune.rhi.PolygonMode
import rune.rhi.RenderPass

enum class RendererPlatform {
    None,
    OpenGL
}

/**
 * Backend-facing rendering operations.
 *
 * Design intent:
 * - **Backend (API) level**: low-level draw/pass/viewport style operations that map closely to the underlying graphics API.
 * - **Engine level**: higher-level systems (scene rendering, post processing, render graphs) should be built on top of
 *   these primitives (e.g. tonemapping is typically a fullscreen pipeline pass + bound input textures).
 *
 * The active backend is owned and selected by [Renderer] (injected at init time).
 */
interface RendererAPI {
    /**
     * Prebuilt quad buffers used by backends for fullscreen passes.
     * This is backend-internal data; the engine submits fullscreen work via [submitFullscreenQuad].
     */
    data class FullscreenQuad(
        val vbo: VertexBuffer,
        val ibo: IndexBuffer
    )

    /** Initialize backend state (blending, depth test, debug, etc.). Called by [Renderer.init]. */
    fun init()

    /** Set the clear color for subsequent [clear] calls. */
    fun setClearColor(color: Vec4)

    /** Clear the currently bound framebuffer (color/depth as appropriate). */
    fun clear()

    /**
     * Draw indexed geometry for the given [pass].
     * If [indexCount] is 0, the backend may draw using the currently bound index buffer's full range (backend-dependent).
     */
    fun drawIndexed(pass: RenderPass, indexCount: Int = 0)

    /** Set the viewport rectangle in pixels. */
    fun setViewport(x: Int, y: Int, width: Int, height: Int)

    /** Draw line primitives for the given [pass]. */
    fun drawLines(pass: RenderPass, vertexCount: Int)

    /** Set line raster width (backend-dependent; may be clamped). */
    fun setLineWidth(width: Float)

    /** Render a static mesh with [pipeline] and per-instance [transform]. */
    fun renderStaticMesh(pipeline: Pipeline, mesh: Mesh, transform: Mat4)

    /**
     * Submit a fullscreen draw for [pipeline].
     * Typical use: post processing (tonemap, FXAA, bloom combine) where inputs are provided via bound textures/UBOs.
     */
    fun submitFullscreenQuad(pipeline: Pipeline)

    /**
     * Begin a render pass.
     * Backends should bind the pass target framebuffer and configure attachments as needed.
     */
    fun beginRenderPass(pass: RenderPass, clear: Boolean = false)

    /** End the current render pass. */
    fun endRenderPass()

    /**
     * Create an environment map from an HDR equirectangular source.
     * Note: this currently lives on the backend interface; long-term it may move to an engine-level utility.
     */
    fun createEnvironmentMap(file: String): Texture

    /** Render a skybox using [envMap] for the given [pass]. */
    fun renderSkybox(pass: RenderPass, envMap: Texture)

    /** Toggle wireframe rendering */
    fun toggleWireframe(mode: PolygonMode)

    fun enableBlend()

    fun disableBlend()
}