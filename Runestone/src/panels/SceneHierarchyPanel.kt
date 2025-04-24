package runestone.panels

import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.ComponentType
import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.World
import glm_.glm
import glm_.vec3.Vec3
import imgui.ImGui
import imgui.ImVec2
import imgui.flag.ImGuiCol
import imgui.flag.ImGuiPopupFlags
import imgui.flag.ImGuiStyleVar
import imgui.flag.ImGuiTreeNodeFlags
import imgui.type.ImString
import rune.components.*
import rune.imgui.RuneFonts
import rune.scene.*

class SceneHierarchyPanel(private var scene: Scene) {
    // ─── reusable scratch buffers ────────────────────────────────────────────────
    private val tmpFloat = FloatArray(1)
    private val tmpColor = FloatArray(4)

    // TODO: make this a callback in a different file and just send out onSelectedEntityChangeEvent or something
    var selectedEntity: Entity? = null

    fun setContext(scene: Scene) {
        this.scene = scene
        selectedEntity = null
    }

    fun onImGuiRender() {
        ImGui.begin("Scene Hierarchy")
        scene.world.family { all(TagComponent) }
            .forEach { scene.world.drawEntityNode(it) }
        if (ImGui.isMouseDown(0) && ImGui.isWindowHovered()) selectedEntity = null

        // right-click on a blank space
        if (ImGui.beginPopupContextWindow("SceneContextWindow", ImGuiPopupFlags.NoOpenOverItems or ImGuiPopupFlags.MouseButtonRight)) {
            if (ImGui.menuItem("Create Empty Entity")) {
                scene.createEntity("Empty Entity")
            }
            ImGui.endPopup()
        }

        ImGui.end()

        ImGui.begin("Properties")
        selectedEntity?.let {
            drawComponents(it)
        }
        ImGui.end()
    }

    private fun World.drawEntityNode(entity: Entity) {
        ImGui.pushStyleVar(ImGuiStyleVar.FramePadding, 0f, 0f)
        ImGui.pushStyleVar(ImGuiStyleVar.ItemSpacing, 0f, 0f)

        val tag = entity[TagComponent].tag
        val flags = ImGuiTreeNodeFlags.OpenOnArrow or
                ImGuiTreeNodeFlags.SpanAvailWidth or
                if (selectedEntity == entity) ImGuiTreeNodeFlags.Selected else 0
        val open = ImGui.treeNodeEx(tag, flags)
        if (ImGui.isItemClicked()) selectedEntity = entity

        // deferring deletion in case any following code depends on the entity
        var entityDeleted = false
        if (ImGui.beginPopupContextItem()) {
            if (ImGui.menuItem("Delete Entity")) {
                entityDeleted = true
            }
            ImGui.endPopup()
        }

        if (open) ImGui.treePop()

        // popping padding
        ImGui.popStyleVar(2)

        if (entityDeleted) {
            scene.destroyEntity(entity)
            if (selectedEntity == entity) {
                selectedEntity = null
            }
        }
    }

    private inline fun <reified C : Component<C>> World.drawComponent(
        entity: Entity,
        type: ComponentType<C>,
        label: String,
        treeFlags: Int = ImGuiTreeNodeFlags.DefaultOpen or
                         ImGuiTreeNodeFlags.AllowItemOverlap or
                         ImGuiTreeNodeFlags.SpanAvailWidth or
                         ImGuiTreeNodeFlags.Framed or
                         ImGuiTreeNodeFlags.FramePadding,
        crossinline body: (C) -> Unit
    ) {
        if (!entity.has(type)) return

        val comp = entity[type]

        val avail = ImGui.getContentRegionAvail()
        ImGui.pushStyleVar(ImGuiStyleVar.FramePadding, ImVec2(4f, 4f))

        val lineHeight = ImGui.getFont().fontSize + ImGui.getStyle().framePadding.y * 2f
        ImGui.separator()
        val open = ImGui.treeNodeEx(C::class.qualifiedName, treeFlags, label)
        ImGui.popStyleVar()

        ImGui.sameLine(avail.x - lineHeight * 0.5f)
        if (ImGui.button("+", ImVec2(lineHeight, lineHeight)))
            ImGui.openPopup("ComponentSettings")

        var removeComp = false
        if (ImGui.beginPopup("ComponentSettings")) {
            if (ImGui.menuItem("Remove Component")) removeComp = true
            ImGui.endPopup()
        }

        if (open) {
            body(comp)
            ImGui.treePop()
        }

        if (removeComp) {
            entity.configure {
                entity -= type
            }
        }
    }

