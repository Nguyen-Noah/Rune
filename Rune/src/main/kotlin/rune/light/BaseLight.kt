package rune.light

import glm_.vec4.Vec4

data class BaseLight(
    var color: Vec4 = Vec4(),
    var ambientIntensity: Float = 0f
)
