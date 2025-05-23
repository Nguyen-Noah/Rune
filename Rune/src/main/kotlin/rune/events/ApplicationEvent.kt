package rune.events

class WindowResizeEvent(
    val width: Int,
    val height: Int
) : Event(EventCategory.Application.mask) {
    override val eventType = EventType.WindowResize

    override fun toString(): String {
        return "WindowResizeEvent: ($width, $height)"
    }
}

class WindowCloseEvent : Event(EventCategory.Application.mask) {
    override val eventType = EventType.WindowClose

    override fun toString(): String {
        return "WindowCloseEvent"
    }
}

class AppTickEvent : Event(EventCategory.Application.mask) {
    override val eventType = EventType.AppTick

    override fun toString(): String = "AppTickEvent"
}

class AppUpdateEvent : Event(EventCategory.Application.mask) {
    override val eventType = EventType.AppUpdate

    override fun toString(): String = "AppUpdateEvent"
}

class AppRenderEvent : Event(EventCategory.Application.mask) {
    override val eventType = EventType.AppRender

    override fun toString(): String = "AppRenderEvent"
}
