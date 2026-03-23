package Platform.Windows

import org.lwjgl.glfw.GLFW.glfwGetTime
import rune.core.FrameTimer

class GlfwFrameTimer : FrameTimer() {
    override fun now(): Double = glfwGetTime()
}
