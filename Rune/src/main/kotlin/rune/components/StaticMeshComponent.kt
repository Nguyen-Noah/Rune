package rune.components

import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.ComponentType
import kotlinx.serialization.Serializable
import rune.renderer.renderer3d.Model

class StaticMeshComponent(
    var model: Model? = null
) : Component<StaticMeshComponent>, CopyableComponent<StaticMeshComponent> {

    constructor(other: StaticMeshComponent) : this(
        model = other.model
    )

    override fun type() = StaticMeshComponent

    override fun copy(): StaticMeshComponent = StaticMeshComponent(this)

    companion object : ComponentType<StaticMeshComponent>()
}