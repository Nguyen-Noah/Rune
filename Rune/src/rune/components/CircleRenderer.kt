package rune.components

import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.ComponentType
import glm_.vec4.Vec4
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import rune.scene.serialization.Vec4AsList
import rune.utils.copy

@Serializable
@SerialName("CircleRenderer")
class CircleRendererComponent(
    @Serializable(with = Vec4AsList::class) var color: Vec4 = Vec4(1f),
    var radius: Float = 0.5f,    // 1m
    var thickness: Float = 1f,
    var fade: Float = 0.005f
) : Component<CircleRendererComponent>, CopyableComponent<CircleRendererComponent> {

    constructor(other: CircleRendererComponent) : this(
        color = other.color.copy(),
        radius = other.radius,
        thickness = other.thickness,
        fade = other.fade
    )

    override fun type() = CircleRendererComponent

    override fun copy(): CircleRendererComponent = CircleRendererComponent(this)

    companion object : ComponentType<CircleRendererComponent>()
}