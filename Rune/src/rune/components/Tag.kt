package rune.components

import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.ComponentType

data class TagComponent(
    var tag: String
) : Component<TagComponent> {
    override fun type(): ComponentType<TagComponent> = TagComponent

    companion object : ComponentType<TagComponent>()
}