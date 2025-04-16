package runestone.panels

import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.World
import imgui.ImGui
import imgui.flag.ImGuiTreeNodeFlags
import imgui.type.ImString
import rune.components.TagComponent
import rune.components.TransformComponent
import rune.scene.Scene

class SceneHierarchyPanel(private var scene: Scene) {
    var selectedEntity: Entity? = null

    fun setContext(scene: Scene) { this.scene = scene }

    fun onImGuiRender() {
        ImGui.begin("Scene Hierarchy")

        /**
         * Based on whether the component has a Tag Component
         * In the event a component doesn't have a tag (probably an internal Rune entity),
         * It will be ignored by this
         */
        scene.world.family { all(TagComponent) }
            .forEach {entity ->
                scene.world.drawEntityNode(entity)
            }

        if (ImGui.isMouseDown(0) and ImGui.isWindowHovered()) {
            selectedEntity = null
        }

        ImGui.end()

        // Properties panel
        ImGui.begin("Properties")

        selectedEntity?.let { drawComponents(it) }

        ImGui.end()

    }

    private fun World.drawEntityNode(entity: Entity) {
        val tag = entity[TagComponent].tag
        val flags = ImGuiTreeNodeFlags.OpenOnArrow or
                (if (selectedEntity == entity)
                    ImGuiTreeNodeFlags.Selected
                else
                    0)

        val open = ImGui.treeNodeEx(tag, flags)

        if (ImGui.isItemClicked()) {
            selectedEntity = entity
        }

        if (open) {
            ImGui.treePop()
        }
    }

    private fun drawComponents(entity: Entity) {
        with(scene.world) {
            if (entity.has(TagComponent)) {
                val tagComp = entity[TagComponent]

                val newString = ImString(tagComp.tag, 256)
                if (ImGui.inputText("Tag", newString)) {
                    tagComp.tag = newString.toString()
                }
            }

            if (entity.has(TransformComponent)) {
                // ptrId: Long, flags: Int, label: String
                if (ImGui.treeNodeEx(entity.id.toLong(), ImGuiTreeNodeFlags.DefaultOpen, "Transform")) {
                    val transformComp = entity[TransformComponent].transform[3]

                    val newTransform = floatArrayOf(
                        transformComp.x,
                        transformComp.y,
                        transformComp.z
                    )

                    ImGui.dragFloat3("Position", newTransform, 0.1f)
                    transformComp.x = newTransform[0]
                    transformComp.y = newTransform[1]
                    transformComp.z = newTransform[2]

                    ImGui.treePop()
                }
            }
        }
    }
}