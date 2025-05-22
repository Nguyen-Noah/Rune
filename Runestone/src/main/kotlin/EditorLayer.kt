package runestone

import com.github.quillraven.fleks.Entity
import glm_.glm
import glm_.mat4x4.Mat4
import glm_.vec2.Vec2
import glm_.vec3.Vec3
import glm_.vec4.Vec4
import imgui.ImGui
import imgui.ImVec2
import imgui.ImVec4
import imgui.extension.imguizmo.ImGuizmo
import imgui.extension.imguizmo.flag.Mode
import imgui.extension.imguizmo.flag.Operation
import imgui.flag.*
import imgui.type.ImBoolean
import org.lwjgl.opengl.GL11.GL_LINEAR
import rune.asset.MeshImporter
import rune.components.*
import rune.core.*
import rune.events.Event
import rune.events.EventDispatcher
import rune.events.KeyPressedEvent
import rune.events.MouseButtonPressedEvent
import rune.platforms.opengl.OpenGLRendererAPI
import rune.renderer.*
import rune.renderer.gpu.*
import rune.renderer.renderer2d.Renderer2D
import rune.renderer.renderer3d.Renderer3D
import rune.scene.Scene
import rune.scene.serialization.SceneSerializer
import rune.utils.decomposeTransform
import runestone.panels.ContentBrowserPanel
import runestone.panels.SceneHierarchyPanel
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.extension

// TODO: create a scene renderer ref: https://youtu.be/U16wc8w8IA4?si=KzL82TfupQIPOq5z&t=2419

class EditorLayer: Layer("Sandbox2D") {
    enum class SceneState(val state: Int) {
        Play(0),
        Edit(1),
        Stop(2),
        Simulate(3),
    }

    // editor resources
    private val iconPlay     = Texture2D.create("src/main/resources/Icons/PlayButton.png",     filter = GL_LINEAR)
    private val iconStop     = Texture2D.create("src/main/resources/Icons/StopButton.png",     filter = GL_LINEAR)
    private val iconSimulate = Texture2D.create("src/main/resources/Icons/SimulateButton.png", filter = GL_LINEAR)
    private var icon: Texture2D = iconPlay

    //private val zelda = MeshImporter.importMesh("Zelda.dae")

    private lateinit var framebuffer: Framebuffer
    private var viewportSize: Vec2 = Vec2(0f)

    private var viewportFocused = false
    private var viewportHovered = false

    // ui
    private lateinit var sceneHierarchyPanel: SceneHierarchyPanel
    private lateinit var contentBrowserPanel: ContentBrowserPanel

    // scene
    private var activeScene = Scene()
    private var sceneState = SceneState.Edit
    private var editorScenePath: Path = Paths.get("")
    private var editorScene: Scene = activeScene

    private var gizmoType = -1
    val editorCamera = EditorCamera(30f, 1778f, 0.1f, 1000f)

    private var hoveredEntity: Entity? = null

    private val viewportBounds: Array<Vec2> = Array(2) { Vec2() }

    // TODO: remove this lmao -> see [[ContentBrowserPanel.kt]]
    private val assetsDirectory: String = "assets"

    private var showColliders: Boolean = false

    override fun onAttach() {
        val spec = framebuffer {
            width = 1280
            height = 720

            attachments {
                color(FramebufferTextureFormat.RGBA8)
                color(FramebufferTextureFormat.RED_INTEGER)
                depth(FramebufferTextureFormat.DEPTH24STENCIL8)
            }
        }
        framebuffer = Framebuffer.create(spec)

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
        contentBrowserPanel = ContentBrowserPanel()

        //! TEMP
        val zelda = activeScene.createEntity("Zelda")
        with(activeScene.world) {
            zelda.configure {
                it += StaticMeshComponent(MeshImporter.importMesh("Zelda.dae"))
            }
        }



        //SceneSerializer(activeScene).deserialize("C:\\Users\\nohan\\Desktop\\Projects\\Original\\Rune3D\\Runestone\\assets\\scenes\\thingy.rune")
    }

