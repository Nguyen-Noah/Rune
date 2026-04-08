package rune.renderer

import glm_.mat4x4.Mat4
import glm_.vec3.Vec3
import rune.events.Event

interface SceneViewportCamera {
    val viewMatrix: Mat4
    val projection: Mat4
    val position: Vec3
    fun getViewProjection(): Mat4
    fun getSkyViewProjection(): Mat4
    fun setViewportSize(width: Float, height: Float)
    fun onUpdate(dt: Float)
    fun onEvent(e: Event)
}
