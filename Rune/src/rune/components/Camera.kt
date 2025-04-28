package rune.components

import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.ComponentType
import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.World
import rune.scene.Scene
import rune.scene.SceneCamera

class CameraComponent(
    val camera: SceneCamera = SceneCamera(),
    var primary: Boolean = true,
    var fixedAspectRatio: Boolean = false
) : Component<CameraComponent> {
    override fun type() = CameraComponent

    companion object : ComponentType<CameraComponent>()

    override fun World.onAdd(entity: Entity) {
        val scene = this.inject<Scene>()
        val cameraComp = entity[CameraComponent]
        if ((!cameraComp.fixedAspectRatio) || (scene.viewportWidth != 0 && scene.viewportHeight != 0)) {
            cameraComp.camera.setViewportSize(scene.viewportWidth, scene.viewportHeight)
        }
    }
}