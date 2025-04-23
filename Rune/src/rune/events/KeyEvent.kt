package rune.events

import rune.core.Key


sealed class KeyEvent(
    val keyCode: Key
) : Event(EventCategory.Keyboard.mask or EventCategory.Input.mask) {
    override fun toString(): String {
        return "${this::class.simpleName}: $keyCode"
    }
}

class KeyPressedEvent(
    keyCode: Key,
    val isRepeat: Boolean = false
) : KeyEvent(keyCode) {
    override val eventType = EventType.KeyPressed

    override fun toString(): String {
        return "KeyPressedEvent: $keyCode (repeat=$isRepeat)"
    }
}

class KeyReleasedEvent(
    keyCode: Key
) : KeyEvent(keyCode) {
    override val eventType = EventType.KeyReleased

    override fun toString(): String {
        return "KeyReleasedEvent: $keyCode"
    }
}

class KeyTypedEvent(
    keyCode: Key
) : KeyEvent(keyCode) {
    override val eventType = EventType.KeyTyped

    override fun toString(): String {
        return "KeyTypedEvent: $keyCode"
    }
}