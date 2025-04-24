package runestone

import com.github.quillraven.fleks.Entity
import glm_.glm
import glm_.mat4x4.Mat4
import glm_.vec2.Vec2
import glm_.vec3.Vec3
import glm_.vec4.Vec4
import imgui.ImGui
import imgui.ImVec2
import imgui.extension.imguizmo.ImGuizmo
import imgui.extension.imguizmo.flag.Mode
import imgui.extension.imguizmo.flag.Operation
import imgui.flag.ImGuiConfigFlags
import imgui.flag.ImGuiDockNodeFlags
import imgui.flag.ImGuiStyleVar
import imgui.flag.ImGuiWindowFlags
import imgui.type.ImBoolean
import rune.components.CameraComponent
import rune.components.TransformComponent
import rune.core.*
import rune.events.Event
import rune.events.EventDispatcher
import rune.events.KeyPressedEvent
import rune.renderer.*
import rune.scene.Scene
import rune.scene.serialization.SceneSerializer
import rune.utils.FileDialog
import rune.utils.decomposeTransform
import runestone.panels.SceneHierarchyPanel
import javax.xml.crypto.dsig.Transform

class EditorLayer: Layer("Sandbox2D") {
    private lateinit var texture: Texture2D

    //private val cameraController = OrthographicCameraController(1280.0f / 720.0f, true)
    private val color = Vec4(0.8, 0.2, 0.3, 1.0)

    private lateinit var framebuffer: Framebuffer
    private var viewportSize: Vec2 = Vec2(0f)

    private var viewportFocused = false
    private var viewportHovered = false

    private lateinit var sceneHierarchyPanel: SceneHierarchyPanel

    private var activeScene = Scene()

    private var gizmoType = -1

    override fun onAttach() {
        texture = Texture2D.create("assets/textures/checkerboard.png")

        // viewport
        val spec = FramebufferSpecification(
            width = 1280,
            height = 720,
        )
        framebuffer = Framebuffer.create(spec)

        // scene
//        camera = activeScene.createEntity("Camera Entity")
//        var cameraScript: ScriptableEntity?
//        square = activeScene.createEntity("Test Square")
//        greenSquare = activeScene.createEntity("Green Square")
//
//        with(activeScene.world) {
//            camera.configure {
//                it += CameraComponent()
//                it += ScriptComponent()
//            }
//            square.configure {
//                it += SpriteRendererComponent(color)
//            }
//            greenSquare.configure {
//                it += SpriteRendererComponent(Vec4(0.3, 0.8, 0.2, 1.0))
//            }
//        }
//
//        // script loading
//        Coroutine(Dispatchers.IO).launchTask {
//            cameraScript = ScriptEngine.loadScript(File("C:\\Users\\nohan\\Desktop\\Projects\\Original\\Rune3D\\Runestone\\scripts\\CameraController.runescript.kts"))
//
//            activeScene.world.family { all(ScriptComponent) }
//            .forEach {entity ->
//                val scriptComp = entity[ScriptComponent]
//                cameraScript?.let {
//                    if (!scriptComp.isBound) {
//                        scriptComp.bind(cameraScript!!)
//                        scriptComp.instance.entity = entity
//                        scriptComp.instance.scene = activeScene
//                        scriptComp.instance.onCreate()
//                    }
//                }
//            }
//        }
//
        sceneHierarchyPanel = SceneHierarchyPanel(activeScene)

        SceneSerializer(activeScene).deserialize("C:\\Users\\nohan\\Desktop\\Projects\\Original\\Rune3D\\Runestone\\assets\\scenes\\3D.rune")
    }

    override fun onUpdate(dt: Float) {
        // Resize
        val spec = framebuffer.getSpecification()

        if (viewportSize.x > 0f && viewportSize.y > 0f &&
            (spec.width != viewportSize.x.toInt() || spec.height != viewportSize.y.toInt())
        ) {
            framebuffer.resize(viewportSize.x.toInt(), viewportSize.y.toInt())

            activeScene.onViewportResize(viewportSize.x.toInt(), viewportSize.y.toInt())
        }
        // Render
        Renderer2D.resetStats()
        framebuffer.bind()
        RenderCommand.setClearColor(Vec4(0.1f, 0.1f, 0.1f, 1.0f))
        RenderCommand.clear()

        activeScene.onUpdate(dt)

        framebuffer.unbind()
    }

    private fun newScene() {
        activeScene = Scene()
        activeScene.onViewportResize(viewportSize.x.toInt(), viewportSize.y.toInt())
        sceneHierarchyPanel.setContext(activeScene)
    }

    private fun openScene() {
        val nfd = rune.utils.FileDialog()
        val filePath = nfd.openFile()

        if (filePath.isNotEmpty()) {
            activeScene = Scene()
            activeScene.onViewportResize(viewportSize.x.toInt(), viewportSize.y.toInt())
            sceneHierarchyPanel.setContext(activeScene)

            // deserializing the scene
            val sceneSerializer = SceneSerializer(activeScene)
            sceneSerializer.deserialize(filePath)
        }
    }

    private fun saveSceneAs() {
        val nfd = rune.utils.FileDialog()
        val filepath = nfd.saveAs("Untitled")

        if (filepath.isNotEmpty()) {
            val sceneSerializer = SceneSerializer(activeScene)
            sceneSerializer.serialize(filepath)
        }
    }

