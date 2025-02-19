package rune.platforms.windows

import org.lwjgl.glfw.GLFW.*
import org.lwjgl.opengl.GL
import rune.core.*
import rune.events.*

class WindowsWindow(props: WindowProps) : Window {
    private data class WindowData(
        var title: String,
        var width: Int,
        var height: Int,
        var vSync: Boolean,
        var eventCallback: EventCallbackFn = {}
    )

    // window info
    private val data = WindowData(
        title = props.title,
        width = props.width,
        height = props.height,
        vSync = false
    )

    private var windowHandle: Long = 0L

    init {
        initWindow(props)
    }

    private fun initWindow(props: WindowProps) {
        data.title = props.title
        data.width = props.width
        data.height = props.height

        println("Creating window $props.title ($props.width, $props.height)")

        // if GLFW isn't initialized, do it now
        if (!glfwInit()) {
            val success = glfwInit()
            require(success) { "Could not initialize GLFW" }

            glfwSetErrorCallback { error, description ->
                println("GLFW Error ($error): $description")
            }
        }

        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3)
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 3)
        glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE)
        glfwWindowHint(GLFW_OPENGL_FORWARD_COMPAT, GLFW_TRUE)

        // create the GLFW window
        windowHandle = glfwCreateWindow(
            props.width,
            props.height,
            props.title,
            0L,
            0L
        )
        require(windowHandle != 0L) { "Failed to create GLFW window" }

        // make the OpenGL current context
        glfwMakeContextCurrent(windowHandle)

        // enable v-sync
        setVSync(true)

        // This line is critical for LWJGL's interoperation with GLFW's
        // OpenGL context, or any context that is managed externally.
        // LWJGL detects the context that is current in the current thread,
        // created the GLCapabilities instance and makes the OpenGL
        // bindings available for use.
        GL.createCapabilities()

        // set GLFW callbacks
        glfwSetWindowSizeCallback(windowHandle) { _, newWidth, newHeight ->
            data.width = newWidth
            data.height = newHeight

            val event = WindowResizeEvent(newWidth, newHeight)
            data.eventCallback(event)
        }

        glfwSetWindowCloseCallback(windowHandle) {
            val event = WindowCloseEvent()
            data.eventCallback(event)
        }

        glfwSetKeyCallback(windowHandle) { window, key, scancode, action, mods ->
            val keyCode = Key.fromCode(key) ?: return@glfwSetKeyCallback

            when (action) {
                GLFW_PRESS -> {
                    val event = KeyPressedEvent(keyCode, false)
                    data.eventCallback(event)
                }
                GLFW_RELEASE -> {
                    val event = KeyReleasedEvent(keyCode)
                    data.eventCallback(event)
                }
                GLFW_REPEAT -> {
                    val event = KeyPressedEvent(keyCode, true)
                    data.eventCallback(event)
                }
            }
        }

        glfwSetCharCallback(windowHandle) { window, key ->
            val keyCode = Key.fromCode(key) ?: return@glfwSetCharCallback

            val event = KeyTypedEvent(keyCode)
            data.eventCallback(event)
        }

        glfwSetMouseButtonCallback(windowHandle) { window, button, action, mods ->
            val buttonCode = MouseButton.fromCode(button) ?: return@glfwSetMouseButtonCallback

            when (action) {
                GLFW_PRESS -> {
                    val event = MouseButtonPressedEvent(buttonCode)
                    data.eventCallback(event)
                }

                GLFW_RELEASE -> {
                    val event = MouseButtonReleasedEvent(buttonCode)
                    data.eventCallback(event)
                }
            }
        }

        glfwSetScrollCallback(windowHandle) { window, xOffset, yOffset ->
            val event = MouseScrolledEvent(xOffset.toFloat(), yOffset.toFloat())
            data.eventCallback(event)
        }

        glfwSetCursorPosCallback(windowHandle) { window, xPos, yPos ->
            val event = MouseMovedEvent(xPos.toFloat(), yPos.toFloat())
            data.eventCallback(event)
        }
    }

    override val width: Int
        get() = data.width
    override val height: Int
        get() = data.height

    override fun onUpdate() {
        // poll events
        glfwPollEvents()
        glfwSwapBuffers(windowHandle)
    }

    override fun setEventCallback(callback: EventCallbackFn) {
        data.eventCallback = callback
    }

    override fun setVSync(enabled: Boolean) {
        if (enabled) glfwSwapInterval(1)
        else glfwSwapInterval(0)
    }

    override fun isVSync(): Boolean {
        TODO("Not yet implemented")
    }

    override fun getNativeWindow(): Long = windowHandle

}