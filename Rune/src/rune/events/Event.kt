package rune.events

enum class EventType {
    None,
    WindowClose, WindowResize, WindowFocus, WindowLostFocus, WindowMoved,
    AppTick, AppUpdate, AppRender,
    KeyPressed, KeyReleased,
    MouseButtonPressed, MouseButtonReleased, MouseMoved, MouseScrolled
}

enum class EventCategory(val mask: Int) {
    None(0),
    Application(1 shl 0),
    Input(1 shl 1),
    Keyboard(1 shl 2),
    Mouse(1 shl 3),
    MouseButton(1 shl 4)
}

sealed class Event(private val categoryFlags: Int, var handled: Boolean = false) {
    fun isInCategory(category: EventCategory): Boolean {
        return (categoryFlags and category.mask) != 0
    }

    override fun toString(): String = this::class.simpleName ?: "UnnamedEvent"
}

class EventDispatcher(val event: Event) {
    inline fun <reified T : Event> dispatch(handler: (T) -> Boolean): Boolean {
        // if the underlying event is T, call 'handler' on it
        if (event is T) {
            event.handled = event.handled || handler(event)
            return true
        }
        return false
    }
}

//fun handleEvent(event: Event) {
//    val dispatcher = EventDispatcher(event)
//    dispatcher.dispatch<WindowCloseEvent> { e ->
//        println("Handling WindowCloseEvent")
//        // Return true if handled
//        true
//    }
//    dispatcher.dispatch<WindowResizeEvent> { e ->
//        println("Handling WindowResizeEvent: ${e.width}x${e.height}")
//        // Return true if handled
//        true
//    }
//    // etc.
//}