package rune.components

import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.ComponentType
import glm_.vec2.Vec2
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import rune.scene.serialization.Vec2AsList
import rune.utils.copy

// TODO: move attributes into physics material
@Serializable
@SerialName("BoxCollider2D")
class BoxCollider2DComponent(
    @Serializable(with = Vec2AsList::class) var offset: Vec2 = Vec2(0f),
    @Serializable(with = Vec2AsList::class) var size: Vec2 = Vec2(1f),
    var density: Float = 1f,
    var friction: Float = 1f,
    var restitution: Float = 0.5f,
    var restitutionThreshold: Float = 0.5f,     // TODO: unused
    //runtimeFixture
) : Component<BoxCollider2DComponent>, CopyableComponent<BoxCollider2DComponent> {

    constructor(other: BoxCollider2DComponent) : this(
        offset = other.offset.copy(),
        size = other.size.copy(),
        density = other.density,
        friction = other.friction,
        restitution = other.restitution,
        restitutionThreshold = other.restitutionThreshold
    )

    override fun type() = BoxCollider2DComponent

    override fun copy(): BoxCollider2DComponent = BoxCollider2DComponent(this)

    companion object : ComponentType<BoxCollider2DComponent>()
}