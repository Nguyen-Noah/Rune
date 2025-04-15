package rune.imgui

import imgui.ImGui
import imgui.flag.ImGuiCol
import imgui.flag.ImGuiConfigFlags
import imgui.gl3.ImGuiImplGl3
import imgui.glfw.ImGuiImplGlfw
import imgui.type.ImBoolean
import org.lwjgl.glfw.GLFW.*
import org.lwjgl.opengl.GL
import rune.core.Application
import rune.core.Layer
import rune.events.Event
import rune.events.EventCategory

class ImguiLayer : Layer("ImGuiLayer") {
    private var imGuiGlfw: ImGuiImplGlfw = ImGuiImplGlfw()
    private var imGuiGl3: ImGuiImplGl3 = ImGuiImplGl3()
    private var blockEvents: Boolean = false

    fun begin() {
        imGuiGl3.newFrame()
        imGuiGlfw.newFrame()
        ImGui.newFrame()
    }

    fun end() {
        ImGui.render()
        imGuiGl3.renderDrawData(ImGui.getDrawData())

        if (ImGui.getIO().hasConfigFlags(ImGuiConfigFlags.ViewportsEnable)) {
            val backupWindowPtr = glfwGetCurrentContext()
            ImGui.updatePlatformWindows()
            ImGui.renderPlatformWindowsDefault()
            glfwMakeContextCurrent(backupWindowPtr)
        }
    }

    override fun onAttach() {
        ImGui.createContext()
        val io = ImGui.getIO()
        val style = ImGui.getStyle()

        // configure flags
        io.configFlags = io.configFlags or ImGuiConfigFlags.NavEnableKeyboard or
                ImGuiConfigFlags.DockingEnable or ImGuiConfigFlags.ViewportsEnable

        // when viewports are enabled we tweak windowRounding/WindowBg so platform windows can look identical to regular ones
        if ((io.configFlags and ImGuiConfigFlags.ViewportsEnable) != 0) {
            style.windowRounding = 0.0f
            style.colors[ImGuiCol.WindowBg].w = 1.0f
        }

        val windowHandle: Long = Application.get().getWindow().getNativeWindow()
        imGuiGlfw.init(windowHandle, true)
        imGuiGl3.init("#version 330")
    }

    override fun onEvent(e: Event) {
        if (blockEvents) {
            val io = ImGui.getIO()
            e.handled = e.handled or e.isInCategory(EventCategory.Mouse) and io.wantCaptureMouse
            e.handled = e.handled or e.isInCategory(EventCategory.Keyboard) and io.wantCaptureKeyboard
        }
    }

    override fun onDetach() {
        imGuiGlfw.shutdown()
        imGuiGl3.shutdown()
        ImGui.destroyContext()
    }

    override fun onImGuiRender() {
        //ImGui.showDemoWindow()
    }

    fun blockEvents(block: Boolean) { blockEvents = block }
}