package runestone.panels

import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.World
import glm_.glm
import imgui.ImGui
import imgui.flag.ImGuiInputTextFlags
import imgui.flag.ImGuiTreeNodeFlags
import imgui.type.ImString
import rune.components.*
import rune.scene.ProjectionType
import rune.scene.Scene

class SceneHierarchyPanel(private var scene: Scene) {
    // ─── reusable scratch buffers ────────────────────────────────────────────────
    private val tmpVec3 = FloatArray(3)
    private val tmpFloat = FloatArray(1)
    private val tmpColor = FloatArray(4)
    private val tagBuffer = ImString(256)

    var selectedEntity: Entity? = null

    fun setContext(scene: Scene) {
        this.scene = scene
    }

    fun onImGuiRender() {
        ImGui.begin("Scene Hierarchy")
        scene.world.family { all(TagComponent) }
            .forEach { scene.world.drawEntityNode(it) }
        if (ImGui.isMouseDown(0) && ImGui.isWindowHovered()) selectedEntity = null
        ImGui.end()

        ImGui.begin("Properties")
        selectedEntity?.let { drawComponents(it) }
        ImGui.end()
    }

    private fun World.drawEntityNode(entity: Entity) {
        val tag = entity[TagComponent].tag
        val flags = ImGuiTreeNodeFlags.OpenOnArrow or
                if (selectedEntity == entity) ImGuiTreeNodeFlags.Selected else 0
        val open = ImGui.treeNodeEx(tag, flags)
        if (ImGui.isItemClicked()) selectedEntity = entity
        if (open) ImGui.treePop()
    }

    private fun drawComponents(entity: Entity) = with(scene.world) {
        // ─── Tag ───────────────────────────────────────────────────────────────────
        if (entity.has(TagComponent)) {
            val tagComp = entity[TagComponent]

            val newString = ImString(tagComp.tag, 256)
            if (ImGui.inputText("Tag", newString)) {
                tagComp.tag = newString.toString()
            }
        }

        // ─── Transform ─────────────────────────────────────────────────────────────
        if (entity.has(TransformComponent)) {
            if (ImGui.treeNodeEx(entity.id.toLong(),
                    ImGuiTreeNodeFlags.DefaultOpen, "Transform"))
            {
                val t = entity[TransformComponent].transform[3]
                tmpVec3[0] = t.x; tmpVec3[1] = t.y; tmpVec3[2] = t.z
                if (ImGui.dragFloat3("Position", tmpVec3, 0.1f)) {
                    t.x = tmpVec3[0]; t.y = tmpVec3[1]; t.z = tmpVec3[2]
                }
                ImGui.treePop()
            }
        }

        // ─── Camera ────────────────────────────────────────────────────────────────
        if (entity.has(CameraComponent)) {
            if (ImGui.treeNodeEx(entity.id.toLong(),
                    ImGuiTreeNodeFlags.DefaultOpen, "Camera"))
            {
                val camComp = entity[CameraComponent]
                val cam     = camComp.camera

                val oldP = camComp.primary
                if (ImGui.checkbox("Primary", oldP))
                    camComp.primary = !oldP

                val types = arrayOf("Perspective", "Orthographic")
                var curr  = types[cam.projectionType.ordinal]
                if (ImGui.beginCombo("Projection", curr)) {
                    types.forEachIndexed { i, s ->
                        val sel = curr == s
                        if (ImGui.selectable(s, sel)) {
                            curr = s
                            cam.projectionType = ProjectionType.fromInt(i)
                        }
                        if (sel) ImGui.setItemDefaultFocus()
                    }
                    ImGui.endCombo()
                }

                when (cam.projectionType) {
                    ProjectionType.Perspective -> {
                        tmpFloat[0] = glm.degrees(cam.perspectiveFOV)
                        if (ImGui.dragFloat("Vertical FOV", tmpFloat))
                            cam.perspectiveFOV = glm.radians(tmpFloat[0])

                        tmpFloat[0] = cam.perspectiveNear
                        if (ImGui.dragFloat("Near", tmpFloat))
                            cam.perspectiveNear = tmpFloat[0]

                        tmpFloat[0] = cam.perspectiveFar
                        if (ImGui.dragFloat("Far", tmpFloat))
                            cam.perspectiveFar = tmpFloat[0]
                    }

                    ProjectionType.Orthographic -> {
                        tmpFloat[0] = cam.orthographicSize
                        if (ImGui.dragFloat("Size", tmpFloat))
                            cam.orthographicSize = tmpFloat[0]

                        tmpFloat[0] = cam.orthographicNear
                        if (ImGui.dragFloat("Near", tmpFloat))
                            cam.orthographicNear = tmpFloat[0]

                        tmpFloat[0] = cam.orthographicFar
                        if (ImGui.dragFloat("Far", tmpFloat))
                            cam.orthographicFar = tmpFloat[0]
                    }
                }

                val oldA = camComp.fixedAspectRatio
                if (ImGui.checkbox("Fixed Aspect Ratio", oldA))
                    camComp.fixedAspectRatio = !oldA

                ImGui.treePop()
            }
        }

        // ─── Sprite Renderer ──────────────────────────────────────────────────────
        if (entity.has(SpriteRendererComponent)) {
            if (ImGui.treeNodeEx(entity.id.toLong(),
                    ImGuiTreeNodeFlags.DefaultOpen, "Sprite Renderer"))
            {
                val src = entity[SpriteRendererComponent]
                tmpColor[0] = src.color.r
                tmpColor[1] = src.color.g
                tmpColor[2] = src.color.b
                tmpColor[3] = src.color.a
                if (ImGui.colorEdit4("Color", tmpColor)) {
                    src.color.r = tmpColor[0]
                    src.color.g = tmpColor[1]
                    src.color.b = tmpColor[2]
                    src.color.a = tmpColor[3]
                }
                ImGui.treePop()
            }
        }
    }
}
