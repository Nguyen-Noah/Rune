package rune.imgui

import imgui.ImGui
import kotlin.reflect.KMutableProperty0

fun dragFloat(
    label: String,
    prop: KMutableProperty0<Float>,
    speed: Float = 0.1f,
    min: Float = 0f,
    max: Float = 10f
): Boolean {
    val ref = floatArrayOf(prop.get())
    val changed = ImGui.dragFloat(label, ref, speed, min, max)
    if (changed) prop.set(ref.first())
    return changed
}
