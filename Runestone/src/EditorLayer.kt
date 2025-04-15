package runestone

import com.github.quillraven.fleks.Entity
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
import rune.components.CameraComponent
import rune.components.ScriptComponent
import rune.components.SpriteRenderer
import rune.components.TransformComponent
import rune.core.*
import rune.events.Event
import rune.renderer.*
import rune.scene.Scene
import javax.xml.crypto.dsig.Transform

class EditorLayer: Layer("Sandbox2D") {
    private lateinit var texture: Texture2D

    //private val cameraController = OrthographicCameraController(1280.0f / 720.0f, true)
    private val color = Vec4(0.8, 0.2, 0.3, 1.0)

    private lateinit var framebuffer: Framebuffer
    private var viewportSize: Vec2 = Vec2(0f)

    private var viewportFocused = false
    private var viewportHovered = false

    private var activeScene = Scene()
    private lateinit var camera: Entity
    private lateinit var square: Entity

    override fun onAttach() {
        texture = Texture2D.create("assets/textures/checkerboard.png")

        // viewport
        val spec = FramebufferSpecification(
            width = 1280,
            height = 720,
        )
        framebuffer = Framebuffer.create(spec)

        // scene
        camera = activeScene.createEntity("Camera Entity")
        square = activeScene.createEntity("Test Square")

        with(activeScene.world) {
            camera.configure {
                //it += CameraComponent()
                it += ScriptComponent()
            }
            square.configure {
                it += SpriteRenderer(color)
            }
        }
    }

    override fun onDetach() {

    }

    override fun onUpdate(dt: Float) {
        // Resize
        val spec = framebuffer.getSpecification()

        if (viewportSize.x > 0f && viewportSize.y > 0f &&
            (spec.width != viewportSize.x.toInt() || spec.height != viewportSize.y.toInt())
        ) {
            framebuffer.resize(viewportSize.x.toInt(), viewportSize.y.toInt())
            //cameraController.onResize(viewportSize.x, viewportSize.y)

            activeScene.onViewportResize(viewportSize.x.toInt(), viewportSize.y.toInt())
        }

        // Update
        //if (viewportFocused)
            //cameraController.onUpdate(dt)

        // Render
        Renderer2D.resetStats()
        framebuffer.bind()
        RenderCommand.setClearColor(Vec4(0.1f, 0.1f, 0.1f, 1.0f))
        RenderCommand.clear()

        activeScene.onUpdate(dt)

        //Renderer2D.beginScene(cameraController.camera)
        //Renderer2D.endScene()

        framebuffer.unbind()
    }

    override fun onEvent(e: Event) {
        //cameraController.onEvent(e)

        if (Input.isKeyPressed(Key.Escape)) {
            Application.get().close()
        }

    }

    override fun onImGuiRender() {
        // dockspace
        val dockSpaceOpen = ImBoolean(true)
        val optFullscreenPersistent = true
        val dockspaceFlags: Int = ImGuiDockNodeFlags.None

        // using ImGuiWindowFlags.NoDocking to make the parent window not dockable into
        var windowFlags = ImGuiWindowFlags.MenuBar or ImGuiWindowFlags.NoDocking
        if (optFullscreenPersistent) {
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


        /**
         * Important: note that we proceed even if Begin() returns false (aka window is collapsed).
         * This is because we want to keep our DockSpace() active. If a DockSpace() is inactive,
         * all active windows docked into it will lose their parent and become undocked.
         * We cannot preserve the docking relationship between an active window and an inactive docking, otherwise
         * any change of dockspace/settings would lead to windows being stuck in limbo and never being visible.
         */
        ImGui.pushStyleVar(ImGuiStyleVar.WindowPadding, ImVec2(0.0f, 0.0f))
        ImGui.begin("Dockspace", dockSpaceOpen, windowFlags)
        ImGui.popStyleVar()

        if (optFullscreenPersistent)
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

        // camera
        val cameraTransform = with(activeScene.world) {
            camera[TransformComponent].transform[3]
        }
        val newTransform = floatArrayOf(cameraTransform.x, cameraTransform.y, cameraTransform.z)
        ImGui.dragFloat3("Camera Transform", newTransform)
        cameraTransform.x = newTransform[0]
        cameraTransform.y = newTransform[1]
        cameraTransform.z = newTransform[2]

        ImGui.end()


        ImGui.pushStyleVar(ImGuiStyleVar.WindowPadding, ImVec2(0f, 0f))
        ImGui.begin("Viewport")

        viewportFocused = ImGui.isWindowFocused()
        viewportHovered = ImGui.isWindowHovered()
        Application.get().getImGuiLayer().blockEvents(!viewportFocused or !viewportHovered)

        val viewportPanelSize = ImGui.getContentRegionAvail()
        viewportSize = Vec2(viewportPanelSize.x, viewportPanelSize.y)

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
