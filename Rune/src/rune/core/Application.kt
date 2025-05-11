package rune.core

import org.lwjgl.glfw.GLFW.glfwGetTime
import rune.events.Event
import rune.events.EventDispatcher
import rune.events.WindowCloseEvent

import rune.events.WindowResizeEvent
import rune.imgui.ImguiLayer
import rune.renderer.Renderer
import kotlin.system.exitProcess


abstract class Application {
    private val window = Window.create()
    private var running = true
    private var minimized = false
    private val layerStack = LayerStack()
    private val imGuiLayer = ImguiLayer()

    // fps and timing
    private var lastFrameTime = 0.0f
    private var dt = 0f
    private var smoothedDt = 1f / 60f
    private var smoothingFactor = 0.1f
    private var fps: Int = 0

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
        dispatcher.dispatch<WindowResizeEvent>(::onWindowResize)

        for (i in layerStack.lastIndex downTo 0) {
            layerStack[i].onEvent(event)
            if (event.handled) {
                break
            }
        }
    }

    private fun onWindowClosed(e: WindowCloseEvent): Boolean {
        running = false
        return true
    }

    private fun onWindowResize(e: WindowResizeEvent): Boolean {
        if (e.width == 0 || e.height == 0) {
            minimized = true
            return false
        }
        minimized = false
        Renderer.onWindowResize(e.width, e.height)

        return false
    }

    fun run() {
        // (!glfwWindowShouldClose(window.getNativeWindow()))
        while (running) {
            val time: Float = glfwGetTime().toFloat()       // TODO: Platform.getTime() instead of hard coding glfw
            dt = time - lastFrameTime
            lastFrameTime = time
            updateFPS()

            if (!minimized) {
                for (layer in layerStack) {
                    layer.onUpdate(dt)
                }
            }

            imGuiLayer.begin()
            for (layer in layerStack) {
                layer.onImGuiRender()
            }
            imGuiLayer.end()

            window.onUpdate()
        }
        exitProcess(0)
    }

    private fun updateFPS() { smoothedDt += (dt - smoothedDt) * smoothingFactor }

    fun getWindow(): Window = window
    fun getFPS(): Int = (1f / smoothedDt).toInt()
    fun getImGuiLayer(): ImguiLayer = imGuiLayer

    // TODO: TEMP
    fun close() { running = false }

    companion object {
        private lateinit var instance: Application
        fun get(): Application = instance
    }
}