    override fun onUpdate(dt: Float) {
        // Resize
        val spec = framebuffer.getSpecification()

        if (viewportSize.x > 0f && viewportSize.y > 0f &&
            (spec.width != viewportSize.x.toInt() || spec.height != viewportSize.y.toInt())
        ) {
            framebuffer.resize(viewportSize.x.toInt(), viewportSize.y.toInt())
            editorCamera.setViewportSize(viewportSize.x, viewportSize.y)
            activeScene.onViewportResize(viewportSize.x.toInt(), viewportSize.y.toInt())
        }

        // Render
        Renderer.resetStats()
        framebuffer.bind()
        RenderCommand.setClearColor(Vec4(0.1f, 0.1f, 0.1f, 1.0f))
        RenderCommand.clear()

        // clear entity ID attachment to -1
        framebuffer.clearAttachment(1, -1)

        when (sceneState) {
            SceneState.Play -> {
                activeScene.onUpdateRuntime(dt)
            }
            SceneState.Edit -> {
                editorCamera.onUpdate(dt)

                activeScene.onUpdateEditor(dt, editorCamera)
            }
            SceneState.Simulate -> {
                editorCamera.onUpdate(dt)

                activeScene.onUpdateSimulation(dt, editorCamera)
            }
            else -> {}
        }

        val mp = ImGui.getMousePos()
        val mx   = (mp.x - viewportBounds[0].x).toInt()
        var my   = (mp.y - viewportBounds[0].y).toInt()

        my = (viewportSize.y - my).toInt()
        if ((mx >= 0) and (my >= 0) and
            (mx < viewportSize.x.toInt()) and (my < viewportSize.y.toInt())) {
            val pixelData = framebuffer.readPixel(1, mx, my)
            hoveredEntity = when (pixelData) {
                -1 -> null
                else ->  {
                    // corner case in the event an invalid entity is created
                    val e = Entity(pixelData, 0u)
                    if (activeScene.world.contains(e)) e else null
                }
            }
        }

        onOverlayRender()

        framebuffer.unbind()
    }

    private fun newScene() {
        activeScene = Scene()
        activeScene.onViewportResize(viewportSize.x.toInt(), viewportSize.y.toInt())
        sceneHierarchyPanel.setContext(activeScene)

        editorScenePath = Paths.get("")
    }

    private fun openScene() {
        val nfd = rune.utils.FileDialog()
        val filePath = nfd.openFile()

        if (filePath.isNotEmpty()) {
            openScene(Paths.get(filePath))
        }
    }

    private fun openScene(path: Path) {
        if (sceneState != SceneState.Edit)
            onSceneStop()

        if (path.extension != "rune") {
            Logger.warn("Could not load ${path.fileName} - not a scene file")
            return
        }

        activeScene = Scene()
        SceneSerializer(activeScene).deserialize(path.toString())

        // swap scenes and bookkeeping
        editorScene = Scene.copy(activeScene)
        editorScenePath = path

        // sync viewport and panels
        activeScene.onViewportResize(viewportSize.x.toInt(), viewportSize.y.toInt())
        sceneHierarchyPanel.setContext(activeScene)

        // clear stale attribs
        sceneHierarchyPanel.selectedEntity = null
        hoveredEntity = null
    }

    private fun saveScene() {
        if (editorScenePath.toString().isNotEmpty()) {
            serializeScene(activeScene, editorScenePath)
            Logger.info("Saved $activeScene to $editorScenePath")
        } else {
            saveSceneAs()
        }
    }

    private fun saveSceneAs() {
        val nfd = rune.utils.FileDialog()
        val filepath = nfd.saveAs("Untitled")

        if (filepath.isNotEmpty()) {
            SceneSerializer(activeScene).serialize(filepath)
            Logger.info("Saved new scene to $filepath")
        }
    }

    private fun serializeScene(scene: Scene, path: Path) {
        SceneSerializer(scene).serialize(path.toString())
    }

    private fun onKeyPressed(e: KeyPressedEvent): Boolean {
        if (e.isRepeat) return false

        val control = Input.isKeyPressed(Key.LeftControl) || Input.isKeyPressed(Key.RightControl)
        val shift = Input.isKeyPressed(Key.LeftShift) || Input.isKeyPressed(Key.RightShift)

        when (e.keyCode) {
            Key.N -> if (control) newScene()
            Key.O -> if (control) openScene()
            Key.S -> if (control) {
                if (shift) {
                    saveSceneAs()
                } else {
                    saveScene()
                }
            }
            else -> {}
        }

        // gizmos
        gizmoType = when (e.keyCode) {
            Key.Q ->  -1
            Key.T ->  Operation.TRANSLATE
            Key.S ->  Operation.SCALE
            Key.R ->  Operation.ROTATE
            else -> gizmoType
        }

        // scene commands
        when (e.keyCode) {
            Key.D -> {
                if (control)
                    onDuplicateEntity()
            }
            else -> {}
        }

        return true
    }

    private fun onMousePressed(e: MouseButtonPressedEvent): Boolean {
        if (e.button == MouseButton.Button0) {  // TODO: this should be MouseButton.LeftButton
            if (viewportHovered and !ImGuizmo.isOver() and !Input.isKeyPressed(Key.Space)) {    // TODO: probably have a boolean for when the camera is moving
                sceneHierarchyPanel.selectedEntity = hoveredEntity
            }
        }
        return true
    }

