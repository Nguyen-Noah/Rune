package runestone

import glm_.glm
import glm_.mat4x4.Mat4
import glm_.vec3.Vec3
import glm_.vec4.Vec4
import imgui.ImGui as imgui
import rune.*
import rune.core.Input
import rune.core.Key
import rune.events.Event
import rune.platforms.opengl.OpenGLShader
import rune.renderer.*
import rune.rune.renderer.RenderCommand


class Test : Layer("Test") {
    private val shaderLib = ShaderLibrary()

    private var shader: Shader
    private var texture: Texture2D
    private var logoTex: Texture2D
    private var vao: VertexArray
    private var vbo: VertexBuffer
    private var ibo: IndexBuffer

    private var cameraController = OrthographicCameraController(1280.0f / 720.0f, true)
    private var cameraPos = Vec3(0.0)
    private val cameraSpeed = 0.5f
    private var cameraRotation = 0.0f

    private var transform = Vec3(0.0)

    private val red = Vec4(0.8, 0.2, 0.3, 1.0)

    init {
        val vertices = floatArrayOf(
            -0.5f, -0.5f, 0.0f, 0.0f, 0.0f,
             0.5f, -0.5f, 0.0f, 1.0f, 0.0f,
             0.5f,  0.5f, 0.0f, 1.0f, 1.0f,
            -0.5f,  0.5f, 0.0f, 0.0f, 1.0f
        )

        vbo = VertexBuffer.create(vertices, vertices.size)
        ibo = IndexBuffer.create(intArrayOf(0, 1, 2, 2, 3, 0), 6)

        vao = VertexArray.create(vbo, bufferLayout {
            attribute("a_Position", 3)
            attribute("a_TexCoord", 2)
        })
        vao.setIndexBuffer(ibo)

        shader = shaderLib.load("assets/shaders/Texture.glsl")
        texture = Texture2D.create("assets/textures/checkerboard.png")
        logoTex = Texture2D.create("assets/textures/logo.png")

        (shader as OpenGLShader).uploadUniform {
            uniform("u_Texture", 0)
        }
    }
    override fun onUpdate(dt: Float) {
        cameraController.onUpdate(dt)

        RenderCommand.setClearColor(Vec4(0.1f, 0.1f, 0.1f, 1.0f))
        RenderCommand.clear()

        Renderer.beginScene(cameraController.camera)

        texture.bind()
        Renderer.submit(shader, vao, glm.scale(Mat4(1.0), Vec3(1.5)))
        logoTex.bind()
        Renderer.submit(shader, vao, glm.translate(Mat4(1.0), Vec3(0.25, -0.25, 0.0)) *glm.scale(Mat4(1.0), Vec3(1.5)))

        Renderer.endScene()
    }

    override fun onEvent(event: Event) {
        cameraController.onEvent(event)
    }

    override fun onImGuiRender() {
        imgui.begin("Settings")
        val col = floatArrayOf(red.x, red.y, red.z)
        if (imgui.colorEdit3("Square Color", col)) {
            red.x = col[0]
            red.y = col[1]
            red.z = col[2]
        }
        imgui.end()
    }
}

class Runestone : Application() {
    init {
        println("Runestone initialized.")
        pushOverlay(Test())
    }
}

fun main() {
    val app: Application = Runestone()
    app.run()
}