package rune.core

import rune.events.Event
import rune.platforms.windows.WindowsWindow

data class WindowProps(
    val title: String = "Rune Engine",
    val width: Int = 1600,
    val height: Int = 900
)

typealias EventCallbackFn = (Event) -> Unit

interface Window {
    val width: Int
    val height: Int

    fun onUpdate()

    // window attributes
    fun setEventCallback(callback: EventCallbackFn)
    fun setVSync(enabled: Boolean)
    fun isVSync(): Boolean

    // if you need to expose the native window pointer,
    // you can represent it as Any? or a platform-specific type
    fun getNativeWindow(): Long

    companion object{
        // return a Window reference
        fun create(props: WindowProps = WindowProps()): Window {
            // change this later to check for platform instead of just returning windows
            return WindowsWindow(props)
        }
    }
}