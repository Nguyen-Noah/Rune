package rune.imgui

import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.ComponentType
import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.World
import glm_.vec3.Vec3
import glm_.vec4.Vec4
import imgui.ImFont
import imgui.ImGui
import imgui.ImGuiStyle
import imgui.ImVec2
import imgui.flag.ImGuiCol
import imgui.flag.ImGuiStyleVar
import imgui.flag.ImGuiTreeNodeFlags
import imgui.type.ImString
import rune.components.TagComponent
import kotlin.reflect.KMutableProperty0

@DslMarker annotation class ImGuiDSL

object ui {
    inline operator fun invoke(block: FrameScope.() -> Unit) =
        FrameScope().apply(block)
}

@ImGuiDSL
class FrameScope {
    inline fun begin(title: String, flags: Int = 0, block: WindowScope.() -> Unit) {
        if (ImGui.begin(title, flags)) WindowScope().block()
        ImGui.end()
    }
}

@ImGuiDSL
open class WindowScope {

    fun getStyle(): ImGuiStyle = ImGui.getStyle()
    fun getFontSize() = ImGui.getFontSize()

    inline fun button(label: String, size: ImVec2 = ImVec2(), onClick: () -> Unit = {}): Boolean {
        if (ImGui.button(label, size)) {
            onClick()
            return true
        }
        return false
    }

    inline fun treeNode(label: String, flags: Int = ImGuiTreeNodeFlags.OpenOnArrow, block: TreeScope.() -> Unit) {
        val open = ImGui.treeNodeEx(label, flags)
        if (open) { TreeScope().block(); ImGui.treePop() }
    }

    fun separator() = ImGui.separator()
    fun sameLine(offsetFromStartX: Float = 0f, spacing: Float = -1f) = ImGui.sameLine(offsetFromStartX, spacing)
    fun text(txt: String) = ImGui.text(txt)
    fun inputText(label: String, text: ImString): Boolean = ImGui.inputText(label, text)

    fun getContentRegionAvailX() = ImGui.getContentRegionAvailX()

    /* ----- wrappers for widgets ----- */
    fun colorEdit4(
        label: String,
        vec: Vec4
    ): Boolean {
        val buf = buffers().f4
        buf[0] = vec.r; buf[1] = vec.g; buf[2] = vec.b; buf[3] = vec.a
        val changed = ImGui.colorEdit4(label, buf)
        if (changed) vec put buf
        return changed
    }

    fun dragFloat(
        label: String,
        value: KMutableProperty0<Float>,
        speed: Float = 0.1f,
        min: Float = 0f,
        max: Float = 0f
    ): Boolean {
        val buf = buffers().f1
        buf[0] = value.get()
        val changed = ImGui.dragFloat(label, buf, speed, min, max)
        if (changed) value.set(buf[0])
        return changed
    }
    fun dragFloat2(
        label: String,
        propX : KMutableProperty0<Float>,
        propY : KMutableProperty0<Float>,
        speed: Float = 0.1f,
        min: Float = 0f,
        max: Float = 0f
    ): Boolean {
        val buf = buffers().f2
        buf[0] = propX.get()
        buf[1] = propY.get()
        val changed = ImGui.dragFloat2(label, buf, speed, min, max)
        if (changed) { propX.set(buf[0]); propY.set(buf[1]) }
        return changed
    }

    //fun dragFloat(label: String, value: FloatArray, speed: Float = 0.1f, min: Float = 0f, max: Float = 0f) = ImGui.dragFloat(label, value, speed, min, max)
    //fun dragFloat2(label: String, value: FloatArray, speed: Float = 0.1f, min: Float = 0f, max: Float = 0f) = ImGui.dragFloat2(label, value, speed, min, max)
    fun dragFloat4(label: String, value: FloatArray, speed: Float = 0.1f, min: Float = 0f, max: Float = 0f) = ImGui.dragFloat4(label, value, speed, min, max)

    fun pushID(id: Int) = ImGui.pushID(id)
    fun pushID(id: Long) = ImGui.pushID(id)
    fun pushID(id: String) = ImGui.pushID(id)
    fun popID() = ImGui.popID()

    fun pushStyleVar(idx: Int, x: Float, y: Float) = ImGui.pushStyleVar(idx, x, y)
    fun popStyleVar(num: Int = 1) = ImGui.popStyleVar(num)

    fun pushItemWidth(width: Float) = ImGui.pushItemWidth(width)
    fun popItemWidth() = ImGui.popItemWidth()

    fun pushStyleColor(style: Int, colX: Float, colY: Float, colZ: Float, colW: Float) = ImGui.pushStyleColor(style, colX, colY, colZ, colW)


    fun pushFont(font: ImFont? = null) = ImGui.pushFont(font)

    fun treeNodeEx(label: String, flags: Int, fmt: String = label): Boolean = ImGui.treeNodeEx(label, flags, fmt)
    fun treePop() = ImGui.treePop()

    fun isItemClicked() = ImGui.isItemClicked()

