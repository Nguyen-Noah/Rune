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
import rune.renderer.*
import rune.renderer.gpu.*
import rune.renderer.renderer2d.Renderer2D
import rune.scene.Scene
import rune.scene.SceneRenderer
import rune.scene.serialization.SceneSerializer
import rune.utils.decomposeTransform
import runestone.panels.ContentBrowserPanel
import runestone.panels.SceneHierarchyPanel
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.extension

// TODO: create a scene renderer ref: https://youtu.be/U16wc8w8IA4?si=KzL82TfupQIPOq5z&t=2419

class EditorLayer : Layer("Sandbox2D") {

    /* --------------------------------------------------------------------- */
    /*  Scene‑state machine                                                  */
    /* --------------------------------------------------------------------- */

    private sealed interface SceneState {
        fun onEnter(layer: EditorLayer) {}
        fun onExit(layer: EditorLayer) {}
        fun onUpdate(layer: EditorLayer, dt: Float) {}

        data object Edit : SceneState {
            override fun onUpdate(layer: EditorLayer, dt: Float) {
                layer.editorCamera.onUpdate(dt)
                layer.activeScene.onUpdateEditor(dt, layer.editorCamera)
            }
        }

        data object Play : SceneState {
            override fun onEnter(layer: EditorLayer) = layer.activeScene.onRuntimeStart()
            override fun onUpdate(layer: EditorLayer, dt: Float) = layer.activeScene.onUpdateRuntime(dt)
            override fun onExit(layer: EditorLayer) = layer.activeScene.onRuntimeStop()
        }

        data object Simulate : SceneState {
            override fun onEnter(layer: EditorLayer) = layer.activeScene.onRuntimeStart()
            override fun onUpdate(layer: EditorLayer, dt: Float) {
                layer.editorCamera.onUpdate(dt)
                layer.activeScene.onUpdateSimulation(dt, layer.editorCamera)
            }
            override fun onExit(layer: EditorLayer) = layer.activeScene.onSimulationStop()
        }
    }

    private var state: SceneState = SceneState.Edit
    private fun changeState(newState: SceneState, copy: (() -> Unit)? = null) {
        if (state === newState) return
        state.onExit(this)
        state = newState
        copy?.let { copy() }
        state.onEnter(this)
    }

    /* --------------------------------------------------------------------- */
    /*  Editor resources                                                     */
    /* --------------------------------------------------------------------- */

    private val iconPlay     = Texture2D.create("src/main/resources/Icons/PlayButton.png",     filter = GL_LINEAR)
    private val iconStop     = Texture2D.create("src/main/resources/Icons/StopButton.png",     filter = GL_LINEAR)
    private val iconSimulate = Texture2D.create("src/main/resources/Icons/SimulateButton.png", filter = GL_LINEAR)
    private var icon: Texture2D = iconPlay

    /* --------------------------------------------------------------------- */
    /*  Frame‑/viewport                                                      */
    /* --------------------------------------------------------------------- */

    private lateinit var framebuffer: Framebuffer
    private var viewportSize: Vec2 = Vec2(0f)

    private val viewportBounds: Array<Vec2> = Array(2) { Vec2() }
    private var viewportFocused = false
    private var viewportHovered = false

    /* --------------------------------------------------------------------- */
    /*  Scene / panels                                                       */
    /* --------------------------------------------------------------------- */

    private var activeScene = Scene()
    private var editorScene: Scene = activeScene
    private var editorScenePath: Path = Paths.get("")

    private val vRenderer = SceneRenderer(activeScene)

    private var sceneHierarchyPanel: SceneHierarchyPanel = SceneHierarchyPanel(activeScene)
    private var contentBrowserPanel: ContentBrowserPanel = ContentBrowserPanel()

    /* --------------------------------------------------------------------- */
    /*  Misc                                                                 */
    /* --------------------------------------------------------------------- */

    val editorCamera = EditorCamera(30f, 1778f, 0.1f, 1000f)
    private var gizmoType = -1
    private var hoveredEntity: Entity? = null
    private var showColliders: Boolean = false


    // TODO: remove this lmao -> see [[ContentBrowserPanel.kt]]
    private val assetsDirectory: String = "assets"


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

