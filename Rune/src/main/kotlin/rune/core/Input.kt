package rune.core

import glm_.vec2.Vec2

interface Input {
    fun init(): Boolean
    fun isKeyPressed(key: Key): Boolean
    fun isMouseButtonPressed(button: MouseButton): Boolean
    fun getMousePosition(): Vec2
    fun getMouseX(): Float
    fun getMouseY(): Float

    companion object : Input {
        /**
         * We store a private 'delegate' that will do the real work.
         * By default, it can be set to a Windows implementation.
         */
        private var delegate: Input = rune.platforms.windows.WindowsInput

        /**
         * If you want to switch implementations (e.g., on Linux),
         * you can do: Input.setDelegate(LinuxInput)
         */
        fun setDelegate(impl: Input) {
            delegate = impl
        }

        override fun init(): Boolean = delegate.init()
        override fun isKeyPressed(key: Key): Boolean = delegate.isKeyPressed(key)
        override fun isMouseButtonPressed(button: MouseButton): Boolean = delegate.isMouseButtonPressed(button)
        override fun getMousePosition(): Vec2 = delegate.getMousePosition()
        override fun getMouseX(): Float = delegate.getMouseX()
        override fun getMouseY(): Float = delegate.getMouseY()
    }
}