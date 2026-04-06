package rune.terrain

import rune.components.TerrainComponent
import rune.components.TransformComponent
import rune.renderer.Renderer
import rune.rhi.Pipeline
import rune.scene.Scene

object TerrainRenderer {

    fun render(scene: Scene, terrainPipeline: Pipeline) {
        scene.world.family { all(TerrainComponent, TransformComponent) }.forEach { entity ->
            val model = entity[TerrainComponent].model ?: return@forEach
            val transform = entity[TransformComponent].getTransform()
            Renderer.renderStaticMesh(terrainPipeline, model.mesh, transform)
        }
    }
}
