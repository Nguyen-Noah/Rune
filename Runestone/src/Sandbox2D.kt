package runestone

import glm_.glm
import glm_.mat4x4.Mat4
import glm_.vec2.Vec2
import glm_.vec3.Vec3
import glm_.vec4.Vec4
import imgui.ImGui
import rune.core.Layer
import rune.events.Event
import rune.platforms.opengl.OpenGLShader
import rune.renderer.*

class Sandbox2D: Layer("Sandbox2D") {
    private lateinit var vao: VertexArray
    private lateinit var vbo: VertexBuffer
    private lateinit var ibo: IndexBuffer
    private lateinit var shader: Shader

    private var cameraController = OrthographicCameraController(1280.0f / 720.0f, true)
    private val color = Vec4(0.8, 0.2, 0.3, 1.0)

    override fun onAttach() {

    }

    override fun onDetach() {

    }

    override fun onUpdate(dt: Float) {
        cameraController.onUpdate(dt)

        RenderCommand.setClearColor(Vec4(0.1f, 0.1f, 0.1f, 1.0f))
        RenderCommand.clear()

        Renderer2D.beginScene(cameraController.camera)

        Renderer2D.drawQuad(Vec2(0.0, 0.0), Vec2(1.0, 1.0), Vec4(0.8, 0.3, 0.2, 1.0))

        Renderer2D.endScene()
    }

    override fun onEvent(e: Event) {
        cameraController.onEvent(e)
    }

    override fun onImGuiRender() {
        ImGui.begin("Settings")
        val col = floatArrayOf(color.r, color.g, color.b, color.a)
        if (ImGui.colorEdit4("Square Color", col)) {
            color.r = col[0]
            color.g = col[1]
            color.b = col[2]
            color.a = col[3]
        }
        ImGui.end()
    }
}