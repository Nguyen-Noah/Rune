package rune.components

import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.ComponentType
import glm_.vec3.Vec3
import kotlinx.serialization.Serializable
import rune.scene.serialization.Vec3AsList

@Serializable
class DirectionalLightComponent(
    @Serializable(with = Vec3AsList::class) var color: Vec3 = Vec3(1f),
    var diffuseIntensity: Float = 1f,
    @Serializable(with = Vec3AsList::class) var direction: Vec3 = Vec3(1f)
) : Component<DirectionalLightComponent>, CopyableComponent<DirectionalLightComponent> {

    constructor(other: DirectionalLightComponent) : this(
        color = other.color,
        diffuseIntensity = other.diffuseIntensity,
        direction = other.direction
    )

    override fun type() = DirectionalLightComponent

    override fun copy(): DirectionalLightComponent = DirectionalLightComponent(this)

    companion object : ComponentType<DirectionalLightComponent>()
}