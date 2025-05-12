package rune.components

import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.ComponentType
import glm_.vec2.Vec2
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import rune.scene.serialization.Vec2AsList
import rune.utils.copy

@Serializable
@SerialName("CircleCollider2D")
class CircleCollider2DComponent(
    var radius: Float = 0.5f,
    @Serializable(with = Vec2AsList::class) var offset: Vec2 = Vec2(0f),

    // TODO: move to physics material
    var density: Float = 1f,
    var friction: Float = 0.5f,
    var restitution: Float = 0f
) : Component<CircleCollider2DComponent>, CopyableComponent<CircleCollider2DComponent> {

    constructor(other: CircleCollider2DComponent) : this(
        radius = other.radius,
        offset = other.offset.copy(),
        density = other.density,
        friction = other.friction,
        restitution = other.restitution
    )

    override fun type() = CircleCollider2DComponent

    override fun copy(): CircleCollider2DComponent = CircleCollider2DComponent(this)

    companion object : ComponentType<CircleCollider2DComponent>()
}