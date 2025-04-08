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
    private lateinit var texture: Texture2D

    private var cameraController = OrthographicCameraController(1280.0f / 720.0f, true)
    private val color = Vec4(0.8, 0.2, 0.3, 1.0)

    var rotation = 0f

    override fun onAttach() {
        texture = Texture2D.create("assets/textures/checkerboard.png")
    }

    override fun onDetach() {

    }

    override fun onUpdate(dt: Float) {
        // Update
        cameraController.onUpdate(dt)

        // Render
        Renderer2D.resetStats()
        RenderCommand.setClearColor(Vec4(0.1f, 0.1f, 0.1f, 1.0f))
        RenderCommand.clear()

        Renderer2D.beginScene(cameraController.camera)

        Renderer2D.drawQuad(Vec2(0.0, 0.0), Vec2(1.0, 1.0), color)
        Renderer2D.drawQuad(
            Vec3(1.0, 1.0, -0.1),
            Vec2(8.0, 8.0),
            texture,
            tilingFactor = 10.0f,
            tintColor = Vec4(0.8f)
        )

        Renderer2D.drawRotatedQuad(
            Vec3(-2.0, 0.0, -0.1),
            Vec2(1.0, 1.0),
            45f,
            texture,
            tilingFactor = 20.0f,
            tintColor = Vec4(0.8f)
        )
        rotation += dt * 20f

        Renderer2D.drawRotatedQuad(
            Vec3(4.0, 0.0, -0.1),
            Vec2(2.0, 2.0),
            rotation,
            Vec4(0.8f, 0.3f, 0.2f, 1.0f)
        )

        Renderer2D.endScene()
    }

    override fun onEvent(e: Event) {
        cameraController.onEvent(e)
    }

    override fun onImGuiRender() {
        ImGui.begin("Settings")

        // stats
        val stats = Renderer2D.getStats()
        ImGui.text("Renderer2D Stats:")
        ImGui.text("Draw Calls: ${stats.drawCalls}")
        ImGui.text("Quads: ${stats.quadCount}")
        ImGui.text("Vertices: ${stats.getTotalVertexCount()}")
        ImGui.text("Indices: ${stats.getTotalIndexCount()}")

        // color
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