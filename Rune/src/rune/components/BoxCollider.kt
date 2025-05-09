package rune.components

import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.ComponentType
import glm_.vec2.Vec2

// TODO: move attributes into physics material
class BoxCollider2DComponent(
    var offset: Vec2 = Vec2(0f),
    var size: Vec2 = Vec2(1f),
    var density: Float = 1f,
    var friction: Float = 1f,
    var restitution: Float = 0.5f,
    var restitutionThreshold: Float = 0.5f,     // TODO: unused
    //runtimeFixture
) : Component<BoxCollider2DComponent> {
    override fun type() = BoxCollider2DComponent

    companion object : ComponentType<BoxCollider2DComponent>()
}