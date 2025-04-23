package rune.components

import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.ComponentType
import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.World

data class TagComponent(
    var tag: String
) : Component<TagComponent> {
    override fun type(): ComponentType<TagComponent> = TagComponent

    companion object : ComponentType<TagComponent>()

    /**
     * Tags are added to entities automatically in World.createEntity(),
     * so this is used for deserialization to set the entity's name to the tag
     */
    override fun World.onAdd(entity: Entity) {
        // TODO
    }
}