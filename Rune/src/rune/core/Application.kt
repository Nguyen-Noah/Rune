package rune.core

import rune.events.Event
import rune.events.EventDispatcher
import rune.events.WindowCloseEvent

import org.lwjgl.opengl.GL33.*
import rune.Application
import rune.imgui.ImguiLayer
import rune.renderer.IndexBuffer
import rune.renderer.Shader
import rune.renderer.VertexBuffer

abstract class Application {
    private val window = Window.create()
    private var running: Boolean = true
    private val layerStack = LayerStack()
    private val imGuiLayer = ImguiLayer()

    lateinit var shader: Shader
    var vaoId: Int = 0
    lateinit var vbo: VertexBuffer
    lateinit var ibo: IndexBuffer

    init {
        // setting global instance for Appliation
        instance = this

        Input.init()
        pushOverlay(imGuiLayer)

        window.setEventCallback(::onEvent)



        // TEST STUFF FOR OPENGL ----------------------------------------
        vaoId = glGenVertexArrays()
        glBindVertexArray(vaoId)

        val vertices = floatArrayOf(
            -0.5f, -0.5f, 0.0f,
             0.5f, -0.5f, 0.0f,
             0.0f,  0.5f, 0.0f
        )

        vbo = VertexBuffer.create(vertices, vertices.size)

        glEnableVertexAttribArray(0)
        glVertexAttribPointer(0, 3, GL_FLOAT, false, 3 * 4, 0)

        val indices = intArrayOf(0, 1, 2)

        ibo = IndexBuffer.create(indices, 3)

        val vertexSrc = """
            #version 330 core
        
            layout(location = 0) in vec3 a_Position;
            out vec3 v_Position;
        
            void main()
            {
                v_Position = a_Position;
                gl_Position = vec4(a_Position, 1.0);    
            }
        """.trimIndent()

        val fragmentSrc = """
            #version 330 core
        
            layout(location = 0) out vec4 color;
            in vec3 v_Position;
        
            void main()
            {
                color = vec4(v_Position * 0.5 + 0.5, 1.0);
            }
        """.trimIndent()

        shader = Shader(vertexSrc, fragmentSrc)
    }

    fun pushLayer(layer: Layer) {
        layerStack.pushLayer(layer)
        layer.onAttach()
    }

    fun pushOverlay(layer: Layer) {
        layerStack.pushOverlay(layer)
        layer.onAttach()
    }

    private fun onEvent(event: Event) {
        val dispatcher = EventDispatcher(event)
        dispatcher.dispatch<WindowCloseEvent>(::onWindowClosed)

        for (i in layerStack.lastIndex downTo 0) {
            layerStack[i].onEvent(event)
            if (event.handled) {
                break
            }
        }
    }

    private fun onWindowClosed(closeEvent: WindowCloseEvent): Boolean {
        running = false
        return true
    }

    fun run() {
        // (!glfwWindowShouldClose(window.getNativeWindow()))
        while (running) {
            glClearColor(0.1f, 0.1f, 0.1f, 1.0f)
            glClear(GL_COLOR_BUFFER_BIT)

            // OPENGL TEST STUFF ----------------------------------------
            shader.bind()
            glBindVertexArray(vaoId)
            glDrawArrays(GL_TRIANGLES, 0, ibo.getCount())

            for (layer in layerStack) {
                layer.onUpdate()
            }

            imGuiLayer.begin()
            for (layer in layerStack) {
                layer.onImGuiRender()
            }
            imGuiLayer.end()

            window.onUpdate()
        }
    }

    fun getWindow(): Window = window

    companion object {
        private lateinit var instance: Application
        fun get(): Application = instance
    }
}
