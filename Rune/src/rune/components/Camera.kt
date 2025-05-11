package rune.components

import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.ComponentType
import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.World
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import rune.scene.Scene
import rune.scene.SceneCamera
import rune.scene.serialization.SceneCameraSerializer

@Serializable
@SerialName("Camera")
class CameraComponent(
    @Serializable(with = SceneCameraSerializer::class) val camera: SceneCamera = SceneCamera(),
    var primary: Boolean = false,
    var fixedAspectRatio: Boolean = false
) : Component<CameraComponent>, CopyableComponent<CameraComponent> {

    constructor(other: CameraComponent) : this(
        camera = other.camera,
        primary = other.primary,
        fixedAspectRatio = other.fixedAspectRatio
    )

    override fun type() = CameraComponent

    override fun copy(): CameraComponent = CameraComponent(this)

    companion object : ComponentType<CameraComponent>()

    override fun World.onAdd(entity: Entity) {
        val scene = this.inject<Scene>()
        val cameraComp = entity[CameraComponent]
        if ((!cameraComp.fixedAspectRatio) || (scene.viewportWidth != 0 && scene.viewportHeight != 0)) {
            cameraComp.camera.setViewportSize(scene.viewportWidth, scene.viewportHeight)
        }
    }
}