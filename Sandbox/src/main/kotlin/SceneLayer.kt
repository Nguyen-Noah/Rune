package sandbox

import glm_.glm
import glm_.mat4x4.Mat4
import glm_.vec2.Vec2
import glm_.vec3.Vec3
import imgui.ImGui
import imgui.ImVec2
import imgui.extension.imguizmo.ImGuizmo
import imgui.extension.imguizmo.flag.Mode
import imgui.extension.imguizmo.flag.Operation
import imgui.flag.ImGuiCol
import imgui.flag.ImGuiStyleVar
import imgui.flag.ImGuiWindowFlags
import rune.imgui.RuneGui
import rune.asset.MeshImporter
import rune.components.DirectionalLightComponent
import rune.components.StaticMeshComponent
import rune.components.TerrainComponent
import rune.components.TransformComponent
import rune.terrain.ProceduralTerrainParams
import rune.terrain.TerrainSystem
import rune.core.Application
import rune.core.Input
import rune.core.Key
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.acos
import rune.core.Layer
import rune.events.Event
import rune.events.EventDispatcher
import rune.events.WindowResizeEvent
import rune.project.ProjectManager
import rune.project.ProjectRoots
import rune.renderer.FlyCamera
import rune.renderer.Renderer
import rune.scene.Scene
import rune.scene.SceneRenderer
import rune.scene.SceneRendererSpec
import sandbox.panels.IrisSettings
import sandbox.panels.SceneHierarchyPanel

class SceneLayer : Layer("Scene") {

    val project = ProjectManager.open(ProjectRoots.resolve("SandboxProject"))
    val assetManager = ProjectManager.getEditorAssetManager()

    private val scene = Scene()

    private val camera = FlyCamera(fov = 30f, aspectRatio = 16f / 9f, nearClip = 0.1f, farClip = 1000f)

    private val vRenderer = SceneRenderer(scene, SceneRendererSpec(viewportWidth = 1280, viewportHeight = 720))

    private var viewportSize = Vec2(1280f, 720f)

    private val irisSettings = IrisSettings()
    private val sceneHierarchyPanel = SceneHierarchyPanel(scene)

    override fun onAttach() {
        val w = Application.get().getWindow().width
        val h = Application.get().getWindow().height
        viewportSize = Vec2(w.toFloat(), h.toFloat())
        camera.setViewportSize(viewportSize.x, viewportSize.y)
        scene.onViewportResize(w, h)
        vRenderer.resizeViewport(w, h)

        scene.createEntity("Zelda").apply {
            with(scene.world) {
                configure {
                    it += StaticMeshComponent(MeshImporter.importStaticMesh("totk/zelda_search.dae"))
                    it += TransformComponent()
                }
            }
        }

//        scene.createEntity("BOTW-Zelda").apply {
//            with(scene.world) {
//                configure {
//                    it += StaticMeshComponent(MeshImporter.importStaticMesh("VahRuta/Ruta.dae"))
//                    it += TransformComponent()
//                }
//            }
//        }

        val grid = 200
        val procedural = ProceduralTerrainParams(
            seed = 91421L,
            frequency = 0.038f,
            octaves = 6,
            persistence = 0.52f,
            lacunarity = 2.1f,
            heightScale = 14f,
        )
        val terrainConfig = TerrainSystem.createProceduralConfig(
            gridX = grid,
            gridZ = grid,
            sizeX = 48f,
            sizeZ = 48f,
            params = procedural,
            meshName = "SandboxTerrain",
        )
        val terrainModel = TerrainSystem.createModel(terrainConfig)
        scene.createEntity("Terrain").apply {
            with(scene.world) {
                configure {
                    it += TerrainComponent(terrainModel)
                    it += TransformComponent()
                }
            }
        }

        scene.createEntity("Light").apply {
            with(scene.world) {
                configure {
                    it += DirectionalLightComponent(
                        color = Vec3(0.976f, 0.878, 0.741),
                        diffuseIntensity = 1f,
                        direction = Vec3(-2.650f, -1.950f, -1.1f)
                    )
                    it += TransformComponent(translation = Vec3(0f, 32f, 0f))
                }
            }
        }
    }

    override fun onUpdate(dt: Float) {
        resizeIfNeeded()
        irisSettings.pushRenderSettings()

        camera.onUpdate(dt)
        Renderer.resetStats()
        scene.onUpdateEditor(dt, camera)
        vRenderer.render(dt, camera, irisSettings.renderSettings.renderWireframe)
    }

    override fun onEvent(e: Event) {
        if (Input.isKeyPressed(Key.Escape)) {
            Application.get().close()
        }
        camera.onEvent(e)
        EventDispatcher(e).dispatch<WindowResizeEvent>(::onWindowResize)
    }

    private fun onWindowResize(e: WindowResizeEvent): Boolean {
        viewportSize = Vec2(e.width.toFloat(), e.height.toFloat())
        return false
    }

