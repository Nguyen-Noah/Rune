package rune.renderer

import glm_.glm
import glm_.mat4x4.Mat4
import org.lwjgl.system.MemoryUtil
import rune.platforms.opengl.OpenGLShader
import rune.renderer.gpu.Shader
import rune.renderer.gpu.UniformBuffer
import rune.renderer.gpu.VertexArray
import rune.renderer.renderer2d.FLOAT_MAT4_SIZE
import rune.renderer.renderer2d.Renderer2D
import rune.renderer.renderer3d.Renderer3D

object Renderer {
    data class CameraData(var viewProjection: Mat4 = Mat4(1f))

    //! STATISTICS
    data class Statistics(var drawCalls: Int = 0, var quadCount: Int = 0) {
        fun getTotalVertexCount(): Int = quadCount * 4
        fun getTotalIndexCount(): Int = quadCount * 6
    }

    private val cameraBuffer: CameraData = CameraData()
    private val cameraUniformBuffer: UniformBuffer = UniformBuffer.create(FLOAT_MAT4_SIZE, 0)

    fun init() {
        initShaders()

        RenderCommand.init()
        Renderer2D.init()
        Renderer3D.init()
    }

    //*///////////////////////////////////////////////////////////////*//
    //*//                       LOADING SHADERS                     //*//
    //*///////////////////////////////////////////////////////////////*//
    val shaderLib: ShaderLibrary = ShaderLibrary()

    private fun initShaders() {
        // Renderer2D
        shaderLib.load("assets/shaders/Renderer2D_Quad.glsl")
        shaderLib.load("assets/shaders/Renderer2D_Circle.glsl")
        shaderLib.load("assets/shaders/Renderer2D_Line.glsl")

        // Renderer3D
        shaderLib.load("assets/shaders/StaticMesh.glsl")

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
        cameraUniformBuffer.setData(cameraBuffer.viewProjection)

        Renderer2D.beginScene()
        Renderer3D.beginScene()
    }

    fun beginScene(camera: EditorCamera) {
        // setting the uniform buffer
        cameraBuffer.viewProjection = camera.getViewProjection()
        cameraUniformBuffer.setData(cameraBuffer.viewProjection)

        Renderer2D.beginScene()
        Renderer3D.beginScene()
    }

    fun endScene() {
        Renderer2D.endScene()
        Renderer3D.endScene()
    }

    fun submit(shader: Shader, vao: VertexArray, transform: Mat4 = Mat4(1.0)) {
//        shader.bind()
//        // TODO: change this back once shaders are abstracted to different rendering platforms
//        (shader as OpenGLShader).uploadUniform {
//            uniform("u_ViewProjection", sceneData!!.viewProjectionMatrix)
//            uniform("u_ModelMatrix", transform)
//        }
//        vao.bind()
//        RenderCommand.drawIndexed(vao)
    }

    fun getShader(name: String): Shader = shaderLib.get(name)

    fun onWindowResize(width: Int, height: Int) {
        RenderCommand.setViewport(0, 0, width, height)
    }
}