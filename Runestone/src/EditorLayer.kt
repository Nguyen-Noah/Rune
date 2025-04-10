package runestone

import glm_.glm
import glm_.vec2.Vec2
import glm_.vec3.Vec3
import glm_.vec4.Vec4
import imgui.ImGui
import imgui.ImVec2
import imgui.flag.ImGuiConfigFlags
import imgui.flag.ImGuiDockNodeFlags
import imgui.flag.ImGuiStyleVar
import imgui.flag.ImGuiWindowFlags
import imgui.type.ImBoolean
import rune.core.*
import rune.events.Event
import rune.renderer.*

class EditorLayer: Layer("Sandbox2D") {
    private lateinit var texture: Texture2D

    private val cameraController = OrthographicCameraController(1280.0f / 720.0f, true)
    private val color = Vec4(0.8, 0.2, 0.3, 1.0)

    private var rotation = 0f

    private lateinit var framebuffer: Framebuffer
    private var viewportSize: Vec2 = Vec2(0f)

    private val prop: ParticleProps = ParticleProps()
    private val particleSystem = ParticleSystem()

    override fun onAttach() {
        texture = Texture2D.create("assets/textures/checkerboard.png")

        val spec = FramebufferSpecification(
            width = 1280,
            height = 720,
        )
        framebuffer = Framebuffer.create(spec)

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
        framebuffer.bind()
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
            glm.radians(45f),
            texture,
            tilingFactor = 20.0f,
            tintColor = Vec4(0.8f)
        )
        rotation += dt * 20f

        Renderer2D.drawRotatedQuad(
            Vec3(4.0, 0.0, -0.1),
            Vec2(2.0, 2.0),
            glm.radians(rotation),
            Vec4(0.8f, 0.3f, 0.2f, 1.0f)
        )

        var yp = -5f
        while (yp <= 5f) {
            var xp = -5f
            while (xp <= 5f) {
                val color = Vec4((xp + 5f) / 10, 0.4f, (yp + 5f) / 10f, 0.7f)
                Renderer2D.drawQuad(Vec2(xp, yp), Vec2(0.45f), color)
                xp += 0.5f
            }
            yp += 0.5f
        }

        Renderer2D.endScene()

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

        particleSystem.onUpdate(dt)
        particleSystem.onRender(cameraController.camera)

        framebuffer.unbind()
    }

    override fun onEvent(e: Event) {
        cameraController.onEvent(e)

        if (Input.isKeyPressed(Key.Escape)) {
            Application.get().close()
        }

    }

    override fun onImGuiRender() {
        // dockspace
        val dockSpaceOpen = ImBoolean(true)
        val optFullscreenPersistent = true
        val optFullscreen = optFullscreenPersistent
        val dockspaceFlags: Int = ImGuiDockNodeFlags.None

        // using ImGuiWindowFlags.NoDocking to make the parent window not dockable into
        var windowFlags = ImGuiWindowFlags.MenuBar or ImGuiWindowFlags.NoDocking
        if (optFullscreen) {
            val viewport = ImGui.getMainViewport()
            ImGui.setNextWindowPos(viewport.pos)
            ImGui.setNextWindowSize(viewport.size)
            ImGui.setNextWindowViewport(viewport.id)
            ImGui.pushStyleVar(ImGuiStyleVar.WindowRounding, 0.0f)
            ImGui.pushStyleVar(ImGuiStyleVar.WindowBorderSize, 0.0f)

            windowFlags = windowFlags or ImGuiWindowFlags.NoTitleBar or
                    ImGuiWindowFlags.NoCollapse or
                    ImGuiWindowFlags.NoResize or
                    ImGuiWindowFlags.NoMove or
                    ImGuiWindowFlags.NoBringToFrontOnFocus or
                    ImGuiWindowFlags.NoNavFocus
        }

        // when using ImGuiDockNodeFlags.PassthruCentralNode, DockSpace() will render our background and handle the pass-thru hole, so we ask Begin() to not render a background
        if ((dockspaceFlags and ImGuiDockNodeFlags.PassthruCentralNode) != 0)
            windowFlags = windowFlags or ImGuiWindowFlags.NoBackground

        // Important: note that we proceed even if Begin() returns false (aka window is collapsed).
        // This is because we want to keep our DockSpace() active. If a DockSpace() is inactive,
        // all active windows docked into it will lose their parent and become undocked.
        // We cannot preserve the docking relationship between an active window and an inactive docking, otherwise
        // any change of dockspace/settings would lead to windows being stuck in limbo and never being visible.
        ImGui.pushStyleVar(ImGuiStyleVar.WindowPadding, ImVec2(0.0f, 0.0f))
        ImGui.begin("Dockspace", dockSpaceOpen, windowFlags)
        ImGui.popStyleVar()

        if (optFullscreen)
            ImGui.popStyleVar(2)

        val io = ImGui.getIO()
        if ((io.configFlags and ImGuiConfigFlags.DockingEnable) != 0) {
            val dockspaceId = ImGui.getID(("MyDockSpace"))
            ImGui.dockSpace(dockspaceId, ImVec2(0.0f, 0.0f), dockspaceFlags)
        }

        if (ImGui.beginMenuBar()) {
            if (ImGui.beginMenu("File")) {
                // Disabling fullscreen would allow the window to be moved to the front of other windows,
                // which we can't undo at the moment without finer window depth/z control.
                //ImGui::MenuItem("Fullscreen", NULL, &opt_fullscreen_persistant)
                if (ImGui.menuItem("Exit"))
                    Application.get().close()
                ImGui.endMenu()
            }
            ImGui.endMenuBar()
        }

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


        ImGui.pushStyleVar(ImGuiStyleVar.WindowPadding, ImVec2(0f, 0f))
        ImGui.begin("Viewport")

        ImGui.getContentRegionAvail().also { (w, h) ->
            if (w > 0 && h > 0 && (w.toInt() != viewportSize.x.toInt() || h.toInt() != viewportSize.y.toInt())) {
                viewportSize.put(w, h)
                framebuffer.resize(w.toInt(), h.toInt())

                cameraController.onResize(w, h)
            }
        }

        ImGui.image(
            framebuffer.getColorAttachment().toLong(),
            ImVec2(viewportSize.x, viewportSize.y),
            ImVec2(0f, 1f),
            ImVec2(1f, 0f)
        )
        ImGui.end()
        ImGui.popStyleVar()

        ImGui.end()
    }
}

private operator fun ImVec2.component1(): Float = x
private operator fun ImVec2.component2(): Float = y
