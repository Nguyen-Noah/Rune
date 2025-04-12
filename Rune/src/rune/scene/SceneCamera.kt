package rune.scene

import glm_.glm
import rune.renderer.RuneCamera

class SceneCamera : RuneCamera() {
    private var orthographicSize = 10f
    private var orthographicNear = -1f
    private var orthographicFar = 1f

//    private var viewportWidth = 0.0f
//    private var viewportHeight = 0.0f
    private var aspectRatio = 0.0f

    init {
        recalculateProjection()
    }

    fun setOrthographic(size: Float, nearClip: Float, farClip: Float) {
        orthographicSize = size
        orthographicNear = nearClip
        orthographicFar = farClip

        recalculateProjection() // TODO: check if the values are actually different
    }

    fun setOrthographicSize(size: Float) {
        orthographicSize = size
        recalculateProjection()
    }

    fun getOrthographicSize() = orthographicSize

    fun setViewportSize(width: Int, height: Int) {
        aspectRatio = width.toFloat() / height.toFloat()
        recalculateProjection()
    }

    private fun recalculateProjection() {
        val orthoLeft: Float =     -orthographicSize * aspectRatio * 0.5f
        val orthoRight: Float =     orthographicSize * aspectRatio * 0.5f
        val orthoBottom: Float =   -orthographicSize * 0.5f
        val orthoTop: Float =       orthographicSize * 0.5f

        projection = glm.ortho(orthoLeft, orthoRight, orthoBottom, orthoTop, orthographicNear, orthographicFar)
    }
}