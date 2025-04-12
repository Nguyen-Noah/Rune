package rune.components

import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.ComponentType
import glm_.vec4.Vec4

class SpriteRenderer(
    val color: Vec4 = Vec4(1.0f)
) : Component<SpriteRenderer> {
    override fun type() = SpriteRenderer

    companion object : ComponentType<SpriteRenderer>()
}