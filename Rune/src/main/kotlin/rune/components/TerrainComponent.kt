package rune.components

import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.ComponentType
import rune.renderer.renderer3d.Model

class TerrainComponent(
    var model: Model? = null,
) : Component<TerrainComponent>, CopyableComponent<TerrainComponent> {

    constructor(other: TerrainComponent) : this(model = other.model)

    override fun type() = TerrainComponent

    override fun copy(): TerrainComponent = TerrainComponent(this)

    companion object : ComponentType<TerrainComponent>()
}
