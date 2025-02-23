package rune.core

import glm_.vec4.Vec4
import rune.events.Event
import rune.events.EventDispatcher
import rune.events.WindowCloseEvent

import org.lwjgl.opengl.GL33.*
import rune.Application
import rune.imgui.ImguiLayer
import rune.renderer.*
import rune.rune.renderer.RenderCommand

abstract class Application {
    private val window = Window.create()
    private var running: Boolean = true
    private val layerStack = LayerStack()
    private val imGuiLayer = ImguiLayer()

    private lateinit var shader: Shader
    private lateinit var vao: VertexArray
    private lateinit var vbo: VertexBuffer
    private lateinit var ibo: IndexBuffer

    init {
        // setting global instance for Appliation
        instance = this

        Input.init()
        pushOverlay(imGuiLayer)

        window.setEventCallback(::onEvent)



        // TEST STUFF FOR OPENGL ----------------------------------------
        val vertices = floatArrayOf(
            -0.5f, -0.5f, 0.0f,
             0.5f, -0.5f, 0.0f,
             0.5f,  0.5f, 0.0f,
            -0.5f,  0.5f, 0.0f
        )

        vbo = VertexBuffer.create(vertices, vertices.size)
        ibo = IndexBuffer.create(intArrayOf(0, 1, 2, 2, 3, 0), 6)

        vao = VertexArray.create(vbo, bufferLayout {
            attribute("a_Position", 3)
        })
        vao.setIndexBuffer(ibo)

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
            RenderCommand.setClearColor(Vec4(0.1f, 0.1f, 0.1f, 1.0f))
            RenderCommand.clear()

            Renderer.beginScene()
            shader.bind()
            Renderer.submit(vao)
            Renderer.endScene()

            // OPENGL TEST STUFF ----------------------------------------

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
