package sandbox.panels

import glm_.glm
import imgui.ImGui
import imgui.type.ImInt
import org.lwjgl.system.MemoryUtil
import rune.renderer.RenderSettings
import rune.renderer.gpu.Std140Layouts
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

        var specularBandWidth: Float = 0.97f,
        var specularColorIntensity: Float = 0.11f,
        var rimColorIntensity: Float = 0.23f,
        var rimWidth: Float = 0.61f,
        var rimIntensity: Float = 0.35f,

        var enableCelShading: Boolean = true,

        // cpu-side
        var renderWireframe: Boolean = false
    )
    private val renderSettingsBuffer = UniformBuffer.create(Std140Layouts.RendererSettings, U_RENDERER_SETTINGS, "Renderer-settings")
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

            putFloat(renderSettings.specularBandWidth)
            putFloat(renderSettings.rimWidth)
            putFloat(renderSettings.rimIntensity)
            putFloat(renderSettings.specularColorIntensity)
            putFloat(renderSettings.rimColorIntensity)

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

        if (ImGui.checkbox("Toon Shading", renderSettings.enableCelShading)) {
            renderSettings.enableCelShading = !renderSettings.enableCelShading
        }

        // TODO: probably find a more memory-efficient way to do this
        val temp = FloatArray(1)
        temp[0] = renderSettings.specularBandWidth.inverse()
        if (ImGui.dragFloat("Specular Band", temp, 0.01f, 0.01f, 1f))
            renderSettings.specularBandWidth = temp.first().inverse()

        temp[0] = renderSettings.specularColorIntensity
        if (ImGui.dragFloat("Specular Color Intensity", temp, 0.01f, 0f, 1f))
            renderSettings.specularColorIntensity = temp.first()

        temp[0] = renderSettings.rimWidth.inverse()
        if (ImGui.dragFloat("Rim Width", temp, 0.01f, 0f, 1f))
            renderSettings.rimWidth = temp.first().inverse()

        temp[0] = renderSettings.rimIntensity.inverse()
        if (ImGui.dragFloat("Rim Intensity", temp, 0.01f, 0.01f, 1f))
            renderSettings.rimIntensity = temp.first().inverse()

        temp[0] = renderSettings.rimColorIntensity
        if (ImGui.dragFloat("Rim Color Intensity", temp, 0.01f, 0f, 1f))
            renderSettings.rimColorIntensity = temp.first()

        ImGui.end()
    }
}

private fun Boolean.toInt() = if (this) 1 else 0
private fun Float.inverse() = 1 - this
