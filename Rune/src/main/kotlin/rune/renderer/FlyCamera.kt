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
import rune.events.Event
import rune.events.EventDispatcher
import rune.events.MouseScrolledEvent
import kotlin.math.PI

/**
 * First-person style camera: **WASD** moves along view forward / right (full 3D).
 * Hold **right mouse** and move the mouse to look around.
 * **Left Shift** increases move speed; mouse wheel adjusts speed slightly.
 */
class FlyCamera(
    private val fov: Float = 45f,
    private var aspectRatio: Float = 1.778f,
    private val nearClip: Float = 0.1f,
    private val farClip: Float = 1000f,
    initialPosition: Vec3 = Vec3(0f, 2f, 4f),
    private var moveSpeed: Float = 12f,
    private var mouseSensitivity: Float = 0.0022f,
) : RuneCamera(glm.perspective(glm.radians(fov), aspectRatio, nearClip, farClip)), SceneViewportCamera {

    override var position = initialPosition
    private var pitch = 0f
    private var yaw = 0f

    private var viewportWidth = 1280f
    private var viewportHeight = 720f

    private var prevMouse = Vec2(Input.getMouseX(), Input.getMouseY())

    override lateinit var viewMatrix: Mat4
        private set

    init {
        updateView()
    }

    private fun updateProjection() {
        aspectRatio = viewportWidth / viewportHeight
        projection = glm.perspective(glm.radians(fov), aspectRatio, nearClip, farClip)
    }

    private fun getOrientation() = Quat(Vec3(-pitch, -yaw, 0f))

    private fun getForward() = glm.rotate(getOrientation(), Vec3(0f, 0f, -1f))
    private fun getRight() = glm.rotate(getOrientation(), Vec3(1f, 0f, 0f))
    private fun getUp() = glm.rotate(getOrientation(), Vec3(0f, 1f, 0f))

    private fun updateView() {
        val camWorld = glm.translate(Mat4(1f), position) * getOrientation().toMat4()
        viewMatrix = glm.inverse(camWorld)
    }

    override fun onUpdate(dt: Float) {
        val mouse = Vec2(Input.getMouseX(), Input.getMouseY())
        if (Input.isMouseButtonPressed(MouseButton.ButtonRight)) {
            val delta = mouse - prevMouse
            yaw += delta.x * mouseSensitivity
            pitch += delta.y * mouseSensitivity
            val limit = (PI / 2f - 0.01f).toFloat()
            pitch = pitch.coerceIn(-limit, limit)
        }
        prevMouse = mouse

        val forward = getForward()
        val right = getRight()
        val up = getUp()
        var move = Vec3(0f)
        if (Input.isKeyPressed(Key.W)) move = move + forward
        if (Input.isKeyPressed(Key.S)) move = move - forward
        if (Input.isKeyPressed(Key.D)) move = move + right
        if (Input.isKeyPressed(Key.A)) move = move - right

        if (Input.isKeyPressed(Key.LeftControl)) move = move - up
        if (Input.isKeyPressed(Key.Space)) move = move + up

        var speed = moveSpeed
        if (Input.isKeyPressed(Key.LeftShift) || Input.isKeyPressed(Key.RightShift)) {
            speed *= 2.5f
        }

        val len = glm.length(move)
        if (len > 1e-6f) {
            move = (move / len) * (speed * dt)
            position = position + move
        }

        updateView()
    }

    override fun onEvent(e: Event) {
        EventDispatcher(e).dispatch<MouseScrolledEvent> { onMouseScroll(it) }
    }

    private fun onMouseScroll(e: MouseScrolledEvent): Boolean {
        val factor = if (e.yOffset > 0f) 1.08f else 0.92f
        moveSpeed = (moveSpeed * factor).coerceIn(1f, 200f)
        return false
    }

    override fun getViewProjection() = projection * viewMatrix

    override fun getSkyViewProjection(): Mat4 {
        val rot3x3 = Mat3(viewMatrix)
        val rotView = Mat4(rot3x3)
        return glm.inverse(projection * rotView)
    }

    override fun setViewportSize(width: Float, height: Float) {
        viewportWidth = width
        viewportHeight = height
        updateProjection()
    }
}
