package sandbox

import glm_.vec2.Vec2
import glm_.vec3.Vec3
import imgui.ImVec2
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
import rune.core.Layer
import rune.events.Event
import rune.events.EventDispatcher
import rune.events.WindowResizeEvent
import rune.project.ProjectManager
import rune.project.ProjectRoots
import rune.renderer.EditorCamera
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

    private val camera = EditorCamera(30f, 16f / 9f, 0.1f, 1000f)

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

        val grid = 48
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
                        color = Vec3(1f),
                        diffuseIntensity = 1f,
                        direction = Vec3(-0.35f, -0.85f, -0.4f)
                    )
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
                }
            }
        }

        irisSettings.onImGuiRender()
        sceneHierarchyPanel.onImGuiRender()
    }
}
