package rune.imgui

import imgui.ImGui
import imgui.ImVec2
import imgui.ImVec4
import imgui.type.ImBoolean

/**
 * Thin Kotlin-friendly layer over [ImGui]: scoped begin/end helpers, style/id stacks, and a few
 * common forwards so call sites can stick to `RuneGui.*` for typical UI code. Anything not wrapped
 * here remains available via `import imgui.ImGui`.
 */
object RuneGui {

    val io get() = ImGui.getIO()

    fun style() = ImGui.getStyle()

    fun mainViewport() = ImGui.getMainViewport()

    fun text(text: String) = ImGui.text(text)

    fun textDisabled(text: String) = ImGui.textDisabled(text)

    fun separator() = ImGui.separator()

    fun spacing() = ImGui.spacing()

    fun sameLine(offsetFromStartX: Float = 0f, spacing: Float = -1f) =
        ImGui.sameLine(offsetFromStartX, spacing)

    fun button(label: String, size: ImVec2 = ImVec2()) = ImGui.button(label, size)

    fun smallButton(label: String) = ImGui.smallButton(label)

    fun image(textureId: Long, size: ImVec2, uv0: ImVec2 = ImVec2(0f, 1f), uv1: ImVec2 = ImVec2(1f, 0f)) =
        ImGui.image(textureId, size, uv0, uv1)

    fun imageButton(strId: String, textureId: Long, size: ImVec2, uv0: ImVec2 = ImVec2(0f, 1f), uv1: ImVec2 = ImVec2(1f, 0f)) =
        ImGui.imageButton(strId, textureId, size, uv0, uv1)

    fun setNextWindowPos(pos: ImVec2, cond: Int = 0) = ImGui.setNextWindowPos(pos, cond)

    fun setNextWindowSize(size: ImVec2, cond: Int = 0) = ImGui.setNextWindowSize(size, cond)

    fun setNextWindowViewport(viewportId: Int) = ImGui.setNextWindowViewport(viewportId)

    fun contentRegionAvail(): ImVec2 = ImGui.getContentRegionAvail()

    fun cursorScreenPos(): ImVec2 = ImGui.getCursorScreenPos()

    fun windowPosX(): Float = ImGui.getWindowPosX()

    fun windowPosY(): Float = ImGui.getWindowPosY()

    fun windowWidth(): Float = ImGui.getWindowWidth()

    fun windowHeight(): Float = ImGui.getWindowHeight()

    fun isWindowHovered(flags: Int = 0) = ImGui.isWindowHovered(flags)

    fun isWindowFocused(flags: Int = 0) = ImGui.isWindowFocused(flags)

    fun isItemHovered() = ImGui.isItemHovered()

    fun getMousePos(): ImVec2 = ImGui.getMousePos()

    fun getID(strId: String): Int = ImGui.getID(strId)

    inline fun window(name: String, flags: Int, block: () -> Unit) {
        if (ImGui.begin(name, flags)) {
            try {
                block()
            } finally {
                ImGui.end()
            }
        }
    }

    inline fun window(name: String, pOpen: ImBoolean?, flags: Int, block: () -> Unit) {
        if (ImGui.begin(name, pOpen, flags)) {
            try {
                block()
            } finally {
                ImGui.end()
            }
        }
    }

    inline fun child(
        id: String,
        size: ImVec2 = ImVec2(),
        border: Boolean = false,
        flags: Int = 0,
        block: () -> Unit
    ) {
        ImGui.beginChild(id, size, border, flags)
        try {
            block()
        } finally {
            ImGui.endChild()
        }
    }

    inline fun menuBar(block: () -> Unit) {
        if (ImGui.beginMenuBar()) {
            try {
                block()
            } finally {
                ImGui.endMenuBar()
            }
        }
    }

    inline fun menu(label: String, enabled: Boolean = true, block: () -> Unit) {
        if (ImGui.beginMenu(label, enabled)) {
            try {
                block()
            } finally {
                ImGui.endMenu()
            }
        }
    }

    inline fun popup(id: String, flags: Int = 0, block: () -> Unit) {
        if (ImGui.beginPopup(id, flags)) {
            try {
                block()
            } finally {
                ImGui.endPopup()
            }
        }
    }

    inline fun <T> withStyleVar(idx: Int, value: Float, block: () -> T): T {
        ImGui.pushStyleVar(idx, value)
        return try {
            block()
        } finally {
            ImGui.popStyleVar()
        }
    }

    inline fun <T> withStyleVar(idx: Int, value: ImVec2, block: () -> T): T {
        ImGui.pushStyleVar(idx, value)
        return try {
            block()
        } finally {
            ImGui.popStyleVar()
        }
    }

    inline fun <T> withStyleVars(vararg pairs: Pair<Int, Float>, block: () -> T): T {
        pairs.forEach { (idx, v) -> ImGui.pushStyleVar(idx, v) }
        return try {
            block()
        } finally {
            ImGui.popStyleVar(pairs.size)
        }
    }

    inline fun <T> withStyleColor(idx: Int, r: Float, g: Float, b: Float, a: Float, block: () -> T): T {
        ImGui.pushStyleColor(idx, r, g, b, a)
        return try {
            block()
        } finally {
            ImGui.popStyleColor()
        }
    }

    inline fun <T> withStyleColor(idx: Int, color: ImVec4, block: () -> T): T {
        ImGui.pushStyleColor(idx, color)
        return try {
            block()
        } finally {
            ImGui.popStyleColor()
        }
    }

    inline fun <T> withId(strId: String, block: () -> T): T {
        ImGui.pushID(strId)
        return try {
            block()
        } finally {
            ImGui.popID()
        }
    }

    inline fun <T> withId(id: Int, block: () -> T): T {
        ImGui.pushID(id)
        return try {
            block()
        } finally {
            ImGui.popID()
        }
    }

    inline fun <T> withItemWidth(width: Float, block: () -> T): T {
        ImGui.pushItemWidth(width)
        return try {
            block()
        } finally {
            ImGui.popItemWidth()
        }
    }
}
