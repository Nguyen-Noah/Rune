package runestone

import glm_.glm
import glm_.vec2.Vec2
import glm_.vec3.Vec3
import glm_.vec4.Vec4
import imgui.ImGui
import rune.core.*
import rune.events.Event
import rune.renderer.*

class Sandbox2D: Layer("Sandbox2D") {
    private lateinit var texture: Texture2D
    private lateinit var spritesheet: Texture2D

    private val cameraController = OrthographicCameraController(1280.0f / 720.0f, true)
    private val color = Vec4(0.8, 0.2, 0.3, 1.0)

    private var rotation = 0f

    private val prop: ParticleProps = ParticleProps()
    private val particleSystem = ParticleSystem()

    override fun onAttach() {
        texture = Texture2D.create("assets/textures/checkerboard.png")
        spritesheet = Texture2D.create("assets/textures/RPGpack_sheet_2X.png")

        // particle
        prop.colorBegin = Vec4(254/255f, 212/255f, 123/255f, 1f)
        prop.colorEnd = Vec4(254/255f, 109/255f, 41/255f, 1f)
        prop.sizeBegin = 0.5f
        prop.sizeVariation = 0.3f
        prop.sizeEnd = 0f
        prop.lifeTime = 1f
        prop.velocity = Vec2(0f, 0f)
        prop.velocityVariation = Vec2(2f, 1f)
        prop.position = Vec2(0f, 0f)
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

//        Renderer2D.beginScene(cameraController.camera)
//
//        Renderer2D.drawQuad(Vec2(0.0, 0.0), Vec2(1.0, 1.0), color)
//        Renderer2D.drawQuad(
//            Vec3(1.0, 1.0, -0.1),
//            Vec2(8.0, 8.0),
//            texture,
//            tilingFactor = 10.0f,
//            tintColor = Vec4(0.8f)
//        )
//
//        Renderer2D.drawRotatedQuad(
//            Vec3(-2.0, 0.0, -0.1),
//            Vec2(1.0, 1.0),
//            glm.radians(45f),
//            texture,
//            tilingFactor = 20.0f,
//            tintColor = Vec4(0.8f)
//        )
//        rotation += dt * 20f
//
//        Renderer2D.drawRotatedQuad(
//            Vec3(4.0, 0.0, -0.1),
//            Vec2(2.0, 2.0),
//            glm.radians(rotation),
//            Vec4(0.8f, 0.3f, 0.2f, 1.0f)
//        )
//
//        Renderer2D.endScene()

        if (Input.isMouseButtonPressed(MouseButton.ButtonLeft)) {
            var (x, y) = Input.getMousePosition()
            val width = Application.get().getWindow().width
            val height = Application.get().getWindow().height

            val bounds = cameraController.getBounds()
            val pos = cameraController.camera.getPosition()
            x = (x / width) * bounds.getWidth() - bounds.getWidth() * 0.5f
            y = bounds.getHeight() * 0.5f - (y / height) * bounds.getHeight()
            prop.position = Vec2(x + pos.x, y + pos.y)
            for (i in 0 until 5) {
                particleSystem.emit(prop)
            }
        }

        Renderer2D.beginScene(cameraController.camera)

        Renderer2D.drawQuad(Vec3(0f), Vec2(1f), spritesheet)

        Renderer2D.endScene()

        particleSystem.onUpdate(dt)
        particleSystem.onRender(cameraController.camera)
    }

    override fun onEvent(e: Event) {
        cameraController.onEvent(e)

        if (Input.isKeyPressed(Key.Escape)) {
            Application.get().close()
        }

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
        ImGui.text("FPS ${Application.get().getFPS()}")

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