        //! TEMP
        activeScene.createEntity("Zelda").apply {
            with(activeScene.world) { configure { it += StaticMeshComponent(MeshImporter.importStaticMesh("totk/zelda_search.dae")) } }//"Zelda/Zelda.dae"
        }
//        activeScene.createEntity("Dempsey").apply {
//            with(activeScene.world) { configure { it += StaticMeshComponent(MeshImporter.importStaticMesh("dempsey/dempsey_playermodel.dae")) } }
//        }
    }

    override fun onUpdate(dt: Float) {
        // Resize
        updateFramebuffer()

        // Render
        Renderer.resetStats()
        framebuffer.bind()
        RenderCommand.run {
            setClearColor(Vec4(0.1f, 0.1f, 0.1f, 1.0f))
            clear()
        }

        // clear entity ID attachment to -1
        framebuffer.clearAttachment(1, -1)

        state.onUpdate(this, dt)

        updateMousePicking()
        renderOverlays()

        framebuffer.unbind()
    }

    private fun newScene() {
        activeScene = Scene().also { it.onViewportResize(viewportSize.x.toInt(), viewportSize.y.toInt()) }
        sceneHierarchyPanel.setContext(activeScene)
        editorScenePath = Paths.get("")
    }

    private fun openScene() {
        rune.utils.FileDialog().openFile()
            .takeIf { it.isNotEmpty() }
            ?.let { openScene(Paths.get(it)) }
    }

    private fun openScene(path: Path) {
        if (path.extension != "rune") {
            Logger.warn("Could not load ${path.fileName} - not a scene file")
            return
        }
        changeState(SceneState.Edit)
        activeScene = Scene().also { SceneSerializer(it).deserialize(path.toString()) }
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
        rune.utils.FileDialog().saveAs("Untitled")
            .takeIf { it.isNotEmpty() }
            ?.also { SceneSerializer(activeScene).serialize(it) }
        Logger.info("Saved new scene to ${'$'}it")
    }

    private fun serializeScene(scene: Scene, path: Path) {
        SceneSerializer(scene).serialize(path.toString())
    }

    private fun onKeyPressed(e: KeyPressedEvent): Boolean {
        if (e.isRepeat) return false

        val ctrl = Input.isKeyPressed(Key.LeftControl) || Input.isKeyPressed(Key.RightControl)
        val shift = Input.isKeyPressed(Key.LeftShift) || Input.isKeyPressed(Key.RightShift)


        when (e.keyCode) {
            Key.N -> if (ctrl) newScene()
            Key.O -> if (ctrl) openScene()
            Key.S -> if (ctrl) {
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
                if (ctrl)
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
        EventDispatcher(e).apply {
            dispatch<KeyPressedEvent>(::onKeyPressed)
            dispatch<MouseButtonPressedEvent>(::onMousePressed)
        }
    }

    private fun onDuplicateEntity() {
        if (state == SceneState.Edit)
            sceneHierarchyPanel.selectedEntity?.let(activeScene::duplicateEntity)
    }

    /* ===================================================================== */
    /*  ––––––––––––––––––––––––––  Private helpers  ––––––––––––––––––––––– */
    /* ===================================================================== */

    /* ---------- Framebuffer ---------- */

    private fun updateFramebuffer() {
        val spec = framebuffer.getSpecification()
        if (viewportSize.x > 0 && viewportSize.y > 0 &&
            (spec.width != viewportSize.x.toInt() || spec.height != viewportSize.y.toInt())) {
            framebuffer.resize(viewportSize.x.toInt(), viewportSize.y.toInt())
            editorCamera.setViewportSize(viewportSize.x, viewportSize.y)
            activeScene.onViewportResize(viewportSize.x.toInt(), viewportSize.y.toInt())
        }
    }

    /* ---------- Mouse picking ---------- */

    private fun updateMousePicking() {
        val (mxGlobal, myGlobal) = ImGui.getMousePos()
        val mx = (mxGlobal - viewportBounds[0].x).toInt()
        var my = (myGlobal - viewportBounds[0].y).toInt()
        my = (viewportSize.y - my).toInt()

        hoveredEntity = if (mx in 0 until viewportSize.x.toInt() && my in 0 until viewportSize.y.toInt()) {
            when (val id = framebuffer.readPixel(1, mx, my)) {
                -1   -> null
                else -> Entity(id, 0u).takeIf { activeScene.world.contains(it) }
            }
        } else null
    }

    /* ---------- Overlay rendering ---------- */

    private fun renderOverlays() {
        with(activeScene.world) {
            val (camera, cameraTransform) = when (state) {
                SceneState.Play       -> activeScene.getPrimaryCameraEntity()?.let { it[CameraComponent].camera to it[TransformComponent].getTransform() }
                else                  -> editorCamera to Mat4(1f)
            } ?: return

            Renderer.beginScene(camera, cameraTransform)
            if (showColliders) drawColliders()
            Renderer2D.endScene()
        }
    }

    private fun drawColliders() {
        with(activeScene.world) {
            family { all(TransformComponent, BoxCollider2DComponent) }.forEach { e ->
                val tComp = e[TransformComponent]
                val bc = e[BoxCollider2DComponent]
                val transform = glm.translate(Mat4(1f), tComp.translation + Vec3(bc.offset, 0.001f)) *
                        glm.rotate(Mat4(1f), tComp.rotation.z, Vec3(0f, 0f, 1f)) *
                        glm.scale(Mat4(1f), tComp.scale * Vec3(bc.size, 1f))
                Renderer2D.drawRect(transform, Vec4(0f, 0.3f, 1f, 1f))
            }
            family { all(TransformComponent, CircleCollider2DComponent) }.forEach { e ->
                val tComp = e[TransformComponent]
                val cc = e[CircleCollider2DComponent]
                val transform = glm.translate(Mat4(1f), tComp.translation + Vec3(cc.offset, 0.001f)) *
                        glm.scale(Mat4(1f), tComp.scale * Vec3(cc.radius * 2f))
                Renderer2D.drawCircle(transform, Vec4(0f, 0.3f, 1f, 1f), 0.05f)
            }
        }
    }

    /* ---------- ImGui: dockspace + windows ---------- */

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

        drawToolbar()

        ImGui.end()
    }

    private fun drawToolbar() {
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

        val size = ImGui.getWindowHeight() - 4

        ImGui.pushStyleVar(ImGuiStyleVar.FramePadding, ImVec2())
        ImGui.setCursorPosX((ImGui.getWindowContentRegionMax().x * 0.5f) - (size * 0.5f))
        icon = when (state) {
            SceneState.Edit, SceneState.Simulate -> iconPlay
            SceneState.Play                      -> iconStop
        }
        drawToolbarButton("##play", icon, size) { togglePlay() }
        ImGui.sameLine()

        icon = when (state) {
            SceneState.Edit, SceneState.Play -> iconSimulate
            SceneState.Simulate             -> iconStop
        }
        drawToolbarButton("##simulate", icon, size) { toggleSimulate() }

        ImGui.popStyleVar(3)
        ImGui.popStyleColor(3)

        ImGui.end()
    }

    private inline fun drawToolbarButton(id: String, icon: Texture2D, size: Float, onClick: () -> Unit) {
        val tintColor = ImVec4(1f, 1f, 1f, if (activeScene != null) 1f else 0.5f)
        if (ImGui.imageButton(id, icon.rendererID.toLong(), ImVec2(size, size), ImVec2(0f, 0f), ImVec2(1f, 1f), ImVec4(0f, 0f, 0f, 0f), tintColor))
            onClick()
    }

    private fun togglePlay() = when (state) {
        SceneState.Edit, SceneState.Simulate -> changeState(SceneState.Play).also { editorScene = Scene.copy(activeScene) }
        SceneState.Play                      -> changeState(SceneState.Edit).also { activeScene = editorScene }
    }

    private fun toggleSimulate() = when (state) {
        SceneState.Edit, SceneState.Play -> changeState(SceneState.Simulate).also { editorScene = Scene.copy(activeScene) }
        SceneState.Simulate              -> changeState(SceneState.Edit).also { activeScene = editorScene }
    }
}

// extensions
private operator fun ImVec2.component1(): Float = x
private operator fun ImVec2.component2(): Float = y
