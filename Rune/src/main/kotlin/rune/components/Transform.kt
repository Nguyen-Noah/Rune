package rune.components

import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.ComponentType
import glm_.glm
import glm_.mat4x4.Mat4
import glm_.quat.Quat
import glm_.vec3.Vec3
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import rune.scene.serialization.Vec3AsList
import rune.utils.copy

@Serializable
@SerialName("Transform")
data class TransformComponent(
    @Serializable(with = Vec3AsList::class) var translation: Vec3 = Vec3(0.0f),
    @Serializable(with = Vec3AsList::class) var rotation:    Vec3 = Vec3(0.0f),
    @Serializable(with = Vec3AsList::class) var scale:       Vec3 = Vec3(1.0f)
) : Component<TransformComponent>, CopyableComponent<TransformComponent> {

    constructor(other: TransformComponent) : this(
        translation = other.translation.copy(),
        rotation    = other.rotation.copy(),
        scale       = other.scale.copy()
    )

    override fun type(): ComponentType<TransformComponent> = TransformComponent

    override fun copy(): TransformComponent = TransformComponent(this)

    fun getTransform(): Mat4 {
        return glm.translate(Mat4(1f), translation) *
                Quat(rotation).toMat4() *
                glm.scale(Mat4(1f), scale)
    }

    companion object : ComponentType<TransformComponent>()
}
