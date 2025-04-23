package rune.imgui

import imgui.ImGui
import imgui.ImVec4
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

        // fonts
        RuneFonts.add(
            FontConfiguration("OpenSans-regular", "assets/fonts/opensans/OpenSans-Regular.ttf"),
            isDefault = true
        )
        RuneFonts.add(FontConfiguration("OpenSans-bold", "assets/fonts/opensans/OpenSans-Bold.ttf"))

        // color theme
        setDarkThemeColors()

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

    private fun setDarkThemeV2Colors() {
        val style  = ImGui.getStyle()

        /* ── helper to unpack your U32 theme constants ── */
        fun u32(col: Int): ImVec4 = ImGui.colorConvertU32ToFloat4(col)

        /* ───────────── Headers ───────────── */
        val header = u32(Colors.Theme.groupHeader)
        listOf(ImGuiCol.Header,
            ImGuiCol.HeaderHovered,
            ImGuiCol.HeaderActive).forEach {
            style.setColor(it, header.x, header.y, header.z, header.w)
        }

        /* ───────────── Buttons ───────────── */
        style.setColor(ImGuiCol.Button,         56f/255, 56f/255, 56f/255, 200f/255)
        style.setColor(ImGuiCol.ButtonHovered,  70f/255, 70f/255, 70f/255,   1f)
        style.setColor(ImGuiCol.ButtonActive,   56f/255, 56f/255, 56f/255, 150f/255)

        /* ───────── Frame BG ───────── */
        val frameBg = u32(Colors.Theme.propertyField)
        listOf(ImGuiCol.FrameBg,
            ImGuiCol.FrameBgHovered,
            ImGuiCol.FrameBgActive).forEach {
            style.setColor(it, frameBg.x, frameBg.y, frameBg.z, frameBg.w)
        }

        /* ───────── Tabs & Title bar ───────── */
        val title = u32(Colors.Theme.titlebar)
        style.setColor(ImGuiCol.Tab,                title.x, title.y, title.z, title.w)
        style.setColor(ImGuiCol.TabUnfocused,       title.x, title.y, title.z, title.w)
        style.setColor(ImGuiCol.TabHovered,         255f/255, 225f/255, 135f/255, 30f/255)
        style.setColor(ImGuiCol.TabActive,          255f/255, 225f/255, 135f/255, 60f/255)
        style.setColor(ImGuiCol.TabUnfocusedActive, 255f/255, 225f/255, 135f/255, 30f/255)

        style.setColor(ImGuiCol.TitleBg,         title.x, title.y, title.z, title.w)
        style.setColor(ImGuiCol.TitleBgActive,   title.x, title.y, title.z, title.w)
        style.setColor(ImGuiCol.TitleBgCollapsed,0.15f, 0.1505f, 0.151f, 1f)

        /* ───────── Resize Grip ───────── */
        style.setColor(ImGuiCol.ResizeGrip,        0.91f, 0.91f, 0.91f, 0.25f)
        style.setColor(ImGuiCol.ResizeGripHovered, 0.81f, 0.81f, 0.81f, 0.67f)
        style.setColor(ImGuiCol.ResizeGripActive,  0.46f, 0.46f, 0.46f, 0.95f)

        /* ───────── Scrollbar ───────── */
        style.setColor(ImGuiCol.ScrollbarBg,        0.02f, 0.02f, 0.02f, 0.53f)
        style.setColor(ImGuiCol.ScrollbarGrab,      0.31f, 0.31f, 0.31f, 1f)
        style.setColor(ImGuiCol.ScrollbarGrabHovered,0.41f, 0.41f, 0.41f, 1f)
        style.setColor(ImGuiCol.ScrollbarGrabActive, 0.51f, 0.51f, 0.51f, 1f)

        /* ───────── Check‑mark & Slider ───────── */
        style.setColor(ImGuiCol.CheckMark,          200f/255, 200f/255, 200f/255, 1f)
        style.setColor(ImGuiCol.SliderGrab,         0.51f, 0.51f, 0.51f, 0.70f)
        style.setColor(ImGuiCol.SliderGrabActive,   0.66f, 0.66f, 0.66f, 1f)

        /* ───────── Text / Checkbox tick ───────── */
        val text = u32(Colors.Theme.text)
        style.setColor(ImGuiCol.Text,      text.x, text.y, text.z, text.w)
        style.setColor(ImGuiCol.CheckMark, text.x, text.y, text.z, text.w)

        /* ───────── Separators ───────── */
        val sepBg  = u32(Colors.Theme.backgroundDark)
        val sepAct = u32(Colors.Theme.highlight)
        style.setColor(ImGuiCol.Separator,         sepBg.x, sepBg.y, sepBg.z, sepBg.w)
        style.setColor(ImGuiCol.SeparatorActive,   sepAct.x, sepAct.y, sepAct.z, sepAct.w)
        style.setColor(ImGuiCol.SeparatorHovered,  39f/255, 185f/255, 242f/255, 150f/255)

        /* ───────── Windows / Borders / Popups ───────── */
        val child  = u32(Colors.Theme.background)
        val popup  = u32(Colors.Theme.backgroundPopup)
        style.setColor(ImGuiCol.WindowBg,    title.x, title.y, title.z, title.w)
        style.setColor(ImGuiCol.ChildBg,     child.x, child.y, child.z, child.w)
        style.setColor(ImGuiCol.PopupBg,     popup.x, popup.y, popup.z, popup.w)
        style.setColor(ImGuiCol.Border,      sepBg.x, sepBg.y, sepBg.z, sepBg.w)

        /* ───────── Tables & Menubar ───────── */
        style.setColor(ImGuiCol.TableHeaderBg,  header.x, header.y, header.z, header.w)
        style.setColor(ImGuiCol.TableBorderLight, sepBg.x, sepBg.y, sepBg.z, sepBg.w)
        style.setColor(ImGuiCol.MenuBarBg, 0f, 0f, 0f, 0f)

        /* ───────── Style tweaks ───────── */
        style.frameRounding   = 2.5f
        style.frameBorderSize = 1f
        style.indentSpacing   = 11f
    }

    private fun setDarkThemeColors() {
        val style = ImGui.getStyle()

        // window background
        style.setColor(ImGuiCol.WindowBg, 0.10f, 0.105f, 0.11f, 1.0f)

        // headers
        style.setColor(ImGuiCol.Header,         0.20f, 0.205f, 0.21f, 1.0f)
        style.setColor(ImGuiCol.HeaderHovered,  0.30f, 0.305f, 0.31f, 1.0f)
        style.setColor(ImGuiCol.HeaderActive,   0.15f, 0.1505f, 0.151f, 1.0f)

        // buttons
        style.setColor(ImGuiCol.Button,         0.20f, 0.205f, 0.21f, 1.0f)
        style.setColor(ImGuiCol.ButtonHovered,  0.30f, 0.305f, 0.31f, 1.0f)
        style.setColor(ImGuiCol.ButtonActive,   0.15f, 0.1505f, 0.151f, 1.0f)

        // frame backgrounds
        style.setColor(ImGuiCol.FrameBg,        0.20f, 0.205f, 0.21f, 1.0f)
        style.setColor(ImGuiCol.FrameBgHovered, 0.30f, 0.305f, 0.31f, 1.0f)
        style.setColor(ImGuiCol.FrameBgActive,  0.15f, 0.1505f, 0.151f, 1.0f)

        // tabs
        style.setColor(ImGuiCol.Tab,                0.15f, 0.1505f, 0.151f, 1.0f)
        style.setColor(ImGuiCol.TabHovered,         0.38f, 0.385f, 0.38f, 1.0f)
        style.setColor(ImGuiCol.TabActive,          0.28f, 0.288f, 0.288f, 1.0f)
        style.setColor(ImGuiCol.TabUnfocused,       0.15f, 0.1505f, 0.151f, 1.0f)
        style.setColor(ImGuiCol.TabUnfocusedActive, 0.20f, 0.205f, 0.21f, 1.0f)

        // title bar
        style.setColor(ImGuiCol.TitleBg,           0.15f, 0.1505f, 0.151f, 1.0f)
        style.setColor(ImGuiCol.TitleBgActive,     0.15f, 0.1505f, 0.151f, 1.0f)
        style.setColor(ImGuiCol.TitleBgCollapsed,  0.95f, 0.1505f, 0.951f, 1.0f)
    }
}