    /*  popups */
    fun openPopup(label: String) = ImGui.openPopup(label)
    fun closeCurrentPopup() = ImGui.closeCurrentPopup()

    fun beginPopupContextItem() = ImGui.beginPopupContextItem()
    fun beginPopup(label: String) = ImGui.beginPopup(label)
    fun endPopup() = ImGui.endPopup()
    fun menuItem(label: String) = ImGui.menuItem(label)

    fun beginPopupContextWindow() = ImGui.beginPopupContextWindow()

    fun isMouseDown(button: Int) = ImGui.isMouseDown(button)

    fun isWindowHovered() = ImGui.isWindowHovered()

    /* Column */
    fun columns(num: Int) = ImGui.columns(num)
    fun setColumnWidth(columnIndex: Int, width: Float) = ImGui.setColumnWidth(columnIndex, width)
    fun nextColumn() = ImGui.nextColumn()

    fun calcItemWidth() = ImGui.calcItemWidth()
}

@ImGuiDSL
class ComponentScope(private val world: World, private val e: Entity) : WindowScope()

@ImGuiDSL
class EntityScope(val world: World, val entity: Entity) : WindowScope() {
    inline fun <reified C : Component<C>> component(
        label: String,
        type: ComponentType<C>,
        treeFlags: Int = ImGuiTreeNodeFlags.DefaultOpen or
                ImGuiTreeNodeFlags.AllowItemOverlap or
                ImGuiTreeNodeFlags.SpanAvailWidth or
                ImGuiTreeNodeFlags.Framed or
                ImGuiTreeNodeFlags.FramePadding,
        crossinline  body: ComponentScope.(C) -> Unit
    ) {
        if (!with(world){ entity.has(type) })
            return
        val comp = with(world) {entity[type] }

        pushStyleVar(ImGuiStyleVar.FramePadding, 4f, 4f)

        val lineH = ImGui.getFont().fontSize + ImGui.getStyle().framePaddingY * 2
        separator()
        val open = C::class.qualifiedName?.let { treeNodeEx(it, treeFlags, label) }

        // settings button
        sameLine(getContentRegionAvailX() - lineH * .5f)
        if (button("+", ImVec2(lineH, lineH))) {
            openPopup("ComponentSettings")
        }

        popStyleVar()

        var remove = false
        if (beginPopup("ComponentSettings")) {
            if (menuItem("Remove Component"))
                remove = true
            endPopup()
        }

        if (open == true) {
            ComponentScope(world, entity).body(comp)
            treePop()
        }
        if (remove)
            with(world) { entity.configure { entity -= type } }
    }
}

fun ComponentScope.vec3(
    label: String,
    vec: KMutableProperty0<Vec3>,
    reset: Float = 0f,
    speed: Float = 0.1f,
    columnWidth: Float = 100f
) {
    val boldFont = RuneFonts.get("OpenSans-bold")

    pushID(label)

    columns(2)
    setColumnWidth(0, columnWidth)
    text(label)
    nextColumn()

    val totalWidth = calcItemWidth()
    val innerGap = getStyle().itemInnerSpacing.x
    val fieldWidth = (totalWidth - innerGap * 2f) / 3f

    pushStyleVar(ImGuiStyleVar.ItemSpacing, 0f, 0f)

    val lineHeight = getFontSize() + getStyle().framePadding.y * 2f
    val buttonSize = ImVec2(lineHeight + 3f, lineHeight)

    pushStyleColor(ImGuiCol.Button,        0.8f, 0.1f, 0.15f, 1f)
    pushStyleColor(ImGuiCol.ButtonHovered, 0.9f, 0.2f, 0.2f,  1f)
    pushStyleColor(ImGuiCol.ButtonActive,  0.8f, 0.1f, 0.15f, 1f)
    pushFont(boldFont)
    //if (button("X", buttonSize))

}


inline fun WindowScope.entity(
    entity: Entity?,
    world: World,
    block: EntityScope.() -> Unit
) {
    if (entity == null) {
        ImGui.textDisabled("-- none selected --")
        return
    }
    EntityScope(world, entity).block()
}



@ImGuiDSL
class TreeScope : WindowScope()

/* --------- small domain helpers for Scene --------- */
inline fun WindowScope.entities(world: World, crossinline block: World.(Entity) -> Unit) {
    world.family { all(TagComponent) }.forEach { entity ->
        world.block(entity)
    }
}

inline fun WindowScope.entityNode(world: World, e: Entity, selected: Entity?, onSelect: (Entity) -> Unit, onDeleted: (Entity) -> Unit, block: TreeScope.() -> Unit = {}) {
    val tag = with(world) { e[TagComponent].tag }
    treeNode(tag, ImGuiTreeNodeFlags.SpanAvailWidth or if (e == selected) ImGuiTreeNodeFlags.Selected else 0) {
        block()
    }

    if (ImGui.isItemClicked()) onSelect(e)
    if (ImGui.beginPopupContextItem()) {
        if (ImGui.menuItem("Delete Entity")) onDeleted(e)
        ImGui.endPopup()
    }
}
