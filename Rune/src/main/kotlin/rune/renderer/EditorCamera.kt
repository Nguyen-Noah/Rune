package rune.renderer

import glm_.glm
import glm_.mat3x3.Mat3
import glm_.mat4x4.Mat4
import glm_.quat.Quat
import glm_.vec2.Vec2
import glm_.vec3.Vec3
import rune.core.Input
import rune.core.Key
import rune.core.MouseButton
import rune.events.EventDispatcher
import rune.events.MouseScrolledEvent

class EditorCamera(private val fov: Float = 45f,
                   private var aspectRatio: Float = 1.778f,
                   private val nearClip: Float = 0.1f,
                   private val farClip: Float = 1000f
) : RuneCamera(glm.perspective(glm.radians(fov), aspectRatio, nearClip, farClip)) {
    private var position: Vec3 = Vec3(0f)
    private var focalPoint: Vec3 = Vec3(0f)

    private var initialMousePosition: Vec2 = Vec2(0f)

    private var distance = 10f
    private var pitch = 0f
    private var yaw = 0f

    private var viewportWidth = 1280f
    private var viewportHeight = 720f

    lateinit var viewMatrix: Mat4

    init {
        updateView()
    }

    private fun updateProjection() {
        aspectRatio = viewportWidth / viewportHeight
        projection = glm.perspective(glm.radians(fov), aspectRatio, nearClip, farClip)
    }

    private fun updateView() {
        // yaw = 0f; pitch = 0f     lock the camera rotation
        position = calculatePosition()

        val orientation = getOrientation()
        viewMatrix = glm.translate(Mat4(1f), position) * orientation.toMat4()
        viewMatrix = glm.inverse(viewMatrix)
    }

    private fun panSpeed(): Pair<Float, Float> {
        val x = (viewportWidth / 1000f).coerceAtMost(2.4f) // pretty much a min()
        val y = (viewportWidth / 1000f).coerceAtMost(2.4f) // pretty much a min()
        val xFactor =  0.0366f * x*x - 0.1778f * x + 0.3021f
        val yFactor =  0.0366f * y*y - 0.1778f * y + 0.3021f
        return xFactor to yFactor
    }

    private fun rotationSpeed() = 0.8f

    private fun zoomSpeed(): Float {
        val d = (distance * 0.2f).coerceAtLeast(0f)
        return (d * d).coerceAtMost(100f)
    }

    fun onUpdate(dt: Float) {
        if (Input.isKeyPressed(Key.Space)) {
            val mouse = Vec2(Input.getMouseX(), Input.getMouseY())
            val delta = (mouse - initialMousePosition) * 0.003f
            initialMousePosition = mouse

            when {
                Input.isMouseButtonPressed(MouseButton.ButtonMiddle) -> mousePan(delta)
                Input.isMouseButtonPressed(MouseButton.ButtonLeft)   -> mouseRotate(delta)
                Input.isMouseButtonPressed(MouseButton.ButtonRight)  -> mouseZoom(delta.y)
            }
        }
        updateView()
    }

    fun onEvent(e: rune.events.Event) {
        EventDispatcher(e).dispatch<MouseScrolledEvent> { onMouseScroll(it) }
    }

    private fun onMouseScroll(e: MouseScrolledEvent): Boolean {
        mouseZoom(e.yOffset * 0.1f)
        updateView()
        return false
    }

    private fun mousePan(delta: Vec2) {
        val (xSpeed, ySpeed) = panSpeed()

        val rightOffset = getRightDirection() * (delta.x * xSpeed * distance)
        focalPoint = focalPoint - rightOffset

        val upOffset = getUpDirection() * (delta.y * ySpeed * distance)
        focalPoint = focalPoint + upOffset
    }

    private fun mouseRotate(delta: Vec2) {
        val sign = if (getUpDirection().y < 0f) -1f else 1f
        yaw     += sign * delta.x * rotationSpeed()
        pitch   +=        delta.y * rotationSpeed()
    }

    private fun mouseZoom(delta: Float) {
        distance -= delta * zoomSpeed()
        if (distance < 1f) {
            focalPoint = focalPoint + getForwardDirection()
            distance = 1f
        }
    }

    fun getViewProjection() = projection * viewMatrix

    fun setViewportSize(width: Float, height: Float) {
        viewportWidth = width
        viewportHeight = height
        updateProjection()
    }

    fun getSkyViewProjection(): Mat4 {
        val rot3x3 = Mat3(viewMatrix)

        val rotView = Mat4(rot3x3)
        return glm.inverse(projection * rotView)
    }

    private fun getUpDirection() =      glm.rotate(getOrientation(), Vec3(0f, 1f, 0f))
    private fun getRightDirection() =   glm.rotate(getOrientation(), Vec3(1f, 0f, 0f))
    private fun getForwardDirection() = glm.rotate(getOrientation(), Vec3(0f, 0f, -1f))

    private fun calculatePosition() = focalPoint - getForwardDirection() * distance
    private fun getOrientation() = Quat(Vec3(-pitch, -yaw, 0f))
}