    private fun drawComponents(entity: Entity) = with(scene.world) {
        if (entity.has(TagComponent)) {
            val tagComp = entity[TagComponent]

            val newString = ImString(tagComp.tag, 256)
            if (ImGui.inputText("##Tag", newString)) {
                tagComp.tag = newString.toString()
            }
        }

        ImGui.sameLine()
        ImGui.pushItemWidth(-1f)

        if (ImGui.button("Add Component"))
            ImGui.openPopup("AddComponent")

        if (ImGui.beginPopup("AddComponent")) {
            selectedEntity?.configure {
                if (ImGui.menuItem("Camera")) {
                    it += CameraComponent()
                    ImGui.closeCurrentPopup()
                }
                if (ImGui.menuItem("Sprite Renderer")) {
                    it += SpriteRendererComponent()
                    ImGui.closeCurrentPopup()
                }
            }
            ImGui.endPopup()
        }

        ImGui.popItemWidth()

        drawComponent<TransformComponent>(entity, TransformComponent, "Transform") { t ->
            drawVec3Control("Translation", t.translation)

            val rot = glm.degrees(t.rotation)
            drawVec3Control("Rotation", rot)
            t.rotation = glm.radians(rot)

            drawVec3Control("Scale", t.scale, 1f)
        }

        drawComponent<CameraComponent>(entity, CameraComponent, "Camera") { camera ->
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
        }

        drawComponent<SpriteRendererComponent>(entity, SpriteRendererComponent, "Sprite Renderer") { src ->
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
        }
    }

    private fun drawVec3Control(label: String, values: Vec3, resetValue: Float = 0.0f, columnWidth: Float = 100f) {
        val boldFont = RuneFonts.get("OpenSans-bold")

        ImGui.pushID(label)

        /* ------------------------------------------------------------------ */
        /*  Label column                                                      */
        /* ------------------------------------------------------------------ */
        ImGui.columns(2)
        ImGui.setColumnWidth(0, columnWidth)
        ImGui.text(label)
        ImGui.nextColumn()

        /* ------------------------------------------------------------------ */
        /*  Manual width split (three fields, two internal gaps)              */
        /* ------------------------------------------------------------------ */
        val totalWidth   = ImGui.calcItemWidth()
        val innerGap     = ImGui.getStyle().itemInnerSpacing.x  // style struct field :contentReference[oaicite:2]{index=2}
        val fieldWidth   = (totalWidth - innerGap * 2f) / 3f

        ImGui.pushStyleVar(ImGuiStyleVar.ItemSpacing, 0f, 0f)

        val lineHeight = ImGui.getFontSize() + ImGui.getStyle().framePadding.y * 2f
        val buttonSize = ImVec2(lineHeight + 3f, lineHeight)

        /* -----------------------------  X  -------------------------------- */
        ImGui.pushStyleColor(ImGuiCol.Button,        0.8f, 0.1f, 0.15f, 1f)
        ImGui.pushStyleColor(ImGuiCol.ButtonHovered, 0.9f, 0.2f, 0.2f,  1f)
        ImGui.pushStyleColor(ImGuiCol.ButtonActive,  0.8f, 0.1f, 0.15f, 1f)
        ImGui.pushFont(boldFont)
        if (ImGui.button("X", buttonSize)) values.x = resetValue
        ImGui.popFont()
        ImGui.popStyleColor(3)

        ImGui.sameLine()
        ImGui.pushItemWidth(fieldWidth)
        val xArr = floatArrayOf(values.x)
        if (ImGui.dragFloat("##X", xArr, 0.1f, 0f, 0f, "%.2f")) values.x = xArr[0]
        ImGui.popItemWidth()
        ImGui.sameLine()

        /* -----------------------------  Y  -------------------------------- */
        ImGui.pushStyleColor(ImGuiCol.Button,        0.2f, 0.7f, 0.2f, 1f)
        ImGui.pushStyleColor(ImGuiCol.ButtonHovered, 0.3f, 0.8f, 0.3f, 1f)
        ImGui.pushStyleColor(ImGuiCol.ButtonActive,  0.2f, 0.7f, 0.2f, 1f)
        ImGui.pushFont(boldFont)
        if (ImGui.button("Y", buttonSize)) values.y = resetValue
        ImGui.popFont()
        ImGui.popStyleColor(3)

        ImGui.sameLine()
        ImGui.pushItemWidth(fieldWidth)
        val yArr = floatArrayOf(values.y)
        if (ImGui.dragFloat("##Y", yArr, 0.1f, 0f, 0f, "%.2f")) values.y = yArr[0]
        ImGui.popItemWidth()
        ImGui.sameLine()

        /* -----------------------------  Z  -------------------------------- */
        ImGui.pushStyleColor(ImGuiCol.Button,        0.1f, 0.25f, 0.8f, 1f)
        ImGui.pushStyleColor(ImGuiCol.ButtonHovered, 0.2f, 0.35f, 0.9f, 1f)
        ImGui.pushStyleColor(ImGuiCol.ButtonActive,  0.1f, 0.25f, 0.8f, 1f)
        ImGui.pushFont(boldFont)
        if (ImGui.button("Z", buttonSize)) values.z = resetValue
        ImGui.popFont()
        ImGui.popStyleColor(3)

        ImGui.sameLine()
        ImGui.pushItemWidth(fieldWidth)
        val zArr = floatArrayOf(values.z)
        if (ImGui.dragFloat("##Z", zArr, 0.1f, 0f, 0f, "%.2f")) values.z = zArr[0]
        ImGui.popItemWidth()

        /* ------------------------------------------------------------------ */
        ImGui.popStyleVar()
        ImGui.columns(1)
        ImGui.popID()
    }
}
