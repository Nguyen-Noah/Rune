package runestone.panels

import imgui.ImGui
import imgui.type.ImFloat
import imgui.type.ImInt
import rune.renderer.RenderSettings
import rune.scene.Scene
import runestone.utils.dragFloat
import java.util.*

class RendererManagerPanel(private var scene: Scene) {

    fun onImGuiRender() {
        ImGui.begin("Iris Settings")

        val current = RenderSettings.ToneMapper.fromIdx(scene.renderSettings.tonemap)
        val toneRef = ImInt(RenderSettings.ToneMapper.uiIndexOf(current))
        if (ImGui.combo("Tone Map", toneRef, RenderSettings.ToneMapper.labelsArray)) {
            val selected = RenderSettings.ToneMapper.fromUiIndex(toneRef.get())
            scene.renderSettings.tonemap = selected.idx
        }

        if (dragFloat("Exposure", scene.renderSettings::exposure, 0.01f)) {

        }

        ImGui.end()
    }
}