    private fun onKeyPressed(e: KeyPressedEvent): Boolean {
        if (e.isRepeat) return false

        val control = Input.isKeyPressed(Key.LeftControl) || Input.isKeyPressed(Key.RightControl)
        val shift = Input.isKeyPressed(Key.LeftShift) || Input.isKeyPressed(Key.RightShift)

        when (e.keyCode) {
            Key.N -> if (control) newScene()
            Key.O -> if (control) openScene()
            Key.S -> if (control && shift) saveSceneAs()
            else -> {}
        }

        // gizmos
        when (e.keyCode) {
            Key.Q -> gizmoType = -1
            Key.T -> gizmoType = Operation.TRANSLATE
            Key.S -> gizmoType = Operation.SCALE
            Key.R -> gizmoType = Operation.ROTATE
            else -> {}
        }

        return true
    }

    override fun onEvent(e: Event) {
        if (Input.isKeyPressed(Key.Escape)) {
            Application.get().close()
        }

        val dispatcher = EventDispatcher(e)
        dispatcher.dispatch<KeyPressedEvent>(::onKeyPressed)

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

        val style = ImGui.getStyle()
        val minWinSize = style.windowMinSize
        style.setWindowMinSize(370f, 1f)

        if ((io.configFlags and ImGuiConfigFlags.DockingEnable) != 0) {
            val dockspaceId = ImGui.getID(("MyDockSpace"))
            ImGui.dockSpace(dockspaceId, ImVec2(0.0f, 0.0f), dockspaceFlags)
        }

        style.windowMinSize = minWinSize

        if (ImGui.beginMenuBar()) {
            if (ImGui.beginMenu("File")) {
                // Disabling fullscreen would allow the window to be moved to the front of other windows,
                // which we can't undo at the moment without finer window depth/z control.
                //ImGui::MenuItem("Fullscreen", NULL, &opt_fullscreen_persistant)
                if (ImGui.menuItem("New", "Ctrl+N")) {
                    newScene()
                }
                if (ImGui.menuItem("Open...", "Ctrl+O")) {
                    openScene()
                }
                if (ImGui.menuItem("Save As...", "Ctrl+Shift+S")) {
                    saveSceneAs()
                }

                if (ImGui.menuItem("Exit"))
                    Application.get().close()
                ImGui.endMenu()
            }
            ImGui.endMenuBar()
        }


        sceneHierarchyPanel.onImGuiRender()


        ImGui.begin("Stats")

        // stats
        val stats = Renderer2D.getStats()
        ImGui.text("Renderer2D Stats:")
        ImGui.text("Draw Calls: ${stats.drawCalls}")
        ImGui.text("Quads: ${stats.quadCount}")
        ImGui.text("Vertices: ${stats.getTotalVertexCount()}")
        ImGui.text("Indices: ${stats.getTotalIndexCount()}")
        ImGui.text("FPS ${Application.get().getFPS()}")

        ImGui.end()

        ImGui.pushStyleVar(ImGuiStyleVar.WindowPadding, ImVec2(0f, 0f))
        ImGui.begin("Viewport")

        viewportFocused = ImGui.isWindowFocused()
        viewportHovered = ImGui.isWindowHovered()
        Application.get().getImGuiLayer().blockEvents(!viewportFocused and !viewportHovered)

        val viewportPanelSize = ImGui.getContentRegionAvail()
        viewportSize = Vec2(viewportPanelSize.x, viewportPanelSize.y)

        ImGui.image(
            framebuffer.getColorAttachment().toLong(),
            ImVec2(viewportSize.x, viewportSize.y),
            ImVec2(0f, 1f),
            ImVec2(1f, 0f)
        )

        // gizmos
        if (gizmoType != -1) {
            val selectedEntity = sceneHierarchyPanel.selectedEntity
            if (selectedEntity != null) {
                ImGuizmo.setOrthographic(false)     // TODO: check for orthographic or nah
                ImGuizmo.setDrawList()

                val windowWidth = ImGui.getWindowWidth()
                val windowHeight = ImGui.getWindowHeight()
                ImGuizmo.setRect(ImGui.getWindowPosX(), ImGui.getWindowPosY(), windowWidth, windowHeight)

                val cameraEntity = activeScene.getPrimaryCameraEntity()
                with (activeScene.world) {
                    val camera = cameraEntity[CameraComponent].camera

                    val cameraProjection: Mat4 = camera.projection
                    val cameraView: Mat4 = glm.inverse(cameraEntity[TransformComponent].getTransform())

                    val entityTransform = selectedEntity[TransformComponent]
                    val transform: Mat4 = entityTransform.getTransform()

                    // snapping
                    val snap = Input.isKeyPressed(Key.LeftControl)
                    var snapValue = 0.5f
                    if (gizmoType == Operation.ROTATE)
                        snapValue = 45f

                    val snapValues = floatArrayOf(snapValue, snapValue, snapValue)

                    val oldTransform = transform.toFloatArray()
                    ImGuizmo.manipulate(
                        cameraView.toFloatArray(),
                        cameraProjection.toFloatArray(),
                        gizmoType,
                        Mode.LOCAL,
                        oldTransform,
                        null,
                        if (snap) { snapValues } else null
                    )

                    if (ImGuizmo.isUsing()) {
                        decomposeTransform(Mat4(oldTransform))?.let { dec ->
                            selectedEntity[TransformComponent].translation  = dec.translation

                            val deltaRotation = dec.rotation - entityTransform.rotation
                            selectedEntity[TransformComponent].rotation = selectedEntity[TransformComponent].rotation + deltaRotation

                            selectedEntity[TransformComponent].scale = dec.scale
                        }
                    }
                }
            }
        }


        ImGui.end()
        ImGui.popStyleVar()

        ImGui.end()
    }
}

// extensions
private operator fun ImVec2.component1(): Float = x
private operator fun ImVec2.component2(): Float = y
