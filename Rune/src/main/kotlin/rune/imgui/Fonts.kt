package rune.imgui

import imgui.ImFont
import imgui.ImGui
import rune.core.Logger
import java.io.File

data class FontConfiguration(
    val fontName: String,
    val filePath: CharSequence,
    val size: Float = 18.0f,
)

object RuneFonts {
    private val fonts: HashMap<String, ImFont> = hashMapOf()

    fun add(config: FontConfiguration, isDefault: Boolean = false) {
        if (fonts.containsKey(config.fontName)) {
            Logger.warn("Tried to add font: ${config.fontName}, but name is already taken!")
            return
        }

        if (File(config.filePath.toString()).exists()) {
            val io = ImGui.getIO()
            val font: ImFont = io.fonts.addFontFromFileTTF(config.filePath.toString(), config.size)
            fonts[config.fontName] = font

            if (isDefault)
                io.fontDefault = font
        } else {
            Logger.warn("Unable to find font at path: ${config.filePath}.")
        }
    }

    fun get(fontName: String): ImFont? {
        return fonts[fontName]
    }
}