    override fun onEvent(e: Event) {
        if (Input.isKeyPressed(Key.Escape))
            Application.get().close()

        editorCamera.onEvent(e)

        val dispatcher = EventDispatcher(e)
        dispatcher.dispatch<KeyPressedEvent>(::onKeyPressed)
        dispatcher.dispatch<MouseButtonPressedEvent>(::onMousePressed)
    }

    private fun onScenePlay() {
        if (sceneState == SceneState.Simulate)
            onSceneStop()

        sceneState = SceneState.Play
        editorScene = Scene.copy(activeScene)
        activeScene.onRuntimeStart()

        sceneHierarchyPanel.setContext(activeScene)
    }

    private fun onSceneStop() {
        if (sceneState == SceneState.Play) {
            activeScene.onRuntimeStop()
        } else if (sceneState == SceneState.Simulate) {
            activeScene.onSimulationStop()
        }

        sceneState = SceneState.Edit

        activeScene = editorScene

        sceneHierarchyPanel.setContext(activeScene)
    }

    private fun onSceneSimulate() {
        if (sceneState == SceneState.Play)
            onSceneStop()

        sceneState = SceneState.Simulate
        editorScene = Scene.copy(activeScene)
        activeScene.onSimulationStart()

        sceneHierarchyPanel.setContext(activeScene)
    }

    private fun onDuplicateEntity() {
        if (sceneState != SceneState.Edit)
            return

        val selectedEntity = sceneHierarchyPanel.selectedEntity
        println(selectedEntity)
        if (selectedEntity != null)
            activeScene.duplicateEntity(selectedEntity)
    }

