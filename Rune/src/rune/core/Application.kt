package rune.core

import rune.events.Event
import rune.events.EventDispatcher
import rune.events.WindowCloseEvent

import org.lwjgl.opengl.GL30.*
import rune.Application
import rune.imgui.ImguiLayer

abstract class Application {
    private val window = Window.create()
    private var running: Boolean = true
    private val layerStack = LayerStack()
    private val imGuiLayer = ImguiLayer()

    init {
        // setting global instance for Appliation
        instance = this

        Input.init()
        pushOverlay(imGuiLayer)

        window.setEventCallback(::onEvent)
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
            glClearColor(1.0f, 0.0f, 1.0f, 1.0f)
            glClear(GL_COLOR_BUFFER_BIT)

            for (layer in layerStack) {
                layer.onUpdate()
            }

            imGuiLayer.begin()
            // makes sure to render every layer that uses imgui
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
