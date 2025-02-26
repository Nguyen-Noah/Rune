package rune.core

import org.lwjgl.glfw.GLFW.glfwGetTime
import rune.events.Event
import rune.events.EventDispatcher
import rune.events.WindowCloseEvent

import rune.Application
import rune.imgui.ImguiLayer
import rune.renderer.Renderer


abstract class Application {
    private val window = Window.create()
    private var running: Boolean = true
    private var lastFrameTime = 0.0f
    private val layerStack = LayerStack()
    private val imGuiLayer = ImguiLayer()

    init {
        // setting global instance for Appliation
        instance = this

        Input.init()
        pushOverlay(imGuiLayer)

        window.setEventCallback(::onEvent)

        Renderer.init()
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
            val time: Float = glfwGetTime().toFloat()       // Platform.getTime() instead of hard coding glfw
            val dt = time - lastFrameTime
            lastFrameTime = time

            for (layer in layerStack) {
                layer.onUpdate(dt)
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
