package rune.platforms.windows

import glm_.vec2.Vec2
import org.lwjgl.glfw.GLFW.*
import rune.core.Application
import rune.core.Input
import rune.core.Key
import rune.core.MouseButton

object WindowsInput : Input {
    private val windowHandle = Application.get().getWindow().getNativeWindow()

    override fun init(): Boolean {
        return true
    }

    override fun isKeyPressed(key: Key): Boolean {
        val state = glfwGetKey(windowHandle, key.code.toInt())
        return state == GLFW_PRESS || state == GLFW_REPEAT
    }

    override fun isMouseButtonPressed(button: MouseButton): Boolean {
        val state = glfwGetMouseButton(windowHandle, button.code.toInt())
        return state == GLFW_PRESS
    }

    override fun getMousePosition(): Vec2 {
        val xPos = DoubleArray(1)
        val yPos = DoubleArray(1)
        glfwGetCursorPos(windowHandle, xPos, yPos)
        return Vec2(xPos.first().toFloat(), yPos.first().toFloat())
    }

    override fun getMouseX(): Float {
        return getMousePosition().x
    }

    override fun getMouseY(): Float {
        return getMousePosition().y
    }
}