package rune.platforms.windows

import org.lwjgl.glfw.GLFW.*
import rune.core.*
import rune.events.*
import rune.platforms.opengl.OpenGLContext

class WindowsWindow(props: WindowProps) : Window {
    lateinit var ctx: OpenGLContext

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

        println("Creating window ${props.title} (${props.width}, ${props.height})")

        // if GLFW isn't initialized, do it now
        if (!glfwInit()) {
            val success = glfwInit()
            require(success) { "Could not initialize GLFW" }

            glfwSetErrorCallback { error, description ->
                println("GLFW Error ($error): $description")
            }
        }

        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 4)
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 5)
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

        ctx = OpenGLContext(windowHandle)
        ctx.init()

        // enable v-sync
        setVSync(false)

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
        ctx.flip()
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