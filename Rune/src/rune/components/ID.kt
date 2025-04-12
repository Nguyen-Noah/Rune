package rune.components

import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.ComponentType
import rune.core.UUID

data class IDComponent(
    val id: UUID
): Component<IDComponent> {
    override fun type(): ComponentType<IDComponent> = IDComponent

    companion object : ComponentType<IDComponent>()
}