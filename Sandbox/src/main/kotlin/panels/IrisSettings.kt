package sandbox.panels

import imgui.ImGui
import imgui.type.ImInt
import org.lwjgl.system.MemoryUtil
import rune.renderer.RenderSettings
import rune.renderer.gpu.U_RENDERER_SETTINGS
import rune.renderer.gpu.UniformBuffer

class IrisSettings {
    data class RendererSettings(
        // gpu-side
        var flags: Int = 0,
        var tonemap: Int = 1,
        var exposure: Float = 1.0f,
        var gamma: Float = 2.2f,
        var bloomIntensity: Float = 0.0f,
        var vignetteStrength: Float = 0.0f,

        var enableCelShading: Boolean = true,

        // cpu-side
        var renderWireframe: Boolean = false
    )
    private val renderSettingsBuffer = UniformBuffer.create(32, U_RENDERER_SETTINGS, "Renderer-settings")
    val renderSettings = RendererSettings()

    fun pushRenderSettings() {
        // TODO: save this to a file and compile the shader with these values injected in RuneProjects
        MemoryUtil.memAlloc(renderSettingsBuffer.size).apply {
            putInt(renderSettings.flags) // flags
            putInt(renderSettings.tonemap) // tonemapper
            putFloat(renderSettings.exposure) // exposure
            putFloat(renderSettings.gamma) // gamma
            putFloat(renderSettings.bloomIntensity) // bloomIntensity
            putFloat(renderSettings.vignetteStrength) // vignetteStrength

            putInt(renderSettings.enableCelShading.toInt())
            putInt(0)

            flip()
            renderSettingsBuffer.setData(this)
            MemoryUtil.memFree(this)
        }
    }

    fun onImGuiRender() {
        ImGui.begin("Iris Settings")

        val current = RenderSettings.ToneMapper.fromIdx(renderSettings.tonemap)
        val toneRef = ImInt(RenderSettings.ToneMapper.uiIndexOf(current))
        if (ImGui.combo("Tone Map", toneRef, RenderSettings.ToneMapper.labelsArray)) {
            val selected = RenderSettings.ToneMapper.fromUiIndex(toneRef.get())
            renderSettings.tonemap = selected.idx
        }

        if (ImGui.checkbox("Render Wireframe", renderSettings.renderWireframe)) {
            renderSettings.renderWireframe = !renderSettings.renderWireframe
        }

        ImGui.end()
    }
}

private fun Boolean.toInt() = if (this) 1 else 0
