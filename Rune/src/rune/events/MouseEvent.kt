package rune.events

import rune.core.MouseButton

class MouseMovedEvent(
    private val x: Float,
    private val y: Float
) : Event(EventCategory.Mouse.mask or EventCategory.Input.mask) {
    override val eventType = EventType.MouseMoved

    override fun toString(): String {
        return "MouseMovedEvent: ($x, $y)"
    }
}

class MouseScrolledEvent(
    val xOffset: Float,
    val yOffset: Float
) : Event(EventCategory.Mouse.mask or EventCategory.Input.mask) {
    override val eventType = EventType.MouseScrolled

    override fun toString(): String {
        return "MouseMovedEvent: ($xOffset, $yOffset)"
    }
}

sealed class MouseButtonEvent(
    val button: MouseButton
) : Event(EventCategory.Mouse.mask or EventCategory.Input.mask or EventCategory.MouseButton.mask)

class MouseButtonPressedEvent(button: MouseButton) : MouseButtonEvent(button) {
    override val eventType = EventType.MouseButtonPressed

    override fun toString(): String {
        return "MouseButtonPressedEvent: $button"
    }
}

class MouseButtonReleasedEvent(button: MouseButton) : MouseButtonEvent(button) {
    override val eventType = EventType.MouseButtonReleased

    override fun toString(): String {
        return "MouseButtonReleasedEvent: $button"
    }
}