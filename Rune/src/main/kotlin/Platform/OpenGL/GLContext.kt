package rune.platforms.opengl

import org.lwjgl.glfw.GLFW
import org.lwjgl.opengl.GL
import rune.renderer.RenderContext

class GLContext(private val windowHandle: Long) : RenderContext {
    override fun init() {
        // make the OpenGL current context
        GLFW.glfwMakeContextCurrent(windowHandle)

        // This line is critical for LWJGL's interoperation with GLFW's
        // OpenGL context, or any context that is managed externally.
        // LWJGL detects the context that is current in the current thread,
        // created the GLCapabilities instance and makes the OpenGL
        // bindings available for use.
        GL.createCapabilities()
    }

    override fun flip() {
        GLFW.glfwSwapBuffers(windowHandle)
    }
}