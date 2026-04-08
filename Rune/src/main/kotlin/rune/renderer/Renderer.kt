package rune.renderer

import glm_.glm
import glm_.mat4x4.Mat4
import glm_.vec3.Vec3
import glm_.vec4.Vec4
import rune.platforms.opengl.GLRendererAPI
import rune.platforms.opengl.RenderCommandQueue
import rune.renderer.gpu.*
import rune.renderer.renderer2d.FLOAT_MAT4_SIZE
import rune.renderer.renderer2d.FLOAT_VEC3_SIZE
import rune.renderer.renderer2d.Renderer2D
import rune.renderer.renderer3d.Mesh
import rune.rhi.Pipeline
import rune.rhi.PolygonMode
import rune.rhi.RenderPass

object Renderer {

    data class Config(
        val platform: RendererPlatform = RendererPlatform.OpenGL,
        /** Optional explicit backend instance (useful for tests, alternate backends, or custom wiring). */
        val backend: RendererAPI? = null
    )

    private var initialized: Boolean = false
    private lateinit var rendererAPI: RendererAPI
    private var platform: RendererPlatform = RendererPlatform.None

    // TODO: this should probably not live in Renderer.kt
    data class CameraData(
        var viewProjection: Mat4 = Mat4(1f),
        var skyProjection: Mat4 = Mat4(1f),
        var viewPosition: Vec3 = Vec3(1f)
    )

    //! STATISTICS
    data class Statistics(var drawCalls: Int = 0, var quadCount: Int = 0) {
        fun getTotalVertexCount(): Int = quadCount * 4
        fun getTotalIndexCount(): Int = quadCount * 6
    }

    private val cameraBuffer: CameraData = CameraData()
    private lateinit var cameraUniformBuffer: UniformBuffer

    /**
     * Initialize the renderer and select the backend.
     *
     * Must be called before creating GPU resources via `*.create(...)` factory methods.
     */
    fun init(config: Config = Config()) {
        if (initialized) return

        platform = config.platform
        rendererAPI = config.backend ?: when (platform) {
            RendererPlatform.OpenGL -> GLRendererAPI()
            RendererPlatform.None -> error("Renderer platform not configured.")
        }

        rendererAPI.init()

        initShaders()

        cameraUniformBuffer = UniformBuffer.create(FLOAT_MAT4_SIZE * 2 + FLOAT_VEC3_SIZE, U_CAMERA, name = "Camera")
        Renderer2D.init()
        initialized = true
    }

    //*///////////////////////////////////////////////////////////////*//
    //*//                       LOADING SHADERS                     //*//
    //*///////////////////////////////////////////////////////////////*//
    val shaderLib: ShaderLibrary = ShaderLibrary()

    private fun initShaders() {
        EngineShaders.root()
        with(shaderLib) {
            load(EngineShaders.pathFor("EquirectangularToSkybox.glsl"))
            load(EngineShaders.pathFor("Geometry.glsl"))
            load(EngineShaders.pathFor("Terrain.glsl"))
            load(EngineShaders.pathFor("Rune_PBR.glsl"))
            load(EngineShaders.pathFor("Renderer2D_Quad.glsl"))
            load(EngineShaders.pathFor("Renderer2D_Circle.glsl"))
            load(EngineShaders.pathFor("Renderer2D_Line.glsl"))
            load(EngineShaders.pathFor("StaticMesh.glsl"))
            load(EngineShaders.pathFor("AutoExposure.glsl"))
            load(EngineShaders.pathFor("Grid.glsl"))
            load(EngineShaders.pathFor("Tonemap.glsl"))
            load(EngineShaders.pathFor("Composite2D.glsl"))
        }
    }

    val stats = Statistics()

    fun resetStats() {
        stats.quadCount = 0
        stats.drawCalls = 0
    }

    fun getAPI(): RendererPlatform = platform

    internal fun requireInitialized() {
        check(initialized) {
            "Renderer is not initialized. Call Renderer.init() before creating GPU resources."
        }
    }

    fun beginScene(camera: RuneCamera, transform: Mat4) {
        cameraBuffer.viewProjection = camera.projection * glm.inverse(transform)
        cameraBuffer.skyProjection = camera.projection
        cameraBuffer.viewPosition = camera.position
        cameraUniformBuffer.setData(cameraBuffer.viewProjection)
        cameraUniformBuffer.setData(cameraBuffer.skyProjection, FLOAT_MAT4_SIZE)
        cameraUniformBuffer.setData(cameraBuffer.viewPosition, FLOAT_MAT4_SIZE * 2)

        Renderer2D.beginScene()
    }

    fun beginScene(camera: SceneViewportCamera) {
        cameraBuffer.viewProjection = camera.getViewProjection()
        cameraBuffer.skyProjection = camera.getSkyViewProjection()
        cameraBuffer.viewPosition = camera.position
        cameraUniformBuffer.setData(cameraBuffer.viewProjection)
        cameraUniformBuffer.setData(cameraBuffer.skyProjection, FLOAT_MAT4_SIZE)
        cameraUniformBuffer.setData(cameraBuffer.viewPosition, FLOAT_MAT4_SIZE * 2)

        Renderer2D.beginScene()
    }

    /** Replaces only `u_ViewProjection` (first mat4) in the camera UBO; leaves sky projection unchanged. */
    fun uploadViewProjection(viewProjection: Mat4) {
        cameraBuffer.viewProjection = viewProjection
        cameraUniformBuffer.setData(cameraBuffer.viewProjection, 0)
    }

    fun endScene(flushRenderer2D: Boolean = true) {
        if (flushRenderer2D) {
            Renderer2D.endScene()
        }
    }

    fun beginRenderPass(pass: RenderPass, clear: Boolean = false) {
        rendererAPI.beginRenderPass(pass, clear)
    }

    fun endRenderPass() {
        rendererAPI.endRenderPass()
    }

    fun renderStaticMesh(pipeline: Pipeline, mesh: Mesh, transform: Mat4) {
        rendererAPI.renderStaticMesh(pipeline, mesh, transform)
    }

    fun render() {
        RenderCommandQueue.flush()
    }

    fun getShader(name: String): Shader = shaderLib.get(name)

    fun onWindowResize(width: Int, height: Int) {
        rendererAPI.setViewport(0, 0, width, height)
    }

    fun setLineThickness(width: Float) {
        rendererAPI.setLineWidth(width)
    }

    fun drawLines(pass: RenderPass, vertexCount: Int) {
        rendererAPI.drawLines(pass, vertexCount)
    }

    fun drawIndexed(pass: RenderPass, indexCount: Int = 0) {
        rendererAPI.drawIndexed(pass, indexCount)
    }

    fun setClearColor(color: Vec4) {
        rendererAPI.setClearColor(color)
    }

    fun clear() {
        rendererAPI.clear()
    }

    fun createEnvironmentMap(file: String): Texture {
        return rendererAPI.createEnvironmentMap(file)
    }

    fun submitFullscreenQuad(pipeline: Pipeline) {
        rendererAPI.submitFullscreenQuad(pipeline)
    }

    fun toggleWireframe(mode: PolygonMode) {
        rendererAPI.toggleWireframe(mode)
    }
}