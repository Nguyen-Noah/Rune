package rune.renderer

import glm_.vec3.Vec3
import rune.core.Input
import rune.core.Key
import rune.events.Event
import rune.events.EventDispatcher
import rune.events.MouseScrolledEvent
import rune.events.WindowResizeEvent
import kotlin.math.max

class OrthographicCameraController(private var aspectRatio: Float, private val rotation: Boolean = false) {
    private var zoomLevel: Float = 1.5f
    private val cameraPosition: Vec3 = Vec3(0.0, 0.0, 0.0)
    private var cameraRotation: Float = 0.0f
    private var cameraTranslationSpeed: Float = 5.0f
    private val cameraRotationSpeed: Float = 10.0f

    val camera = OrthographicCamera(-aspectRatio * zoomLevel, aspectRatio * zoomLevel, -zoomLevel, zoomLevel)

    fun onUpdate(dt: Float) {
        if (Input.isKeyPressed(Key.A)) {
            cameraPosition.x -= cameraTranslationSpeed * dt
        } else if (Input.isKeyPressed(Key.D)) {
            cameraPosition.x += cameraTranslationSpeed * dt
        }

        if (Input.isKeyPressed(Key.W)) {
            cameraPosition.y += cameraTranslationSpeed * dt
        } else if (Input.isKeyPressed(Key.S)) {
            cameraPosition.y -= cameraTranslationSpeed * dt
        }
        println(rotation)

        if (rotation) {
            if (Input.isKeyPressed(Key.Q)) {
                cameraRotation += cameraRotationSpeed * dt
            } else if (Input.isKeyPressed(Key.E)) {
                cameraRotation -= cameraRotationSpeed * dt
            }
            camera.setRotation(cameraRotation)
        }

        camera.setPosition(cameraPosition)

        cameraTranslationSpeed = zoomLevel
    }

    fun onEvent(e: Event) {
        val dispatcher = EventDispatcher(e)
        dispatcher.dispatch<MouseScrolledEvent>(::onMouseScrolled)
        dispatcher.dispatch<WindowResizeEvent>(::onWindowResized)
    }

    private fun onMouseScrolled(e: MouseScrolledEvent): Boolean {
        zoomLevel -= e.yOffset * 0.25f
        zoomLevel = max(zoomLevel, 0.25f)
        camera.setProjection(-aspectRatio * zoomLevel, aspectRatio * zoomLevel, -zoomLevel, zoomLevel)
        return false
    }

    private fun onWindowResized(e: WindowResizeEvent): Boolean {
        aspectRatio = e.width.toFloat() / e.height.toFloat()
        camera.setProjection(-aspectRatio * zoomLevel, aspectRatio * zoomLevel, -zoomLevel, zoomLevel)
        return false
    }
}