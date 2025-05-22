package runestone.panels

import imgui.ImGui
import imgui.ImVec2
import imgui.ImVec4
import imgui.flag.ImGuiCol
import imgui.flag.ImGuiMouseButton
import org.lwjgl.opengl.GL11.GL_LINEAR
import rune.renderer.gpu.Texture2D
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

// TODO: dont refresh the file scanning every single second, maybe have a listener or refresh every 30-60Hz

class ContentBrowserPanel {

    // TODO: once we have projects, change this to take in a RuneProject
    private val assetsDirectory: String = "assets"

    private val assetPath: Path = Paths.get(assetsDirectory)
    private var currentDirectory: Path = assetPath
    private val directoryIcon = Texture2D.create("src/main/resources/Icons/ContentBrowser/DirectoryIcon.png", filter = GL_LINEAR)
    private val fileIcon =      Texture2D.create("src/main/resources/Icons/ContentBrowser/FileIcon.png", filter = GL_LINEAR)

    private var padding = 16f
    private var thumbnailSize = 128f

    fun onImGuiRender() {
        ImGui.begin("Content Browser")

        if (currentDirectory != assetPath) {
            if (ImGui.button("<-"))
                currentDirectory = currentDirectory.parent
        }

        val cellSize = thumbnailSize + padding

        val panelWidth = ImGui.getContentRegionAvail().x
        var columnCount = (panelWidth / cellSize).toInt()
        if (columnCount < 1)
            columnCount = 1

        ImGui.columns(columnCount, 0.toString(), false)

        Files.list(currentDirectory).use { dir ->
            dir.forEach { path ->
                val relativePath = assetPath.relativize(path)
                val filename = relativePath.fileName.toString()

                val isDirectory = Files.isDirectory(path)
                val icon: Texture2D = if (isDirectory) directoryIcon else fileIcon
                ImGui.pushStyleColor(ImGuiCol.Button, ImVec4(0f, 0f, 0f, 0f))       // TEMP maybe
                ImGui.pushID(filename)
                ImGui.imageButton("##icon", icon.rendererID.toLong(), ImVec2(thumbnailSize, thumbnailSize), ImVec2(0f, 1f), ImVec2(1f, 0f))

                // drag n drop
                if (ImGui.beginDragDropSource()) {
                    ImGui.setDragDropPayload("CONTENT_BROWSER_ITEM", relativePath.toString())

                    ImGui.endDragDropSource()
                }

                ImGui.popID()
                ImGui.popStyleColor()
                if (ImGui.isItemHovered() && ImGui.isMouseDoubleClicked(ImGuiMouseButton.Left)) {
                    if (isDirectory) {
                        currentDirectory = currentDirectory.resolve(path.fileName)
                    }
                }
                ImGui.textWrapped(filename)

                ImGui.nextColumn()
            }
        }
        ImGui.columns(1)

        var temp = floatArrayOf(thumbnailSize)
        if (ImGui.sliderFloat("Thumbnail Size", temp, 16f, 512f))
            thumbnailSize = temp[0]

        temp = floatArrayOf(padding)
        if (ImGui.sliderFloat("Padding", temp, 0f, 32f))
            padding = temp[0]

        ImGui.end()
    }
}

//                if (Files.isDirectory(path)) {
//                    icon = directoryIcon
//                    ImGui.imageButton(icon.rendererID.toString(), icon.rendererID.toLong(), ImVec2(thumbnailSize, thumbnailSize), ImVec2(0f, 1f), ImVec2(1f, 0f))
//                    if (ImGui.isItemHovered() && ImGui.isMouseDoubleClicked(ImGuiMouseButton.Left)) {
//                        if ()
//                    }
//
//                    if (ImGui.button(filename))
//                        currentDirectory = currentDirectory.resolve(path.fileName)
//
//                } else {
//                    icon = fileIcon
//
//                    if (ImGui.button(filename))
//                        TODO("handle file click")
//                }