package rune.components

import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.ComponentType
import glm_.vec4.Vec4

class SpriteRendererComponent(
    val color: Vec4 = Vec4(1.0f)
) : Component<SpriteRendererComponent> {
    override fun type() = SpriteRendererComponent

    companion object : ComponentType<SpriteRendererComponent>()
}