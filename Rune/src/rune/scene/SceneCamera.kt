package rune.scene

import glm_.glm
import rune.renderer.RuneCamera

enum class ProjectionType(private val type: Int) {
    Perspective(0),
    Orthographic(1);
    companion object {
        fun fromInt(value: Int) = entries.first { it.type == value }
    }
}

class SceneCamera : RuneCamera() {
    var orthographicSize = 10f
        set(size) {
            field = size
            recalculateProjection()
        }
    var orthographicNear = -1f
        set(value) {
            field = value
            recalculateProjection()
        }
    var orthographicFar = 1f
        set(value) {
            field = value
            recalculateProjection()
        }

    var perspectiveFOV = glm.radians(45f)
        set(fov) {
            field = fov
            recalculateProjection()
        }
    var perspectiveNear = 0.01f
        set(value) {
            field = value
            recalculateProjection()
        }
    var perspectiveFar = 1000.0f
        set(value) {
            field = value
            recalculateProjection()
        }

    private var aspectRatio = 0.0f

    var projectionType = ProjectionType.Orthographic
        set(type) {
            field = type
            recalculateProjection()
        }


    init {
        recalculateProjection()
    }

    fun setOrthographic(size: Float, nearClip: Float, farClip: Float) {
        projectionType = ProjectionType.Orthographic
        orthographicSize = size
        orthographicNear = nearClip
        orthographicFar = farClip

        recalculateProjection() // TODO: check if the values are actually different
    }

    fun setPerspective(verticalFOV: Float, nearClip: Float, farClip: Float) {
        projectionType = ProjectionType.Perspective
        perspectiveFOV = verticalFOV
        perspectiveNear = nearClip
        perspectiveFar = farClip

        recalculateProjection()
    }

    fun setViewportSize(width: Int, height: Int) {
        //require(width > 0 && height > 0)      TODO: make this true when loading
        aspectRatio = width.toFloat() / height.toFloat()
        recalculateProjection()
    }

    private fun recalculateProjection() {
        if (projectionType == ProjectionType.Perspective) {
            projection = glm.perspective(perspectiveFOV, aspectRatio, perspectiveNear, perspectiveFar)
        } else {
            val orthoLeft: Float =     -orthographicSize * aspectRatio * 0.5f
            val orthoRight: Float =     orthographicSize * aspectRatio * 0.5f
            val orthoBottom: Float =   -orthographicSize * 0.5f
            val orthoTop: Float =       orthographicSize * 0.5f

            projection = glm.ortho(orthoLeft, orthoRight, orthoBottom, orthoTop, orthographicNear, orthographicFar)
        }

    }
}