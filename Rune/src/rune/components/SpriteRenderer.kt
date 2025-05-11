package rune.components

import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.ComponentType
import glm_.vec4.Vec4
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import rune.renderer.Texture2D
import rune.scene.serialization.Texture2DPath
import rune.scene.serialization.Vec4AsList
import rune.utils.copy

@Serializable
@SerialName("SpriteRenderer")
class SpriteRendererComponent(
    @Serializable(with = Vec4AsList::class) var color: Vec4 = Vec4(1.0f),
    @Serializable(with = Texture2DPath::class) var texture: Texture2D? = null,
    var tilingFactor: Float = 1f
) : Component<SpriteRendererComponent>, CopyableComponent<SpriteRendererComponent> {

    constructor(other: SpriteRendererComponent) : this(
        color = other.color.copy(),
        texture = other.texture,
        tilingFactor = other.tilingFactor
    )

    override fun type() = SpriteRendererComponent

    override fun copy(): SpriteRendererComponent = SpriteRendererComponent(this)

    companion object : ComponentType<SpriteRendererComponent>()
}