    private fun onOverlayRender() {
        if (sceneState == SceneState.Play) {
            val camera = activeScene.getPrimaryCameraEntity() ?: return
            with(activeScene.world) { Renderer.beginScene(camera[CameraComponent].camera, camera[TransformComponent].getTransform()) }
        } else {
            Renderer.beginScene(editorCamera)
        }

        // TODO: disable depth testing and render physics colliders after the rest of scene -> lets lines go through walls
        if (showColliders) {
            activeScene.world.family { all(TransformComponent, BoxCollider2DComponent) }.forEach { src ->
                val transformComp = src[TransformComponent]
                val bc2d = src[BoxCollider2DComponent]

                val translation = transformComp.translation + Vec3(bc2d.offset, 0.001f)
                val scale = transformComp.scale * Vec3(bc2d.size, 1f)

                val transform = glm.translate(Mat4(1f), translation) *
                        glm.rotate(Mat4(1f), transformComp.rotation.z, Vec3(0f, 0f, 1f)) *
                        glm.scale(Mat4(1f), scale)

                Renderer2D.drawRect(transform, Vec4(0f, 0.3f, 1f, 1f))
            }

            activeScene.world.family { all(TransformComponent, CircleCollider2DComponent) }.forEach { src ->
                val transformComp = src[TransformComponent]
                val cc2d = src[CircleCollider2DComponent]

                val translation = transformComp.translation + Vec3(cc2d.offset, 0.001f)
                val scale = transformComp.scale * Vec3(cc2d.radius * 2f)    // diameter

                val transform = glm.translate(Mat4(1f), translation) * glm.scale(Mat4(1f), scale)

                Renderer2D.drawCircle(transform, Vec4(0f, 0.3f, 1f, 1f), 0.05f)
            }
        }
        Renderer2D.endScene()
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
        contentBrowserPanel.onImGuiRender()

        ImGui.begin("Stats")

        // entity picking
        var name = ""
        if (hoveredEntity != null)
            name = with (activeScene.world) { hoveredEntity!![TagComponent].tag }
        ImGui.text("Hovered Entity: $name")

        // stats
        val stats = Renderer.stats
        ImGui.text("Renderer2D Stats:")
        ImGui.text("Draw Calls: ${stats.drawCalls}")
        ImGui.text("Quads: ${stats.quadCount}")
        ImGui.text("Vertices: ${stats.getTotalVertexCount()}")
        ImGui.text("Indices: ${stats.getTotalIndexCount()}")
        ImGui.text("FPS ${Application.get().getFPS()}")

        ImGui.end()

        ImGui.begin("Settings")

        val oldC = showColliders
        if (ImGui.checkbox("Show physics colliders", oldC))
            showColliders = !oldC

        ImGui.end()

        ImGui.pushStyleVar(ImGuiStyleVar.WindowPadding, ImVec2(0f, 0f))
        ImGui.begin("Viewport")

        // mouse picking
        val viewportScreenPos = ImGui.getCursorScreenPos()

        val avail = ImGui.getContentRegionAvail()
        val viewportW = avail.x
        val viewportH = avail.y

        viewportBounds[0] = Vec2(viewportScreenPos.x,           viewportScreenPos.y)
        viewportBounds[1] = Vec2(viewportScreenPos.x + viewportW, viewportScreenPos.y + viewportH)

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

        // drag n drop target
        if (ImGui.beginDragDropTarget()) {
            val payload: String? = ImGui.acceptDragDropPayload("CONTENT_BROWSER_ITEM")
            payload?.let {
                openScene(Paths.get("$assetsDirectory/$it"))
            }
            ImGui.endDragDropTarget()
        }

        // gizmos
        if (gizmoType != -1) {
            val selectedEntity = sceneHierarchyPanel.selectedEntity
            if (selectedEntity != null) {
                ImGuizmo.setOrthographic(false)     // TODO: check for orthographic or nah
                ImGuizmo.setDrawList()

                val windowWidth = ImGui.getWindowWidth()
                val windowHeight = ImGui.getWindowHeight()
                ImGuizmo.setRect(ImGui.getWindowPosX(), ImGui.getWindowPosY(), windowWidth, windowHeight)

                with (activeScene.world) {
                    // editorCamera
                    val cameraProjection: Mat4 = editorCamera.projection
                    val cameraView: Mat4 = editorCamera.viewMatrix

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

        // scene drag n drop
        if (ImGui.beginDragDropTarget()) {
            val payload: String? = ImGui.acceptDragDropPayload("CONTENT_BROWSER_ITEM")

            ImGui.endDragDropTarget()
        }


        ImGui.end()
        ImGui.popStyleVar()

        UI_Toolbar()

        ImGui.end()
    }

    private fun UI_Toolbar() {

        ImGui.pushStyleVar(ImGuiStyleVar.WindowPadding, ImVec2(0f, 2f))
        ImGui.pushStyleVar(ImGuiStyleVar.ItemInnerSpacing, ImVec2(0f, 0f))

        ImGui.pushStyleColor(ImGuiCol.Button, ImVec4(0f, 0f, 0f, 0f))

        val colors = ImGui.getStyle().colors
        val buttonHovered = colors[ImGuiCol.ButtonHovered]
        ImGui.pushStyleColor(ImGuiCol.ButtonHovered, ImVec4(buttonHovered.x, buttonHovered.y, buttonHovered.z, 0.5f))     // TODO: move this to extern file

        val buttonActive = colors[ImGuiCol.ButtonActive]
        ImGui.pushStyleColor(ImGuiCol.ButtonActive, ImVec4(buttonActive.x, buttonActive.y, buttonActive.z, 0.5f))     // TODO: move this to extern file

        val flags = ImGuiWindowFlags.NoDecoration or ImGuiWindowFlags.NoScrollbar or ImGuiWindowFlags.NoScrollWithMouse
        ImGui.begin("##toolbar", null, flags)

        val toolbarEnabled = (activeScene != null)

        val tintColor = ImVec4(1f, 1f, 1f, 1f)
        if (!toolbarEnabled)
            tintColor.w = 0.5f

        val size = ImGui.getWindowHeight() - 4

        icon = if (sceneState == SceneState.Edit || sceneState == SceneState.Simulate) iconPlay else iconStop
        ImGui.pushStyleVar(ImGuiStyleVar.FramePadding, ImVec2(0f, 0f))
        ImGui.setCursorPosX((ImGui.getWindowContentRegionMax().x * 0.5f) - (size * 0.5f))
        if (ImGui.imageButton("##playButton", icon.rendererID.toLong(), ImVec2(size, size), ImVec2(0f, 0f), ImVec2(1f, 1f), ImVec4(0f, 0f, 0f, 0f), tintColor) && toolbarEnabled) {
            if (sceneState == SceneState.Edit || sceneState == SceneState.Simulate) {
                onScenePlay()
            } else if (sceneState == SceneState.Play) {
                onSceneStop()
            }
        }
        ImGui.sameLine()

        icon = if (sceneState == SceneState.Edit || sceneState == SceneState.Play) iconSimulate else iconStop
        if (ImGui.imageButton("##simButton", icon.rendererID.toLong(), ImVec2(size, size), ImVec2(0f, 0f), ImVec2(1f, 1f), ImVec4(0f, 0f, 0f, 0f), tintColor) && toolbarEnabled) {
            if (sceneState == SceneState.Edit || sceneState == SceneState.Play) {
                onSceneSimulate()
            } else if (sceneState == SceneState.Simulate) {
                onSceneStop()
            }
        }

        ImGui.popStyleVar(3)
        ImGui.popStyleColor(3)
        ImGui.end()
    }
}

// extensions
private operator fun ImVec2.component1(): Float = x
private operator fun ImVec2.component2(): Float = y
