package rune.renderer

import glm_.glm
import glm_.mat4x4.Mat4
import glm_.vec4.Vec4
import rune.platforms.opengl.GLRendererAPI
import rune.renderer.gpu.*
import rune.renderer.renderer2d.FLOAT_MAT4_SIZE
import rune.renderer.renderer2d.Renderer2D
import rune.renderer.renderer3d.Mesh
import rune.rhi.Pipeline
import rune.rhi.RenderPass

data class RenderTask(
    val name: String,
    val exec: () -> Unit
) {
    override fun toString(): String {
        return name
    }
}

private val renderQueue: MutableList<RenderTask> = mutableListOf()

object Renderer {

    private val rendererAPI = when(RendererAPI.getAPI()) {
        RendererPlatform.OpenGL -> GLRendererAPI()
        RendererPlatform.None -> TODO()
    }

    data class CameraData(
        var viewProjection: Mat4 = Mat4(1f),
        var skyProjection: Mat4 = Mat4(1f)
    )

    //! STATISTICS
    data class Statistics(var drawCalls: Int = 0, var quadCount: Int = 0) {
        fun getTotalVertexCount(): Int = quadCount * 4
        fun getTotalIndexCount(): Int = quadCount * 6
    }

    private val cameraBuffer: CameraData = CameraData()
    private val cameraUniformBuffer: UniformBuffer = UniformBuffer.create(FLOAT_MAT4_SIZE * 2, U_CAMERA, name = "Camera")

    fun init() {
        initShaders()

        Renderer2D.init()
    }

    //*///////////////////////////////////////////////////////////////*//
    //*//                       LOADING SHADERS                     //*//
    //*///////////////////////////////////////////////////////////////*//
    val shaderLib: ShaderLibrary = ShaderLibrary()

    private fun initShaders() {
        // Compute
        shaderLib.load("assets/shaders/EquirectangularToSkybox.glsl")

        // Skybox
        shaderLib.load("assets/shaders/Skybox.glsl")

        // Renderer2D
        shaderLib.load("assets/shaders/Renderer2D_Quad.glsl")
        shaderLib.load("assets/shaders/Renderer2D_Circle.glsl")
        shaderLib.load("assets/shaders/Renderer2D_Line.glsl")

        // Renderer3D
        shaderLib.load("assets/shaders/StaticMesh.glsl")

        // Runestone
        shaderLib.load("assets/shaders/Grid.glsl")

        println(shaderLib)
    }


    val stats = Statistics()

    fun resetStats() {
        stats.quadCount = 0
        stats.drawCalls = 0
    }



    fun getAPI() = RendererAPI.getAPI()

    fun beginScene(camera: RuneCamera, transform: Mat4) {
        cameraBuffer.viewProjection = camera.projection * glm.inverse(transform)
        cameraBuffer.skyProjection = camera.projection
        cameraUniformBuffer.setData(cameraBuffer.viewProjection)
        println(cameraBuffer.skyProjection)
        cameraUniformBuffer.setData(cameraBuffer.skyProjection, FLOAT_MAT4_SIZE)

        Renderer2D.beginScene()
    }

    fun beginScene(camera: EditorCamera) {
        // setting the uniform buffer
        cameraBuffer.viewProjection = camera.getViewProjection()
        cameraBuffer.skyProjection = camera.getSkyViewProjection()
        cameraUniformBuffer.setData(cameraBuffer.viewProjection)
        cameraUniformBuffer.setData(cameraBuffer.skyProjection, FLOAT_MAT4_SIZE)

        Renderer2D.beginScene()
    }

    fun endScene() {
        Renderer2D.endScene()
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
        renderQueue.forEach { task ->
            task.exec()
        }
        renderQueue.clear()
    }

    fun getShader(name: String): Shader = shaderLib.get(name)

    fun onWindowResize(width: Int, height: Int) {
        rendererAPI.setViewport(0, 0, width, height)
    }

    fun setLineThickness(width: Float) {
        rendererAPI.setLineWidth(width)
    }

    fun drawLines(vao: VertexArray, vertexCount: Int) {
        rendererAPI.drawLines(vao, vertexCount)
    }

    fun drawIndexed(vao: VertexArray, indexCount: Int = 0) {
        rendererAPI.drawIndexed(vao, indexCount)
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

    fun renderSkybox(pass: RenderPass, envMap: Texture) {
        rendererAPI.renderSkybox(pass, envMap)
    }
}

fun SubmitRender(name: String = "Unnamed task", exec: () -> Unit) {
    renderQueue += RenderTask(name, exec)
}