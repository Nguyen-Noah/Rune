package rune.events

import rune.core.MouseButton

class MouseMovedEvent(
    private val x: Float,
    private val y: Float
) : Event(EventCategory.Mouse.mask or EventCategory.Input.mask) {
    override fun toString(): String {
        return "MouseMovedEvent: ($x, $y)"
    }
}

class MouseScrolledEvent(
    private val xOffset: Float,
    private val yOffset: Float
) : Event(EventCategory.Mouse.mask or EventCategory.Input.mask) {
    override fun toString(): String {
        return "MouseMovedEvent: ($xOffset, $yOffset)"
    }
}

sealed class MouseButtonEvent(
    val button: MouseButton
) : Event(EventCategory.Mouse.mask or EventCategory.Input.mask or EventCategory.MouseButton.mask)

class MouseButtonPressedEvent(button: MouseButton) : MouseButtonEvent(button) {
    override fun toString(): String {
        return "MouseButtonPressedEvent: $button"
    }
}

class MouseButtonReleasedEvent(button: MouseButton) : MouseButtonEvent(button) {
    override fun toString(): String {
        return "MouseButtonReleasedEvent: $button"
    }
}