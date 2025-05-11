package rune.components

import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.ComponentType
import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.World
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import rune.core.UUID
import rune.scene.serialization.UUIDSerializer

@Serializable
@SerialName("ID")
data class IDComponent(
    @Serializable(with = UUIDSerializer::class) val id: UUID
): Component<IDComponent>, CopyableComponent<IDComponent> {

    // probably dont keep this
    constructor(other: IDComponent) : this(
        id = other.id
    )

    override fun type(): ComponentType<IDComponent> = IDComponent

    override fun copy(): IDComponent = IDComponent(this)

    companion object : ComponentType<IDComponent>()
}