    private fun resizeIfNeeded() {
        val w = viewportSize.x.toInt()
        val h = viewportSize.y.toInt()
        if (w > 0 && h > 0 && (w != vRenderer.finalFramebuffer.spec.width || h != vRenderer.finalFramebuffer.spec.height)) {
            vRenderer.resizeViewport(w, h)
            camera.setViewportSize(viewportSize.x, viewportSize.y)
            scene.onViewportResize(w, h)
        }
    }

    override fun onImGuiRender() {
        val viewport = RuneGui.mainViewport()

        RuneGui.setNextWindowPos(viewport.pos)
        RuneGui.setNextWindowSize(viewport.size)
        RuneGui.withStyleVars(
            ImGuiStyleVar.WindowRounding to 0f,
            ImGuiStyleVar.WindowBorderSize to 0f
        ) {
            RuneGui.withStyleColor(ImGuiCol.WindowBg, 0f, 0f, 0f, 1f) {
                RuneGui.window(
                    "SandboxViewport",
                    ImGuiWindowFlags.NoTitleBar or
                        ImGuiWindowFlags.NoCollapse or
                        ImGuiWindowFlags.NoResize or
                        ImGuiWindowFlags.NoMove or
                        ImGuiWindowFlags.NoBringToFrontOnFocus or
                        ImGuiWindowFlags.NoNavFocus
                ) {
                    val panel = RuneGui.contentRegionAvail()
                    viewportSize = Vec2(panel.x, panel.y)

                    RuneGui.image(
                        vRenderer.finalFramebuffer.getColorAttachment().toLong(),
                        ImVec2(panel.x, panel.y),
                        ImVec2(0f, 1f),
                        ImVec2(1f, 0f)
                    )

                    val selected = sceneHierarchyPanel.selectedEntity
                    if (selected != null) {
                        with(scene.world) {
                            if (selected.has(DirectionalLightComponent) && selected.has(TransformComponent)) {
                                ImGuizmo.setOrthographic(false)
                                ImGuizmo.setDrawList()
                                ImGuizmo.setRect(
                                    ImGui.getWindowPosX(),
                                    ImGui.getWindowPosY(),
                                    ImGui.getWindowWidth(),
                                    ImGui.getWindowHeight()
                                )

                                val pos = selected[TransformComponent].translation
                                val light = selected[DirectionalLightComponent]
                                val matrix = directionalLightGizmoMatrix(pos, light.direction)
                                val matrixArr = matrix.toFloatArray()
                                val snap = Input.isKeyPressed(Key.LeftControl)
                                ImGuizmo.manipulate(
                                    camera.viewMatrix.toFloatArray(),
                                    camera.projection.toFloatArray(),
                                    Operation.ROTATE,
                                    Mode.LOCAL,
                                    matrixArr,
                                    null,
                                    if (snap) floatArrayOf(45f, 45f, 45f) else null
                                )
                                if (ImGuizmo.isUsing()) {
                                    light.direction = travelDirectionFromGizmoMatrix(Mat4(matrixArr))
                                }
                            }
                        }
                    }
                }
            }
        }

        irisSettings.onImGuiRender()
        sceneHierarchyPanel.onImGuiRender()
    }
}

/** Local -Y maps to [rune.components.DirectionalLightComponent.direction] (light travel; see Rune_PBR). */
private val LOCAL_LIGHT_TRAVEL = Vec3(0f, -1f, 0f)

private fun directionalLightGizmoMatrix(translation: Vec3, travel: Vec3): Mat4 {
    val len = glm.length(travel)
    if (len < 1e-6f) return glm.translate(Mat4(1f), translation)
    val t = travel / len
    val rot = rotationAlign(LOCAL_LIGHT_TRAVEL, t)
    return glm.translate(Mat4(1f), translation) * rot
}

private fun rotationAlign(from: Vec3, to: Vec3): Mat4 {
    val f = glm.normalize(from)
    val t = glm.normalize(to)
    var dot = glm.dot(f, t)
    dot = dot.coerceIn(-1f, 1f)
    if (dot > 0.9999f) return Mat4(1f)
    if (dot < -0.9999f) {
        val ortho = if (abs(f.x) < 0.9f) glm.cross(Vec3(1f, 0f, 0f), f) else glm.cross(Vec3(0f, 1f, 0f), f)
        val axis = glm.normalize(ortho)
        return glm.rotate(Mat4(1f), PI.toFloat(), axis)
    }
    val axis = glm.normalize(glm.cross(f, t))
    val angle = acos(dot)
    return glm.rotate(Mat4(1f), angle, axis)
}

private fun travelDirectionFromGizmoMatrix(m: Mat4): Vec3 {
    val c0 = Vec3(m[0].x, m[0].y, m[0].z)
    val c1 = Vec3(m[1].x, m[1].y, m[1].z)
    val c2 = Vec3(m[2].x, m[2].y, m[2].z)
    val v = c0 * LOCAL_LIGHT_TRAVEL.x + c1 * LOCAL_LIGHT_TRAVEL.y + c2 * LOCAL_LIGHT_TRAVEL.z
    val len = glm.length(v)
    if (len < 1e-6f) return LOCAL_LIGHT_TRAVEL
    return v / len
}
