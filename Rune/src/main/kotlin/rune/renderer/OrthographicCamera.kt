package rune.renderer

import glm_.glm
import glm_.mat4x4.Mat4
import glm_.vec3.Vec3


class OrthographicCamera(
    left: Float,
    right: Float,
    bottom: Float,
    top: Float
) {
    private var projectionMatrix: Mat4 = Mat4(1.0)
    private var viewMatrix: Mat4 = Mat4(1.0)
    private var viewProjectionMatrix: Mat4 = Mat4(1.0)

    private var position: Vec3 = Vec3()
    private var rotation: Float = 0.0f

    init {
        projectionMatrix = glm.ortho(left, right, bottom, top, -1.0f, 1.0f)
        viewProjectionMatrix = projectionMatrix * viewMatrix
    }

    fun setPosition(position: Vec3) {
        this.position = position
        recalculateViewMatrix()
    }
    fun getPosition(): Vec3 = position

    fun setRotation(rotation: Float) {
        this.rotation = rotation
        recalculateViewMatrix()
    }
    fun getRotation(): Float = rotation

    fun getProjectionMatrix(): Mat4 = projectionMatrix
    fun getViewMatrix(): Mat4 = viewMatrix
    fun getViewProjectionMatrix(): Mat4 = viewProjectionMatrix

    private fun recalculateViewMatrix() {
        val transform = glm.translate(Mat4(1.0f), position) * glm.rotate(Mat4(1.0f), rotation, Vec3(0, 0, 1))
        viewMatrix = glm.inverse(transform)
        viewProjectionMatrix = projectionMatrix * viewMatrix
    }

    fun setProjection(left: Float, right: Float, bottom: Float, top: Float) {
        projectionMatrix = glm.ortho(left, right, bottom, top)
        viewProjectionMatrix = projectionMatrix * viewMatrix
    }
}